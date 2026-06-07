package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;

import java.util.List;

public class WarpsCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public WarpsCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.warp")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }
        List<String> names = plugin.getWarpManager().getWarpNames();
        if (names.isEmpty()) {
            sender.sendMessage(CC.color("&7There are no warps set."));
            return true;
        }
        sender.sendMessage(CC.color("&7Warps &8(" + names.size() + ")&7: &f" + String.join("&7, &f", names)));
        return true;
    }
}
