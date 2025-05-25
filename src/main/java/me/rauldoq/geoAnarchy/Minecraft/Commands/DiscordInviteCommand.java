package me.rauldoq.geoAnarchy.Minecraft.Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscordInviteCommand {
    public boolean run(CommandSender sender) {
        if (sender instanceof Player player){
            String message = "Entre no nosso discord!";
            String url = "https://discord.gg/XBwg3WWyqA";
            player.sendMessage(message);
            player.sendMessage(url);
        } else{
            System.out.println("Command not meant to be run by the console");
        }

        return true;
    }
}
