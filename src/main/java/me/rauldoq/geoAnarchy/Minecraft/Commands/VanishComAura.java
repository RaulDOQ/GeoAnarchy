package me.rauldoq.geoAnarchy.Minecraft.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VanishComAura implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player){
            if (player.hasPermission("geoanarchy.vanishcomaura")){
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "smite " + player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "v " + player.getName());
            } else {
                player.sendMessage("Você não tem permissão para usar esse comando.");
            }

        }
        return true;
    }
}
