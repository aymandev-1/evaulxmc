package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WarpManager {

    private final EvaulxCore plugin;
    private final File warpsFile;
    private YamlConfiguration config;
    private final Map<String, Location> warps = new LinkedHashMap<>();

    public WarpManager(EvaulxCore plugin) {
        this.plugin = plugin;
        this.warpsFile = new File(plugin.getDataFolder(), "data/warps.yml");
        load();
    }

    public void load() {
        if (!warpsFile.exists()) {
            warpsFile.getParentFile().mkdirs();
            try { warpsFile.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(warpsFile);
        warps.clear();
        for (String key : config.getKeys(false)) {
            String worldName = config.getString(key + ".world");
            World world = worldName != null ? Bukkit.getWorld(worldName) : null;
            if (world == null) continue;
            warps.put(key.toLowerCase(), new Location(world,
                    config.getDouble(key + ".x"), config.getDouble(key + ".y"), config.getDouble(key + ".z"),
                    (float) config.getDouble(key + ".yaw"), (float) config.getDouble(key + ".pitch")));
        }
    }

    public Location getWarp(String name) { return warps.get(name.toLowerCase()); }

    public List<String> getWarpNames() { return new ArrayList<>(warps.keySet()); }

    public boolean warpExists(String name) { return warps.containsKey(name.toLowerCase()); }

    public void setWarp(String name, Location location) {
        name = name.toLowerCase();
        warps.put(name, location);
        if (location.getWorld() == null) return;
        config.set(name + ".world", location.getWorld().getName());
        config.set(name + ".x", location.getX());
        config.set(name + ".y", location.getY());
        config.set(name + ".z", location.getZ());
        config.set(name + ".yaw", (double) location.getYaw());
        config.set(name + ".pitch", (double) location.getPitch());
        save();
    }

    public boolean deleteWarp(String name) {
        if (warps.remove(name.toLowerCase()) != null) {
            config.set(name.toLowerCase(), null);
            save();
            return true;
        }
        return false;
    }

    private void save() {
        try { config.save(warpsFile); } catch (IOException e) {
            plugin.getLogger().warning("Failed to save warps: " + e.getMessage());
        }
    }
}
