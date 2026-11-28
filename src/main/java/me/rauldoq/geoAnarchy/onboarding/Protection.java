package me.rauldoq.geoAnarchy.onboarding;

import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * A new player's first minutes without player fights: other players can't hurt them, and they can't hurt other
 * players either, so a fresh account can't be used as a fighter nobody can kill. Mobs, falls, lava and the rest
 * still hurt. Lava or fire a player placed can't be traced back to them, so those still hurt too.
 * <p>
 * The damage is cancelled late (HIGHEST), after Lands has had its say, so CombatLogX never sees a hit.
 */
final class Protection implements Listener {

    private final Newcomers newcomers;

    Protection(Newcomers newcomers) {
        this.newcomers = newcomers;
    }

    /** The player behind a hit: the one who struck, shot, lit the TNT, threw the potion, or owns the wolf. */
    static Player culprit(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            return shooter(projectile.getShooter());
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
            return player;
        }
        if (damager instanceof AreaEffectCloud cloud) {
            return shooter(cloud.getSource());
        }
        if (damager instanceof Tameable pet && pet.isTamed()) {
            AnimalTamer owner = pet.getOwner();
            return owner instanceof Player player ? player : null;
        }
        return null;
    }

    private static Player shooter(ProjectileSource source) {
        return source instanceof Player player ? player : null;
    }

    /** True (and the hit is stopped) when either side of a player-on-player hit is protected. */
    private boolean blocked(Player attacker, Entity victim) {
        if (attacker == null || !(victim instanceof Player target) || attacker.equals(target)) {
            return false;
        }
        if (newcomers.shielded(target)) {
            attacker.sendActionBar(newcomers.game("protection-target", "player", target.getName(),
                    "time", Newcomers.timeText(newcomers.shieldLeft(target))));
            return true;
        }
        if (newcomers.shielded(attacker)) {
            attacker.sendActionBar(newcomers.game("protection-self"));
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = culprit(event.getDamager());
        if (attacker == null) {
            try {
                // Paper's damage source also knows who caused explosions and the like
                Entity causing = event.getDamageSource().getCausingEntity();
                attacker = causing instanceof Player player ? player : null;
            } catch (LinkageError | RuntimeException olderServer) {
                attacker = null;
            }
        }
        if (blocked(attacker, event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /** Fire Aspect and burning arrows. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustByEntityEvent event) {
        Player attacker = culprit(event.getCombuster());
        if (attacker != null && event.getEntity() instanceof Player target && !attacker.equals(target)
                && (newcomers.shielded(target) || newcomers.shielded(attacker))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent event) {
        Player thrower = shooter(event.getPotion().getShooter());
        if (thrower == null) {
            return;
        }
        for (LivingEntity hit : event.getAffectedEntities()) {
            if (hit instanceof Player target && !target.equals(thrower)
                    && (newcomers.shielded(target) || newcomers.shielded(thrower))) {
                event.setIntensity(target, 0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCloud(AreaEffectCloudApplyEvent event) {
        Player thrower = shooter(event.getEntity().getSource());
        if (thrower == null) {
            return;
        }
        event.getAffectedEntities().removeIf(hit -> hit instanceof Player target && !target.equals(thrower)
                && (newcomers.shielded(target) || newcomers.shielded(thrower)));
    }

    /** No pulling a protected player around with a fishing rod. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY && event.getCaught() instanceof Player target
                && !target.equals(event.getPlayer())
                && (newcomers.shielded(target) || newcomers.shielded(event.getPlayer()))) {
            event.setCancelled(true);
        }
    }
}
