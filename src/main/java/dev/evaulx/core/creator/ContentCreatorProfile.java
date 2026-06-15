package dev.evaulx.core.creator;

import java.util.Locale;
import java.util.UUID;

public final class ContentCreatorProfile {

    private final UUID uuid;
    private String name = "";
    private String displayName = "";
    private String youtube = "";
    private String twitch = "";
    private String twitter = "";
    private String rewardCode = "";
    private String description = "";
    private String streamTitle = "";
    private String tiktok = "";
    private String instagram = "";
    private String discord = "";

    public ContentCreatorProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public String effectiveDisplayName() {
        return displayName.isEmpty() ? name : displayName;
    }

    public void setRewardCode(String code) {
        this.rewardCode = code == null ? "" : code.toLowerCase(Locale.ENGLISH).trim();
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n == null ? "" : n; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String d) { this.displayName = d == null ? "" : d; }
    public String getYoutube() { return youtube; }
    public void setYoutube(String y) { this.youtube = y == null ? "" : y; }
    public String getTwitch() { return twitch; }
    public void setTwitch(String t) { this.twitch = t == null ? "" : t; }
    public String getTwitter() { return twitter; }
    public void setTwitter(String t) { this.twitter = t == null ? "" : t; }
    public String getRewardCode() { return rewardCode; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d == null ? "" : d; }
    public String getStreamTitle() { return streamTitle; }
    public void setStreamTitle(String t) { this.streamTitle = t == null ? "" : t; }
    public String getTiktok() { return tiktok; }
    public void setTiktok(String t) { this.tiktok = t == null ? "" : t; }
    public String getInstagram() { return instagram; }
    public void setInstagram(String i) { this.instagram = i == null ? "" : i; }
    public String getDiscord() { return discord; }
    public void setDiscord(String d) { this.discord = d == null ? "" : d; }
}
