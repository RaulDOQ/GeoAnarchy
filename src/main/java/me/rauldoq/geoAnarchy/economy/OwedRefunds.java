package me.rauldoq.geoAnarchy.economy;

import me.rauldoq.geoAnarchy.storage.Database;

import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Money a player is owed because giving it back failed: a /depositar whose money left the wallet but could not
 * reach the country's bank, and whose refund then failed too (the economy could not save, say). It is kept in
 * geoanarchy.db and given back every few minutes until it works, so it is never lost.
 */
public final class OwedRefunds implements Runnable {

    private final Database database;
    private final Supplier<Economy> economy;
    private final Logger log;

    public OwedRefunds(Database database, Supplier<Economy> economy, Logger log) {
        this.database = database;
        this.economy = economy;
        this.log = log;
    }

    /** Gives back what it can now, or keeps it in geoanarchy.db for the next round. */
    public void refund(java.util.UUID player, long cents, String reason) {
        if (!economy.get().give(player, cents, reason)) {
            database.addOwed(player, cents, reason, System.currentTimeMillis());
            log.warning("Could not give back " + cents + " cents to " + player + " (" + reason + "); trying again in a few minutes.");
        }
    }

    /** Tries every money still owed again. Runs on the server thread. */
    @Override
    public void run() {
        for (Database.Owed owed : database.owed()) {
            if (economy.get().give(owed.player(), owed.cents(), owed.reason())) {
                database.deleteOwed(owed.id());
                log.info("Gave back " + owed.cents() + " cents owed to " + owed.player() + " (" + owed.reason() + ").");
            }
        }
    }
}
