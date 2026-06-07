package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.managers.HomesManager;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SetHomeCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public SetHomeCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!sender.hasPermission("evaulx.home")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        Player player = (Player) sender;
        String name = args.length >= 1 ? args[0] : "home";
        if (!name.matches("[A-Za-z0-9_-]{1,32}")) {
            sender.sendMessage(CC.color("&cHome names must be 1-32 letters, numbers, _ or -."));
            return true;
        }

        HomesManager.SetResult result = plugin.getHomesManager().setHome(player, name, player.getLocation());
        switch (result) {
            case OK:
                int limit = plugin.getHomesManager().getHomeLimit(player);
                String limitText = limit < 0 ? "" : " &8(" + plugin.getHomesManager().getHomeCount(player.getUniqueId()) + "/" + limit + ")";
                player.sendMessage(CC.color("&7Home &f" + name + " &7set." + limitText));
                break;
            case LIMIT_REACHED:
                player.sendMessage(CC.color("&cYou have reached your home limit (&f"
                        + plugin.getHomesManager().getHomeLimit(player) + "&c). Delete one or use a different name."));
                break;
            default:
                player.sendMessage(CC.color("&cCould not set that home."));
                break;
        }
        return true;
    }
}
