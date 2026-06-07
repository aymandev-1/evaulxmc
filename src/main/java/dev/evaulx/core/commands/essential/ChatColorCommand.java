package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.DisplayUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class ChatColorCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public ChatColorCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.chatcolor")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(CC.color("&cChat colors: &7" + DisplayUtil.colorNames()));
            sender.sendMessage(CC.color("&7Usage: &f/chatcolor <color|reset> [player]"));
            return true;
        }

        Player target = resolveTarget(sender, args);
        if (target == null) return true;

        String color = DisplayUtil.colorCode(args[0]);
        if (color == null) {
            sender.sendMessage(CC.color("&cUnknown color. Use /chatcolor list."));
            return true;
        }

        PlayerProfile profile = plugin.getPlayerManager().getProfile(target);
        if (profile == null) {
            sender.sendMessage(CC.color("&cProfile not loaded."));
            return true;
        }

        profile.setChatColor(color);
        plugin.getPlayerManager().saveProfile(profile);
        target.sendMessage(CC.color(color.isEmpty()
                ? "&7Your chat color was reset."
                : "&7Your chat color is now " + color + "this color&7."));
        if (!target.equals(sender)) {
            sender.sendMessage(CC.color("&aUpdated &f" + target.getName() + "&a's chat color."));
        }
        return true;
    }

    private Player resolveTarget(CommandSender sender, String[] args) {
        if (args.length > 1) {
            if (!sender.hasPermission("evaulx.chatcolor.others")) {
                sender.sendMessage(CC.color("&cNo permission to edit other players."));
                return null;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) sender.sendMessage(CC.color("&cPlayer must be online."));
            return target;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cUsage: /chatcolor <color|reset> <player>"));
            return null;
        }
        return (Player) sender;
    }
}
