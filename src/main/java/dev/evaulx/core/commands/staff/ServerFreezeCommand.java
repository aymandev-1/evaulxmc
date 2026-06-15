package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ServerFreezeCommand implements CommandExecutor, TabCompleter {

    private static volatile boolean FROZEN = false;

    private final EvaulxCore plugin;

    public ServerFreezeCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public static boolean isFrozen() {
        return FROZEN;
    }

    public static void applyFreezeEffects(Player player) {
        if (!FROZEN) return;
        if (player.hasPermission("evaulx.serverfreeze.exempt") || player.hasPermission("evaulx.staff")) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE / 2, 7, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE / 2, 200, false, false));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.serverfreeze")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        boolean enable;
        if (args.length > 0) {
            String arg = args[0].toLowerCase(Locale.ENGLISH);
            if (arg.equals("on") || arg.equals("true") || arg.equals("enable")) {
                enable = true;
            } else if (arg.equals("off") || arg.equals("false") || arg.equals("disable")) {
                enable = false;
            } else {
                sender.sendMessage(CC.color("&cUsage: /serverfreeze [on|off]"));
                return true;
            }
        } else {
            enable = !FROZEN;
        }

        if (enable == FROZEN) {
            sender.sendMessage(CC.color("&7Server freeze is already " + (FROZEN ? "&cenabled" : "&adisabled") + "&7."));
            return true;
        }

        FROZEN = enable;
        String senderName = sender instanceof Player ? sender.getName() : "Console";

        if (enable) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("evaulx.serverfreeze.exempt") || player.hasPermission("evaulx.staff")) continue;
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE / 2, 7, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE / 2, 200, false, false));
                player.sendMessage(CC.color("&8[&c!&8] &cThe server has been frozen by staff."));
                try { player.playSound(player.getLocation(), Sound.valueOf("BLOCK_ANVIL_LAND"), 0.5f, 0.5f); } catch (Throwable ignored) {}
            }
            broadcastStaff(senderName, "&8[&cServer Freeze&8] &cEnabled &7by &f" + senderName + "&7. Non-staff players are frozen.");
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.removePotionEffect(PotionEffectType.SLOW);
                player.removePotionEffect(PotionEffectType.JUMP);
                if (!player.hasPermission("evaulx.staff")) {
                    player.sendMessage(CC.color("&8[&a✔&8] &aThe server freeze has been lifted."));
                    try { player.playSound(player.getLocation(), Sound.valueOf("ENTITY_PLAYER_LEVELUP"), 0.5f, 1.2f); } catch (Throwable ignored) {}
                }
            }
            broadcastStaff(senderName, "&8[&aServer Freeze&8] &aDisabled &7by &f" + senderName + "&7. Players may move freely.");
        }

        sender.sendMessage(CC.color("&7Server freeze &f" + (enable ? "&cenabled" : "&adisabled") + "&7."));
        plugin.getStaffRequestManager().logAction(senderName, "SERVER_FREEZE", enable ? "on" : "off", "Server-wide freeze toggled");
        return true;
    }

    private void broadcastStaff(String senderName, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("evaulx.staff")) {
                player.sendMessage(CC.color(message));
            }
        }
        plugin.getServer().getConsoleSender().sendMessage(CC.color(message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.serverfreeze")) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("on", "off").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ENGLISH)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
