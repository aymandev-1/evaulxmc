package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HatCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public HatCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("evaulx.hat")) {
            player.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        ItemStack held = player.getItemInHand();
        if (held == null || held.getType() == org.bukkit.Material.AIR) {
            player.sendMessage(CC.color("&cHold an item to wear it as a hat."));
            return true;
        }

        ItemStack currentHelmet = player.getInventory().getHelmet();
        player.getInventory().setHelmet(held.clone());
        player.setItemInHand(currentHelmet != null ? currentHelmet : new ItemStack(org.bukkit.Material.AIR));
        player.updateInventory();
        player.sendMessage(CC.color("&7Hat equipped."));
        return true;
    }
}

