package me.rauldoq.geoAnarchy.onboarding;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The continent selector as a chest. With the interface pack it is a world map with a pin on each continent
 * (pictures drawn through the title, pins as items with the pack's models); without it, a plain chest with a
 * coloured wool per continent in the same slots.
 */
final class StartMenu implements InventoryHolder {

    private static final NamespacedKey TOOLTIP = new NamespacedKey("geoanarchy", "geo");
    /** Without the pack: a wool in roughly each continent's colour. */
    private static final Map<String, Material> PLAIN = Map.of(
            "america-do-norte", Material.BLUE_WOOL, "america-do-sul", Material.LIME_WOOL, "europa", Material.PURPLE_WOOL,
            "africa", Material.ORANGE_WOOL, "asia", Material.RED_WOOL, "oceania", Material.CYAN_WOOL);

    private final Inventory inventory;
    private final Map<Integer, Continent> buttons = new HashMap<>();
    private final int randomSlot;

    StartMenu(Onboarding onboarding, Player player, boolean art) {
        StartUi ui = onboarding.ui();
        Component heading = onboarding.game("start-title");
        Component title = art ? ui.title(heading, List.of(onboarding.game("start-line-1"), onboarding.game("start-line-2"))) : heading;
        this.inventory = Bukkit.createInventory(this, ui.rows() * 9, title);
        this.randomSlot = ui.randomSlot();
        if (!art) {
            ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            filler.editMeta(meta -> meta.setHideTooltip(true));
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                inventory.setItem(slot, filler);
            }
        }
        Map<String, Integer> here = onboarding.playersPerContinent();
        for (Continent c : ui.continents()) {
            ItemStack item = new ItemStack(art ? Material.PAPER : PLAIN.getOrDefault(c.id(), Material.WHITE_WOOL));
            dress(item, art, "start_" + c.id(), Component.text(c.name(), c.colour()), List.of(
                    onboarding.game("start-lore-players", "players", String.valueOf(here.getOrDefault(c.id(), 0))),
                    Component.empty(),
                    onboarding.game("start-lore-click")));
            inventory.setItem(c.slot(), item);
            buttons.put(c.slot(), c);
        }
        ItemStack random = new ItemStack(art ? Material.PAPER : Material.ENDER_EYE);
        dress(random, art, "start_random", onboarding.game("start-random"), List.of(onboarding.game("start-random-lore")));
        inventory.setItem(randomSlot, random);
    }

    private static void dress(ItemStack item, boolean art, String model, Component name, List<Component> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        if (art) {
            // the pack's pin (or die) instead of the paper, and the server's tooltip
            meta.setItemModel(new NamespacedKey("geoanarchy", model));
            meta.setTooltipStyle(TOOLTIP);
        }
        item.setItemMeta(meta);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** The continent at this slot; null for the random button; empty for anything else. */
    java.util.Optional<Continent> pressed(int slot, List<Continent> all) {
        if (slot == randomSlot) {
            return java.util.Optional.of(all.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(all.size())));
        }
        return java.util.Optional.ofNullable(buttons.get(slot));
    }
}
