package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class MaintenanceCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public MaintenanceCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.maintenance")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player) {
                plugin.getGuiManager().openMaintenance((Player) sender);
            } else {
                sendStatus(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ENGLISH);
        if (sub.equals("gui") || sub.equals("menu")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(CC.color("&cPlayers only."));
                return true;
            }
            plugin.getGuiManager().openMaintenance((Player) sender);
            return true;
        }

        if (sub.equals("status")) {
            sendStatus(sender);
            return true;
        }

        if (sub.equals("on") || sub.equals("enable")) {
            String reason = args.length > 1 ? join(args, 1) : plugin.getConfig().getString("maintenance.reason", "Server maintenance");
            setEnabled(true, reason);
            sender.sendMessage(CC.color("&aMaintenance mode enabled. &7Reason: &f" + reason));
            plugin.getStaffRequestManager().logAction(sender.getName(), "MAINTENANCE_ON", "Server", reason);
            plugin.getDiscordManager().sendMaintenance(true, sender.getName(), reason);
            kickBlockedPlayers();
            return true;
        }

        if (sub.equals("off") || sub.equals("disable")) {
            setEnabled(false, plugin.getConfig().getString("maintenance.reason", "Server maintenance"));
            sender.sendMessage(CC.color("&cMaintenance mode disabled."));
            plugin.getStaffRequestManager().logAction(sender.getName(), "MAINTENANCE_OFF", "Server", "");
            plugin.getDiscordManager().sendMaintenance(false, sender.getName(), "");
            return true;
        }

        if (sub.equals("allow") || sub.equals("add")) {
            if (args.length < 2) return usage(sender, "/maintenance allow <player>");
            List<String> allowed = allowedPlayers();
            String name = args[1].toLowerCase(Locale.ENGLISH);
            if (!allowed.contains(name)) allowed.add(name);
            plugin.getConfig().set("maintenance.allowed-players", allowed);
            plugin.saveConfig();
            sender.sendMessage(CC.color("&aAllowed &f" + args[1] + " &aduring maintenance."));
            return true;
        }

        if (sub.equals("remove") || sub.equals("deny")) {
            if (args.length < 2) return usage(sender, "/maintenance remove <player>");
            List<String> allowed = allowedPlayers();
            allowed.remove(args[1].toLowerCase(Locale.ENGLISH));
            plugin.getConfig().set("maintenance.allowed-players", allowed);
            plugin.saveConfig();
            sender.sendMessage(CC.color("&cRemoved &f" + args[1] + " &cfrom the maintenance allow list."));
            return true;
        }

        if (sub.equals("list")) {
            List<String> allowed = allowedPlayers();
            sender.sendMessage(CC.color("&7Maintenance allowed players: &f" + (allowed.isEmpty() ? "none" : joinList(allowed))));
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cMaintenance Commands"));
        sender.sendMessage(CC.color("&f/maintenance &7- Open the maintenance GUI"));
        sender.sendMessage(CC.color("&f/maintenance status &7- Show current state"));
        sender.sendMessage(CC.color("&f/maintenance on [reason] &7- Enable maintenance"));
        sender.sendMessage(CC.color("&f/maintenance off &7- Disable maintenance"));
        sender.sendMessage(CC.color("&f/maintenance allow <player> &7- Allow a player by name"));
        sender.sendMessage(CC.color("&f/maintenance remove <player> &7- Remove an allowed player"));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cMaintenance Mode"));
        sender.sendMessage(CC.color("&7Enabled: " + (isEnabled() ? "&atrue" : "&cfalse")));
        sender.sendMessage(CC.color("&7Reason: &f" + plugin.getConfig().getString("maintenance.reason", "Server maintenance")));
        sender.sendMessage(CC.color("&7Bypass permission: &f" + plugin.getConfig().getString("maintenance.bypass-permission", "evaulx.maintenance.bypass")));
        sender.sendMessage(CC.color("&7Allowed players: &f" + allowedPlayers().size()));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void setEnabled(boolean enabled, String reason) {
        plugin.getConfig().set("maintenance.enabled", enabled);
        plugin.getConfig().set("maintenance.reason", reason);
        plugin.saveConfig();
    }

    private void kickBlockedPlayers() {
        if (!plugin.getConfig().getBoolean("maintenance.kick-online-players", true)) return;
        String message = CC.color(plugin.getConfig().getString("maintenance.kick-message",
                "&cEvaulxMC is currently in maintenance.\n&7Reason: &f{reason}")
                .replace("{reason}", plugin.getConfig().getString("maintenance.reason", "Server maintenance")));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (canJoinDuringMaintenance(player)) continue;
            player.kickPlayer(message);
        }
    }

    private boolean canJoinDuringMaintenance(Player player) {
        String bypass = plugin.getConfig().getString("maintenance.bypass-permission", "evaulx.maintenance.bypass");
        return player.hasPermission(bypass) || player.isOp()
                || allowedPlayers().contains(player.getName().toLowerCase(Locale.ENGLISH));
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("maintenance.enabled", false);
    }

    private List<String> allowedPlayers() {
        List<String> lower = new ArrayList<>();
        for (String name : plugin.getConfig().getStringList("maintenance.allowed-players")) {
            lower.add(name.toLowerCase(Locale.ENGLISH));
        }
        return lower;
    }

    private boolean usage(CommandSender sender, String usage) {
        sender.sendMessage(CC.color("&cUsage: " + usage));
        return true;
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private String joinList(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(value);
        }
        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.maintenance")) return Collections.emptyList();
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            Collections.addAll(suggestions, "gui", "status", "on", "off", "allow", "remove", "list");
            return filter(suggestions, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("allow") || args[0].equalsIgnoreCase("remove"))) {
            for (Player player : Bukkit.getOnlinePlayers()) suggestions.add(player.getName());
            suggestions.addAll(plugin.getPlayerLookupManager().suggest(args[1], 20));
            return filter(suggestions, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ENGLISH);
        List<String> filtered = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ENGLISH).startsWith(lower) && !filtered.contains(value)) filtered.add(value);
        }
        Collections.sort(filtered, String.CASE_INSENSITIVE_ORDER);
        return filtered;
    }
}
