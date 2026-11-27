package me.rauldoq.geoAnarchy.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns colored text from other plugins (Lands names, prefixes) into the "&" codes chat plugins read.
 * Lands keeps names in MiniMessage, so this understands MiniMessage colors, styles, gradients and rainbows,
 * as well as "&" and "§" codes and "&#RRGGBB":
 * <ul>
 *   <li>a color becomes "&c", or Bukkit's "&x&R&R&G&G&B&B" for hex colors;</li>
 *   <li>a gradient or a rainbow gives each letter its own hex color, as in the game;</li>
 *   <li>styles (bold, italic...) are written again after every color, since a color code resets them;</li>
 *   <li>any other tag (hover, click...) is dropped.</li>
 * </ul>
 */
public final class LegacyColors {

    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.builder()
            .character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    /**
     * A display name in "&" codes, hex colours and gradients included. Used instead of the server's own
     * legacy text of the name, which could round hex colours to the 16 Minecraft ones.
     */
    public static String ofComponent(Component component) {
        return component == null ? "" : AMPERSAND.serialize(component);
    }

    /**
     * The Minecraft colour closest to the colour of the first coloured letter, as "&a", or "" when the name has
     * no colour. Above a player's head the game can only paint the name in one of the 16 colours.
     */
    public static String nearestCode(Component component) {
        TextColor color = component == null ? null : firstColor(component, null);
        if (color == null) {
            return "";
        }
        return AMPERSAND.serialize(Component.text("x", NamedTextColor.nearestTo(color))).substring(0, 2);
    }

