package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RepairCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public RepairCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("evaulx.repair")) {
            player.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        boolean all = args.length > 0 && args[0].equalsIgnoreCase("all");

        if (all) {
            if (!player.hasPermission("evaulx.repair.all")) {
                player.sendMessage(CC.color("&cYou don't have permission to repair all items."));
                return true;
            }
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (repair(item)) count++;
            }
            player.updateInventory();
            player.sendMessage(CC.color("&aRepaired &f" + count + " &aitem" + (count == 1 ? "" : "s") + "."));
            return true;
        }

        ItemStack held = player.getItemInHand();
        if (held == null || held.getType() == Material.AIR) {
            player.sendMessage(CC.color("&cHold an item to repair it."));
            return true;
        }
        if (!repair(held)) {
            player.sendMessage(CC.color("&cThat item cannot be repaired or is already at full durability."));
            return true;
        }
        player.updateInventory();
        player.sendMessage(CC.color("&aHeld item repaired."));
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean repair(ItemStack item) {
        if (item.getDurability() == 0) return false;
        item.setDurability((short) 0);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.repair")) return Collections.emptyList();
        if (args.length == 1 && sender.hasPermission("evaulx.repair.all")) return Collections.singletonList("all");
        return Collections.emptyList();
    }
}

