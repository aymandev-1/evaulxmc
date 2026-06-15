package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class NearCommand implements CommandExecutor, TabCompleter {

    private static final int DEFAULT_RADIUS = 200;
    private static final int MAX_RADIUS = 1000;

    private final EvaulxCore plugin;

    public NearCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (!sender.hasPermission("evaulx.near")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        int radius = DEFAULT_RADIUS;
        if (args.length > 0) {
            try {
                radius = Math.min(MAX_RADIUS, Math.max(1, Integer.parseInt(args[0])));
            } catch (NumberFormatException e) {
                player.sendMessage(CC.color("&cRadius must be a number."));
                return true;
            }
        }

        final int r = radius;
        List<Player> nearby = player.getWorld().getPlayers().stream()
                .filter(p -> !p.equals(player) && p.getLocation().distance(player.getLocation()) <= r)
                .sorted(Comparator.comparingDouble(p -> p.getLocation().distance(player.getLocation())))
                .collect(Collectors.toList());

        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &aNearby Players &8(radius: &f" + r + "&8) &8— &f" + nearby.size() + " found"));
        if (nearby.isEmpty()) {
            player.sendMessage(CC.color("  &7No players nearby."));
        } else {
            for (Player p : nearby) {
                int dist = (int) p.getLocation().distance(player.getLocation());
                player.sendMessage(CC.color("  &7▸ &f" + p.getName() + " &8[&7" + dist + "m&8]"));
            }
        }
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
