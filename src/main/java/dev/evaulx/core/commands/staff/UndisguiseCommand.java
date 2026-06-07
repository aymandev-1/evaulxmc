package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class UndisguiseCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public UndisguiseCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (!sender.hasPermission("evaulx.disguise.others")) {
                plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
                return true;
            }

            Player target = plugin.getDisguiseManager().findOnlinePlayer(args[0]);
            if (target == null) {
                plugin.getMessageManager().send(sender, "player-not-online", "&cPlayer not online.");
                return true;
            }

            if (!plugin.getDisguiseManager().undisguise(target)) {
                plugin.getMessageManager().send(sender, "disguise.force-not-disguised", "&cThat player is not disguised.");
                return true;
            }

            plugin.getMessageManager().send(sender, "disguise.forced-undisguise",
                    "&7Removed &f{target}&7's disguise.",
                    plugin.getMessageManager().placeholders("{target}", target.getName()));
            plugin.getStaffRequestManager().logAction(sender.getName(), "FORCE_UNDISGUISE", target.getName(), "Removed disguise");
            return true;
        }

        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }

        if (!sender.hasPermission("evaulx.disguise") && !sender.hasPermission("evaulx.nick")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (!plugin.getDisguiseManager().undisguise((Player) sender)) {
            plugin.getMessageManager().send(sender, "disguise.not-disguised", "&cYou are not disguised.");
        }
        return true;
    }
}
