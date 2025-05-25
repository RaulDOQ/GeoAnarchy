package me.rauldoq.geoAnarchy.Minecraft.Commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.rauldoq.geoAnarchy.Classes.ConnectedAccounts;
import me.rauldoq.geoAnarchy.Global;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class DiscordRegister {
    public boolean run(CommandSender sender) throws IOException {

        if (sender instanceof Player player){

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileReader reader = new FileReader(Global.discordConnectionsFile.getPath());

            Boolean userExists = false;

            for (ConnectedAccounts connectedAccounts : Global.connectedAccounts){
                if (connectedAccounts.getMinecraftID().equals(player.getUniqueId().toString())){
                    if (connectedAccounts.getCode() == -1){
                        player.sendMessage("Você já vinculou sua conta");
                        userExists = true;
                        break;
                    }
                }
            }
            if (!userExists){

                ConnectedAccounts connectedAccounts = new ConnectedAccounts();
                connectedAccounts.setMinecraftID(player.getUniqueId().toString());
                Random random = new Random();
                connectedAccounts.setCode(random.nextInt(999999));

                Global.connectedAccounts.add(connectedAccounts);

                player.sendMessage("Use esse comando no discord: /vincular " + String.valueOf(connectedAccounts.getCode()));}

        }

        return false;
    }
}
