package me.rauldoq.geoAnarchy.onboarding;

import me.rauldoq.geoAnarchy.GeoAnarchy;
import me.rauldoq.geoAnarchy.country.Countries;
import me.rauldoq.geoAnarchy.country.Country;
import me.rauldoq.geoAnarchy.storage.Database;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recruiting, the unofficial way: leaders put their country on a list (/recrutar ligar), anyone can look at it
 * (/recrutando) and ask to join, and the leaders get a chat message with buttons to answer or to send Lands' own
 * invite. Nothing here makes anyone a citizen: the leader still decides and invites through Lands. Leaders of
 * recruiting countries also hear when a new player lands.
 * <p>
 * "Leaders" are a country's founder and its citizens with one of the roles in recruiting.leader-roles (Admin).
 */
public final class Recruiting implements Listener, TabExecutor {

    private final GeoAnarchy plugin;
    private final StartUi ui;
    private Newcomers newcomers;
    /** Players whose join requests were already cleared after they joined a country. */
    private final Set<UUID> cleared = ConcurrentHashMap.newKeySet();

    Recruiting(GeoAnarchy plugin, StartUi ui) {
        this.plugin = plugin;
        this.ui = ui;
    }

    void setNewcomers(Newcomers newcomers) {
        this.newcomers = newcomers;
    }

    void start() {
        // old requests go away by themselves
        Bukkit.getScheduler().runTaskTimer(plugin, () -> plugin.database().forgetRequests(System.currentTimeMillis() - requestMillis()),
                20L * 60, 20L * 600);
    }

