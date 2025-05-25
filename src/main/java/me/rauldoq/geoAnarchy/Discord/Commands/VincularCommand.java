package me.rauldoq.geoAnarchy.Discord.Commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.rauldoq.geoAnarchy.Classes.ConnectedAccounts;
import me.rauldoq.geoAnarchy.Discord.ICommand;
import me.rauldoq.geoAnarchy.Global;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VincularCommand implements ICommand {
    @Override
    public String getName() {
        return "vincular";
    }

    @Override
    public String getDescription() {
        return "vincula conta do minecraft com o discord";
    }

    @Override
    public List<OptionData> getOptions() {
        List<OptionData> data = new ArrayList<>();
        data.add(new OptionData(OptionType.INTEGER, "codigo", "codigo que recebeu no minecraft", true));
        return data;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {

        int code = event.getOption("codigo").getAsInt();
        System.out.println("Code executed, code: " + code);
        boolean doesUserExist = false;
        if (Global.connectedAccounts != null){
            try{
                for (ConnectedAccounts account : Global.connectedAccounts){
                    if (account.getDiscordID().equals(event.getUser().getId())){
                        System.out.println("usuario ja existe");
                        doesUserExist = true;
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        if (!doesUserExist){
            try{
                for (ConnectedAccounts account : Global.connectedAccounts){
                    if (account.getCode() == code && account.getCode() != -1){
                        account.setDiscordID(event.getUser().getId());
                        try {
                            Bukkit.getPlayer(UUID.fromString(account.getMinecraftID())).sendMessage("Registrado com sucesso");
                            account.setCode(-1);
                            event.reply("Conta vinculada com sucesso, " + Bukkit.getPlayer(UUID.fromString(account.getMinecraftID())).getName()).queue();
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        try (FileWriter writer = new FileWriter(Global.discordConnectionsFile.getPath())){
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            gson.toJson(Global.connectedAccounts, writer);
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }

            } catch (Exception e){
                e.printStackTrace();
            }

        }

    }

    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
