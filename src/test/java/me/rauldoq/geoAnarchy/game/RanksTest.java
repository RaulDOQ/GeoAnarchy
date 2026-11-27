package me.rauldoq.geoAnarchy.game;

import me.rauldoq.geoAnarchy.game.Ranks.Board;
import me.rauldoq.geoAnarchy.game.Ranks.Entry;
import me.rauldoq.geoAnarchy.game.Ranks.Stats;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RanksTest {

    private static final long TICKS_PER_HOUR = 20L * 3600L;

    /** Made-up players instead of the server. Counts the statistics read, two per player (time and kills). */
    private static final class World implements Ranks.Source {
        final Map<UUID, Stats> players = new LinkedHashMap<>();
        final Set<UUID> online = new LinkedHashSet<>();
        final Set<UUID> broken = new HashSet<>();
        int statReads;
        int everyoneAsked;
        long nanos;
        long nanosPerRead;

        UUID add(String name, long hours, long kills) {
            UUID id = UUID.randomUUID();
            // a little past the hour, to check that the hours are rounded down
            players.put(id, new Stats(name, hours * TICKS_PER_HOUR + 17, kills));
            return id;
        }

        void set(UUID id, long hours, long kills) {
            players.put(id, new Stats(players.get(id).name(), hours * TICKS_PER_HOUR, kills));
        }

        @Override
        public Collection<UUID> everyone() {
            everyoneAsked++;
            return new ArrayList<>(players.keySet());
        }

        @Override
        public Collection<UUID> online() {
            return new ArrayList<>(online);
        }

        @Override
        public Stats read(UUID player) {
            statReads += 2;
            nanos += nanosPerRead;
            if (broken.contains(player)) {
                throw new IllegalStateException("unreadable stats file");
            }
            return players.get(player);
        }
    }

    private final World world = new World();
    private long now = 1_000_000;
    private int refreshMinutes = 5;
    private final Ranks ranks = new Ranks(world, () -> now, () -> world.nanos, () -> refreshMinutes);

    /** Ticks until everyone has been read once. Returns how many ticks that took. */
    private int countEveryone() {
        int ticks = 0;
        while (!ranks.get().complete()) {
            ranks.tick();
            ticks++;
            assertTrue(ticks < 1_000_000, "the count never ends");
        }
        return ticks;
    }

    /** The rule the old code sorted by, copied from it: highest first, then the name ignoring case, top five, no zeros. */
    private static List<Entry> oldTop(Collection<Entry> entries) {
        return entries.stream()
                .filter(entry -> entry.value() > 0)
                .sorted(Comparator.comparingLong(Entry::value).reversed().thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .toList();
    }

    private List<Entry> expectedHours() {
        return oldTop(world.players.values().stream().map(s -> new Entry(s.name(), s.playTicks() / TICKS_PER_HOUR)).toList());
    }

    private List<Entry> expectedKills() {
        return oldTop(world.players.values().stream().map(s -> new Entry(s.name(), s.kills())).toList());
    }

    private static List<String> names(List<Entry> entries) {
        return entries.stream().map(Entry::name).toList();
    }

    @Test
    void onAColdStartTheCommandAnswersAtOnceAndSaysItIsStillCounting() {
        for (int i = 1; i <= 23; i++) {
            world.add("Player" + i, i, 100 - i);
        }
        Board before = ranks.get();
        assertFalse(before.complete());
        assertEquals(0, before.percent());
        assertEquals(List.of(), before.hoursPlayed());
        assertEquals(0, world.statReads, "asking for the board before the first tick reads nothing");

        ranks.tick();
        Board partial = ranks.get();
        assertEquals(2 * Ranks.PER_TICK, world.statReads, "one tick reads at most PER_TICK players");
        assertFalse(partial.complete());
        assertEquals(10, partial.counted());
        assertEquals(23, partial.total());
        assertEquals(43, partial.percent());
        // What is on the board so far is right for the players read so far.
        assertEquals(List.of("Player10", "Player9", "Player8", "Player7", "Player6"), names(partial.hoursPlayed()));
        assertEquals(List.of("Player1", "Player2", "Player3", "Player4", "Player5"), names(partial.kills()));

        for (int i = 0; i < 100; i++) {
            assertSame(partial, ranks.get());
        }
        assertEquals(20, world.statReads, "asking again and again reads nothing more");

        assertEquals(2, countEveryone());
        Board full = ranks.get();
        assertTrue(full.complete());
        assertEquals(100, full.percent());
        assertEquals(23, full.counted());
        assertEquals(expectedHours(), full.hoursPlayed());
        assertEquals(List.of(new Entry("Player23", 23), new Entry("Player22", 22), new Entry("Player21", 21),
                new Entry("Player20", 20), new Entry("Player19", 19)), full.hoursPlayed());
        assertEquals(expectedKills(), full.kills());
        assertEquals(46, world.statReads, "everyone is read exactly once");
        assertEquals(1, world.everyoneAsked, "the list of everyone is asked for once");

        // Nobody is online, and offline players can't change: later ticks read nothing and keep the same board.
        for (int i = 0; i < 1000; i++) {
            ranks.tick();
        }
        assertEquals(46, world.statReads);
        assertSame(full, ranks.get());
    }

    /**
     * The evidence for the change: how much the command reads as more and more players have joined over time.
     * The old /ranks read two statistics of every player who ever joined, on the command's own tick, every time
     * its five-minute copy had run out (2 x players). Now the command reads nothing, at any size, and the reading
     * happens in the background, never more than PER_TICK players in one tick.
     */
    @Test
    void theCommandReadsNothingHoweverManyPlayersHaveEverJoined() {
        for (int players : new int[]{100, 1_000, 10_000, 100_000}) {
            World many = new World();
            for (int i = 0; i < players; i++) {
                many.add("P" + i, i % 500, i % 77);
            }
            long[] clock = {0};
            Ranks board = new Ranks(many, () -> clock[0], () -> 0, () -> 5);
            int[] calls = {0};
            int[] readsByCommands = {0};
            Runnable command = () -> {
                int before = many.statReads;
                board.get();
                calls[0]++;
                readsByCommands[0] += many.statReads - before;
            };

            command.run(); // cold: before the first tick
            int mostInOneTick = 0;
            int ticks = 0;
            while (!board.get().complete()) {
                int before = many.statReads;
                board.tick();
                ticks++;
                mostInOneTick = Math.max(mostInOneTick, many.statReads - before);
                command.run(); // while the count goes on
            }
            // once the board is ready, and after it ran out
            command.run();
            clock[0] += 10 * 60_000L;
            command.run();
            board.tick();
            command.run();

            assertEquals(0, readsByCommands[0], players + " players");
            assertTrue(mostInOneTick <= 2 * Ranks.PER_TICK, players + " players: " + mostInOneTick + " reads in one tick");
            assertEquals((players + Ranks.PER_TICK - 1) / Ranks.PER_TICK, ticks, players + " players");
            assertEquals(2 * players, many.statReads, players + " players: everyone is read once, in the background");
            System.out.printf(Locale.ROOT, "/ranks, %,d players who ever joined: %d statistics read in %,d command calls"
                            + " (old code: %,d per cold or expired call). Background: %,d ticks, at most %d reads per tick.%n",
                    players, readsByCommands[0], calls[0], 2 * players, ticks, mostInOneTick);
        }
    }

    @Test
    void whenTheBoardRunsOutOnlyThePlayersOnlineAreReadAgainAFewPerTick() {
        List<UUID> ids = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            ids.add(world.add("Player" + i, i, i));
        }
        countEveryone();
        assertEquals(100, world.statReads);
        Board ready = ranks.get();

        // 25 players come online (without a join event, as after a plugin reload) and keep playing.
        for (int i = 0; i < 25; i++) {
            world.online.add(ids.get(i));
            world.set(ids.get(i), 1000 + i, i + 1);
        }
        UUID offline = ids.get(49);
        world.set(offline, 5000, 5000); // can't happen to an offline player; shows that offline players are not read

        now += refreshMinutes * 60_000L - 1;
        ranks.tick();
        assertEquals(100, world.statReads, "nothing is read before the refresh time");
        assertSame(ready, ranks.get(), "the old board is served until then");

        now += 1;
        ranks.tick();
        assertEquals(100 + 2 * Ranks.PER_TICK, world.statReads, "the online players are read a few per tick");
        ranks.tick();
        ranks.tick();
        assertEquals(100 + 2 * 25, world.statReads, "every online player once, nobody offline");
        ranks.tick();
        assertEquals(150, world.statReads);

        Board refreshed = ranks.get();
        assertEquals(List.of("Player25", "Player24", "Player23", "Player22", "Player21"), names(refreshed.hoursPlayed()));
        assertEquals(1024, refreshed.hoursPlayed().getFirst().value());
        assertFalse(names(refreshed.hoursPlayed()).contains("Player50"), "offline players keep what was read at the start");

        // A shorter refresh time from /geoanarchy reload counts from the last refresh.
        refreshMinutes = 1;
        now += 60_000L;
        ranks.tick();
        assertEquals(150 + 2 * Ranks.PER_TICK, world.statReads);
    }

    @Test
    void killsCountAtOnceAndJoiningOrLeavingReadsThePlayer() {
        UUID steve = world.add("Steve", 10, 3);
        UUID alex = world.add("Alex", 20, 1);
        countEveryone();
        int reads = world.statReads;

        // The server reports the kill before it counts it, so the number in the event is the one that counts.
        ranks.killed(steve, 4);
        assertEquals(reads, world.statReads, "a kill of someone already read needs no reading");
        assertEquals(List.of(new Entry("Steve", 4), new Entry("Alex", 1)), ranks.get().kills());

        UUID notch = world.add("Notch", 50, 0);
        ranks.joined(notch);
        assertEquals(reads + 2, world.statReads);
        assertEquals("Notch", ranks.get().hoursPlayed().getFirst().name());

        // Alex plays a lot more and leaves: what he has when leaving is on the board at once.
        world.set(alex, 70, 1);
        ranks.left(alex);
        assertEquals(reads + 4, world.statReads);
        assertEquals(new Entry("Alex", 70), ranks.get().hoursPlayed().getFirst());

        // A new name shows up at the next join, and the old one is gone.
        world.players.put(steve, new Stats("Steve2", 10 * TICKS_PER_HOUR, 4));
        ranks.joined(steve);
        assertEquals(List.of("Steve2", "Alex"), names(ranks.get().kills()));
    }

    @Test
    void duringTheColdCountJoinsAndKillsAreReadAtOnceAndNotReadTwice() {
        for (int i = 1; i <= 30; i++) {
            world.add("Player" + i, i, 0);
        }
        UUID late = world.add("Late", 1, 5);
        UUID killer = world.add("Killer", 1, 7);
        ranks.tick();
        assertEquals(20, world.statReads);

        ranks.joined(late);
        assertEquals(new Entry("Late", 5), ranks.get().kills().getFirst());

        // The killer has not been read yet: they are read now, and the kill in the event is added to what was read.
        ranks.killed(killer, 8);
        assertEquals(new Entry("Killer", 8), ranks.get().kills().getFirst());
        assertFalse(ranks.get().complete());

        countEveryone();
        assertEquals(2 * 32, world.statReads, "the two read early are skipped by the count");
        assertEquals(List.of(new Entry("Killer", 8), new Entry("Late", 5)), ranks.get().kills());
        assertEquals(32, ranks.get().counted());
    }

    @Test
    void tiesGoByNameIgnoringCaseLikeBefore() {
        world.add("Zed", 10, 0);
        for (String name : new String[]{"eve", "Dave", "carl", "bob", "Alice"}) {
            world.add(name, 5, 2);
        }
        countEveryone();
        assertEquals(List.of("Zed", "Alice", "bob", "carl", "Dave"), names(ranks.get().hoursPlayed()));
        assertEquals(List.of("Alice", "bob", "carl", "Dave", "eve"), names(ranks.get().kills()));
        assertEquals(expectedHours(), ranks.get().hoursPlayed());
        assertEquals(expectedKills(), ranks.get().kills());
        assertFalse(names(ranks.get().kills()).contains("Zed"), "nobody with zero is listed");

        // Names that differ only in case were in no fixed order before; now the exact name decides.
        List<Entry> twins = new ArrayList<>(List.of(new Entry("steve", 3), new Entry("Steve", 3), new Entry("STEVE", 3)));
        for (int i = 0; i < 10; i++) {
            Collections.shuffle(twins, new Random(i));
            assertEquals(List.of("STEVE", "Steve", "steve"), names(Ranks.top(twins)));
        }
    }

    @Test
    void theBoardPicksTheSameTopFiveAsTheOldSortOnRandomPlayers() {
        Random random = new Random(42);
        for (int round = 0; round < 300; round++) {
            List<Entry> entries = new ArrayList<>();
            int size = random.nextInt(40);
            for (int i = 0; i < size; i++) {
                // unique names (ignoring case), random case, few values so there are many ties
                String name = "player" + i;
                StringBuilder mixed = new StringBuilder();
                for (char c : name.toCharArray()) {
                    mixed.append(random.nextBoolean() ? Character.toUpperCase(c) : c);
                }
                entries.add(new Entry(mixed.toString(), random.nextInt(4)));
            }
            assertEquals(oldTop(entries), Ranks.top(entries), entries.toString());
        }
    }

    @Test
    void playersWithoutANameOrWithUnreadableFilesAreLeftOut() {
        world.add("Good", 3, 3);
        UUID nameless = UUID.randomUUID();
        world.players.put(nameless, new Stats(null, 99 * TICKS_PER_HOUR, 99));
        UUID broken = world.add("Broken", 50, 50);
        world.broken.add(broken);

        countEveryone();
        assertEquals(List.of(new Entry("Good", 3)), ranks.get().hoursPlayed());
        assertEquals(List.of(new Entry("Good", 3)), ranks.get().kills());
        assertEquals(3, ranks.get().counted());
    }

    @Test
    void aSlowDiskReadsFewerPlayersPerTickButAlwaysAtLeastOne() {
        for (int i = 0; i < 20; i++) {
            world.add("P" + i, 1, 1);
        }
        world.nanosPerRead = 1_000_000L; // 1 ms per player
        ranks.tick();
        assertEquals(2 * 2, world.statReads, "two players fit in the 2 ms of a tick");

        world.nanosPerRead = 50_000_000L; // 50 ms per player: one per tick, so the count still ends
        ranks.tick();
        assertEquals(2 * 3, world.statReads);
        assertEquals(17, countEveryone());
        assertEquals(2 * 20, world.statReads);
    }
}
