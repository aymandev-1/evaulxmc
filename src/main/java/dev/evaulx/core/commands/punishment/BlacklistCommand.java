package dev.evaulx.core.commands.punishment;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;

public class BlacklistCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public BlacklistCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.blacklist")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /blacklist <player> [reason]"));
            return true;
        }

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        String reason = args.length > 1 ? join(args, 1) : "Blacklisted";
        InetSocketAddress addr = target.isOnline() ? ((Player) target).getAddress() : null;
        String ip = addr != null ? addr.getAddress().getHostAddress() : null;
        plugin.getPunishmentManager().punish(sender, target.getUniqueId(), target.getName(), ip,
                Punishment.Type.BLACKLIST, reason, -1L, false);
        return true;
    }

    private String join(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
