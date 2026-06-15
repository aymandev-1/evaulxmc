package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DevModeCommand implements CommandExecutor, TabCompleter {

    private static final Set<UUID> DEV_MODE = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final EvaulxCore plugin;

    public DevModeCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public static boolean isInDevMode(UUID uuid) {
        return DEV_MODE.contains(uuid);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }

        if (!sender.hasPermission("evaulx.devmode")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        boolean enable;

        if (args.length > 0) {
            String arg = args[0].toLowerCase(Locale.ENGLISH);
            if (arg.equals("on") || arg.equals("enable")) {
                enable = true;
            } else if (arg.equals("off") || arg.equals("disable")) {
                enable = false;
            } else {
                player.sendMessage(CC.color("&cUsage: /devmode [on|off]"));
                return true;
            }
        } else {
            enable = !DEV_MODE.contains(player.getUniqueId());
        }

        if (enable) {
            DEV_MODE.add(player.getUniqueId());
            player.setGameMode(GameMode.CREATIVE);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE / 2, 0, false, false));
            player.sendMessage(CC.color(""));
            player.sendMessage(CC.color("&1&m" + "================================================"));
            player.sendMessage(CC.color("  &9&lDEVELOPER MODE &1&l[ON]"));
            player.sendMessage(CC.color("  &7Creative · Fly · Night Vision enabled."));
            player.sendMessage(CC.color("  &7Type &b/devmode off &7to exit."));
            player.sendMessage(CC.color("&1&m" + "================================================"));
            player.sendMessage(CC.color(""));
            try { player.playSound(player.getLocation(), Sound.valueOf("BLOCK_BEACON_ACTIVATE"), 0.7f, 1.5f); } catch (Throwable ignored) {}
        } else {
            DEV_MODE.remove(player.getUniqueId());
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.sendMessage(CC.color("&8[&9Dev&8] &7Developer mode &cdisabled&7. Survival restored."));
            try { player.playSound(player.getLocation(), Sound.valueOf("BLOCK_BEACON_DEACTIVATE"), 0.7f, 1.0f); } catch (Throwable ignored) {}
        }

        plugin.getStaffRequestManager().logAction(player.getName(), "DEV_MODE", enable ? "on" : "off", "Developer mode toggled");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.devmode")) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("on", "off").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ENGLISH)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
