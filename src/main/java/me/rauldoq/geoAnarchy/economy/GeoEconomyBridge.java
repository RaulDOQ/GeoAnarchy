package me.rauldoq.geoAnarchy.economy;

import me.rauldoq.geoeconomy.GeoEconomy;
import me.rauldoq.geoeconomy.account.Account;
import me.rauldoq.geoeconomy.currency.Currency;
import me.rauldoq.geoeconomy.ledger.Result;
import me.rauldoq.geoeconomy.market.Market;
import me.rauldoq.geoeconomy.market.MarketItem;
import me.rauldoq.geoeconomy.money.Money;
import me.rauldoq.geoeconomy.storage.TransactionType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * The economy, answered by our GeoEconomy plugin. Every reference to GeoEconomy's classes is in this file,
 * so the rest of the plugin still loads on a server without it.
 */
public final class GeoEconomyBridge implements Economy {

    private final GeoEconomy geo;

    private GeoEconomyBridge(GeoEconomy geo) {
        this.geo = geo;
    }

    /** @return the bridge, or {@link Economy#NONE} if that plugin is not GeoEconomy */
    public static Economy connect(Plugin plugin) {
        return plugin instanceof GeoEconomy economy ? new GeoEconomyBridge(economy) : Economy.NONE;
    }

    @Override
    public boolean available() {
        return geo.isEnabled() && geo.isReady();
    }

    @Override
    public OptionalLong parse(String typed) {
        return Money.parse(typed);
    }

    @Override
    public String geos(long cents) {
        return geo.currencies().geo().format(cents);
    }

    @Override
    public long geoBalance(UUID player) {
        return geo.accounts().get(player).map(Account::balance).orElse(0L);
    }

    @Override
    public List<String> balances(UUID player) {
        List<String> lines = new ArrayList<>();
        Optional<Account> found = geo.accounts().get(player);
        Currency world = geo.currencies().geo();
        lines.add(world.format(found.map(Account::balance).orElse(0L)));
        if (found.isEmpty()) {
            return lines;
        }
        for (Currency currency : geo.currencies().all()) {
            if (currency.isGeo()) {
                continue;
            }
            long balance = found.get().balance(currency.code());
            if (balance > 0) {
                lines.add(currency.format(balance) + " (vale cerca de "
                        + world.format(geo.exchange().geoValue(currency, balance)) + ")");
            }
        }
        return lines;
    }

    @Override
    public boolean take(UUID player, long cents, String reason) {
        Optional<Account> account = geo.accounts().get(player);
        return account.isPresent()
                && geo.ledger().withdraw(account.get().wallet(), geo.currencies().geo().code(), cents, reason).success();
    }

    @Override
    public void give(UUID player, long cents, String reason) {
        geo.accounts().get(player).ifPresent(account ->
                geo.ledger().deposit(account.wallet(), geo.currencies().geo().code(), cents, reason));
    }

    @Override
    public Optional<String> accountName(String typedName) {
        return geo.accounts().findByName(typedName).map(Account::name);
    }

    @Override
    public Pay pay(UUID from, String toName, long cents) {
        Optional<Account> payer = geo.accounts().get(from);
        Optional<Account> receiver = geo.accounts().findByName(toName);
        if (receiver.isEmpty()) {
            return new Pay(PayStatus.NO_SUCH_PLAYER, toName, null);
        }
        String name = receiver.get().name();
        UUID id = receiver.get().uuid();
        if (payer.isEmpty()) {
            return new Pay(PayStatus.NOT_ENOUGH, name, id);
        }
        if (id.equals(from)) {
            return new Pay(PayStatus.SAME_PLAYER, name, id);
        }
        Result result = geo.ledger().transfer(payer.get().wallet(), receiver.get().wallet(),
                geo.currencies().geo().code(), cents, TransactionType.TRANSFER, "discord:/pagar");
        PayStatus status = switch (result.status()) {
            case SUCCESS -> PayStatus.PAID;
            case INSUFFICIENT_FUNDS -> PayStatus.NOT_ENOUGH;
            case BALANCE_LIMIT -> PayStatus.RECEIVER_FULL;
            default -> PayStatus.FAILED;
        };
        return new Pay(status, name, id);
    }

    @Override
    public List<Price> prices(String item) {
        Optional<MarketItem> found = geo.markets().catalog().find(item.trim().replace(' ', '_'));
        if (found.isEmpty()) {
            return List.of();
        }
        MarketItem marketItem = found.get();
        List<Price> prices = new ArrayList<>();
        for (Market market : geo.markets().all()) {
            if (!market.trades(marketItem.material())) {
                continue;
            }
            boolean trading = geo.markets().trades(market, marketItem);
            long stock = market.stock(marketItem.material());
            String currency = geo.currencies().get(market.currency()).map(Currency::plural).orElse(market.currency());
            prices.add(new Price(market.name(), currency,
                    trading ? geo.markets().sellPrice(market, marketItem) : -1,
                    trading && stock > 0 ? geo.markets().buyPrice(market, marketItem) : -1,
                    stock, market.open()));
        }
        prices.sort(Comparator.comparing(Price::market, String.CASE_INSENSITIVE_ORDER));
        return prices;
    }
}
