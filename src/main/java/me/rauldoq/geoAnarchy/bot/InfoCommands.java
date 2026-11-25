package me.rauldoq.geoAnarchy.bot;

import me.rauldoq.geoAnarchy.GeoAnarchy;
import me.rauldoq.geoAnarchy.config.Messages;
import me.rauldoq.geoAnarchy.country.Countries;
import me.rauldoq.geoAnarchy.country.Country;
import me.rauldoq.geoAnarchy.economy.Economy;
import me.rauldoq.geoAnarchy.storage.Database.Link;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Discord commands that only look: /saldo, /pais, /guerra and /preco. They work whether the player is in
 * the game or not. Every answer is private (only the person who asked sees it).
 */
public final class InfoCommands {

    private final GeoAnarchy plugin;
    private final Bot bot;

    public InfoCommands(GeoAnarchy plugin, Bot bot) {
        this.plugin = plugin;
        this.bot = bot;
    }

    public void register() {
        bot.add(new Slash("saldo", "Mostra o seu dinheiro no servidor, em todas as moedas.", null, this::saldo));
        bot.add(new Slash("pais", "Mostra o banco, a manutenção, o território e quem está online no seu país.", null, this::pais));
        bot.add(new Slash("guerra", "Mostra as guerras do seu país: contra quem, quando começa e o placar.", null, this::guerra));
        bot.add(new Slash("preco", "Mostra quanto cada mercado paga e cobra por um item.",
                List.of(new OptionData(OptionType.STRING, "item", "O nome do item como no Minecraft em inglês, por exemplo diamond", true)
                        .setMaxLength(60)),
                this::preco));
    }

    /** The linked account of whoever used the command. If there is none, they are told how to link. */
    static Optional<Link> linked(GeoAnarchy plugin, IReplyCallback event) {
        Optional<Link> link = plugin.links().byDiscord(event.getUser().getId());
        if (link.isEmpty()) {
            event.reply(plugin.messages().text("link.needed")).setEphemeral(true).queue();
        }
        return link;
    }

    /** Runs the work on the server thread and shows what it built, privately. */
    private void answer(SlashCommandInteractionEvent event, Supplier<List<MessageEmbed>> work) {
        event.deferReply(true).queue(hook -> plugin.onMain(work).whenComplete((embeds, error) -> {
            if (error != null || embeds == null) {
                if (error != null) {
                    bot.failed(error);
                }
                hook.editOriginal(plugin.messages().text("error")).queue();
            } else {
                hook.editOriginalEmbeds(embeds).queue();
            }
        }));
    }

    private static MessageEmbed card(String title, String text) {
        return new EmbedBuilder().setColor(Bot.COLOR).setTitle(title).setDescription(text).build();
    }

    private void saldo(SlashCommandInteractionEvent event) {
        Optional<Link> link = linked(plugin, event);
        if (link.isEmpty()) {
            return;
        }
        answer(event, () -> {
            Messages texts = plugin.messages();
            Economy economy = plugin.economy();
            if (!economy.available()) {
                return List.of(card(texts.text("saldo.title", "player", link.get().name()), texts.text("economy-off")));
            }
            StringBuilder lines = new StringBuilder();
            for (String line : economy.balances(link.get().minecraft())) {
                lines.append("• ").append(line).append('\n');
            }
            return List.of(card(texts.text("saldo.title", "player", link.get().name()), lines.toString()));
        });
    }

