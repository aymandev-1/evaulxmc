package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Map;

public class VanishCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public VanishCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target = null;
        Boolean requestedState = null;

        if (args.length == 0 || isState(args[0])) {
            if (!(sender instanceof Player)) {
                plugin.getMessageManager().send(sender, "vanish.usage", "&cUsage: /vanish [player] [on|off]");
                return true;
            }

            target = (Player) sender;
            if (args.length > 0) requestedState = parseState(args[0]);
        } else {
            if (!hasPermission(sender, "evaulx.vanish.others")) {
                plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
                return true;
            }

            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.getMessageManager().send(sender, "player-not-online", "&cPlayer not online.");
                return true;
            }

            if (args.length > 1) {
                requestedState = parseState(args[1]);
                if (requestedState == null) {
                    plugin.getMessageManager().send(sender, "vanish.usage", "&cUsage: /vanish [player] [on|off]");
                    return true;
                }
            }
        }

        if (!target.equals(sender) && !hasPermission(sender, "evaulx.vanish.others")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (target.equals(sender) && !hasPermission(sender, "evaulx.vanish")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        PlayerProfile profile = plugin.getPlayerManager().getProfile(target);
        if (profile == null) {
            plugin.getMessageManager().send(sender, "profile-loading", "&cThat player's profile is still loading.");
            return true;
        }

        boolean vanished = requestedState != null ? requestedState : !profile.isVanished();
        Map<String, String> placeholders = placeholders(target, vanished);
        if (profile.isVanished() == vanished) {
            plugin.getMessageManager().send(sender, "vanish.already",
                    "&7Vanish is already {state} &7for &f{target}&7.", placeholders);
            return true;
        }

        setVanished(target, profile, vanished);

        sendVanishFeedback(target, vanished);
        if (!target.equals(sender)) {
            plugin.getMessageManager().send(sender, "vanish.set-other",
                    "&7Set vanish for &f{target} &7to {state}&7.", placeholders);
        }

        notifyStaffExcept(target, plugin.getMessageManager().get("vanish.staff-notify",
                "&8[&cStaff&8] &f{player} &7is now {state}&7.", placeholders));
        plugin.getStaffRequestManager().logAction(sender.getName(), "VANISH", target.getName(), vanished ? "enabled" : "disabled");
        return true;
    }

    public void sendVanishFeedback(Player target, boolean vanished) {
        Map<String, String> placeholders = placeholders(target, vanished);
        plugin.getMessageManager().send(target, vanished ? "vanish.enabled" : "vanish.disabled",
                vanished ? "&7You are now &cvanished&7." : "&7You are now &fvisible&7.", placeholders);
        plugin.getMessageManager().actionBar(target, vanished ? "action-bars.vanish.enabled" : "action-bars.vanish.disabled",
                vanished ? "&cVanish enabled &8| &fHidden from players" : "&aVanish disabled &8| &fYou are visible",
                placeholders);
    }

    public void setVanished(Player target, PlayerProfile profile, boolean vanished) {
        profile.setVanished(vanished);
        plugin.getPlayerManager().saveProfile(profile);

        if (vanished) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(target)) continue;
                if (canSeeVanished(viewer)) viewer.showPlayer(target);
                else viewer.hidePlayer(target);
            }
            publishVanishStatus(target, vanished);
            return;
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showPlayer(target);
        }
        publishVanishStatus(target, vanished);
    }

    private void publishVanishStatus(Player target, boolean vanished) {
        if (plugin.getRedisSyncManager() == null) return;
        plugin.getRedisSyncManager().publishVanish(target.getName(), target.getUniqueId(), vanished);
        plugin.getRedisSyncManager().publishStaffStatus(target);
    }

    private boolean canSeeVanished(Player viewer) {
        return viewer.hasPermission("evaulx.staff") || viewer.hasPermission("evaulx.vanish.see");
    }

    private void notifyStaffExcept(Player excluded, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(excluded) && plugin.getStaffRequestManager().canReceiveStaffAlerts(player)) {
                player.sendMessage(message);
            }
        }
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return !(sender instanceof Player) || sender.hasPermission(permission);
    }

    private boolean isState(String value) {
        return parseState(value) != null;
    }

    private Boolean parseState(String value) {
        if (value == null) return null;
        if (value.equalsIgnoreCase("on") || value.equalsIgnoreCase("enable") || value.equalsIgnoreCase("enabled")) {
            return true;
        }
        if (value.equalsIgnoreCase("off") || value.equalsIgnoreCase("disable") || value.equalsIgnoreCase("disabled")) {
            return false;
        }
        return null;
    }

    private String stateText(boolean state) {
        return plugin.getMessageManager().get(state ? "state.enabled-word" : "state.disabled-word",
                state ? "&aenabled" : "&cdisabled");
    }

    private Map<String, String> placeholders(Player target, boolean state) {
        return plugin.getMessageManager().placeholders(
                "{player}", target.getName(),
                "{target}", target.getName(),
                "{state}", stateText(state)
        );
    }
}
