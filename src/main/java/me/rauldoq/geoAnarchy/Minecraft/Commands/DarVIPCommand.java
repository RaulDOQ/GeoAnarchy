package me.rauldoq.geoAnarchy.Minecraft.Commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.leangen.geantyref.TypeToken;
import me.rauldoq.geoAnarchy.Classes.VIP;
import me.rauldoq.geoAnarchy.Global;
import me.rauldoq.geoAnarchy.LocalDateAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;

public class DarVIPCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender.hasPermission("geoanarchy.darvip")){
            OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
            Integer duration = Integer.valueOf(args[1]);

            Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).setPrettyPrinting().create();

            try(FileReader reader = new FileReader(Global.vipsfile)){
                Type listType = new TypeToken<List<VIP>>() {}.getType();

                List<VIP> vips = gson.fromJson(reader, listType);

                boolean exists = false;
                for (VIP vip : vips){
                    if (vip.getUsername().equals(args[0])){
                        vip.increaseDuration();
                        exists = true;
                    }
                }

                if (!exists){
                    VIP vip = new VIP();
                    vip.setDuration(1);
                    vip.setUsername(args[0]);
                    vip.setDateOfOrder();
                    vip.setMinecraftUUID(String.valueOf(Bukkit.getOfflinePlayer(args[0]).getUniqueId()));
                    Bukkit.dispatchCommand(sender, "lp user " + args[0] + " parent add vip");
                    vips.add(vip);
                }

                FileWriter writer = new FileWriter(Global.vipsfile);
                gson.toJson(vips, writer);
                writer.close();

            } catch (Exception e){
                System.out.println(e.getMessage());
            }
        }
        return true;
    }
}
