package me.rauldoq.geoAnarchy.Classes;

public class ConnectedAccounts {
    private String discordID;
    private String minecraftID;
    private int Code;


    public String getDiscordID(){
        return discordID;
    }
    public String getMinecraftID(){
        return minecraftID;
    }
    public void setDiscordID(String ID){
        this.discordID = ID;
    }
    public void setMinecraftID(String ID){
        this.minecraftID = ID;
    }

    public int getCode() {
        return Code;
    }

    public void setCode(int code) {
        Code = code;
    }

}
