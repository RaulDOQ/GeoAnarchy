package me.rauldoq.geoAnarchy.Minecraft.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DiscordCommandsManager implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        DiscordInviteCommand discordInviteCommand = new DiscordInviteCommand();
        if (args.length == 0){
            discordInviteCommand.run(sender);
            return true;
        }
        try {
            switch (args[0].toLowerCase()){
                case "servidor":
                    discordInviteCommand.run(sender);
                    break;
                case "vincular":
                    DiscordRegister register = new DiscordRegister();
                    register.run(sender);
                    break;
                default:
                    break;
            }
        } catch (Exception e){
            discordInviteCommand.run(sender);
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
