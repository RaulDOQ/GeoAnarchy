package me.rauldoq.geoAnarchy.Discord;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;

public class CommandManager extends ListenerAdapter {
    private final List<ICommand> commands = new ArrayList<>();

    @Override
    public void onReady(ReadyEvent event) { // runs when the app is ready.

        for (Guild guild : event.getJDA().getGuilds()) { // goes through every guild the bot is part of

            for (ICommand command : commands) { // goes through every command in "commands" list

                if (command.getOptions() == null) { //doesn't add options if its null
                    guild.upsertCommand(command.getName(), command.getDescription()).queue();

                } else { // adds all options.
                    guild.upsertCommand(command.getName(), command.getDescription()).addOptions(command.getOptions()).queue();}

            }
        }
    }
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        for(ICommand command : commands) { // goes through every command

            if (command.getName().equals(event.getName())){ // finds which command was run, command.getName() gets the name of a command in the list, and compares to event.getName()

                command.execute(event); // executes command giving the name of the command.

                return;
            }
        }
    }

    public void add(ICommand command) {commands.add(command);}
}
