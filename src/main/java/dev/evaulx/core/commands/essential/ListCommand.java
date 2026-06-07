package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class ListCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public ListCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.list")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        boolean canSeeVanished = !(sender instanceof Player)
                || sender.hasPermission("evaulx.vanish.see")
                || sender.hasPermission("evaulx.staff");
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        int visible = 0;

        List<Rank> ranks = plugin.getRankManager().getRanksByWeight();
        for (Rank rank : ranks) {
            grouped.put(rank.getName().toLowerCase(), new ArrayList<>());
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (profile != null && profile.isVanished() && !canSeeVanished) continue;

            Rank rank = null;
            if (profile != null && profile.isDisguised() && profile.getDisguiseRank() != null) {
                rank = plugin.getRankManager().getRank(profile.getDisguiseRank());
            }
            if (rank == null && profile != null) rank = plugin.getRankManager().getRank(profile.getRankName());
            if (rank == null) rank = plugin.getRankManager().getDefaultRank();

            String key = rank != null ? rank.getName().toLowerCase() : "default";
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>());
            grouped.get(key).add(plugin.getDisguiseManager().getVisibleName(player));
            visible++;
        }

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cOnline Players &7(" + visible + "/" + Bukkit.getMaxPlayers() + ")"));
        for (Rank rank : ranks) {
            List<String> names = grouped.get(rank.getName().toLowerCase());
            if (names == null || names.isEmpty()) continue;
            Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
            sender.sendMessage(CC.color("  " + rank.getDisplayName() + " &8(" + names.size() + ") &7" + String.join("&8, &f", names)));
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }
}
