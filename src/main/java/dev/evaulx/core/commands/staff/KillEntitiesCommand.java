package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Removes entities by category across all worlds.
 * Usage: /killentities <monsters|animals|items|all>
 */
public class KillEntitiesCommand implements CommandExecutor, TabCompleter {

    private static final List<String> MODES = Arrays.asList("monsters", "animals", "items", "all");

    private final EvaulxCore plugin;

    public KillEntitiesCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.admin.killentities")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /killentities <monsters|animals|items|all>"));
            return true;
        }

        String mode = args[0].toLowerCase(Locale.ENGLISH);
        if (!MODES.contains(mode)) {
            sender.sendMessage(CC.color("&cUnknown type. Use: monsters, animals, items, all"));
            return true;
        }

        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Player) continue;
                boolean match;
                switch (mode) {
                    case "monsters": match = entity instanceof Monster; break;
                    case "animals":  match = entity instanceof Animals; break;
                    case "items":    match = entity instanceof Item;    break;
                    default:         match = !(entity instanceof Player); break; // all
                }
                if (match) {
                    entity.remove();
                    removed++;
                }
            }
        }

        sender.sendMessage(CC.color("&8[&cKillEntities&8] &7Removed &e" + removed + " &7" + mode + "."));
        String senderName = sender instanceof Player ? sender.getName() : "Console";
        plugin.getStaffRequestManager().logAction(senderName, "KILLENTITIES", mode, "Removed " + removed);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return MODES.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ENGLISH))).collect(Collectors.toList());
        }
        return java.util.Collections.emptyList();
    }
}
