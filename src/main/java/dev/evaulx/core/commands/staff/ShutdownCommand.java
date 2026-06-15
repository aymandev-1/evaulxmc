package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Schedules a graceful server shutdown with countdown broadcasts. Owner tool.
 * Usage: /shutdown <seconds|cancel> [reason]
 */
public class ShutdownCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;
    private BukkitRunnable task;
    private int remaining;
    private String reason;

    public ShutdownCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.owner.shutdown")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /shutdown <seconds|cancel> [reason]"));
            return true;
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            if (task == null) {
                sender.sendMessage(CC.color("&cNo shutdown is currently scheduled."));
                return true;
            }
            task.cancel();
            task = null;
            Bukkit.broadcastMessage(CC.color("&8[&4Server&8] &aScheduled shutdown cancelled."));
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(CC.color("&cUsage: /shutdown <seconds|cancel> [reason]"));
            return true;
        }
        if (seconds < 1 || seconds > 3600) {
            sender.sendMessage(CC.color("&cSeconds must be between 1 and 3600."));
            return true;
        }
        if (task != null) {
            sender.sendMessage(CC.color("&cA shutdown is already scheduled. Use /shutdown cancel first."));
            return true;
        }

        this.reason = args.length > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : "Scheduled maintenance";
        this.remaining = seconds;

        String senderName = sender instanceof Player ? sender.getName() : "Console";
        plugin.getStaffRequestManager().logAction(senderName, "SHUTDOWN", seconds + "s", reason);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (remaining <= 0) {
                    Bukkit.broadcastMessage(CC.color("&8[&4Server&8] &cShutting down now. &7(" + reason + ")"));
                    cancel();
                    task = null;
                    Bukkit.shutdown();
                    return;
                }
                if (remaining <= 5 || remaining % 15 == 0 || remaining == 30) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(CC.color("&8[&4Server&8] &cShutdown in &e" + remaining + "s&c. &7(" + reason + ")"));
                        try { player.playSound(player.getLocation(), Sound.valueOf("NOTE_PLING"), 0.6f, 1.2f); } catch (Throwable ignored) {}
                    }
                }
                remaining--;
            }
        };
        task.runTaskTimer(plugin, 0L, 20L);
        sender.sendMessage(CC.color("&8[&4Server&8] &7Shutdown scheduled in &e" + seconds + "s&7."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.Arrays.asList("cancel", "10", "30", "60");
        }
        return Collections.emptyList();
    }
}
