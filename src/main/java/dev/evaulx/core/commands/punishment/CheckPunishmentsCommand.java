package dev.evaulx.core.commands.punishment;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import dev.evaulx.core.utils.TimeUtil;
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
        if (args.length > 0 && args[0].equalsIgnoreCase("severity")) {
            return updateSeverity(sender, args);
        }

        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /checkpunishments <player> [page]"));
            sender.sendMessage(CC.color("&cUsage: /checkpunishments meta <player> <id> <evidence|appeal|staffnote|internalnote> <value|none>"));
            sender.sendMessage(CC.color("&cUsage: /checkpunishments severity <player> <id> <minor|moderate|severe|critical>"));
            return true;
        }

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) { }
        }
        int perPage = 5;

        List<Punishment> all = plugin.getDatabaseManager().getPunishments(target.getUniqueId());
        int totalPages = all.isEmpty() ? 1 : (int) Math.ceil(all.size() / (double) perPage);
        page = Math.min(page, totalPages);

        String name = target.getName() != null ? target.getName() : args[0];
        String risk = plugin.getPunishmentManager().getRiskLevel(target.getUniqueId());

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cPunishment History &7— &f" + name
                + " &7| Total: &f" + all.size()
                + " &7| Risk: " + risk));

        // Active status summary at top
        Punishment activeBan  = plugin.getPunishmentManager().getActiveBan(target.getUniqueId());
        Punishment activeMute = plugin.getPunishmentManager().getActiveMute(target.getUniqueId());
        int activeWarns = plugin.getPunishmentManager().getActiveWarnCount(target.getUniqueId());

        if (activeBan != null) {
            String expStr = activeBan.getExpires() == -1L ? "permanent"
                    : "expires " + TimeUtil.formatDuration(activeBan.getExpires());
            sender.sendMessage(CC.color("&c[BAN] &f" + activeBan.getType().name()
                    + " &7by &f" + activeBan.getPunisherName()
                    + " &8| &7" + activeBan.getReason()
                    + " &8| &7" + expStr
                    + " &8| &7#" + activeBan.getId()));
        }
        if (activeMute != null) {
            String expStr = activeMute.getExpires() == -1L ? "permanent"
                    : "expires " + TimeUtil.formatDuration(activeMute.getExpires());
            sender.sendMessage(CC.color("&e[MUTE] &f" + activeMute.getType().name()
                    + " &7by &f" + activeMute.getPunisherName()
                    + " &8| &7" + activeMute.getReason()
                    + " &8| &7" + expStr
                    + " &8| &7#" + activeMute.getId()));
        }
        if (activeBan == null && activeMute == null) {
            sender.sendMessage(CC.color("&a[CLEAR] &7No active ban or mute."));
        }
        if (activeWarns > 0) {
            sender.sendMessage(CC.color("&e[WARNS] &f" + activeWarns + " &7active warning(s)."));
        }

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&7Full History &8(page &f" + page + "&8/&f" + totalPages + "&8)"));

        if (all.isEmpty()) {
            sender.sendMessage(CC.color("  &7No punishments on record."));
        } else {
            int start = (page - 1) * perPage;
            int end   = Math.min(start + perPage, all.size());
            for (int i = start; i < end; i++) {
                Punishment p = all.get(i);
                String stateColor = p.isActive() ? "&a" : "&8";
                String state      = p.isActive() ? "ACTIVE" : "INACTIVE";
                String sev        = p.getSeverity().display();

                sender.sendMessage(CC.color(stateColor + "[" + state + "] " + sev
                        + " &8#&f" + p.getId()
                        + " &7" + p.getType().name()
                        + " &7by &f" + p.getPunisherName()
                        + " &8| &7" + p.getReason()
                        + " &8| &7" + dateFormat.format(new Date(p.getIssued()))));

                // Duration / expiry
                if (p.getExpires() > 0) {
                    sender.sendMessage(CC.color("  &8└ &7Duration: &f" + TimeUtil.formatDuration(p.getExpires())));
                }
                // Evidence
                if (p.getEvidenceUrl() != null && !p.getEvidenceUrl().isEmpty()) {
                    sender.sendMessage(CC.color("  &8└ &7Evidence: &f" + p.getEvidenceUrl()));
                }
                // Staff note (visible to staff)
                if (p.getStaffNote() != null && !p.getStaffNote().isEmpty()) {
                    sender.sendMessage(CC.color("  &8└ &7Note: &f" + p.getStaffNote()));
                }
                // Internal note (requires higher perm)
                if (p.getInternalNote() != null && !p.getInternalNote().isEmpty()
                        && sender.hasPermission("evaulx.punish")) {
                    sender.sendMessage(CC.color("  &8└ &8[Internal] &7" + p.getInternalNote()));
                }
                // Appeal status
                sender.sendMessage(CC.color("  &8└ &7Appeal: &f" + p.getAppealStatus()));
                // Removal info
                if (!p.isActive() && p.getRemovedBy() != null) {
                    String remover = resolveRemoverName(p);
                    String removeTime = p.getRemovedAt() > 0
                            ? " &8| &7" + dateFormat.format(new Date(p.getRemovedAt()))
                            : "";
                    String removeReason = p.getRemovedReason() != null && !p.getRemovedReason().isEmpty()
                            ? " &8| &7\"" + p.getRemovedReason() + "\""
                            : "";
                    sender.sendMessage(CC.color("  &8└ &7Removed by: &f" + remover + removeReason + removeTime));
                }
            }
            if (totalPages > 1) {
                sender.sendMessage(CC.color("  &8Use &7/" + label + " " + args[0] + " <page> &8to navigate."));
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
            punishment.setAppealStatus(value.isEmpty()
                    ? plugin.getConfig().getString("punishments.default-appeal-status", "not-submitted")
                    : value);
        } else if (field.equals("staffnote")) {
            punishment.setStaffNote(value);
        } else if (field.equals("internalnote") || field.equals("internal")) {
            punishment.setInternalNote(value);
        } else {
            sender.sendMessage(CC.color("&cUnknown field. Valid: evidence, appeal, staffnote, internalnote"));
            return true;
        }

        plugin.getDatabaseManager().updatePunishment(punishment);
        plugin.getStaffRequestManager().logAction(sender.getName(), "PUNISHMENT_META",
                punishment.getTargetName(), punishment.getId() + " " + field);
        sender.sendMessage(CC.color("&aUpdated punishment &f" + punishment.getId() + " &afield &f" + field + "&a."));
        return true;
    }

    private boolean updateSeverity(CommandSender sender, String[] args) {
        if (!sender.hasPermission("evaulx.punish")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(CC.color("&cUsage: /checkpunishments severity <player> <id> <minor|moderate|severe|critical>"));
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

        Punishment.Severity severity;
        try {
            severity = Punishment.Severity.valueOf(args[3].toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(CC.color("&cInvalid severity. Valid: minor, moderate, severe, critical"));
            return true;
        }

        punishment.setSeverity(severity);
        plugin.getDatabaseManager().updatePunishment(punishment);
        plugin.getStaffRequestManager().logAction(sender.getName(), "PUNISHMENT_SEVERITY",
                punishment.getTargetName(), punishment.getId() + " -> " + severity.name());
        sender.sendMessage(CC.color("&aSet severity of &f" + punishment.getId() + " &ato " + severity.display() + "&a."));
        return true;
    }

    private Punishment findPunishment(OfflinePlayer target, String id) {
        for (Punishment p : plugin.getDatabaseManager().getPunishments(target.getUniqueId())) {
            if (p.getId().equalsIgnoreCase(id)) return p;
        }
        return null;
    }

    private String resolveRemoverName(Punishment p) {
        if (p.getRemovedBy() == null) return "console";
        org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayer(p.getRemovedBy());
        if (online != null) return online.getName();
        org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(p.getRemovedBy());
        return op.getName() != null ? op.getName() : p.getRemovedBy().toString().substring(0, 8);
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
