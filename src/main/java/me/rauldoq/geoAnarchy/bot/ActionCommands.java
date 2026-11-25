package me.rauldoq.geoAnarchy.bot;

import me.rauldoq.geoAnarchy.GeoAnarchy;
import me.rauldoq.geoAnarchy.config.Messages;
import me.rauldoq.geoAnarchy.country.Country;
import me.rauldoq.geoAnarchy.economy.Economy;
import me.rauldoq.geoAnarchy.storage.Database.Link;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Discord commands that move money: /depositar (wallet to the country's bank) and /pagar (to another player).
 * <p>
 * Safety, because a stolen Discord account must not empty someone's wallet:
 * <ul>
 *   <li>Nothing happens until the person presses Confirmar, and the button only works for them, for a minute.</li>
 *   <li>/pagar has a daily limit per player (config.yml).</li>
 *   <li>/depositar follows the country's own permissions, the same as /lands deposit in game.</li>
 *   <li>Everything done here is shown to the player the next time they log in.</li>
 *   <li>A player can switch all of this off for themselves in game: /discord acoes off.</li>
 * </ul>
 */
public final class ActionCommands {

    private static final String CONFIRM = "geo:act:";
    private static final ZoneId BRAZIL = ZoneId.of("America/Sao_Paulo");

    private enum Kind {
        DEPOSIT, PAY
    }

    /** Something a player asked for and has not confirmed yet. */
    private record Pending(String discordId, UUID player, String playerName, Kind kind, long cents, String target,
                           String targetName, long expiresAt) {
    }

    /** What a step decided: the text to show, and whether a Confirmar button goes with it. */
    private record Step(String text, String confirmId) {
        static Step say(String text) {
            return new Step(text, null);
        }
    }

    private final GeoAnarchy plugin;
    private final Bot bot;
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();

    public ActionCommands(GeoAnarchy plugin, Bot bot) {
        this.plugin = plugin;
        this.bot = bot;
    }

    public void register() {
        bot.add(new Slash("depositar", "Deposita Geos da sua carteira no banco do seu país.",
                List.of(new OptionData(OptionType.STRING, "valor", "Quantos Geos, por exemplo 2500 ou 2,5k", true).setMaxLength(20)),
                this::depositar));
        bot.add(new Slash("pagar", "Envia Geos da sua carteira para outro jogador.",
                List.of(new OptionData(OptionType.STRING, "jogador", "O nome do jogador no Minecraft", true).setMaxLength(32),
                        new OptionData(OptionType.STRING, "valor", "Quantos Geos, por exemplo 500 ou 12,5", true).setMaxLength(20)),
                this::pagar));
        bot.onButton(CONFIRM, this::confirmed);
    }

    /** Checks that apply to every action. @return the reason it can't go ahead, or null if it can */
    private String blocked(Link link) {
        Messages texts = plugin.messages();
        if (!plugin.settings().actionsEnabled()) {
            return texts.text("action.disabled");
        }
        if (!plugin.prefs().actions(link.minecraft())) {
            return texts.text("action.off-by-player");
        }
        if (!plugin.economy().available()) {
            return texts.text("economy-off");
        }
        return null;
    }

    private void ask(SlashCommandInteractionEvent event, Supplier<Step> work) {
        event.deferReply(true).queue(hook -> plugin.onMain(work).whenComplete((step, error) -> {
            if (error != null || step == null) {
                if (error != null) {
                    bot.failed(error);
                }
                hook.editOriginal(plugin.messages().text("error")).queue();
                return;
            }
            MessageEditBuilder message = new MessageEditBuilder().setContent(step.text());
            if (step.confirmId() != null) {
                message.setComponents(ActionRow.of(
                        Button.success(step.confirmId(), plugin.messages().text("action.confirm-button"))));
            }
            hook.editOriginal(message.build()).queue();
        }));
    }

    private String remember(Pending action) {
        long now = System.currentTimeMillis();
        pending.values().removeIf(old -> old.expiresAt() <= now || old.discordId().equals(action.discordId()));
        String id = CONFIRM + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        pending.put(id, action);
        return id;
    }

    private long deadline() {
        return System.currentTimeMillis() + plugin.settings().confirmSeconds() * 1000L;
    }

    private void depositar(SlashCommandInteractionEvent event) {
        Optional<Link> found = InfoCommands.linked(plugin, event);
        if (found.isEmpty()) {
            return;
        }
        Link link = found.get();
        String typed = event.getOption("valor", "", OptionMapping::getAsString);
        ask(event, () -> {
            Messages texts = plugin.messages();
            Economy economy = plugin.economy();
            String blocked = blocked(link);
            if (blocked != null) {
                return Step.say(blocked);
            }
            OptionalLong amount = economy.parse(typed);
            if (amount.isEmpty()) {
                return Step.say(texts.text("action.bad-amount", "typed", typed));
            }
            Country country = null;
            for (Country candidate : plugin.countries().of(link.minecraft())) {
                if (plugin.countries().canDeposit(candidate.id(), link.minecraft())) {
                    country = candidate;
                    break;
                }
            }
            if (country == null) {
                return Step.say(texts.text(plugin.countries().available() ? "deposit.no-country" : "countries-off"));
            }
            long balance = economy.geoBalance(link.minecraft());
            if (balance < amount.getAsLong()) {
                return Step.say(texts.text("action.not-enough", "balance", economy.geos(balance)));
            }
            String id = remember(new Pending(link.discord(), link.minecraft(), link.name(), Kind.DEPOSIT,
                    amount.getAsLong(), country.id(), country.name(), deadline()));
            return new Step(texts.text("deposit.confirm", "amount", economy.geos(amount.getAsLong()),
                    "country", country.name(), "balance", economy.geos(balance)), id);
        });
    }

    private void pagar(SlashCommandInteractionEvent event) {
        Optional<Link> found = InfoCommands.linked(plugin, event);
        if (found.isEmpty()) {
            return;
        }
        Link link = found.get();
        String typedName = event.getOption("jogador", "", OptionMapping::getAsString).trim();
        String typed = event.getOption("valor", "", OptionMapping::getAsString);
        ask(event, () -> {
            Messages texts = plugin.messages();
            Economy economy = plugin.economy();
            String blocked = blocked(link);
            if (blocked != null) {
                return Step.say(blocked);
            }
            OptionalLong amount = economy.parse(typed);
            if (amount.isEmpty()) {
                return Step.say(texts.text("action.bad-amount", "typed", typed));
            }
            Optional<String> receiver = economy.accountName(typedName);
            if (receiver.isEmpty()) {
                return Step.say(texts.text("pay.no-player", "player", typedName));
            }
            if (receiver.get().equalsIgnoreCase(link.name())) {
                return Step.say(texts.text("pay.self"));
            }
            long left = leftToday(link.minecraft());
            if (amount.getAsLong() > left) {
                return Step.say(texts.text("pay.limit", "limit", economy.geos(dailyLimit()), "left", economy.geos(Math.max(0, left))));
            }
            long balance = economy.geoBalance(link.minecraft());
            if (balance < amount.getAsLong()) {
                return Step.say(texts.text("action.not-enough", "balance", economy.geos(balance)));
            }
            String id = remember(new Pending(link.discord(), link.minecraft(), link.name(), Kind.PAY,
                    amount.getAsLong(), receiver.get(), receiver.get(), deadline()));
            return new Step(texts.text("pay.confirm", "amount", economy.geos(amount.getAsLong()),
                    "player", receiver.get(), "balance", economy.geos(balance)), id);
        });
    }

    private long dailyLimit() {
        return Math.round(plugin.settings().payDailyLimit() * 100);
    }

    private static String today() {
        return LocalDate.now(BRAZIL).toString();
    }

    private long leftToday(UUID player) {
        return dailyLimit() - plugin.database().spent(player, today());
    }

    private void confirmed(ButtonInteractionEvent event) {
        Pending action = pending.get(event.getComponentId());
        Messages texts = plugin.messages();
        if (action == null || action.expiresAt() <= System.currentTimeMillis()) {
            pending.remove(event.getComponentId());
            event.editMessage(new MessageEditBuilder().setContent(texts.text("action.expired")).setComponents().build()).queue();
            return;
        }
        if (!action.discordId().equals(event.getUser().getId())) {
            event.reply(texts.text("action.not-yours")).setEphemeral(true).queue();
            return;
        }
        pending.remove(event.getComponentId());
        event.deferEdit().queue(hook -> plugin.onMain(() -> perform(action)).whenComplete((text, error) -> {
            if (error != null) {
                bot.failed(error);
            }
            hook.editOriginal(new MessageEditBuilder()
                    .setContent(error != null || text == null ? plugin.messages().text("error") : text)
                    .setComponents().build()).queue();
        }));
    }

    /** Does the confirmed action. Runs on the server thread. @return the text to show in Discord */
    private String perform(Pending action) {
        Messages texts = plugin.messages();
        Economy economy = plugin.economy();
        Optional<Link> link = plugin.links().byDiscord(action.discordId());
        if (link.isEmpty() || !link.get().minecraft().equals(action.player())) {
            return texts.text("link.needed");
        }
        String blocked = blocked(link.get());
        if (blocked != null) {
            return blocked;
        }
        String amount = economy.geos(action.cents());
        if (action.kind() == Kind.DEPOSIT) {
            if (!plugin.countries().canDeposit(action.target(), action.player())) {
                return texts.text("deposit.no-country");
            }
            if (!economy.take(action.player(), action.cents(), "discord:/depositar " + action.targetName())) {
                return texts.text("action.not-enough", "balance", economy.geos(economy.geoBalance(action.player())));
            }
            if (!plugin.countries().addToBank(action.target(), action.cents() / 100.0)) {
                economy.give(action.player(), action.cents(), "discord:/depositar devolvido");
                return texts.text("deposit.failed");
            }
            plugin.rememberAction(action.player(), texts.text("log.deposit", "amount", amount, "country", action.targetName()));
            return texts.text("deposit.done", "amount", amount, "country", action.targetName(),
                    "balance", economy.geos(economy.geoBalance(action.player())));
        }

        if (action.cents() > leftToday(action.player())) {
            return texts.text("pay.limit", "limit", economy.geos(dailyLimit()),
                    "left", economy.geos(Math.max(0, leftToday(action.player()))));
        }
        Economy.Pay pay = economy.pay(action.player(), action.target(), action.cents());
        switch (pay.status()) {
            case PAID -> {
                plugin.database().addSpent(action.player(), today(), action.cents());
                plugin.rememberAction(action.player(), texts.text("log.pay", "amount", amount, "player", pay.receiver()));
                Player receiver = pay.receiverId() == null ? null : Bukkit.getPlayer(pay.receiverId());
                if (receiver != null) {
                    plugin.messages().send(receiver, "pay-received", "amount", amount, "player", action.playerName());
                }
                return texts.text("pay.done", "amount", amount, "player", pay.receiver(),
                        "balance", economy.geos(economy.geoBalance(action.player())));
            }
            case NO_SUCH_PLAYER -> {
                return texts.text("pay.no-player", "player", action.target());
            }
            case SAME_PLAYER -> {
                return texts.text("pay.self");
            }
            case NOT_ENOUGH -> {
                return texts.text("action.not-enough", "balance", economy.geos(economy.geoBalance(action.player())));
            }
            case RECEIVER_FULL -> {
                return texts.text("pay.receiver-full", "player", pay.receiver());
            }
            default -> {
                return texts.text("error");
            }
        }
    }
}
