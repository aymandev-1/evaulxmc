package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class HomeCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public HomeCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        Player player = (Player) sender;
        String lbl = label.toLowerCase();

        if (lbl.equals("sethome")) return handleSet(player, args.length > 0 ? args[0] : "home");
        if (lbl.equals("delhome") || lbl.equals("deletehome")) return handleDelete(player, args.length > 0 ? args[0] : "home");
        if (lbl.equals("homes")) return handleList(player);

        if (!sender.hasPermission("evaulx.home")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        String name = args.length > 0 ? args[0] : "home";
        Location home = plugin.getHomeManager().getHome(player.getUniqueId(), name);
        if (home == null) {
            player.sendMessage(CC.color("&cHome &f" + name + " &cdoes not exist. Use &f/sethome " + name + "&c."));
            return true;
        }
        player.teleport(home);
        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &aTeleported to home &f" + name + "&a."));
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean handleSet(Player player, String name) {
        if (!player.hasPermission("evaulx.home.set")) {
            plugin.getMessageManager().send(player, "no-permission", "&cNo permission.");
            return true;
        }
        if (!name.matches("[A-Za-z0-9_-]{1,20}")) {
            player.sendMessage(CC.color("&cHome name must be 1-20 alphanumeric characters."));
            return true;
        }
        int limit = plugin.getHomeManager().getHomeLimit(player);
        boolean exists = plugin.getHomeManager().getHome(player.getUniqueId(), name) != null;
        if (!exists && plugin.getHomeManager().getHomeCount(player.getUniqueId()) >= limit) {
            player.sendMessage(CC.color("&cHome limit reached &8(&f" + limit + "&8)&c. Delete one with &f/delhome&c."));
            return true;
        }
        plugin.getHomeManager().setHome(player.getUniqueId(), name, player.getLocation());
        player.sendMessage(CC.color("&aHome &f" + name + " &aset."));
        return true;
    }

    private boolean handleDelete(Player player, String name) {
        if (!player.hasPermission("evaulx.home.delete")) {
            plugin.getMessageManager().send(player, "no-permission", "&cNo permission.");
            return true;
        }
        if (plugin.getHomeManager().deleteHome(player.getUniqueId(), name)) {
            player.sendMessage(CC.color("&aHome &f" + name + " &adeleted."));
        } else {
            player.sendMessage(CC.color("&cHome &f" + name + " &cnot found."));
        }
        return true;
    }

    private boolean handleList(Player player) {
        if (!player.hasPermission("evaulx.home")) {
            plugin.getMessageManager().send(player, "no-permission", "&cNo permission.");
            return true;
        }
        List<String> names = plugin.getHomeManager().getHomeNames(player.getUniqueId());
        int limit = plugin.getHomeManager().getHomeLimit(player);
        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &aYour Homes &8(&f" + names.size() + "&8/&f" + limit + "&8)"));
        if (names.isEmpty()) {
            player.sendMessage(CC.color("  &7No homes set. Use &f/sethome [name]&7."));
        } else {
            for (String n : names) {
                Location loc = plugin.getHomeManager().getHome(player.getUniqueId(), n);
                String coords = loc != null
                        ? " &8[&7" + (int) loc.getX() + ", " + (int) loc.getY() + ", " + (int) loc.getZ() + "&8]"
                        : "";
                player.sendMessage(CC.color("  &a▸ &f" + n + coords));
            }
        }
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) { return Collections.emptyList(); }
        String lbl = alias.toLowerCase();
        if (args.length == 1 && (lbl.equals("home") || lbl.equals("delhome") || lbl.equals("deletehome") || lbl.equals("homes"))) {
            return plugin.getHomeManager().getHomeNames(((Player) sender).getUniqueId()).stream()
                    .filter(n -> n.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}






