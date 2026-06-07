package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SudoCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public SudoCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.sudo")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /sudo <player> <command|c:message>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not online."));
            return true;
        }

        String input = join(args, 1);
        if (input.toLowerCase().startsWith("c:") || input.toLowerCase().startsWith("chat:")) {
            String message = input.substring(input.indexOf(':') + 1).trim();
            if (message.isEmpty()) {
                sender.sendMessage(CC.color("&cChat message cannot be empty."));
                return true;
            }
            target.chat(message);
            sender.sendMessage(CC.color("&7Forced &f" + target.getName() + " &7to chat."));
            return true;
        }

        if (input.startsWith("/")) input = input.substring(1);
        Bukkit.dispatchCommand(target, input);
        sender.sendMessage(CC.color("&7Forced &f" + target.getName() + " &7to run &f/" + input + "&7."));
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
