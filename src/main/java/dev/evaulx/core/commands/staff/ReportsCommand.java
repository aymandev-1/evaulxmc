package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.staff.StaffRequestManager.RequestResult;
import dev.evaulx.core.staff.StaffRequestManager.StaffRequest;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;

import java.util.List;

public class ReportsCommand implements CommandExecutor {

    private static final int PER_PAGE = 6;

    private final EvaulxCore plugin;

    public ReportsCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.staff") && !sender.hasPermission("evaulx.reports")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("claim")) {
            claim(sender, args);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("close")) {
            close(sender, args);
            return true;
        }

        boolean helpOps = args.length > 0 && args[0].equalsIgnoreCase("helpop");

        if (args.length > 0 && args[0].equalsIgnoreCase("clear")) {
            clear(sender, args);
            return true;
        }

        int pageArg = helpOps ? 1 : 0;
        int page = parsePage(args, pageArg);
        List<StaffRequest> requests = helpOps
                ? plugin.getStaffRequestManager().getHelpOps()
                : plugin.getStaffRequestManager().getReports();

        sendPage(sender, requests, helpOps ? "HelpOP" : "Reports", page);
        return true;
    }

    private void claim(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /reports claim <id>"));
            return;
        }

        Integer id = parseId(args[1]);
        if (id == null) {
            sender.sendMessage(CC.color("&cInvalid report id."));
            return;
        }

        sendResult(sender, plugin.getStaffRequestManager().claimRequest(id, sender.getName()), "claimed", id);
    }

    private void close(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /reports close <id> [reason]"));
            return;
        }

        Integer id = parseId(args[1]);
        if (id == null) {
            sender.sendMessage(CC.color("&cInvalid report id."));
            return;
        }

        String reason = args.length > 2 ? clean(join(args, 2), 120) : "Handled";
        sendResult(sender, plugin.getStaffRequestManager().closeRequest(id, sender.getName(), reason), "closed", id);
    }

    private void clear(CommandSender sender, String[] args) {
        boolean helpOps = args.length > 1 && args[1].equalsIgnoreCase("helpop");
        if (helpOps) {
            plugin.getStaffRequestManager().clearHelpOps();
            sender.sendMessage(CC.color("&7Cleared recent helpop messages."));
            return;
        }

        plugin.getStaffRequestManager().clearReports();
        sender.sendMessage(CC.color("&7Cleared recent reports."));
    }

    private Integer parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void sendResult(CommandSender sender, RequestResult result, String action, int id) {
        if (result == RequestResult.UPDATED) {
            sender.sendMessage(CC.color("&7Request &f#" + id + " &7" + action + "."));
            return;
        }

        if (result == RequestResult.CLOSED) {
            sender.sendMessage(CC.color("&cRequest #" + id + " is already closed."));
            return;
        }

        sender.sendMessage(CC.color("&cRequest #" + id + " was not found."));
    }

    private int parsePage(String[] args, int index) {
        if (args.length <= index) return 1;
        try {
            return Math.max(1, Integer.parseInt(args[index]));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void sendPage(CommandSender sender, List<StaffRequest> requests, String title, int page) {
        int totalPages = Math.max(1, (int) Math.ceil(requests.size() / (double) PER_PAGE));
        page = Math.min(page, totalPages);

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cRecent " + title + " &7(Page " + page + "/" + totalPages + ")"));

        if (requests.isEmpty()) {
            sender.sendMessage(CC.color("&7No recent entries."));
            sender.sendMessage(CC.color(CC.SEPARATOR));
            return;
        }

        int start = (page - 1) * PER_PAGE;
        int end = Math.min(start + PER_PAGE, requests.size());
        for (int i = start; i < end; i++) {
            StaffRequest request = requests.get(i);
            String status = formatStatus(request);
            if (request.getType() == StaffRequest.Type.REPORT) {
                sender.sendMessage(CC.color("&8#" + request.getId() + " " + status + " &7[" + request.getFormattedTime()
                        + "] &f" + request.getSender() + " &8-> &f" + request.getTarget()
                        + " &7- " + request.getMessage()));
            } else {
                sender.sendMessage(CC.color("&8#" + request.getId() + " " + status + " &7[" + request.getFormattedTime()
                        + "] &f" + request.getSender() + " &7- " + request.getMessage()));
            }
        }

        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private String formatStatus(StaffRequest request) {
        if (request.getStatus() == StaffRequest.Status.CLOSED) return "&8[CLOSED]";
        if (request.getStatus() == StaffRequest.Status.CLAIMED) return "&e[CLAIMED:" + request.getClaimedBy() + "]";
        return "&a[OPEN]";
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
}
