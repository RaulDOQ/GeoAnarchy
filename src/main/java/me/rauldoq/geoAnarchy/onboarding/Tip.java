package me.rauldoq.geoAnarchy.onboarding;

/**
 * One-time tips for new players, each shown once, at the moment it matters. The texts are game.tip-&lt;id&gt; in
 * messages.yml.
 */
enum Tip {
    /** The first night falls. */
    NOITE("noite"),
    /** Back from the first death. */
    MORTE("morte"),
    /** Walked into a country that is not theirs. */
    PAIS("pais"),
    /** Typed /tpa, /home, /spawn and the like. */
    TELEPORTE("teleporte"),
    /** Mined their first iron or diamond. */
    MINERIO("minerio"),
    /** Said something in chat. */
    CHAT("chat"),
    /** Half an hour in, still without a country. */
    RECRUTAR("recrutar");

    final String id;

    Tip(String id) {
        this.id = id;
    }
}
