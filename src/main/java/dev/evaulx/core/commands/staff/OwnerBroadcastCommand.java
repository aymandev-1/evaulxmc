package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class OwnerBroadcastCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public OwnerBroadcastCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.ownerbc")) {
            sender.sendMessage(CC.color("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /ownerbc <message>"));
            return true;
        }

        String message = String.join(" ", args);
        String separator = CC.color("&4&m                                                ");
        String formatted  = CC.color("&4&l[&c&lOwner&4&l] &f" + message);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(CC.color(" "));
            p.sendMessage(separator);
            p.sendMessage(formatted);
            p.sendMessage(separator);
            p.sendMessage(CC.color(" "));
            try {
                p.playSound(p.getLocation(), Sound.valueOf("BLOCK_NOTE_BLOCK_BELL"), 1.0f, 0.5f);
            } catch (Throwable ignored) {}
        }

        Bukkit.getConsoleSender().sendMessage(formatted);
        return true;
    }
}
