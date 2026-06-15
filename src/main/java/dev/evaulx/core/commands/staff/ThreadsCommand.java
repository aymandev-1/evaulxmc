package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Reports JVM thread statistics. Developer tool.
 * Usage: /threads
 */
public class ThreadsCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public ThreadsCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.dev.threads")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
        int total = all.size();
        int daemon = 0, runnable = 0;
        for (Thread thread : all.keySet()) {
            if (thread.isDaemon()) daemon++;
            if (thread.getState() == Thread.State.RUNNABLE) runnable++;
        }

        sender.sendMessage(CC.color("&8&m----------------------------------------"));
        sender.sendMessage(CC.color("  &9&lThread Report"));
        sender.sendMessage(CC.color("  &7Total threads: &f" + total));
        sender.sendMessage(CC.color("  &7Runnable: &a" + runnable));
        sender.sendMessage(CC.color("  &7Daemon: &e" + daemon));
        sender.sendMessage(CC.color("  &7Available processors: &b" + Runtime.getRuntime().availableProcessors()));
        sender.sendMessage(CC.color("&8&m----------------------------------------"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
