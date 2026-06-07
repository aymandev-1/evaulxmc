package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DelWarpCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public DelWarpCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.warp.admin")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /delwarp <name>"));
            return true;
        }
        if (plugin.getWarpManager().deleteWarp(args[0])) {
            sender.sendMessage(CC.color("&7Deleted warp &f" + args[0] + "&7."));
        } else {
            sender.sendMessage(CC.color("&cWarp &f" + args[0] + " &cnot found."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ENGLISH);
            for (String name : plugin.getWarpManager().getWarpNames()) {
                if (name.toLowerCase(Locale.ENGLISH).startsWith(prefix)) result.add(name);
            }
        }
        return result;
    }
}
