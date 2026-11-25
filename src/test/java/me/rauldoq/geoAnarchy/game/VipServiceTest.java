package me.rauldoq.geoAnarchy.game;

import me.rauldoq.geoAnarchy.storage.Database;
import me.rauldoq.geoAnarchy.storage.Database.Vip;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VipServiceTest {

    private static final UUID STEVE = UUID.randomUUID();
    private static final long DAY = 24L * 60 * 60 * 1000;

    @TempDir
    Path folder;
    private Database database;
    private VipService vips;
    private final List<String> console = new ArrayList<>();

    @BeforeEach
    void open() throws Exception {
        database = new Database(folder.resolve("test.db"));
        vips = new VipService(database, console::add);
    }

    @AfterEach
    void close() {
        database.close();
    }

    @Test
    void givingVipAddsTheGroupOnceAndCountsEveryMonth() {
        assertEquals(3, vips.give(STEVE, "Steve", 3, "vip", 0), "the old command always gave one month, whatever was typed");
        assertEquals(List.of("lp user Steve parent add vip"), console);

        assertEquals(5, vips.give(STEVE, "Steve", 2, "vip", 10 * DAY));
        assertEquals(1, console.size(), "more months for someone who is already VIP need no new group command");
        assertEquals(0, database.vips().get(0).periodStart(), "adding months does not restart the current month");
    }

    @Test
    void aMonthIsUsedUpEveryThirtyDaysAndTheGroupGoesWhenNoneIsLeft() {
        vips.give(STEVE, "Steve", 2, "vip", 0);
        console.clear();

        vips.tick("vip", 29 * DAY);
        assertEquals(2, database.vips().get(0).months());

        vips.tick("vip", 30 * DAY);
        assertEquals(1, database.vips().get(0).months());
        assertTrue(console.isEmpty());

        vips.tick("vip", 60 * DAY);
        assertTrue(database.vips().isEmpty());
        assertEquals(List.of("lp user Steve parent remove vip"), console);
    }

    @Test
    void timeTheServerWasOffStillCounts() {
        Vip vip = new Vip(STEVE, "Steve", 5, 0);
        assertEquals(3, VipService.advance(vip, 61 * DAY).months());
        assertEquals(60 * DAY, VipService.advance(vip, 61 * DAY).periodStart());
        assertNull(VipService.advance(vip, 150 * DAY));
        assertEquals(vip, VipService.advance(vip, DAY));
    }
}
