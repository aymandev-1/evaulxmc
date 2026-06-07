package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.staff.StaffRequestManager.StaffSession;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class StaffSessionsCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public StaffSessionsCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.staffsessions")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0 && sender instanceof Player) {
            plugin.getGuiManager().openStaffSessions((Player) sender);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("player")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /staffsessions player <name>"));
                return true;
            }
            long total = plugin.getStaffRequestManager().getTotalStaffSessionTime(args[1]);
            sender.sendMessage(CC.color("&7Total tracked staff time for &f" + args[1] + "&7: &f" + format(total)));
            return true;
        }

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cStaff Sessions"));
        sender.sendMessage(CC.color("&7Active: &f" + plugin.getStaffRequestManager().getActiveStaffSessionCount()));
        for (StaffSession session : plugin.getStaffRequestManager().getActiveStaffSessions()) {
            sender.sendMessage(CC.color("&aOnline &f" + session.getName() + " &7for &f" + session.getDurationString()));
        }
        for (StaffSession session : plugin.getStaffRequestManager().getRecentStaffSessions(8)) {
            sender.sendMessage(CC.color("&8" + session.getStartedFormatted() + " &f" + session.getName()
                    + " &7worked &f" + session.getDurationString()));
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private String format(long millis) {
        long seconds = millis / 1000L;
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        if (hours > 0L) return hours + "h " + minutes + "m";
        if (minutes > 0L) return minutes + "m " + secs + "s";
        return secs + "s";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.staffsessions")) return Collections.emptyList();
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            Collections.addAll(suggestions, "list", "player");
            return filter(suggestions, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            suggestions.addAll(plugin.getPlayerLookupManager().suggest(args[1], 20));
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) suggestions.add(player.getName());
            return filter(suggestions, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ENGLISH);
        List<String> filtered = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ENGLISH).startsWith(lower) && !filtered.contains(value)) filtered.add(value);
        }
        Collections.sort(filtered, String.CASE_INSENSITIVE_ORDER);
        return filtered;
    }
}
