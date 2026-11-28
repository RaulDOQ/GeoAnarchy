package me.rauldoq.geoAnarchy.onboarding;

import me.rauldoq.geoAnarchy.country.Countries;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Finds where a new player lands on their continent: a random spot inside its outlines, away from every
 * country, on solid ground with room to stand. The chunk is loaded in the background; the checks run on the
 * server thread (Paper finishes chunk loads there). Gives up after a number of tries (null).
 */
final class Landing {

    /** Ground nobody should be dropped on. */
    private static final Set<Material> BAD_GROUND = EnumSet.of(Material.MAGMA_BLOCK, Material.CACTUS, Material.POWDER_SNOW,
            Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.POINTED_DRIPSTONE, Material.SWEET_BERRY_BUSH);

    private final Supplier<Countries> countries;

    Landing(Supplier<Countries> countries) {
        this.countries = countries;
    }

    CompletableFuture<Location> find(World world, Continent continent, int tries, int claimRadiusChunks) {
        CompletableFuture<Location> result = new CompletableFuture<>();
        attempt(world, continent, tries, claimRadiusChunks, result);
        return result;
    }

    private void attempt(World world, Continent c, int left, int radius, CompletableFuture<Location> result) {
        if (left <= 0) {
            result.complete(null);
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double lon = 0;
        double lat = 0;
        boolean inside = false;
        for (int i = 0; i < 300 && !inside; i++) {
            lon = c.lonMin() + random.nextDouble() * (c.lonMax() - c.lonMin());
            lat = c.latMin() + random.nextDouble() * (c.latMax() - c.latMin());
            inside = c.contains(lon, lat);
        }
        if (!inside) {
            result.complete(null);
            return;
        }
        int x = Continent.blockX(lon);
        int z = Continent.blockZ(lat);
        if (!world.getWorldBorder().isInside(new Location(world, x, 64, z))
                || countries.get().claimedNear(world, x, z, radius)) {
            attempt(world, c, left - 1, radius, result);
            return;
        }
        world.getChunkAtAsync(x >> 4, z >> 4).whenComplete((chunk, error) -> {
            Location spot = error == null ? standingSpot(world, x, z) : null;
            if (spot != null) {
                result.complete(spot);
            } else {
                attempt(world, c, left - 1, radius, result);
            }
        });
    }

    /** The top of the ground at (x, z) if a player can stand there: solid, not liquid, two free blocks above. */
    static Location standingSpot(World world, int x, int z) {
        Block ground = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        Material type = ground.getType();
        if (ground.isLiquid() || !type.isSolid() || BAD_GROUND.contains(type)) {
            return null;
        }
        Block feet = ground.getRelative(BlockFace.UP);
        Block head = feet.getRelative(BlockFace.UP);
        if (!feet.isPassable() || !head.isPassable() || feet.isLiquid() || head.isLiquid()) {
            return null;
        }
        return new Location(world, x + 0.5, ground.getY() + 1, z + 0.5);
    }
}
