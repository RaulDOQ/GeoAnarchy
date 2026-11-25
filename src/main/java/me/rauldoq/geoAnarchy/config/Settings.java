package me.rauldoq.geoAnarchy.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Everything in config.yml, read once. A reload builds a new one. */
public record Settings(
        String token, long guildId, String activity, long warChannel, long statusChannel,
        String invite, String map, String site,
        int codeMinutes, int maxAttempts, int lockMinutes, String linkRole, boolean syncNickname,
        int warPreparationMinutes, int flagCooldownMinutes, int upkeepCooldownHours,
        boolean actionsEnabled, double payDailyLimit, int confirmSeconds,
        int ranksRefreshMinutes, List<VipPlan> vipPlans, int vipDaysPerMonth) {

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
                vipPlans(c.getConfigurationSection("vip.plans")),
                Math.max(1, c.getInt("vip.days-per-month", 30)));
    }

    /** The plans of config.yml, or GeoUltra's three when the file has none. Groups with odd names are left out. */
    private static List<VipPlan> vipPlans(ConfigurationSection section) {
        List<VipPlan> plans = new ArrayList<>();
        if (section != null) {
            for (String id : section.getKeys(false)) {
                String group = section.getString(id + ".group", id).trim().toLowerCase(Locale.ROOT);
                if (VipPlan.validGroup(group)) {
                    plans.add(new VipPlan(id.toLowerCase(Locale.ROOT), section.getString(id + ".name", id), group));
                }
            }
        }
        if (plans.isEmpty()) {
            plans.add(new VipPlan("geoplus", "Geo+", "geoplus"));
            plans.add(new VipPlan("ultra", "Geo Ultra", "ultra"));
            plans.add(new VipPlan("ultraplus", "Geo Ultra+", "ultraplus"));
        }
        return List.copyOf(plans);
    }

    /** A plan by what is typed in /darvip, ignoring case. */
    public Optional<VipPlan> vipPlan(String id) {
        return vipPlans.stream().filter(plan -> plan.id().equalsIgnoreCase(id)).findFirst();
    }

    public boolean hasToken() {
        return !token.isEmpty();
    }
}
