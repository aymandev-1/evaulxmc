package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * Opens the Owner control-panel GUI.
 * Usage: /ownerpanel
 */
public class OwnerPanelCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public OwnerPanelCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission("evaulx.owner.panel")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }
        plugin.getGuiManager().openOwnerPanel((Player) sender);
        return true;
    }
}
