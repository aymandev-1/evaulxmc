package dev.evaulx.core.commands.rank;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Grant;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;

import java.util.List;

public class GrantsCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public GrantsCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.grants")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /grants <player|active|search> [query]"));
            return true;
        }

        if (args[0].equalsIgnoreCase("active")) {
            sendList(sender, plugin.getGrantManager().getActiveGrants(), "Active Grants");
            return true;
        }

        if (args[0].equalsIgnoreCase("search")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /grants search <query>"));
                return true;
            }
            sendList(sender, plugin.getGrantManager().searchGrants(join(args, 1), 10), "Grant Search");
            return true;
        }

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        sendList(sender, plugin.getGrantManager().getGrants(target.getUniqueId()), "Grants for &f" + target.getName());
        return true;
    }

    private void sendList(CommandSender sender, List<Grant> grants, String title) {
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&c" + title));
        if (grants.isEmpty()) {
            sender.sendMessage(CC.color("&7No grants found."));
        } else {
            for (Grant grant : grants) {
                sender.sendMessage(CC.color("&8" + grant.getId() + " &f" + grant.getRankName()
                        + " &7by &f" + grant.getIssuerName()
                        + " &8- " + (grant.isActive() ? "&aActive" : "&cInactive")
                        + " &7(" + grant.getDurationString() + ")"));
            }
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
