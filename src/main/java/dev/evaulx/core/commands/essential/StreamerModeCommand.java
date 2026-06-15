package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class StreamerModeCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public StreamerModeCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can use this command."));
            return true;
        }
        if (!sender.hasPermission("evaulx.streamermode")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        Player player = (Player) sender;
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null) {
            sender.sendMessage(CC.color("&cYour profile is not loaded."));
            return true;
        }

        if (args.length == 0) {
            plugin.getGuiManager().openStreamerMode(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            if (profile.isStreamerMode()) disable(player, profile);
            else enable(player, profile, null);
            return true;
        }

        if (args[0].equalsIgnoreCase("refresh")) {
            if (!profile.isStreamerMode()) {
                enable(player, profile, null);
            } else {
                String newAlias = pickAlias(player);
                profile.setStreamerAlias(newAlias);
                plugin.getNameTagManager().applyNameTag(player);
                player.sendMessage(CC.color("&8[&dStreamer&8] &7Your alias is now &f" + newAlias + "&7."));
                broadcastStaff(player, profile, true);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            if (!profile.isStreamerMode()) {
                sender.sendMessage(CC.color("&cStreamer mode is already off."));
                return true;
            }
            disable(player, profile);
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            if (profile.isStreamerMode()) {
                sender.sendMessage(CC.color("&7Streamer mode is already on. Alias: &f" + profile.getStreamerAlias()
                        + " &7| Use &f/streamermode alias <name> &7to change it."));
                return true;
            }
            enable(player, profile, null);
            return true;
        }

        if (args[0].equalsIgnoreCase("alias") && args.length > 1) {
            String newAlias = args[1];
            if (!newAlias.matches("[a-zA-Z0-9_]{3,16}")) {
                sender.sendMessage(CC.color("&cAlias must be 3-16 letters, numbers, or underscores."));
                return true;
            }
            if (isAliasInUse(newAlias, player)) {
                sender.sendMessage(CC.color("&cThat alias is already in use by someone else."));
                return true;
            }
            enable(player, profile, newAlias);
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            if (profile.isStreamerMode()) {
                sender.sendMessage(CC.color("&8[&dStreamer&8] &7Status: &aON &7| Alias: &f" + profile.getStreamerAlias()));
            } else {
                sender.sendMessage(CC.color("&8[&dStreamer&8] &7Status: &cOFF"));
            }
            return true;
        }

        sender.sendMessage(CC.color("&cUsage: /streamermode [on|off|toggle|refresh|alias <name>|status]"));
        return true;
    }

    private void enable(Player player, PlayerProfile profile, String requestedAlias) {
        String alias = requestedAlias != null ? requestedAlias : pickAlias(player);
        profile.setStreamerMode(true);
        profile.setStreamerAlias(alias);
        plugin.getPlayerManager().saveProfile(profile);
        plugin.getNameTagManager().applyNameTag(player);
        player.sendMessage(CC.color("&8[&dStreamer&8] &7Streamer mode &aenabled&7. You appear as &f" + alias + " &7in chat and tab."));
        player.sendMessage(CC.color("&8[&dStreamer&8] &7Staff can still see your real name with &f/realname " + alias + "&7."));
        broadcastStaff(player, profile, true);
    }

    private void disable(Player player, PlayerProfile profile) {
        profile.setStreamerMode(false);
        profile.setStreamerAlias(null);
        plugin.getPlayerManager().saveProfile(profile);
        plugin.getNameTagManager().applyNameTag(player);
        player.sendMessage(CC.color("&8[&dStreamer&8] &7Streamer mode &cdisabled&7. Your real name is now visible."));
        broadcastStaff(player, profile, false);
    }

    private void broadcastStaff(Player player, PlayerProfile profile, boolean on) {
        String alias = profile.getStreamerAlias();
        String msg = on
                ? "&8[&dStreamer&8] &f" + player.getName() + " &7enabled streamer mode &8(alias: &f" + alias + "&8)&7."
                : "&8[&dStreamer&8] &f" + player.getName() + " &7disabled streamer mode.";
        plugin.getStaffRequestManager().broadcastStaff(msg, "evaulx.streamermode.see");
        plugin.getStaffRequestManager().logAction(player.getName(),
                on ? "STREAMER_ON" : "STREAMER_OFF",
                on ? alias : "off",
                "Streamer mode " + (on ? "enabled" : "disabled"));
    }

    private String pickAlias(Player self) {
        List<String> pool = plugin.getConfig().getStringList("streamer-mode.alias-pool");
        if (!pool.isEmpty()) {
            List<String> shuffled = new ArrayList<>(pool);
            Collections.shuffle(shuffled);
            for (String candidate : shuffled) {
                if (candidate != null && !candidate.trim().isEmpty() && !isAliasInUse(candidate, self)) {
                    return candidate;
                }
            }
        }
        // Fallback: random generic alias
        return "Streamer" + (int) (Math.random() * 9000 + 1000);
    }

    private boolean isAliasInUse(String alias, Player self) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(self)) continue;
            PlayerProfile p = plugin.getPlayerManager().getProfile(online);
            if (p != null && p.isStreamerMode() && alias.equalsIgnoreCase(p.getStreamerAlias())) return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.streamermode")) return Collections.emptyList();
        if (args.length == 1) {
            return filter(Arrays.asList("on", "off", "toggle", "refresh", "alias", "status"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("alias")) {
            List<String> pool = plugin.getConfig().getStringList("streamer-mode.alias-pool");
            return filter(pool, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase(Locale.ENGLISH);
        List<String> result = new ArrayList<>();
        for (String s : list) if (s.toLowerCase(Locale.ENGLISH).startsWith(lower)) result.add(s);
        return result;
    }
}
