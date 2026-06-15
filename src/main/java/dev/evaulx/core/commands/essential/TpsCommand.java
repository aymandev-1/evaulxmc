package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;

public class TpsCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public TpsCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.tps")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        double[] tps = safeTps();
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1_048_576L;
        long maxMb  = rt.maxMemory() / 1_048_576L;
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000L;

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("  &aServer Performance"));
        sender.sendMessage(CC.color("  &7TPS  &81m&8: " + fmt(tps[0]) + "  &85m&8: " + fmt(tps[1]) + "  &815m&8: " + fmt(tps[2])));
        sender.sendMessage(CC.color("  &7RAM  &f" + usedMb + "MB &8/ &f" + maxMb + "MB"));
        sender.sendMessage(CC.color("  &7Uptime  &f" + formatUptime(uptime)));
        sender.sendMessage(CC.color("  &7Players &f" + Bukkit.getOnlinePlayers().size() + " &8/ &f" + Bukkit.getMaxPlayers()));
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private static double[] safeTps() {
        try {
            Object craftServer = Bukkit.getServer();
            Object minecraftServer = craftServer.getClass().getMethod("getServer").invoke(craftServer);
            return (double[]) minecraftServer.getClass().getField("recentTps").get(minecraftServer);
        } catch (Throwable ignored) { return new double[]{20.0, 20.0, 20.0}; }
    }

    private String fmt(double tps) {
        double capped = Math.min(tps, 20.0);
        String color = capped >= 18 ? "&a" : capped >= 15 ? "&e" : "&c";
        return color + String.format("%.2f", capped);
    }

    private String formatUptime(long seconds) {
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        return h + "h " + m + "m " + s + "s";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
