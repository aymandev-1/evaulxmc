package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class ServerMsgCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public ServerMsgCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.servermsg")) {
            sender.sendMessage(CC.color("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /servermsg <set|clear|view> [message]"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ENGLISH);

        switch (sub) {
            case "set":
                if (args.length < 2) {
                    sender.sendMessage(CC.color("&cUsage: /servermsg set <message>"));
                    return true;
                }
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                plugin.getConfig().set("server-message.enabled", true);
                plugin.getConfig().set("server-message.text", message);
                plugin.saveConfig();
                sender.sendMessage(CC.color("&aServer join message set to: &f" + message));
                break;
            case "clear":
                plugin.getConfig().set("server-message.enabled", false);
                plugin.getConfig().set("server-message.text", "");
                plugin.saveConfig();
                sender.sendMessage(CC.color("&aServer join message cleared."));
                break;
            case "view":
                boolean enabled = plugin.getConfig().getBoolean("server-message.enabled", false);
                String text = plugin.getConfig().getString("server-message.text", "");
                if (!enabled || text.isEmpty()) {
                    sender.sendMessage(CC.color("&7No server join message is currently set."));
                } else {
                    sender.sendMessage(CC.color("&7Current server join message:"));
                    sender.sendMessage(CC.color("&f" + text));
                }
                break;
            default:
                sender.sendMessage(CC.color("&cUsage: /servermsg <set|clear|view> [message]"));
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("set", "clear", "view").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ENGLISH)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
