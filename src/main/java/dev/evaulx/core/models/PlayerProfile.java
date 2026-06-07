package dev.evaulx.core.models;

import java.util.*;

public class PlayerProfile {

    private UUID uuid;
    private String name;
    private String ip;
    private String rankName;
    private List<String> extraRanks;
    private List<String> permissions;
    private String chatColor;
    private String nameColor;
    private String tag;
    private boolean buildMode;
    private long firstJoin;
    private long lastSeen;
    private boolean vanished;
    private boolean staffMode;
    private boolean godMode;
    private boolean socialSpy;
    private boolean msgToggled;
    private String disguiseName;
    private String disguiseSkin;
    private String disguiseRank;
    private boolean disguised;

    public PlayerProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.rankName = "default";
        this.extraRanks = new ArrayList<>();
        this.permissions = new ArrayList<>();
        this.chatColor = "";
        this.nameColor = "";
        this.tag = "";
        this.buildMode = false;
        this.firstJoin = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
        this.vanished = false;
        this.staffMode = false;
        this.godMode = false;
        this.socialSpy = false;
        this.msgToggled = true;
        this.disguised = false;
    }

    public boolean hasPermission(String permission) {
        if (permissions.contains("*")) return true;
        if (permissions.contains(permission)) return true;
        // Wildcard check e.g. evaulx.staff.*
        for (String perm : permissions) {
            if (perm.endsWith(".*")) {
                String base = perm.substring(0, perm.length() - 2);
                if (permission.startsWith(base)) return true;
            }
        }
        return false;
    }

    // Getters/setters
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getRankName() { return rankName; }
    public void setRankName(String rankName) { this.rankName = rankName; }

    public List<String> getExtraRanks() { return extraRanks; }
    public void addExtraRank(String rank) { if (!extraRanks.contains(rank)) extraRanks.add(rank); }
    public void removeExtraRank(String rank) { extraRanks.remove(rank); }

    public List<String> getPermissions() { return permissions; }
    public void addPermission(String perm) { if (!permissions.contains(perm)) permissions.add(perm); }
    public void removePermission(String perm) { permissions.remove(perm); }

    public String getChatColor() { return chatColor; }
    public void setChatColor(String chatColor) { this.chatColor = chatColor == null ? "" : chatColor; }

    public String getNameColor() { return nameColor; }
    public void setNameColor(String nameColor) { this.nameColor = nameColor == null ? "" : nameColor; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag == null ? "" : tag; }

    public boolean isBuildMode() { return buildMode; }
    public void setBuildMode(boolean buildMode) { this.buildMode = buildMode; }

    public long getFirstJoin() { return firstJoin; }
    public void setFirstJoin(long firstJoin) { this.firstJoin = firstJoin; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public boolean isVanished() { return vanished; }
    public void setVanished(boolean vanished) { this.vanished = vanished; }

    public boolean isStaffMode() { return staffMode; }
    public void setStaffMode(boolean staffMode) { this.staffMode = staffMode; }

    public boolean isGodMode() { return godMode; }
    public void setGodMode(boolean godMode) { this.godMode = godMode; }

    public boolean isSocialSpy() { return socialSpy; }
    public void setSocialSpy(boolean socialSpy) { this.socialSpy = socialSpy; }

    public boolean isMsgToggled() { return msgToggled; }
    public void setMsgToggled(boolean msgToggled) { this.msgToggled = msgToggled; }

    public boolean isDisguised() { return disguised; }
    public void setDisguised(boolean disguised) { this.disguised = disguised; }

    public String getDisguiseName() { return disguiseName; }
    public void setDisguiseName(String disguiseName) { this.disguiseName = disguiseName; }

    public String getDisguiseSkin() { return disguiseSkin; }
    public void setDisguiseSkin(String disguiseSkin) { this.disguiseSkin = disguiseSkin; }

    public String getDisguiseRank() { return disguiseRank; }
    public void setDisguiseRank(String disguiseRank) { this.disguiseRank = disguiseRank; }
}
