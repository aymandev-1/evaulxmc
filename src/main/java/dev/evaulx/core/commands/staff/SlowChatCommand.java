package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Enables/disables a global chat slow mode enforced by {@link dev.evaulx.core.listeners.ChatSlowModeListener}.
 * Usage: /slowchat <seconds|off>
 */
public class SlowChatCommand implements CommandExecutor, TabCompleter {

    /** Seconds players must wait between chat messages; 0 = disabled. */
    private static volatile int slowSeconds = 0;

    private final EvaulxCore plugin;

    public SlowChatCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public static int getSlowSeconds() {
        return slowSeconds;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.slowchat")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /slowchat <seconds|off>"));
            sender.sendMessage(CC.color("&7Chat slow mode is currently: " + (slowSeconds > 0 ? "&e" + slowSeconds + "s" : "&cdisabled")));
            return true;
        }

        String arg = args[0].toLowerCase(Locale.ENGLISH);
        if (arg.equals("off") || arg.equals("0") || arg.equals("disable")) {
            slowSeconds = 0;
        } else {
            int seconds;
            try {
                seconds = Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                sender.sendMessage(CC.color("&cUsage: /slowchat <seconds|off>"));
                return true;
            }
            if (seconds < 0 || seconds > 3600) {
                sender.sendMessage(CC.color("&cSlow mode must be between 0 and 3600 seconds."));
                return true;
            }
            slowSeconds = seconds;
        }

        String senderName = sender instanceof Player ? sender.getName() : "Console";
        String formatted = slowSeconds > 0
                ? CC.color("&8[&6SlowChat&8] &7Chat slow mode enabled: &e" + slowSeconds + "s &7between messages. &8— &6" + senderName)
                : CC.color("&8[&6SlowChat&8] &7Chat slow mode &adisabled&7. &8— &6" + senderName);
        Bukkit.broadcastMessage(formatted);

        plugin.getStaffRequestManager().logAction(senderName, "SLOWCHAT", "all", slowSeconds > 0 ? slowSeconds + "s" : "off");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return java.util.Arrays.asList("off", "3", "5", "10", "30");
        return Collections.emptyList();
    }
}
