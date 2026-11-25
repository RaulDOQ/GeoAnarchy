package me.rauldoq.geoAnarchy.bot;

import me.rauldoq.geoAnarchy.GeoAnarchy;
import me.rauldoq.geoAnarchy.alerts.AlertKind;
import me.rauldoq.geoAnarchy.alerts.Prefs;
import me.rauldoq.geoAnarchy.config.Messages;
import me.rauldoq.geoAnarchy.storage.Database.Link;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * /notificacoes: a private card with one button per kind of alert. A green button is on, a grey one is off,
 * and pressing it flips it.
 */
public final class NotifyCommand {

    private static final String BUTTON = "geo:notify:";
    private static final String CITIZEN = "cidadao";

    private final GeoAnarchy plugin;
    private final Bot bot;

    public NotifyCommand(GeoAnarchy plugin, Bot bot) {
        this.plugin = plugin;
        this.bot = bot;
    }

    public void register() {
        bot.add(new Slash("notificacoes", "Escolha quais avisos do servidor você recebe por mensagem privada.", null, this::show));
        bot.onButton(BUTTON, this::pressed);
    }

    private void show(SlashCommandInteractionEvent event) {
        Optional<Link> link = InfoCommands.linked(plugin, event);
        if (link.isPresent()) {
            event.reply(card(link.get().minecraft())).setEphemeral(true).queue();
        }
    }

    private void pressed(ButtonInteractionEvent event) {
        Optional<Link> link = InfoCommands.linked(plugin, event);
        if (link.isEmpty()) {
            return;
        }
        UUID player = link.get().minecraft();
        Prefs prefs = plugin.prefs();
        String what = event.getComponentId().substring(BUTTON.length());
        if (what.equals(CITIZEN)) {
            prefs.setCitizenAlerts(player, !prefs.citizenAlerts(player));
        } else {
            AlertKind kind = AlertKind.byId(what);
            if (kind != null) {
                prefs.setWants(player, kind, !prefs.wants(player, kind));
            }
        }
        event.editMessage(MessageEditData.fromCreateData(card(player))).queue();
    }

    private MessageCreateData card(UUID player) {
        Messages texts = plugin.messages();
        Prefs prefs = plugin.prefs();
        StringBuilder lines = new StringBuilder(texts.text("notify.intro")).append("\n\n");
        List<Button> buttons = new ArrayList<>();
        for (AlertKind kind : AlertKind.values()) {
            boolean on = prefs.wants(player, kind);
            lines.append(on ? "🔔 " : "🔕 ").append("**").append(texts.text("notify." + kind.id() + ".name")).append("**: ")
                    .append(texts.text("notify." + kind.id() + ".about")).append('\n');
            buttons.add(button(BUTTON + kind.id(), texts.text("notify." + kind.id() + ".name"), on));
        }
        boolean citizen = prefs.citizenAlerts(player);
        lines.append('\n').append(citizen ? "🔔 " : "🔕 ").append("**").append(texts.text("notify.cidadao.name")).append("**: ")
                .append(texts.text("notify.cidadao.about"));
        if (prefs.dmClosed(player)) {
            lines.append("\n\n⚠️ ").append(texts.text("notify.dm-closed"));
        }
        var embed = new EmbedBuilder().setColor(Bot.COLOR).setTitle(texts.text("notify.title"))
                .setDescription(lines.toString()).setFooter(texts.text("notify.footer")).build();
        return new MessageCreateBuilder().setEmbeds(embed).setComponents(
                ActionRow.of(buttons),
                ActionRow.of(button(BUTTON + CITIZEN, texts.text("notify.cidadao.name"), citizen))).build();
    }

    private static Button button(String id, String label, boolean on) {
        return on ? Button.success(id, label) : Button.secondary(id, label);
    }
}