    private void pais(SlashCommandInteractionEvent event) {
        Optional<Link> link = linked(plugin, event);
        if (link.isEmpty()) {
            return;
        }
        answer(event, () -> {
            Messages texts = plugin.messages();
            Economy economy = plugin.economy();
            List<MessageEmbed> cards = new ArrayList<>();
            for (Country country : plugin.countries().of(link.get().minecraft())) {
                Optional<Countries.Status> found = plugin.countries().status(country.id());
                if (found.isEmpty() || cards.size() == 5) {
                    continue;
                }
                Countries.Status status = found.get();
                long bank = Math.round(status.bank() * 100);
                long upkeep = Math.round(status.upkeep() * 100);
                EmbedBuilder card = new EmbedBuilder().setColor(Bot.COLOR)
                        .setTitle(status.level().isEmpty() ? country.name() : country.name() + " · " + status.level())
                        .addField(texts.text("pais.bank"), economy.geos(bank), true)
                        .addField(texts.text("pais.upkeep"), economy.geos(upkeep) + "\n"
                                + texts.text(bank >= upkeep ? "pais.upkeep-ok" : "pais.upkeep-short",
                                "missing", economy.geos(Math.max(0, upkeep - bank))), true)
                        .addField(texts.text("pais.land"), texts.text("pais.chunks", "amount", String.valueOf(status.chunks())), true)
                        .addField(texts.text("pais.citizens", "amount", String.valueOf(status.citizens())),
                                status.online().isEmpty() ? texts.text("pais.nobody-online")
                                        : texts.text("pais.online", "players", String.join(", ", status.online())), false);
                if (status.war() != null) {
                    card.addField(texts.text("pais.war"), describe(status.war(), texts), false);
                }
                if (country.founder() != null && country.founder().equals(link.get().minecraft())) {
                    card.setFooter(texts.text("pais.founder"));
                }
                cards.add(card.build());
            }
            if (cards.isEmpty()) {
                return List.of(card(texts.text("pais.title"),
                        texts.text(plugin.countries().available() ? "pais.none" : "countries-off")));
            }
            return cards;
        });
    }

    private void guerra(SlashCommandInteractionEvent event) {
        Optional<Link> link = linked(plugin, event);
        if (link.isEmpty()) {
            return;
        }
        answer(event, () -> {
            Messages texts = plugin.messages();
            StringBuilder lines = new StringBuilder();
            for (Country country : plugin.countries().of(link.get().minecraft())) {
                plugin.countries().status(country.id()).map(Countries.Status::war).ifPresent(war ->
                        lines.append("**").append(country.name()).append("**\n").append(describe(war, texts)).append("\n\n"));
            }
            if (lines.length() == 0) {
                lines.append(texts.text(plugin.countries().available() ? "guerra.none" : "countries-off"));
            }
            return List.of(card(texts.text("guerra.title"), lines.toString()));
        });
    }

    private static String describe(Countries.WarInfo war, Messages texts) {
        if (!war.started()) {
            long seconds = war.startsAt() / 1000;
            return texts.text(war.attacking() ? "guerra.declared-by-us" : "guerra.declared-on-us",
                    "enemy", war.enemy(), "when", "<t:" + seconds + ":R> (<t:" + seconds + ":t>)");
        }
        String line = texts.text("guerra.running", "enemy", war.enemy(),
                "points", String.valueOf(war.points()), "enemy-points", String.valueOf(war.enemyPoints()));
        return war.pointsToWin() > 0 ? line + " " + texts.text("guerra.to-win", "points", String.valueOf(war.pointsToWin())) : line;
    }

    private void preco(SlashCommandInteractionEvent event) {
        String item = event.getOption("item", "", OptionMapping::getAsString);
        answer(event, () -> {
            Messages texts = plugin.messages();
            Economy economy = plugin.economy();
            String title = texts.text("preco.title", "item", item);
            if (!economy.available()) {
                return List.of(card(title, texts.text("economy-off")));
            }
            List<Economy.Price> prices = economy.prices(item);
            if (prices.isEmpty()) {
                return List.of(card(title, texts.text("preco.none", "item", item)));
            }
            StringBuilder lines = new StringBuilder();
            for (Economy.Price price : prices) {
                lines.append("**").append(price.market()).append("** (").append(price.currency()).append(")");
                if (!price.open()) {
                    lines.append(" · ").append(texts.text("preco.closed"));
                }
                lines.append('\n')
                        .append(texts.text("preco.sell", "price", price.sell() < 0 ? texts.text("preco.not-now") : number(price.sell())))
                        .append(" · ")
                        .append(texts.text("preco.buy", "price", price.buy() < 0 ? texts.text("preco.no-stock") : number(price.buy())))
                        .append(" · ")
                        .append(texts.text("preco.stock", "amount", String.format(Locale.US, "%,d", price.stock())))
                        .append("\n\n");
            }
            lines.append(texts.text("preco.footer"));
            return List.of(card(title, lines.toString()));
        });
    }

    private static String number(double value) {
        return String.format(Locale.US, Math.abs(value) >= 1 ? "%,.2f" : "%,.4f", value);
    }
}
