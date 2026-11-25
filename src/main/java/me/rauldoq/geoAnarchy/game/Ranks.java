package me.rauldoq.geoAnarchy.game;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The /ranks lists: who played the longest and who has the most player kills.
 * Reading every player's statistics takes a moment, so the lists are worked out when someone asks
 * and then kept for a few minutes.
 */
public final class Ranks {

    public record Entry(String name, long value) {
    }

    public record Board(List<Entry> hoursPlayed, List<Entry> kills) {
    }

    private static final int SIZE = 5;

    private Board board;
    private long computedAt;

    /** Call from the server thread. */
    public Board get(int refreshMinutes) {
        long now = System.currentTimeMillis();
        if (board == null || now - computedAt > refreshMinutes * 60_000L) {
            board = compute();
            computedAt = now;
        }
        return board;
    }

    private static Board compute() {
        List<Entry> time = new ArrayList<>();
        List<Entry> kills = new ArrayList<>();
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            String name = player.getName();
            if (name == null) {
                continue;
            }
            try {
                // PLAY_ONE_MINUTE counts ticks, despite its name: 20 a second.
                long hours = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L / 3600L;
                long killed = player.getStatistic(Statistic.PLAYER_KILLS);
                time.add(new Entry(name, hours));
                kills.add(new Entry(name, killed));
            } catch (RuntimeException unreadable) {
                // a player file that can't be read is left out
            }
        }
        return new Board(top(time), top(kills));
    }

    static List<Entry> top(List<Entry> entries) {
        return entries.stream()
                .filter(entry -> entry.value() > 0)
                .sorted(Comparator.comparingLong(Entry::value).reversed().thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER))
                .limit(SIZE)
                .toList();
    }
}
