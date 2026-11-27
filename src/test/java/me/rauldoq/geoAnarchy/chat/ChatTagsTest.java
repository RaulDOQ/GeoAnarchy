package me.rauldoq.geoAnarchy.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTagsTest {

    @Test
    void aCountryShowsInBracketsAndNoCountryShowsNothing() {
        assertEquals("&f[&cPortugal&f] ", ChatTags.tagOf("&cPortugal"));
        assertEquals("", ChatTags.tagOf(null));
        assertEquals("", ChatTags.tagOf("  "));
    }

    @Test
    void aNameWithoutColorsGetsTheTabListsGreen() {
        assertEquals("&2Portugal", ChatTags.display("Portugal"));
        assertEquals("&cEspanha", ChatTags.display("&cEspanha"));
        assertEquals("", ChatTags.display(""));
    }

    @Test
    void everyColorFormatBecomesAmpersandCodes() {
        assertEquals("&cFrança", LegacyColors.toLegacy("§cFrança"));
        assertEquals("&x&f&f&0&0&a&aBrasil", LegacyColors.toLegacy("&#FF00AABrasil"));
        assertEquals("&x&1&2&a&b&3&4Chile", LegacyColors.toLegacy("<#12AB34>Chile"));
        assertEquals("&x&1&2&a&b&3&4Chile", LegacyColors.toLegacy("&x&1&2&A&B&3&4Chile"));
        assertEquals("&c&lPeru", LegacyColors.toLegacy("<red><bold>Peru</bold></red>"));
        assertEquals("Itália", LegacyColors.toLegacy("<hover:show_text:'x'>Itália</hover>"));
        assertEquals("Portugal & Algarves", LegacyColors.toLegacy("Portugal & Algarves"));
    }

    @Test
    void aGradientGivesEachLetterItsOwnColor() {
        // Red to blue over five letters: the middle one is half and half.
        assertEquals("&x&f&f&0&0&0&0J&x&b&f&0&0&4&0a&x&8&0&0&0&8&0p&x&4&0&0&0&b&fã&x&0&0&0&0&f&fo",
                LegacyColors.toLegacy("<gradient:#FF0000:#0000FF>Japão</gradient>"));
        // MiniMessage lets the closing tag go, and takes color names; styles follow every letter.
        assertEquals("&x&f&f&5&5&5&5&lA&x&5&5&5&5&f&f&lB", LegacyColors.toLegacy("<gradient:red:blue><b>AB"));
        assertTrue(LegacyColors.toLegacy("<rainbow>Arco</rainbow>").startsWith("&x&f&f&0&0&0&0A"));
    }

    @Test
    void colorsAreFoundWhateverTheirForm() {
        assertTrue(LegacyColors.hasColor("&cX"));
        assertTrue(LegacyColors.hasColor("&x&f&f&0&0&0&0X"));
        assertFalse(LegacyColors.hasColor("&lX"), "bold alone is not a color");
        assertFalse(LegacyColors.hasColor("Portugal & Algarves"));
    }
}
