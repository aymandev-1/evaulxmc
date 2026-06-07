package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffRecoverCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public StaffRecoverCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.staffrecover")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.getMessageManager().send(sender, "player-not-online", "&cPlayer not online.");
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(CC.color("&cUsage: /staffrecover <player>"));
            return true;
        }

        if (!plugin.getStaffRequestManager().hasStaffModeRecovery(target)) {
            plugin.getMessageManager().send(sender, "staffrecover.no-snapshot",
                    "&cNo staff mode recovery snapshot exists for &f{target}&c.",
                    plugin.getMessageManager().placeholders("{target}", target.getName()));
            return true;
        }

        boolean restored = plugin.getStaffRequestManager().recoverStaffMode(target);
        if (!restored) {
            plugin.getMessageManager().send(sender, "staffrecover.failed",
                    "&cCould not restore staff mode snapshot for &f{target}&c.",
                    plugin.getMessageManager().placeholders("{target}", target.getName()));
            return true;
        }

        PlayerProfile profile = plugin.getPlayerManager().getProfile(target);
        if (profile != null) {
            profile.setStaffMode(false);
            plugin.getPlayerManager().saveProfile(profile);
        }

        plugin.getMessageManager().send(sender, "staffrecover.restored",
                "&7Recovered staff mode inventory/state for &f{target}&7.",
                plugin.getMessageManager().placeholders("{target}", target.getName()));
        if (!target.equals(sender)) {
            plugin.getMessageManager().send(target, "staffrecover.restored-target",
                    "&7Your staff mode inventory/state was recovered by &f{staff}&7.",
                    plugin.getMessageManager().placeholders("{staff}", sender.getName()));
        }
        plugin.getStaffRequestManager().logAction(sender.getName(), "STAFF_RECOVER", target.getName(), "Recovered staff mode snapshot");
        return true;
    }
}
