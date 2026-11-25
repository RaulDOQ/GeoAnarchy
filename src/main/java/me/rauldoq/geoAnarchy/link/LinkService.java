package me.rauldoq.geoAnarchy.link;

import me.rauldoq.geoAnarchy.storage.Database;
import me.rauldoq.geoAnarchy.storage.Database.Link;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Links a Minecraft account to a Discord account.
 * <ol>
 *   <li>In game, {@link #issueCode} gives the player a six-digit code that lasts a few minutes.</li>
 *   <li>In Discord, {@link #redeem} trades that code for the link.</li>
 * </ol>
 * The game side is the one we trust: the player is logged in there, and the code is shown only to them.
 * One Minecraft account goes with one Discord account, and the other way round.
 */
public final class LinkService {

    public enum Status {
        LINKED,
        /** No code like that, or it ran out. */
        WRONG_CODE,
        /** Too many wrong codes from this Discord account. It has to wait. */
        LOCKED,
        /** This Discord account is already linked to a Minecraft account. */
        DISCORD_ALREADY_LINKED
    }

    /** @param link    the new link, or the one that already existed, or null
     *  @param minutes for LOCKED, how long is left to wait */
    public record Redeem(Status status, Link link, int minutes) {
    }

    private record Pending(UUID player, String name, long expiresAt) {
    }

    private static final class Attempts {
        int wrong;
        long lockedUntil;
    }

    private final Database database;
    private final LongSupplier clock;
    private final long codeMillis;
    private final int maxAttempts;
    private final long lockMillis;
    private final SecureRandom random = new SecureRandom();

    private final Map<UUID, Link> byMinecraft = new HashMap<>();
    private final Map<String, Link> byDiscord = new HashMap<>();
    private final Map<String, Pending> codes = new HashMap<>();
    private final Map<String, Attempts> attempts = new HashMap<>();

    public LinkService(Database database, LongSupplier clock, int codeMinutes, int maxAttempts, int lockMinutes) {
        this.database = database;
        this.clock = clock;
        this.codeMillis = codeMinutes * 60_000L;
        this.maxAttempts = maxAttempts;
        this.lockMillis = lockMinutes * 60_000L;
        for (Link link : database.links()) {
            byMinecraft.put(link.minecraft(), link);
            byDiscord.put(link.discord(), link);
        }
    }

    public synchronized int size() {
        return byMinecraft.size();
    }

    public synchronized Optional<Link> byMinecraft(UUID player) {
        return Optional.ofNullable(byMinecraft.get(player));
    }

    public synchronized Optional<Link> byDiscord(String discordId) {
        return Optional.ofNullable(byDiscord.get(discordId));
    }

    /**
     * Gives the player a fresh code. Asking again replaces the earlier code.
     * @return the six digits, or empty if the player is already linked
     */
    public synchronized Optional<String> issueCode(UUID player, String name) {
        if (byMinecraft.containsKey(player)) {
            return Optional.empty();
        }
        long now = clock.getAsLong();
        codes.values().removeIf(pending -> pending.player().equals(player) || pending.expiresAt() <= now);
        String code;
        do {
            code = String.format("%06d", random.nextInt(1_000_000));
        } while (codes.containsKey(code));
        codes.put(code, new Pending(player, name, now + codeMillis));
        return Optional.of(code);
    }

    /** Trades a code typed in Discord for a link. Spaces and dashes in the code are ignored. */
    public synchronized Redeem redeem(String discordId, String typed) {
        long now = clock.getAsLong();
        Link existing = byDiscord.get(discordId);
        if (existing != null) {
            return new Redeem(Status.DISCORD_ALREADY_LINKED, existing, 0);
        }
        Attempts tries = attempts.computeIfAbsent(discordId, id -> new Attempts());
        if (tries.lockedUntil > now) {
            return new Redeem(Status.LOCKED, null, minutesUntil(tries.lockedUntil, now));
        }

        String code = typed == null ? "" : typed.replaceAll("[^0-9]", "");
        Pending pending = codes.get(code);
        if (pending != null && pending.expiresAt() <= now) {
            codes.remove(code);
            pending = null;
        }
        if (pending == null || byMinecraft.containsKey(pending.player())) {
            tries.wrong++;
            if (tries.wrong >= maxAttempts) {
                tries.wrong = 0;
                tries.lockedUntil = now + lockMillis;
                return new Redeem(Status.LOCKED, null, minutesUntil(tries.lockedUntil, now));
            }
            return new Redeem(Status.WRONG_CODE, null, 0);
        }

        codes.remove(code);
        attempts.remove(discordId);
        Link link = new Link(pending.player(), pending.name(), discordId, now);
        database.saveLink(link);
        byMinecraft.put(link.minecraft(), link);
        byDiscord.put(link.discord(), link);
        return new Redeem(Status.LINKED, link, 0);
    }

    /** Undoes the link of a Minecraft account. @return the link that was removed */
    public synchronized Optional<Link> unlinkMinecraft(UUID player) {
        Link link = byMinecraft.remove(player);
        if (link == null) {
            return Optional.empty();
        }
        byDiscord.remove(link.discord());
        database.deleteLink(player);
        return Optional.of(link);
    }

    /** Undoes the link of a Discord account. @return the link that was removed */
    public synchronized Optional<Link> unlinkDiscord(String discordId) {
        Link link = byDiscord.get(discordId);
        return link == null ? Optional.empty() : unlinkMinecraft(link.minecraft());
    }

    /** Keeps the stored name current when a player changes their Minecraft name. */
    public synchronized void rename(UUID player, String name) {
        Link link = byMinecraft.get(player);
        if (link != null && !link.name().equals(name)) {
            Link renamed = new Link(link.minecraft(), name, link.discord(), link.linkedAt());
            byMinecraft.put(player, renamed);
            byDiscord.put(renamed.discord(), renamed);
            database.saveLink(renamed);
        }
    }

    /** Forgets codes and locks that have run out. Called now and then so the maps stay small. */
    public synchronized void tidy() {
        long now = clock.getAsLong();
        codes.values().removeIf(pending -> pending.expiresAt() <= now);
        for (Iterator<Attempts> it = attempts.values().iterator(); it.hasNext(); ) {
            Attempts tries = it.next();
            if (tries.lockedUntil <= now && tries.wrong == 0) {
                it.remove();
            }
        }
    }

    /** "482913" becomes "482 913", which is easier to read and type. */
    public static String pretty(String code) {
        return code.length() == 6 ? code.substring(0, 3) + " " + code.substring(3) : code;
    }

    private static int minutesUntil(long time, long now) {
        return (int) Math.max(1, (time - now + 59_999) / 60_000);
    }
}
