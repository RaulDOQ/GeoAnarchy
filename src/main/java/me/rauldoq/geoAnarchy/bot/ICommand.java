package me.rauldoq.geoAnarchy.bot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public interface ICommand {
    String getName();

    String getDescription();

    List<OptionData> getOptions();

    default DefaultMemberPermissions getDefaultPermissions() {
        return DefaultMemberPermissions.ENABLED;
    }

    void execute(SlashCommandInteractionEvent event);
}
