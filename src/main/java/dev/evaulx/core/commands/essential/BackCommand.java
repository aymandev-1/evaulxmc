package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class BackCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public BackCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!sender.hasPermission("evaulx.back")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        Player player = (Player) sender;
        Location back = plugin.getEssentialsManager().getBackLocation(player);
        if (back == null || back.getWorld() == null) {
            sender.sendMessage(CC.color("&cNo back location saved."));
            return true;
        }

        Location current = player.getLocation();
        player.teleport(back);
        plugin.getEssentialsManager().setBackLocation(player, current);
        sender.sendMessage(CC.color("&7Teleported to your previous location."));
        return true;
    }
}
