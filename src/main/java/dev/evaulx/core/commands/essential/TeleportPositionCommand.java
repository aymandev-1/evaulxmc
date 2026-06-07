package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TeleportPositionCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public TeleportPositionCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.tppos")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(CC.color("&cUsage: /tppos [player] <x> <y> <z> [world] [yaw] [pitch]"));
            return true;
        }

        int offset = 0;
        Player target;
        if (args.length >= 4 && Bukkit.getPlayer(args[0]) != null) {
            if (!sender.hasPermission("evaulx.tppos.others")) {
                sender.sendMessage(CC.color("&cNo permission."));
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            offset = 1;
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(CC.color("&cUsage from console: /tppos <player> <x> <y> <z> [world] [yaw] [pitch]"));
            return true;
        }

        if (args.length < offset + 3) {
            sender.sendMessage(CC.color("&cUsage: /tppos [player] <x> <y> <z> [world] [yaw] [pitch]"));
            return true;
        }

        Double x = parseDouble(args[offset]);
        Double y = parseDouble(args[offset + 1]);
        Double z = parseDouble(args[offset + 2]);
        if (x == null || y == null || z == null) {
            sender.sendMessage(CC.color("&cCoordinates must be numbers."));
            return true;
        }

        World world = target.getWorld();
        if (args.length > offset + 3) {
            world = Bukkit.getWorld(args[offset + 3]);
            if (world == null) {
                sender.sendMessage(CC.color("&cWorld not found."));
                return true;
            }
        }

        float yaw = target.getLocation().getYaw();
        float pitch = target.getLocation().getPitch();
        if (args.length > offset + 4) {
            Double parsedYaw = parseDouble(args[offset + 4]);
            if (parsedYaw == null) {
                sender.sendMessage(CC.color("&cYaw must be a number."));
                return true;
            }
            yaw = parsedYaw.floatValue();
        }
        if (args.length > offset + 5) {
            Double parsedPitch = parseDouble(args[offset + 5]);
            if (parsedPitch == null) {
                sender.sendMessage(CC.color("&cPitch must be a number."));
                return true;
            }
            pitch = parsedPitch.floatValue();
        }

        plugin.getEssentialsManager().setBackLocation(target, target.getLocation());
        target.teleport(new Location(world, x, y, z, yaw, pitch));
        plugin.getStaffRequestManager().logAction(sender.getName(), "TELEPORT_POS", target.getName(),
                world.getName() + " " + x + " " + y + " " + z);
        target.sendMessage(CC.color("&7Teleported to &f" + x + ", " + y + ", " + z + "&7."));
        if (!target.equals(sender)) {
            sender.sendMessage(CC.color("&7Teleported &f" + target.getName() + " &7to coordinates."));
        }
        return true;
    }

    private Double parseDouble(String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
