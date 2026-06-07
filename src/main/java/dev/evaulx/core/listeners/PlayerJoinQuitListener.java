package dev.evaulx.core.listeners;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.commands.staff.VanishCommand;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

public class PlayerJoinQuitListener implements Listener {

    private final EvaulxCore plugin;

    public PlayerJoinQuitListener(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        boolean firstJoin = !player.hasPlayedBefore();
        TaskUtil.async(() -> {
            PlayerProfile profile = plugin.getPlayerManager().loadProfile(player);
            profile.setIp(player.getAddress().getAddress().getHostAddress());
            profile.setLastSeen(System.currentTimeMillis());
            if (firstJoin || profile.getFirstJoin() == 0) profile.setFirstJoin(System.currentTimeMillis());
            plugin.getGrantManager().reconcileProfile(profile);

            TaskUtil.sync(() -> {
                applyFirstJoinRules(player, profile, firstJoin);
                plugin.getPlayerLookupManager().remember(player.getUniqueId(), player.getName());
                plugin.getNameTagManager().applyNameTag(player);
                plugin.getPlayerManager().applyPermissions(player, profile);
                plugin.getStaffRequestManager().startStaffSession(player);
                if (shouldAutoStaffMode(player, profile)) {
                    profile.setStaffMode(true);
                    plugin.getPlayerManager().saveProfile(profile);
                }
                if (profile.isStaffMode()) {
                    plugin.getStaffRequestManager().applyStaffModeState(player);
                    plugin.getStaffRequestManager().applyStaffModeItems(player);
                    if (plugin.getConfig().getBoolean("staff-tools.staffmode.auto-vanish", true) && !profile.isVanished()) {
                        new VanishCommand(plugin).setVanished(player, profile, true);
                        plugin.getStaffRequestManager().markStaffModeAutoVanished(player);
                    }
                }
                sendStaffServerAlert(player, true);
                plugin.getNoteManager().alertStaffOnJoin(player);

                // Vanish check
                if (profile.isVanished()) {
                    for (Player online : player.getServer().getOnlinePlayers()) {
                        if (!canSeeVanished(online)) online.hidePlayer(player);
                    }
                }
                // Hide vanished players from this player
                for (Player online : player.getServer().getOnlinePlayers()) {
                    PlayerProfile op = plugin.getPlayerManager().getProfile(online);
                    if (op != null && op.isVanished() && !canSeeVanished(player)) {
                        player.hidePlayer(online);
                    }
                }

                if (plugin.getStaffRequestManager().isFrozen(player)) {
                    player.sendMessage(CC.color("&cYou are still frozen. &7Do not log out."));
                }
                plugin.getDisguiseManager().handleJoin(player);
            });
        });

        String joinMsg = plugin.getConfig().getBoolean("join-quit.enabled", true)
                ? plugin.getConfig().getString(firstJoin ? "join-quit.first-join-message" : "join-quit.join-message",
                        firstJoin ? "&6[NEW] &f{player} &7joined for the first time!" : "&a+ &f{player}").replace("{player}", player.getName())
                : null;
        e.setJoinMessage(joinMsg != null ? CC.color(joinMsg) : null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        plugin.getNameTagManager().removeNameTag(player);
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile != null && profile.isStaffMode()) {
            plugin.getStaffRequestManager().restoreStaffModeItems(player);
            plugin.getStaffRequestManager().restoreStaffModeState(player);
            profile.setStaffMode(false);
        }
        if (plugin.getStaffRequestManager().isFrozen(player)) {
            plugin.getStaffRequestManager().logAction(player.getName(), "FROZEN_LOGOUT", player.getName(), "Player quit while frozen");
            plugin.getDiscordManager().sendFrozenLogout(player.getName());
        }
        sendStaffServerAlert(player, false);
        plugin.getStaffRequestManager().endStaffSession(player);
        plugin.getPlayerLookupManager().remember(player.getUniqueId(), player.getName());

        TaskUtil.async(() -> plugin.getPlayerManager().unloadProfile(player.getUniqueId()));

        String quitMsg = plugin.getConfig().getBoolean("join-quit.enabled", true)
                ? plugin.getConfig().getString("join-quit.quit-message", "&c- &f{player}").replace("{player}", player.getName())
                : null;
        e.setQuitMessage(quitMsg != null ? CC.color(quitMsg) : null);
    }

