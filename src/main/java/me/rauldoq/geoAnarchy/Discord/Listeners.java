package me.rauldoq.geoAnarchy.Discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.rauldoq.geoAnarchy.Classes.ConnectedAccounts;
import me.rauldoq.geoAnarchy.Global;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

import java.awt.*;
import java.io.FileWriter;
import java.util.UUID;

public class Listeners extends ListenerAdapter {


    public void onReady(ReadyEvent event){
        MessageChannel channel= event.getJDA().getTextChannelById("1254199563888099448");
        assert channel != null;
        channel.sendMessage("Server Info\n"
                + "Server Name: " + Bukkit.getServer().getName() + "\n"
                + "Server Version: " + Bukkit.getServer().getVersion()+ "\n"
                + "Online Players: " + Bukkit.getOnlinePlayers().size() ).queue();

    }

    public void onMessageReceived(MessageReceivedEvent event){


    }
}
