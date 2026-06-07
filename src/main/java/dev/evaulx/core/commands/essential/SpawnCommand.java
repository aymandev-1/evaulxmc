package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public SpawnCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!sender.hasPermission("evaulx.spawn")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        Player player = (Player) sender;
        Location spawn = plugin.getEssentialsManager().getSpawn();
        if (spawn == null) {
            World world = player.getWorld();
            spawn = world.getSpawnLocation();
        }

        plugin.getEssentialsManager().setBackLocation(player, player.getLocation());
        player.teleport(spawn);
        player.sendMessage(CC.color("&7Teleported to spawn."));
        return true;
    }
}
