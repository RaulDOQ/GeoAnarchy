package me.rauldoq.geoAnarchy;

import com.earth2me.essentials.Essentials;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.leangen.geantyref.TypeToken;
import me.rauldoq.geoAnarchy.Classes.VIP;
import me.rauldoq.geoAnarchy.Discord.CommandManager;
import me.rauldoq.geoAnarchy.Discord.Commands.BalanceCommand;
import me.rauldoq.geoAnarchy.Discord.Commands.VincularCommand;
import me.rauldoq.geoAnarchy.Discord.Listeners;
import me.rauldoq.geoAnarchy.Minecraft.Commands.*;
import me.rauldoq.geoAnarchy.Minecraft.Listeners.MCListeners;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;

public final class GeoAnarchy extends JavaPlugin implements Listener {
    public static Economy econ = null;
    public static JDA jda;
    private static Essentials essentials;
    @Override
    public void onEnable() {

        new BukkitRunnable(){
            @Override
            public void run(){

                Global.timeLB = new ArrayList<>();
                Global.killsLB = new ArrayList<>();

                TreeMap<Integer, List<OfflinePlayer>> unorderedTimeLB = new TreeMap<>();
                TreeMap<Integer, List<OfflinePlayer>> unorderedKillsLB = new TreeMap<>();


                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()){
                    int playtimeSeconds = offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
                    int totalKills = offlinePlayer.getStatistic(Statistic.PLAYER_KILLS);

                    unorderedTimeLB.computeIfAbsent(playtimeSeconds, k -> new ArrayList<>()).add(offlinePlayer);
                    unorderedKillsLB.computeIfAbsent(totalKills, k -> new ArrayList<>()).add(offlinePlayer);
                }

                for (Integer key : unorderedTimeLB.descendingKeySet()){
                    for (OfflinePlayer player : unorderedTimeLB.get(key)){
                        Global.timeLB.add(player.getUniqueId());
                    }

                }
                for (Integer key : unorderedKillsLB.descendingKeySet()){
                    for (OfflinePlayer player : unorderedKillsLB.get(key)){
                        Global.killsLB.add(player.getUniqueId());
                    }
                }

            }
        }.runTaskTimer(this, 0L, 800);


        new BukkitRunnable(){
            @Override
            public void run() {
                Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).setPrettyPrinting().create();
                System.out.println(" -------------- Running vip checks ------------");
                try(FileReader reader = new FileReader(Global.vipsfile)){
                    Type listType = new TypeToken<List<VIP>>() {}.getType();

                    List<VIP> vips = gson.fromJson(reader, listType);
                    Iterator<VIP> iterator = vips.iterator();

                    while (iterator.hasNext()){
                        VIP vip = iterator.next();

                        if (!vip.isValid()){

                            vip.decreaseDuration();
                            vip.setDateOfOrder();
                            if (vip.getDuration() == 0){

                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + vip.getUsername() + " parent remove vip");
                                System.out.println("Vip removido de " + vip.getUsername());
                                iterator.remove();
                                System.out.println("Vip removed");

                            }
                        }

                    }


                    FileWriter writer = new FileWriter(Global.vipsfile);
                    gson.toJson(vips, writer);
                    writer.close();
                } catch (Exception sigma){
                    System.out.println(sigma.getMessage());
                }
            }
        }.runTaskTimer(this, 0l, 12000);
        //  ------- VARIABLE SECTION -------- //

        if (!getDataFolder().exists()){
            boolean folderCreated = getDataFolder().mkdirs();
            if (folderCreated){
                getLogger().info("Plugin folder created successfully");
            } else {
                getLogger().warning("Failed to create plugin folder.");
            }
        }

        Global.StartVariables(this);

        // -------- DISCORD SECTION ------- //

        jda = JDABuilder.createDefault("nothing")
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .enableIntents(GatewayIntent.DIRECT_MESSAGES)
                .enableIntents(GatewayIntent.DIRECT_MESSAGE_TYPING)
                .setActivity(Activity.playing(".Jogando GeoAnarchy.net"))
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .build();

        jda.addEventListener(new Listeners());
        CommandManager manager = new CommandManager();

        manager.add(new VincularCommand());


        System.out.println(jda.getToken());


        jda.addEventListener(manager);


        // ------- MINECRAFT SERVER SECTION -------- //

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new MCListeners(), this);
        Objects.requireNonNull(getCommand("mapa")).setExecutor(new MapCommand());
        Objects.requireNonNull(getCommand("discord")).setExecutor(new DiscordCommandsManager());
        Objects.requireNonNull(getCommand("vanishcomaura")).setExecutor(new VanishComAura());
        Objects.requireNonNull(getCommand("vaura")).setExecutor(new VanishComAura());
        Objects.requireNonNull(getCommand("ranks")).setExecutor(new RanksCommand());
        Objects.requireNonNull(getCommand("darvip")).setExecutor(new DarVIPCommand());

        if (!setupEconomy()) {
            getLogger().severe("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Vault hooked successfully!");

    }

    @Override
    public void onDisable() {

    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }
    public static Essentials getEssentials(){
        if (essentials ==null){
            Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("Essentials");
            if (plugin instanceof Essentials){
                essentials = (Essentials) plugin;
            }
        }
        return essentials;
    }
}

