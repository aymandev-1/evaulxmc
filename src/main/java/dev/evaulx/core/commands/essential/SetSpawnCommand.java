package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public SetSpawnCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!sender.hasPermission("evaulx.setspawn")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        Player player = (Player) sender;
        plugin.getEssentialsManager().setSpawn(player.getLocation());
        sender.sendMessage(CC.color("&7Spawn set to your current location."));
        return true;
    }
}
