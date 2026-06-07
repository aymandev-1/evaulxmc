package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class NameTagCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public NameTagCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.nametag")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("all")) {
            refreshAll();
            sender.sendMessage(CC.color("&aNametags refreshed for all online players."));
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(CC.color("&7Nametags: " + (plugin.getConfig().getBoolean("nametags.enabled", true) ? "&aenabled" : "&cdisabled")));
            sender.sendMessage(CC.color("&7Scoreboard teams: " + (plugin.getNameTagManager().isScoreboardEnabled() ? "&aenabled" : "&cdisabled")));
            sender.sendMessage(CC.color("&7Tab list: " + (plugin.getNameTagManager().isTabListEnabled() ? "&aenabled" : "&cdisabled")));
            sender.sendMessage(CC.color("&7Hub hook: " + (plugin.getHubHook() != null && plugin.getHubHook().handlesNameTags() ? "&aowns nametags" : "&7not handling nametags")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        plugin.getNameTagManager().applyNameTag(target);
        sender.sendMessage(CC.color("&aRefreshed nametag for &f" + target.getName() + "&a."));
        return true;
    }

    private void refreshAll() {
        plugin.getNameTagManager().refreshAll();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.nametag")) return Collections.emptyList();
        if (args.length != 1) return Collections.emptyList();

        List<String> suggestions = new ArrayList<>();
        Collections.addAll(suggestions, "all", "reload", "status");
        for (Player player : Bukkit.getOnlinePlayers()) {
            suggestions.add(player.getName());
        }
        return filter(suggestions, args[0]);
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ENGLISH);
        List<String> filtered = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ENGLISH).startsWith(lower)) filtered.add(value);
        }
        Collections.sort(filtered);
        return filtered;
    }
}
