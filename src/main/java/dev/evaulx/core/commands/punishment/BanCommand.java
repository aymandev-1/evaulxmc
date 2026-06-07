package dev.evaulx.core.commands.punishment;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class BanCommand implements CommandExecutor {
    private final EvaulxCore plugin;
    public BanCommand(EvaulxCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.ban")) { sender.sendMessage(CC.color("&cNo permission.")); return true; }
        if (args.length < 1) { sender.sendMessage(CC.color("&cUsage: /ban <player> [reason] [-s]")); return true; }

        boolean silent = args[args.length - 1].equalsIgnoreCase("-s");
        String reason = args.length > 1 ? buildReason(args, 1, silent) : "Banned by staff";

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target == null || !target.hasPlayedBefore()) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }

        if (plugin.getPunishmentManager().isBanned(target.getUniqueId())) {
            sender.sendMessage(CC.color("&cThat player is already banned.")); return true;
        }

        String ip = target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : null;
        plugin.getPunishmentManager().punish(sender, target.getUniqueId(), target.getName(), ip, Punishment.Type.BAN, reason, -1L, silent);
        return true;
    }

    private String buildReason(String[] args, int start, boolean silent) {
        StringBuilder sb = new StringBuilder();
        int end = silent ? args.length - 1 : args.length;
        for (int i = start; i < end; i++) { if (i > start) sb.append(" "); sb.append(args[i]); }
        return sb.toString().isEmpty() ? "Banned by staff" : sb.toString();
    }
}
