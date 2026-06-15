package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class WarpCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public WarpCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String lbl = label.toLowerCase();
        if (lbl.equals("setwarp")) return handleSet(sender, args);
        if (lbl.equals("delwarp") || lbl.equals("deletewarp")) return handleDelete(sender, args);
        if (lbl.equals("warps")) return handleList(sender);

        if (!sender.hasPermission("evaulx.warp")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }
        if (args.length == 0) return handleList(sender);
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can teleport.");
            return true;
        }

        Player player = (Player) sender;
        Location warp = plugin.getWarpManager().getWarp(args[0]);
        if (warp == null) {
            player.sendMessage(CC.color("&cWarp &f" + args[0] + " &cdoes not exist."));
            return true;
        }
        if (!player.hasPermission("evaulx.warp.*") && !player.hasPermission("evaulx.warp." + args[0].toLowerCase())) {
            player.sendMessage(CC.color("&cYou do not have access to that warp."));
            return true;
        }
        player.teleport(warp);
        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &aTeleported to warp &f" + args[0] + "&a."));
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("evaulx.warp.set")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can set warps.");
            return true;
        }
        if (args.length == 0) { sender.sendMessage(CC.color("&cUsage: /setwarp <name>")); return true; }
        if (!args[0].matches("[A-Za-z0-9_-]{1,32}")) {
            sender.sendMessage(CC.color("&cWarp name must be 1-32 alphanumeric characters."));
            return true;
        }
        plugin.getWarpManager().setWarp(args[0], ((Player) sender).getLocation());
        sender.sendMessage(CC.color("&aWarp &f" + args[0] + " &aset."));
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("evaulx.warp.delete")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }
        if (args.length == 0) { sender.sendMessage(CC.color("&cUsage: /delwarp <name>")); return true; }
        if (plugin.getWarpManager().deleteWarp(args[0])) {
            sender.sendMessage(CC.color("&aWarp &f" + args[0] + " &adeleted."));
        } else {
            sender.sendMessage(CC.color("&cWarp &f" + args[0] + " &cnot found."));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<String> names = plugin.getWarpManager().getWarpNames();
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("  &aWarps &8(&f" + names.size() + "&8)"));
        if (names.isEmpty()) {
            sender.sendMessage(CC.color("  &7No warps set."));
        } else {
            sender.sendMessage(CC.color("  &7" + String.join("&8, &f", names)));
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.warp")) return Collections.emptyList();
        String lbl = alias.toLowerCase();
        if (args.length == 1 && (lbl.equals("warp") || lbl.equals("delwarp") || lbl.equals("deletewarp") || lbl.equals("warps"))) {
            return plugin.getWarpManager().getWarpNames().stream()
                    .filter(n -> n.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
