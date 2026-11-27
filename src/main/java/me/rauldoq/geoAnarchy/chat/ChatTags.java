package me.rauldoq.geoAnarchy.chat;

import me.rauldoq.geoAnarchy.country.Countries;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The country shown before a player's name in chat: "[Portugal] " in the country's own colors, or nothing for a
 * player without a country. Lands is asked on the server thread every few seconds and when a player joins;
 * chat plugins read the answer from any thread, so a message never waits on Lands.
 */
public final class ChatTags implements Listener {

    /** Country names without colors of their own get this one: the dark green the tab list uses. */
    static final String DEFAULT_COLOR = "&2";

    private final Plugin plugin;
    private final Supplier<Countries> countries;
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    public ChatTags(Plugin plugin, Supplier<Countries> countries) {
        this.plugin = plugin;
        this.countries = countries;
    }

    /** Starts refreshing every five seconds. Call once, from onEnable. */
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 20L, 100L);
    }

    /** The tag before a name: "&f[&2Portugal&f] ", or "" without a country. */
    public String tag(UUID player) {
        return tagOf(names.get(player));
    }

    /** The country's name in its colors, or "". */
    public String name(UUID player) {
        String name = names.get(player);
        return name == null ? "" : name;
    }

    static String tagOf(String coloredName) {
        return coloredName == null || coloredName.isBlank() ? "" : "&f[" + coloredName + "&f] ";
    }

    /** A Lands name as chat shows it: "&" codes, and dark green when it has no color of its own. */
    static String display(String landName) {
        String legacy = LegacyColors.toLegacy(landName).strip();
        if (legacy.isEmpty()) {
            return "";
        }
        return LegacyColors.hasColor(legacy) ? legacy : DEFAULT_COLOR + legacy;
    }

    private void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    private void refresh(Player player) {
        try {
            countries.get().coloredName(player.getUniqueId())
                    .map(ChatTags::display)
                    .filter(name -> !name.isEmpty())
                    .ifPresentOrElse(name -> names.put(player.getUniqueId(), name), () -> names.remove(player.getUniqueId()));
        } catch (RuntimeException | LinkageError lands) {
            names.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> refresh(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        names.remove(event.getPlayer().getUniqueId());
    }
}
