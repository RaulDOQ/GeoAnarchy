package me.rauldoq.geoAnarchy;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.rauldoq.geoAnarchy.Classes.ConnectedAccounts;
import me.rauldoq.geoAnarchy.Classes.Order;
import me.rauldoq.geoAnarchy.Classes.VIP;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Global {

    public static File discordConnectionsFile;
    public static File vipsfile;
    public static File ordersFile;
    public static List<ConnectedAccounts> connectedAccounts = new ArrayList<>();
    public static List<VIP> vips = new ArrayList<>();
    public static List<Order> orders = new ArrayList<>();
    public static List<UUID> timeLB = new ArrayList<>();
    public static List<UUID> killsLB = new ArrayList<>();

    public static void StartVariables(GeoAnarchy geoAnarchy){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        discordConnectionsFile = createFile(geoAnarchy,"discordConnections.json", true);
        vipsfile = createFile(geoAnarchy, "vips.json", true);
        ordersFile = createFile(geoAnarchy, "orders.json", true);
        connectedAccounts = loadUsers(gson);
    }

    private static File createFile(GeoAnarchy geoAnarchy, String fileName, Boolean isJson) {
        File file = new File(geoAnarchy.getDataFolder(), fileName);

        if (!file.exists()){

            try {

                boolean fileCreated = file.createNewFile();

                if (fileCreated) {

                    geoAnarchy.getLogger().info(fileName + " created successfully!");
                    FileWriter writer = new FileWriter(file.getPath());
                    writer.write("[]");
                    return file;

                } else {

                    geoAnarchy.getLogger().warning("Failed to create " + fileName);

                }

            } catch (IOException e) {

                geoAnarchy.getLogger().severe("An error occurred while creating " + fileName + ": " + e.getMessage());
                e.printStackTrace();

            }
        } else {

            geoAnarchy.getLogger().info(fileName + " already exists.");
            return file;

        }
        return null;
    }

    public static List<ConnectedAccounts> loadUsers(Gson gson) {
        try (FileReader reader = new FileReader(discordConnectionsFile)) {

            Type userListType = new TypeToken<List<ConnectedAccounts>>() {}.getType();
            List<ConnectedAccounts> connectedAccounts = gson.fromJson(reader, userListType);
            return (connectedAccounts != null) ? connectedAccounts : new ArrayList<>();

        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


}
