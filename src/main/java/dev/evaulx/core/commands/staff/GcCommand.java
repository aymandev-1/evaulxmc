package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Runs garbage collection and reports JVM memory usage. Developer tool.
 * Usage: /gc
 */
public class GcCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public GcCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.dev.gc")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Runtime runtime = Runtime.getRuntime();
        long mb = 1024L * 1024L;
        long usedBefore = (runtime.totalMemory() - runtime.freeMemory()) / mb;
        System.gc();
        long usedAfter = (runtime.totalMemory() - runtime.freeMemory()) / mb;
        long max = runtime.maxMemory() / mb;
        long freed = usedBefore - usedAfter;

        sender.sendMessage(CC.color("&8&m----------------------------------------"));
        sender.sendMessage(CC.color("  &9&lGarbage Collection"));
        sender.sendMessage(CC.color("  &7Before: &f" + usedBefore + " MB"));
        sender.sendMessage(CC.color("  &7After:  &f" + usedAfter + " MB"));
        sender.sendMessage(CC.color("  &7Freed:  &a" + (freed < 0 ? 0 : freed) + " MB"));
        sender.sendMessage(CC.color("  &7Max heap: &b" + max + " MB"));
        sender.sendMessage(CC.color("&8&m----------------------------------------"));

        String senderName = sender instanceof Player ? sender.getName() : "Console";
        plugin.getStaffRequestManager().logAction(senderName, "GC", "server", "Freed " + freed + " MB");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
