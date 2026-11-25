package me.rauldoq.geoAnarchy.game;

import me.rauldoq.geoAnarchy.GeoAnarchy;
import me.rauldoq.geoAnarchy.config.Settings;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Coffee, a BreweryX drink. Drinking it runs "/geoanarchy cafe <player> <quality>" from the console (set in
 * BreweryX's recipes.yml), and the player gets Haste and a hunger bar that goes down slower, both for the
 * same time. How long depends on how well the coffee was made: a badly made one barely lasts, a perfect one
 * lasts the whole time (20 minutes by default). The time grows with the square of the quality, so only the
 * best coffee is worth the effort.
 * <p>
 * The slower hunger lasts while the player still has Haste and the coffee's time hasn't run out, so milk,
 * dying, or simply time ends both together. The end time is kept on the player, so it survives a relog.
 */
public final class Coffee implements Listener {

    private final GeoAnarchy plugin;
    private final NamespacedKey until;

    public Coffee(GeoAnarchy plugin) {
        this.plugin = plugin;
        this.until = new NamespacedKey(plugin, "coffee_until");
    }

    /** How long a coffee of this quality (1 to 10) lasts, in seconds. */
    public static int seconds(Settings.Coffee settings, int quality) {
        int q = Math.max(1, Math.min(10, quality));
        double share = (q - 1) / 9.0;
        return (int) Math.round(settings.worstSeconds() + (settings.bestSeconds() - settings.worstSeconds()) * share * share);
    }

    /** The player drank a coffee. Returns how many seconds it lasts. */
    public int drink(Player player, int quality) {
        Settings.Coffee settings = plugin.settings().coffee();
        int seconds = seconds(settings, quality);
        long end = System.currentTimeMillis() + seconds * 1000L;
        Long current = player.getPersistentDataContainer().get(until, PersistentDataType.LONG);
        if (current == null || current < end) {
            player.getPersistentDataContainer().set(until, PersistentDataType.LONG, end);
        }
        PotionEffect haste = player.getPotionEffect(PotionEffectType.HASTE);
        if (haste == null || haste.getAmplifier() == 0 && haste.getDuration() < seconds * 20) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, seconds * 20, 0, false, true, true));
        }
        return seconds;
    }

    /** True while the coffee still works on this player. */
    public boolean active(Player player) {
        Long end = player.getPersistentDataContainer().get(until, PersistentDataType.LONG);
        return end != null && end > System.currentTimeMillis() && player.hasPotionEffect(PotionEffectType.HASTE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExhaustion(EntityExhaustionEvent event) {
        if (event.getEntity() instanceof Player player && active(player)) {
            event.setExhaustion(event.getExhaustion() * (float) plugin.settings().coffee().hungerMultiplier());
        }
    }
}
