package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffDashboardCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public StaffDashboardCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.staffdashboard")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cPlayers only."));
            return true;
        }
        plugin.getGuiManager().openStaffDashboard((Player) sender);
        return true;
    }
}
