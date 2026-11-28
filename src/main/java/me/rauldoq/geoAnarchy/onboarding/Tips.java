package me.rauldoq.geoAnarchy.onboarding;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Set;

/**
 * The tips that come from something the player did: dying, asking for a teleport, mining ore, chatting. The
 * night, walking into a country and the half hour without a country are noticed by {@link Newcomers}' clock.
 */
final class Tips implements Listener {

    /** Commands that move a player somewhere on other servers; none of them works here. */
    private static final Set<String> TELEPORTS = Set.of("tpa", "tpahere", "tpaccept", "tpask", "tpo", "tp", "tphere",
            "home", "homes", "sethome", "spawn", "warp", "warps", "rtp", "wild", "randomtp", "back", "etpa", "ehome",
            "espawn", "ewarp", "eback");
    private static final Set<Material> ORES = Set.of(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE);

    private final Plugin plugin;
    private final Newcomers newcomers;

    Tips(Plugin plugin, Newcomers newcomers) {
        this.plugin = plugin;
        this.newcomers = newcomers;
    }

    private void later(Player player, Tip tip, long ticks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                newcomers.tip(player, tip);
            }
        }, ticks);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        later(event.getPlayer(), Tip.MORTE, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String typed = event.getMessage();
        int space = typed.indexOf(' ');
        String root = (space < 0 ? typed.substring(1) : typed.substring(1, space)).toLowerCase(Locale.ROOT);
        if (root.contains(":")) {
            root = root.substring(root.indexOf(':') + 1);
        }
        if (TELEPORTS.contains(root)) {
            later(event.getPlayer(), Tip.TELEPORTE, 2L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (ORES.contains(event.getBlock().getType())) {
            later(event.getPlayer(), Tip.MINERIO, 10L);
        }
    }

    /** The chat plugin may cancel the event and send the line itself, so cancelled lines count too. */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.isEnabled()) {
            later(player, Tip.CHAT, 20L);
        }
    }
}
