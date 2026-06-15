package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WatchPartyCommand implements CommandExecutor, TabCompleter {

    private static final Map<UUID, String> ACTIVE_HOSTS = new ConcurrentHashMap<>();

    private final EvaulxCore plugin;

    public WatchPartyCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }

        if (!sender.hasPermission("evaulx.watchparty") && !sender.hasPermission("evaulx.creator")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        boolean enable;

        if (args.length > 0) {
            String arg = args[0].toLowerCase(Locale.ENGLISH);
            if (arg.equals("on") || arg.equals("start")) {
                enable = true;
            } else if (arg.equals("off") || arg.equals("end") || arg.equals("stop")) {
                enable = false;
            } else {
                player.sendMessage(CC.color("&cUsage: /watchparty <on|off>"));
                return true;
            }
        } else {
            enable = !ACTIVE_HOSTS.containsKey(player.getUniqueId());
        }

        if (enable) {
            ACTIVE_HOSTS.put(player.getUniqueId(), player.getName());

            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendTitle(
                        CC.color("&d&l🎥 WATCH PARTY"),
                        CC.color("&7Join &d" + player.getName() + "&7's stream!")
                );
                online.sendMessage(CC.color("&d✦ &5[&dWatch Party&5] &f" + player.getName()
                        + " &7has started a watch party! Check out their stream!"));
                try { online.playSound(online.getLocation(), Sound.valueOf("ENTITY_FIREWORK_ROCKET_BLAST"), 0.7f, 1.0f); } catch (Throwable ignored) {}
            }
        } else {
            ACTIVE_HOSTS.remove(player.getUniqueId());

            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(CC.color("&5[&dWatch Party&5] &f" + player.getName() + " &7's watch party has ended. Thanks for watching!"));
            }
        }

        plugin.getStaffRequestManager().logAction(player.getName(), "WATCH_PARTY", enable ? "started" : "ended", "Watch party toggled");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.watchparty") && !sender.hasPermission("evaulx.creator")) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("on", "off").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ENGLISH)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
