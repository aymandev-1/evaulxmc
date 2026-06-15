package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class EnderChestCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public EnderChestCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            if (!player.hasPermission("evaulx.ec")) {
                player.sendMessage(CC.color("&cYou don't have permission to use this command."));
                return true;
            }
            player.openInventory(player.getEnderChest());
            return true;
        }

        if (!player.hasPermission("evaulx.ec.others")) {
            player.sendMessage(CC.color("&cYou don't have permission to view other players' ender chests."));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(CC.color("&cPlayer &e" + args[0] + " &cis not online."));
            return true;
        }

        player.openInventory(target.getEnderChest());
        player.sendMessage(CC.color("&7Viewing &e" + target.getName() + "&7's ender chest."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("evaulx.ec.others")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ENGLISH).startsWith(args[0].toLowerCase(Locale.ENGLISH)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}






