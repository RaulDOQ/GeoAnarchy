package me.rauldoq.geoAnarchy.alerts;

import me.rauldoq.geoAnarchy.country.Country;
import me.rauldoq.geoAnarchy.link.LinkService;
import me.rauldoq.geoAnarchy.storage.Database.Link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Decides who gets an alert and hands it to Discord.
 * <ul>
 *   <li>A country's founder gets its alerts, if their account is linked and they left that kind on.</li>
 *   <li>A citizen gets them too if they asked for it with /notificacoes.</li>
 * </ul>
 */
public final class AlertService {

    /** One message. The text may use Discord formatting such as **bold** and <t:123:t> times. */
    public record Alert(String title, String text) {
    }

    /** Whatever delivers the private message. In the plugin this is the Discord bot. */
    public interface Sender {
        void send(Link to, Alert alert);
    }

    private final LinkService links;
    private final Prefs prefs;
    private final LongSupplier clock;
    private final Map<String, Long> lastSent = new HashMap<>();
    private volatile Sender sender = (to, alert) -> {
    };

    public AlertService(LinkService links, Prefs prefs, LongSupplier clock) {
        this.links = links;
        this.prefs = prefs;
        this.clock = clock;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    /** Who would get an alert of this kind about this country. */
    public List<Link> recipients(Country country, AlertKind kind) {
        List<Link> result = new ArrayList<>();
        if (country.founder() != null) {
            links.byMinecraft(country.founder())
                    .filter(link -> prefs.wants(link.minecraft(), kind))
                    .ifPresent(result::add);
        }
        for (UUID member : country.members()) {
            if (member.equals(country.founder())) {
                continue;
            }
            links.byMinecraft(member)
                    .filter(link -> prefs.citizenAlerts(link.minecraft()) && prefs.wants(link.minecraft(), kind))
                    .ifPresent(result::add);
        }
        return result;
    }

    /** Sends an alert about a country to everyone who should get it. @return how many were sent */
    public int toCountry(Country country, AlertKind kind, Alert alert) {
        List<Link> to = recipients(country, kind);
        for (Link link : to) {
            sender.send(link, alert);
        }
        return to.size();
    }

    /** Sends an alert to one player, if they are linked and left that kind on. */
    public boolean toPlayer(UUID player, AlertKind kind, Alert alert) {
        return links.byMinecraft(player)
                .filter(link -> prefs.wants(link.minecraft(), kind))
                .map(link -> {
                    sender.send(link, alert);
                    return true;
                })
                .orElse(false);
    }

    /**
     * For alerts that could repeat quickly, like captured flags.
     * @return true if nothing was sent under this key in the last {@code millis}, and remembers this one
     */
    public synchronized boolean allow(String key, long millis) {
        long now = clock.getAsLong();
        Long last = lastSent.get(key);
        if (last != null && now - last < millis) {
            return false;
        }
        lastSent.put(key, now);
        return true;
    }
}
