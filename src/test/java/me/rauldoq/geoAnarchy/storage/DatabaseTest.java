package me.rauldoq.geoAnarchy.storage;

import me.rauldoq.geoAnarchy.storage.Database.LogLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Discord log: what a player did through Discord, shown to them at their next login. */
class DatabaseTest {

    private static final UUID STEVE = UUID.randomUUID();
    private static final UUID ALEX = UUID.randomUUID();

    @TempDir
    Path folder;
    private Database database;

    @BeforeEach
    void open() throws Exception {
        database = new Database(folder.resolve("test.db"));
    }

    @Test
    void moneyOwedStaysUntilItIsGivenBackEvenAcrossARestart() throws Exception {
        database.addOwed(STEVE, 2500, "discord:/depositar devolvido", 1000);
        database.addOwed(STEVE, 100, "discord:/depositar devolvido", 2000);
        database.close();
        database = new Database(folder.resolve("test.db"));
        List<Database.Owed> owed = database.owed();
        assertEquals(List.of(2500L, 100L), owed.stream().map(Database.Owed::cents).toList());
        assertEquals(STEVE, owed.get(0).player());
        database.deleteOwed(owed.get(0).id());
        assertEquals(List.of(100L), database.owed().stream().map(Database.Owed::cents).toList());
    }

    @AfterEach
    void close() {
        database.close();
    }

    /** Counts the lines not shown yet, through a connection of its own, so it reads what is really in the file. */
    private int unseen(UUID player) throws Exception {
        try (Connection own = DriverManager.getConnection("jdbc:sqlite:" + folder.resolve("test.db").toAbsolutePath());
             PreparedStatement statement = own.prepareStatement("SELECT COUNT(*) FROM discord_log WHERE minecraft = ? AND seen = 0")) {
            statement.setString(1, player.toString());
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    /** 25 lines for Steve, with Alex's lines in between: 1 to 25 for Steve, and one for Alex after each even one. */
    private void logTwentyFiveForSteveAndTwelveForAlex() {
        for (int i = 1; i <= 25; i++) {
            database.log(STEVE, "Steve " + i, i);
            if (i % 2 == 0) {
                database.log(ALEX, "Alex " + i, i);
            }
        }
    }

    @Test
    void aLoginShowsTwentyLinesAndOnlyThoseAreMarkedTheRestComeNextTime() throws Exception {
        logTwentyFiveForSteveAndTwelveForAlex();
        assertEquals(25, unseen(STEVE));
        assertEquals(12, unseen(ALEX));

        List<LogLine> first = database.takeUnseenLines(STEVE);
        assertEquals(20, first.size());
        assertEquals(5, unseen(STEVE), "only the 20 lines shown may be marked");
        assertEquals(12, unseen(ALEX), "Steve's login must not touch Alex's lines");

        List<LogLine> second = database.takeUnseenLines(STEVE);
        assertEquals(5, second.size());
        assertEquals(0, unseen(STEVE));

        List<LogLine> third = database.takeUnseenLines(STEVE);
        assertEquals(List.of(), third);
        assertEquals(12, unseen(ALEX));

        List<LogLine> all = new ArrayList<>(first);
        all.addAll(second);
        assertEquals(IntStream.rangeClosed(1, 25).mapToObj(i -> "Steve " + i).toList(), all.stream().map(LogLine::text).toList(),
                "every line once, oldest first");
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < all.size(); i++) {
            assertTrue(ids.add(all.get(i).id()), "line " + all.get(i) + " came twice");
            if (i > 0) {
                assertTrue(all.get(i - 1).id() < all.get(i).id(), "out of order at " + all.get(i));
            }
        }

        // Alex still gets all of his, in order, at his own login.
        assertEquals(IntStream.rangeClosed(1, 12).mapToObj(i -> "Alex " + (2 * i)).toList(), database.takeUnseenLog(ALEX));
        assertEquals(0, unseen(ALEX));
        assertEquals(List.of(), database.takeUnseenLog(ALEX));
    }

    @Test
    void theLinesThatDidNotFitAreStillThereAfterARestart() throws Exception {
        logTwentyFiveForSteveAndTwelveForAlex();
        assertEquals(IntStream.rangeClosed(1, 20).mapToObj(i -> "Steve " + i).toList(), database.takeUnseenLog(STEVE));

        database.close();
        database = new Database(folder.resolve("test.db"));
        assertEquals(List.of("Steve 21", "Steve 22", "Steve 23", "Steve 24", "Steve 25"), database.takeUnseenLog(STEVE));
        assertEquals(List.of(), database.takeUnseenLog(STEVE));

        // The connection is back to saving each change on its own after the transaction.
        database.log(STEVE, "Steve 26", 26);
        assertEquals(1, unseen(STEVE));
    }
}
