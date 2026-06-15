package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public final class MilestoneCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public MilestoneCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can broadcast milestones."));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("evaulx.milestone")) {
            player.sendMessage(CC.color("&cYou don't have permission to use this command."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(CC.color("&cUsage: /milestone <message>"));
            player.sendMessage(CC.color("&7Example: /milestone 10,000 subscribers on YouTube!"));
            return true;
        }
        String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
        String error = plugin.getContentCreatorManager().broadcastMilestone(player, message);
        if (error != null) player.sendMessage(CC.color(error));
        return true;
    }
}






