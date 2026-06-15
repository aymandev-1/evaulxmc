package dev.evaulx.core.commands.punishment;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;

import java.util.List;

public class EvidenceCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public EvidenceCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.punish")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /evidence <player> <punishment-id> [url|clear]"));
            return true;
        }

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        String id = args[1];
        Punishment punishment = findPunishment(target, id);
        if (punishment == null) {
            sender.sendMessage(CC.color("&cPunishment ID &f" + id + " &cnot found for &f" + args[0] + "&c."));
            return true;
        }

        // No URL arg → show current evidence
        if (args.length < 3) {
            String current = punishment.getEvidenceUrl();
            if (current == null || current.isEmpty()) {
                sender.sendMessage(CC.color("&7No evidence set for &f" + id + "&7. Use /evidence " + args[0] + " " + id + " <url>"));
            } else {
                sender.sendMessage(CC.color("&7Evidence for &f" + id + "&7: &f" + current));
            }
            return true;
        }

        String value = join(args, 2);
        if (value.equalsIgnoreCase("clear") || value.equalsIgnoreCase("none") || value.equalsIgnoreCase("remove")) {
            punishment.setEvidenceUrl("");
            plugin.getDatabaseManager().updatePunishment(punishment);
            plugin.getStaffRequestManager().logAction(sender.getName(), "EVIDENCE_CLEAR",
                    punishment.getTargetName(), id);
            sender.sendMessage(CC.color("&aEvidence cleared from punishment &f" + id + "&a."));
        } else {
            punishment.setEvidenceUrl(value);
            plugin.getDatabaseManager().updatePunishment(punishment);
            plugin.getStaffRequestManager().logAction(sender.getName(), "EVIDENCE_SET",
                    punishment.getTargetName(), id + " -> " + value);
            sender.sendMessage(CC.color("&aEvidence set on punishment &f" + id + "&a: &f" + value));
        }
        return true;
    }

    private Punishment findPunishment(OfflinePlayer target, String id) {
        List<Punishment> list = plugin.getDatabaseManager().getPunishments(target.getUniqueId());
        for (Punishment p : list) {
            if (p.getId().equalsIgnoreCase(id)) return p;
        }
        return null;
    }

    private String join(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
