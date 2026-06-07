package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public class HomesCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public HomesCommand(EvaulxCore plugin) {
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
        List<String> names = plugin.getHomesManager().getHomeNames(player.getUniqueId());
        int limit = plugin.getHomesManager().getHomeLimit(player);
        String limitText = limit < 0 ? "unlimited" : String.valueOf(limit);
        if (names.isEmpty()) {
            player.sendMessage(CC.color("&7You have no homes. &8(limit: " + limitText + ")"));
            return true;
        }
        player.sendMessage(CC.color("&7Your homes &8(" + names.size() + "/" + limitText + ")&7: &f" + String.join("&7, &f", names)));
        return true;
    }
}
