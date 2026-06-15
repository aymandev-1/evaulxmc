package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExtinguishCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public ExtinguishCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.ext")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length > 0 && sender.hasPermission("evaulx.ext.others")) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }
            target.setFireTicks(0);
            sender.sendMessage(CC.color("&aExtinguished &f" + target.getName() + "&a."));
            if (!target.equals(sender)) target.sendMessage(CC.color("&aYou have been extinguished."));
            return true;
        }

        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this without a target.");
            return true;
        }

        Player player = (Player) sender; player.setFireTicks(0);
        player.sendMessage(CC.color("&aYou have been extinguished."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.ext.others")) return Collections.emptyList();
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}




