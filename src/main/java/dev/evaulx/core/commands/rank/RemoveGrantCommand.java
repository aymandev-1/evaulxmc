package dev.evaulx.core.commands.rank;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;

public class RemoveGrantCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public RemoveGrantCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.grant.remove")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /removegrant <id> [reason]"));
            return true;
        }

        String reason = args.length > 1 ? join(args, 1) : "Removed by staff";
        if (!plugin.getGrantManager().removeGrant(args[0], sender, reason)) {
            sender.sendMessage(CC.color("&cGrant not found or already inactive."));
            return true;
        }

        sender.sendMessage(CC.color("&aGrant removed."));
        return true;
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
