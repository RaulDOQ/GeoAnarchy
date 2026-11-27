package me.rauldoq.geoAnarchy.economy;

import me.rauldoq.geoAnarchy.config.Messages;
import me.rauldoq.geoAnarchy.storage.Database;
import me.rauldoq.geoAnarchy.storage.Database.Payment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The /pagar payments confirmed on Discord, made safe to click twice, to retry, and to survive a restart.
 * <p>
 * Each payment has a stable id (its Confirmar button's id), saved in geoanarchy.db from the moment it waits for
 * the click. Its money moves through {@link Economy#payOnce} with that id, which GeoEconomy saves together with the
 * transfer, so it can never move twice. Then the payment is booked in one transaction: the day's spending, the
 * receipt shown at the next login, and the answer Discord gets. If anything fails between the transfer and the
 * booking, the next click on the same button (or {@link #reconcile}, which runs on its own) finds the money already
 * sent and finishes the booking, against the day the money was sent.
 * <p>
 * Call it from the server thread.
 */
public final class Payments {

    private static final ZoneId BRAZIL = ZoneId.of("America/Sao_Paulo");
    /** Finished payments are forgotten after this long. */
    private static final long KEEP_MILLIS = 30L * 24 * 60 * 60 * 1000;

    /**
     * What a click got.
     * @param paidNow    true when the money moved during this click (so the receiver can be told in game)
     * @param receiverId the receiver, when known
     */
    public record Outcome(String text, boolean paidNow, UUID receiverId, String receiverName) {
        static Outcome say(String text) {
            return new Outcome(text, false, null, null);
        }
    }

    private final Database database;
    private final Supplier<Economy> economy;
    private final Supplier<Messages> messages;
    private final LongSupplier clock;
    private final LongSupplier dailyLimit;
    private final Logger log;

    /** @param dailyLimit the most a player may send through Discord in one day, in cents */
    public Payments(Database database, Supplier<Economy> economy, Supplier<Messages> messages, LongSupplier clock,
                    LongSupplier dailyLimit, Logger log) {
        this.database = database;
        this.economy = economy;
        this.messages = messages;
        this.clock = clock;
        this.dailyLimit = dailyLimit;
        this.log = log;
    }

    /** The day in Brazil at this moment, as "2026-11-27". The daily limit counts by it. */
    public String today() {
        return LocalDate.ofInstant(Instant.ofEpochMilli(clock.getAsLong()), BRAZIL).toString();
    }

    /** What is left of the player's daily limit on that day, in cents. */
    public long leftOn(UUID player, String day) {
        return dailyLimit.getAsLong() - database.spent(player, day);
    }

    /** Saves a new payment waiting for its button. Older payments of the same Discord user still waiting are dropped. */
    public void create(String id, String discord, UUID player, String name, String target, long cents, long expiresAt) {
        database.replaceWaitingPayments(discord, messages.get().text("action.expired"));
        database.addPayment(new Payment(id, discord, player, name, target, cents, clock.getAsLong(), expiresAt,
                null, null, Database.WAITING, null));
    }

    public Optional<Payment> find(String id) {
        return database.payment(id);
    }

    /**
     * A click on the payment's Confirmar button, the first or a repeated one.
     *
     * @param check the checks to pass before money moves (the account is still linked, Discord actions are on);
     *              it returns why not, or null
     */
    public Outcome confirm(String id, Supplier<String> check) {
        Messages texts = messages.get();
        Economy money = economy.get();
        Optional<Payment> found = database.payment(id);
        if (found.isEmpty()) {
            return Outcome.say(texts.text("action.expired"));
        }
        Payment payment = found.get();
        if (Database.BOOKED.equals(payment.state()) || Database.REFUSED.equals(payment.state())) {
            // Already decided: the same answer again, and nothing else happens.
            return new Outcome(payment.result(), false, null, payment.receiver());
        }
        if (!money.paid(id)) {
            String refused = refusal(payment, check, texts);
            if (refused != null) {
                database.refusePayment(id, refused);
                return Outcome.say(refused);
            }
            database.startPayment(id, payment.day() != null ? payment.day() : today());
        }
        Economy.Pay pay = money.payOnce(id, payment.minecraft(), payment.target(), payment.cents());
        return switch (pay.status()) {
            case PAID, ALREADY_PAID -> book(payment, pay.receiver(), pay.receiverId(), pay.status() == Economy.PayStatus.PAID);
            default -> {
                String text = failure(pay, payment, texts, money);
                database.refusePayment(id, text);
                yield Outcome.say(text);
            }
        };
    }

    /** Why a payment whose money has not moved must not move now, or null. */
    private String refusal(Payment payment, Supplier<String> check, Messages texts) {
        if (payment.expiresAt() <= clock.getAsLong()) {
            return texts.text("action.expired");
        }
        String blocked = check.get();
        if (blocked != null) {
            return blocked;
        }
        String day = payment.day() != null ? payment.day() : today();
        long left = leftOn(payment.minecraft(), day);
        if (payment.cents() > left) {
            Economy money = economy.get();
            return texts.text("pay.limit", "limit", money.geos(dailyLimit.getAsLong()), "left", money.geos(Math.max(0, left)));
        }
        return null;
    }

    private static String failure(Economy.Pay pay, Payment payment, Messages texts, Economy money) {
        return switch (pay.status()) {
            case NO_SUCH_PLAYER -> texts.text("pay.no-player", "player", payment.target());
            case SAME_PLAYER -> texts.text("pay.self");
            case NOT_ENOUGH -> texts.text("action.not-enough", "balance", money.geos(money.geoBalance(payment.minecraft())));
            case RECEIVER_FULL -> texts.text("pay.receiver-full", "player", pay.receiver());
            default -> texts.text("error");
        };
    }

    /** The money moved (now or before): count it, give the receipt, keep the answer. */
    private Outcome book(Payment payment, String receiverName, UUID receiverId, boolean paidNow) {
        Messages texts = messages.get();
        Economy money = economy.get();
        String receiver = receiverName != null ? receiverName : payment.target();
        String amount = money.geos(payment.cents());
        String receipt = texts.text("log.pay", "amount", amount, "player", receiver);
        String done = texts.text("pay.done", "amount", amount, "player", receiver,
                "balance", money.geos(money.geoBalance(payment.minecraft())));
        try {
            if (!database.bookPayment(payment.id(), receiver, receipt, done, clock.getAsLong(), today())) {
                // Booked by an earlier click or by reconcile in the meantime: its answer stands.
                return new Outcome(database.payment(payment.id()).map(Payment::result).orElse(done), false, receiverId, receiver);
            }
        } catch (RuntimeException failure) {
            log.log(Level.WARNING, "Payment " + payment.id() + " was sent but could not be booked yet; it will be booked later.", failure);
            return new Outcome(texts.text("pay.not-recorded", "amount", amount, "player", receiver), paidNow, receiverId, receiver);
        }
        return new Outcome(done, paidNow, receiverId, receiver);
    }

    /**
     * Finishes what a failure or a restart left open: a payment whose money was sent but not booked is booked now
     * (no click needed), and one that never sent its money and ran out of time is refused. Old finished payments
     * are forgotten. Does nothing while the economy is away, since it can't tell which payments were sent.
     *
     * @return how many payments it booked
     */
    public int reconcile() {
        Economy money = economy.get();
        if (!money.available()) {
            return 0;
        }
        int booked = 0;
        long now = clock.getAsLong();
        for (Payment payment : database.paymentsIn(Database.PAYING)) {
            if (money.paid(payment.id())) {
                Outcome outcome = book(payment, money.accountName(payment.target()).orElse(payment.target()), null, false);
                if (database.payment(payment.id()).map(found -> Database.BOOKED.equals(found.state())).orElse(false)) {
                    booked++;
                    log.info("Booked a Discord payment that was sent before a failure or restart: " + payment.id()
                            + " (" + outcome.text().lines().findFirst().orElse("") + ")");
                }
            } else if (payment.expiresAt() <= now) {
                database.refusePayment(payment.id(), messages.get().text("action.expired"));
            }
        }
        for (Payment payment : database.paymentsIn(Database.WAITING)) {
            if (payment.expiresAt() <= now) {
                database.refusePayment(payment.id(), messages.get().text("action.expired"));
            }
        }
        database.forgetPayments(now - KEEP_MILLIS);
        return booked;
    }
}
