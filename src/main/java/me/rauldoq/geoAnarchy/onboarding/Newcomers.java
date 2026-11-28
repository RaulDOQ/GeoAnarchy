package me.rauldoq.geoAnarchy.onboarding;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import me.rauldoq.geoAnarchy.GeoAnarchy;
import me.rauldoq.geoAnarchy.country.Country;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A new player's first hours, from the moment they land with the continent selector:
 * <ul>
 *   <li>the "Primeiros passos" ({@link Step}), each paid once in Geos, shown in the sidebar and in /passos;</li>
 *   <li>protection from other players for their first minutes of play ({@link Protection});</li>
 *   <li>one-time tips at the right moment ({@link Tip}, triggered here and in {@link Tips});</li>
 *   <li>/guia, which opens the guide book at any time.</li>
 * </ul>
 * Only players who landed through the selector are new players here, so nobody who played before gets steps or
 * rewards. What a player has done is kept in their own player data, so it survives restarts without a table.
 */
public final class Newcomers implements Listener, TabExecutor {

    /** Player metadata GeoRoster reads to draw the sidebar: rows of "kind TAB label TAB value". */
    public static final String SIDEBAR = "geoanarchy-newcomer";
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final GeoAnarchy plugin;
    private final Onboarding onboarding;
    private final Recruiting recruiting;
    /** When they landed (epoch millis): the mark of a new player. */
    private final NamespacedKey newKey;
    /** Their play time (in ticks) when they landed. */
    private final NamespacedKey landedPlayKey;
    /** Seconds of protection left. */
    private final NamespacedKey shieldKey;
    private final NamespacedKey stepsKey;
    private final NamespacedKey tipsKey;
    private final NamespacedKey tipsOffKey;
    private final NamespacedKey hiddenKey;

    /** Seconds of protection left, for the players online; written to their data every few seconds. */
    private final Map<UUID, Integer> shield = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTip = new ConcurrentHashMap<>();
    /** Seen alive at night: the world's full time when that night ends. Dying or leaving starts over. */
    private final Map<UUID, Long> nightEnds = new ConcurrentHashMap<>();
    /** The country each player stood in at the last look ("" for none), to notice walking into one. */
    private final Map<UUID, String> standingIn = new ConcurrentHashMap<>();
    private final Set<UUID> checkingMarket = ConcurrentHashMap.newKeySet();
    private long seconds;

    Newcomers(GeoAnarchy plugin, Onboarding onboarding, Recruiting recruiting) {
        this.plugin = plugin;
        this.onboarding = onboarding;
        this.recruiting = recruiting;
        this.newKey = new NamespacedKey(plugin, "newcomer");
        this.landedPlayKey = new NamespacedKey(plugin, "landed_play");
        this.shieldKey = new NamespacedKey(plugin, "shield");
        this.stepsKey = new NamespacedKey(plugin, "steps");
        this.tipsKey = new NamespacedKey(plugin, "tips");
        this.tipsOffKey = new NamespacedKey(plugin, "tips_off");
        this.hiddenKey = new NamespacedKey(plugin, "steps_hidden");
    }

