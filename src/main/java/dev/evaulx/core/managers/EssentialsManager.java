package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EssentialsManager {

    private final EvaulxCore plugin;
    private final Map<UUID, Location> backLocations = new ConcurrentHashMap<>();

    // Lockdown
    private boolean lockdownEnabled = false;
    private String lockdownReason = "Server locked down by an admin.";

    // Temp fly: uuid → expiry timestamp (ms). -1 = permanent via this system (cancelled manually).
    private final Map<UUID, Long> tempFlyExpiry = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> tempFlyTasks = new ConcurrentHashMap<>();

    public EssentialsManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    // ── Back locations ────────────────────────────────────────────────────────

    public void setBackLocation(Player player, Location location) {
        if (location != null) backLocations.put(player.getUniqueId(), location.clone());
    }

    public Location getBackLocation(Player player) {
        Location location = backLocations.get(player.getUniqueId());
        return location == null ? null : location.clone();
    }

    // ── Spawn ─────────────────────────────────────────────────────────────────

    public void setSpawn(Location location) {
        plugin.getConfig().set("spawn.world", location.getWorld().getName());
        plugin.getConfig().set("spawn.x", location.getX());
        plugin.getConfig().set("spawn.y", location.getY());
        plugin.getConfig().set("spawn.z", location.getZ());
        plugin.getConfig().set("spawn.yaw", location.getYaw());
        plugin.getConfig().set("spawn.pitch", location.getPitch());
        plugin.saveConfig();
    }

    public Location getSpawn() {
        String worldName = plugin.getConfig().getString("spawn.world", "");
        World world = worldName == null || worldName.isEmpty() ? null : Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(
                world,
                plugin.getConfig().getDouble("spawn.x"),
                plugin.getConfig().getDouble("spawn.y"),
                plugin.getConfig().getDouble("spawn.z"),
                (float) plugin.getConfig().getDouble("spawn.yaw"),
                (float) plugin.getConfig().getDouble("spawn.pitch")
        );
    }

    // ── Broadcasts ────────────────────────────────────────────────────────────

    public void sendBroadcast(String senderName, String message) {
        String format = plugin.getConfig().getString("announcements.broadcast-format",
                "&8[&6Broadcast&8] &f{message}");
        String formatted = format
                .replace("{sender}", senderName)
                .replace("{message}", message);
        Bukkit.broadcastMessage(CC.color(formatted));
        if (plugin.getRedisSyncManager() != null) plugin.getRedisSyncManager().publishBroadcast(formatted);
    }

    public void sendAnnouncement(String senderName, String message) {
        String format = plugin.getConfig().getString("announcements.announcement-format",
                "&8[&cAnnouncement&8] &f{message}");
        String formatted = format
                .replace("{sender}", senderName)
                .replace("{message}", message);
        Bukkit.broadcastMessage(CC.color(formatted));
        if (plugin.getRedisSyncManager() != null) plugin.getRedisSyncManager().publishBroadcast(formatted);
    }

    // ── Lockdown ──────────────────────────────────────────────────────────────

    public boolean isLockdown() { return lockdownEnabled; }
    public String getLockdownReason() { return lockdownReason; }

    public void setLockdown(boolean enabled, String reason) {
        this.lockdownEnabled = enabled;
        if (reason != null && !reason.trim().isEmpty()) this.lockdownReason = reason;
    }

    // ── Temp fly ──────────────────────────────────────────────────────────────

    public boolean hasTempFly(UUID uuid) {
        return tempFlyExpiry.containsKey(uuid);
    }

    public long getTempFlyExpiry(UUID uuid) {
        return tempFlyExpiry.getOrDefault(uuid, -1L);
    }

    public void grantTempFly(Player player, long durationMs) {
        cancelTempFly(player.getUniqueId());

        long expiresAt = System.currentTimeMillis() + durationMs;
        tempFlyExpiry.put(player.getUniqueId(), expiresAt);

        player.setAllowFlight(true);
        player.setFlying(true);

        long ticks = durationMs / 50L; // ms → ticks
        int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            tempFlyExpiry.remove(player.getUniqueId());
            tempFlyTasks.remove(player.getUniqueId());
            if (!player.isOnline()) return;
            if (!player.hasPermission("evaulx.fly")) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
            player.sendMessage(CC.color("&cYour temporary fly has expired."));
        }, ticks).getTaskId();

        tempFlyTasks.put(player.getUniqueId(), taskId);
    }

    public void cancelTempFly(UUID uuid) {
        tempFlyExpiry.remove(uuid);
        Integer taskId = tempFlyTasks.remove(uuid);
        if (taskId != null) plugin.getServer().getScheduler().cancelTask(taskId);
    }

    public void onPlayerQuitCancelTempFly(UUID uuid) {
        // Don't cancel the task — just remove tracking. Fly will be re-applied on rejoin via
        // no persistent state, which is intentional (temp fly doesn't survive restarts/relog).
        cancelTempFly(uuid);
    }
}
