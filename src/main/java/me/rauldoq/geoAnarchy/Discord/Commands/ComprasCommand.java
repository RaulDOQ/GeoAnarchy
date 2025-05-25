package me.rauldoq.geoAnarchy.Discord.Commands;

import com.earth2me.essentials.Kit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.rauldoq.geoAnarchy.Classes.ConnectedAccounts;
import me.rauldoq.geoAnarchy.Classes.Order;
import me.rauldoq.geoAnarchy.Classes.VIP;
import me.rauldoq.geoAnarchy.Discord.ICommand;
import me.rauldoq.geoAnarchy.GeoAnarchy;
import me.rauldoq.geoAnarchy.Global;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Debug;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ComprasCommand implements ICommand {
    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public List<OptionData> getOptions() {
        List<OptionData> data = new ArrayList<>();
        data.add(new OptionData(OptionType.INTEGER, "id-compra", "id-compra", true));
        data.add(new OptionData(OptionType.STRING, "nickname", "nome do mine", true));
        data.add(new OptionData(OptionType.INTEGER, "duracao", "tempo de duracao da compra em MESES (se tiver)", false));
        return data;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)){

            int OrderId = Objects.requireNonNull(event.getOption("id-compra")).getAsInt();
            int Duration = Objects.requireNonNull(event.getOption("duracao")).getAsInt();
            String name = Objects.requireNonNull(event.getOption("nickname")).toString();

            Boolean found = false;
            String mcUUID = null;
            ConnectedAccounts account = null;
            for (ConnectedAccounts ac : Global.connectedAccounts){
                if (Bukkit.getPlayer(ac.getMinecraftID()).getName().equals(name)){
                    found = true;
                    mcUUID = ac.getMinecraftID();
                    account = ac;
                    break;
                }
            }

            Order order;
            LuckPerms lp = LuckPermsProvider.get();
            switch (OrderId){
                case 1:
                    try{
                        User user = lp.getPlayerAdapter(Player.class).getUser(Bukkit.getPlayer(name));
                        Node node = InheritanceNode.builder("vip").build();

                        lp.getUserManager().saveUser(user);
                    } catch (Exception ex){
                        event.reply("Invalid username").queue();
                        return;
                    }

                    VIP vip = new VIP();

                    vip.setMinecraftUUID(mcUUID);
                    vip.setDiscordid(account.getDiscordID());
                    vip.setDuration(Duration);
                    vip.setUsername(Bukkit.getPlayer(mcUUID).getName());
                    vip.setDateOfOrder();

                    if (!found){
                        event.reply("Usuario nao vinculou a conta do discord.").queue();
                    }

                    try (FileWriter writer = new FileWriter(Global.vipsfile.getPath())){
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        gson.toJson(Global.vips, writer);
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    break;
                case 2:
                    order = new Order();
                    order.setMinecraftUsername(Bukkit.getPlayer(account.getMinecraftID()).getName());
                    order.setProductId(2);
                    Global.orders.add(order);
                case 3:
                    order = new Order();
                    order.setMinecraftUsername(Bukkit.getPlayer(account.getMinecraftID()).getName());
                    order.setProductId(3);
                    Global.orders.add(order);

            }



        }
    }
}
