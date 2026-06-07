package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class AfkCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public AfkCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!sender.hasPermission("evaulx.afk")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        Player player = (Player) sender;
        String reason = args.length > 0 ? String.join(" ", args) : null;
        plugin.getAfkManager().toggle(player, reason);
        return true;
    }
}
