package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.disguise.DisguiseManager.DisguiseHistoryEntry;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NickHistoryCommand implements CommandExecutor, TabCompleter {

    private static final int PAGE_SIZE = 10;
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

        String query = null;
        int page = 1;

        if (args.length > 0) {
            // Detect if the last argument is a page number
            try {
                if (args.length >= 2) {
                    page = Integer.parseInt(args[args.length - 1]);
                    query = String.join(" ", java.util.Arrays.copyOfRange(args, 0, args.length - 1));
                } else {
                    // Could be a page number or a player name
                    try {
                        page = Integer.parseInt(args[0]);
                    } catch (NumberFormatException e) {
                        query = args[0];
                    }
                }
            } catch (NumberFormatException e) {
                query = args[0];
            }
        }

        if (page < 1) page = 1;
        int offset = (page - 1) * PAGE_SIZE;
        int total = plugin.getDisguiseManager().countHistory(query);
        int maxPage = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        if (page > maxPage) page = maxPage;
        offset = (page - 1) * PAGE_SIZE;

        List<DisguiseHistoryEntry> entries = plugin.getDisguiseManager().getHistory(query, PAGE_SIZE, offset);

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cNick History"
                + (query != null ? " &7(" + query + ")" : "")
                + " &8- &7Page &f" + page + "&7/&f" + maxPage
                + " &8(&f" + total + " &7total&8)"));
        if (entries.isEmpty()) {
            sender.sendMessage(CC.color("&7No disguise history found."));
        } else {
            for (DisguiseHistoryEntry entry : entries) {
                String cmdLabel = entry.getCommandName().equalsIgnoreCase("nick") ? "&d/nick" : "&c/disguise";
                sender.sendMessage(CC.color("&7" + dateFormat.format(new Date(entry.getCreatedAt()))
                        + " " + cmdLabel + " &8| &f" + entry.getRealName()
                        + " &7→ &c" + entry.getDisguiseName()
                        + " &8| &7skin &f" + none(entry.getSkinName())
                        + " &7rank &f" + none(entry.getRankName())
                        + " &8@&7" + entry.getServerId()));
            }
        }
        if (page < maxPage) {
            String nextCmd = "/" + label + (query != null ? " " + query : "") + " " + (page + 1);
            sender.sendMessage(CC.color("&7Next page: &f" + nextCmd));
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.nickhistory")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            String input = args[0].toLowerCase(Locale.ENGLISH);
            names.removeIf(n -> !n.toLowerCase(Locale.ENGLISH).startsWith(input));
            return names;
        }
        return Collections.emptyList();
    }

    private String none(String value) {
        return value == null || value.isEmpty() ? "none" : value;
    }
}
