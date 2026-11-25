package me.rauldoq.geoAnarchy.bot;

import me.rauldoq.geoAnarchy.GeoAnarchy;
import me.rauldoq.geoAnarchy.link.LinkService;
import me.rauldoq.geoAnarchy.storage.Database.Link;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.modals.Modal;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * The Discord half of linking: /vincular, the "Vincular conta" button with its small window, and /desvincular.
 * The player gets the code in game first (see GameCommands).
 */
public final class LinkCommands {

    /** The id of the "Vincular conta" button on the info panel. */
    public static final String OPEN_BUTTON = "geo:link:open";
    private static final String WINDOW = "geo:link:code";
    private static final String FIELD = "codigo";

    private final GeoAnarchy plugin;
    private final Bot bot;

    public LinkCommands(GeoAnarchy plugin, Bot bot) {
        this.plugin = plugin;
        this.bot = bot;
    }

    public void register() {
        bot.add(new Slash("vincular", "Vincula a sua conta do Minecraft. Pegue o código no jogo com /vincular.",
                List.of(new OptionData(OptionType.STRING, FIELD, "O código de 6 números que o jogo mostrou", false)
                        .setMaxLength(12)),
                this::vincular));
        bot.add(new Slash("desvincular", "Desfaz o vínculo entre o seu Discord e a sua conta do Minecraft.", null, this::desvincular));
        bot.onButton(OPEN_BUTTON, this::openWindow);
        bot.onModal(WINDOW, this::windowSent);
    }

    private void vincular(SlashCommandInteractionEvent event) {
        String code = event.getOption(FIELD, OptionMapping::getAsString);
        if (code == null) {
            Optional<Link> existing = plugin.links().byDiscord(event.getUser().getId());
            if (existing.isPresent()) {
                event.reply(plugin.messages().text("link.already", "player", existing.get().name())).setEphemeral(true).queue();
            } else {
                event.replyModal(window()).queue();
            }
            return;
        }
        redeem(event, code);
    }

    private void openWindow(ButtonInteractionEvent event) {
        Optional<Link> existing = plugin.links().byDiscord(event.getUser().getId());
        if (existing.isPresent()) {
            event.reply(plugin.messages().text("link.already", "player", existing.get().name())).setEphemeral(true).queue();
            return;
        }
        event.replyModal(window()).queue();
    }

    private Modal window() {
        TextInput code = TextInput.create(FIELD, TextInputStyle.SHORT)
                .setPlaceholder("482 913")
                .setRequiredRange(6, 12)
                .build();
        return Modal.create(WINDOW, plugin.messages().text("link.window-title"))
                .addComponents(Label.of(plugin.messages().text("link.window-label"), code))
                .build();
    }

    private void windowSent(ModalInteractionEvent event) {
        var value = event.getValue(FIELD);
        redeem(event, value == null ? "" : value.getAsString());
    }

    private void redeem(IReplyCallback event, String code) {
        LinkService.Redeem result = plugin.links().redeem(event.getUser().getId(), code);
        var texts = plugin.messages();
        switch (result.status()) {
            case LINKED -> {
                Link link = result.link();
                event.reply(texts.text("link.done", "player", link.name())).setEphemeral(true).queue();
                bot.markLinked(link);
                String discordName = event.getUser().getName();
                plugin.onMain(() -> {
                    Player player = Bukkit.getPlayer(link.minecraft());
                    if (player != null) {
                        plugin.messages().send(player, "link-done", "discord", discordName);
                    }
                    return null;
                });
                plugin.getLogger().info(link.name() + " linked their Discord account (" + discordName + ").");
            }
            case DISCORD_ALREADY_LINKED -> event.reply(texts.text("link.already", "player", result.link().name()))
                    .setEphemeral(true).queue();
            case LOCKED -> event.reply(texts.text("link.locked", "minutes", String.valueOf(result.minutes())))
                    .setEphemeral(true).queue();
            case WRONG_CODE -> event.reply(texts.text("link.wrong")).setEphemeral(true).queue();
        }
    }

    private void desvincular(SlashCommandInteractionEvent event) {
        Optional<Link> removed = plugin.links().unlinkDiscord(event.getUser().getId());
        if (removed.isEmpty()) {
            event.reply(plugin.messages().text("link.none")).setEphemeral(true).queue();
            return;
        }
        Link link = removed.get();
        event.reply(plugin.messages().text("link.removed", "player", link.name())).setEphemeral(true).queue();
        bot.markUnlinked(link.discord());
        plugin.onMain(() -> {
            Player player = Bukkit.getPlayer(link.minecraft());
            if (player != null) {
                plugin.messages().send(player, "unlink-done");
            }
            return null;
        });
    }
}
