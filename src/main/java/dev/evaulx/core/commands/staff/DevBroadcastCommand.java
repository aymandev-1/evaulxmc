package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class DevBroadcastCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public DevBroadcastCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.devbroadcast")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /devbroadcast <message>"));
            return true;
        }

        String message = String.join(" ", args);
        String senderName = sender instanceof Player ? sender.getName() : "Console";

        String formatted = CC.color("&1[&9⚙ Dev&1] &7" + message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formatted);
            try { player.playSound(player.getLocation(), Sound.valueOf("BLOCK_NOTE_BLOCK_BASS"), 0.5f, 1.5f); } catch (Throwable ignored) {}
        }
        plugin.getServer().getConsoleSender().sendMessage(formatted);

        plugin.getStaffRequestManager().logAction(senderName, "DEV_BROADCAST", "all", message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
