package dev.evaulx.core.hooks;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VaultHook {

    private final EvaulxCore plugin;
    private EvaulxPermission permissionProvider;
    private EvaulxChat chatProvider;
    private boolean hooked;

    public VaultHook(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (!plugin.getConfig().getBoolean("hooks.vault.enabled", true)) return;
        // The check for Bukkit.getPluginManager().isPluginEnabled("Vault") is now done in EvaulxCore
        // to avoid loading this class if Vault is missing.

        permissionProvider = new EvaulxPermission(plugin);
        chatProvider = new EvaulxChat(plugin, permissionProvider);
        Bukkit.getServicesManager().register(Permission.class, permissionProvider, plugin, ServicePriority.Normal);
        Bukkit.getServicesManager().register(Chat.class, chatProvider, plugin, ServicePriority.Normal);
        hooked = true;
        plugin.getLogger().info("Hooked into Vault for permissions and chat metadata.");
    }

    public void unload() {
        if (!hooked) return;
        Bukkit.getServicesManager().unregister(Permission.class, permissionProvider);
        Bukkit.getServicesManager().unregister(Chat.class, chatProvider);
        hooked = false;
    }

    public boolean isHooked() {
        return hooked;
    }

    private static class EvaulxPermission extends Permission {

        private final EvaulxCore plugin;

        private EvaulxPermission(EvaulxCore plugin) {
            this.plugin = plugin;
        }

        @Override public String getName() { return "EvaulxMC"; }
        @Override public boolean isEnabled() { return plugin.isEnabled(); }
        @Override public boolean hasSuperPermsCompat() { return true; }

        @Override
        public boolean playerHas(String world, String player, String permission) {
            if (permission == null || player == null) return false;
            Player online = Bukkit.getPlayerExact(player);
            if (online != null && online.hasPermission(permission)) return true;

            PlayerProfile profile = profile(player);
            if (profile == null) return false;
            if (matches(profile.getPermissions(), permission)) return true;

            Rank primary = plugin.getRankManager().getRank(profile.getRankName());
            if (rankHas(primary, permission)) return true;
            for (String extra : profile.getExtraRanks()) {
                if (rankHas(plugin.getRankManager().getRank(extra), permission)) return true;
            }
            return false;
        }

        @Override
        public boolean playerAdd(String world, String player, String permission) {
            PlayerProfile profile = profile(player);
            if (profile == null || permission == null || permission.trim().isEmpty()) return false;
            profile.addPermission(permission);
            saveAndApply(profile);
            return true;
        }

        @Override
        public boolean playerRemove(String world, String player, String permission) {
            PlayerProfile profile = profile(player);
            if (profile == null || permission == null) return false;
            profile.removePermission(permission);
            saveAndApply(profile);
            return true;
        }

        @Override
        public boolean groupHas(String world, String group, String permission) {
            return rankHas(plugin.getRankManager().getRank(group), permission);
        }

        @Override
        public boolean groupAdd(String world, String group, String permission) {
            Rank rank = plugin.getRankManager().getRank(group);
            if (rank == null || permission == null || permission.trim().isEmpty()) return false;
            rank.addPermission(permission);
            plugin.getRankManager().saveRank(rank);
            refreshRank(rank.getName());
            return true;
        }

        @Override
        public boolean groupRemove(String world, String group, String permission) {
            Rank rank = plugin.getRankManager().getRank(group);
            if (rank == null || permission == null) return false;
            rank.removePermission(permission);
            plugin.getRankManager().saveRank(rank);
            refreshRank(rank.getName());
            return true;
        }

        @Override
        public boolean playerInGroup(String world, String player, String group) {
            PlayerProfile profile = profile(player);
            return profile != null && inGroup(profile, group);
        }

        @Override
        public boolean playerAddGroup(String world, String player, String group) {
            PlayerProfile profile = profile(player);
            Rank rank = plugin.getRankManager().getRank(group);
            if (profile == null || rank == null) return false;
            if (!profile.getRankName().equalsIgnoreCase(rank.getName())) profile.addExtraRank(rank.getName());
            saveAndApply(profile);
            return true;
        }

        @Override
        public boolean playerRemoveGroup(String world, String player, String group) {
            PlayerProfile profile = profile(player);
            if (profile == null || group == null) return false;
            if (profile.getRankName().equalsIgnoreCase(group)) {
                Rank defaultRank = plugin.getRankManager().getDefaultRank();
                if (defaultRank == null) return false;
                profile.setRankName(defaultRank.getName());
            }
            removeIgnoreCase(profile.getExtraRanks(), group);
            saveAndApply(profile);
            return true;
        }

        @Override
        public String[] getPlayerGroups(String world, String player) {
            PlayerProfile profile = profile(player);
            if (profile == null) return new String[0];
            List<String> groups = new ArrayList<>();
            groups.add(profile.getRankName());
            groups.addAll(profile.getExtraRanks());
            return groups.toArray(new String[0]);
        }

        @Override
        public String getPrimaryGroup(String world, String player) {
            PlayerProfile profile = profile(player);
            return profile == null ? "" : profile.getRankName();
        }

        @Override
        public String[] getGroups() {
            List<String> groups = new ArrayList<>();
            for (Rank rank : plugin.getRankManager().getRanks()) groups.add(rank.getName());
            return groups.toArray(new String[0]);
        }

        @Override public boolean hasGroupSupport() { return true; }

        private PlayerProfile profile(String playerName) {
            if (playerName == null || playerName.trim().isEmpty()) return null;
            Player online = Bukkit.getPlayerExact(playerName);
            if (online != null) return plugin.getPlayerManager().getProfile(online);

            OfflinePlayer target = plugin.getPlayerLookupManager().find(playerName);
            if (target == null) return null;
            PlayerProfile loaded = plugin.getPlayerManager().getProfile(target.getUniqueId());
            if (loaded != null) return loaded;
            return plugin.getDatabaseManager().loadProfile(target.getUniqueId(),
                    target.getName() == null ? playerName : target.getName());
        }

        private boolean rankHas(Rank rank, String permission) {
            return rank != null && matches(plugin.getRankManager().getAllPermissions(rank), permission);
        }

        private boolean matches(List<String> permissions, String permission) {
            if (permission == null) return false;
            for (String current : permissions) {
                if (current == null) continue;
                if (current.equals("*") || current.equalsIgnoreCase(permission)) return true;
                if (current.endsWith(".*")) {
                    String base = current.substring(0, current.length() - 1).toLowerCase(Locale.ENGLISH);
                    if (permission.toLowerCase(Locale.ENGLISH).startsWith(base)) return true;
                }
            }
            return false;
        }

        private boolean inGroup(PlayerProfile profile, String group) {
            if (group == null) return false;
            if (profile.getRankName().equalsIgnoreCase(group)) return true;
            for (String extra : profile.getExtraRanks()) {
                if (extra.equalsIgnoreCase(group)) return true;
            }
            return false;
        }

        private void saveAndApply(PlayerProfile profile) {
            Player online = Bukkit.getPlayer(profile.getUuid());
            if (online != null) {
                plugin.getPlayerManager().saveProfile(profile);
                plugin.getPlayerManager().applyPermissions(online, profile);
                plugin.getNameTagManager().applyNameTag(online);
            } else {
                plugin.getDatabaseManager().saveProfile(profile);
            }
        }

        private void refreshRank(String rankName) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
                if (profile == null || !inGroup(profile, rankName)) continue;
                plugin.getPlayerManager().applyPermissions(player, profile);
                plugin.getNameTagManager().applyNameTag(player);
            }
        }

        private boolean removeIgnoreCase(List<String> values, String value) {
            boolean removed = false;
            for (java.util.Iterator<String> iterator = values.iterator(); iterator.hasNext();) {
                if (!iterator.next().equalsIgnoreCase(value)) continue;
                iterator.remove();
                removed = true;
            }
            return removed;
        }
    }

    private static class EvaulxChat extends Chat {

        private final EvaulxCore plugin;

        private EvaulxChat(EvaulxCore plugin, Permission permission) {
            super(permission);
            this.plugin = plugin;
        }

        @Override public String getName() { return "EvaulxMC"; }
        @Override public boolean isEnabled() { return plugin.isEnabled(); }

        @Override public String getPlayerPrefix(String world, String player) { return rank(player).getPrefix(); }
        @Override public void setPlayerPrefix(String world, String player, String prefix) {}
        @Override public String getPlayerSuffix(String world, String player) { return rank(player).getSuffix(); }
        @Override public void setPlayerSuffix(String world, String player, String suffix) {}
        @Override public String getGroupPrefix(String world, String group) { Rank rank = plugin.getRankManager().getRank(group); return rank == null ? "" : rank.getPrefix(); }
        @Override public void setGroupPrefix(String world, String group, String prefix) { updateGroup(group, prefix, null); }
        @Override public String getGroupSuffix(String world, String group) { Rank rank = plugin.getRankManager().getRank(group); return rank == null ? "" : rank.getSuffix(); }
        @Override public void setGroupSuffix(String world, String group, String suffix) { updateGroup(group, null, suffix); }

        @Override public int getPlayerInfoInteger(String world, String player, String node, int defaultValue) { return intInfo(rank(player), node, defaultValue); }
        @Override public void setPlayerInfoInteger(String world, String player, String node, int value) {}
        @Override public int getGroupInfoInteger(String world, String group, String node, int defaultValue) { return intInfo(plugin.getRankManager().getRank(group), node, defaultValue); }
        @Override public void setGroupInfoInteger(String world, String group, String node, int value) {}
        @Override public double getPlayerInfoDouble(String world, String player, String node, double defaultValue) { return intInfo(rank(player), node, (int) defaultValue); }
        @Override public void setPlayerInfoDouble(String world, String player, String node, double value) {}
        @Override public double getGroupInfoDouble(String world, String group, String node, double defaultValue) { return intInfo(plugin.getRankManager().getRank(group), node, (int) defaultValue); }
        @Override public void setGroupInfoDouble(String world, String group, String node, double value) {}
        @Override public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) { return boolInfo(rank(player), node, defaultValue); }
        @Override public void setPlayerInfoBoolean(String world, String player, String node, boolean value) {}
        @Override public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) { return boolInfo(plugin.getRankManager().getRank(group), node, defaultValue); }
        @Override public void setGroupInfoBoolean(String world, String group, String node, boolean value) {}
        @Override public String getPlayerInfoString(String world, String player, String node, String defaultValue) { return stringInfo(rank(player), node, defaultValue); }
        @Override public void setPlayerInfoString(String world, String player, String node, String value) {}
        @Override public String getGroupInfoString(String world, String group, String node, String defaultValue) { return stringInfo(plugin.getRankManager().getRank(group), node, defaultValue); }
        @Override public void setGroupInfoString(String world, String group, String node, String value) {}

        private Rank rank(String playerName) {
            Player online = Bukkit.getPlayerExact(playerName);
            PlayerProfile profile = online == null ? null : plugin.getPlayerManager().getProfile(online);
            if (profile == null) {
                OfflinePlayer target = plugin.getPlayerLookupManager().find(playerName);
                if (target != null) profile = plugin.getDatabaseManager().loadProfile(target.getUniqueId(),
                        target.getName() == null ? playerName : target.getName());
            }
            Rank rank = profile == null ? null : plugin.getRankManager().getRank(profile.getRankName());
            if (rank == null) rank = plugin.getRankManager().getDefaultRank();
            return rank == null ? new Rank("Default") : rank;
        }

        private int intInfo(Rank rank, String node, int fallback) {
            if (rank == null || node == null) return fallback;
            if (node.equalsIgnoreCase("weight") || node.equalsIgnoreCase("priority")) return rank.getWeight();
            return fallback;
        }

        private boolean boolInfo(Rank rank, String node, boolean fallback) {
            if (rank == null || node == null) return fallback;
            if (node.equalsIgnoreCase("staff")) return rank.isStaff();
            if (node.equalsIgnoreCase("default")) return rank.isDefault();
            if (node.equalsIgnoreCase("hidden")) return rank.isHidden();
            return fallback;
        }

        private String stringInfo(Rank rank, String node, String fallback) {
            if (rank == null || node == null) return fallback;
            if (node.equalsIgnoreCase("rank") || node.equalsIgnoreCase("group")) return rank.getName();
            if (node.equalsIgnoreCase("display")) return rank.getDisplay();
            if (node.equalsIgnoreCase("category")) return plugin.getRankManager().getRankCategory(rank);
            if (node.equalsIgnoreCase("color") || node.equalsIgnoreCase("namecolor")) return rank.getColor();
            return fallback;
        }

        private void updateGroup(String group, String prefix, String suffix) {
            Rank rank = plugin.getRankManager().getRank(group);
            if (rank == null) return;
            if (prefix != null) rank.setPrefix(prefix);
            if (suffix != null) rank.setSuffix(suffix);
            plugin.getRankManager().saveRank(rank);
        }
    }
}
