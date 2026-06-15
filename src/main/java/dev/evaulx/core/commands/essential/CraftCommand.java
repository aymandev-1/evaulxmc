package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CraftCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public CraftCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("evaulx.craft")) {
            player.sendMessage(CC.color("&cYou don't have permission to use a workbench."));
            return true;
        }
        player.openWorkbench(null, true);
        return true;
    }
}
