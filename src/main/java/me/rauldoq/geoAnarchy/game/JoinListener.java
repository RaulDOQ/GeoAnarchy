package me.rauldoq.geoAnarchy.game;

import me.rauldoq.geoAnarchy.GeoAnarchy;
import me.rauldoq.geoAnarchy.config.Messages;
import me.rauldoq.geoAnarchy.country.Country;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

/**
 * A few seconds after a player joins, tells them what concerns their Discord link:
 * what they did through Discord while away, that the bot can't reach them, or that linking is worth it.
 */
public final class JoinListener implements Listener {

    /** Long enough for the login plugin to finish and for the join messages to scroll by. */
    private static final long DELAY_TICKS = 160L;

    private final GeoAnarchy plugin;

    public JoinListener(GeoAnarchy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        plugin.links().rename(id, event.getPlayer().getName());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                greet(player);
            }
        }, DELAY_TICKS);
    }

    private void greet(Player player) {
        Messages texts = plugin.messages();
        UUID id = player.getUniqueId();
        List<String> done = plugin.database().takeUnseenLog(id);
        if (!done.isEmpty()) {
            texts.send(player, "login-log-header");
            for (String line : done) {
                texts.send(player, "login-log-line", "text", line);
            }
        }
        if (plugin.links().byMinecraft(id).isPresent()) {
            if (plugin.prefs().dmClosed(id)) {
                texts.send(player, "dm-closed");
            }
            return;
        }
        // Not linked: the reminder only goes to founders, who have the most to lose, and only while the bot
        // is online, since linking can't be finished without it.
        if (!plugin.bot().online()) {
            return;
        }
        for (Country country : plugin.countries().of(id)) {
            if (id.equals(country.founder())) {
                texts.send(player, "link-tip");
                return;
            }
        }
    }
}
