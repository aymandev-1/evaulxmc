package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlaytimeCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public PlaytimeCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.playtime")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        OfflinePlayer target;
        if (args.length > 0) {
            target = plugin.getPlayerLookupManager().find(args[0]);
            if (target == null) {
                sender.sendMessage(CC.color("&cPlayer not found."));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(CC.color("&cUsage: /playtime <player>"));
            return true;
        }

        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();

        PlayerProfile profile = null;
        Player online = target.getPlayer();
        if (online != null) {
            profile = plugin.getPlayerManager().getProfile(online);
        }
        if (profile == null) {
            profile = plugin.getDatabaseManager().loadProfile(target.getUniqueId(), targetName);
        }

        if (profile == null || profile.getFirstJoin() == 0) {
            sender.sendMessage(CC.color("&cNo data found for &f" + targetName + "&c."));
            return true;
        }

        long now = System.currentTimeMillis();
        String firstJoinDate = new SimpleDateFormat("dd MMM yyyy").format(new Date(profile.getFirstJoin()));
        String totalTime = formatElapsed(now - profile.getFirstJoin());
        String lastSeen = target.isOnline()
                ? "&aOnline now"
                : "&7" + formatElapsed(now - profile.getLastSeen()) + " ago";

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cPlaytime &8| &f" + targetName));
        sender.sendMessage(CC.color("&7First joined: &f" + firstJoinDate + " &8(" + totalTime + " ago)"));
        sender.sendMessage(CC.color("&7Last seen: " + lastSeen));
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private String formatElapsed(long ms) {
        long s = ms / 1000;
        long m = s / 60;
        long h = m / 60;
        long d = h / 24;
        if (d > 365) return (d / 365) + "y " + ((d % 365) / 30) + "mo";
        if (d > 30)  return (d / 30) + "mo " + (d % 30) + "d";
        if (d > 0)   return d + "d " + (h % 24) + "h";
        if (h > 0)   return h + "h " + (m % 60) + "m";
        if (m > 0)   return m + "m";
        return s + "s";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.playtime")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) suggestions.add(p.getName());
            suggestions.addAll(plugin.getPlayerLookupManager().suggest(args[0], 10));
            return filter(suggestions, args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase(Locale.ENGLISH);
        List<String> result = new ArrayList<>();
        for (String s : list) if (s.toLowerCase(Locale.ENGLISH).startsWith(lower)) result.add(s);
        return result;
    }
}
