package me.rauldoq.geoAnarchy.alerts;

import me.rauldoq.geoAnarchy.storage.Database;

import java.util.UUID;

/**
 * Each player's choices. Only choices that differ from the default are stored.
 * Defaults: every kind of alert on, alerts for countries where the player is only a citizen off,
 * and money actions through Discord on.
 */
public final class Prefs {

    private static final String CITIZEN = "alertas.cidadao";
    private static final String ACTIONS = "discord.acoes";
    private static final String DM_CLOSED = "discord.dm-fechada";

    private final Database database;

    public Prefs(Database database) {
        this.database = database;
    }

    /** Does the player want this kind of alert? */
    public boolean wants(UUID player, AlertKind kind) {
        return !"off".equals(database.setting(player, "alertas." + kind.id()).orElse("on"));
    }

    public void setWants(UUID player, AlertKind kind, boolean on) {
        database.setSetting(player, "alertas." + kind.id(), on ? null : "off");
    }

    /** Does the player also want country alerts where they are a citizen and not the founder? */
    public boolean citizenAlerts(UUID player) {
        return "on".equals(database.setting(player, CITIZEN).orElse("off"));
    }

    public void setCitizenAlerts(UUID player, boolean on) {
        database.setSetting(player, CITIZEN, on ? "on" : null);
    }

    /** May /depositar and /pagar be used from Discord for this player? The player decides in game. */
    public boolean actions(UUID player) {
        return !"off".equals(database.setting(player, ACTIONS).orElse("on"));
    }

    public void setActions(UUID player, boolean on) {
        database.setSetting(player, ACTIONS, on ? null : "off");
    }

    /** True if the last private message to this player could not be delivered. */
    public boolean dmClosed(UUID player) {
        return database.setting(player, DM_CLOSED).isPresent();
    }

    public void setDmClosed(UUID player, boolean closed) {
        database.setSetting(player, DM_CLOSED, closed ? "1" : null);
    }
}
