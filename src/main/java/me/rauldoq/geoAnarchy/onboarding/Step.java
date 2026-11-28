package me.rauldoq.geoAnarchy.onboarding;

import org.bukkit.Material;

/**
 * The "Primeiros passos": what a new player is asked to do in their first hours, each paid once in Geos.
 * The amounts are in config.yml (newcomers.steps), the texts in messages.yml (game.step-&lt;id&gt;-name and -how).
 */
enum Step {
    GUIA("guia", Material.WRITTEN_BOOK, 50),
    CAMA("cama", Material.RED_BED, 150),
    NOITE("noite", Material.CLOCK, 150),
    RECRUTANDO("recrutando", Material.WHITE_BANNER, 50),
    MERCADO("mercado", Material.EMERALD, 200),
    PAIS("pais", Material.FILLED_MAP, 400);

    final String id;
    final Material icon;
    /** Used when config.yml has no amount for this step. */
    final int defaultGeos;

    Step(String id, Material icon, int defaultGeos) {
        this.id = id;
        this.icon = icon;
        this.defaultGeos = defaultGeos;
    }
}
