package me.rauldoq.geoAnarchy.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Everything the plugin remembers, in one SQLite file (plugins/GeoAnarchy/geoanarchy.db).
 * Every method is safe to call from the server thread and from Discord's threads: one at a time.
 * The amounts of data are tiny (a row per linked player), so each call finishes at once.
 */
public final class Database implements AutoCloseable {

    /** A Minecraft account linked to a Discord account. */
    public record Link(UUID minecraft, String name, String discord, long linkedAt) {
    }

    private final Connection connection;

    public Database(Path file) throws SQLException {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
        } catch (java.io.IOException error) {
            throw new SQLException("Could not create the folder for " + file, error);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("CREATE TABLE IF NOT EXISTS links (minecraft TEXT PRIMARY KEY, name TEXT NOT NULL, "
                    + "discord TEXT NOT NULL UNIQUE, linked_at INTEGER NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS settings (minecraft TEXT NOT NULL, key TEXT NOT NULL, "
                    + "value TEXT NOT NULL, PRIMARY KEY (minecraft, key))");
            statement.execute("CREATE TABLE IF NOT EXISTS discord_log (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "minecraft TEXT NOT NULL, text TEXT NOT NULL, at INTEGER NOT NULL, seen INTEGER NOT NULL DEFAULT 0)");
            statement.execute("CREATE TABLE IF NOT EXISTS spending (minecraft TEXT NOT NULL, day TEXT NOT NULL, "
                    + "cents INTEGER NOT NULL, PRIMARY KEY (minecraft, day))");
            // A /pagar from the moment it waits for its Confirmar button until it is booked or refused.
            statement.execute("CREATE TABLE IF NOT EXISTS payments (id TEXT PRIMARY KEY, discord TEXT NOT NULL, "
                    + "minecraft TEXT NOT NULL, name TEXT NOT NULL, target TEXT NOT NULL, cents INTEGER NOT NULL, "
                    + "created INTEGER NOT NULL, expires INTEGER NOT NULL, day TEXT, receiver TEXT, "
                    + "state TEXT NOT NULL, result TEXT)");
        }
    }

    // ---- Links ----

    public synchronized List<Link> links() {
        List<Link> result = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT minecraft, name, discord, linked_at FROM links")) {
            while (rows.next()) {
                result.add(new Link(UUID.fromString(rows.getString(1)), rows.getString(2), rows.getString(3), rows.getLong(4)));
            }
        } catch (SQLException error) {
            throw new IllegalStateException(error);
        }
        return result;
    }

    public synchronized void saveLink(Link link) {
        update("INSERT OR REPLACE INTO links (minecraft, name, discord, linked_at) VALUES (?, ?, ?, ?)",
                link.minecraft().toString(), link.name(), link.discord(), link.linkedAt());
    }

    public synchronized void deleteLink(UUID minecraft) {
        update("DELETE FROM links WHERE minecraft = ?", minecraft.toString());
    }

    // ---- One setting per player and name (alert switches, Discord actions on or off) ----

    public synchronized Optional<String> setting(UUID player, String key) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT value FROM settings WHERE minecraft = ? AND key = ?")) {
            statement.setString(1, player.toString());
            statement.setString(2, key);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(rows.getString(1)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException(error);
        }
    }

    /** A null value forgets the setting, which puts it back to its default. */
    public synchronized void setSetting(UUID player, String key, String value) {
        if (value == null) {
            update("DELETE FROM settings WHERE minecraft = ? AND key = ?", player.toString(), key);
        } else {
            update("INSERT OR REPLACE INTO settings (minecraft, key, value) VALUES (?, ?, ?)", player.toString(), key, value);
        }
    }

    // ---- What a player did through Discord, to show them at their next login ----

    public synchronized void log(UUID player, String text, long at) {
        update("INSERT INTO discord_log (minecraft, text, at) VALUES (?, ?, ?)", player.toString(), text, at);
    }

    /** At most this many lines are shown at one login. The rest wait, in order, for the next login. */
    static final int LOG_PER_LOGIN = 20;

    /** One line of the Discord log: its row in the table and its text. */
    record LogLine(long id, String text) {
    }

    /**
     * Returns what the player has not been shown yet, oldest first and at most {@link #LOG_PER_LOGIN} lines,
     * and marks exactly those lines as shown. Anything that did not fit stays unseen for the next login.
     */
    public synchronized List<String> takeUnseenLog(UUID player) {
        return takeUnseenLines(player).stream().map(LogLine::text).toList();
    }

    /**
     * The lines behind {@link #takeUnseenLog}. Picking the lines and marking them happen in one transaction, and
     * only the ids that were picked get marked, so a line is marked as shown only if it is handed back.
     */
    synchronized List<LogLine> takeUnseenLines(UUID player) {
        List<LogLine> result = new ArrayList<>();
        try {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT id, text FROM discord_log WHERE minecraft = ? AND seen = 0 ORDER BY id LIMIT ?")) {
                    select.setString(1, player.toString());
                    select.setInt(2, LOG_PER_LOGIN);
                    try (ResultSet rows = select.executeQuery()) {
                        while (rows.next()) {
                            result.add(new LogLine(rows.getLong(1), rows.getString(2)));
                        }
                    }
                }
                if (!result.isEmpty()) {
                    try (PreparedStatement mark = connection.prepareStatement("UPDATE discord_log SET seen = 1 WHERE id = ?")) {
                        for (LogLine line : result) {
                            mark.setLong(1, line.id());
                            mark.addBatch();
                        }
                        mark.executeBatch();
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException error) {
            throw new IllegalStateException(error);
        }
        return result;
    }

    // ---- How much a player has paid through Discord on a given day ----

    public synchronized long spent(UUID player, String day) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT cents FROM spending WHERE minecraft = ? AND day = ?")) {
            statement.setString(1, player.toString());
            statement.setString(2, day);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong(1) : 0;
            }
        } catch (SQLException error) {
            throw new IllegalStateException(error);
        }
    }

    public synchronized void addSpent(UUID player, String day, long cents) {
        update("INSERT INTO spending (minecraft, day, cents) VALUES (?, ?, ?) "
                + "ON CONFLICT (minecraft, day) DO UPDATE SET cents = cents + excluded.cents", player.toString(), day, cents);
    }

    // ---- Payments confirmed on Discord (/pagar) ----

    /**
     * One /pagar. Its id is the id of its Confirmar button.
     * @param day   the day its money counts against the daily limit, fixed when the money is first sent
     * @param state {@link #WAITING}, {@link #PAYING}, {@link #BOOKED} or {@link #REFUSED}
     * @param result what Discord was told, once it is booked or refused, so a repeated click gets the same answer
     */
    public record Payment(String id, String discord, UUID minecraft, String name, String target, long cents,
                          long created, long expiresAt, String day, String receiver, String state, String result) {
    }

    /** Waiting for its Confirmar button. */
    public static final String WAITING = "waiting";
    /** Its money is being sent, or was sent and not booked yet (the server or the bookkeeping failed in between). */
    public static final String PAYING = "paying";
    /** Sent, added to the day's spending and given its receipt. Final. */
    public static final String BOOKED = "booked";
    /** Never sent: expired, blocked, over the limit or refused by the economy. Final. */
    public static final String REFUSED = "refused";

    /** Tests only: the steps of {@link #bookPayment} at which to fail as if the disk refused. */
    private volatile java.util.function.Predicate<String> failAt = step -> false;

    /** Tests only: makes {@link #bookPayment} fail at the named step ("start", "after-spending"). */
    public void failAt(java.util.function.Predicate<String> steps) {
        failAt = steps;
    }

    private void step(String name) throws SQLException {
        if (failAt.test(name)) {
            throw new SQLException("failed on purpose at " + name + " (test)");
        }
    }

    public synchronized void addPayment(Payment payment) {
        update("INSERT INTO payments (id, discord, minecraft, name, target, cents, created, expires, state) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", payment.id(), payment.discord(), payment.minecraft().toString(),
                payment.name(), payment.target(), payment.cents(), payment.created(), payment.expiresAt(), WAITING);
    }

    public synchronized Optional<Payment> payment(String id) {
        List<Payment> found = payments("WHERE id = ?", id);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    /** Every payment in one state, oldest first. */
    public synchronized List<Payment> paymentsIn(String state) {
        return payments("WHERE state = ? ORDER BY created", state);
    }

    private List<Payment> payments(String where, Object... values) {
        List<Payment> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT id, discord, minecraft, name, target, cents, "
                + "created, expires, day, receiver, state, result FROM payments " + where)) {
            for (int i = 0; i < values.length; i++) {
                statement.setObject(i + 1, values[i]);
            }
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new Payment(rows.getString(1), rows.getString(2), UUID.fromString(rows.getString(3)),
                            rows.getString(4), rows.getString(5), rows.getLong(6), rows.getLong(7), rows.getLong(8),
                            rows.getString(9), rows.getString(10), rows.getString(11), rows.getString(12)));
                }
            }
        } catch (SQLException error) {
            throw new IllegalStateException(error);
        }
        return result;
    }

    /** The money is about to be sent. The first time, this also fixes the day the payment counts against. */
    public synchronized void startPayment(String id, String day) {
        update("UPDATE payments SET state = ?, day = COALESCE(day, ?) WHERE id = ? AND state IN (?, ?)",
                PAYING, day, id, WAITING, PAYING);
    }

    /** The payment will never be sent. A booked payment is never changed. */
    public synchronized void refusePayment(String id, String result) {
        update("UPDATE payments SET state = ?, result = ? WHERE id = ? AND state <> ?", REFUSED, result, id, BOOKED);
    }

    /** A newer /pagar of the same Discord user replaces their payments still waiting for a click. */
    public synchronized void replaceWaitingPayments(String discord, String result) {
        update("UPDATE payments SET state = ?, result = ? WHERE discord = ? AND state = ?", REFUSED, result, discord, WAITING);
    }

    /** Forgets finished payments older than this, so the table stays small. */
    public synchronized void forgetPayments(long before) {
        update("DELETE FROM payments WHERE created < ? AND state IN (?, ?)", before, BOOKED, REFUSED);
    }

    /**
     * Books a payment whose money was sent: adds it to the spending of its day, writes the receipt shown at the
     * next login, stores what Discord is told and marks it booked. All in one transaction, so either all of it is
     * saved or none, and a payment already booked is left alone: retrying never counts or receipts twice.
     *
     * @param fallbackDay the day to count against if the payment never got one (it always does when it is started)
     * @return true if it was booked now, false if it already was
     */
    public synchronized boolean bookPayment(String id, String receiver, String receipt, String result, long at, String fallbackDay) {
        try {
            connection.setAutoCommit(false);
            try {
                step("start");
                Payment payment = payments("WHERE id = ?", id).stream().findFirst()
                        .orElseThrow(() -> new SQLException("no payment " + id));
                if (BOOKED.equals(payment.state())) {
                    connection.rollback();
                    return false;
                }
                String day = payment.day() != null ? payment.day() : fallbackDay;
                update("INSERT INTO spending (minecraft, day, cents) VALUES (?, ?, ?) "
                        + "ON CONFLICT (minecraft, day) DO UPDATE SET cents = cents + excluded.cents",
                        payment.minecraft().toString(), day, payment.cents());
                step("after-spending");
                update("INSERT INTO discord_log (minecraft, text, at) VALUES (?, ?, ?)", payment.minecraft().toString(), receipt, at);
                update("UPDATE payments SET state = ?, day = ?, receiver = ?, result = ? WHERE id = ?",
                        BOOKED, day, receiver, result, id);
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error instanceof RuntimeException runtime ? runtime : new IllegalStateException(error);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException error) {
            throw new IllegalStateException(error);
        }
    }

    private void update(String sql, Object... values) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                statement.setObject(i + 1, values[i]);
            }
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException(error);
        }
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // nothing left to do
        }
    }
}
