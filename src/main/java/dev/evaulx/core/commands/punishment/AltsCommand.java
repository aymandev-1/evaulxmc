package dev.evaulx.core.commands.punishment;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AltsCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public AltsCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.alts")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /alts <player>"));
            return true;
        }

        org.bukkit.OfflinePlayer target = plugin.getPlayerLookupManager().find(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        String ip = resolveIp(target);

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cAlt Lookup &7— &f" + targetName
                + " &7| IP: " + (ip != null ? "&f" + ip : "&8unknown")));

        if (ip == null) {
            sender.sendMessage(CC.color("&7No IP data found. Player must have joined at least once while online."));
            sender.sendMessage(CC.color(CC.SEPARATOR));
            return true;
        }

        // Collect UUIDs linked to this IP via punishment records and online players
        Set<UUID> linked = new LinkedHashSet<UUID>();
        for (Punishment p : plugin.getDatabaseManager().getPunishmentsByIp(ip)) {
            if (!p.getTarget().equals(target.getUniqueId())) {
                linked.add(p.getTarget());
            }
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(target.getUniqueId())) continue;
            PlayerProfile profile = plugin.getPlayerManager().getProfile(online);
            if (profile != null && ip.equals(profile.getIp())) {
                linked.add(online.getUniqueId());
            }
        }

        if (linked.isEmpty()) {
            sender.sendMessage(CC.color("&7No alternate accounts found for this IP."));
        } else {
            sender.sendMessage(CC.color("&7Found &f" + linked.size() + " &7linked account(s):"));
            for (UUID uuid : linked) {
                org.bukkit.OfflinePlayer alt = Bukkit.getOfflinePlayer(uuid);
                String altName = alt.getName() != null ? alt.getName() : uuid.toString().substring(0, 8) + "..";
                boolean isOnline = alt.isOnline();
                boolean isBanned = plugin.getPunishmentManager().isBanned(uuid);
                boolean isMuted  = plugin.getPunishmentManager().isMuted(uuid);
                int warnCount    = plugin.getPunishmentManager().getActiveWarnCount(uuid);
                int total        = plugin.getPunishmentManager().getTotalPunishmentCount(uuid);

                List<String> flags = new ArrayList<String>();
                if (isBanned) flags.add("&cBAN");
                if (isMuted)  flags.add("&eMUTE");
                if (warnCount > 0) flags.add("&6" + warnCount + "W");

                String onlineStr  = isOnline ? "&aONLINE" : "&8offline";
                String flagStr    = flags.isEmpty() ? "" : " &8[" + join(flags, "&8, ") + "&8]";
                String riskStr    = " &7risk:" + plugin.getPunishmentManager().getRiskLevel(uuid);

                sender.sendMessage(CC.color("  &f" + altName + " &8| " + onlineStr + flagStr + riskStr
                        + " &8| &7" + total + " punish(s)"));

                // Show their active ban detail if they have one
                Punishment activeBan = plugin.getPunishmentManager().getActiveBan(uuid);
                if (activeBan != null) {
                    sender.sendMessage(CC.color("    &8└ &c" + activeBan.getType().name()
                            + " &7by &f" + activeBan.getPunisherName()
                            + " &8| &7" + activeBan.getReason()
                            + " &8| &7#" + activeBan.getId()));
                }
            }
        }

        plugin.getStaffRequestManager().logAction(sender.getName(), "ALTS", targetName, ip);
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private String resolveIp(org.bukkit.OfflinePlayer target) {
        // Prefer live profile for online players
        Player online = target.getPlayer();
        if (online != null) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(online);
            if (profile != null && profile.getIp() != null) return profile.getIp();
            if (online.getAddress() != null) return online.getAddress().getAddress().getHostAddress();
        }
        // Fall back to stored profile
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        PlayerProfile stored = plugin.getDatabaseManager().loadProfile(target.getUniqueId(), name);
        return stored != null ? stored.getIp() : null;
    }

    private String join(List<String> list, String separator) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) sb.append(separator);
            sb.append(s);
        }
        return sb.toString();
    }
}
