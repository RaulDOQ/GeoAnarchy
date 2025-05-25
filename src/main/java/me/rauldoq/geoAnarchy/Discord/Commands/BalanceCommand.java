package me.rauldoq.geoAnarchy.Discord.Commands;

import me.rauldoq.geoAnarchy.Classes.ConnectedAccounts;
import me.rauldoq.geoAnarchy.Discord.ICommand;
import me.rauldoq.geoAnarchy.GeoAnarchy;
import me.rauldoq.geoAnarchy.Global;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

public class BalanceCommand implements ICommand {
    @Override
    public String getName() {
        return "balance";
    }

    @Override
    public String getDescription() {
        return "mostra quanto dinheiro voce tem no servidor";
    }

    @Override
    public List<OptionData> getOptions() {
        return null;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String discordId = event.getUser().getId();
        boolean found = false;
        for (ConnectedAccounts ac : Global.connectedAccounts){
            try{
                if (ac.getDiscordID().equals(discordId)){
                    OfflinePlayer player = Bukkit.getOfflinePlayer(ac.getMinecraftID());
                    found = true;
                    if (player.isOnline()){
                        Player onlinePlayer = Bukkit.getPlayer(ac.getMinecraftID());
                        event.reply("Seu dinheiro: R$" + GeoAnarchy.econ.getBalance(onlinePlayer)).queue();
                        break;
                    }
                    event.reply("Seu dinheiro: R$" + GeoAnarchy.econ.getBalance(player)).queue();
                }
            } catch (Exception e){
            }

        }
        if (!found){
            event.reply("Sua conta não esta vinculada.").queue();
        }
    }
}
