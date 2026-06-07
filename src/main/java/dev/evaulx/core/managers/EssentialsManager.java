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

    public EssentialsManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void setBackLocation(Player player, Location location) {
        if (location != null) backLocations.put(player.getUniqueId(), location.clone());
    }

    public Location getBackLocation(Player player) {
        Location location = backLocations.get(player.getUniqueId());
        return location == null ? null : location.clone();
    }

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
}
