package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class GlobalFlyCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public GlobalFlyCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.globalfly")) {
            sender.sendMessage(CC.color("&cYou don't have permission to use this command."));
            return true;
        }

        boolean enable;
        if (args.length == 0) {
            enable = Bukkit.getOnlinePlayers().stream().noneMatch(Player::getAllowFlight);
        } else {
            String arg = args[0].toLowerCase(Locale.ENGLISH);
            if (arg.equals("on")) {
                enable = true;
            } else if (arg.equals("off")) {
                enable = false;
            } else {
                sender.sendMessage(CC.color("&cUsage: /globalfly [on|off]"));
                return true;
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setAllowFlight(enable);
            if (!enable) p.setFlying(false);
        }

        String senderName = sender instanceof Player ? ((Player) sender).getName() : "Console";
        String state = enable ? "&aenabled" : "&cdisabled";
        Bukkit.broadcastMessage(CC.color("&8[&6Owner&8] &7Global fly has been " + state + " &7by &e" + senderName + "&7."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("on", "off").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ENGLISH)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
