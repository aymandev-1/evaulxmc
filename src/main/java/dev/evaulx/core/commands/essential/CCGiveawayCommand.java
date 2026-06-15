package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CCGiveawayCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public CCGiveawayCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can run a giveaway."));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("evaulx.ccgiveaway")) {
            player.sendMessage(CC.color("&cYou don't have permission to run a giveaway."));
            return true;
        }
        String error = plugin.getContentCreatorManager().runGiveaway(player);
        if (error != null) player.sendMessage(CC.color(error));
        return true;
    }
}
