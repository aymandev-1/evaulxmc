package dev.evaulx.core.commands.punishment;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CheckPunishmentsCommand implements CommandExecutor {

    private final EvaulxCore plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy HH:mm");

    public CheckPunishmentsCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.checkpunishments")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("meta")) {
            return updateMetadata(sender, args);
        }

        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /checkpunishments <player>"));
            sender.sendMessage(CC.color("&cUsage: /checkpunishments meta <player> <id> <evidence|appeal|staffnote|internalnote> <value|none>"));
            return true;
        }

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        List<Punishment> punishments = plugin.getDatabaseManager().getPunishments(target.getUniqueId());
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cHistory &7for &f" + args[0] + " &7(" + punishments.size() + " total)"));
        sender.sendMessage(CC.color(CC.SEPARATOR));
        if (punishments.isEmpty()) {
            sender.sendMessage(CC.color("  &7No punishments found."));
        } else {
            for (Punishment punishment : punishments) {
                String state = punishment.isActive() ? "&a[ACTIVE]" : "&8[INACTIVE]";
                sender.sendMessage(CC.color(state + " &8#" + punishment.getId()
                        + " &f" + punishment.getType().name()
                        + " &7by &f" + punishment.getPunisherName()
                        + " &8| &7" + punishment.getReason()
                        + " &8| &7appeal:&f" + punishment.getAppealStatus()
                        + " &8| &7" + dateFormat.format(new Date(punishment.getIssued()))));
                if (punishment.getEvidenceUrl() != null && !punishment.getEvidenceUrl().isEmpty()) {
                    sender.sendMessage(CC.color("  &7Evidence: &f" + punishment.getEvidenceUrl()));
                }
                if (punishment.getStaffNote() != null && !punishment.getStaffNote().isEmpty()) {
                    sender.sendMessage(CC.color("  &7Staff note: &f" + punishment.getStaffNote()));
                }
            }
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean updateMetadata(CommandSender sender, String[] args) {
        if (!sender.hasPermission("evaulx.punish")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 5) {
            sender.sendMessage(CC.color("&cUsage: /checkpunishments meta <player> <id> <evidence|appeal|staffnote|internalnote> <value|none>"));
            return true;
        }

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[1]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        Punishment punishment = findPunishment(target, args[2]);
        if (punishment == null) {
            sender.sendMessage(CC.color("&cPunishment ID not found for that player."));
            return true;
        }

        String value = join(args, 4);
        if (value.equalsIgnoreCase("none") || value.equalsIgnoreCase("clear")) value = "";
        String field = args[3].toLowerCase(Locale.ENGLISH);
        if (field.equals("evidence") || field.equals("evidenceurl")) {
            punishment.setEvidenceUrl(value);
        } else if (field.equals("appeal") || field.equals("appealstatus")) {
            punishment.setAppealStatus(value.isEmpty() ? plugin.getConfig().getString("punishments.default-appeal-status", "not-submitted") : value);
        } else if (field.equals("staffnote")) {
            punishment.setStaffNote(value);
        } else if (field.equals("internalnote") || field.equals("internal")) {
            punishment.setInternalNote(value);
        } else {
            sender.sendMessage(CC.color("&cUnknown metadata field."));
            return true;
        }

        plugin.getDatabaseManager().updatePunishment(punishment);
        plugin.getStaffRequestManager().logAction(sender.getName(), "PUNISHMENT_META",
                punishment.getTargetName(), punishment.getId() + " " + field);
        sender.sendMessage(CC.color("&aUpdated punishment &f" + punishment.getId() + " &ametadata."));
        return true;
    }

    private Punishment findPunishment(OfflinePlayer target, String id) {
        for (Punishment punishment : plugin.getDatabaseManager().getPunishments(target.getUniqueId())) {
            if (punishment.getId().equalsIgnoreCase(id)) return punishment;
        }
        return null;
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
