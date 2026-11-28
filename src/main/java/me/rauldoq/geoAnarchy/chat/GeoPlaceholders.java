package me.rauldoq.geoAnarchy.chat;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Locale;

/**
 * PlaceholderAPI placeholders for chat and other plugins, so they need no extra PlaceholderAPI downloads:
 * <ul>
 *   <li>%geoanarchy_country%: "[Portugal] " in the country's colors, or nothing without a country</li>
 *   <li>%geoanarchy_country_name%: the country's name in its colors, or nothing</li>
 *   <li>%geoanarchy_name%: the player's display name, with the colours of GeoUltra's /nome</li>
 *   <li>%geoanarchy_name_color%: the Minecraft colour closest to that name's first colour ("&a"), or nothing.
 *       For the name above the head, which the game paints in one of the 16 colours only.</li>
 *   <li>%geoanarchy_player%: the player's account name</li>
 *   <li>%geoanarchy_prefix%: the rank prefix from LuckPerms (through Vault), or nothing</li>
 * </ul>
 * Only loaded when PlaceholderAPI is installed.
 */
public final class GeoPlaceholders extends PlaceholderExpansion {

    private final Plugin plugin;
    private final ChatTags tags;

    private GeoPlaceholders(Plugin plugin, ChatTags tags) {
        this.plugin = plugin;
        this.tags = tags;
    }

    /** Registers the placeholders. Call only when PlaceholderAPI is enabled. */
    public static boolean register(Plugin plugin, ChatTags tags) {
        return new GeoPlaceholders(plugin, tags).register();
    }

    @Override
    public String getIdentifier() {
        return "geoanarchy";
    }

    @Override
    public String getAuthor() {
        return "RaulDOQ";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "country" -> tags.tag(player.getUniqueId());
            case "country_name" -> tags.name(player.getUniqueId());
            case "country_sort" -> tags.sortKey(player.getUniqueId());
            case "name" -> player instanceof Player online ? LegacyColors.ofComponent(online.displayName())
                    : player.getName() == null ? "" : player.getName();
            case "name_color" -> player instanceof Player online ? LegacyColors.nearestCode(online.displayName()) : "";
            case "player" -> player.getName() == null ? "" : player.getName();
            case "prefix" -> prefix(player);
            default -> null;
        };
    }

    private static String prefix(OfflinePlayer player) {
        try {
            RegisteredServiceProvider<Chat> chat = Bukkit.getServicesManager().getRegistration(Chat.class);
            if (chat == null) {
                return "";
            }
            String prefix = chat.getProvider().getPlayerPrefix(null, player);
            return prefix == null ? "" : LegacyColors.toLegacy(prefix);
        } catch (RuntimeException | LinkageError noVault) {
            return "";
        }
    }
}
