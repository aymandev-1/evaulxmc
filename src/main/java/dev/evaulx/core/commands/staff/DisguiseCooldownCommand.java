package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DisguiseCooldownCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public DisguiseCooldownCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.disguise.admin")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /disguisecooldown <player> [reset]"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "player-not-online", "&cPlayer not online.");
            return true;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("reset")) {
            plugin.getDisguiseManager().resetCooldown(target.getUniqueId());
            sender.sendMessage(CC.color("&aReset disguise cooldown for &f" + target.getName() + "&a."));
            return true;
        }

        long remaining = plugin.getDisguiseManager().getCooldownRemaining(target);
        if (remaining <= 0L) {
            sender.sendMessage(CC.color("&f" + target.getName() + " &7has no active disguise cooldown."));
        } else {
            sender.sendMessage(CC.color("&f" + target.getName()
                    + " &7has &f" + ((remaining + 999L) / 1000L) + "s &7remaining on their disguise cooldown."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.disguise.admin")) return Collections.emptyList();

        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ENGLISH);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ENGLISH).startsWith(input))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if ("reset".startsWith(args[1].toLowerCase(Locale.ENGLISH))) return Collections.singletonList("reset");
        }

        return Collections.emptyList();
    }
}
