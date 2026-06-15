package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;

/**
 * Store-rank perk: launches the player into the air.
 * Usage: /launch [power]
 */
public class LaunchCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public LaunchCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission("evaulx.store.launch")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        double power = 1.5;
        if (args.length > 0) {
            try {
                power = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(CC.color("&cUsage: /launch [power]"));
                return true;
            }
            if (power < 0.5 || power > 5.0) {
                player.sendMessage(CC.color("&cPower must be between 0.5 and 5.0."));
                return true;
            }
        }

        player.setVelocity(new Vector(0, power, 0));
        player.setFallDistance(0f);
        player.sendMessage(CC.color("&8[&6Perks&8] &7Wheee! &e🚀"));
        try { player.playSound(player.getLocation(), Sound.valueOf("FIREWORK_LAUNCH"), 0.8f, 1.0f); } catch (Throwable ignored) {}
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