    /** Starts the once-a-second clock. Call once, after registering the listeners. */
    void start() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadShield(player);
            refresh(player);
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    /** Before the plugin stops: the protection left is written into each player's data. */
    void stop() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            saveShield(player);
            player.removeMetadata(SIDEBAR, plugin);
        }
    }

    private ConfigurationSection config() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("newcomers");
        return section != null ? section : plugin.getConfig().createSection("newcomers");
    }

    Component game(String key, String... placeholders) {
        return onboarding.game(key, placeholders);
    }

    static String plain(Component text) {
        return PlainTextComponentSerializer.plainText().serialize(text);
    }

    Onboarding onboarding() {
        return onboarding;
    }

    // ---- who is new ----------------------------------------------------------------------------------------------

    boolean isNew(Player player) {
        return player.getPersistentDataContainer().has(newKey, PersistentDataType.LONG);
    }

    /** Called by the selector once the player stands on their landing spot. */
    void landed(Player player, Continent continent) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(newKey, PersistentDataType.LONG, System.currentTimeMillis());
        data.set(landedPlayKey, PersistentDataType.INTEGER, player.getStatistic(Statistic.PLAY_ONE_MINUTE));
        int protection = Math.max(0, config().getInt("protection-minutes", 20)) * 60;
        shield.put(player.getUniqueId(), protection);
        saveShield(player);
        if (protection > 0) {
            player.sendMessage(game("protection-start", "minutes", String.valueOf(protection / 60)));
        }
        recruiting.announceNewPlayer(player, continent);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage(game("steps-intro", "geos", whole(totalCents())));
                refresh(player);
            }
        }, 100L);
    }

    /** /comecar reiniciar: back to a player who never arrived (for tests). */
    void reset(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        for (NamespacedKey key : List.of(newKey, landedPlayKey, shieldKey, stepsKey, tipsKey, tipsOffKey, hiddenKey)) {
            data.remove(key);
        }
        UUID id = player.getUniqueId();
        shield.remove(id);
        nightEnds.remove(id);
        lastTip.remove(id);
        standingIn.remove(id);
        player.removeMetadata(SIDEBAR, plugin);
    }

    private int playedTicksSinceLanding(Player player) {
        Integer at = player.getPersistentDataContainer().get(landedPlayKey, PersistentDataType.INTEGER);
        return Math.max(0, player.getStatistic(Statistic.PLAY_ONE_MINUTE) - (at == null ? 0 : at));
    }

    // ---- steps ---------------------------------------------------------------------------------------------------

    long cents(Step step) {
        return Math.max(0, config().getLong("steps." + step.id, step.defaultGeos)) * 100;
    }

    long totalCents() {
        long total = 0;
        for (Step step : Step.values()) {
            total += cents(step);
        }
        return total;
    }

    /** 1.000 (whole Geos, Brazilian style). */
    static String whole(long cents) {
        return NumberFormat.getIntegerInstance(PT_BR).format(cents / 100);
    }

    private Set<String> list(Player player, NamespacedKey key) {
        String raw = player.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        Set<String> out = new LinkedHashSet<>();
        if (raw != null && !raw.isBlank()) {
            out.addAll(Arrays.asList(raw.split(",")));
        }
        return out;
    }

    private void setList(Player player, NamespacedKey key, Set<String> values) {
        player.getPersistentDataContainer().set(key, PersistentDataType.STRING, String.join(",", values));
    }

    boolean done(Player player, Step step) {
        return list(player, stepsKey).contains(step.id);
    }

    int doneCount(Player player) {
        int count = 0;
        for (Step step : Step.values()) {
            if (done(player, step)) {
                count++;
            }
        }
        return count;
    }

    String stepName(Step step) {
        return plain(game("step-" + step.id + "-name"));
    }

    /** Marks a step done and pays it, once. Does nothing for players who are not new or did it already. */
    void complete(Player player, Step step) {
        if (!isNew(player) || done(player, step)) {
            return;
        }
        Set<String> finished = list(player, stepsKey);
        finished.add(step.id);
        setList(player, stepsKey, finished);
        long cents = cents(step);
        if (cents > 0) {
            // paid now, or kept in geoanarchy.db and paid as soon as the economy can save
            plugin.owedRefunds().refund(player.getUniqueId(), cents, "Primeiros passos: " + step.id);
        }
        player.sendMessage(game("step-done", "step", stepName(step), "geos", whole(cents)));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
        if (doneCount(player) == Step.values().length) {
            player.sendMessage(game("steps-all-done", "geos", whole(totalCents())));
        }
        refresh(player);
    }

    boolean hidden(Player player) {
        return player.getPersistentDataContainer().has(hiddenKey);
    }

    void setHidden(Player player, boolean hide) {
        if (hide) {
            player.getPersistentDataContainer().set(hiddenKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            player.getPersistentDataContainer().remove(hiddenKey);
        }
        refresh(player);
    }

    // ---- protection ----------------------------------------------------------------------------------------------

    private void loadShield(Player player) {
        Integer left = player.getPersistentDataContainer().get(shieldKey, PersistentDataType.INTEGER);
        if (left != null && left > 0) {
            shield.put(player.getUniqueId(), left);
        }
    }

    private void saveShield(Player player) {
        Integer left = shield.get(player.getUniqueId());
        if (left != null) {
            player.getPersistentDataContainer().set(shieldKey, PersistentDataType.INTEGER, Math.max(0, left));
        }
    }

    /** Seconds of protection left (0 when none). */
    int shieldLeft(Player player) {
        return shield.getOrDefault(player.getUniqueId(), 0);
    }

    boolean shielded(Player player) {
        return shieldLeft(player) > 0;
    }

    void endShield(Player player, String messageKey) {
        if (shieldLeft(player) <= 0) {
            return;
        }
        shield.put(player.getUniqueId(), 0);
        saveShield(player);
        player.sendMessage(game(messageKey));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.2f);
        refresh(player);
    }

    /** "14 min", or "40 s" in the last minute. */
    static String timeText(int seconds) {
        return seconds >= 60 ? ((seconds + 59) / 60) + " min" : seconds + " s";
    }

    // ---- tips ----------------------------------------------------------------------------------------------------

    boolean tipsOff(Player player) {
        return player.getPersistentDataContainer().has(tipsOffKey);
    }

    /**
     * Shows a tip if this new player has never seen it and no other tip came in the last tip-gap-seconds.
     * A tip that has to wait is shown the next time its moment comes.
     */
    boolean tip(Player player, Tip tip, String... placeholders) {
        if (!isNew(player) || tipsOff(player)) {
            return false;
        }
        Set<String> seen = list(player, tipsKey);
        if (seen.contains(tip.id)) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastTip.getOrDefault(player.getUniqueId(), 0L) < config().getLong("tip-gap-seconds", 90) * 1000) {
            return false;
        }
        seen.add(tip.id);
        setList(player, tipsKey, seen);
        lastTip.put(player.getUniqueId(), now);
        player.sendMessage(game("tip-" + tip.id, placeholders));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
        return true;
    }

    // ---- the sidebar ---------------------------------------------------------------------------------------------

    /**
     * Rows for GeoRoster's sidebar while the steps are still worth showing (row 2 stays empty: the panel draws its
     * divider there). Empty when the player is not new, hid the steps, finished them, or has played long enough.
     */
    List<String> sidebarRows(Player player) {
        if (!isNew(player) || hidden(player) || doneCount(player) == Step.values().length
                || playedTicksSinceLanding(player) >= config().getDouble("steps-sidebar-hours", 5) * 72000) {
            return List.of();
        }
        List<String> rows = new ArrayList<>();
        int left = shieldLeft(player);
        if (left > 0) {
            rows.add(row("shield", plain(game("side-protection")), timeText(left)));
        } else {
            long missing = 0;
            for (Step step : Step.values()) {
                if (!done(player, step)) {
                    missing += cents(step);
                }
            }
            rows.add(row("hint", plain(game("side-reward", "geos", whole(missing))), ""));
        }
        rows.add(row("head", plain(game("side-steps")), doneCount(player) + "/" + Step.values().length));
        rows.add(row("gap", "", ""));
        List<Step> order = new ArrayList<>();
        for (Step step : Step.values()) {
            if (!done(player, step)) {
                order.add(step);
            }
        }
        for (Step step : Step.values()) {
            if (done(player, step)) {
                order.add(step);
            }
        }
        for (int i = 0; i < 3 && i < order.size(); i++) {
            Step step = order.get(i);
            rows.add(row(done(player, step) ? "done" : "todo", stepName(step), "+" + whole(cents(step))));
        }
        return rows;
    }

    private static String row(String kind, String label, String value) {
        return kind + "\t" + label.replace('\t', ' ') + "\t" + value.replace('\t', ' ');
    }

    void refresh(Player player) {
        List<String> rows = sidebarRows(player);
        if (rows.isEmpty()) {
            if (player.hasMetadata(SIDEBAR)) {
                player.removeMetadata(SIDEBAR, plugin);
            }
        } else {
            player.setMetadata(SIDEBAR, new FixedMetadataValue(plugin, List.copyOf(rows)));
        }
    }

    // ---- the clock -----------------------------------------------------------------------------------------------

    private void tick() {
        seconds++;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isNew(player)) {
                continue;
            }
            UUID id = player.getUniqueId();
            int left = shield.getOrDefault(id, 0);
            if (left > 0) {
                left--;
                shield.put(id, left);
                if (left == 300 || left == 60) {
                    player.sendMessage(game("protection-warning", "time", timeText(left)));
                }
                if (left == 0) {
                    saveShield(player);
                    player.sendMessage(game("protection-ended"));
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.2f);
                } else if (left % 15 == 0) {
                    saveShield(player);
                }
            }
            if (seconds % 5 == 0) {
                slowChecks(player);
            }
        }
    }

    /** Every five seconds, for each new player online. */
    private void slowChecks(Player player) {
        UUID id = player.getUniqueId();
        boolean citizen = !plugin.countries().of(id).isEmpty();
        if (citizen) {
            complete(player, Step.PAIS);
            endShield(player, "protection-ended-country");
            recruiting.joined(id);
        }

        World world = player.getWorld();
        if (world.getEnvironment() == World.Environment.NORMAL) {
            long time = world.getTime();
            boolean night = time >= 13000 && time < 23000;
            Long end = nightEnds.get(id);
            if (end != null && world.getFullTime() >= end) {
                nightEnds.remove(id);
                complete(player, Step.NOITE);
            } else if (night && end == null && !player.isDead() && !done(player, Step.NOITE)) {
                nightEnds.put(id, world.getFullTime() - time + 23000);
            }
            if (night) {
                tip(player, Tip.NOITE);
            }
        }

        Optional<Country> here = plugin.countries().at(player.getLocation());
        String hereId = here.map(Country::id).orElse("");
        String before = standingIn.put(id, hereId);
        if (!hereId.isEmpty() && !hereId.equals(before) && !here.get().members().contains(id)) {
            tip(player, Tip.PAIS, "country", here.get().name());
        }
        if (!citizen && playedTicksSinceLanding(player) >= 30 * 1200) {
            tip(player, Tip.RECRUTAR);
        }
        if (seconds % 30 == 0) {
            checkMarket(player);
        }
        refresh(player);
    }

    /** Asks the economy, in the background, whether this new player has sold anything to a market yet. */
    private void checkMarket(Player player) {
        UUID id = player.getUniqueId();
        if (!isNew(player) || done(player, Step.MERCADO) || !plugin.economy().available() || !checkingMarket.add(id)) {
            return;
        }
        plugin.economy().soldAtMarket(id).whenComplete((sold, error) -> {
            if (!plugin.isEnabled()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                checkingMarket.remove(id);
                Player online = Bukkit.getPlayer(id);
                if (online != null && Boolean.TRUE.equals(sold)) {
                    complete(online, Step.MERCADO);
                }
            });
        });
    }

    // ---- events --------------------------------------------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isNew(player)) {
            loadShield(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    refresh(player);
                }
            }, 40L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (isNew(player)) {
            saveShield(player);
        }
        shield.remove(id);
        nightEnds.remove(id);
        standingIn.remove(id);
        checkingMarket.remove(id);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        nightEnds.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBed(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            complete(event.getPlayer(), Step.CAMA);
        }
    }

    /** Using a bed by day also moves where you respawn, which is what the step teaches. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawnSet(PlayerSetSpawnEvent event) {
        if (event.getCause() == PlayerSetSpawnEvent.Cause.BED) {
            complete(event.getPlayer(), Step.CAMA);
        }
    }

    /** Opening the guide book from the inventory counts as reading it. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && onboarding.isGuideBook(event.getItem())) {
            complete(event.getPlayer(), Step.GUIA);
        }
    }

    /** Closing a menu may mean a sale at a market just happened. */
    @EventHandler
    public void onAnyClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player && isNew(player) && !done(player, Step.MERCADO)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    checkMarket(player);
                }
            }, 10L);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof StepsMenu menu) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == event.getInventory() && event.getSlot() == StepsMenu.TOGGLE_SLOT && isNew(player)) {
            setHidden(player, !hidden(player));
            player.sendMessage(game(hidden(player) ? "steps-hidden" : "steps-shown"));
            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(new StepsMenu(this, player).getInventory()));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof StepsMenu) {
            event.setCancelled(true);
        }
    }

    // ---- /guia, /passos, /dicas, /protecao -----------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(game("players-only"));
            return true;
        }
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "guia" -> {
                ItemStack book = onboarding.guideBook();
                player.openBook(book);
                complete(player, Step.GUIA);
            }
            case "passos" -> {
                if (!isNew(player)) {
                    player.sendMessage(game("steps-not-new"));
                } else if (args.length > 0 && args[0].equalsIgnoreCase("esconder")) {
                    setHidden(player, true);
                    player.sendMessage(game("steps-hidden"));
                } else if (args.length > 0 && args[0].equalsIgnoreCase("mostrar")) {
                    setHidden(player, false);
                    player.sendMessage(game("steps-shown"));
                } else {
                    player.openInventory(new StepsMenu(this, player).getInventory());
                }
            }
            case "dicas" -> {
                if (!isNew(player)) {
                    player.sendMessage(game("tips-not-new"));
                } else if (tipsOff(player)) {
                    player.getPersistentDataContainer().remove(tipsOffKey);
                    player.sendMessage(game("tips-on"));
                } else {
                    player.getPersistentDataContainer().set(tipsOffKey, PersistentDataType.BYTE, (byte) 1);
                    player.sendMessage(game("tips-off"));
                }
            }
            case "protecao" -> {
                int left = shieldLeft(player);
                if (left <= 0) {
                    player.sendMessage(game("protection-none"));
                } else if (args.length > 0 && args[0].equalsIgnoreCase("desligar")) {
                    if (args.length > 1 && args[1].equalsIgnoreCase("confirmar")) {
                        endShield(player, "protection-turned-off");
                    } else {
                        player.sendMessage(game("protection-confirm"));
                    }
                } else {
                    player.sendMessage(game("protection-left", "time", timeText(left)));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (args.length == 1 && name.equals("passos")) {
            return List.of("esconder", "mostrar");
        }
        if (args.length == 1 && name.equals("protecao")) {
            return List.of("desligar");
        }
        return List.of();
    }
}
