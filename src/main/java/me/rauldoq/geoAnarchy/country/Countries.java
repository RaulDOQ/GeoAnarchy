package me.rauldoq.geoAnarchy.country;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What the rest of the plugin needs to know about countries. The real answers come from the Lands plugin
 * ({@code LandsHook}); without Lands, {@link #NONE} answers that there are no countries.
 * Call these from the server thread.
 */
public interface Countries {

    /** A war a country is in, or one that was declared and has not started. */
    record WarInfo(String enemy, boolean attacking, boolean started, long startsAt, int points, int enemyPoints, int pointsToWin) {
    }

    /** A country's numbers, for the /pais and /guerra commands. */
    record Status(Country country, double bank, double upkeep, int chunks, String level, int citizens,
                  List<String> online, WarInfo war) {
    }

    boolean available();

    /** The countries a player founded or is a citizen of, founded ones first. */
    List<Country> of(UUID player);

    Optional<Status> status(String countryId);

    /** May this player put money into the country's bank? The founder always may. */
    boolean canDeposit(String countryId, UUID player);

    /** Adds Geos to the country's bank. @return false if the country is gone or Lands refused */
    boolean addToBank(String countryId, double amount);

    Countries NONE = new Countries() {
        @Override
        public boolean available() {
            return false;
        }

        @Override
        public List<Country> of(UUID player) {
            return List.of();
        }

        @Override
        public Optional<Status> status(String countryId) {
            return Optional.empty();
        }

        @Override
        public boolean canDeposit(String countryId, UUID player) {
            return false;
        }

        @Override
        public boolean addToBank(String countryId, double amount) {
            return false;
        }
    };
}
