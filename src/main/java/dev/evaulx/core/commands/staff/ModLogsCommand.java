package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.staff.StaffRequestManager.StaffAction;
import dev.evaulx.core.staff.StaffRequestManager.StaffRequest;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ModLogsCommand implements CommandExecutor {

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
            sender.sendMessage(CC.color("&cUsage: /modlogs <player>"));
            return true;
        }

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        String name = target.getName() != null ? target.getName() : args[0];

        List<Punishment> punishments = plugin.getDatabaseManager().getPunishments(target.getUniqueId());
        List<StaffRequest> requests = plugin.getStaffRequestManager().getRequestsFor(name, 5);
        List<StaffAction> actions = plugin.getStaffRequestManager().getActionsFor(name, 5);

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cMod Logs &7for &f" + name));

        sender.sendMessage(CC.color("&7Punishments &8(" + punishments.size() + ")"));
        if (punishments.isEmpty()) {
            sender.sendMessage(CC.color("  &7None"));
        } else {
            int shown = 0;
            for (Punishment punishment : punishments) {
                sender.sendMessage(CC.color("  " + status(punishment) + " &f" + punishment.getType().name()
                        + " &7by &f" + punishment.getPunisherName()
                        + " &8| &7" + punishment.getReason()
                        + " &8| &7" + format.format(new Date(punishment.getIssued()))));
                if (++shown >= 5) break;
            }
        }

        sender.sendMessage(CC.color("&7Reports / HelpOP"));
        if (requests.isEmpty()) {
            sender.sendMessage(CC.color("  &7None"));
        } else {
            for (StaffRequest request : requests) {
                sender.sendMessage(CC.color("  &8#" + request.getId() + " &f" + request.getType().name()
                        + " &7" + request.getStatus().name()
                        + " &8| &7" + request.getMessage()));
            }
        }

        sender.sendMessage(CC.color("&7Staff Actions"));
        if (actions.isEmpty()) {
            sender.sendMessage(CC.color("  &7None"));
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

    private String status(Punishment punishment) {
        return punishment.isActive() ? "&a[ACTIVE]" : "&8[INACTIVE]";
    }
}
