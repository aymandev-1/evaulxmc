package dev.evaulx.core.commands.punishment;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;

public class IpBanCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public IpBanCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.ipban")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /ipban <player> [reason]"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer must be online for IP ban."));
            return true;
        }

        InetSocketAddress addr = target.getAddress();
        if (addr == null) {
            sender.sendMessage(CC.color("&cCould not retrieve IP address for that player."));
            return true;
        }

        String reason = args.length > 1 ? join(args, 1) : "IP Banned";
        String ip = addr.getAddress().getHostAddress();
        plugin.getPunishmentManager().punish(sender, target.getUniqueId(), target.getName(), ip,
                Punishment.Type.IPBAN, reason, -1L, false);
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
