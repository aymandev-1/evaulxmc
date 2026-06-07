package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;

import java.util.ArrayList;
import java.util.List;

public class AnnounceCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public AnnounceCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.announce")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /announce <message|list|add|remove>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            list(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (!sender.hasPermission("evaulx.announce.manage")) {
                sender.sendMessage(CC.color("&cNo permission."));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /announce add <message>"));
                return true;
            }
            add(sender, join(args, 1));
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("evaulx.announce.manage")) {
                sender.sendMessage(CC.color("&cNo permission."));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /announce remove <number>"));
                return true;
            }
            remove(sender, args[1]);
            return true;
        }

        plugin.getEssentialsManager().sendAnnouncement(sender.getName(), join(args, 0));
        return true;
    }

    private void list(CommandSender sender) {
        List<String> messages = plugin.getConfig().getStringList("tips.messages");
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cAuto Announcements &7(" + messages.size() + ")"));
        if (messages.isEmpty()) {
            sender.sendMessage(CC.color("&7No announcements configured."));
        } else {
            for (int i = 0; i < messages.size(); i++) {
                sender.sendMessage(CC.color("&8" + (i + 1) + ". &7" + messages.get(i)));
            }
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void add(CommandSender sender, String message) {
        List<String> messages = new ArrayList<>(plugin.getConfig().getStringList("tips.messages"));
        messages.add(message);
        plugin.getConfig().set("tips.messages", messages);
        plugin.saveConfig();
        sender.sendMessage(CC.color("&7Added announcement &f#" + messages.size() + "&7."));
    }

    private void remove(CommandSender sender, String rawIndex) {
        int index;
        try {
            index = Integer.parseInt(rawIndex);
        } catch (NumberFormatException ignored) {
            sender.sendMessage(CC.color("&cAnnouncement number must be numeric."));
            return;
        }

        List<String> messages = new ArrayList<>(plugin.getConfig().getStringList("tips.messages"));
        if (index < 1 || index > messages.size()) {
            sender.sendMessage(CC.color("&cAnnouncement not found."));
            return;
        }

        messages.remove(index - 1);
        plugin.getConfig().set("tips.messages", messages);
        plugin.saveConfig();
        sender.sendMessage(CC.color("&7Removed announcement &f#" + index + "&7."));
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
