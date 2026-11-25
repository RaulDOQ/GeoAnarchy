package me.rauldoq.geoAnarchy.bot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public final class PublishUpdateCommand implements ICommand {
    private final UpdateNotifications notifications;

    public PublishUpdateCommand(UpdateNotifications notifications) {
        this.notifications = notifications;
    }

    @Override public String getName() { return "atualizacao"; }
    @Override public String getDescription() { return "Publica uma novidade e menciona quem escolheu receber atualizações."; }
    @Override public DefaultMemberPermissions getDefaultPermissions() {
        return DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER);
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(new OptionData(OptionType.STRING, "texto", "Novidade para os jogadores", true)
                .setRequiredLength(1, 4000));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!UpdateNotifications.staff(event)) {
            event.reply("Somente quem tem Gerenciar Servidor pode publicar por este comando.").setEphemeral(true).queue();
            return;
        }
        String text = event.getOption("texto").getAsString();
        event.deferReply(true).queue(hook ->
                notifications.respond(hook, () -> notifications.publish(event.getGuild(), text)));
    }
}
