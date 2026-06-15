package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Teleports a builder straight up, placing a glass platform beneath them.
 * Usage: /up [blocks]
 */
public class UpCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public UpCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission("evaulx.up")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        int blocks = 1;
        if (args.length > 0) {
            try {
                blocks = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(CC.color("&cUsage: /up [blocks]"));
                return true;
            }
            if (blocks < 1 || blocks > 256) {
                player.sendMessage(CC.color("&cBlocks must be between 1 and 256."));
                return true;
            }
        }

        Location dest = player.getLocation().clone().add(0, blocks, 0);
        Block under = dest.clone().subtract(0, 1, 0).getBlock();
        if (under.getType() == Material.AIR) {
            under.setType(Material.GLASS);
        }
        player.teleport(dest);
        player.sendMessage(CC.color("&8[&aBuilder&8] &7Teleported up &a" + blocks + " &7blocks."));
        try { player.playSound(player.getLocation(), Sound.valueOf("ENDERMAN_TELEPORT"), 0.6f, 1.6f); } catch (Throwable ignored) {}
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
