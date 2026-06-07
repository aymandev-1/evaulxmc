package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ProfileCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public ProfileCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.profile")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cPlayers only."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /profile <player>"));
            return true;
        }

        OfflinePlayer target = plugin.getPlayerLookupManager().find(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }
        plugin.getGuiManager().openPlayerProfile((Player) sender, target);
        return true;
    }
}
