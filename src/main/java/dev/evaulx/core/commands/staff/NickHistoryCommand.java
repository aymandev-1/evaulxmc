package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.disguise.DisguiseManager.DisguiseHistoryEntry;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NickHistoryCommand implements CommandExecutor {

    private final EvaulxCore plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm");

    public NickHistoryCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.nickhistory")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        String query = args.length > 0 ? args[0] : null;
        List<DisguiseHistoryEntry> entries = plugin.getDisguiseManager().getHistory(query, 10);

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cNick History" + (query == null ? "" : " &7(" + query + ")")));
        if (entries.isEmpty()) {
            sender.sendMessage(CC.color("&7No disguise history found."));
        } else {
            for (DisguiseHistoryEntry entry : entries) {
                sender.sendMessage(CC.color("&7" + dateFormat.format(new Date(entry.getCreatedAt()))
                        + " &f" + entry.getRealName()
                        + " &7-> &c" + entry.getDisguiseName()
                        + " &7skin &f" + none(entry.getSkinName())
                        + " &7rank &f" + none(entry.getRankName())
                        + " &8@" + entry.getServerId()));
            }
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private String none(String value) {
        return value == null || value.isEmpty() ? "none" : value;
    }
}
