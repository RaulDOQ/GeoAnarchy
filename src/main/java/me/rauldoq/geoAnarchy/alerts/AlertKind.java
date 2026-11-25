package me.rauldoq.geoAnarchy.alerts;

/** The kinds of alert a player can switch on and off with /notificacoes. */
public enum AlertKind {
    /** War declared, started or ended, and flags captured. */
    WAR("guerra"),
    /** The country can't pay its upkeep, or lost chunks for not paying. */
    UPKEEP("manutencao"),
    /** Alliance and other relation requests from other countries. */
    DIPLOMACY("diplomacia"),
    /** The player was invited to a country. */
    INVITES("convites");

    private final String id;

    AlertKind(String id) {
        this.id = id;
    }

    /** The short name used in button ids, message keys and saved settings. */
    public String id() {
        return id;
    }

    public static AlertKind byId(String id) {
        for (AlertKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        return null;
    }
}
