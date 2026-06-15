package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class IpCheckCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public IpCheckCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.ipcheck")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /ipcheck <player>"));
            return true;
        }

        OfflinePlayer target = plugin.getPlayerLookupManager().find(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();

        // Resolve stored IP — prefer in-memory profile for online players
        String ip = null;
        Player online = target.getPlayer();
        if (online != null) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(online);
            if (profile != null) ip = profile.getIp();
        }
        if (ip == null) {
            PlayerProfile stored = plugin.getDatabaseManager().loadProfile(target.getUniqueId(), targetName);
            if (stored != null) ip = stored.getIp();
        }

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cIP Check &8| &f" + targetName + " &8| &7" + (online != null ? "&aOnline" : "&7Offline")));
        sender.sendMessage(CC.color("&7IP: " + (ip != null ? "&f" + ip : "&8unknown")));

        if (ip != null) {
            // Online alts (same stored IP)
            List<String> alts = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getUniqueId().equals(target.getUniqueId())) continue;
                PlayerProfile profile = plugin.getPlayerManager().getProfile(p);
                if (profile != null && ip.equals(profile.getIp())) alts.add(p.getName());
            }
            if (!alts.isEmpty()) {
                sender.sendMessage(CC.color("&eAlts online: &f" + String.join("&7, &f", alts)));
            } else {
                sender.sendMessage(CC.color("&7No alts currently online."));
            }

            // Active IP bans against this address
            List<Punishment> ipHistory = plugin.getDatabaseManager().getPunishmentsByIp(ip);
            int activeIpBans = 0;
            for (Punishment p : ipHistory) {
                if (p.isActive() && p.getType() == Punishment.Type.IPBAN) activeIpBans++;
            }
            if (activeIpBans > 0) {
                sender.sendMessage(CC.color("&c" + activeIpBans + " active IP ban(s) on this address."));
            } else {
                sender.sendMessage(CC.color("&7No active IP bans on this address."));
            }
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));

        plugin.getStaffRequestManager().logAction(sender.getName(), "IPCHECK", targetName, ip != null ? ip : "unknown");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.ipcheck")) return Collections.emptyList();
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
