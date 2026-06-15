package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HideAllCommand implements CommandExecutor, TabCompleter {

    private static final Set<UUID> HIDING = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final EvaulxCore plugin;

    public HideAllCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public static boolean isHiding(UUID uuid) {
        return HIDING.contains(uuid);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }

        if (!sender.hasPermission("evaulx.hideall")) {
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
                player.sendMessage(CC.color("&cUsage: /hideall [on|off]"));
                return true;
            }
        } else {
            enable = !HIDING.contains(player.getUniqueId());
        }

        if (enable) {
            HIDING.add(player.getUniqueId());
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.getUniqueId().equals(player.getUniqueId())) {
                    player.hidePlayer(other);
                }
            }
            player.sendMessage(CC.color("&8[&aBuilder&8] &7All players hidden. &8(&f" + (Bukkit.getOnlinePlayers().size() - 1) + " hidden&8)"));
            try { player.playSound(player.getLocation(), Sound.valueOf("BLOCK_ENDER_CHEST_OPEN"), 0.6f, 1.2f); } catch (Throwable ignored) {}
        } else {
            HIDING.remove(player.getUniqueId());
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.getUniqueId().equals(player.getUniqueId())) {
                    player.showPlayer(other);
                }
            }
            player.sendMessage(CC.color("&8[&aBuilder&8] &7All players are now &avisible&7."));
            try { player.playSound(player.getLocation(), Sound.valueOf("BLOCK_ENDER_CHEST_CLOSE"), 0.6f, 1.2f); } catch (Throwable ignored) {}
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.hideall")) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("on", "off").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ENGLISH)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
