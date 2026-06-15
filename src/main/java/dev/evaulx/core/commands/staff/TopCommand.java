package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Teleports a builder to the highest block at their current X/Z.
 * Usage: /top
 */
public class TopCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public TopCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission("evaulx.top")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();
        int y = player.getWorld().getHighestBlockYAt(loc);
        Location dest = new Location(player.getWorld(), loc.getX(), y + 1, loc.getZ(), loc.getYaw(), loc.getPitch());
        player.teleport(dest);
        player.sendMessage(CC.color("&8[&aBuilder&8] &7Teleported to the &atop &7block (&fy=" + (y + 1) + "&7)."));
        try { player.playSound(player.getLocation(), Sound.valueOf("ENDERMAN_TELEPORT"), 0.6f, 1.4f); } catch (Throwable ignored) {}
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
