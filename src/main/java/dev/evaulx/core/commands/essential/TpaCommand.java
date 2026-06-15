package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TpaCommand implements CommandExecutor, TabCompleter {

    // requester -> target
    public static final Map<UUID, UUID> PENDING = new ConcurrentHashMap<>();
    // requester -> isHere (true = /tpahere)
    public static final Map<UUID, Boolean> IS_HERE = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> EXPIRY = new ConcurrentHashMap<>();
    private static final long TIMEOUT_TICKS = 60 * 20L;

    private final EvaulxCore plugin;
    private final boolean isHere;

    public TpaCommand(EvaulxCore plugin, boolean isHere) {
        this.plugin = plugin;
        this.isHere = isHere;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        if (!sender.hasPermission("evaulx.tpa")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /" + label + " <player>"));
            return true;
        }

        Player from = (Player) sender;
        Player to = Bukkit.getPlayerExact(args[0]);
        if (to == null || to.equals(from)) {
            from.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        // Cancel old request from this player
        cancelExpiry(from.getUniqueId());
        PENDING.put(from.getUniqueId(), to.getUniqueId());
        IS_HERE.put(from.getUniqueId(), isHere);

        BukkitTask[] holder = {null};
        holder[0] = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PENDING.remove(from.getUniqueId());
            IS_HERE.remove(from.getUniqueId());
            EXPIRY.remove(from.getUniqueId());
            if (from.isOnline()) from.sendMessage(CC.color("&7Your teleport request to &f" + to.getName() + " &7expired."));
            if (to.isOnline()) to.sendMessage(CC.color("&7Teleport request from &f" + from.getName() + " &7expired."));
        }, TIMEOUT_TICKS);
        EXPIRY.put(from.getUniqueId(), holder[0]);

        from.sendMessage(CC.color("&aTeleport request sent to &f" + to.getName() + "&a."));
        to.sendMessage(CC.color(CC.SEPARATOR));
        if (isHere) {
            to.sendMessage(CC.color("  &f" + from.getName() + " &7wants you to teleport to them."));
        } else {
            to.sendMessage(CC.color("  &f" + from.getName() + " &7wants to teleport to you."));
        }
        to.sendMessage(CC.color("  &aType &f/tpaccept &aor &c/tpdeny&a."));
        to.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private void cancelExpiry(UUID uuid) {
        BukkitTask t = EXPIRY.remove(uuid);
        if (t != null) t.cancel();
        PENDING.remove(uuid);
        IS_HERE.remove(uuid);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.tpa")) return Collections.emptyList();
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(sender))
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
