package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * Opens the Builder control-panel GUI.
 * Usage: /builderpanel
 */
public class BuilderPanelCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public BuilderPanelCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission("evaulx.panel.builder")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }
        plugin.getGuiManager().openBuilderPanel((Player) sender);
        return true;
    }
}
