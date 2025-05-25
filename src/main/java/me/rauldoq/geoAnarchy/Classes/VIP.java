package me.rauldoq.geoAnarchy.Classes;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class VIP {
    private String username;
    private String discordid;
    private String minecraftUUID;
    private int duration;
    private LocalDate dateOfOrder;

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getMinecraftUUID() {
        return minecraftUUID;
    }

    public void setMinecraftUUID(String minecraftUUID) {
        this.minecraftUUID = minecraftUUID;
    }

    public String getDiscordid() {
        return discordid;
    }

    public void setDiscordid(String discordid) {
        this.discordid = discordid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDate getDateOfOrder() {
        return dateOfOrder;
    }

    public void setDateOfOrder() {
        this.dateOfOrder = LocalDate.now();
    }

    public boolean isValid(){
        LocalDate currentDate = LocalDate.now();
        long monthsBetween = ChronoUnit.MONTHS.between(dateOfOrder, currentDate);
        if (monthsBetween >= 1){
            return false;
        } else {
            return true;
        }
    }

    public void increaseDuration(){
        this.duration++;
    }

    public void decreaseDuration(){
        this.duration--;
    }
}
