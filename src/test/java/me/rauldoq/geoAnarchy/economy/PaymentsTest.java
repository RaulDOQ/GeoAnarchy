package me.rauldoq.geoAnarchy.economy;

import me.rauldoq.geoAnarchy.config.Messages;
import me.rauldoq.geoAnarchy.storage.Database;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A /pagar is paid once, counted once against the day it was paid, and given one receipt, whatever fails in between. */
class PaymentsTest {

    private static final Logger QUIET = Logger.getLogger("PaymentsTest");
    private static final ZoneId BRAZIL = ZoneId.of("America/Sao_Paulo");
    private static final long LIMIT = 1_000_000;
    private static final UUID STEVE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-000000000002");

    static {
        QUIET.setUseParentHandlers(false);
    }

    @TempDir
    Path folder;

    private final long[] now = {at(2026, 11, 27, 23, 50)};
    private final FakeEconomy economy = new FakeEconomy();
    private Messages messages;
    private Database database;
    private Payments payments;

    private static long at(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(BRAZIL).toInstant().toEpochMilli();
    }

    @BeforeEach
    void open() throws Exception {
        messages = new Messages(YamlConfiguration.loadConfiguration(new File("src/main/resources/messages.yml")), null);
        economy.balances.put(STEVE, 500_000L);
        economy.balances.put(ALEX, 0L);
        start();
    }

    /** Opens the database and the service, as a server start does. */
    private void start() throws Exception {
        database = new Database(folder.resolve("geoanarchy.db"));
        payments = new Payments(database, () -> economy, () -> messages, () -> now[0], () -> LIMIT, QUIET);
    }

    private void restart() throws Exception {
        database.close();
        start();
    }

    @AfterEach
    void close() {
        database.close();
    }

    private String newPayment(String id, long cents) {
        payments.create(id, "discord-steve", STEVE, "Steve", "Alex", cents, now[0] + 60_000);
        return id;
    }

    private Payments.Outcome click(String id) {
        return payments.confirm(id, () -> null);
    }

    private String done(long cents, long balance) {
        return messages.text("pay.done", "amount", economy.geos(cents), "player", "Alex", "balance", economy.geos(balance));
    }

    @Test
    void aPaymentIsPaidOnceAndClickingAgainGetsTheSameAnswer() {
        String id = newPayment("geo:act:one", 20_000);
        Payments.Outcome first = click(id);
        assertEquals(done(20_000, 480_000), first.text());
        assertTrue(first.paidNow());
        assertEquals(ALEX, first.receiverId());

        Payments.Outcome again = click(id);
        assertEquals(first.text(), again.text(), "a second click hears what the first one did");
        assertFalse(again.paidNow());
        assertEquals(1, economy.transfers);
        assertEquals(20_000, database.spent(STEVE, "2026-11-27"));
        assertEquals(List.of(messages.text("log.pay", "amount", economy.geos(20_000), "player", "Alex")), database.takeUnseenLog(STEVE));
    }

    @Test
    void aFailureRightAfterTheTransferIsFinishedByTheSameClickAfterARestartOnTheOriginalDay() throws Exception {
        String id = newPayment("geo:act:late", 30_000);
        database.failAt(step -> step.equals("start"));
        Payments.Outcome failed = click(id);
        assertEquals(messages.text("pay.not-recorded", "amount", economy.geos(30_000), "player", "Alex"), failed.text(),
                "Discord is told the money went, not that something went wrong");
        assertEquals(1, economy.transfers);
        assertEquals(0, database.spent(STEVE, "2026-11-27"));

        // The server restarts, and the player presses the same button after midnight.
        restart();
        now[0] = at(2026, 11, 28, 0, 20);
        Payments.Outcome replay = click(id);
        assertEquals(done(30_000, 470_000), replay.text());
        assertFalse(replay.paidNow(), "nothing moved this time");
        assertEquals(1, economy.transfers, "one transfer");
        assertEquals(30_000, database.spent(STEVE, "2026-11-27"), "counted against the day it was paid");
        assertEquals(0, database.spent(STEVE, "2026-11-28"));
        assertEquals(1, database.takeUnseenLog(STEVE).size(), "one receipt");
        assertEquals(replay.text(), click(id).text());
    }