    private boolean canSeeVanished(Player player) {
        return player.hasPermission("evaulx.staff") || player.hasPermission("evaulx.vanish.see");
    }

    private void sendStaffServerAlert(Player player, boolean joined) {
        if (!plugin.getConfig().getBoolean("staff-tools.staff-join-leave-alerts.enabled", true)) return;
        if (!plugin.getStaffRequestManager().canReceiveStaffAlerts(player)) return;

        String server = plugin.getConfig().getString("server.server-id", "hub");
        plugin.getStaffRequestManager().logAction(player.getName(), joined ? "STAFF_JOIN" : "STAFF_LEAVE", server,
                joined ? "Joined server" : "Left server");
        plugin.getDiscordManager().sendStaffServerEvent(player.getName(), joined, server);
        if (plugin.getConfig().getBoolean("staff-tools.staff-join-leave-alerts.local", true)) {
            String key = joined ? "staff-alerts.join-local" : "staff-alerts.leave-local";
            String fallback = joined
                    ? "&8[&cStaff&8] &f{player} &7joined this server."
                    : "&8[&cStaff&8] &f{player} &7left this server.";
            String message = plugin.getMessageManager().get(key, fallback,
                    plugin.getMessageManager().placeholders("{player}", player.getName(), "{server}", server));
            plugin.getStaffRequestManager().broadcastStaff(message);
        }

        if (plugin.getConfig().getBoolean("staff-tools.staff-join-leave-alerts.network", true)
                && plugin.getRedisSyncManager() != null) {
            plugin.getRedisSyncManager().publishStaffServerEvent(player.getName(), player.getUniqueId(), joined);
        }
    }

    private boolean shouldAutoStaffMode(Player player, PlayerProfile profile) {
        if (profile == null || profile.isStaffMode()) return false;
        if (!plugin.getConfig().getBoolean("staff-tools.staffmode.auto-enable-on-join.enabled", false)) return false;

        String permission = plugin.getConfig().getString("staff-tools.staffmode.auto-enable-on-join.permission", "evaulx.staffmode.autojoin");
        if (permission != null && !permission.trim().isEmpty() && player.hasPermission(permission)) return true;

        for (String rank : plugin.getConfig().getStringList("staff-tools.staffmode.auto-enable-on-join.ranks")) {
            if (rank.equalsIgnoreCase(profile.getRankName())) return true;
            for (String extra : profile.getExtraRanks()) {
                if (rank.equalsIgnoreCase(extra)) return true;
            }
        }
        return false;
    }

    private void applyFirstJoinRules(Player player, PlayerProfile profile, boolean firstJoin) {
        if (!firstJoin || !plugin.getConfig().getBoolean("first-join.enabled", true)) return;

        boolean changed = false;
        String rankName = plugin.getConfig().getString("first-join.rank", "");
        if (rankName != null && !rankName.trim().isEmpty()) {
            Rank rank = plugin.getRankManager().getRank(rankName);
            if (rank != null) {
                profile.setRankName(rank.getName());
                changed = true;
            }
        }

        String tag = plugin.getConfig().getString("first-join.tag", "");
        if (tag != null && !tag.trim().isEmpty()) {
            profile.setTag(tag);
            changed = true;
        }

        for (String command : plugin.getConfig().getStringList("first-join.commands")) {
            if (command == null || command.trim().isEmpty()) continue;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
        }

        String message = plugin.getConfig().getString("first-join.message", "");
        if (message != null && !message.trim().isEmpty()) {
            player.sendMessage(CC.color(message.replace("{player}", player.getName())));
        }

        if (changed) plugin.getPlayerManager().saveProfile(profile);
    }
}
