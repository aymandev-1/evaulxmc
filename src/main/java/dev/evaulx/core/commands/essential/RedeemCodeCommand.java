package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RedeemCodeCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public RedeemCodeCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can redeem codes."));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("evaulx.creator.redeem")) {
            player.sendMessage(CC.color("&cYou don't have permission to redeem codes."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(CC.color("&cUsage: /redeemcode <code>"));
            player.sendMessage(CC.color("&7Content creators share codes with their viewers for free rewards!"));
            return true;
        }
        boolean found = plugin.getContentCreatorManager().redeemCode(player, args[0]);
        if (!found) {
            player.sendMessage(CC.color("&cUnknown code: &e" + args[0]));
            player.sendMessage(CC.color("&7Check your creator's video or stream for the correct code."));
        }
        return true;
    }
}






