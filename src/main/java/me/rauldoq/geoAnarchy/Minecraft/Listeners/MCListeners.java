package me.rauldoq.geoAnarchy.Minecraft.Listeners;

import fr.xephi.authme.events.LoginEvent;
import me.angeschossen.lands.api.events.war.WarDeclareEvent;
import me.angeschossen.lands.api.war.War;
import me.rauldoq.geoAnarchy.Classes.ConnectedAccounts;
import me.rauldoq.geoAnarchy.GeoAnarchy;
import me.rauldoq.geoAnarchy.Global;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import me.angeschossen.lands.api.LandsIntegration;

public class MCListeners implements Listener {
    @EventHandler
    public void onWarDeclareEvent(WarDeclareEvent event){
        MessageChannel channel = GeoAnarchy.jda.getTextChannelById(1333077790072901653L);

        Player attackerPlayer = Bukkit.getPlayer(event.getAttacker().getOwnerUID());
        Player defenderPlayer = Bukkit.getPlayer(event.getDefender().getOwnerUID());

        String attackerMention = "Conta não conectada ao discord";
        String defenderMention = "Conta não conectada ao discord";

        for (ConnectedAccounts ac : Global.connectedAccounts){
            if (ac.getMinecraftID().equals(attackerPlayer.getUniqueId().toString())){
                attackerMention = GeoAnarchy.jda.getUserById(ac.getDiscordID()).getAsMention();
            }
            if (ac.getMinecraftID().equals(defenderPlayer.getUniqueId().toString())){
                defenderMention = GeoAnarchy.jda.getUserById(ac.getDiscordID()).getAsMention();
            }
        }

        channel.sendMessage("O pais " +
                event.getAttacker().getName() + " (" + attackerMention + ")" + " Declarou guerra no pais "
                + event.getDefender().getName() + " (" + defenderMention + ")!").queue();

    }
}
