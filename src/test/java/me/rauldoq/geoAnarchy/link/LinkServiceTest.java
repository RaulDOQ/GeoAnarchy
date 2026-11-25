package me.rauldoq.geoAnarchy.link;

import me.rauldoq.geoAnarchy.link.LinkService.Redeem;
import me.rauldoq.geoAnarchy.link.LinkService.Status;
import me.rauldoq.geoAnarchy.storage.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkServiceTest {

    private static final UUID STEVE = UUID.randomUUID();
    private static final UUID ALEX = UUID.randomUUID();

    @TempDir
    Path folder;
    private Database database;
    private long now = 1_000_000;

    private LinkService service() {
        return new LinkService(database, () -> now, 10, 5, 5);
    }

    @BeforeEach
    void open() throws Exception {
        database = new Database(folder.resolve("test.db"));
    }

    @AfterEach
    void close() {
        database.close();
    }

    @Test
    void aCodeFromTheGameLinksTheDiscordAccountThatTypesIt() {
        LinkService links = service();
        String code = links.issueCode(STEVE, "Steve").orElseThrow();
        assertTrue(code.matches("\\d{6}"), code);

        Redeem result = links.redeem("111", LinkService.pretty(code));
        assertEquals(Status.LINKED, result.status());
        assertEquals("Steve", result.link().name());
        assertEquals(STEVE, links.byDiscord("111").orElseThrow().minecraft());
        assertEquals("111", links.byMinecraft(STEVE).orElseThrow().discord());
    }

    @Test
    void aLinkIsStillThereAfterARestart() {
        LinkService links = service();
        links.redeem("111", links.issueCode(STEVE, "Steve").orElseThrow());

        LinkService restarted = service();
        assertEquals(1, restarted.size());
        assertEquals("Steve", restarted.byDiscord("111").orElseThrow().name());
    }

    @Test
    void aCodeWorksOnceAndOnlyForTenMinutes() {
        LinkService links = service();
        String code = links.issueCode(STEVE, "Steve").orElseThrow();
        now += 10 * 60_000 + 1;
        assertEquals(Status.WRONG_CODE, links.redeem("111", code).status(), "a code that ran out must not work");

        String fresh = links.issueCode(STEVE, "Steve").orElseThrow();
        assertEquals(Status.LINKED, links.redeem("111", fresh).status());
        assertEquals(Status.WRONG_CODE, links.redeem("222", fresh).status(), "a used code must not work again");
    }

    @Test
    void askingAgainReplacesTheOldCode() {
        LinkService links = service();
        String first = links.issueCode(STEVE, "Steve").orElseThrow();
        String second = links.issueCode(STEVE, "Steve").orElseThrow();
        if (!first.equals(second)) {
            assertEquals(Status.WRONG_CODE, links.redeem("111", first).status());
        }
        assertEquals(Status.LINKED, links.redeem("111", second).status());
    }

    @Test
    void oneMinecraftAccountPerDiscordAccountAndBack() {
        LinkService links = service();
        links.redeem("111", links.issueCode(STEVE, "Steve").orElseThrow());

        assertTrue(links.issueCode(STEVE, "Steve").isEmpty(), "a linked player gets no new code");

        String alexCode = links.issueCode(ALEX, "Alex").orElseThrow();
        Redeem again = links.redeem("111", alexCode);
        assertEquals(Status.DISCORD_ALREADY_LINKED, again.status());
        assertEquals("Steve", again.link().name());
        assertTrue(links.byMinecraft(ALEX).isEmpty());
    }

    @Test
    void wrongCodesLockThatDiscordAccountForAWhile() {
        LinkService links = service();
        String real = links.issueCode(STEVE, "Steve").orElseThrow();
        String wrong = real.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 4; i++) {
            assertEquals(Status.WRONG_CODE, links.redeem("111", wrong).status());
        }
        Redeem locked = links.redeem("111", wrong);
        assertEquals(Status.LOCKED, locked.status());
        assertEquals(5, locked.minutes());
        assertEquals(Status.LOCKED, links.redeem("111", real).status(), "even the right code waits out the lock");
        assertEquals(Status.LINKED, links.redeem("222", real).status(), "another Discord account is not affected");

        now += 5 * 60_000 + 1;
        assertNotEquals(Status.LOCKED, links.redeem("111", wrong).status(), "the lock ends by itself");
    }

    @Test
    void unlinkingWorksFromBothSides() {
        LinkService links = service();
        links.redeem("111", links.issueCode(STEVE, "Steve").orElseThrow());
        assertEquals("111", links.unlinkMinecraft(STEVE).orElseThrow().discord());
        assertTrue(links.byDiscord("111").isEmpty());
        assertTrue(links.unlinkMinecraft(STEVE).isEmpty());

        links.redeem("111", links.issueCode(STEVE, "Steve").orElseThrow());
        assertEquals(STEVE, links.unlinkDiscord("111").orElseThrow().minecraft());
        assertFalse(service().byMinecraft(STEVE).isPresent(), "an undone link must not come back after a restart");
    }

    @Test
    void aNewMinecraftNameIsRemembered() {
        LinkService links = service();
        links.redeem("111", links.issueCode(STEVE, "Steve").orElseThrow());
        links.rename(STEVE, "Stephen");
        assertEquals("Stephen", service().byDiscord("111").orElseThrow().name());
    }

    @Test
    void codesAreShownInTwoGroups() {
        assertEquals("482 913", LinkService.pretty("482913"));
    }
}
