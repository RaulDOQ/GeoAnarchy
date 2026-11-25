package me.rauldoq.geoAnarchy.bot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.logging.Logger;

/** Discord keeps the subscriptions on members. Only the panel and channel IDs live here. */
final class UpdateSettings {
    record Binding(long role, long rolesChannel, long updatesChannel, long panel) {}

    private final Path path;
    private final Properties guilds = new Properties();
    private boolean available = true;

    UpdateSettings(Path path, Logger logger) {
        this.path = path;
        if (Files.exists(path)) {
            try (var input = Files.newInputStream(path)) {
                guilds.load(input);
                for (String guild : guilds.stringPropertyNames()) {
                    get(Long.parseLong(guild));
                }
            } catch (IOException | IllegalArgumentException error) {
                logger.warning("Falha ao ler " + path + ": " + error.getMessage());
                guilds.clear();
                available = false;
            }
        }
    }

    synchronized Binding get(long guild) {
        String value = guilds.getProperty(Long.toString(guild));
        if (value == null) return null;
        String[] ids = value.split(",", -1);
        if (ids.length != 4) throw new IllegalArgumentException("Registro de atualizações inválido.");
        return new Binding(Long.parseLong(ids[0]), Long.parseLong(ids[1]),
                Long.parseLong(ids[2]), Long.parseLong(ids[3]));
    }

    synchronized void save(long guild, Binding binding) {
        requireAvailable();
        Properties next = new Properties();
        next.putAll(guilds);
        String value = binding.role() + "," + binding.rolesChannel() + ","
                + binding.updatesChannel() + "," + binding.panel();
        next.setProperty(Long.toString(guild), value);
        try {
            Files.createDirectories(path.getParent());
            Path temp = Files.createTempFile(path.getParent(), "discord-updates-", ".tmp");
            try {
                try (var output = Files.newOutputStream(temp)) {
                    next.store(output, "GeoAnarchy Discord update subscriptions");
                }
                try {
                    Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                }
                guilds.setProperty(Long.toString(guild), value);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    void requireAvailable() {
        if (!available) throw new IllegalStateException("Confira o arquivo discord-updates.properties e reinicie o plugin.");
    }
}
