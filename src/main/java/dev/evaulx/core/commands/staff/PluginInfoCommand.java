package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Lists installed plugins, or shows details for one. Developer tool.
 * Usage: /plugininfo [plugin]
 */
public class PluginInfoCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public PluginInfoCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.dev.plugininfo")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length == 0) {
            Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
            List<String> names = new ArrayList<>();
            for (Plugin p : plugins) {
                names.add((p.isEnabled() ? "&a" : "&c") + p.getName());
            }
            sender.sendMessage(CC.color("&8&m----------------------------------------"));
            sender.sendMessage(CC.color("  &9&lPlugins &7(" + plugins.length + ")"));
            sender.sendMessage(CC.color("  " + String.join("&7, ", names)));
            sender.sendMessage(CC.color("&8&m----------------------------------------"));
            return true;
        }

        Plugin target = Bukkit.getPluginManager().getPlugin(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cNo plugin named &f" + args[0] + "&c found."));
            return true;
        }

        sender.sendMessage(CC.color("&8&m----------------------------------------"));
        sender.sendMessage(CC.color("  &9&l" + target.getName()));
        sender.sendMessage(CC.color("  &7Version: &f" + target.getDescription().getVersion()));
        sender.sendMessage(CC.color("  &7Enabled: " + (target.isEnabled() ? "&ayes" : "&cno")));
        sender.sendMessage(CC.color("  &7Authors: &f" + String.join(", ", target.getDescription().getAuthors())));
        sender.sendMessage(CC.color("  &7Main: &f" + target.getDescription().getMain()));
        sender.sendMessage(CC.color("&8&m----------------------------------------"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ENGLISH);
            List<String> names = new ArrayList<>();
            for (Plugin p : Bukkit.getPluginManager().getPlugins()) names.add(p.getName());
            return names.stream().filter(n -> n.toLowerCase(Locale.ENGLISH).startsWith(prefix)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
