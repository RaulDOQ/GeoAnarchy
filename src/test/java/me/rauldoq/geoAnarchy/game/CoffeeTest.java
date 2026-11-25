package me.rauldoq.geoAnarchy.game;

import me.rauldoq.geoAnarchy.config.Settings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoffeeTest {

    private static final Settings.Coffee SHIPPED =
            Settings.load(YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"))).coffee();

    @Test
    void badCoffeeBarelyLastsAndPerfectCoffeeLastsTwentyMinutes() {
        assertEquals(15, Coffee.seconds(SHIPPED, 1));
        assertEquals(1200, Coffee.seconds(SHIPPED, 10));
        assertEquals(0.5, SHIPPED.hungerMultiplier());
    }

    @Test
    void onlyTheBestCoffeeIsWorthIt() {
        // The time grows with the square of the quality: middling coffee is short, the last steps count most.
        assertTrue(Coffee.seconds(SHIPPED, 3) < 90, "quality 3 lasts " + Coffee.seconds(SHIPPED, 3) + " s");
        assertTrue(Coffee.seconds(SHIPPED, 5) < 300, "quality 5 lasts " + Coffee.seconds(SHIPPED, 5) + " s");
        assertEquals(732, Coffee.seconds(SHIPPED, 8));
        for (int quality = 1; quality < 10; quality++) {
            assertTrue(Coffee.seconds(SHIPPED, quality) < Coffee.seconds(SHIPPED, quality + 1));
        }
    }

    @Test
    void qualitiesOutsideOneToTenAreKeptInside() {
        assertEquals(Coffee.seconds(SHIPPED, 1), Coffee.seconds(SHIPPED, 0));
        assertEquals(Coffee.seconds(SHIPPED, 10), Coffee.seconds(SHIPPED, 42));
    }

    @Test
    void aFileWithoutTheCoffeeSectionUsesTheDefaults() throws Exception {
        YamlConfiguration empty = new YamlConfiguration();
        empty.loadFromString("vip:\n  days-per-month: 30\n");
        Settings.Coffee coffee = Settings.load(empty).coffee();
        assertEquals(15, coffee.worstSeconds());
        assertEquals(1200, coffee.bestSeconds());
        assertEquals(0.5, coffee.hungerMultiplier());
    }
}
