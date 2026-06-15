package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class FriendMessageCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public FriendMessageCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("evaulx.friends")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /fm <player> <message>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(CC.color("&cThat player is not online."));
            return true;
        }
        if (!plugin.getFriendManager().areFriends(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(CC.color("&f" + target.getName() + " &7is not your friend."));
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) sb.append(" ");
            sb.append(args[i]);
        }
        String msg = sb.toString();
        String format = CC.color("&8[&6Friend&8] &f" + player.getName() + " &7→ &f" + target.getName() + " &8» &7" + msg);
        player.sendMessage(format);
        target.sendMessage(format);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("evaulx.friends")) {
            return Collections.emptyList();
        }
        Player player = (Player) sender;
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(player))
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
