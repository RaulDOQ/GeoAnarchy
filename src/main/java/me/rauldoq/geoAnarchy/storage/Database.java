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

    /** A player with paid VIP months left. The current month started at periodStart. */
    public record Vip(UUID minecraft, String name, int months, long periodStart) {
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
            statement.execute("CREATE TABLE IF NOT EXISTS vips (minecraft TEXT PRIMARY KEY, name TEXT NOT NULL, "
                    + "months INTEGER NOT NULL, period_start INTEGER NOT NULL)");
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

    /** Returns what the player has not been shown yet, oldest first, and marks it as shown. */
    public synchronized List<String> takeUnseenLog(UUID player) {
        List<String> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT text FROM discord_log WHERE minecraft = ? AND seen = 0 ORDER BY id LIMIT 20")) {
            statement.setString(1, player.toString());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(rows.getString(1));
                }
            }
        } catch (SQLException error) {
            throw new IllegalStateException(error);
        }
        if (!result.isEmpty()) {
            update("UPDATE discord_log SET seen = 1 WHERE minecraft = ? AND seen = 0", player.toString());
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

    // ---- VIPs ----

    public synchronized List<Vip> vips() {
        List<Vip> result = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT minecraft, name, months, period_start FROM vips")) {
            while (rows.next()) {
                result.add(new Vip(UUID.fromString(rows.getString(1)), rows.getString(2), rows.getInt(3), rows.getLong(4)));
            }
        } catch (SQLException error) {
            throw new IllegalStateException(error);
        }
        return result;
    }

    public synchronized void saveVip(Vip vip) {
        update("INSERT OR REPLACE INTO vips (minecraft, name, months, period_start) VALUES (?, ?, ?, ?)",
                vip.minecraft().toString(), vip.name(), vip.months(), vip.periodStart());
    }

    public synchronized void deleteVip(UUID minecraft) {
        update("DELETE FROM vips WHERE minecraft = ?", minecraft.toString());
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
