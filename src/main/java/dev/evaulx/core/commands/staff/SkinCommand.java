package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SkinCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public SkinCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }

        if (!plugin.getConfig().getBoolean("disguise.enabled", true)) {
            plugin.getMessageManager().send(sender, "disguise.disabled", "&cDisguises are currently disabled.");
            return true;
        }

        if (!canUseSkin(sender)) {
            plugin.getMessageManager().send(sender, "disguise.skin-permission",
                    "&cYou do not have permission to choose disguise skins.");
            return true;
        }

        String commandName = label == null ? "skin" : label.toLowerCase(Locale.ENGLISH);
        if (args.length != 1) {
            plugin.getMessageManager().send(sender, "disguise.skin-usage",
                    "&cUsage: /{command} <skin>",
                    plugin.getMessageManager().placeholders("{command}", commandName));
            return true;
        }

        plugin.getDisguiseManager().changeSkin((Player) sender, args[0]);
        return true;
    }

    private boolean canUseSkin(CommandSender sender) {
        return sender.hasPermission("evaulx.skin")
                || sender.hasPermission("evaulx.disguise.skin")
                || sender.hasPermission("evaulx.nick.skin")
                || sender.hasPermission("evaulx.disguise.admin");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !canUseSkin(sender)) return Collections.emptyList();
        return filter(plugin.getConfig().getStringList("disguise.skins"), args[0]);
    }

    private List<String> filter(List<String> values, String input) {
        String lowered = input == null ? "" : input.toLowerCase(Locale.ENGLISH);
        List<String> results = new ArrayList<>();
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ENGLISH).startsWith(lowered)) results.add(value);
        }
        Collections.sort(results);
        return results;
    }
}
