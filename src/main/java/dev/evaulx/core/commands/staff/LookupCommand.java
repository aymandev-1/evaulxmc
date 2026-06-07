package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.managers.PlayerLookupManager.LookupEntry;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class LookupCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public LookupCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.lookup")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /lookup <player>"));
            sender.sendMessage(CC.color("&7Cached players: &f" + plugin.getPlayerLookupManager().size()));
            return true;
        }

        OfflinePlayer target = plugin.getPlayerLookupManager().find(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cNo cached player found for &f" + args[0] + "&c."));
            return true;
        }

        PlayerProfile profile = target.isOnline()
                ? plugin.getPlayerManager().getProfile(target.getPlayer())
                : plugin.getDatabaseManager().loadProfile(target.getUniqueId(), target.getName() == null ? args[0] : target.getName());

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cPlayer Lookup"));
        sender.sendMessage(CC.color("&7Name: &f" + (target.getName() == null ? args[0] : target.getName())));
        sender.sendMessage(CC.color("&7UUID: &f" + target.getUniqueId()));
        sender.sendMessage(CC.color("&7Online: &f" + target.isOnline()));
        if (profile != null) {
            sender.sendMessage(CC.color("&7Rank: &f" + profile.getRankName()));
            sender.sendMessage(CC.color("&7Extra ranks: &f" + (profile.getExtraRanks().isEmpty() ? "none" : join(profile.getExtraRanks()))));
            sender.sendMessage(CC.color("&7First join: &f" + format(profile.getFirstJoin())));
            sender.sendMessage(CC.color("&7Last seen: &f" + format(profile.getLastSeen())));
            sender.sendMessage(CC.color("&7IP: &f" + (profile.getIp() == null ? "unknown" : profile.getIp())));
            sender.sendMessage(CC.color("&7Vanished: &f" + profile.isVanished()
                    + " &7Staff mode: &f" + profile.isStaffMode()
                    + " &7Disguised: &f" + profile.isDisguised()));
        }
        sender.sendMessage(CC.color("&7Punishments: &f" + plugin.getPunishmentManager().getHistory(target.getUniqueId()).size()
                + " &7Grants: &f" + plugin.getGrantManager().getGrants(target.getUniqueId()).size()
                + " &7Notes: &f" + plugin.getNoteManager().getNoteCount(target.getUniqueId())));
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private String format(long time) {
        if (time <= 0L) return "unknown";
        return new SimpleDateFormat("MM/dd/yy HH:mm").format(new Date(time));
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
        if (!sender.hasPermission("evaulx.lookup")) return Collections.emptyList();
        if (args.length != 1) return Collections.emptyList();
        List<String> suggestions = new ArrayList<>(plugin.getPlayerLookupManager().suggest(args[0], 20));
        for (LookupEntry entry : plugin.getPlayerLookupManager().entries()) {
            if (suggestions.size() >= 20) break;
            if (!entry.getName().toLowerCase(Locale.ENGLISH).startsWith(args[0].toLowerCase(Locale.ENGLISH))) continue;
            if (!suggestions.contains(entry.getName())) suggestions.add(entry.getName());
        }
        Collections.sort(suggestions, String.CASE_INSENSITIVE_ORDER);
        return suggestions;
    }
}
