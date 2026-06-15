package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SmiteCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public SmiteCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.smite")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }
        if (args.length == 0) { sender.sendMessage(CC.color("&cUsage: /smite <player>")); return true; }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }

        target.getWorld().strikeLightning(target.getLocation());
        sender.sendMessage(CC.color("&aSmitten &f" + target.getName() + "&a."));
        plugin.getStaffRequestManager().logAction(
                sender instanceof Player ? ((Player) sender).getName() : "Console", "SMITE", target.getName(), "Lightning strike");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.smite")) return Collections.emptyList();
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}






