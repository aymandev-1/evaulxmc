package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public final class CCChatCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public CCChatCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("evaulx.cchat")) {
            player.sendMessage(CC.color("&cYou don't have permission to use CC chat."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(CC.color("&cUsage: /cchat <message>"));
            player.sendMessage(CC.color("&7This is a private channel visible only to content creators and staff."));
            return true;
        }
        String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
        plugin.getContentCreatorManager().sendCCChat(player, message);
        return true;
    }
}
