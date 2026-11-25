package me.rauldoq.geoAnarchy.game;

import me.rauldoq.geoAnarchy.storage.Database;
import me.rauldoq.geoAnarchy.storage.Database.Vip;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Paid VIP months. /darvip adds months to a player; each 30 days one month is used up; when none is left the
 * player leaves the VIP group. The group itself belongs to LuckPerms, which is told through console commands.
 */
public final class VipService {

    static final long MONTH = 30L * 24 * 60 * 60 * 1000;

    private final Database database;
    /** Runs a console command, such as "lp user Steve parent add vip". */
    private final Consumer<String> console;

    public VipService(Database database, Consumer<String> console) {
        this.database = database;
        this.console = console;
    }

    /** Adds months to a player. @return how many months they have now */
    public int give(UUID player, String name, int months, String group, long now) {
        for (Vip vip : database.vips()) {
            if (vip.minecraft().equals(player)) {
                Vip more = new Vip(player, name, vip.months() + months, vip.periodStart());
                database.saveVip(more);
                return more.months();
            }
        }
        database.saveVip(new Vip(player, name, months, now));
        console.accept("lp user " + name + " parent add " + group);
        return months;
    }

    /** Uses up the months that have passed and removes the VIPs that ran out. Call about once an hour. */
    public void tick(String group, long now) {
        for (Vip vip : database.vips()) {
            Vip after = advance(vip, now);
            if (after == null) {
                database.deleteVip(vip.minecraft());
                console.accept("lp user " + vip.name() + " parent remove " + group);
            } else if (!after.equals(vip)) {
                database.saveVip(after);
            }
        }
    }

    /** @return the VIP after the months that have passed since its current month began, or null if it ran out */
    static Vip advance(Vip vip, long now) {
        int months = vip.months();
        long start = vip.periodStart();
        while (months > 0 && now - start >= MONTH) {
            months--;
            start += MONTH;
        }
        return months <= 0 ? null : new Vip(vip.minecraft(), vip.name(), months, start);
    }
}
