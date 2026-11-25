package me.rauldoq.geoAnarchy.alerts;

import me.rauldoq.geoAnarchy.alerts.AlertService.Alert;
import me.rauldoq.geoAnarchy.country.Country;
import me.rauldoq.geoAnarchy.link.LinkService;
import me.rauldoq.geoAnarchy.storage.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertServiceTest {

    private static final UUID FOUNDER = UUID.randomUUID();
    private static final UUID CITIZEN = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();
    private static final Alert ALERT = new Alert("Guerra", "Argentina declarou guerra ao Brasil.");

    @TempDir
    Path folder;
    private Database database;
    private LinkService links;
    private Prefs prefs;
    private AlertService alerts;
    private final List<String> sent = new ArrayList<>();
    private long now = 1_000_000;
    private Country brasil;

    @BeforeEach
    void open() throws Exception {
        database = new Database(folder.resolve("test.db"));
        links = new LinkService(database, () -> now, 10, 5, 5);
        prefs = new Prefs(database);
        alerts = new AlertService(links, prefs, () -> now);
        alerts.setSender((to, alert) -> sent.add(to.name()));
        brasil = new Country("id", "Brasil", FOUNDER, Set.of(FOUNDER, CITIZEN));
        link(FOUNDER, "Fundador", "1");
        link(CITIZEN, "Cidadao", "2");
        link(STRANGER, "Estranho", "3");
    }

    private void link(UUID player, String name, String discord) {
        links.redeem(discord, links.issueCode(player, name).orElseThrow());
    }

    @AfterEach
    void close() {
        database.close();
    }

    @Test
    void theFounderGetsCountryAlertsWithoutAskingAndCitizensDoNot() {
        assertEquals(1, alerts.toCountry(brasil, AlertKind.WAR, ALERT));
        assertEquals(List.of("Fundador"), sent);
    }

    @Test
    void aCitizenGetsThemAfterAskingForThem() {
        prefs.setCitizenAlerts(CITIZEN, true);
        alerts.toCountry(brasil, AlertKind.WAR, ALERT);
        assertEquals(Set.of("Fundador", "Cidadao"), Set.copyOf(sent));
    }

    @Test
    void aKindThatWasSwitchedOffIsNotSent() {
        prefs.setWants(FOUNDER, AlertKind.WAR, false);
        assertEquals(0, alerts.toCountry(brasil, AlertKind.WAR, ALERT));
        assertEquals(1, alerts.toCountry(brasil, AlertKind.UPKEEP, ALERT), "other kinds still arrive");

        prefs.setWants(FOUNDER, AlertKind.WAR, true);
        assertEquals(1, alerts.toCountry(brasil, AlertKind.WAR, ALERT));
    }

    @Test
    void aFounderWhoIsNotLinkedGetsNothing() {
        links.unlinkMinecraft(FOUNDER);
        assertEquals(0, alerts.toCountry(brasil, AlertKind.WAR, ALERT));
        assertTrue(sent.isEmpty());
    }

    @Test
    void anAlertForOnePlayerRespectsTheirSwitch() {
        assertTrue(alerts.toPlayer(STRANGER, AlertKind.INVITES, ALERT));
        prefs.setWants(STRANGER, AlertKind.INVITES, false);
        assertFalse(alerts.toPlayer(STRANGER, AlertKind.INVITES, ALERT));
        assertFalse(alerts.toPlayer(UUID.randomUUID(), AlertKind.INVITES, ALERT), "someone who never linked gets nothing");
        assertEquals(List.of("Estranho"), sent);
    }

    @Test
    void repeatedAlertsAreHeldBackForAWhile() {
        assertTrue(alerts.allow("flag:brasil", 5 * 60_000));
        assertFalse(alerts.allow("flag:brasil", 5 * 60_000));
        assertTrue(alerts.allow("flag:argentina", 5 * 60_000), "each country has its own timer");
        now += 5 * 60_000;
        assertTrue(alerts.allow("flag:brasil", 5 * 60_000));
    }

    @Test
    void choicesSurviveARestartAndDefaultsAreNotStored() {
        prefs.setCitizenAlerts(CITIZEN, true);
        prefs.setActions(FOUNDER, false);
        prefs.setDmClosed(FOUNDER, true);
        Prefs again = new Prefs(database);
        assertTrue(again.citizenAlerts(CITIZEN));
        assertFalse(again.actions(FOUNDER));
        assertTrue(again.dmClosed(FOUNDER));
        assertTrue(again.actions(CITIZEN), "Discord actions are on unless the player switches them off");
        assertFalse(again.citizenAlerts(FOUNDER));
        for (AlertKind kind : AlertKind.values()) {
            assertTrue(again.wants(STRANGER, kind), kind + " is on by default");
            assertEquals(kind, AlertKind.byId(kind.id()));
        }
    }
}
