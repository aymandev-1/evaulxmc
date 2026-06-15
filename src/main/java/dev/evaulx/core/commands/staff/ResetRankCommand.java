package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Grant;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class ResetRankCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public ResetRankCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.resetrank")) {
            sender.sendMessage(CC.color("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /resetrank <player>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(CC.color("&cPlayer &e" + args[0] + " &chas never played on this server."));
            return true;
        }

        List<Grant> active = plugin.getGrantManager().getGrants(target.getUniqueId())
                .stream()
                .filter(Grant::isStoredActive)
                .collect(Collectors.toList());

        if (active.isEmpty()) {
            sender.sendMessage(CC.color("&e" + target.getName() + " &7has no active grants to remove."));
            return true;
        }

        int removed = 0;
        for (Grant grant : active) {
            if (plugin.getGrantManager().removeGrant(grant.getId(), sender, "Reset rank by " + sender.getName())) {
                removed++;
            }
        }

        sender.sendMessage(CC.color("&aReset &e" + target.getName() + "&a's rank. Removed &e" + removed + " &agrant(s)."));

        Player online = target.getPlayer();
        if (online != null) {
            online.sendMessage(CC.color("&cYour extra ranks have been reset by a staff member."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("evaulx.resetrank")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ENGLISH).startsWith(args[0].toLowerCase(Locale.ENGLISH)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
