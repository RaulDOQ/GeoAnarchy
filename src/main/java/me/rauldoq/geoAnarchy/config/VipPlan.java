package me.rauldoq.geoAnarchy.config;

/**
 * A VIP plan /darvip can give: Geo+, Geo Ultra or Geo Ultra+. The group is the LuckPerms group the GeoUltra
 * plugin looks for, so giving the group is giving the plan.
 * @param id    what is typed in /darvip ("ultra")
 * @param name  what players read ("Geo Ultra")
 * @param group the LuckPerms group ("ultra")
 */
public record VipPlan(String id, String name, String group) {

    /** LuckPerms group names: lowercase letters, digits, "_", "-" and ".". Anything else is refused. */
    public static boolean validGroup(String group) {
        return group != null && group.matches("[a-z0-9_.-]{1,36}");
    }
}
