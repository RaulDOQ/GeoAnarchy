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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /recrutando: a banner per recruiting country (in its continent's colour) with its level, citizens, leaders online
 * and the leaders' message. Clicking one asks to join. With the interface pack it has the GeoMenu's list frame.
 */
final class RecruitMenu implements InventoryHolder {

    private static final NamespacedKey TOOLTIP = new NamespacedKey("geoanarchy", "geo");
    private static final Map<String, Material> BANNERS = Map.of(
            "america-do-norte", Material.BLUE_BANNER, "america-do-sul", Material.LIME_BANNER, "europa", Material.PURPLE_BANNER,
            "africa", Material.ORANGE_BANNER, "asia", Material.RED_BANNER, "oceania", Material.CYAN_BANNER);
    /** Characters per line of the leaders' message in the tooltip. */
    private static final int WRAP = 32;

    private final Inventory inventory;
    private final Map<Integer, String> countries = new HashMap<>();

    RecruitMenu(Recruiting recruiting, StartUi ui, Player viewer, List<Recruiting.Entry> entries) {
        boolean art = viewer.hasMetadata("geoanarchy-ui");
        int rows = Math.max(1, Math.min(6, (entries.size() + 8) / 9));
        Component heading = recruiting.game("recruit-title");
        inventory = Bukkit.createInventory(this, rows * 9, art ? ui.listTitle(rows, heading) : heading);
        if (entries.isEmpty()) {
            inventory.setItem(4, item(Material.BARRIER, recruiting.game("recruit-empty"),
                    List.of(recruiting.game("recruit-empty-lore")), art));
            return;
        }
        for (int i = 0; i < entries.size() && i < rows * 9; i++) {
            Recruiting.Entry e = entries.get(i);
            List<Component> lore = new ArrayList<>();
            lore.add(recruiting.game("recruit-lore-level", "level", e.level().isBlank() ? "—" : e.level()));
            lore.add(recruiting.game("recruit-lore-citizens", "citizens", String.valueOf(e.citizens())));
            lore.add(recruiting.game("recruit-lore-continent", "continent",
                    e.continent() == null ? "—" : e.continent().name()));
            lore.add(recruiting.game("recruit-lore-leaders", "online", String.valueOf(e.leadersOnline())));
            lore.add(Component.empty());
            for (String line : wrap(e.message())) {
                lore.add(recruiting.game("recruit-lore-message", "message", line));
            }
            lore.add(Component.empty());
            boolean member = e.country().members().contains(viewer.getUniqueId());
            lore.add(recruiting.game(member ? "recruit-lore-member" : "recruit-lore-click"));
            Material banner = e.continent() == null ? Material.WHITE_BANNER : BANNERS.getOrDefault(e.continent().id(), Material.WHITE_BANNER);
            inventory.setItem(i, item(banner, recruiting.game("recruit-item-name", "country", e.country().name()), lore, art));
            countries.put(i, e.country().id());
        }
    }

    static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > WRAP) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static ItemStack item(Material material, Component name, List<Component> lore, boolean art) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ATTRIBUTES);
        if (art) {
            meta.setTooltipStyle(TOOLTIP);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** The country id behind a slot, or null. */
    String countryAt(int slot) {
        return countries.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
