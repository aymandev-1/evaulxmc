package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public HomeCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!sender.hasPermission("evaulx.home")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        Player player = (Player) sender;
        List<String> names = plugin.getHomesManager().getHomeNames(player.getUniqueId());
        if (names.isEmpty()) {
            player.sendMessage(CC.color("&cYou have no homes set. Use &f/sethome <name>&c."));
            return true;
        }

        String name;
        if (args.length == 0) {
            if (names.size() > 1) {
                player.sendMessage(CC.color("&7Your homes &8(" + names.size() + ")&7: &f" + String.join("&7, &f", names)));
                player.sendMessage(CC.color("&7Use &f/home <name>&7."));
                return true;
            }
            name = names.get(0);
        } else {
            name = args[0];
        }

        Location home = plugin.getHomesManager().getHome(player.getUniqueId(), name);
        if (home == null) {
            player.sendMessage(CC.color("&cHome &f" + name + " &cnot found."));
            return true;
        }
        if (home.getWorld() == null) {
            player.sendMessage(CC.color("&cThat home's world is not loaded."));
            return true;
        }

        plugin.getEssentialsManager().setBackLocation(player, player.getLocation());
        player.teleport(home);
        player.sendMessage(CC.color("&7Teleported to home &f" + name + "&7."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1 && sender instanceof Player) {
            String prefix = args[0].toLowerCase(Locale.ENGLISH);
            for (String name : plugin.getHomesManager().getHomeNames(((Player) sender).getUniqueId())) {
                if (name.toLowerCase(Locale.ENGLISH).startsWith(prefix)) result.add(name);
            }
        }
        return result;
    }
}
