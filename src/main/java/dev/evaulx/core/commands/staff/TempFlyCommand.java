package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class TempFlyCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public TempFlyCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.tempfly")) {
            sender.sendMessage(CC.color("&cYou don't have permission to do that."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /tempfly <player> <duration>"));
            sender.sendMessage(CC.color("&7Duration examples: &f30s&7, &f5m&7, &f1h&7, &f2d"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer &e" + args[0] + " &cis not online."));
            return true;
        }

        long durationMs = parseDuration(args[1]);
        if (durationMs <= 0) {
            sender.sendMessage(CC.color("&cInvalid duration. Use &e30s&c, &e5m&c, &e1h&c, or &e2d&c."));
            return true;
        }

        plugin.getEssentialsManager().grantTempFly(target, durationMs);

        String formatted = formatDuration(durationMs);
        target.sendMessage(CC.color("&a✔ You have been granted temporary fly for &e" + formatted
                + "&a by &f" + sender.getName() + "&a."));
        sender.sendMessage(CC.color("&a✔ Granted temp fly to &e" + target.getName()
                + " &afor &e" + formatted + "&a."));
        return true;
    }

    private long parseDuration(String input) {
        if (input == null || input.trim().isEmpty()) return -1;
        input = input.toLowerCase(Locale.ENGLISH).trim();
        try {
            char unit = input.charAt(input.length() - 1);
            long value = Long.parseLong(input.substring(0, input.length() - 1));
            if (value <= 0) return -1;
            switch (unit) {
                case 's': return value * 1000L;
                case 'm': return value * 60_000L;
                case 'h': return value * 3_600_000L;
                case 'd': return value * 86_400_000L;
                default: return -1L;
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return -1;
        }
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m";
        if (seconds < 86400) return (seconds / 3600) + "h";
        return (seconds / 86400) + "d";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ENGLISH).startsWith(args[0].toLowerCase(Locale.ENGLISH)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) return Arrays.asList("30s", "5m", "1h", "6h", "1d");
        return Collections.emptyList();
    }
}
