package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CoinsManager {

    private final EvaulxCore plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<UUID, Long> cache = new ConcurrentHashMap<>();

    public CoinsManager(EvaulxCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/coins.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
        cache.clear();
        for (String key : config.getKeys(false)) {
            try { cache.put(UUID.fromString(key), config.getLong(key)); } catch (IllegalArgumentException ignored) {}
        }
    }

    public long getCoins(UUID uuid) { return cache.getOrDefault(uuid, 0L); }

    public void setCoins(UUID uuid, long amount) {
        long val = Math.max(0L, amount);
        cache.put(uuid, val);
        config.set(uuid.toString(), val);
        saveAsync();
    }

    public void addCoins(UUID uuid, long amount) { setCoins(uuid, getCoins(uuid) + amount); }

    public boolean removeCoins(UUID uuid, long amount) {
        if (getCoins(uuid) < amount) return false;
        setCoins(uuid, getCoins(uuid) - amount);
        return true;
    }

    public List<Map.Entry<UUID, Long>> getTopBalances(int limit) {
        List<Map.Entry<UUID, Long>> list = new ArrayList<>(cache.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return list.subList(0, Math.min(limit, list.size()));
    }

    private void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try { config.save(file); } catch (IOException e) {
                plugin.getLogger().warning("Failed to save coins: " + e.getMessage());
            }
        });
    }
}
