package me.rauldoq.geoAnarchy.game;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * The /ranks lists: who played the longest and who has the most player kills.
 * <p>
 * A player's numbers are in memory while they are online, but for an offline player the server reads their
 * files from disk, and only on the server thread. Doing that for everyone who ever joined would freeze the tick
 * the command runs on, so /ranks never reads anything: it shows a board that is kept ready here.
 * <ul>
 *   <li>At start, everyone who ever joined is read once, a few players per tick ({@link #PER_TICK}, and no more
 *       than {@link #BUDGET_NANOS} of a tick). Until that is done, /ranks shows what has been counted so far
 *       and says it is still counting.</li>
 *   <li>An offline player's numbers can't change, so they are not read again. A player is read when they join
 *       and when they leave, their kills count the moment they happen, and the time of everyone online is read
 *       again every few minutes (ranks.refresh-minutes), a few players per tick as well.</li>
 * </ul>
 * Everything here runs on the server thread.
 */
public final class Ranks implements Listener {

    public record Entry(String name, long value) {
    }

    /**
     * What /ranks shows. {@code complete} stays false until everyone who ever joined has been read once since the
     * start; until then {@code counted} of {@code total} players are in.
     */
    public record Board(List<Entry> hoursPlayed, List<Entry> kills, boolean complete, int counted, int total) {

        public int percent() {
            if (complete) {
                return 100;
            }
            return total == 0 ? 0 : (int) Math.min(99, counted * 100L / total);
        }
    }

    /** One player's name and numbers, as the server has them. */
    public record Stats(String name, long playTicks, long kills) {
    }

    /** Where the numbers come from: the server in the game, made-up players in the tests. Used on the server thread only. */
    public interface Source {

        /** Everyone who has ever joined. Asked once, when the count starts. */
        Collection<UUID> everyone();

        /** Everyone online now. */
        Collection<UUID> online();

        /** One player's name and numbers, or null when the player has no name or their files can't be read. */
        Stats read(UUID player);
    }

    /** The most players read in one tick. An offline player is a few small files read from disk. */
    static final int PER_TICK = 10;
    /** The most time spent reading players in one tick, two milliseconds. At least one player is always read. */
    static final long BUDGET_NANOS = 2_000_000L;

    private static final int SIZE = 5;
    /** Highest first. A tie goes by name, ignoring case; names that differ only in case go by the exact name. */
    static final Comparator<Entry> ORDER = Comparator.comparingLong(Entry::value).reversed()
            .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Entry::name);

    private record Row(String name, long hours, long kills) {
    }

    private final Source source;
    private final LongSupplier millis;
    private final LongSupplier nanos;
    private final IntSupplier refreshMinutes;

    /** Everyone read since the start. */
    private final Map<UUID, Row> rows = new HashMap<>();
    /** Everyone who ever joined and is still to be read for the first time. */
    private final ArrayDeque<UUID> pending = new ArrayDeque<>();
    /** Online players whose time is to be read again. They go before {@link #pending}. */
    private final Set<UUID> again = new LinkedHashSet<>();

    private final List<Entry> topTime = new ArrayList<>();
    private final List<Entry> topKills = new ArrayList<>();
    /** A row that was there already changed, so the top lists must be worked out again from every row. */
    private boolean rebuild;
    /** Something changed since the board was last made. */
    private boolean changed;

    private boolean started;
    private boolean complete;
    private int total;
    private int counted;
    private long onlineReadAt;
    private volatile Board board = new Board(List.of(), List.of(), false, 0, 0);

    /**
     * @param millis         the clock, for when to read the online players again
     * @param nanos          the clock for the time spent in one tick
     * @param refreshMinutes how often the online players are read again (ranks.refresh-minutes)
     */
    public Ranks(Source source, LongSupplier millis, LongSupplier nanos, IntSupplier refreshMinutes) {
        this.source = source;
        this.millis = millis;
        this.nanos = nanos;
        this.refreshMinutes = refreshMinutes;
    }

    /** The real server. */
    public static Source server() {
        return new ServerSource();
    }

    /** What /ranks shows right now. It reads and sorts nothing: the board is the one the last tick left ready. */
    public Board get() {
        return board;
    }

    /** Runs every tick: reads a few players, then makes a new board if anything changed. */
    public void tick() {
        long now = millis.getAsLong();
        if (!started) {
            started = true;
            onlineReadAt = now;
            changed = true;
            pending.addAll(new LinkedHashSet<>(source.everyone()));
            total = pending.size();
        } else if (now - onlineReadAt >= refreshMinutes.getAsInt() * 60_000L) {
            onlineReadAt = now;
            again.addAll(source.online());
        }

        long start = nanos.getAsLong();
        int reads = 0;
        while (reads < PER_TICK && (reads == 0 || nanos.getAsLong() - start < BUDGET_NANOS)) {
            UUID next;
            Iterator<UUID> online = again.iterator();
            if (online.hasNext()) {
                next = online.next();
                online.remove();
            } else {
                next = nextPending();
                if (next == null) {
                    break;
                }
            }
            read(next);
            reads++;
        }
        if (!complete && pending.isEmpty()) {
            complete = true;
            changed = true;
        }
        publish();
    }

    /** The next player still to be read for the first time; players read already (they joined meanwhile) are skipped. */
    private UUID nextPending() {
        while (!pending.isEmpty()) {
            UUID next = pending.poll();
            counted++;
            changed = true;
            if (!rows.containsKey(next)) {
                return next;
            }
        }
        return null;
    }

    /** A player joined: their numbers are in memory now, so they are read at once. */
    public void joined(UUID player) {
        read(player);
        publish();
    }

    /** A player is leaving: read them one last time while their numbers are still in memory. */
    public void left(UUID player) {
        again.remove(player);
        read(player);
        publish();
    }

    /**
     * A player killed another player and now has this many kills. The server tells this before it adds the kill
     * to its own count, so the number given here wins over what reading the player would say.
     */
    public void killed(UUID player, long kills) {
        if (!rows.containsKey(player)) {
            read(player);
        }
        Row row = rows.get(player);
        if (row != null && row.kills() != kills) {
            store(player, new Row(row.name(), row.hours(), kills));
        }
        publish();
    }

    private void read(UUID player) {
        Stats stats;
        try {
            stats = source.read(player);
        } catch (RuntimeException unreadable) {
            stats = null;
        }
        if (stats != null && stats.name() != null) {
            // PLAY_ONE_MINUTE counts ticks, despite its name: 20 a second.
            store(player, new Row(stats.name(), stats.playTicks() / 20L / 3600L, stats.kills()));
        }
        // a player without a name, or whose files can't be read, is left out
    }

    private void store(UUID player, Row row) {
        Row old = rows.put(player, row);
        if (old == null) {
            // Someone new can only push others down, so the top lists just take them in.
            offer(topTime, new Entry(row.name(), row.hours()));
            offer(topKills, new Entry(row.name(), row.kills()));
            changed = true;
        } else if (!old.equals(row)) {
            rebuild = true;
            changed = true;
        }
    }

    private void publish() {
        if (rebuild) {
            rebuild = false;
            topTime.clear();
            topKills.clear();
            for (Row row : rows.values()) {
                offer(topTime, new Entry(row.name(), row.hours()));
                offer(topKills, new Entry(row.name(), row.kills()));
            }
        }
        if (changed) {
            changed = false;
            board = new Board(List.copyOf(topTime), List.copyOf(topKills), complete, counted, total);
        }
    }

    /** Keeps the best {@link #SIZE} entries above zero, best first, in {@link #ORDER}. */
    private static void offer(List<Entry> best, Entry entry) {
        if (entry.value() <= 0) {
            return;
        }
        if (best.size() == SIZE && ORDER.compare(entry, best.get(SIZE - 1)) >= 0) {
            return;
        }
        int at = 0;
        while (at < best.size() && ORDER.compare(best.get(at), entry) <= 0) {
            at++;
        }
        best.add(at, entry);
        if (best.size() > SIZE) {
            best.remove(SIZE);
        }
    }

    /** The top of a list, the same way the board picks it. */
    static List<Entry> top(Collection<Entry> entries) {
        List<Entry> best = new ArrayList<>();
        for (Entry entry : entries) {
            offer(best, entry);
        }
        return List.copyOf(best);
    }

    // ---- The server ----

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        joined(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        left(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStatistic(PlayerStatisticIncrementEvent event) {
        if (event.getStatistic() == Statistic.PLAYER_KILLS) {
            killed(event.getPlayer().getUniqueId(), event.getNewValue());
        }
    }

    private static final class ServerSource implements Source {

        @Override
        public Collection<UUID> everyone() {
            Set<UUID> ids = new LinkedHashSet<>();
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                ids.add(player.getUniqueId());
            }
            return ids;
        }

        @Override
        public Collection<UUID> online() {
            List<UUID> ids = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                ids.add(player.getUniqueId());
            }
            return ids;
        }

        @Override
        public Stats read(UUID id) {
            // The online player while they are on (numbers in memory), otherwise their files on disk.
            OfflinePlayer player = Bukkit.getOfflinePlayer(id);
            String name = player.getName();
            if (name == null) {
                return null;
            }
            return new Stats(name, player.getStatistic(Statistic.PLAY_ONE_MINUTE), player.getStatistic(Statistic.PLAYER_KILLS));
        }
    }
}
