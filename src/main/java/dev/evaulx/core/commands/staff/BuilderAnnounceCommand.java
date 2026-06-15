package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class BuilderAnnounceCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public BuilderAnnounceCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.builderannounce")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /builderannounce <message>"));
            return true;
        }

        String message = String.join(" ", args);
        String senderName = sender instanceof Player ? sender.getName() : "Builder";
        String formatted = CC.color("&8[&a⚙ &aBuilder&8] &7" + message + " &8— &a" + senderName);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formatted);
            try { player.playSound(player.getLocation(), Sound.valueOf("BLOCK_NOTE_BLOCK_PLING"), 0.6f, 1.4f); } catch (Throwable ignored) {}
        }
        plugin.getServer().getConsoleSender().sendMessage(formatted);

        plugin.getStaffRequestManager().logAction(senderName, "BUILDER_ANNOUNCE", "all", message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
