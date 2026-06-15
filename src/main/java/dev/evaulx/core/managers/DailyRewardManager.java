package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DailyRewardManager {

    private static final long DAY_MS = 86_400_000L;

    private final EvaulxCore plugin;
    private final File file;
    private YamlConfiguration config;

    public DailyRewardManager(EvaulxCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/daily.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean canClaim(UUID uuid) {
        long last = config.getLong(uuid.toString(), 0L);
        return System.currentTimeMillis() - last >= DAY_MS;
    }

    public long getTimeUntilNext(UUID uuid) {
        long last = config.getLong(uuid.toString(), 0L);
        return Math.max(0L, DAY_MS - (System.currentTimeMillis() - last));
    }

    public void setClaimed(UUID uuid) {
        config.set(uuid.toString(), System.currentTimeMillis());
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try { config.save(file); } catch (IOException e) {
                plugin.getLogger().warning("Failed to save daily: " + e.getMessage());
            }
        });
    }

    public int getCoinsReward() {
        return plugin.getConfig().getInt("daily-reward.coins", 100);
    }

    public String getStreakBonus(int streak) {
        if (streak >= 30) return "&6+500 bonus coins (30-day streak!)";
        if (streak >= 7)  return "&e+100 bonus coins (7-day streak!)";
        return null;
    }
}
