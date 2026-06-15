package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class TimeCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public TimeCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.time")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        World world = sender instanceof Player ? ((Player) sender).getWorld() : null;
        if (world == null && args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /time <day|night|noon|midnight|set <ticks>>"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&7World time: &f" + world.getTime() + " &8(" + describeTick(world.getTime()) + "&8)"));
            return true;
        }

        long ticks;
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "day":    ticks = 1000;  break;
            case "noon":   ticks = 6000;  break;
            case "sunset": ticks = 12000; break;
            case "night":  ticks = 13000; break;
            case "midnight": ticks = 18000; break;
            case "sunrise": ticks = 23000; break;
            case "set":
                if (args.length < 2) { sender.sendMessage(CC.color("&cUsage: /time set <ticks>")); return true; }
                try { ticks = Long.parseLong(args[1]); } catch (NumberFormatException e) { sender.sendMessage(CC.color("&cInvalid ticks value.")); return true; }
                break;
            case "add":
                if (args.length < 2 || world == null) { sender.sendMessage(CC.color("&cUsage: /time add <ticks>")); return true; }
                try { ticks = world.getTime() + Long.parseLong(args[1]); } catch (NumberFormatException e) { sender.sendMessage(CC.color("&cInvalid ticks value.")); return true; }
                break;
            default:
                sender.sendMessage(CC.color("&cUsage: /time <day|noon|night|midnight|sunrise|sunset|set <ticks>|add <ticks>>"));
                return true;
        }

        if (world == null) { sender.sendMessage(CC.color("&cThis command can only be run by a player.")); return true; }
        world.setTime(ticks);
        sender.sendMessage(CC.color("&7Time set to &f" + ticks + " &8(" + describeTick(ticks) + "&8)&7."));
        return true;
    }

    private String describeTick(long t) {
        t = t % 24000;
        if (t < 1000) return "&6sunrise";
        if (t < 6000) return "&eday";
        if (t < 12000) return "&enoon";
        if (t < 13000) return "&6sunset";
        if (t < 18000) return "&9night";
        return "&9midnight";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.time")) return Collections.emptyList();
        if (args.length == 1) return Arrays.asList("day", "noon", "night", "midnight", "sunrise", "sunset", "set", "add");
        return Collections.emptyList();
    }
}
