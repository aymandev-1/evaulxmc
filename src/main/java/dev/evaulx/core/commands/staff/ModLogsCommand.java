package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.staff.StaffRequestManager.StaffAction;
import dev.evaulx.core.staff.StaffRequestManager.StaffRequest;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import dev.evaulx.core.utils.TimeUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ModLogsCommand implements CommandExecutor {

    private static final int PER_PAGE = 6;

    private final EvaulxCore plugin;
    private final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy HH:mm");

    public ModLogsCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.modlogs")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /modlogs <player> [page] [type:<ban|mute|warn|kick|all>]"));
            return true;
        }

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        String name = target.getName() != null ? target.getName() : args[0];

        // Parse optional args: page and type filter
        int page = 1;
        String typeFilter = null;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i].toLowerCase(Locale.ENGLISH);
            if (arg.startsWith("type:")) {
                typeFilter = arg.substring(5);
            } else {
                try {
                    page = Math.max(1, Integer.parseInt(arg));
                } catch (NumberFormatException ignored) {
                    typeFilter = arg;
                }
            }
        }

        List<Punishment> all = plugin.getDatabaseManager().getPunishments(target.getUniqueId());

        // Apply type filter
        List<Punishment> filtered = new ArrayList<Punishment>();
        if (typeFilter == null || typeFilter.equals("all")) {
            filtered.addAll(all);
        } else {
            for (Punishment p : all) {
                if (matchesFilter(p, typeFilter)) filtered.add(p);
            }
        }

        int totalPages = filtered.isEmpty() ? 1 : (int) Math.ceil(filtered.size() / (double) PER_PAGE);
        page = Math.min(page, totalPages);

        String risk = plugin.getPunishmentManager().getRiskLevel(target.getUniqueId());
        int warnCount = plugin.getPunishmentManager().getActiveWarnCount(target.getUniqueId());
        Punishment activeBan  = plugin.getPunishmentManager().getActiveBan(target.getUniqueId());
        Punishment activeMute = plugin.getPunishmentManager().getActiveMute(target.getUniqueId());

        List<StaffRequest> requests = plugin.getStaffRequestManager().getRequestsFor(name, 3);
        List<StaffAction>  actions  = plugin.getStaffRequestManager().getActionsFor(name, 3);

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cMod Logs &7— &f" + name
                + " &7| Total punishments: &f" + all.size()
                + " &7| Risk: " + risk));

        // Active status line
        if (activeBan != null) {
            sender.sendMessage(CC.color("&c[BANNED] &7" + activeBan.getType().name()
                    + " &8| &7" + activeBan.getReason()
                    + " &8| &7by &f" + activeBan.getPunisherName()
                    + " &8| &7#" + activeBan.getId()));
        }
        if (activeMute != null) {
            String expStr = activeMute.getExpires() == -1L ? "perm"
                    : "exp " + TimeUtil.formatDuration(activeMute.getExpires());
            sender.sendMessage(CC.color("&e[MUTED] &7" + activeMute.getType().name()
                    + " &8| &7" + activeMute.getReason()
                    + " &8| &7" + expStr));
        }
        if (warnCount > 0) {
            sender.sendMessage(CC.color("&e[WARNS] &f" + warnCount + " &7active warning(s)"));
        }
        if (activeBan == null && activeMute == null && warnCount == 0) {
            sender.sendMessage(CC.color("&a[CLEAR] &7No active punishments."));
        }

        // Punishment history
        String filterLabel = typeFilter != null && !typeFilter.equals("all") ? " [filter: " + typeFilter + "]" : "";
        sender.sendMessage(CC.color("&7Punishments" + filterLabel + " &8(page &f" + page + "&8/&f" + totalPages + "&8)"));
        if (filtered.isEmpty()) {
            sender.sendMessage(CC.color("  &7None."));
        } else {
            int start = (page - 1) * PER_PAGE;
            int end   = Math.min(start + PER_PAGE, filtered.size());
            for (int i = start; i < end; i++) {
                Punishment p = filtered.get(i);
                String stateColor = p.isActive() ? "&a" : "&8";
                String state      = p.isActive() ? "+" : "-";
                sender.sendMessage(CC.color(stateColor + "[" + state + "] "
                        + p.getSeverity().display() + " &f" + p.getType().name()
                        + " &7by &f" + p.getPunisherName()
                        + " &8| &7" + p.getReason()
                        + " &8| &7" + format.format(new Date(p.getIssued()))
                        + " &8| &7#" + p.getId()));
                if (p.getEvidenceUrl() != null && !p.getEvidenceUrl().isEmpty()) {
                    sender.sendMessage(CC.color("  &8└ &7Evidence: &f" + p.getEvidenceUrl()));
                }
                if (!p.isActive() && p.getRemovedBy() != null) {
                    sender.sendMessage(CC.color("  &8└ &7Removed by: &f" + resolveRemoverName(p)
                            + (p.getRemovedReason() != null && !p.getRemovedReason().isEmpty()
                            ? " &8| &7\"" + p.getRemovedReason() + "\"" : "")));
                }
            }
            if (totalPages > 1) {
                sender.sendMessage(CC.color("  &8/" + label + " " + args[0] + " <page> [type:<filter>]"));
            }
        }

        // Reports
        sender.sendMessage(CC.color("&7Recent Reports"));
        if (requests.isEmpty()) {
            sender.sendMessage(CC.color("  &7None."));
        } else {
            for (StaffRequest req : requests) {
                sender.sendMessage(CC.color("  &8#" + req.getId() + " &f" + req.getType().name()
                        + " &8[" + req.getStatus().name() + "] &7" + req.getMessage()));
            }
        }

        // Staff actions
        sender.sendMessage(CC.color("&7Recent Staff Actions"));
        if (actions.isEmpty()) {
            sender.sendMessage(CC.color("  &7None."));
        } else {
            for (StaffAction action : actions) {
                sender.sendMessage(CC.color("  &f" + action.getAction()
                        + " &7by &f" + action.getActor()
                        + " &8| &7" + action.getDetail()
                        + " &8| &7" + action.getFormattedTime()));
            }
        }

        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean matchesFilter(Punishment p, String filter) {
        switch (filter) {
            case "ban":       return p.getType().isBan();
            case "mute":      return p.getType().isMute();
            case "warn":      return p.getType() == Punishment.Type.WARN;
            case "kick":      return p.getType() == Punishment.Type.KICK;
            case "active":    return p.isActive();
            case "inactive":  return !p.isActive();
            default: {
                try {
                    Punishment.Type t = Punishment.Type.valueOf(filter.toUpperCase(Locale.ENGLISH));
                    return p.getType() == t;
                } catch (IllegalArgumentException e) {
                    return true;
                }
            }
        }
    }

    private String resolveRemoverName(Punishment p) {
        if (p.getRemovedBy() == null) return "console";
        org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayer(p.getRemovedBy());
        if (online != null) return online.getName();
        org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(p.getRemovedBy());
        return op.getName() != null ? op.getName() : p.getRemovedBy().toString().substring(0, 8);
    }
}
