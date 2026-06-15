package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class MoreCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public MoreCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("evaulx.more")) {
            player.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        ItemStack held = player.getItemInHand();
        if (held == null || held.getType() == Material.AIR) {
            player.sendMessage(CC.color("&cHold an item to fill its stack."));
            return true;
        }

        int max = held.getMaxStackSize();
        if (held.getAmount() >= max) {
            player.sendMessage(CC.color("&7Stack is already full &8(&f" + max + "&8)."));
            return true;
        }

        held.setAmount(max);
        player.updateInventory();
        player.sendMessage(CC.color("&7Stack filled to &f" + max + "&7."));
        return true;
    }
}

