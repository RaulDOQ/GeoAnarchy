package me.rauldoq.geoAnarchy.Minecraft.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class GeoAnarchyCommandsManager implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        try {
            switch (args[0].toLowerCase()){
                case "claim":

                case "vincular":
                    break;
                default:
                    break;
            }
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
