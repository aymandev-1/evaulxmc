package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class ClearInventoryCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public ClearInventoryCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.clear")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(CC.color("&cUsage: /clearinv <player>"));
                return true;
            }
            Player player = (Player) sender;
            clear(player);
            player.sendMessage(CC.color("&7Your inventory has been cleared."));
            return true;
        }

        if (!sender.hasPermission("evaulx.clear.others")) {
            sender.sendMessage(CC.color("&cNo permission to clear other players' inventories."));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer &f" + args[0] + " &cis not online."));
            return true;
        }

        clear(target);
        sender.sendMessage(CC.color("&7Cleared &f" + target.getName() + "&7's inventory."));
        target.sendMessage(CC.color("&7Your inventory was cleared by &f" + sender.getName() + "&7."));
        return true;
    }

    private void clear(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        try {
            Class<?> is = Class.forName("org.bukkit.inventory.ItemStack");
            player.getInventory().getClass().getMethod("setItemInOffHand", is).invoke(player.getInventory(), (Object) null);
        } catch (Throwable ignored) {}
        player.updateInventory();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.clear.others")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        return Collections.emptyList();
    }
}
