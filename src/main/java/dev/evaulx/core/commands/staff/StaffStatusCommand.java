package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class StaffStatusCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public StaffStatusCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission("evaulx.staffstatus")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null) {
            plugin.getMessageManager().send(sender, "profile-loading", "&cThat player's profile is still loading.");
            return true;
        }

        String server = plugin.getConfig().getString("server.server-id", "hub");
        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("&cStaff Status &8@ &f" + server));
        player.sendMessage(CC.color("&7Staff Mode: " + state(profile.isStaffMode())));
        player.sendMessage(CC.color("&7Vanish: " + state(profile.isVanished())));
        player.sendMessage(CC.color("&7Disguised: " + state(profile.isDisguised())
                + (profile.isDisguised() ? " &8(&f" + profile.getDisguiseName() + "&8)" : "")));
        player.sendMessage(CC.color("&7God: " + state(profile.isGodMode())));
        player.sendMessage(CC.color("&7SocialSpy: " + state(profile.isSocialSpy())));
        player.sendMessage(CC.color("&7CommandSpy: " + state(plugin.getStaffRequestManager().isCommandSpyEnabled(player))));
        player.sendMessage(CC.color("&7Frozen Players: &f" + plugin.getStaffRequestManager().getFrozenCount()));
        if (plugin.getRedisSyncManager() != null && plugin.getRedisSyncManager().isEnabled()) {
            player.sendMessage(CC.color("&7Remote Staff: &f" + plugin.getRedisSyncManager().getRemoteStaff().size()));
        }
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private String state(boolean enabled) {
        return enabled ? "&aON" : "&cOFF";
    }
}
