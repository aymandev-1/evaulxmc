package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * Opens the Moderator control-panel GUI.
 * Usage: /modpanel
 */
public class ModPanelCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public ModPanelCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission("evaulx.panel.mod")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }
        plugin.getGuiManager().openModPanel((Player) sender);
        return true;
    }
}
