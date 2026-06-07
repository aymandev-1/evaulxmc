package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.staff.StaffRequestManager.StaffAction;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;

import java.util.List;

public class StaffLogsCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public StaffLogsCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.stafflogs")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        List<StaffAction> actions;
        String title;
        if (args.length == 0 || args[0].equalsIgnoreCase("recent")) {
            actions = plugin.getStaffRequestManager().getRecentActions(10);
            title = "Recent Staff Logs";
        } else if (args[0].equalsIgnoreCase("search")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /stafflogs search <query>"));
                return true;
            }
            actions = plugin.getStaffRequestManager().searchActions(join(args, 1), 10);
            title = "Staff Log Search";
        } else {
            actions = plugin.getStaffRequestManager().getActionsFor(args[0], 10);
            title = "Staff Logs for &f" + args[0];
        }

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&c" + title));
        if (actions.isEmpty()) {
            sender.sendMessage(CC.color("&7No staff logs found."));
        } else {
            for (StaffAction action : actions) {
                sender.sendMessage(CC.color("&7[" + action.getFormattedTime() + "] &f"
                        + action.getActor() + " &c" + action.getAction()
                        + (action.getTarget() == null ? "" : " &f" + action.getTarget())
                        + (action.getDetail().isEmpty() ? "" : " &8- &7" + action.getDetail())));
            }
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
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
}
