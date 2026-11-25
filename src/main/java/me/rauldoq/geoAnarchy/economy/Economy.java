package me.rauldoq.geoAnarchy.economy;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * What the plugin needs from the economy. The real answers come from our GeoEconomy plugin
 * ({@code GeoEconomyBridge}); without it, {@link #NONE} says the economy is not there.
 * Amounts are whole cents. Call these from the server thread.
 */
public interface Economy {

    enum PayStatus {
        PAID, NO_SUCH_PLAYER, SAME_PLAYER, NOT_ENOUGH, RECEIVER_FULL, FAILED
    }

    /** @param receiver the receiving player's name as the economy knows it */
    record Pay(PayStatus status, String receiver, UUID receiverId) {
    }

    /** What one market pays and charges for an item. A price below zero means "not right now". */
    record Price(String market, String currency, double sell, double buy, long stock, boolean open) {
    }

    boolean available();

    /** Reads an amount a player typed, such as "250", "12,5" or "3k". Empty if it is not a valid amount. */
    OptionalLong parse(String typed);

    /** "1,234.50 Geos" */
    String geos(long cents);

    long geoBalance(UUID player);

    /** One line per currency the player holds, Geos first. */
    List<String> balances(UUID player);

    /** Takes Geos out of a player's wallet. @return false if they don't have that much */
    boolean take(UUID player, long cents, String reason);

    /** Puts Geos into a player's wallet. Used to give money back when something else failed. */
    void give(UUID player, long cents, String reason);

    /** The account's exact name, if a player with that name has ever joined. */
    Optional<String> accountName(String typedName);

    Pay pay(UUID from, String toName, long cents);

    /** Every market's price for an item, or empty if no market trades it. The item is its Minecraft name. */
    List<Price> prices(String item);

    Economy NONE = new Economy() {
        @Override
        public boolean available() {
            return false;
        }

        @Override
        public OptionalLong parse(String typed) {
            return OptionalLong.empty();
        }

        @Override
        public String geos(long cents) {
            return String.format(java.util.Locale.US, "%,.2f Geos", cents / 100.0);
        }

        @Override
        public long geoBalance(UUID player) {
            return 0;
        }

        @Override
        public List<String> balances(UUID player) {
            return List.of();
        }

        @Override
        public boolean take(UUID player, long cents, String reason) {
            return false;
        }

        @Override
        public void give(UUID player, long cents, String reason) {
        }

        @Override
        public Optional<String> accountName(String typedName) {
            return Optional.empty();
        }

        @Override
        public Pay pay(UUID from, String toName, long cents) {
            return new Pay(PayStatus.FAILED, toName, null);
        }

        @Override
        public List<Price> prices(String item) {
            return List.of();
        }
    };
}
