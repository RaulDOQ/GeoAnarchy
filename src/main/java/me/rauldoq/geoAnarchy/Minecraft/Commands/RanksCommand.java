package me.rauldoq.geoAnarchy.Minecraft.Commands;

import me.rauldoq.geoAnarchy.Global;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class RanksCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        try{
            sender.sendMessage("------ RANKS -------" + "\n TEMPO JOGADO" + "\n" +
                    "1 - " + Bukkit.getOfflinePlayer(Global.timeLB.get(0)).getName() + " - " + ((Bukkit.getOfflinePlayer(Global.timeLB.get(0)).getStatistic(Statistic.PLAY_ONE_MINUTE) / 20) / 60 ) / 60 + "hrs\n" +
                    "2 - " + Bukkit.getOfflinePlayer(Global.timeLB.get(1)).getName() + " - " + ((Bukkit.getOfflinePlayer(Global.timeLB.get(1)).getStatistic(Statistic.PLAY_ONE_MINUTE) / 20) / 60 ) / 60 + "hrs\n" +
                    "3 - " + Bukkit.getOfflinePlayer(Global.timeLB.get(2)).getName() + " - " + ((Bukkit.getOfflinePlayer(Global.timeLB.get(2)).getStatistic(Statistic.PLAY_ONE_MINUTE) / 20) / 60 ) / 60 + "hrs\n" +
                    "4 - " + Bukkit.getOfflinePlayer(Global.timeLB.get(3)).getName() + " - " + ((Bukkit.getOfflinePlayer(Global.timeLB.get(3)).getStatistic(Statistic.PLAY_ONE_MINUTE) / 20) / 60 ) / 60 + "hrs\n" +
                    "5 - " + Bukkit.getOfflinePlayer(Global.timeLB.get(4)).getName() + " - " + ((Bukkit.getOfflinePlayer(Global.timeLB.get(4)).getStatistic(Statistic.PLAY_ONE_MINUTE) / 20) / 60 ) / 60 + "hrs\n" +
                    "KILLS\n" +
                    "1 - " + Bukkit.getOfflinePlayer(Global.killsLB.get(0)).getName() + " - " + Bukkit.getOfflinePlayer(Global.killsLB.get(0)).getStatistic(Statistic.PLAYER_KILLS) + " kills\n" +
                    "2 - " + Bukkit.getOfflinePlayer(Global.killsLB.get(1)).getName() + " - " + Bukkit.getOfflinePlayer(Global.killsLB.get(1)).getStatistic(Statistic.PLAYER_KILLS) + " kills\n" +
                    "3 - " + Bukkit.getOfflinePlayer(Global.killsLB.get(2)).getName() + " - " + Bukkit.getOfflinePlayer(Global.killsLB.get(2)).getStatistic(Statistic.PLAYER_KILLS) + " kills\n" +
                    "4 - " + Bukkit.getOfflinePlayer(Global.killsLB.get(3)).getName() + " - " + Bukkit.getOfflinePlayer(Global.killsLB.get(3)).getStatistic(Statistic.PLAYER_KILLS) + " kills\n" +
                    "5 - " + Bukkit.getOfflinePlayer(Global.killsLB.get(4)).getName() + " - " + Bukkit.getOfflinePlayer(Global.killsLB.get(4)).getStatistic(Statistic.PLAYER_KILLS) + " kills\n"
            );
        } catch (Exception e){
            sender.sendMessage("Houve um erro executando esse comando.");
            System.out.println(e.getMessage());
        }

        return true;
    }
}
