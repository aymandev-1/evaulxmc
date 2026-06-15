package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ForcedisguiseCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public ForcedisguiseCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.disguise.admin")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /forcedisguise <player> <name> [skin] [rank]"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "player-not-online", "&cPlayer not online.");
            return true;
        }

        String name = args[1];
        String skin = args.length > 2 ? args[2] : null;
        String rank = args.length > 3 ? args[3] : null;

        plugin.getDisguiseManager().forceDisguise(sender, target, name, skin, rank);
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

        if (args.length == 3) {
            String input = args[2].toLowerCase(Locale.ENGLISH);
            return plugin.getConfig().getStringList("disguise.skins").stream()
                    .filter(s -> s.toLowerCase(Locale.ENGLISH).startsWith(input))
                    .collect(Collectors.toList());
        }

        if (args.length == 4) {
            String input = args[3].toLowerCase(Locale.ENGLISH);
            List<String> ranks = new ArrayList<>();
            for (dev.evaulx.core.models.Rank r : plugin.getRankManager().getRanks()) ranks.add(r.getName());
            ranks.removeIf(r -> !r.toLowerCase(Locale.ENGLISH).startsWith(input));
            return ranks;
        }

        return Collections.emptyList();
    }
}
