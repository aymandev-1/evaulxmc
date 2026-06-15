package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Lets a content creator shine a server-wide spotlight on another player.
 * Usage: /spotlight <player> [message]
 */
public class SpotlightCommand implements CommandExecutor, TabCompleter {

    private static final String BORDER = "&d&m==================================================";

    private final EvaulxCore plugin;

    public SpotlightCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.creator.spotlight")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /spotlight <player> [message]"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "player-not-found", "&cPlayer not found.");
            return true;
        }

        String creator = sender instanceof Player ? sender.getName() : "Creator";
        String message = args.length > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : "Show them some love!";

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(CC.color(""));
            player.sendMessage(CC.color(BORDER));
            player.sendMessage(CC.color("        &d&l✦ CREATOR SPOTLIGHT ✦"));
            player.sendMessage(CC.color(""));
            player.sendMessage(CC.color("  &f" + creator + " &7is spotlighting &d" + target.getName() + "&7!"));
            player.sendMessage(CC.color("  &7" + message));
            player.sendMessage(CC.color(BORDER));
            player.sendMessage(CC.color(""));
            try { player.playSound(player.getLocation(), Sound.valueOf("LEVEL_UP"), 0.5f, 1.6f); } catch (Throwable ignored) {}
        }
        target.sendTitle(CC.color("&d&l✦ SPOTLIGHT ✦"), CC.color("&7" + creator + " spotlighted you!"));

        plugin.getStaffRequestManager().logAction(creator, "SPOTLIGHT", target.getName(), message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ENGLISH);
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names.stream().filter(n -> n.toLowerCase(Locale.ENGLISH).startsWith(prefix)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
