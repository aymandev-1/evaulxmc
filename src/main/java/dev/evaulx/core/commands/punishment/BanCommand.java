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

    public BanCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.ban")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /ban <player> [reason] [-s] [-e <url>]"));
            return true;
        }

        boolean silent   = hasFlag(args, "-s");
        String evidence  = extractFlag(args, "-e");
        String reason    = buildReason(args, 1);

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        if (plugin.getPunishmentManager().isBanned(target.getUniqueId())) {
            sender.sendMessage(CC.color("&cThat player is already banned."));
            return true;
        }

        java.net.InetSocketAddress addr = target.isOnline() ? ((Player) target).getAddress() : null;
        String ip = addr != null ? addr.getAddress().getHostAddress() : null;

        Punishment pun = plugin.getPunishmentManager().punish(sender, target.getUniqueId(),
                target.getName(), ip, Punishment.Type.BAN, reason, -1L, silent);

        if (evidence != null && !evidence.isEmpty()) {
            pun.setEvidenceUrl(evidence);
            plugin.getDatabaseManager().updatePunishment(pun);
            sender.sendMessage(CC.color("&7Evidence attached: &f" + evidence));
        }
        return true;
    }

    // Returns true if the flag is anywhere in args
    private boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(flag)) return true;
        }
        return false;
    }

    // Returns the value after a flag (e.g. -e https://...) or null
    private String extractFlag(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase(flag)) return args[i + 1];
        }
        return null;
    }

    // Builds the reason, skipping any flag tokens (-s, -e, and the value after -e)
    private String buildReason(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("-s")) { i++; continue; }
            if (arg.equalsIgnoreCase("-e")) { i += 2; continue; }
            if (sb.length() > 0) sb.append(' ');
            sb.append(arg);
            i++;
        }
        return sb.toString().isEmpty() ? "Banned by staff" : sb.toString();
    }
}
