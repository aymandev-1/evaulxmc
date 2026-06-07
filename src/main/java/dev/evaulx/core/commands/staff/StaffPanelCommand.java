package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class StaffPanelCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public StaffPanelCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can open the staff panel."));
            return true;
        }
        if (!sender.hasPermission("evaulx.staffpanel")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        plugin.getGuiManager().openStaffPanel((Player) sender);
        return true;
    }
}
