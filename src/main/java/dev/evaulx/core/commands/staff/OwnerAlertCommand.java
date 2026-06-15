package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OwnerAlertCommand implements CommandExecutor, TabCompleter {

    private static final String BORDER = "&4&m=================================================="; // 50 chars

    private final EvaulxCore plugin;

    public OwnerAlertCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.owner.alert")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /owneralert <message>"));
            return true;
        }

        String message = String.join(" ", args);
        String senderName = sender instanceof Player ? sender.getName() : "Owner";

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(CC.color(""));
            player.sendMessage(CC.color(BORDER));
            player.sendMessage(CC.color("         &4&l⚠ OWNER ALERT ⚠"));
            player.sendMessage(CC.color(""));
            player.sendMessage(CC.color("  &f" + message));
            player.sendMessage(CC.color(""));
            player.sendMessage(CC.color("  &8— &4" + senderName));
            player.sendMessage(CC.color(BORDER));
            player.sendMessage(CC.color(""));
            player.sendTitle(
                    CC.color("&4&l⚠ OWNER ALERT ⚠"),
                    CC.color("&c" + message)
            );
            try {
                player.playSound(player.getLocation(), Sound.valueOf("ENTITY_ENDER_DRAGON_GROWL"), 0.6f, 1.0f);
            } catch (Throwable ignored) {}
        }

        plugin.getStaffRequestManager().logAction(senderName, "OWNER_ALERT", "all", message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