    @Test
    void aFailureRightAfterTheDaysSpendingLeavesNothingHalfSavedAndIsBookedWithoutAClick() throws Exception {
        String id = newPayment("geo:act:half", 40_000);
        database.failAt(step -> step.equals("after-spending"));
        click(id);
        assertEquals(1, economy.transfers);
        assertEquals(0, database.spent(STEVE, "2026-11-27"), "the spending was undone with the rest");
        database.failAt(step -> false);

        // Nobody clicks again: the server books it by itself after a restart.
        restart();
        now[0] = at(2026, 11, 28, 9, 0);
        assertEquals(1, payments.reconcile());
        assertEquals(40_000, database.spent(STEVE, "2026-11-27"));
        assertEquals(1, database.takeUnseenLog(STEVE).size());
        assertEquals(0, payments.reconcile(), "nothing left to book");
        // A late click on the old button then just shows what happened.
        assertEquals(done(40_000, 460_000), click(id).text());
        assertEquals(1, economy.transfers);
        assertEquals(40_000, database.spent(STEVE, "2026-11-27"));
    }

    @Test
    void aNewPaymentOfTheSameAmountToTheSamePersonStillGoesThrough() {
        click(newPayment("geo:act:a", 10_000));
        click(newPayment("geo:act:b", 10_000));
        assertEquals(2, economy.transfers);
        assertEquals(480_000, economy.balances.get(STEVE));
        assertEquals(20_000, database.spent(STEVE, "2026-11-27"));
        assertEquals(2, database.takeUnseenLog(STEVE).size());
    }

    @Test
    void anExpiredOrOverTheLimitPaymentIsRefusedForGood() {
        String late = newPayment("geo:act:expired", 1_000);
        now[0] += 120_000;
        assertEquals(messages.text("action.expired"), click(late).text());
        String big = newPayment("geo:act:big", LIMIT + 1);
        String refused = click(big).text();
        assertTrue(refused.contains(economy.geos(LIMIT)), refused);
        assertEquals(refused, click(big).text());
        // A blocked check (unlinked, actions off) is refused too, and nothing moves.
        String blocked = newPayment("geo:act:blocked", 1_000);
        assertEquals("bloqueado", payments.confirm(blocked, () -> "bloqueado").text());
        assertEquals(0, economy.transfers);
        assertEquals(0, database.spent(STEVE, "2026-11-27"));
    }

    @Test
    void aNewRequestReplacesTheOneStillWaitingAndNothingIsDoneWithoutTheEconomy() {
        String old = newPayment("geo:act:old", 1_000);
        newPayment("geo:act:new", 2_000);
        assertEquals(messages.text("action.expired"), click(old).text());

        String waiting = newPayment("geo:act:wait", 3_000);
        database.startPayment(waiting, "2026-11-27");
        economy.available = false;
        now[0] += 120_000;
        assertEquals(0, payments.reconcile(), "without the economy it can't tell what was sent, so it waits");
        assertEquals(Database.PAYING, database.payment(waiting).orElseThrow().state());
        economy.available = true;
        payments.reconcile();
        assertEquals(Database.REFUSED, database.payment(waiting).orElseThrow().state(), "never sent and out of time");
    }

    /** An economy that keeps balances in memory and, like GeoEconomy, pays each payment id once. */
    private static final class FakeEconomy implements Economy {
        final Map<UUID, Long> balances = new HashMap<>();
        final Set<String> paid = new HashSet<>();
        int transfers;
        boolean available = true;

        @Override
        public boolean available() {
            return available;
        }

        @Override
        public OptionalLong parse(String typed) {
            return OptionalLong.empty();
        }

        @Override
        public String geos(long cents) {
            return Economy.NONE.geos(cents);
        }

        @Override
        public long geoBalance(UUID player) {
            return balances.getOrDefault(player, 0L);
        }

        @Override
        public List<String> balances(UUID player) {
            return List.of();
        }

        @Override
        public boolean take(UUID player, long cents, String reason) {
            return false;
        }

        @Override
        public boolean give(UUID player, long cents, String reason) {
            return true;
        }

        @Override
        public Optional<String> accountName(String typedName) {
            return "alex".equalsIgnoreCase(typedName) ? Optional.of("Alex") : Optional.empty();
        }

        @Override
        public Pay pay(UUID from, String toName, long cents) {
            throw new AssertionError("payments must go through payOnce");
        }

        @Override
        public Pay payOnce(String paymentId, UUID from, String toName, long cents) {
            if (paid.contains(paymentId)) {
                return new Pay(PayStatus.ALREADY_PAID, "Alex", ALEX);
            }
            if (geoBalance(from) < cents) {
                return new Pay(PayStatus.NOT_ENOUGH, "Alex", ALEX);
            }
            balances.merge(from, -cents, Long::sum);
            balances.merge(ALEX, cents, Long::sum);
            paid.add(paymentId);
            transfers++;
            return new Pay(PayStatus.PAID, "Alex", ALEX);
        }

        @Override
        public boolean paid(String paymentId) {
            return paid.contains(paymentId);
        }

        @Override
        public List<Price> prices(String item) {
            return List.of();
        }
    }
}
