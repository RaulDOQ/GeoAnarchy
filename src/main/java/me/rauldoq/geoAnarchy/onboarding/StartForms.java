package me.rauldoq.geoAnarchy.onboarding;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;

/**
 * The continent selector for Bedrock players: a Bedrock window with a button per continent, each with its
 * picture from the GeoAnarchy Bedrock pack (textures/geoanarchy/continents). Loaded only with Floodgate.
 */
final class StartForms {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private StartForms() {
    }

    static StartForms create() {
        if (!Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
            return null;
        }
        try {
            FloodgateApi.getInstance();
            return new StartForms();
        } catch (LinkageError | RuntimeException unavailable) {
            return null;
        }
    }

    boolean bedrock(Player player) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (LinkageError | RuntimeException e) {
            return false;
        }
    }

    void show(Onboarding onboarding, Player player) {
        List<Continent> continents = onboarding.ui().continents();
        var here = onboarding.playersPerContinent();
        var countries = onboarding.countriesPerContinent();
        SimpleForm.Builder form = SimpleForm.builder()
                .title(LEGACY.serialize(onboarding.game("start-title")))
                .content(LEGACY.serialize(onboarding.game("start-line-1")) + " " + LEGACY.serialize(onboarding.game("start-line-2")));
        for (Continent c : continents) {
            int[] there = countries.getOrDefault(c.id(), new int[2]);
            String second = LEGACY.serialize(onboarding.game("start-lore-countries", "countries", String.valueOf(there[0]),
                    "recruiting", String.valueOf(there[1])));
            form.button(LEGACY.serialize(net.kyori.adventure.text.Component.text(c.name(), c.colour())) + "\n" + second,
                    FormImage.Type.PATH, "textures/geoanarchy/continents/" + c.id());
        }
        form.button(LEGACY.serialize(onboarding.game("start-random")), FormImage.Type.PATH, "textures/geoanarchy/continents/random");
        form.validResultHandler(response -> {
            int pressed = response.clickedButtonId();
            Bukkit.getScheduler().runTask(onboarding.plugin(), () -> {
                if (!player.isOnline()) {
                    return;
                }
                Continent chosen = pressed >= 0 && pressed < continents.size() ? continents.get(pressed)
                        : continents.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(continents.size()));
                onboarding.choose(player, chosen);
            });
        });
        form.closedResultHandler(() -> Bukkit.getScheduler().runTask(onboarding.plugin(), () -> onboarding.closedWithoutChoosing(player)));
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }
}
