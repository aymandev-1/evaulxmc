package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PingCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public PingCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.ping")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission("evaulx.ping.others") && !(sender instanceof Player && args[0].equalsIgnoreCase(((Player) sender).getName()))) {
                sender.sendMessage(CC.color("&cNo permission to check other players' ping."));
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(CC.color("&cPlayer not online."));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(CC.color("&cUsage: /ping <player>"));
            return true;
        }

        int ping = safeGetPing(target);
        String quality = ping < 80 ? "&aExcellent" : ping < 150 ? "&eGood" : ping < 250 ? "&6Fair" : "&cPoor";
        String color   = ping < 80 ? "&a"           : ping < 150 ? "&e"     : ping < 250 ? "&6"     : "&c";
        boolean isSelf = sender instanceof Player && target.equals(sender);
        String who = isSelf ? "Your" : "&f" + target.getName() + "'s";

        sender.sendMessage(CC.color("&8[&cPing&8] &7" + who + " &7ping: " + color + ping + "ms &8(" + quality + "&8)"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.ping")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) suggestions.add(p.getName());
            return filter(suggestions, args[0]);
        }
        return Collections.emptyList();
    }

    private static int safeGetPing(Player p) {
        try {
            Object handle = p.getClass().getMethod("getHandle").invoke(p);
            return handle.getClass().getField("ping").getInt(handle);
        } catch (Throwable ignored) { return 0; }
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase(Locale.ENGLISH);
        List<String> result = new ArrayList<>();
        for (String s : list) if (s.toLowerCase(Locale.ENGLISH).startsWith(lower)) result.add(s);
        return result;
    }
}
