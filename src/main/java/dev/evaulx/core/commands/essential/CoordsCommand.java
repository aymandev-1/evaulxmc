package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CoordsCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public CoordsCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (!sender.hasPermission("evaulx.coords")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Location loc = player.getLocation();
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
        String dir = getDirection(loc.getYaw());

        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &aYour Coordinates"));
        player.sendMessage(CC.color("  &7X &f" + (int) loc.getX() + "  &7Y &f" + (int) loc.getY() + "  &7Z &f" + (int) loc.getZ()));
        player.sendMessage(CC.color("  &7World &f" + world + "  &7Facing &f" + dir));
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private String getDirection(float yaw) {
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw < 22.5 || yaw >= 337.5) return "South";
        if (yaw < 67.5)  return "Southwest";
        if (yaw < 112.5) return "West";
        if (yaw < 157.5) return "Northwest";
        if (yaw < 202.5) return "North";
        if (yaw < 247.5) return "Northeast";
        if (yaw < 292.5) return "East";
        return "Southeast";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
