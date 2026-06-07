package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SetWarpCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public SetWarpCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!sender.hasPermission("evaulx.warp.admin")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /setwarp <name>"));
            return true;
        }

        Player player = (Player) sender;
        String name = args[0];
        if (!name.matches("[A-Za-z0-9_-]{1,32}")) {
            sender.sendMessage(CC.color("&cWarp names must be 1-32 letters, numbers, _ or -."));
            return true;
        }

        boolean existed = plugin.getWarpManager().exists(name);
        if (plugin.getWarpManager().setWarp(name, player.getLocation())) {
            sender.sendMessage(CC.color((existed ? "&7Updated warp &f" : "&7Created warp &f") + name + " &7at your location."));
        } else {
            sender.sendMessage(CC.color("&cCould not set that warp."));
        }
        return true;
    }
}
