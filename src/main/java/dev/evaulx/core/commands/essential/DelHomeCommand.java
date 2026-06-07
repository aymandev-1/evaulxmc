package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DelHomeCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public DelHomeCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!sender.hasPermission("evaulx.home")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /delhome <name>"));
            return true;
        }

        Player player = (Player) sender;
        if (plugin.getHomesManager().deleteHome(player.getUniqueId(), args[0])) {
            player.sendMessage(CC.color("&7Deleted home &f" + args[0] + "&7."));
        } else {
            player.sendMessage(CC.color("&cHome &f" + args[0] + " &cnot found."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1 && sender instanceof Player) {
            String prefix = args[0].toLowerCase(Locale.ENGLISH);
            for (String name : plugin.getHomesManager().getHomeNames(((Player) sender).getUniqueId())) {
                if (name.toLowerCase(Locale.ENGLISH).startsWith(prefix)) result.add(name);
            }
        }
        return result;
    }
}
