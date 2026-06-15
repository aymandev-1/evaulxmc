package dev.evaulx.core.commands.rank;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SetRankCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public SetRankCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.rank.set")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /setrank <player> <rank>"));
            return true;
        }

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        Rank rank = plugin.getRankManager().getRank(args[1]);
        if (rank == null) {
            sender.sendMessage(CC.color("&cRank '" + args[1] + "' not found."));
            return true;
        }

        PlayerProfile profile = plugin.getPlayerManager().getProfile(target.getUniqueId());
        if (profile == null) {
            sender.sendMessage(CC.color("&cPlayer must be online for /setrank."));
            return true;
        }

        String oldRank = profile.getRankName();
        profile.setRankName(rank.getName());
        plugin.getPlayerManager().saveProfile(profile);

        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            plugin.getPlayerManager().applyPermissions(online, profile);
            plugin.getNameTagManager().applyNameTag(online);
            online.sendMessage(CC.color("&8[&6Rank&8] &7Your rank has been set to "
                    + rank.getDisplayName() + " &7by &f" + sender.getName() + "&7."));
        }

        plugin.getDiscordManager().sendRankChange(target.getName(), oldRank, rank.getName(), sender.getName());
        if (plugin.getRedisSyncManager() != null) {
            plugin.getRedisSyncManager().publishRankChange(target.getName(), target.getUniqueId(),
                    oldRank, rank.getName(), sender.getName());
        }
        plugin.getStaffRequestManager().logAction(sender.getName(), "SET_RANK",
                target.getName(), oldRank + " -> " + rank.getName());

        sender.sendMessage(CC.color("&aSet &f" + target.getName() + " &arank to " + rank.getDisplayName() + "&a."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.rank.set")) return Collections.emptyList();
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) suggestions.add(p.getName());
            return filter(suggestions, args[0]);
        }
        if (args.length == 2) {
            for (Rank r : plugin.getRankManager().getVisibleRanksByWeight()) suggestions.add(r.getName());
            return filter(suggestions, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase(Locale.ENGLISH);
        List<String> result = new ArrayList<>();
        for (String s : list) if (s.toLowerCase(Locale.ENGLISH).startsWith(lower)) result.add(s);
        Collections.sort(result);
        return result;
    }
}
