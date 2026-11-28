package me.rauldoq.geoAnarchy.onboarding;

import me.rauldoq.geoAnarchy.GeoAnarchy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A new player's first minutes: right after they log in for the first time, they choose the continent they
 * start on (a world map menu, or a Bedrock window), land on a safe spot there away from every country, and get
 * the "Guia do GeoAnarchy" book. This is the only teleport on the server, once, for players who just arrived:
 * the choice is saved, and /comecar works only until it is made and only in the first minutes of play.
 */
public final class Onboarding implements Listener, TabExecutor {

    public static final String ADMIN = "geoanarchy.comecar.admin";

    private final GeoAnarchy plugin;
    private final StartUi ui;
    private final Landing landing;
    private final StartForms forms;
    private final NamespacedKey chosenKey;
    private final NamespacedKey bookKey;
    /** On the guide book item, so opening it can count as reading it. */
    private final NamespacedKey guideItemKey;
    private Newcomers newcomers;
    private Recruiting recruiting;
    /** Players whose landing spot is being searched (one search at a time each). */
    private final Set<UUID> searching = ConcurrentHashMap.newKeySet();
    /** Players who pressed a button: their menu closing is not "closed without choosing". */
    private final Set<UUID> pressed = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitTask> waiting = new ConcurrentHashMap<>();
    private Method authMeCheck;
    private Object authMeApi;

    private Onboarding(GeoAnarchy plugin, StartUi ui) {
        this.plugin = plugin;
        this.ui = ui;
        this.landing = new Landing(plugin::countries);
        this.forms = StartForms.create();
        this.chosenKey = new NamespacedKey(plugin, "start");
        this.bookKey = new NamespacedKey(plugin, "guide");
        this.guideItemKey = new NamespacedKey(plugin, "guide_book");
    }

    /** The first hours after landing (steps, protection, tips) and recruiting. */
    void connect(Newcomers newcomers, Recruiting recruiting) {
        this.newcomers = newcomers;
        this.recruiting = recruiting;
    }

    /** Null when ui.json is missing from the jar (then there is no selector). */
    public static Onboarding create(GeoAnarchy plugin) {
        try (InputStream in = plugin.getResource("ui.json")) {
            if (in == null) {
                plugin.getLogger().warning("ui.json is missing from the jar: no continent selector.");
                return null;
            }
            return new Onboarding(plugin, StartUi.load(in));
        } catch (IOException | RuntimeException error) {
            plugin.getLogger().warning("ui.json could not be read (" + error + "): no continent selector.");
            return null;
        }
    }

    /**
     * Builds and registers everything that follows the landing: the first steps, protection, tips, recruiting and
     * their commands. Returns what onDisable hands back to {@link #stopNewcomers}.
     */
    public static Newcomers startNewcomers(GeoAnarchy plugin, Onboarding onboarding) {
        Recruiting recruiting = new Recruiting(plugin, onboarding.ui);
        Newcomers newcomers = new Newcomers(plugin, onboarding, recruiting);
        recruiting.setNewcomers(newcomers);
        onboarding.connect(newcomers, recruiting);
        var events = plugin.getServer().getPluginManager();
        events.registerEvents(newcomers, plugin);
        events.registerEvents(new Protection(newcomers), plugin);
        events.registerEvents(new Tips(plugin, newcomers), plugin);
        events.registerEvents(recruiting, plugin);
        for (String name : List.of("guia", "passos", "dicas", "protecao")) {
            org.bukkit.command.PluginCommand command = plugin.getCommand(name);
            if (command != null) {
                command.setExecutor(newcomers);
                command.setTabCompleter(newcomers);
            }
        }
        for (String name : List.of("recrutar", "recrutando")) {
            org.bukkit.command.PluginCommand command = plugin.getCommand(name);
            if (command != null) {
                command.setExecutor(recruiting);
                command.setTabCompleter(recruiting);
            }
        }
        newcomers.start();
        recruiting.start();
        return newcomers;
    }

    public static void stopNewcomers(Newcomers newcomers) {
        newcomers.stop();
    }

