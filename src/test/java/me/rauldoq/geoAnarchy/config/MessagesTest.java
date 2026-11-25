package me.rauldoq.geoAnarchy.config;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the real messages.yml that ships in the jar, and that the code only asks for texts that are in it. */
class MessagesTest {

    private static YamlConfiguration file;
    private static Messages messages;

    @BeforeAll
    static void load() {
        file = YamlConfiguration.loadConfiguration(new File("src/main/resources/messages.yml"));
        messages = new Messages(file, null);
    }

    private static String plain(String key, String... placeholders) {
        return PlainTextComponentSerializer.plainText().serialize(messages.game(key, placeholders));
    }

    @Test
    void everyGameTextRendersWithoutLeftoverFormatting() {
        int count = 0;
        for (String key : file.getConfigurationSection("game").getKeys(false)) {
            String text = plain(key);
            assertFalse(text.isBlank(), key + " is empty");
            assertFalse(text.contains("<prefix>") || text.contains("<gray>") || text.contains("<newline>"),
                    key + " has broken formatting: " + text);
            count++;
        }
        assertTrue(count > 25, "only " + count + " game texts were found");
    }

    @Test
    void valuesGoInAsPlainText() {
        assertEquals("[GeoAnarchy] Você recebeu 500.00 Geos de Steve (enviado pelo Discord).",
                plain("pay-received", "amount", "500.00 Geos", "player", "Steve"));
        // A name with formatting in it must show up as typed instead of changing how the line looks.
        assertTrue(plain("vip-unknown-player", "player", "<red>Evil</red>").contains("<red>Evil</red>"));
        assertEquals("Depositar **2,500.00 Geos** no banco de **Brasil**?\nSua carteira tem 9,000.00 Geos.",
                messages.text("deposit.confirm", "amount", "2,500.00 Geos", "country", "Brasil", "balance", "9,000.00 Geos"));
        assertTrue(plain("discord-usage").contains("/discord acoes <on|off>"), "usage text keeps its angle brackets");
    }

    @Test
    void aMissingTextIsVisibleInsteadOfSilent() {
        assertTrue(messages.text("no-such-text").contains("Texto ausente: discord.no-such-text"));
    }

    /**
     * Reads the plugin's own source code and collects every text it asks for. A text that is asked for but is not
     * in messages.yml would show up in game or in Discord as "Texto ausente".
     */
    @Test
    void everyTextTheCodeAsksForExists() throws IOException {
        String key = "\"([a-z0-9][a-z0-9.\\-]*[a-z0-9])\"";
        // The first argument is a text name, or "condition ? name : otherName". It never reaches past a comma.
        String firstArgument = "[^\";,]*?" + key + "(?:\\s*:\\s*" + key + ")?";
        Pattern discord = Pattern.compile("\\btext\\(\\s*" + firstArgument);
        Pattern alert = Pattern.compile("\\balert\\(\\s*" + firstArgument);
        Pattern game = Pattern.compile("\\b(?:send\\(\\s*\\w+\\s*,|game\\(|link\\(\\s*sender\\s*,)\\s*" + firstArgument);

        Set<String> wanted = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                collect(discord.matcher(source), "discord.", wanted);
                collect(game.matcher(source), "game.", wanted);
                Matcher alerts = alert.matcher(source);
                while (alerts.find()) {
                    for (int group = 1; group <= 2; group++) {
                        if (alerts.group(group) != null) {
                            wanted.add("discord.alert." + alerts.group(group));
                            wanted.add("discord.alert." + alerts.group(group).split("\\.")[0] + ".title");
                        }
                    }
                }
            }
        }
        assertTrue(wanted.size() > 80, "the scan found only " + wanted.size() + " texts, so it is probably broken");

        Set<String> missing = new TreeSet<>();
        for (String wantedKey : wanted) {
            if (!file.isString(wantedKey)) {
                missing.add(wantedKey);
            }
        }
        assertTrue(missing.isEmpty(), "the code asks for texts that messages.yml does not have: " + missing);
    }

    private static void collect(Matcher matcher, String prefix, Set<String> into) {
        while (matcher.find()) {
            for (int group = 1; group <= 2; group++) {
                if (matcher.group(group) != null) {
                    into.add(prefix + matcher.group(group));
                }
            }
        }
    }

    /** The alert switches build their text names from the kind, so they are checked by hand. */
    @Test
    void everyAlertSwitchHasItsTexts() {
        for (String id : new String[]{"guerra", "manutencao", "diplomacia", "convites", "cidadao"}) {
            assertTrue(file.isString("discord.notify." + id + ".name"), id);
            assertTrue(file.isString("discord.notify." + id + ".about"), id);
        }
        // The diplomacy alert picks its text from the kind of request.
        for (String kind : new String[]{"ally", "enemy", "neutral", "title"}) {
            assertTrue(file.isString("discord.alert.relation." + kind), kind);
        }
    }
}
