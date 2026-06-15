package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CreatorAnnounceCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public CreatorAnnounceCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.ccannounce") && !sender.hasPermission("evaulx.creator")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /ccannounce <message>"));
            return true;
        }

        String message = String.join(" ", args);
        String senderName = sender instanceof Player ? sender.getName() : "Creator";

        String line1 = CC.color("&d&m              &r &5✦ &d&l" + senderName + " &5✦ &d&m              ");
        String line2 = CC.color("  &f" + message);
        String line3 = CC.color("&d&m              &r &5✦ &d&m              ");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(CC.color(""));
            player.sendMessage(line1);
            player.sendMessage(line2);
            player.sendMessage(line3);
            player.sendMessage(CC.color(""));
            try { player.playSound(player.getLocation(), Sound.valueOf("ENTITY_EXPERIENCE_ORB_PICKUP"), 0.7f, 1.8f); } catch (Throwable ignored) {}
        }

        plugin.getStaffRequestManager().logAction(senderName, "CREATOR_ANNOUNCE", "all", message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
