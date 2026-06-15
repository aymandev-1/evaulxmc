package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class IgnoreCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public IgnoreCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("evaulx.ignore")) {
            player.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null) {
            player.sendMessage(CC.color("&cYour profile is not loaded."));
            return true;
        }

        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("list"))) {
            List<String> ignored = profile.getIgnoredPlayers();
            if (ignored.isEmpty()) {
                player.sendMessage(CC.color("&7You are not ignoring anyone."));
            } else {
                player.sendMessage(CC.color("&7Ignored players &8(" + ignored.size() + "&8):"));
                for (String uuidStr : ignored) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        Player online = Bukkit.getPlayer(uuid);
                        String name = online != null ? online.getName() : uuidStr.substring(0, 8) + "...";
                        player.sendMessage(CC.color("  &8» &f" + name));
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(CC.color("  &8» &f" + uuidStr));
                    }
                }
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(CC.color("&cPlayer &f" + args[0] + " &cis not online."));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(CC.color("&cYou cannot ignore yourself."));
            return true;
        }

        if (profile.isIgnoring(target.getUniqueId())) {
            profile.removeIgnoredPlayer(target.getUniqueId());
            plugin.getPlayerManager().saveProfile(profile);
            player.sendMessage(CC.color("&7You are no longer ignoring &f" + target.getName() + "&7."));
        } else {
            profile.addIgnoredPlayer(target.getUniqueId());
            plugin.getPlayerManager().saveProfile(profile);
            player.sendMessage(CC.color("&7You are now ignoring &f" + target.getName() + "&7. They cannot message you."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.ignore")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            names.add("list");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender)) names.add(p.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
