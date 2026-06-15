package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.Collections;
import java.util.List;

/**
 * Removes ground items and stray projectiles to relieve server lag.
 * Usage: /clearlag
 */
public class ClearLagCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public ClearLagCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.clearlag")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item || entity instanceof Projectile
                        || entity instanceof ExperienceOrb || entity instanceof FallingBlock) {
                    entity.remove();
                    removed++;
                }
            }
        }

        String senderName = sender instanceof Player ? sender.getName() : "Console";
        String formatted = CC.color("&8[&eClearLag&8] &7Removed &e" + removed + " &7entities to reduce lag. &8— &e" + senderName);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formatted);
            try { player.playSound(player.getLocation(), Sound.valueOf("CLICK"), 0.7f, 1.3f); } catch (Throwable ignored) {}
        }
        plugin.getServer().getConsoleSender().sendMessage(formatted);

        plugin.getStaffRequestManager().logAction(senderName, "CLEARLAG", "all", "Removed " + removed + " entities");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
