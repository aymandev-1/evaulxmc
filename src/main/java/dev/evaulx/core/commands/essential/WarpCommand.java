package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WarpCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public WarpCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!sender.hasPermission("evaulx.warp")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            sendList(player);
            return true;
        }

        String name = args[0];
        Location warp = plugin.getWarpManager().getWarp(name);
        if (warp == null) {
            player.sendMessage(CC.color("&cWarp &f" + name + " &cnot found."));
            sendList(player);
            return true;
        }
        if (warp.getWorld() == null) {
            player.sendMessage(CC.color("&cThat warp's world is not loaded."));
            return true;
        }

        plugin.getEssentialsManager().setBackLocation(player, player.getLocation());
        player.teleport(warp);
        player.sendMessage(CC.color("&7Warped to &f" + plugin.getWarpManager().getDisplayName(name) + "&7."));
        return true;
    }

    private void sendList(Player player) {
        List<String> names = plugin.getWarpManager().getWarpNames();
        if (names.isEmpty()) {
            player.sendMessage(CC.color("&7There are no warps set."));
            return;
        }
        player.sendMessage(CC.color("&7Warps &8(" + names.size() + ")&7: &f" + String.join("&7, &f", names)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ENGLISH);
            for (String name : plugin.getWarpManager().getWarpNames()) {
                if (name.toLowerCase(Locale.ENGLISH).startsWith(prefix)) result.add(name);
            }
        }
        return result;
    }
}