    private static TextColor firstColor(Component component, TextColor inherited) {
        TextColor own = component.color() != null ? component.color() : inherited;
        if (own != null && component instanceof TextComponent text && !text.content().isEmpty()) {
            return own;
        }
        for (Component child : component.children()) {
            TextColor found = firstColor(child, own);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static final Map<String, String> CODES = Map.ofEntries(
            Map.entry("black", "0"), Map.entry("dark_blue", "1"), Map.entry("dark_green", "2"),
            Map.entry("dark_aqua", "3"), Map.entry("dark_red", "4"), Map.entry("dark_purple", "5"),
            Map.entry("gold", "6"), Map.entry("gray", "7"), Map.entry("grey", "7"), Map.entry("dark_gray", "8"),
            Map.entry("dark_grey", "8"), Map.entry("blue", "9"), Map.entry("green", "a"), Map.entry("aqua", "b"),
            Map.entry("red", "c"), Map.entry("light_purple", "d"), Map.entry("yellow", "e"), Map.entry("white", "f"));
    private static final Map<String, String> HEXES = Map.ofEntries(
            Map.entry("black", "000000"), Map.entry("dark_blue", "0000aa"), Map.entry("dark_green", "00aa00"),
            Map.entry("dark_aqua", "00aaaa"), Map.entry("dark_red", "aa0000"), Map.entry("dark_purple", "aa00aa"),
            Map.entry("gold", "ffaa00"), Map.entry("gray", "aaaaaa"), Map.entry("grey", "aaaaaa"),
            Map.entry("dark_gray", "555555"), Map.entry("dark_grey", "555555"), Map.entry("blue", "5555ff"),
            Map.entry("green", "55ff55"), Map.entry("aqua", "55ffff"), Map.entry("red", "ff5555"),
            Map.entry("light_purple", "ff55ff"), Map.entry("yellow", "ffff55"), Map.entry("white", "ffffff"));
    private static final Map<String, String> STYLES = Map.ofEntries(
            Map.entry("obfuscated", "k"), Map.entry("obf", "k"), Map.entry("bold", "l"), Map.entry("b", "l"),
            Map.entry("strikethrough", "m"), Map.entry("st", "m"), Map.entry("underlined", "n"), Map.entry("u", "n"),
            Map.entry("italic", "o"), Map.entry("i", "o"), Map.entry("em", "o"));

    private static final Pattern TOKEN = Pattern.compile(
            "<(/?)([^<>]*)>|&#([0-9a-fA-F]{6})|&[xX]((?:&[0-9a-fA-F]){6})|&([0-9a-fk-orA-FK-OR])|([^<&]+|[<&])");
    private static final Pattern TRAILING_CODES = Pattern.compile("(&[0-9a-fA-Fk-oK-OrRxX])+$");
    private static final Pattern HEX = Pattern.compile("#?([0-9a-fA-F]{6})");
    private static final Pattern LEGACY_COLOR = Pattern.compile("&[0-9a-fA-FxX]");

    private LegacyColors() {
    }

    public static String toLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return new Converter().run(text.replace('§', '&'));
    }

    /** True if the text sets a color somewhere, so a default color isn't needed. */
    public static boolean hasColor(String legacy) {
        return legacy != null && LEGACY_COLOR.matcher(legacy).find();
    }

    /** One pass over the text, keeping track of the color and styles in force. */
    private static final class Converter {
        private final StringBuilder out = new StringBuilder();
        /** The color code in force ("&c" or "&x&..."), or "" for none. */
        private String color = "";
        private final Set<String> styles = new LinkedHashSet<>();
        /** While a gradient is open: its colors; its letters wait in {@link #letters}. */
        private List<int[]> gradient;
        private boolean rainbow;
        private final List<Letter> letters = new ArrayList<>();

        private record Letter(int codePoint, List<String> styles) {
        }

        String run(String text) {
            Matcher token = TOKEN.matcher(text);
            while (token.find()) {
                if (token.group(2) != null) {
                    tag(token.group(1).equals("/"), token.group(2).trim().toLowerCase(Locale.ROOT));
                } else if (token.group(3) != null) {
                    setColor(hex(token.group(3)));
                } else if (token.group(4) != null) {
                    setColor(hex(token.group(4).replace("&", "")));
                } else if (token.group(5) != null) {
                    legacy(Character.toLowerCase(token.group(5).charAt(0)));
                } else {
                    text(token.group(6));
                }
            }
            flushGradient();
            // Codes after the last letter change nothing.
            return TRAILING_CODES.matcher(out).replaceAll("");
        }

        private void tag(boolean closing, String name) {
            if (name.startsWith("gradient") || name.startsWith("transition") || name.equals("rainbow")
                    || name.startsWith("rainbow:")) {
                if (closing) {
                    flushGradient();
                } else {
                    flushGradient();
                    rainbow = name.startsWith("rainbow");
                    gradient = rainbow ? List.of() : stops(name);
                    if (!rainbow && gradient.isEmpty()) {
                        gradient = null;
                    }
                }
                return;
            }
            String style = STYLES.get(name);
            if (style != null) {
                if (closing) {
                    styles.remove(style);
                    if (gradient == null) {
                        out.append("&r").append(color);
                        styles.forEach(s -> out.append('&').append(s));
                    }
                } else {
                    styles.add(style);
                    if (gradient == null) {
                        out.append('&').append(style);
                    }
                }
                return;
            }
            if (name.equals("reset") || name.equals("r")) {
                flushGradient();
                color = "";
                styles.clear();
                out.append("&r");
                return;
            }
            String code = colorCode(name);
            if (code != null) {
                if (closing) {
                    color = "";
                    if (gradient == null) {
                        out.append("&r");
                        styles.forEach(s -> out.append('&').append(s));
                    }
                } else {
                    setColor(code);
                }
            }
            // Anything else (hover, click, font...) is left out.
        }

        private void legacy(char code) {
            if (code == 'r') {
                tag(false, "reset");
            } else if ("klmno".indexOf(code) >= 0) {
                styles.add(String.valueOf(code));
                if (gradient == null) {
                    out.append('&').append(code);
                }
            } else {
                setColor("&" + code);
            }
        }

        private void setColor(String code) {
            flushGradient();
            color = code;
            styles.clear();
            out.append(code);
        }

        private void text(String text) {
            if (gradient == null) {
                out.append(text);
                return;
            }
            text.codePoints().forEach(c -> letters.add(new Letter(c, List.copyOf(styles))));
        }

        /** Writes the letters of an open gradient, each with its own color. */
        private void flushGradient() {
            if (gradient == null) {
                return;
            }
            int count = letters.size();
            for (int i = 0; i < count; i++) {
                Letter letter = letters.get(i);
                double t = count == 1 ? 0 : (double) i / (count - 1);
                out.append(hex(rainbow ? rainbow(t) : blend(gradient, t)));
                letter.styles().forEach(s -> out.append('&').append(s));
                out.appendCodePoint(letter.codePoint());
            }
            letters.clear();
            gradient = null;
            rainbow = false;
            color = "";
            if (!styles.isEmpty()) {
                styles.forEach(s -> out.append('&').append(s));
            }
        }
    }

    private static String colorCode(String name) {
        String key = name;
        if (key.startsWith("color:") || key.startsWith("colour:") || key.startsWith("c:")) {
            key = key.substring(key.indexOf(':') + 1);
        }
        Matcher hex = HEX.matcher(key);
        if (key.startsWith("#") && hex.matches()) {
            return hex(hex.group(1));
        }
        String code = CODES.get(key);
        return code == null ? null : "&" + code;
    }

    /** The colors of "gradient:#ff0000:blue:..." as RGB. */
    private static List<int[]> stops(String tag) {
        List<int[]> colors = new ArrayList<>();
        String[] parts = tag.split(":");
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            Matcher hex = HEX.matcher(part);
            String rgb = hex.matches() ? hex.group(1) : HEXES.get(part);
            if (rgb != null) {
                colors.add(new int[]{Integer.parseInt(rgb.substring(0, 2), 16),
                        Integer.parseInt(rgb.substring(2, 4), 16), Integer.parseInt(rgb.substring(4, 6), 16)});
            }
        }
        if (colors.size() == 1) {
            colors.add(colors.get(0));
        }
        return colors;
    }

    private static String blend(List<int[]> stops, double t) {
        double position = t * (stops.size() - 1);
        int index = Math.min((int) Math.floor(position), stops.size() - 2);
        double local = position - index;
        int[] a = stops.get(index);
        int[] b = stops.get(index + 1);
        return String.format(Locale.ROOT, "%02x%02x%02x",
                Math.round(a[0] + (b[0] - a[0]) * local),
                Math.round(a[1] + (b[1] - a[1]) * local),
                Math.round(a[2] + (b[2] - a[2]) * local));
    }

    private static String rainbow(double t) {
        java.awt.Color color = java.awt.Color.getHSBColor((float) t, 1f, 1f);
        return String.format(Locale.ROOT, "%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String hex(String rrggbb) {
        StringBuilder code = new StringBuilder("&x");
        for (char c : rrggbb.toLowerCase(Locale.ROOT).toCharArray()) {
            code.append('&').append(c);
        }
        return code.toString();
    }
}