    private ConfigurationSection config() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("recruiting");
        return section != null ? section : plugin.getConfig().createSection("recruiting");
    }

    private List<String> leaderRoles() {
        return config().isList("leader-roles") ? config().getStringList("leader-roles") : List.of("Admin");
    }

    private long requestMillis() {
        return Math.max(1, config().getLong("request-days", 3)) * 86_400_000L;
    }

    private Countries countries() {
        return plugin.countries();
    }

    private Database database() {
        return plugin.database();
    }

    Component game(String key, String... placeholders) {
        return plugin.messages().game(key, placeholders);
    }

    private World world() {
        return Bukkit.getWorld(plugin.getConfig().getString("onboarding.world", "earth"));
    }

    Optional<Continent> continentAt(double x, double z) {
        double lon = Continent.lon(x);
        double lat = Continent.lat(z);
        for (Continent continent : ui.continents()) {
            if (continent.contains(lon, lat)) {
                return Optional.of(continent);
            }
        }
        return Optional.empty();
    }

    private Optional<Continent> continentOf(String countryId, World world) {
        return world == null ? Optional.empty() : countries().home(countryId, world).flatMap(home -> continentAt(home[0], home[1]));
    }

    private boolean alertsOff(UUID leader) {
        return database().setting(leader, "recruit-alerts").map("off"::equals).orElse(false);
    }

    // ---- what the list shows -------------------------------------------------------------------------------------

    /** A country on the list, as the menu shows it. */
    record Entry(Country country, String message, String level, int citizens, Continent continent, int leadersOnline) {
    }

    /** The recruiting countries, the viewer's own continent first, then those with a leader online, then the biggest. */
    List<Entry> entries(Player viewer) {
        World world = world();
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<String, String> listed : database().recruiting().entrySet()) {
            String id = listed.getKey();
            Optional<Countries.Status> status = countries().status(id);
            if (status.isEmpty()) {
                database().stopRecruiting(id);   // the country no longer exists
                continue;
            }
            int online = (int) countries().leaders(id, leaderRoles()).stream().map(Bukkit::getPlayer).filter(Objects::nonNull).count();
            out.add(new Entry(status.get().country(), listed.getValue(), status.get().level(), status.get().citizens(),
                    continentOf(id, world).orElse(null), online));
        }
        String mine = continentAt(viewer.getLocation().getX(), viewer.getLocation().getZ()).map(Continent::id).orElse("");
        out.sort(Comparator.<Entry, Boolean>comparing(e -> e.continent() == null || !e.continent().id().equals(mine))
                .thenComparing(e -> e.leadersOnline() == 0)
                .thenComparing(Entry::citizens, Comparator.reverseOrder())
                .thenComparing(e -> e.country().name(), String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    /** For the continent selector: continent id to {countries there, of which recruiting}. */
    Map<String, int[]> perContinent() {
        Map<String, int[]> out = new HashMap<>();
        World world = world();
        if (world == null) {
            return out;
        }
        Set<String> listed = database().recruiting().keySet();
        for (Country country : countries().all()) {
            continentOf(country.id(), world).ifPresent(continent -> {
                int[] counts = out.computeIfAbsent(continent.id(), k -> new int[2]);
                counts[0]++;
                if (listed.contains(country.id())) {
                    counts[1]++;
                }
            });
        }
        return out;
    }

    // ---- chat buttons --------------------------------------------------------------------------------------------

    /** [Responder] [Convidar], and [Dispensar] for a request. Each one only fills in a command; nothing is automatic. */
    private Component buttons(String name, boolean dismiss) {
        Component reply = game("button-reply").clickEvent(ClickEvent.suggestCommand("/tell " + name + " "))
                .hoverEvent(HoverEvent.showText(game("button-reply-hover", "player", name)));
        Component invite = game("button-invite").clickEvent(ClickEvent.suggestCommand("/lands trust " + name))
                .hoverEvent(HoverEvent.showText(game("button-invite-hover", "player", name)));
        Component out = Component.text(" ").append(reply).append(Component.text(" ")).append(invite);
        if (dismiss) {
            out = out.append(Component.text(" ")).append(game("button-dismiss")
                    .clickEvent(ClickEvent.runCommand("/recrutar dispensar " + name))
                    .hoverEvent(HoverEvent.showText(game("button-dismiss-hover", "player", name))));
        }
        return out;
    }

    private String ago(long millis) {
        long minutes = millis / 60_000;
        if (minutes < 1) {
            return Newcomers.plain(game("ago-now"));
        }
        if (minutes < 60) {
            return Newcomers.plain(game("ago-minutes", "n", String.valueOf(minutes)));
        }
        if (minutes < 48 * 60) {
            return Newcomers.plain(game("ago-hours", "n", String.valueOf(minutes / 60)));
        }
        return Newcomers.plain(game("ago-days", "n", String.valueOf(minutes / 1440)));
    }

    // ---- new players and requests --------------------------------------------------------------------------------

    /** A new player just landed: every leader online of a recruiting country hears it, once. */
    void announceNewPlayer(Player newcomer, Continent continent) {
        Set<UUID> told = new HashSet<>();
        for (String id : database().recruiting().keySet()) {
            for (UUID leader : countries().leaders(id, leaderRoles())) {
                if (leader.equals(newcomer.getUniqueId()) || !told.add(leader) || alertsOff(leader)) {
                    continue;
                }
                Player online = Bukkit.getPlayer(leader);
                if (online != null) {
                    online.sendMessage(game("recruit-new-player", "player", newcomer.getName(), "continent", continent.name())
                            .append(buttons(newcomer.getName(), false)));
                    online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.4f);
                }
            }
        }
    }

    /** "I want to join": saved, and sent at once to the leaders online. */
    void request(Player player, String countryId) {
        Optional<Countries.Status> status = countries().status(countryId);
        if (status.isEmpty()) {
            player.sendMessage(game("recruit-gone"));
            return;
        }
        Country country = status.get().country();
        if (country.members().contains(player.getUniqueId())) {
            player.sendMessage(game("recruit-member", "country", country.name()));
            return;
        }
        long now = System.currentTimeMillis();
        long hours = Math.max(1, config().getLong("request-cooldown-hours", 24));
        boolean recent = database().requestsBy(player.getUniqueId(), now - hours * 3_600_000L).stream()
                .anyMatch(r -> r.country().equals(countryId));
        if (recent) {
            player.sendMessage(game("recruit-wait", "country", country.name(), "hours", String.valueOf(hours)));
            return;
        }
        database().addRequest(player.getUniqueId(), player.getName(), countryId, now);
        cleared.remove(player.getUniqueId());
        String where = continentAt(player.getLocation().getX(), player.getLocation().getZ()).map(Continent::name)
                .orElseGet(() -> Newcomers.plain(game("recruit-somewhere")));
        int told = 0;
        for (UUID leader : countries().leaders(countryId, leaderRoles())) {
            Player online = Bukkit.getPlayer(leader);
            if (online != null && !online.equals(player)) {
                online.sendMessage(game("recruit-request", "player", player.getName(), "country", country.name(), "continent", where)
                        .append(buttons(player.getName(), true)));
                online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.4f);
                told++;
            }
        }
        if (told > 0) {
            player.sendMessage(game("recruit-sent", "country", country.name(), "online", String.valueOf(told)));
        } else {
            player.sendMessage(game("recruit-sent-offline", "country", country.name(),
                    "days", String.valueOf(requestMillis() / 86_400_000L)));
        }
    }

    /** A player who joined a country needs no more introductions. */
    void joined(UUID player) {
        if (cleared.add(player)) {
            database().deleteRequestsBy(player);
        }
    }

    /** The requests waiting for this leader's countries, at login or with /recrutar pedidos. */
    void showPending(Player leader, boolean evenIfNone) {
        long now = System.currentTimeMillis();
        boolean any = false;
        for (Country country : countries().led(leader.getUniqueId(), leaderRoles())) {
            List<Database.JoinRequest> waiting = database().requests(country.id(), now - requestMillis()).stream()
                    .filter(r -> !country.members().contains(r.player())).toList();
            if (waiting.isEmpty()) {
                continue;
            }
            any = true;
            leader.sendMessage(game("recruit-pending-head", "country", country.name(), "count", String.valueOf(waiting.size())));
            for (Database.JoinRequest r : waiting.subList(Math.max(0, waiting.size() - 5), waiting.size())) {
                leader.sendMessage(game("recruit-pending-line", "player", r.name(), "when", ago(now - r.at()))
                        .append(buttons(r.name(), true)));
            }
        }
        if (!any && evenIfNone) {
            leader.sendMessage(game("recruit-no-requests"));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // after the login and the welcome texts
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                showPending(player, false);
            }
        }, 240L);
    }

    // ---- the menu ------------------------------------------------------------------------------------------------

    void open(Player player) {
        player.openInventory(new RecruitMenu(this, ui, player, entries(player)).getInventory());
        if (newcomers != null) {
            newcomers.complete(player, Step.RECRUTANDO);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof RecruitMenu menu) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getInventory()) {
            return;
        }
        String countryId = menu.countryAt(event.getSlot());
        if (countryId != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                request(player, countryId);
            });
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof RecruitMenu) {
            event.setCancelled(true);
        }
    }

    // ---- /recrutar and /recrutando -------------------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(game("players-only"));
            return true;
        }
        if (command.getName().equalsIgnoreCase("recrutando")) {
            open(player);
            return true;
        }
        List<Country> led = countries().led(player.getUniqueId(), leaderRoles());
        if (led.isEmpty()) {
            player.sendMessage(game("recruit-not-leader"));
            return true;
        }
        Country country = led.get(0);
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "ligar" -> {
                String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).strip();
                if (message.isEmpty()) {
                    message = Newcomers.plain(game("recruit-default-message"));
                }
                int max = Math.max(10, config().getInt("message-max", 80));
                if (message.length() > max) {
                    message = message.substring(0, max).strip();
                }
                database().setRecruiting(country.id(), message, player.getUniqueId(), System.currentTimeMillis());
                player.sendMessage(game("recruit-on", "country", country.name(), "message", message));
            }
            case "desligar" -> {
                database().stopRecruiting(country.id());
                player.sendMessage(game("recruit-off", "country", country.name()));
            }
            case "avisos" -> {
                boolean off = !alertsOff(player.getUniqueId());
                database().setSetting(player.getUniqueId(), "recruit-alerts", off ? "off" : null);
                player.sendMessage(game(off ? "recruit-alerts-off" : "recruit-alerts-on"));
            }
            case "pedidos" -> showPending(player, true);
            case "dispensar" -> {
                if (args.length < 2) {
                    player.sendMessage(game("recruit-usage"));
                    return true;
                }
                boolean found = false;
                for (Country mine : led) {
                    for (Database.JoinRequest r : database().requests(mine.id(), 0)) {
                        if (r.name().equalsIgnoreCase(args[1])) {
                            database().deleteRequest(mine.id(), r.player());
                            found = true;
                        }
                    }
                }
                player.sendMessage(found ? game("recruit-dismissed", "player", args[1]) : game("recruit-no-such-request", "player", args[1]));
            }
            default -> {
                String message = database().recruiting().get(country.id());
                player.sendMessage(message != null ? game("recruit-status-on", "country", country.name(), "message", message)
                        : game("recruit-status-off", "country", country.name()));
                player.sendMessage(game("recruit-usage"));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("recrutar") && args.length == 1) {
            return List.of("ligar", "desligar", "avisos", "pedidos", "dispensar");
        }
        return List.of();
    }
}
