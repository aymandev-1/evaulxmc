package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class NickCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public NickCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("evaulx.nick")) {
            player.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            plugin.getDisguiseManager().openNickSelector(player);
            return true;
        }

        String sub = args[0];

        if (sub.equalsIgnoreCase("off") || sub.equalsIgnoreCase("reset")) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (profile == null || !profile.isDisguised()) {
                player.sendMessage(CC.color("&cYou don't have a nickname set."));
                return true;
            }
            plugin.getDisguiseManager().undisguise(player);
            player.sendMessage(CC.color("&7Your nickname has been removed."));
            return true;
        }

        if (!sub.matches("[a-zA-Z0-9_]{3,16}")) {
            player.sendMessage(CC.color("&cNickname must be 3–16 alphanumeric characters or underscores."));
            return true;
        }

        // /nick <name> — set directly using own skin, no rank change
        plugin.getDisguiseManager().disguise(player, sub, player.getName(), null);
        player.sendMessage(CC.color("&7Nickname set to &f" + sub + "&7."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.nick")) return Collections.emptyList();
        if (args.length == 1) return Arrays.asList("off", "reset");
        return Collections.emptyList();
    }
}
