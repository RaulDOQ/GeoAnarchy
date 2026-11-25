package me.rauldoq.geoAnarchy.game;

import me.rauldoq.geoAnarchy.config.VipPlan;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Gives VIP plans. A plan is a LuckPerms group, given for a number of days as a temporary group that
 * accumulates: giving 30 days to someone with 10 left leaves them 40. LuckPerms counts the days and takes the
 * group away when they run out, and the GeoUltra plugin reads the days left from LuckPerms, so nothing here
 * has to keep a calendar.
 */
public final class VipService {

    /** Runs a console command, such as "lp user ... parent addtemp ultra 30d accumulate". */
    private final Consumer<String> console;

    public VipService(Consumer<String> console) {
        this.console = console;
    }

    /** Adds months of a plan to a player. @return the days added */
    public int give(UUID player, VipPlan plan, int months, int daysPerMonth) {
        int days = Math.max(1, months) * Math.max(1, daysPerMonth);
        console.accept(command(player, plan, days));
        return days;
    }

    /**
     * The LuckPerms command. The player goes in by UUID, never by a typed name, and the group was checked when
     * the settings were read: nothing a player types can end up inside a console command.
     */
    static String command(UUID player, VipPlan plan, int days) {
        if (!VipPlan.validGroup(plan.group())) {
            throw new IllegalArgumentException("Not a LuckPerms group name: " + plan.group());
        }
        return "lp user " + player + " parent addtemp " + plan.group() + " " + days + "d accumulate";
    }
}
