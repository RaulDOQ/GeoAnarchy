package me.rauldoq.geoAnarchy.onboarding;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the GeoAnarchy interface pack holds for the continent selector: the continents (outlines, pin slots,
 * colours), the menu's pictures and the moving characters. ui.json is written by the same build that makes the
 * pack (GeoRoster's art/geoart.py), so the menu always matches the pack the players have.
 */
public final class StartUi {

    static final Key FONT = Key.key("geoanarchy", "menu");
    static final TextColor TEXT = TextColor.color(0xEEF0F3);
    static final TextColor MUTED = TextColor.color(0xA8B0BB);

    private final Map<String, String> glyphs = new HashMap<>();
    private final Map<String, Integer> advances = new HashMap<>();
    private final Map<Integer, String> spaces = new HashMap<>();
    private final Map<Integer, Integer> widths = new HashMap<>();
    private final List<Continent> continents = new ArrayList<>();
    private final List<Key> textFonts = new ArrayList<>();
    private final int rows;
    private final int randomSlot;

    private StartUi(JsonObject data) {
        data.getAsJsonObject("menu").entrySet().forEach(e -> glyphs.put(e.getKey(), e.getValue().getAsString()));
        data.getAsJsonObject("menu_adv").entrySet().forEach(e -> advances.put(e.getKey(), e.getValue().getAsInt()));
        data.getAsJsonObject("menu_space").entrySet().forEach(e -> spaces.put(Integer.parseInt(e.getKey()), e.getValue().getAsString()));
        data.getAsJsonObject("widths").entrySet().forEach(e -> widths.put(e.getKey().codePointAt(0), e.getValue().getAsInt()));
        JsonObject start = data.getAsJsonObject("start");
        rows = start.get("rows").getAsInt();
        randomSlot = start.get("random_slot").getAsInt();
        start.getAsJsonArray("text_fonts").forEach(e -> textFonts.add(Key.key(e.getAsString())));
        for (var element : start.getAsJsonArray("continents")) {
            JsonObject c = element.getAsJsonObject();
            List<double[][]> polygons = new ArrayList<>();
            for (var poly : c.getAsJsonArray("polygons")) {
                JsonArray points = poly.getAsJsonArray();
                double[][] out = new double[points.size()][];
                for (int i = 0; i < points.size(); i++) {
                    JsonArray p = points.get(i).getAsJsonArray();
                    out[i] = new double[]{p.get(0).getAsDouble(), p.get(1).getAsDouble()};
                }
                polygons.add(out);
            }
            JsonArray lon = c.getAsJsonArray("lon");
            JsonArray lat = c.getAsJsonArray("lat");
            continents.add(new Continent(c.get("id").getAsString(), c.get("name").getAsString(), c.get("slot").getAsInt(),
                    TextColor.fromHexString("#" + c.get("colour").getAsString()),
                    lon.get(0).getAsDouble(), lon.get(1).getAsDouble(), lat.get(0).getAsDouble(), lat.get(1).getAsDouble(), polygons));
        }
    }

    public static StartUi load(InputStream in) throws IOException {
        try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return new StartUi(JsonParser.parseReader(reader).getAsJsonObject());
        }
    }

    public List<Continent> continents() {
        return continents;
    }

    public int rows() {
        return rows;
    }

    public int randomSlot() {
        return randomSlot;
    }

    int width(String plain) {
        int total = 0;
        for (int i = 0; i < plain.length(); ) {
            int cp = plain.codePointAt(i);
            total += widths.getOrDefault(cp, 6);
            i += Character.charCount(cp);
        }
        return total;
    }

    int width(Component text) {
        return width(PlainTextComponentSerializer.plainText().serialize(text));
    }

    private Component space(int n) {
        if (n == 0) {
            return Component.empty();
        }
        StringBuilder out = new StringBuilder();
        int sign = n < 0 ? -1 : 1;
        int left = Math.abs(n);
        for (int p = 8; p >= 0; p--) {
            int v = 1 << p;
            while (left >= v) {
                out.append(spaces.get(sign * v));
                left -= v;
            }
        }
        return Component.text(out.toString()).font(FONT);
    }

    /**
     * The menu title that draws the whole selector: the frame, the title, the map (two halves, drawn at double
     * resolution) and the explanation under it. The game writes a chest title 8 px in, so the pen starts there.
     */
    Component title(Component heading, List<Component> lines) {
        TextComponent.Builder out = Component.text();
        int[] pen = {8};
        java.util.function.IntConsumer to = x -> {
            out.append(space(x - pen[0]));
            pen[0] = x;
        };
        to.accept(0);
        out.append(Component.text(glyphs.get("start_bg"), NamedTextColor.WHITE).font(FONT));
        pen[0] += advances.get("start_bg");
        to.accept(22);
        out.append(Component.text().color(TEXT).append(heading).build());
        pen[0] += width(heading);
        to.accept(7);
        out.append(Component.text(glyphs.get("start_map_l"), NamedTextColor.WHITE).font(FONT));
        pen[0] += advances.get("start_map_l");
        to.accept(7 + advances.get("start_map_l") - 1);
        out.append(Component.text(glyphs.get("start_map_r"), NamedTextColor.WHITE).font(FONT));
        pen[0] += advances.get("start_map_r");
        for (int i = 0; i < lines.size() && i < textFonts.size(); i++) {
            to.accept(8);
            out.append(Component.text().color(MUTED).font(textFonts.get(i)).append(lines.get(i)).build());
            pen[0] += width(lines.get(i));
        }
        return out.build();
    }
}
