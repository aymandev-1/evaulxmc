package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.staff.StaffRequestManager.StaffRequest;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class ReportCommand implements CommandExecutor {

    private static final int MAX_REASON_LENGTH = 160;

    private final EvaulxCore plugin;

    public ReportCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /report <player> <reason>"));
            return true;
        }

        Player reporter = (Player) sender;
        String targetName = clean(args[0], 32);
        String reason = clean(join(args, 1), MAX_REASON_LENGTH);

        if (targetName.isEmpty() || reason.isEmpty()) {
            sender.sendMessage(CC.color("&cUsage: /report <player> <reason>"));
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (targetName.equalsIgnoreCase(reporter.getName())
                || (target != null && target.getUniqueId().equals(reporter.getUniqueId()))) {
            sender.sendMessage(CC.color("&cYou cannot report yourself."));
            return true;
        }

        long cooldown = plugin.getStaffRequestManager().getReportCooldownRemaining(reporter);
        if (cooldown > 0L && !reporter.hasPermission("evaulx.staff")) {
            sender.sendMessage(CC.color("&cPlease wait " + formatSeconds(cooldown) + "s before sending another report."));
            return true;
        }

        StaffRequest request = plugin.getStaffRequestManager().submitReport(reporter, targetName, reason);
        sender.sendMessage(CC.color("&7Your report was sent to staff. &8(#" + request.getId() + ")"));
        return true;
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private String clean(String input, int maxLength) {
        String cleaned = CC.strip(input).replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

    private long formatSeconds(long millis) {
        return (millis + 999L) / 1000L;
    }
}
