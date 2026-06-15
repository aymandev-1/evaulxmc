package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HomeManager {

    private final EvaulxCore plugin;
    private final File homesFolder;
    private final Map<UUID, Map<String, Location>> cache = new HashMap<>();

    public HomeManager(EvaulxCore plugin) {
        this.plugin = plugin;
        this.homesFolder = new File(plugin.getDataFolder(), "data/homes");
        if (!homesFolder.exists()) homesFolder.mkdirs();
    }

    public Map<String, Location> getHomes(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadHomes);
    }

    public Location getHome(UUID uuid, String name) {
        return getHomes(uuid).get(name.toLowerCase());
    }

    public List<String> getHomeNames(UUID uuid) {
        return new ArrayList<>(getHomes(uuid).keySet());
    }

    public int getHomeCount(UUID uuid) {
        return getHomes(uuid).size();
    }

    public int getHomeLimit(Player player) {
        for (int i = 20; i >= 1; i--) {
            if (player.hasPermission("evaulx.homes." + i)) return i;
        }
        return plugin.getConfig().getInt("homes.default-limit", 3);
    }

    public void setHome(UUID uuid, String name, Location location) {
        getHomes(uuid).put(name.toLowerCase(), location);
        saveHomes(uuid);
    }

    public boolean deleteHome(UUID uuid, String name) {
        if (getHomes(uuid).remove(name.toLowerCase()) != null) {
            saveHomes(uuid);
            return true;
        }
        return false;
    }

    public void unloadPlayer(UUID uuid) {
        saveHomes(uuid);
        cache.remove(uuid);
    }

    private Map<String, Location> loadHomes(UUID uuid) {
        Map<String, Location> homes = new LinkedHashMap<>();
        File file = new File(homesFolder, uuid + ".yml");
        if (!file.exists()) return homes;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            String worldName = cfg.getString(key + ".world");
            World world = worldName != null ? Bukkit.getWorld(worldName) : null;
            if (world == null) continue;
            homes.put(key, new Location(world,
                    cfg.getDouble(key + ".x"), cfg.getDouble(key + ".y"), cfg.getDouble(key + ".z"),
                    (float) cfg.getDouble(key + ".yaw"), (float) cfg.getDouble(key + ".pitch")));
        }
        return homes;
    }

    private void saveHomes(UUID uuid) {
        File file = new File(homesFolder, uuid + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        Map<String, Location> homes = cache.get(uuid);
        if (homes != null) {
            for (Map.Entry<String, Location> e : homes.entrySet()) {
                Location loc = e.getValue();
                if (loc.getWorld() == null) continue;
                cfg.set(e.getKey() + ".world", loc.getWorld().getName());
                cfg.set(e.getKey() + ".x", loc.getX());
                cfg.set(e.getKey() + ".y", loc.getY());
                cfg.set(e.getKey() + ".z", loc.getZ());
                cfg.set(e.getKey() + ".yaw", (double) loc.getYaw());
                cfg.set(e.getKey() + ".pitch", (double) loc.getPitch());
            }
        }
        try { cfg.save(file); } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save homes for " + uuid + ": " + ex.getMessage());
        }
    }
}
