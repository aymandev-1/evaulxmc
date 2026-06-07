package dev.evaulx.core.hooks;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.DisplayUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class PlaceholderHook extends PlaceholderExpansion {

    private final EvaulxCore plugin;

    public PlaceholderHook(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "evaulx"; }
    @Override public String getAuthor()     { return "EvaulxMC"; }
    @Override public String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()      { return true; }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        String serverValue = serverPlaceholder(identifier);
        if (serverValue != null) return serverValue;
        if (player == null) return "";

        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null) return "";
        Rank rank = null;
        if (profile.isDisguised() && profile.getDisguiseRank() != null) {
            rank = plugin.getRankManager().getRank(profile.getDisguiseRank());
        }
        if (rank == null) rank = plugin.getRankManager().getRank(profile.getRankName());

        switch (identifier) {
            case "player_name":
            case "visible_name":
                return plugin.getDisguiseManager().getVisibleName(player);
            case "player_real_name":
            case "real_name":
                return profile.getName();
            case "player_rank":
                return rank != null ? rank.getName() : "default";
            case "player_rank_display":
                return rank != null ? rank.getDisplayName() : "default";
            case "player_rank_display_raw":
                return rank != null ? rank.getDisplay() : "";
            case "player_rank_category":
                return rank != null ? plugin.getRankManager().getRankCategory(rank) : "Default";
            case "player_rank_permission":
                return rank != null ? rank.getPermission() : "";
            case "player_rank_priority":
            case "player_rank_weight":
                return rank != null ? String.valueOf(rank.getWeight()) : "0";
            case "player_rank_staff":
                return String.valueOf(rank != null && rank.isStaff());
            case "player_rank_default":
                return String.valueOf(rank != null && rank.isDefault());
            case "player_rank_hidden":
                return String.valueOf(rank != null && rank.isHidden());
            case "player_prefix":
                return rank != null ? rank.getPrefix() : "";
            case "player_prefix_plain":
                return rank != null ? DisplayUtil.stripFormat(rank.getPrefix()) : "";
            case "player_suffix":
                return rank != null ? rank.getSuffix() : "";
            case "player_suffix_plain":
                return rank != null ? DisplayUtil.stripFormat(rank.getSuffix()) : "";
            case "player_color":
            case "player_name_color_rank":
                return rank != null ? rank.getColor() : "&f";
            case "player_tag":
                return profile.getTag() != null ? profile.getTag() : "";
            case "player_tag_plain":
                return DisplayUtil.stripFormat(profile.getTag());
            case "player_has_tag":
                return String.valueOf(profile.getTag() != null && !profile.getTag().isEmpty());
            case "player_chat_color":
                return profile.getChatColor();
            case "player_name_color":
                return profile.getNameColor();
            case "player_buildmode":
                return String.valueOf(profile.isBuildMode());
            case "player_vanished":
                return String.valueOf(profile.isVanished());
            case "player_staffmode":
                return String.valueOf(profile.isStaffMode());
            case "player_godmode":
                return String.valueOf(profile.isGodMode());
            case "player_disguised":
            case "is_disguised":
                return String.valueOf(profile.isDisguised());
            case "player_disguise_name":
            case "disguise_name":
                return profile.isDisguised() ? profile.getDisguiseName() : profile.getName();
            case "player_disguise_skin":
            case "disguise_skin":
                return profile.isDisguised() && profile.getDisguiseSkin() != null ? profile.getDisguiseSkin() : "";
            case "player_disguise_rank":
            case "disguise_rank":
                return profile.isDisguised() && profile.getDisguiseRank() != null ? profile.getDisguiseRank() : "";
            case "player_first_join":
                return String.valueOf(profile.getFirstJoin());
            case "player_last_seen":
                return String.valueOf(profile.getLastSeen());
            case "lobby_protection_world":
                return String.valueOf(isProtectedWorld(player.getWorld().getName()));
            default:
                return null;
        }
    }

    private String serverPlaceholder(String identifier) {
        switch (identifier) {
            case "server_name":
                return plugin.getConfig().getString("server.name", "EvaulxMC");
            case "server_id":
                return plugin.getConfig().getString("server.server-id", "hub");
            case "online":
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            case "ranks_loaded":
                return String.valueOf(plugin.getRankManager().getRanks().size());
            case "ranks_visible":
                return String.valueOf(plugin.getRankManager().getVisibleRanks().size());
            case "nametags_enabled":
                return String.valueOf(plugin.getConfig().getBoolean("nametags.enabled", true));
            case "nametags_scoreboard_enabled":
                return String.valueOf(plugin.getNameTagManager().isScoreboardEnabled());
            case "tablist_enabled":
                return String.valueOf(plugin.getNameTagManager().isTabListEnabled());
            case "grants_active":
                return String.valueOf(plugin.getGrantManager().getActiveGrantCount());
            case "grants_total":
                return String.valueOf(plugin.getGrantManager().getStoredGrantCount());
            case "pending_grants":
                return String.valueOf(plugin.getGrantManager().getPendingGrants().size());
            case "lookup_cache_size":
                return String.valueOf(plugin.getPlayerLookupManager().size());
            case "disguise_online":
                return String.valueOf(plugin.getDisguiseManager().getOnlineDisguiseCount());
            case "disguise_skin_cache_size":
                return String.valueOf(plugin.getDisguiseManager().getSkinCacheSize());
            case "disguise_safe_mode":
                return String.valueOf(plugin.getDisguiseManager().isSafeModeEnabled());
            case "staff_sessions_active":
                return String.valueOf(plugin.getStaffRequestManager().getActiveStaffSessionCount());
            case "maintenance_enabled":
                return String.valueOf(plugin.getConfig().getBoolean("maintenance.enabled", false));
            case "maintenance_reason":
                return plugin.getConfig().getString("maintenance.reason", "Server maintenance");
            case "lobby_protection_enabled":
                return String.valueOf(plugin.getConfig().getBoolean("lobby-protection.enabled", true));
            case "lobby_protection_worlds":
                return protectionMode();
            case "lobby_protection_mode":
                return plugin.getConfig().getStringList("lobby-protection.worlds").isEmpty() ? "all" : "configured";
            default:
                return null;
        }
    }

    private boolean isProtectedWorld(String world) {
        if (!plugin.getConfig().getBoolean("lobby-protection.enabled", true)) return false;
        List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
        if (worlds.isEmpty()) return true;
        for (String configured : worlds) {
            if (configured.equalsIgnoreCase(world)) return true;
        }
        return false;
    }

    private String protectionMode() {
        List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
        if (worlds.isEmpty()) return "all";
        StringBuilder builder = new StringBuilder();
        for (String world : worlds) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(world);
        }
        return builder.toString();
    }
}
