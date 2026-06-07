package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class StaffChatCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public StaffChatCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.staffchat")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(CC.color("&cUsage: /staffchat <message>"));
                return true;
            }

            Player player = (Player) sender;
            boolean enabled = plugin.getStaffRequestManager().toggleStaffChat(player);
            player.sendMessage(CC.color(enabled ? "&aStaff chat enabled." : "&cStaff chat disabled."));
            return true;
        }

        String message = clean(join(args), 240);
        if (message.isEmpty()) {
            sender.sendMessage(CC.color("&cUsage: /staffchat <message>"));
            return true;
        }

        plugin.getStaffRequestManager().sendStaffChat(asPlayerName(sender), message);
        return true;
    }

    private String asPlayerName(CommandSender sender) {
        return sender instanceof Player ? sender.getName() : "Console";
    }

    private String join(String[] args) {
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(arg);
        }
        return builder.toString();
    }

    private String clean(String input, int maxLength) {
        String cleaned = CC.strip(input).replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }
}
