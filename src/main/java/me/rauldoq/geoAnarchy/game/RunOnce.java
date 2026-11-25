package me.rauldoq.geoAnarchy.game;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Console commands to run once, at the next start: plugins/GeoAnarchy/run-once.txt, one command per line.
 * Lines starting with # are notes and blank lines are skipped. Once the server has finished starting (so
 * LuckPerms and every other plugin is ready), each command runs from the console, in order, and the file is
 * renamed to run-once-&lt;date&gt;.done with the result written next to each command. Nothing runs twice.
 * <p>
 * This lets setup steps that need the console (LuckPerms groups, for example) be prepared as a file.
 * Whoever can upload files to the server could already replace plugins, so it gives nobody new powers.
 */
public final class RunOnce {

    public static final String FILE = "run-once.txt";

    private RunOnce() {
    }

    /** The commands in the file's lines: no notes, no blank lines, no leading "/". */
    public static List<String> commands(List<String> lines) {
        List<String> commands = new ArrayList<>();
        for (String line : lines) {
            String command = line.strip();
            if (command.isEmpty() || command.startsWith("#")) {
                continue;
            }
            while (command.startsWith("/")) {
                command = command.substring(1).strip();
            }
            if (!command.isEmpty()) {
                commands.add(command);
            }
        }
        return commands;
    }

    /**
     * Runs the file, if there is one, and moves it out of the way. {@code console} runs one command and says
     * whether the server knew it. Returns how many commands ran.
     */
    public static int run(Path folder, Predicate<String> console, Logger log) {
        Path file = folder.resolve(FILE);
        if (!Files.isRegularFile(file)) {
            return 0;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException error) {
            log.warning("Could not read " + FILE + ": " + error.getMessage() + ". Nothing was run.");
            return 0;
        }
        String when = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path done = folder.resolve("run-once-" + when + ".done");
        // Moved first: if a command crashes the server, the file can't run again at the next start.
        try {
            Files.move(file, done);
        } catch (IOException error) {
            log.warning("Could not rename " + FILE + " (" + error.getMessage() + "). Nothing was run, so it can't run twice.");
            return 0;
        }

        List<String> commands = commands(lines);
        List<String> report = new ArrayList<>();
        report.add("# Ran at the start of " + when + ". Each command's result is on the line under it.");
        int ran = 0;
        for (String command : commands) {
            boolean known;
            try {
                known = console.test(command);
            } catch (RuntimeException error) {
                known = false;
                log.warning("run-once: '" + command + "' failed: " + error.getMessage());
            }
            ran++;
            report.add(command);
            report.add(known ? "#   ok (see the console for what it printed)" : "#   the server did not know this command");
            log.info("run-once: " + command + (known ? "" : "  <- unknown command"));
        }
        try {
            Files.writeString(done, String.join("\n", report) + "\n", StandardCharsets.UTF_8);
        } catch (IOException error) {
            log.warning("Could not write the results to " + done.getFileName() + ": " + error.getMessage());
        }
        log.info("run-once: ran " + ran + " command(s) from " + FILE + ". The file is now " + done.getFileName() + ".");
        return ran;
    }
}
