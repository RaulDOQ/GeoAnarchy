package me.rauldoq.geoAnarchy.bot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.function.Consumer;

/** A slash command in one line: its name, what Discord shows under it, its options, and what it runs. */
record Slash(String name, String description, List<OptionData> options,
             Consumer<SlashCommandInteractionEvent> run) implements ICommand {

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<OptionData> getOptions() {
        return options;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        run.accept(event);
    }
}
