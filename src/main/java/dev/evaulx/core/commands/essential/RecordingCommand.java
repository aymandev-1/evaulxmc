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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Toggles a content creator's "recording" status and announces it to the server.
 * Usage: /recording [on|off]
 */
public class RecordingCommand implements CommandExecutor, TabCompleter {

    private static final Set<UUID> RECORDING = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final EvaulxCore plugin;

    public RecordingCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public static boolean isRecording(UUID uuid) {
        return RECORDING.contains(uuid);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission("evaulx.creator.recording")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        boolean enable;
        if (args.length > 0) {
            String arg = args[0].toLowerCase(Locale.ENGLISH);
            if (arg.equals("on") || arg.equals("enable")) enable = true;
            else if (arg.equals("off") || arg.equals("disable")) enable = false;
            else {
                player.sendMessage(CC.color("&cUsage: /recording [on|off]"));
                return true;
            }
        } else {
            enable = !RECORDING.contains(player.getUniqueId());
        }

        if (enable) {
            RECORDING.add(player.getUniqueId());
            Bukkit.broadcastMessage(CC.color("&8[&dLIVE&8] &f" + player.getName() + " &7is now &drecording&7! Be on your best behaviour."));
            player.sendMessage(CC.color("&8[&dCreator&8] &7Recording status &aenabled&7."));
            try { player.playSound(player.getLocation(), Sound.valueOf("NOTE_PLING"), 0.6f, 1.8f); } catch (Throwable ignored) {}
        } else {
            RECORDING.remove(player.getUniqueId());
            player.sendMessage(CC.color("&8[&dCreator&8] &7Recording status &cdisabled&7."));
        }

        plugin.getStaffRequestManager().logAction(player.getName(), "RECORDING", enable ? "on" : "off", "Recording toggled");
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
