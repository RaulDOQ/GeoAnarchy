package me.rauldoq.geoAnarchy.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunOnceTest {

    private static final Logger LOG = Logger.getLogger("test");

    @Test
    void notesAndBlankLinesAreSkippedAndSlashesDropped() {
        assertEquals(List.of("lp creategroup vipperks", "lp group default permission set gsit.* false"),
                RunOnce.commands(List.of("# VIP perks", "", "/lp creategroup vipperks", "   ",
                        "  lp group default permission set gsit.* false  ", "#done")));
    }

    @Test
    void theCommandsRunInOrderOnceAndTheFileIsKeptAsAReport(@TempDir Path folder) throws Exception {
        Files.write(folder.resolve(RunOnce.FILE), List.of("# perks", "lp creategroup vipperks", "nosuchcommand"),
                StandardCharsets.UTF_8);
        List<String> ran = new ArrayList<>();
        assertEquals(2, RunOnce.run(folder, command -> ran.add(command) && !command.equals("nosuchcommand"), LOG));
        assertEquals(List.of("lp creategroup vipperks", "nosuchcommand"), ran);

        assertFalse(Files.exists(folder.resolve(RunOnce.FILE)), "the file is moved away, so it can't run twice");
        Path done;
        try (Stream<Path> files = Files.list(folder)) {
            done = files.filter(p -> p.getFileName().toString().endsWith(".done")).findFirst().orElseThrow();
        }
        String report = Files.readString(done, StandardCharsets.UTF_8);
        assertTrue(report.contains("lp creategroup vipperks\n#   ok"), report);
        assertTrue(report.contains("nosuchcommand\n#   the server did not know this command"), report);

        // The next start finds nothing to run.
        assertEquals(0, RunOnce.run(folder, command -> ran.add(command), LOG));
        assertEquals(2, ran.size());
    }

    @Test
    void withoutTheFileNothingHappens(@TempDir Path folder) {
        assertEquals(0, RunOnce.run(folder, command -> {
            throw new AssertionError("nothing should run");
        }, LOG));
    }
}
