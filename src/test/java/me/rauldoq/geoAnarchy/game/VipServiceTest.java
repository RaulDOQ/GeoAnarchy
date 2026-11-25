package me.rauldoq.geoAnarchy.game;

import me.rauldoq.geoAnarchy.config.Settings;
import me.rauldoq.geoAnarchy.config.VipPlan;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VipServiceTest {

    private static final UUID STEVE = UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");

    @Test
    void aPlanIsGivenAsATimedLuckPermsGroupThatAddsUp() {
        List<String> console = new ArrayList<>();
        VipService vips = new VipService(console::add);
        VipPlan ultra = new VipPlan("ultra", "Geo Ultra", "ultra");

        assertEquals(90, vips.give(STEVE, ultra, 3, 30));
        assertEquals(List.of("lp user 8667ba71-b85a-4004-af54-457a9734eed7 parent addtemp ultra 90d accumulate"), console);
    }

    @Test
    void thePlayerGoesInByUuidAndOddGroupNamesAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> VipService.command(STEVE, new VipPlan("x", "X", "ultra parent add admin"), 30));
        assertTrue(VipPlan.validGroup("ultraplus"));
        assertFalse(VipPlan.validGroup("Ultra"), "LuckPerms group names are lowercase");
        assertFalse(VipPlan.validGroup("ultra; op"));
        assertFalse(VipPlan.validGroup(""));
    }

    @Test
    void theShippedSettingsListGeoUltrasThreePlans() {
        Settings settings = Settings.load(YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml")));
        assertEquals(List.of("geoplus", "ultra", "ultraplus"), settings.vipPlans().stream().map(VipPlan::id).toList());
        assertEquals(List.of("Geo+", "Geo Ultra", "Geo Ultra+"), settings.vipPlans().stream().map(VipPlan::name).toList());
        assertEquals("ultraplus", settings.vipPlan("ULTRAPLUS").orElseThrow().group());
        assertTrue(settings.vipPlan("vip").isEmpty(), "the old single group is gone");
        assertEquals(30, settings.vipDaysPerMonth());
    }

    @Test
    void aFileWithoutPlansStillHasGeoUltrasThree() throws Exception {
        YamlConfiguration old = new YamlConfiguration();
        old.loadFromString("vip:\n  group: vip\n");
        Settings settings = Settings.load(old);
        assertEquals(List.of("geoplus", "ultra", "ultraplus"), settings.vipPlans().stream().map(VipPlan::group).toList());
    }
}
