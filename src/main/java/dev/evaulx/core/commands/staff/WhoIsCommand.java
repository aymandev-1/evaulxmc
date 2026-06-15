package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.models.Rank;
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

public class WhoIsCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public WhoIsCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.whois")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /whois <player>"));
            return true;
        }

        OfflinePlayer target = plugin.getPlayerLookupManager().find(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        Player online = target.getPlayer();

        PlayerProfile profile = null;
        if (online != null) profile = plugin.getPlayerManager().getProfile(online);
        if (profile == null) profile = plugin.getDatabaseManager().loadProfile(target.getUniqueId(), targetName);

        long now = System.currentTimeMillis();

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cWhoIs &8| &f" + targetName + " &8| " + (online != null ? "&aOnline" : "&7Offline")));
        sender.sendMessage(CC.color(CC.SEPARATOR));

        if (profile != null) {
            // Rank
            Rank rank = plugin.getRankManager().getRank(profile.getRankName());
            String rankDisplay = rank != null ? rank.getDisplayName() : "&7" + profile.getRankName();
            sender.sendMessage(CC.color("&7Rank: " + rankDisplay));

            // Extra ranks
            if (!profile.getExtraRanks().isEmpty()) {
                sender.sendMessage(CC.color("&7Extra ranks: &f" + String.join("&7, &f", profile.getExtraRanks())));
            }

            // IP
            sender.sendMessage(CC.color("&7IP: &f" + (profile.getIp() != null ? profile.getIp() : "unknown")));

            // First join + last seen
            if (profile.getFirstJoin() > 0) {
                String firstDate = new SimpleDateFormat("dd MMM yyyy").format(new Date(profile.getFirstJoin()));
                sender.sendMessage(CC.color("&7First join: &f" + firstDate
                        + " &8(" + elapsed(now - profile.getFirstJoin()) + " ago)"));
            }
            String lastSeen = online != null ? "&aOnline now"
                    : "&7" + elapsed(now - profile.getLastSeen()) + " ago";
            sender.sendMessage(CC.color("&7Last seen: " + lastSeen));

            // Streamer / disguise
            if (profile.isStreamerMode()) {
                sender.sendMessage(CC.color("&dStreamer mode: &aON &8(alias: &f" + profile.getStreamerAlias() + "&8)"));
            }
            if (profile.isDisguised()) {
                sender.sendMessage(CC.color("&7Disguised as: &f" + profile.getDisguiseName()));
            }

            // Staff flags
            List<String> flags = new ArrayList<>();
            if (profile.isVanished()) flags.add("&7Vanished");
            if (profile.isStaffMode()) flags.add("&7Staff mode");
            if (profile.isGodMode()) flags.add("&7God mode");
            if (!flags.isEmpty()) sender.sendMessage(CC.color("&7Active: " + String.join(" &8| ", flags)));
        }

        // Ping (online only)
        if (online != null) {
            int ping = safeGetPing(online);
            String pingColor = ping < 80 ? "&a" : ping < 150 ? "&e" : ping < 250 ? "&6" : "&c";
            sender.sendMessage(CC.color("&7Ping: " + pingColor + ping + "ms"));
        }

        // Punishment summary
        List<Punishment> punishments = plugin.getDatabaseManager().getPunishments(target.getUniqueId());
        if (!punishments.isEmpty()) {
            int bans = 0, mutes = 0, kicks = 0, warns = 0;
            boolean curBanned = false, curMuted = false;
            for (Punishment p : punishments) {
                switch (p.getType()) {
                    case BAN: case TEMPBAN: case IPBAN: case BLACKLIST: bans++; break;
                    case MUTE: case TEMPMUTE: mutes++; break;
                    case KICK: kicks++; break;
                    case WARN: warns++; break;
                    default: break;
                }
                if (p.isActive() && p.getType().isBan()) curBanned = true;
                if (p.isActive() && p.getType().isMute()) curMuted = true;
            }
            StringBuilder punStr = new StringBuilder();
            if (bans > 0) punStr.append("&c").append(bans).append(" ban(s) ");
            if (mutes > 0) punStr.append("&e").append(mutes).append(" mute(s) ");
            if (kicks > 0) punStr.append("&7").append(kicks).append(" kick(s) ");
            if (warns > 0) punStr.append("&7").append(warns).append(" warn(s)");
            sender.sendMessage(CC.color("&7Punishments: " + punStr.toString().trim()));
            if (curBanned) sender.sendMessage(CC.color("&c⚠ Currently BANNED"));
            if (curMuted)  sender.sendMessage(CC.color("&e⚠ Currently MUTED"));
        } else {
            sender.sendMessage(CC.color("&7Punishments: &anone"));
        }

        sender.sendMessage(CC.color("&7UUID: &8" + target.getUniqueId()));
        sender.sendMessage(CC.color(CC.SEPARATOR));

        plugin.getStaffRequestManager().logAction(sender.getName(), "WHOIS", targetName, "lookup");
        return true;
    }

    private String elapsed(long ms) {
        long s = ms / 1000;
        long m = s / 60;
        long h = m / 60;
        long d = h / 24;
        if (d > 365) return (d / 365) + "y";
        if (d > 0)   return d + "d";
        if (h > 0)   return h + "h";
        if (m > 0)   return m + "m";
        return s + "s";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.whois")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) suggestions.add(p.getName());
            suggestions.addAll(plugin.getPlayerLookupManager().suggest(args[0], 10));
            return filter(suggestions, args[0]);
        }
        return Collections.emptyList();
    }

    private static int safeGetPing(Player p) {
        try {
            Object handle = p.getClass().getMethod("getHandle").invoke(p);
            return handle.getClass().getField("ping").getInt(handle);
        } catch (Throwable ignored) { return 0; }
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase(Locale.ENGLISH);
        List<String> result = new ArrayList<>();
        for (String s : list) if (s.toLowerCase(Locale.ENGLISH).startsWith(lower)) result.add(s);
        return result;
    }
}
