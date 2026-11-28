package me.rauldoq.geoAnarchy.onboarding;

import net.kyori.adventure.text.format.TextColor;

import java.util.List;

/**
 * One continent a new player can start on: its name, where its pin is in the menu, and where a landing spot may
 * be picked: inside one of its outlines and inside its window (which keeps out the poles and far corners).
 */
public record Continent(String id, String name, int slot, TextColor colour,
                        double lonMin, double lonMax, double latMin, double latMax, List<double[][]> polygons) {

    /** The Earth map is a plain world map: this many blocks per degree, longitude east, latitude north = -z. */
    public static final double BLOCKS_PER_DEGREE = 102.4;

    public boolean contains(double lon, double lat) {
        if (lon < lonMin || lon > lonMax || lat < latMin || lat > latMax) {
            return false;
        }
        for (double[][] poly : polygons) {
            if (inside(poly, lon, lat)) {
                return true;
            }
        }
        return false;
    }

    static boolean inside(double[][] poly, double lon, double lat) {
        boolean inside = false;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
            double xi = poly[i][0], yi = poly[i][1], xj = poly[j][0], yj = poly[j][1];
            if ((yi > lat) != (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) {
                inside = !inside;
            }
        }
        return inside;
    }

    static int blockX(double lon) {
        return (int) Math.floor(lon * BLOCKS_PER_DEGREE);
    }

    static int blockZ(double lat) {
        return (int) Math.floor(-lat * BLOCKS_PER_DEGREE);
    }

    static double lon(double blockX) {
        return blockX / BLOCKS_PER_DEGREE;
    }

    static double lat(double blockZ) {
        return -blockZ / BLOCKS_PER_DEGREE;
    }
}
