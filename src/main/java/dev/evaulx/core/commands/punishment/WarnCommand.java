package dev.evaulx.core.commands.punishment;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public class WarnCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public WarnCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.warn")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /warn <player> [reason]"));
            return true;
        }

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        boolean silent = args.length >= 2 && args[args.length - 1].equalsIgnoreCase("-s");
        String reason = args.length > 1 ? buildReason(args, 1, silent) : "Warned by staff";

        String ip = null;
        if (target.isOnline()) {
            Player online = (Player) target;
            if (online.getAddress() != null) {
                ip = online.getAddress().getAddress().getHostAddress();
            }
        }

        plugin.getPunishmentManager().punish(sender, target.getUniqueId(), target.getName(), ip,
                Punishment.Type.WARN, reason, -1L, silent);

        // Count active warns without using streams
        List<Punishment> history = plugin.getDatabaseManager().getPunishments(target.getUniqueId());
        int activeWarns = 0;
        for (Punishment p : history) {
            if (p.getType() == Punishment.Type.WARN && p.isActive()) {
                activeWarns++;
            }
        }

        // Tell the staff member how many warns the player now has
        int threshold = plugin.getConfig().getInt("punishments.warn-threshold", 5);
        sender.sendMessage(CC.color("&f" + target.getName() + " &7now has &c" + activeWarns
                + " &7active warning(s) &8(threshold: &c" + threshold + "&8)."));

        // Notify the warned player if they are online
        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            online.sendMessage(CC.color("&c&lWARNING &7— You have been warned."));
            online.sendMessage(CC.color("&7Reason: &f" + reason));
            online.sendMessage(CC.color("&7Active warnings: &c" + activeWarns + " &7/ &c" + threshold));
        }

        // Threshold escalation
        if (activeWarns >= threshold) {
            String action = plugin.getConfig().getString("punishments.warn-threshold-action",
                    "tempban {player} 1d Reached warn threshold")
                    .replace("{player}", target.getName() != null ? target.getName() : "");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action);
        }

        return true;
    }

    private String buildReason(String[] args, int start, boolean silent) {
        int end = silent ? args.length - 1 : args.length;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString().isEmpty() ? "Warned by staff" : sb.toString();
    }
}
