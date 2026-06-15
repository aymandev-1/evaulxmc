package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;

public class ServerInfoCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public ServerInfoCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.serverinfo")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        double[] tps = safeTps();
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1_048_576L;
        long maxMb  = rt.maxMemory() / 1_048_576L;
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000L;
        String tpsColor = tps[0] >= 18 ? "&a" : tps[0] >= 15 ? "&e" : "&c";

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("  &aServer Information"));
        sender.sendMessage(CC.color("  &7Software  &f" + Bukkit.getName()));
        sender.sendMessage(CC.color("  &7Version   &f" + Bukkit.getVersion()));
        sender.sendMessage(CC.color("  &7API       &f" + Bukkit.getBukkitVersion()));
        sender.sendMessage(CC.color("  &7Players   &f" + Bukkit.getOnlinePlayers().size() + " &8/ &f" + Bukkit.getMaxPlayers()));
        sender.sendMessage(CC.color("  &7Worlds    &f" + Bukkit.getWorlds().size()));
        sender.sendMessage(CC.color("  &7TPS (1m)  " + tpsColor + String.format("%.2f", Math.min(tps[0], 20.0))));
        sender.sendMessage(CC.color("  &7RAM       &f" + usedMb + "MB &8/ &f" + maxMb + "MB"));
        sender.sendMessage(CC.color("  &7Uptime    &f" + formatUptime(uptime)));
        sender.sendMessage(CC.color("  &7Java      &f" + System.getProperty("java.version")));
        sender.sendMessage(CC.color("  &7OS        &f" + System.getProperty("os.name")));
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

    private String formatUptime(long seconds) {
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        return h + "h " + m + "m " + s + "s";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