    GeoAnarchy plugin() {
        return plugin;
    }

    StartUi ui() {
        return ui;
    }

    private ConfigurationSection config() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("onboarding");
        return section != null ? section : plugin.getConfig().createSection("onboarding");
    }

    Component game(String key, String... placeholders) {
        return plugin.messages().game(key, placeholders);
    }

    // ---- who gets it ---------------------------------------------------------------------------------------------

    private int playedMinutes(Player player) {
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 1200;
    }

    /** Never chose a continent, and still in their first minutes of play. */
    boolean eligible(Player player) {
        return config().getBoolean("enabled", true)
                && !player.getPersistentDataContainer().has(chosenKey)
                && playedMinutes(player) < config().getInt("first-minutes", 30);
    }

    /** AuthMe's "logged in", read without depending on AuthMe. Without AuthMe everyone counts as logged in. */
    private boolean loggedIn(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("AuthMe")) {
            return true;
        }
        try {
            if (authMeCheck == null) {
                Class<?> api = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
                authMeApi = api.getMethod("getInstance").invoke(null);
                authMeCheck = api.getMethod("isAuthenticated", Player.class);
            }
            return (boolean) authMeCheck.invoke(authMeApi, player);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return true;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!eligible(player)) {
            return;
        }
        // wait for the login (AuthMe), then a second more so the welcome texts come first
        long[] ticks = {0};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticks[0] += 10;
            if (!player.isOnline() || ticks[0] > 20L * 60 * 10) {
                stopWaiting(player);
                return;
            }
            if (loggedIn(player)) {
                stopWaiting(player);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && eligible(player)) {
                        open(player);
                    }
                }, 20L);
            }
        }, 20L, 10L);
        BukkitTask old = waiting.put(player.getUniqueId(), task);
        if (old != null) {
            old.cancel();
        }
    }

    private void stopWaiting(Player player) {
        BukkitTask task = waiting.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    // ---- the menu ------------------------------------------------------------------------------------------------

    void open(Player player) {
        pressed.remove(player.getUniqueId());
        if (forms != null && forms.bedrock(player)) {
            forms.show(this, player);
            return;
        }
        boolean art = player.hasMetadata("geoanarchy-ui");
        player.openInventory(new StartMenu(this, player, art).getInventory());
    }

    /** How many players are on each continent right now (shown on the buttons). */
    Map<String, Integer> playersPerContinent() {
        Map<String, Integer> counts = new HashMap<>();
        World world = world();
        if (world == null) {
            return counts;
        }
        for (Player p : world.getPlayers()) {
            double lon = Continent.lon(p.getLocation().getX());
            double lat = Continent.lat(p.getLocation().getZ());
            for (Continent c : ui.continents()) {
                if (c.contains(lon, lat)) {
                    counts.merge(c.id(), 1, Integer::sum);
                    break;
                }
            }
        }
        return counts;
    }

    /** Continent id to {countries there, of which recruiting} (shown on the buttons). */
    Map<String, int[]> countriesPerContinent() {
        try {
            return recruiting == null ? Map.of() : recruiting.perContinent();
        } catch (RuntimeException | LinkageError error) {
            return Map.of();
        }
    }

    private World world() {
        return Bukkit.getWorld(config().getString("world", "earth"));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof StartMenu menu) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getInventory()) {
            return;
        }
        menu.pressed(event.getSlot(), ui.continents()).ifPresent(continent -> {
            pressed.add(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                choose(player, continent);
            });
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof StartMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof StartMenu && event.getPlayer() instanceof Player player
                && !pressed.remove(player.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> closedWithoutChoosing(player));
        }
    }

    void closedWithoutChoosing(Player player) {
        if (player.isOnline() && eligible(player) && !searching.contains(player.getUniqueId())) {
            player.sendMessage(game("start-closed", "minutes", String.valueOf(config().getInt("first-minutes", 30))));
        }
    }

    // ---- landing -------------------------------------------------------------------------------------------------

    void choose(Player player, Continent continent) {
        if (!eligible(player)) {
            player.sendMessage(game("start-already"));
            return;
        }
        if (!searching.add(player.getUniqueId())) {
            player.sendMessage(game("start-busy"));
            return;
        }
        World world = world();
        if (world == null) {
            searching.remove(player.getUniqueId());
            plugin.getLogger().warning("onboarding.world '" + config().getString("world", "earth") + "' does not exist.");
            return;
        }
        player.sendMessage(game("start-searching", "continent", continent.name()));
        landing.find(world, continent, config().getInt("tries", 40), config().getInt("distance-from-countries-chunks", 8))
                .whenComplete((spot, error) -> Bukkit.getScheduler().runTask(plugin, () -> land(player, continent, spot)));
    }

    private void land(Player player, Continent continent, Location spot) {
        if (!player.isOnline()) {
            searching.remove(player.getUniqueId());
            return;
        }
        if (spot == null) {
            searching.remove(player.getUniqueId());
            player.sendMessage(game("start-failed"));
            return;
        }
        player.teleportAsync(spot).whenComplete((ok, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            searching.remove(player.getUniqueId());
            if (!player.isOnline() || !Boolean.TRUE.equals(ok)) {
                if (player.isOnline()) {
                    player.sendMessage(game("start-failed"));
                }
                return;
            }
            player.getPersistentDataContainer().set(chosenKey, PersistentDataType.STRING, continent.id());
            // Dying right away must not send them back across the world: the landing spot is their respawn
            // point (like /spawnpoint) until they sleep in a bed, and the first seconds there are safe.
            player.setRespawnLocation(spot, true);
            player.setFallDistance(0);
            int seconds = config().getInt("protection-seconds", 15);
            if (seconds > 0) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE,
                        seconds * 20, 4, false, false, true));
            }
            welcome(player, continent);
            if (newcomers != null) {
                newcomers.landed(player, continent);
            }
        }));
    }

    private void welcome(Player player, Continent continent) {
        player.showTitle(Title.title(Component.text(continent.name(), continent.colour()),
                game("start-title-sub", "player", player.getName()),
                Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(3), Duration.ofMillis(800))));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        player.sendMessage(game("start-landed", "continent", continent.name()));
        if (config().getBoolean("guide-book", true) && !player.getPersistentDataContainer().has(bookKey)) {
            player.getPersistentDataContainer().set(bookKey, PersistentDataType.BYTE, (byte) 1);
            ItemStack book = guideBook();
            player.getInventory().addItem(book).values().forEach(left -> player.getWorld().dropItem(player.getLocation(), left));
        }
    }

    /** "Guia do GeoAnarchy": its pages are game.guide-page-1, -2... in messages.yml. */
    ItemStack guideBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(game("guide-title"));
        meta.author(game("guide-author"));
        List<Component> pages = new ArrayList<>();
        for (int page = 1; plugin.messages().has("game.guide-page-" + page); page++) {
            pages.add(game("guide-page-" + page).decoration(TextDecoration.ITALIC, false));
        }
        meta.pages(pages);
        meta.displayName(game("guide-title").decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(guideItemKey, PersistentDataType.BYTE, (byte) 1);
        book.setItemMeta(meta);
        return book;
    }

    boolean isGuideBook(ItemStack item) {
        return item != null && item.getType() == Material.WRITTEN_BOOK && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(guideItemKey);
    }

    // ---- /comecar ------------------------------------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("reiniciar") && sender.hasPermission(ADMIN)) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(game("start-not-online", "player", args[1]));
                return true;
            }
            target.getPersistentDataContainer().remove(chosenKey);
            target.getPersistentDataContainer().remove(bookKey);
            if (newcomers != null) {
                newcomers.reset(target);
            }
            sender.sendMessage(game("start-reset", "player", target.getName()));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(game("players-only"));
            return true;
        }
        if (player.getPersistentDataContainer().has(chosenKey)) {
            player.sendMessage(game("start-already"));
            return true;
        }
        if (!eligible(player)) {
            player.sendMessage(game("start-too-late"));
            return true;
        }
        open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            return List.of();
        }
        if (args.length == 1) {
            return List.of("reiniciar");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reiniciar")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
