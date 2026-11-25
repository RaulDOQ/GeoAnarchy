package me.rauldoq.geoAnarchy.config;

import org.bukkit.configuration.ConfigurationSection;

/** Everything in config.yml, read once. A reload builds a new one. */
public record Settings(
        String token, long guildId, String activity, long warChannel, long statusChannel,
        String invite, String map, String site,
        int codeMinutes, int maxAttempts, int lockMinutes, String linkRole, boolean syncNickname,
        int warPreparationMinutes, int flagCooldownMinutes, int upkeepCooldownHours,
        boolean actionsEnabled, double payDailyLimit, int confirmSeconds,
        int ranksRefreshMinutes, String vipGroup) {

    public static Settings load(ConfigurationSection c) {
        return new Settings(
                c.getString("discord.token", "").trim(),
                c.getLong("discord.guild-id", 0),
                c.getString("discord.activity", "GeoAnarchy.net"),
                c.getLong("discord.channels.war", 0),
                c.getLong("discord.channels.status", 0),
                c.getString("links.invite", ""),
                c.getString("links.map", "https://mapa.geoanarchy.net/"),
                c.getString("links.site", "https://geoanarchy.net"),
                Math.max(1, c.getInt("linking.code-minutes", 10)),
                Math.max(1, c.getInt("linking.max-attempts", 5)),
                Math.max(1, c.getInt("linking.lock-minutes", 5)),
                c.getString("linking.role", "Vinculado").trim(),
                c.getBoolean("linking.sync-nickname", true),
                Math.max(0, c.getInt("alerts.war-preparation-minutes", 60)),
                Math.max(0, c.getInt("alerts.flag-cooldown-minutes", 5)),
                Math.max(0, c.getInt("alerts.upkeep-cooldown-hours", 12)),
                c.getBoolean("actions.enabled", true),
                Math.max(0, c.getDouble("actions.pay-daily-limit", 10_000)),
                Math.max(10, c.getInt("actions.confirm-seconds", 60)),
                Math.max(1, c.getInt("ranks.refresh-minutes", 5)),
                c.getString("vip.group", "vip").trim());
    }

    public boolean hasToken() {
        return !token.isEmpty();
    }
}
