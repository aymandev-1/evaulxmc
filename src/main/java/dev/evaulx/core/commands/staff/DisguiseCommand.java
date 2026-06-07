package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DisguiseCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public DisguiseCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = label == null ? "disguise" : label.toLowerCase(Locale.ENGLISH);
        boolean nickCommand = commandName.equals("nick");

        if (args.length > 0 && isAdminSubcommand(args[0])) {
            return handleAdmin(sender, commandName, args);
        }

        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }

        if (!plugin.getConfig().getBoolean("disguise.enabled", true)) {
            plugin.getMessageManager().send(sender, "disguise.disabled", "&cDisguises are currently disabled.");
            return true;
        }

        String basePermission = nickCommand ? "evaulx.nick" : "evaulx.disguise";
        if (!sender.hasPermission(basePermission)) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            if (nickCommand) {
                plugin.getDisguiseManager().openNickSelector(player);
                return true;
            }
            if (!canUseSkin(sender, nickCommand) || !canUseRank(sender, nickCommand)) return true;
            plugin.getDisguiseManager().openSkinSelector(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("random")) {
            String rank = args.length > 1 ? args[1] : null;
            if (!canUseSkin(sender, nickCommand)) return true;
            if (rank != null && !canUseRank(sender, nickCommand)) return true;
            plugin.getDisguiseManager().randomDisguise(player, rank, commandName);
            return true;
        }

        if (isOffSubcommand(args[0])) {
            if (!plugin.getDisguiseManager().undisguise(player)) {
                plugin.getMessageManager().send(sender, "disguise.not-disguised", "&cYou are not disguised.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("skin")) {
            if (args.length != 2) {
                plugin.getMessageManager().send(sender, "disguise.skin-usage",
                        "&cUsage: /{command} skin <skin>",
                        plugin.getMessageManager().placeholders("{command}", commandName));
                return true;
            }
            if (!canUseSkin(sender, nickCommand)) return true;
            plugin.getDisguiseManager().changeSkin(player, args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("rank")) {
            if (args.length != 2) {
                plugin.getMessageManager().send(sender, "disguise.rank-usage",
                        "&cUsage: /{command} rank <rank|none>",
                        plugin.getMessageManager().placeholders("{command}", commandName));
                return true;
            }
            if (!canUseRank(sender, nickCommand)) return true;
            plugin.getDisguiseManager().changeRank(player, args[1]);
            return true;
        }

        if (args.length > 3) {
            plugin.getMessageManager().send(sender, "disguise.usage",
                    "&cUsage: /{command} <name|random|skin|rank|off|reload|clearskincache|debug|test|refresh|status|menu> [skin] [rank]",
                    plugin.getMessageManager().placeholders("{command}", commandName));
            return true;
        }

        String name = args[0];
        String skin = null;
        String rank = null;

        if (args.length > 1) {
            if (!canUseSkin(sender, nickCommand)) return true;
            skin = args[1];
        } else if (nickCommand) {
            skin = player.getName();
        } else {
            skin = name;
        }

        if (args.length > 2) {
            if (!canUseRank(sender, nickCommand)) return true;
            rank = args[2];
        }

        plugin.getDisguiseManager().disguise(player, name, skin, rank, commandName);
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String commandName, String[] args) {
        if (!sender.hasPermission("evaulx.disguise.admin")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        String subcommand = args[0];
        if (subcommand.equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getMessageManager().reload();
            if (plugin.getProtocolLibHook() != null) plugin.getProtocolLibHook().load();
            plugin.getMessageManager().send(sender, "disguise.reloaded", "&aDisguise settings reloaded.");
            return true;
        }

        if (subcommand.equalsIgnoreCase("debug")) {
            String target = args.length > 1 ? args[1] : sender instanceof Player ? sender.getName() : null;
            if (target == null) {
                plugin.getMessageManager().send(sender, "disguise.debug-usage",
                        "&cUsage: /{command} debug <player>",
                        plugin.getMessageManager().placeholders("{command}", commandName));
                return true;
            }

            plugin.getDisguiseManager().sendDebug(sender, target);
            return true;
        }

        if (subcommand.equalsIgnoreCase("test") || subcommand.equalsIgnoreCase("doctor")) {
            if (args.length < 2) {
                plugin.getMessageManager().send(sender, "disguise.test-usage",
                        "&cUsage: /{command} test <skin> [rank]",
                        plugin.getMessageManager().placeholders("{command}", commandName));
                return true;
            }

            plugin.getDisguiseManager().testDisguise(sender, args[1], args.length > 2 ? args[2] : null);
            return true;
        }

        if (subcommand.equalsIgnoreCase("status")) {
            plugin.getDisguiseManager().sendStatus(sender);
            return true;
        }

        if (subcommand.equalsIgnoreCase("refresh")) {
            String target = args.length > 1 ? args[1] : sender instanceof Player ? sender.getName() : null;
            if (target == null) {
                plugin.getMessageManager().send(sender, "disguise.refresh-usage",
                        "&cUsage: /{command} refresh <player>",
                        plugin.getMessageManager().placeholders("{command}", commandName));
                return true;
            }

            plugin.getDisguiseManager().refreshDisguise(sender, target);
            return true;
        }

        if (subcommand.equalsIgnoreCase("menu") || subcommand.equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player)) {
                plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
                return true;
            }

            plugin.getDisguiseManager().openAdminGui((Player) sender);
            return true;
        }

        int cleared = plugin.getDisguiseManager().clearSkinCache();
        plugin.getMessageManager().send(sender, "disguise.skin-cache-cleared",
                "&aDisguise skin cache cleared. &7Removed &f{count}&7 cached skin(s).",
                plugin.getMessageManager().placeholders("{count}", String.valueOf(cleared), "{command}", commandName));
        return true;
    }

    private boolean canUseSkin(CommandSender sender, boolean nickCommand) {
        String permission = nickCommand ? "evaulx.nick.skin" : "evaulx.disguise.skin";
        if (sender.hasPermission(permission) || sender.hasPermission("evaulx.disguise.admin")) return true;
        plugin.getMessageManager().send(sender, "disguise.skin-permission",
                "&cYou do not have permission to choose disguise skins.");
        return false;
    }

    private boolean canUseRank(CommandSender sender, boolean nickCommand) {
        String permission = nickCommand ? "evaulx.nick.rank" : "evaulx.disguise.rank";
        if (sender.hasPermission(permission) || sender.hasPermission("evaulx.disguise.admin")) return true;
        plugin.getMessageManager().send(sender, "disguise.rank-permission",
                "&cYou do not have permission to choose disguise ranks.");
        return false;
    }

    private boolean isAdminSubcommand(String value) {
        return value.equalsIgnoreCase("reload")
                || value.equalsIgnoreCase("debug")
                || value.equalsIgnoreCase("test")
                || value.equalsIgnoreCase("doctor")
                || value.equalsIgnoreCase("status")
                || value.equalsIgnoreCase("refresh")
                || value.equalsIgnoreCase("menu")
                || value.equalsIgnoreCase("gui")
                || value.equalsIgnoreCase("clearskincache")
                || value.equalsIgnoreCase("clearcache");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String commandName = alias == null ? "disguise" : alias.toLowerCase(Locale.ENGLISH);
        boolean nickCommand = commandName.equals("nick");

        if (args.length == 1) {
            List<String> values = new ArrayList<>(Arrays.asList("random", "skin", "rank", "off"));
            if (sender.hasPermission("evaulx.disguise.admin")) {
                values.add("reload");
                values.add("clearskincache");
                values.add("debug");
                values.add("test");
                values.add("refresh");
                values.add("status");
                values.add("menu");
            }
            if (!nickCommand) values.addAll(plugin.getConfig().getStringList("disguise.random-names"));
            return filter(values, args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("debug")) return filter(onlineNames(), args[1]);
            if (args[0].equalsIgnoreCase("refresh")) return filter(onlineNames(), args[1]);
            if (args[0].equalsIgnoreCase("test")) return filter(plugin.getConfig().getStringList("disguise.skins"), args[1]);
            if (args[0].equalsIgnoreCase("skin")) return filter(plugin.getConfig().getStringList("disguise.skins"), args[1]);
            if (args[0].equalsIgnoreCase("rank")) return rankNames(args[1], true);
            if (isOffSubcommand(args[0])) return Collections.emptyList();
            if (isAdminSubcommand(args[0])) return Collections.emptyList();
            if (args[0].equalsIgnoreCase("random")) return rankNames(args[1], false);
            return filter(plugin.getConfig().getStringList("disguise.skins"), args[1]);
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("test")) return rankNames(args[2], false);
            if (args[0].equalsIgnoreCase("skin") || args[0].equalsIgnoreCase("rank") || isOffSubcommand(args[0])) {
                return Collections.emptyList();
            }
            return rankNames(args[2], false);
        }

        return Collections.emptyList();
    }

    private List<String> rankNames(String input, boolean includeNone) {
        List<String> values = new ArrayList<>();
        if (includeNone) values.add("none");
        for (dev.evaulx.core.models.Rank rank : plugin.getRankManager().getRanks()) values.add(rank.getName());
        return filter(values, input);
    }

    private boolean isOffSubcommand(String value) {
        return value.equalsIgnoreCase("off")
                || value.equalsIgnoreCase("clear")
                || value.equalsIgnoreCase("reset")
                || value.equalsIgnoreCase("remove");
    }

    private List<String> onlineNames() {
        List<String> values = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            values.add(player.getName());
            String visibleName = plugin.getDisguiseManager().getVisibleName(player);
            if (visibleName != null && !visibleName.equalsIgnoreCase(player.getName())) values.add(visibleName);
        }
        return values;
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
