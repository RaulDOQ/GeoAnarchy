package me.rauldoq.geoAnarchy.country;

import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.applicationframework.util.ULID;
import me.angeschossen.lands.api.events.ChunkDeleteEvent;
import me.angeschossen.lands.api.events.LandInvitePlayerEvent;
import me.angeschossen.lands.api.events.land.DeleteReason;
import me.angeschossen.lands.api.events.memberholder.MemberHolderEvent;
import me.angeschossen.lands.api.events.memberholder.MemberHolderUpkeepEvent;
import me.angeschossen.lands.api.events.war.WarDeclareEvent;
import me.angeschossen.lands.api.events.war.WarEndEvent;
import me.angeschossen.lands.api.events.war.WarStartEvent;
import me.angeschossen.lands.api.events.war.captureflag.CaptureFlagCapturedEvent;
import me.angeschossen.lands.api.flags.type.Flags;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.memberholder.MemberHolder;
import me.angeschossen.lands.api.war.War;
import me.angeschossen.lands.api.war.captureflag.CaptureFlag;
import me.angeschossen.lands.api.war.enums.WarResult;
import me.rauldoq.geoAnarchy.alerts.AlertKind;
import me.rauldoq.geoAnarchy.alerts.AlertService;
import me.rauldoq.geoAnarchy.alerts.AlertService.Alert;
import me.rauldoq.geoAnarchy.config.Messages;
import me.rauldoq.geoAnarchy.config.Settings;
import me.rauldoq.geoAnarchy.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Everything that touches the Lands plugin: reading countries, and turning Lands' events into alerts.
 * Every reference to the Lands API is in this file, so the plugin still loads on a server without Lands.
 * <p>
 * Written against the Lands API 7.25 and checked against Lands 9.3.0, which the server runs.
 * Lands may call its events from another thread; nothing here needs the server thread.
 */
public final class LandsHook implements Countries, Listener {

    private final Plugin plugin;
    private final LandsIntegration lands;
    private final AlertService alerts;
    private final Supplier<Settings> settings;
    private final Supplier<Messages> messages;
    private final Supplier<Economy> economy;
    /** Posts a text in the public war channel. */
    private final Consumer<String> announce;

    /** Wars that were declared and have not started: "attackerId>defenderId" to the moment the fight begins. */
    private final Map<String, Long> declared = new ConcurrentHashMap<>();
    /** Chunks lost for unpaid upkeep, counted per country until the alert goes out. */
    private final Map<String, Integer> lostChunks = new ConcurrentHashMap<>();

    public LandsHook(Plugin plugin, AlertService alerts, Supplier<Settings> settings, Supplier<Messages> messages,
                     Supplier<Economy> economy, Consumer<String> announce) {
        this.plugin = plugin;
        this.lands = LandsIntegration.of(plugin);
        this.alerts = alerts;
        this.settings = settings;
        this.messages = messages;
        this.economy = economy;
        this.announce = announce;
    }

