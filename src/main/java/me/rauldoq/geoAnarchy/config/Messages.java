package me.rauldoq.geoAnarchy.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * The texts of messages.yml. There are two kinds:
 * <ul>
 *   <li>"game." texts are shown in Minecraft. They use MiniMessage, and values go in as {@code <name>}.</li>
 *   <li>"discord." texts are shown in Discord. They are plain text (Discord's own **bold** works), and
 *       values go in as {@code {name}}.</li>
 * </ul>
 * Values are always inserted as plain text, so a player or country name can never change the formatting.
 */
public final class Messages {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Map<String, String> texts = new HashMap<>();

    /** @param defaults the messages.yml inside the jar, so a text missing from the server's file still works */
    public Messages(ConfigurationSection file, ConfigurationSection defaults) {
        if (defaults != null) {
            read(defaults);
        }
        if (file != null) {
            read(file);
        }
    }

    private void read(ConfigurationSection section) {
        for (String key : section.getKeys(true)) {
            if (section.isString(key)) {
                texts.put(key, section.getString(key));
            }
        }
    }

    public boolean has(String key) {
        return texts.containsKey(key);
    }

    private String raw(String key) {
        return texts.getOrDefault(key, "Texto ausente: " + key);
    }

    /** A game message. Placeholders come in name, value pairs: "player", "Steve". */
    public Component game(String key, String... placeholders) {
        TagResolver.Builder resolver = TagResolver.builder()
                .resolver(Placeholder.parsed("prefix", raw("game.prefix")));
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            resolver.resolver(Placeholder.unparsed(placeholders[i], placeholders[i + 1] == null ? "" : placeholders[i + 1]));
        }
        return MINI.deserialize(raw("game." + key), resolver.build());
    }

    public void send(CommandSender to, String key, String... placeholders) {
        to.sendMessage(game(key, placeholders));
    }

    /** A Discord text. Placeholders come in name, value pairs and replace {name}. */
    public String text(String key, String... placeholders) {
        String text = raw("discord." + key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace("{" + placeholders[i] + "}", placeholders[i + 1] == null ? "" : placeholders[i + 1]);
        }
        return text;
    }
}
