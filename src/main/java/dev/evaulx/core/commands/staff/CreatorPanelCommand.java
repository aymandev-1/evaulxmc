package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * Opens the Content Creator control-panel GUI.
 * Usage: /creatorpanel
 */
public class CreatorPanelCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public CreatorPanelCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        boolean isCreator = plugin.getContentCreatorManager().isCreator(player.getUniqueId());
        if (!isCreator && !player.hasPermission("evaulx.panel.creator")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }
        plugin.getGuiManager().openCreatorPanel(player);
        return true;
    }
}