    @Override
    public boolean claimedNear(org.bukkit.World world, int x, int z, int radiusChunks) {
        int cx = x >> 4;
        int cz = z >> 4;
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                if (lands.getLandByUnloadedChunk(world, cx + dx, cz + dz) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<Country> all() {
        List<Country> out = new ArrayList<>();
        for (Land land : lands.getLands()) {
            if (quietly(land::exists, true) && isCountry(land)) {
                out.add(country(land));
            }
        }
        return out;
    }

    @Override
    public List<Country> led(UUID player, java.util.Collection<String> leaderRoles) {
        List<Country> out = new ArrayList<>();
        for (Land land : lands.getLands()) {
            if (isCountry(land) && (player.equals(land.getOwnerUID())
                    || (land.isTrusted(player) && hasLeaderRole(land, player, leaderRoles)))) {
                out.add(country(land));
            }
        }
        // founded ones first, like of()
        out.sort(java.util.Comparator.comparing(c -> !player.equals(c.founder())));
        return out;
    }

    @Override
    public List<UUID> leaders(String countryId, java.util.Collection<String> leaderRoles) {
        Land land = find(countryId);
        if (land == null) {
            return List.of();
        }
        List<UUID> out = new ArrayList<>();
        if (land.getOwnerUID() != null) {
            out.add(land.getOwnerUID());
        }
        for (UUID member : land.getTrustedPlayers()) {
            if (!member.equals(land.getOwnerUID()) && hasLeaderRole(land, member, leaderRoles)) {
                out.add(member);
            }
        }
        return out;
    }

    private static boolean hasLeaderRole(Land land, UUID player, java.util.Collection<String> leaderRoles) {
        String role = quietly(() -> stripColors(land.getDefaultArea().getRole(player).getName()), "");
        for (String wanted : leaderRoles) {
            if (!role.isEmpty() && role.equalsIgnoreCase(stripColors(wanted))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<Country> at(org.bukkit.Location where) {
        if (where.getWorld() == null) {
            return Optional.empty();
        }
        Land land = quietly(() -> lands.getLandByChunk(where.getWorld(), where.getBlockX() >> 4, where.getBlockZ() >> 4), null);
        return land == null || !isCountry(land) ? Optional.empty() : Optional.of(country(land));
    }

    @Override
    public Optional<int[]> home(String countryId, org.bukkit.World world) {
        Land land = find(countryId);
        if (land == null) {
            return Optional.empty();
        }
        org.bukkit.Location spawn = quietly(land::getSpawn, null);
        if (spawn != null && world.equals(spawn.getWorld())) {
            return Optional.of(new int[]{spawn.getBlockX(), spawn.getBlockZ()});
        }
        var chunks = quietly(() -> land.getChunks(world), List.<me.angeschossen.lands.api.land.ChunkCoordinate>of());
        for (var chunk : chunks) {
            return Optional.of(new int[]{(chunk.getX() << 4) + 8, (chunk.getZ() << 4) + 8});
        }
        return Optional.empty();
    }

    /** Starts listening to Lands. Call once, from onEnable. */
    public void listen() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Two events are newer than the API this is compiled against, so they are looked up by name.
        listenByName("me.angeschossen.lands.api.events.memberholder.MemberHolderUpkeepReminderEvent", this::onUpkeepReminder);
        listenByName("me.angeschossen.lands.api.events.memberholder.MemberHolderRelationRequestEvent", this::onRelationRequest);
    }

    private void listenByName(String className, Consumer<Event> handler) {
        try {
            Class<? extends Event> type = Class.forName(className).asSubclass(Event.class);
            Bukkit.getPluginManager().registerEvent(type, this, EventPriority.MONITOR, (listener, event) -> {
                if (type.isInstance(event)) {
                    guarded(() -> handler.accept(event));
                }
            }, plugin, true);
        } catch (ClassNotFoundException | LinkageError | ClassCastException missing) {
            plugin.getLogger().info("This Lands version has no " + className.substring(className.lastIndexOf('.') + 1)
                    + "; the alert that depends on it is off.");
        }
    }

    /** An alert must never break Lands, so nothing thrown here leaves the handler. */
    private void guarded(Runnable work) {
        try {
            work.run();
        } catch (Throwable error) {
            plugin.getLogger().log(Level.WARNING, "Could not send a country alert.", error);
        }
    }

    // ---- Countries ----

    /**
     * Only real countries count. Lands also keeps camps (the free one-day camp a new player can place) and admin
     * areas as lands; joining or owning one of those is not having a country.
     */
    private static boolean isCountry(Land land) {
        return quietly(() -> land.getLandType() == me.angeschossen.lands.api.land.enums.LandType.LAND, true);
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public List<Country> of(UUID player) {
        List<Country> founded = new ArrayList<>();
        List<Country> citizen = new ArrayList<>();
        for (Land land : lands.getLands()) {
            if (!isCountry(land)) {
                continue;
            }
            if (player.equals(land.getOwnerUID())) {
                founded.add(country(land));
            } else if (land.isTrusted(player)) {
                citizen.add(country(land));
            }
        }
        founded.addAll(citizen);
        return founded;
    }

    @Override
    public Optional<String> coloredName(UUID player) {
        Land citizenOf = null;
        for (Land land : lands.getLands()) {
            if (!isCountry(land)) {
                continue;
            }
            if (player.equals(land.getOwnerUID())) {
                return Optional.of(colored(land));
            }
            if (citizenOf == null && land.isTrusted(player)) {
                citizenOf = land;
            }
        }
        return Optional.ofNullable(citizenOf).map(LandsHook::colored);
    }

    private static String colored(Land land) {
        return quietly(land::getColorName, land.getName());
    }

    @Override
    public Optional<Status> status(String countryId) {
        Land land = find(countryId);
        if (land == null) {
            return Optional.empty();
        }
        Country country = country(land);
        List<String> online = new ArrayList<>();
        for (Player player : land.getOnlinePlayers()) {
            online.add(player.getName());
        }
        online.sort(String.CASE_INSENSITIVE_ORDER);
        return Optional.of(new Status(country, land.getBalance(), quietly(land::getUpkeepCosts, 0.0),
                land.getChunksAmount(), quietly(() -> stripColors(land.getLevel().getName()), ""),
                country.members().size(), online, war(land)));
    }

    private War warOf(Land land) {
        return quietly(() -> land.isInWar() ? land.getWar() : null, null);
    }

    private WarInfo war(Land land) {
        String id = land.getULID().toString();
        War war = warOf(land);
        if (war != null) {
            boolean attacking = id.equals(war.getAttacker().getULID().toString());
            MemberHolder enemy = attacking ? war.getDefender() : war.getAttacker();
            int attackerPoints = quietly(() -> war.getAttackerStats().getPoints(), 0);
            int defenderPoints = quietly(() -> war.getDefenderStats().getPoints(), 0);
            // The target score changed its type between Lands versions, so it is read by name.
            int toWin = quietly(() -> {
                try {
                    return ((Number) War.class.getMethod("getPointsToWin").invoke(war)).intValue();
                } catch (ReflectiveOperationException different) {
                    return 0;
                }
            }, 0);
            return new WarInfo(enemy.getName(), attacking, true, 0,
                    attacking ? attackerPoints : defenderPoints, attacking ? defenderPoints : attackerPoints, toWin);
        }
        for (Map.Entry<String, Long> entry : declared.entrySet()) {
            String[] sides = entry.getKey().split(">");
            if (sides.length == 2 && (sides[0].equals(id) || sides[1].equals(id))) {
                boolean attacking = sides[0].equals(id);
                Land enemy = find(attacking ? sides[1] : sides[0]);
                if (enemy != null) {
                    return new WarInfo(enemy.getName(), attacking, false, entry.getValue(), 0, 0, 0);
                }
            }
        }
        return null;
    }

    @Override
    public boolean canDeposit(String countryId, UUID player) {
        Land land = find(countryId);
        if (land == null) {
            return false;
        }
        if (player.equals(land.getOwnerUID())) {
            return true;
        }
        // "Deposit into the bank" is looked up by name, because older Lands libraries don't list it.
        RoleFlag deposit = quietly(() -> lands.getFlagRegistry().getRole("balance_deposit"), Flags.BALANCE_WITHDRAW);
        return land.isTrusted(player) && land.getDefaultArea().hasRoleFlag(player, deposit);
    }

    @Override
    public boolean addToBank(String countryId, double amount) {
        Land land = find(countryId);
        return land != null && amount > 0 && land.modifyBalance(amount);
    }

    private Land find(String countryId) {
        try {
            Land land = lands.getLandByULID(ULID.fromString(countryId));
            return land != null && land.exists() && isCountry(land) ? land : null;
        } catch (RuntimeException notAnId) {
            return null;
        }
    }

    private static Country country(MemberHolder holder) {
        Set<UUID> members = new HashSet<>(holder.getTrustedPlayers());
        if (holder.getOwnerUID() != null) {
            members.add(holder.getOwnerUID());
        }
        return new Country(holder.getULID().toString(), holder.getName(), holder.getOwnerUID(), Set.copyOf(members));
    }

    private static String stripColors(String text) {
        return text == null ? "" : text.replaceAll("<[^>]*>", "").replaceAll("[&§][0-9a-fk-orx]", "").trim();
    }

    private static <T> T quietly(Supplier<T> read, T fallback) {
        try {
            T value = read.get();
            return value == null ? fallback : value;
        } catch (Throwable notInThisVersion) {
            return fallback;
        }
    }

    // ---- Alerts ----

    private Alert alert(String key, String... placeholders) {
        Messages texts = messages.get();
        String title = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
        return new Alert(texts.text("alert." + title + ".title"), texts.text("alert." + key, placeholders));
    }

    private static String when(long epochMillis) {
        long seconds = epochMillis / 1000;
        return "<t:" + seconds + ":R> (<t:" + seconds + ":t>)";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWarDeclare(WarDeclareEvent event) {
        guarded(() -> {
            MemberHolder attacker = event.getAttacker();
            MemberHolder defender = event.getDefender();
            long startsAt = System.currentTimeMillis() + settings.get().warPreparationMinutes() * 60_000L;
            declared.put(attacker.getULID() + ">" + defender.getULID(), startsAt);
            String when = when(startsAt);
            alerts.toCountry(country(defender), AlertKind.WAR, alert("war-declared.defender",
                    "attacker", attacker.getName(), "country", defender.getName(), "when", when));
            alerts.toCountry(country(attacker), AlertKind.WAR, alert("war-declared.attacker",
                    "defender", defender.getName(), "country", attacker.getName(), "when", when));
            announce.accept(messages.get().text("alert.war-declared.public",
                    "attacker", attacker.getName(), "defender", defender.getName(), "when", when));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWarStart(WarStartEvent event) {
        guarded(() -> {
            MemberHolder attacker = event.getWar().getAttacker();
            MemberHolder defender = event.getWar().getDefender();
            declared.remove(attacker.getULID() + ">" + defender.getULID());
            Alert alert = alert("war-started.text", "attacker", attacker.getName(), "defender", defender.getName());
            alerts.toCountry(country(attacker), AlertKind.WAR, alert);
            alerts.toCountry(country(defender), AlertKind.WAR, alert);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWarEnd(WarEndEvent event) {
        guarded(() -> {
            MemberHolder attacker = event.getWar().getAttacker();
            MemberHolder defender = event.getWar().getDefender();
            declared.remove(attacker.getULID() + ">" + defender.getULID());
            MemberHolder winner = event.getWinner();
            MemberHolder loser = event.getLoser();
            WarResult result = event.getResult();
            if (winner == null || loser == null || result == WarResult.DRAW || result == WarResult.ADMIN) {
                Alert alert = alert(result == WarResult.ADMIN ? "war-ended.admin" : "war-ended.draw",
                        "attacker", attacker.getName(), "defender", defender.getName());
                alerts.toCountry(country(attacker), AlertKind.WAR, alert);
                alerts.toCountry(country(defender), AlertKind.WAR, alert);
                return;
            }
            boolean surrendered = result == WarResult.SURRENDERED;
            alerts.toCountry(country(winner), AlertKind.WAR, alert(surrendered ? "war-ended.surrendered-won" : "war-ended.won",
                    "country", winner.getName(), "enemy", loser.getName()));
            alerts.toCountry(country(loser), AlertKind.WAR, alert(surrendered ? "war-ended.surrendered-lost" : "war-ended.lost",
                    "country", loser.getName(), "enemy", winner.getName()));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFlagCaptured(CaptureFlagCapturedEvent event) {
        guarded(() -> {
            CaptureFlag flag = event.getCaptureFlag();
            // The server's Lands has CaptureFlag.getWar(); the library this is compiled against does not, so it is
            // called by name.
            War war;
            try {
                war = (War) CaptureFlag.class.getMethod("getWar").invoke(flag);
            } catch (ReflectiveOperationException | ClassCastException different) {
                return;
            }
            if (war == null) {
                return;
            }
            // Whoever placed the flag is the invader; the other side is losing ground.
            boolean attackerPlaced = "ATTACKER".equalsIgnoreCase(String.valueOf(flag.getPlacedByTeam()));
            MemberHolder invader = attackerPlaced ? war.getAttacker() : war.getDefender();
            MemberHolder victim = attackerPlaced ? war.getDefender() : war.getAttacker();
            if (invader == null || victim == null) {
                return;
            }
            long cooldown = settings.get().flagCooldownMinutes() * 60_000L;
            if (alerts.allow("flag:" + victim.getULID(), cooldown)) {
                alerts.toCountry(country(victim), AlertKind.WAR,
                        alert("flag.text", "enemy", invader.getName(), "country", victim.getName()));
            }
        });
    }

    /** At collection time: the country could not pay. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUpkeep(MemberHolderUpkeepEvent event) {
        guarded(() -> {
            if (event.getBalance() < event.getUpkeep()) {
                MemberHolder holder = event.getMemberHolder();
                alerts.toCountry(country(holder), AlertKind.UPKEEP, alert("upkeep-unpaid.text",
                        "country", holder.getName(), "upkeep", geos(event.getUpkeep()), "bank", geos(event.getBalance())));
            }
        });
    }

    /** In the hours before collection: the bank is short. */
    private void onUpkeepReminder(Event raw) {
        if (!(raw instanceof MemberHolderUpkeepEvent event) || event.getBalance() >= event.getUpkeep()) {
            return;
        }
        MemberHolder holder = event.getMemberHolder();
        long cooldown = settings.get().upkeepCooldownHours() * 3_600_000L;
        if (alerts.allow("upkeep:" + holder.getULID(), cooldown)) {
            alerts.toCountry(country(holder), AlertKind.UPKEEP, alert("upkeep-short.text",
                    "country", holder.getName(), "upkeep", geos(event.getUpkeep()), "bank", geos(event.getBalance()),
                    "missing", geos(Math.max(0, event.getUpkeep() - event.getBalance()))));
        }
    }

    /** Chunks released for unpaid upkeep come one event at a time, so they are counted and reported together. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkDelete(ChunkDeleteEvent event) {
        guarded(() -> {
            if (event.getReason() != DeleteReason.UPKEEP || event.getLand() == null) {
                return;
            }
            Land land = event.getLand();
            String id = land.getULID().toString();
            if (lostChunks.merge(id, 1, Integer::sum) == 1) {
                Country country = country(land);
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> guarded(() -> {
                    Integer amount = lostChunks.remove(id);
                    if (amount != null) {
                        alerts.toCountry(country, AlertKind.UPKEEP, alert("chunks-lost.text",
                                "country", country.name(), "amount", String.valueOf(amount)));
                    }
                }), 60L);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInvite(LandInvitePlayerEvent event) {
        guarded(() -> {
            if (event.getTargetUUID() != null && event.getLand() != null) {
                alerts.toPlayer(event.getTargetUUID(), AlertKind.INVITES,
                        alert("invite.text", "country", event.getLand().getName()));
            }
        });
    }

    /** Another country asked for an alliance or another change of relation. */
    private void onRelationRequest(Event raw) {
        if (!(raw instanceof MemberHolderEvent event)) {
            return;
        }
        try {
            MemberHolder sender = event.getMemberHolder();
            MemberHolder target = (MemberHolder) raw.getClass().getMethod("getTarget").invoke(raw);
            String intent = String.valueOf(raw.getClass().getMethod("getIntent").invoke(raw)).toLowerCase();
            String key = switch (intent) {
                case "ally" -> "relation.ally";
                case "enemy" -> "relation.enemy";
                default -> "relation.neutral";
            };
            if (sender != null && target != null) {
                alerts.toCountry(country(target), AlertKind.DIPLOMACY,
                        alert(key, "sender", sender.getName(), "country", target.getName()));
            }
        } catch (ReflectiveOperationException | ClassCastException different) {
            plugin.getLogger().log(Level.FINE, "Relation request event has a different shape in this Lands version.", different);
        }
    }

    private String geos(double amount) {
        return economy.get().geos(Math.round(amount * 100));
    }
}
