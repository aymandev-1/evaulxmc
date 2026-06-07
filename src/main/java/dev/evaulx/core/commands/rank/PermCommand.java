package dev.evaulx.core.commands.rank;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PermCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public PermCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.perm")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("debug")) {
            return debug(sender, args);
        }

        if (args.length < 3) {
            sender.sendMessage(CC.color("&cUsage: /perm <player> <add|remove> <permission>"));
            sender.sendMessage(CC.color("&cUsage: /perm debug <player> <permission>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer must be online."));
            return true;
        }

        PlayerProfile profile = plugin.getPlayerManager().getProfile(target);
        if (profile == null) {
            sender.sendMessage(CC.color("&cProfile not loaded."));
            return true;
        }

        if (args[1].equalsIgnoreCase("add")) {
            profile.addPermission(args[2]);
            sender.sendMessage(CC.color("&aAdded &f" + args[2] + " &ato &f" + target.getName()));
        } else if (args[1].equalsIgnoreCase("remove")) {
            profile.removePermission(args[2]);
            sender.sendMessage(CC.color("&aRemoved &f" + args[2] + " &afrom &f" + target.getName()));
        } else {
            sender.sendMessage(CC.color("&cUse add or remove."));
            return true;
        }

        plugin.getPlayerManager().applyPermissions(target, profile);
        plugin.getPlayerManager().saveProfile(profile);
        plugin.getStaffRequestManager().logAction(sender.getName(), "PERMISSION_" + args[1].toUpperCase(Locale.ENGLISH),
                target.getName(), args[2]);
        return true;
    }

    private boolean debug(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(CC.color("&cUsage: /perm debug <player> <permission>"));
            return true;
        }

        OfflinePlayer target = plugin.getPlayerLookupManager().find(args[1]);
        Player online = Bukkit.getPlayer(args[1]);
        if (target == null && online != null) target = online;
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        String permission = args[2];
        PlayerProfile profile = target.isOnline()
                ? plugin.getPlayerManager().getProfile(target.getPlayer())
                : plugin.getDatabaseManager().loadProfile(target.getUniqueId(), target.getName() == null ? args[1] : target.getName());
        if (profile == null) {
            sender.sendMessage(CC.color("&cProfile not loaded."));
            return true;
        }

        List<String> sources = new ArrayList<>();
        boolean result = false;
        if (target.isOp()) {
            sources.add("Bukkit op");
            result = true;
        }
        if (online != null && online.hasPermission(permission)) {
            sources.add("Bukkit effective permission");
            result = true;
        }
        if (matches(profile.getPermissions(), permission)) {
            sources.add("direct profile permission");
            result = true;
        }

        Rank primary = plugin.getRankManager().getRank(profile.getRankName());
        if (rankMatches(primary, permission)) {
            sources.add("primary rank " + primary.getName());
            result = true;
        }

        for (String extraRank : profile.getExtraRanks()) {
            Rank rank = plugin.getRankManager().getRank(extraRank);
            if (!rankMatches(rank, permission)) continue;
            sources.add("extra rank " + rank.getName());
            result = true;
        }

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cPermission Debug"));
        sender.sendMessage(CC.color("&7Player: &f" + (target.getName() == null ? args[1] : target.getName())));
        sender.sendMessage(CC.color("&7Permission: &f" + permission));
        sender.sendMessage(CC.color("&7Result: " + (result ? "&aTRUE" : "&cFALSE")));
        sender.sendMessage(CC.color("&7Primary rank: &f" + profile.getRankName()));
        sender.sendMessage(CC.color("&7Extra ranks: &f" + (profile.getExtraRanks().isEmpty() ? "none" : join(profile.getExtraRanks()))));
        if (sources.isEmpty()) {
            sender.sendMessage(CC.color("&7Sources: &fnone"));
        } else {
            sender.sendMessage(CC.color("&7Sources:"));
            for (String source : sources) sender.sendMessage(CC.color("  &8- &f" + source));
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean rankMatches(Rank rank, String permission) {
        return rank != null && matches(plugin.getRankManager().getAllPermissions(rank), permission);
    }

    private boolean matches(List<String> permissions, String permission) {
        String lowered = permission.toLowerCase(Locale.ENGLISH);
        for (String current : permissions) {
            if (current == null) continue;
            String clean = current.toLowerCase(Locale.ENGLISH);
            if (clean.equals("*") || clean.equals(lowered)) return true;
            if (clean.endsWith(".*") && lowered.startsWith(clean.substring(0, clean.length() - 1))) return true;
        }
        return false;
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(value);
        }
        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.perm")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> values = new ArrayList<>();
            values.add("debug");
            for (Player player : Bukkit.getOnlinePlayers()) values.add(player.getName());
            return filter(values, args[0]);
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("debug")) {
            return filter(Arrays.asList("add", "remove"), args[1]);
        }
        if (args.length == 2) {
            List<String> values = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) values.add(player.getName());
            values.addAll(plugin.getPlayerLookupManager().suggest(args[1], 20));
            return filter(values, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ENGLISH);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ENGLISH).startsWith(lower) && !matches.contains(value)) {
                matches.add(value);
            }
        }
        Collections.sort(matches, String.CASE_INSENSITIVE_ORDER);
        return matches;
    }
}
