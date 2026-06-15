package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class SeenCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;
    private static final SimpleDateFormat FMT = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a");

    public SeenCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.seen")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /seen <player>"));
            return true;
        }

        String name = args[0];
        Player online = Bukkit.getPlayer(name);
        if (online != null) {
            sender.sendMessage(CC.color("&7" + online.getName() + " &ais currently online."));
            return true;
        }

        TaskUtil.async(() -> {
            OfflinePlayer offlinePlayer = plugin.getPlayerLookupManager().find(name);
            if (offlinePlayer == null) {
                TaskUtil.sync(() -> sender.sendMessage(CC.color("&cPlayer &f" + name + " &chas never been seen on this server.")));
                return;
            }
            String resolvedName = offlinePlayer.getName() != null ? offlinePlayer.getName() : name;
            PlayerProfile profile = plugin.getDatabaseManager().loadProfile(offlinePlayer.getUniqueId(), resolvedName);
            TaskUtil.sync(() -> {
                if (profile == null || profile.getLastSeen() <= 0) {
                    sender.sendMessage(CC.color("&cPlayer &f" + resolvedName + " &chas never been seen on this server."));
                    return;
                }
                long lastSeen = profile.getLastSeen();
                String formatted = FMT.format(new Date(lastSeen));
                long diff = System.currentTimeMillis() - lastSeen;
                sender.sendMessage(CC.color("&7" + profile.getName() + " &7was last seen &f" + formatted
                        + " &7(&f" + formatDiff(diff) + " ago&7)."));
            });
        });
        return true;
    }

    private String formatDiff(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h " + (minutes % 60) + "m";
        long days = hours / 24;
        return days + "d " + (hours % 24) + "h";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.seen")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        return Collections.emptyList();
    }
}
