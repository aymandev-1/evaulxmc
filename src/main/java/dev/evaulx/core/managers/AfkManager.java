package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks AFK status: manual ({@code /afk}) and automatic (after a configurable idle period).
 * Activity is registered by {@link dev.evaulx.core.listeners.AfkListener}.
 */
public class AfkManager {

    private final EvaulxCore plugin;
    private final java.util.Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Set<UUID> afk = ConcurrentHashMap.newKeySet();
    private BukkitTask task;

    public AfkManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        for (Player player : Bukkit.getOnlinePlayers()) lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        if (task != null) task.cancel();
        // Check for idle players every 20 seconds.
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::checkAutoAfk, 400L, 400L);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        afk.clear();
        lastActivity.clear();
    }

    public boolean isAfk(UUID uuid) {
        return afk.contains(uuid);
    }

    public void handleJoin(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void handleQuit(Player player) {
        lastActivity.remove(player.getUniqueId());
        afk.remove(player.getUniqueId());
    }

    /** Registers player activity; automatically clears AFK if they were marked AFK. */
    public void markActivity(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        if (afk.contains(player.getUniqueId())) setAfk(player, false, null);
    }

    public void toggle(Player player, String reason) {
        setAfk(player, !afk.contains(player.getUniqueId()), reason);
    }

    public void setAfk(Player player, boolean state, String reason) {
        boolean current = afk.contains(player.getUniqueId());
        if (state == current) return;

        if (state) afk.add(player.getUniqueId());
        else afk.remove(player.getUniqueId());
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());

        if (!plugin.getConfig().getBoolean("afk.broadcast", true)) return;
        String key = state ? "afk.enter-message" : "afk.leave-message";
        String fallback = state ? "&7* &f{player} &7is now AFK{reason}." : "&7* &f{player} &7is no longer AFK.";
        String reasonText = (state && reason != null && !reason.trim().isEmpty()) ? " &7(&f" + reason.trim() + "&7)" : "";
        String message = plugin.getConfig().getString(key, fallback)
                .replace("{player}", player.getName())
                .replace("{reason}", reasonText);
        Bukkit.broadcastMessage(CC.color(message));
    }

    private void checkAutoAfk() {
        int minutes = plugin.getConfig().getInt("afk.auto-minutes", 5);
        if (minutes <= 0) return;
        long threshold = minutes * 60_000L;
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (afk.contains(player.getUniqueId()) || player.hasPermission("evaulx.afk.exempt")) continue;
            Long last = lastActivity.get(player.getUniqueId());
            if (last == null) {
                lastActivity.put(player.getUniqueId(), now);
                continue;
            }
            if (now - last >= threshold) setAfk(player, true, null);
        }
    }
}
