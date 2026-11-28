package me.rauldoq.geoAnarchy.onboarding;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * /passos: the six steps in a row, done ones shining, and a button to take them off the sidebar or put them back.
 * With the interface pack it has the same frame as the GeoMenu's lists.
 */
final class StepsMenu implements InventoryHolder {

    static final int ROWS = 3;
    static final int FIRST_SLOT = 10;
    static final int TOGGLE_SLOT = 22;
    private static final NamespacedKey TOOLTIP = new NamespacedKey("geoanarchy", "geo");

    private final Inventory inventory;

    StepsMenu(Newcomers newcomers, Player player) {
        boolean art = player.hasMetadata("geoanarchy-ui");
        Component heading = newcomers.game("steps-title", "done", String.valueOf(newcomers.doneCount(player)),
                "total", String.valueOf(Step.values().length));
        Step[] steps = Step.values();
        List<Integer> finished = new ArrayList<>();
        for (int i = 0; i < steps.length; i++) {
            if (newcomers.done(player, steps[i])) {
                finished.add(FIRST_SLOT + i);
            }
        }
        Component title = art ? newcomers.onboarding().ui().listTitle(ROWS, heading, finished) : heading;
        inventory = Bukkit.createInventory(this, ROWS * 9, title);
        for (int i = 0; i < steps.length; i++) {
            Step step = steps[i];
            boolean done = newcomers.done(player, step);
            List<Component> lore = new ArrayList<>();
            lore.add(newcomers.game("step-" + step.id + "-how"));
            lore.add(Component.empty());
            lore.add(newcomers.game("steps-lore-reward", "geos", Newcomers.whole(newcomers.cents(step))));
            lore.add(newcomers.game(done ? "steps-lore-done" : "steps-lore-todo"));
            Component name = newcomers.game(done ? "steps-name-done" : "steps-name-todo", "step", newcomers.stepName(step));
            inventory.setItem(FIRST_SLOT + i, item(step.icon, name, lore, done, art));
        }
        boolean hidden = newcomers.hidden(player);
        inventory.setItem(TOGGLE_SLOT, item(hidden ? Material.ENDER_EYE : Material.ENDER_PEARL,
                newcomers.game(hidden ? "steps-toggle-show" : "steps-toggle-hide"),
                List.of(newcomers.game("steps-toggle-lore")), false, art));
    }

    private static ItemStack item(Material material, Component name, List<Component> lore, boolean glow, boolean art) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        if (glow) {
            meta.setEnchantmentGlintOverride(true);
        }
        if (art) {
            meta.setTooltipStyle(TOOLTIP);
        }
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
