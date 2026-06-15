package dev.evaulx.core.hooks;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.managers.PartyManager;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.DisplayUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;

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
            case "player_playtime": {
                long ms = System.currentTimeMillis() - profile.getFirstJoin();
                long h = ms / 3600000; long m = (ms % 3600000) / 60000;
                return h + "h " + m + "m";
            }
            case "player_afk":
                return String.valueOf(plugin.getAfkManager().isAfk(player.getUniqueId()));
            case "player_ping":
                return String.valueOf(safeGetPing(player));
            case "player_frozen":
                return String.valueOf(plugin.getStaffRequestManager().isFrozen(player));
            case "player_streamer":
                return String.valueOf(profile.isStreamerMode());
            case "player_streamer_name":
                return profile.isStreamerMode() && profile.getStreamerAlias() != null ? profile.getStreamerAlias() : "";
            case "player_msg_toggle":
                return String.valueOf(profile.isMsgToggled());
            case "player_muted":
                return String.valueOf(plugin.getPunishmentManager().getActiveMute(player.getUniqueId()) != null);
            case "lobby_protection_world":
                return String.valueOf(isProtectedWorld(player.getWorld().getName()));
            // Coins
            case "player_coins":
                return String.valueOf(plugin.getCoinsManager().getCoins(player.getUniqueId()));
            // Friends
            case "player_friends_count":
                return String.valueOf(plugin.getFriendManager().getFriends(player.getUniqueId()).size());
            case "player_online_friends":
                return String.valueOf(plugin.getFriendManager().getOnlineFriendsCount(player.getUniqueId()));
            // Party
            case "player_in_party":
                return String.valueOf(plugin.getPartyManager().isInParty(player.getUniqueId()));
            case "player_party_size": {
                PartyManager.Party party = plugin.getPartyManager().getParty(player.getUniqueId());
                return party != null ? String.valueOf(party.size()) : "0";
            }
            case "player_party_role": {
                if (!plugin.getPartyManager().isInParty(player.getUniqueId())) return "none";
                return plugin.getPartyManager().isLeader(player.getUniqueId()) ? "leader" : "member";
            }
            case "player_party_leader": {
                PartyManager.Party p2 = plugin.getPartyManager().getParty(player.getUniqueId());
                if (p2 == null) return "";
                Player leader = Bukkit.getPlayer(p2.getLeader());
                return leader != null ? leader.getName() : p2.getLeader().toString().substring(0, 8);
            }
            // Homes
            case "player_home_count":
                return String.valueOf(plugin.getHomeManager().getHomes(player.getUniqueId()).size());
            // Mail
            case "player_mail_unread":
                return String.valueOf(plugin.getMailManager().getUnreadCount(player.getUniqueId()));
            // Daily reward
            case "player_daily_claimed":
                return String.valueOf(!plugin.getDailyRewardManager().canClaim(player.getUniqueId()));
            case "player_daily_time_remaining": {
                if (plugin.getDailyRewardManager().canClaim(player.getUniqueId())) return "ready";
                long ms = plugin.getDailyRewardManager().getTimeUntilNext(player.getUniqueId());
                long h = ms / 3600000L;
                long m = (ms % 3600000L) / 60000L;
                return h + "h " + m + "m";
            }
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
            case "pending_appeals":
                return String.valueOf(plugin.getAppealManager().getPending().size());
            // TPS (Paper-only; returns "N/A" on Spigot)
            case "server_tps": {
                double[] tps = safeTps();
                return String.format("%.2f", Math.min(20.0, tps[0]));
            }
            case "server_tps_5m": {
                double[] tps = safeTps();
                return String.format("%.2f", Math.min(20.0, tps[1]));
            }
            case "server_tps_15m": {
                double[] tps = safeTps();
                return String.format("%.2f", Math.min(20.0, tps[2]));
            }
            case "server_max_players":
                return String.valueOf(Bukkit.getMaxPlayers());
            // RAM
            case "server_ram_used": {
                long used = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L;
                return used + "MB";
            }
            case "server_ram_max": {
                long max = Runtime.getRuntime().maxMemory() / 1048576L;
                return max + "MB";
            }
            case "server_ram_percent": {
                long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                long max = Runtime.getRuntime().maxMemory();
                return String.format("%.1f", (used * 100.0) / max) + "%";
            }
            // Uptime
            case "server_uptime": {
                long ms = ManagementFactory.getRuntimeMXBean().getUptime();
                long h = ms / 3600000L;
                long m = (ms % 3600000L) / 60000L;
                return h + "h " + m + "m";
            }
            default:
                return null;
        }
    }

    private static int safeGetPing(Player p) {
        try {
            Object handle = p.getClass().getMethod("getHandle").invoke(p);
            return handle.getClass().getField("ping").getInt(handle);
        } catch (Throwable ignored) { return 0; }
    }

    private static double[] safeTps() {
        try {
            Object craftServer = Bukkit.getServer();
            Object minecraftServer = craftServer.getClass().getMethod("getServer").invoke(craftServer);
            return (double[]) minecraftServer.getClass().getField("recentTps").get(minecraftServer);
        } catch (Throwable ignored) { return new double[]{20.0, 20.0, 20.0}; }
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
