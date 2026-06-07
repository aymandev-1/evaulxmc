package dev.evaulx.core.commands.punishment;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import dev.evaulx.core.utils.TimeUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TempBanCommand implements CommandExecutor {
    private final EvaulxCore plugin;
    public TempBanCommand(EvaulxCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.tempban")) { sender.sendMessage(CC.color("&cNo permission.")); return true; }
        if (args.length < 2) { sender.sendMessage(CC.color("&cUsage: /tempban <player> <duration> [reason] [-s]")); return true; }

        boolean silent = args[args.length - 1].equalsIgnoreCase("-s");
        long expires = TimeUtil.parseDuration(args[1]);
        if (expires == -1L) { sender.sendMessage(CC.color("&cInvalid duration. Example: 1d, 2h, 30m")); return true; }

        String reason = args.length > 2 ? buildReason(args, 2, silent) : "Temp banned by staff";
        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target == null || !target.hasPlayedBefore()) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }

        String ip = target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : null;
        plugin.getPunishmentManager().punish(sender, target.getUniqueId(), target.getName(), ip, Punishment.Type.TEMPBAN, reason, expires, silent);
        return true;
    }

    private String buildReason(String[] args, int start, boolean silent) {
        StringBuilder sb = new StringBuilder();
        int end = silent ? args.length - 1 : args.length;
        for (int i = start; i < end; i++) { if (i > start) sb.append(" "); sb.append(args[i]); }
        return sb.toString().isEmpty() ? "Temp banned by staff" : sb.toString();
    }
}
