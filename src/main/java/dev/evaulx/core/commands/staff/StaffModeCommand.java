package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Map;

public class StaffModeCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public StaffModeCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target = null;
        Boolean requestedState = null;

        if (args.length == 0 || isState(args[0])) {
            if (!(sender instanceof Player)) {
                plugin.getMessageManager().send(sender, "staffmode.usage", "&cUsage: /staffmode [player] [on|off]");
                return true;
            }

            target = (Player) sender;
            if (args.length > 0) requestedState = parseState(args[0]);
        } else {
            if (!hasPermission(sender, "evaulx.staffmode.others")) {
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
                    plugin.getMessageManager().send(sender, "staffmode.usage", "&cUsage: /staffmode [player] [on|off]");
                    return true;
                }
            }
        }

        if (!target.equals(sender) && !hasPermission(sender, "evaulx.staffmode.others")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (target.equals(sender) && !hasPermission(sender, "evaulx.staffmode")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        PlayerProfile profile = plugin.getPlayerManager().getProfile(target);
        if (profile == null) {
            plugin.getMessageManager().send(sender, "profile-loading", "&cThat player's profile is still loading.");
            return true;
        }

        boolean enabled = requestedState != null ? requestedState : !profile.isStaffMode();
        Map<String, String> placeholders = placeholders(target, enabled);
        if (profile.isStaffMode() == enabled) {
            plugin.getMessageManager().send(sender, "staffmode.already",
                    "&7Staff mode is already {state} &7for &f{target}&7.", placeholders);
            return true;
        }

        setStaffMode(target, profile, enabled);
        plugin.getPlayerManager().saveProfile(profile);
        if (plugin.getRedisSyncManager() != null) plugin.getRedisSyncManager().publishStaffStatus(target);

        plugin.getMessageManager().send(target, enabled ? "staffmode.enabled" : "staffmode.disabled",
                enabled ? "&aStaff mode enabled&7. You are now in creative mode." : "&7Staff mode &cdisabled&7.",
                placeholders);
        plugin.getMessageManager().actionBar(target, enabled ? "action-bars.staffmode.enabled" : "action-bars.staffmode.disabled",
                enabled ? "&aStaff mode enabled &8| &fGamemode: &cCREATIVE" : "&cStaff mode disabled", placeholders);

        if (!target.equals(sender)) {
            plugin.getMessageManager().send(sender, "staffmode.set-other",
                    "&7Set staff mode for &f{target} &7to {state}&7.", placeholders);
        }

        plugin.getStaffRequestManager().logAction(sender.getName(), "STAFFMODE", target.getName(), enabled ? "enabled" : "disabled");
        return true;
    }

    private void setStaffMode(Player target, PlayerProfile profile, boolean enabled) {
        profile.setStaffMode(enabled);

        if (enabled) {
            plugin.getStaffRequestManager().applyStaffModeState(target);
            plugin.getStaffRequestManager().applyStaffModeItems(target);
            if (plugin.getConfig().getBoolean("staff-tools.staffmode.auto-vanish", true) && !profile.isVanished()) {
                new VanishCommand(plugin).setVanished(target, profile, true);
                plugin.getStaffRequestManager().markStaffModeAutoVanished(target);
            }
            return;
        }

        plugin.getStaffRequestManager().restoreStaffModeItems(target);
        plugin.getStaffRequestManager().restoreStaffModeState(target);
        if (plugin.getConfig().getBoolean("staff-tools.staffmode.disable-vanish-on-exit", false)
                && plugin.getStaffRequestManager().consumeStaffModeAutoVanished(target)
                && profile.isVanished()) {
            new VanishCommand(plugin).setVanished(target, profile, false);
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
                "{state}", stateText(state),
                "{gamemode}", "CREATIVE"
        );
    }
}
