package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSpyCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public CommandSpyCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission("evaulx.commandspy")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        boolean enabled = plugin.getStaffRequestManager().toggleCommandSpy(player);
        plugin.getMessageManager().send(player, enabled ? "commandspy.enabled" : "commandspy.disabled",
                enabled ? "&7Command spy &aenabled&7." : "&7Command spy &cdisabled&7.");
        plugin.getStaffRequestManager().logAction(player.getName(), "COMMAND_SPY", player.getName(), enabled ? "enabled" : "disabled");
        return true;
    }
}
