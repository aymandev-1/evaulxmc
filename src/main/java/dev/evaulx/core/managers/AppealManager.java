package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AppealManager {

    private final EvaulxCore plugin;
    private final Map<String, Appeal> appeals = new ConcurrentHashMap<>();
    private File file;

    public AppealManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "appeals.yml");
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String id : config.getKeys(false)) {
            Appeal a = new Appeal();
            a.punishmentId = id;
            a.playerName = config.getString(id + ".playerName", "");
            a.reason = config.getString(id + ".reason", "");
            a.submittedAt = config.getLong(id + ".submittedAt", 0);
            a.status = config.getString(id + ".status", "pending");
            a.resolvedBy = config.getString(id + ".resolvedBy", null);
            a.resolvedAt = config.getLong(id + ".resolvedAt", 0);
            a.resolveReason = config.getString(id + ".resolveReason", null);
            appeals.put(id, a);
        }
    }

    public void save() {
        if (file == null) file = new File(plugin.getDataFolder(), "appeals.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Appeal> entry : appeals.entrySet()) {
            String id = entry.getKey();
            Appeal a = entry.getValue();
            config.set(id + ".playerName", a.playerName);
            config.set(id + ".reason", a.reason);
            config.set(id + ".submittedAt", a.submittedAt);
            config.set(id + ".status", a.status);
            if (a.resolvedBy != null) config.set(id + ".resolvedBy", a.resolvedBy);
            if (a.resolvedAt > 0) config.set(id + ".resolvedAt", a.resolvedAt);
            if (a.resolveReason != null) config.set(id + ".resolveReason", a.resolveReason);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save appeals.yml: " + e.getMessage());
        }
    }

    public Appeal submit(String punishmentId, String playerName, String reason) {
        Appeal a = new Appeal();
        a.punishmentId = punishmentId;
        a.playerName = playerName;
        a.reason = reason;
        a.submittedAt = System.currentTimeMillis();
        a.status = "pending";
        appeals.put(punishmentId, a);
        save();
        return a;
    }

    public boolean hasPending(String punishmentId) {
        Appeal a = appeals.get(punishmentId);
        return a != null && "pending".equals(a.status);
    }

    public boolean hasAny(String punishmentId) {
        return appeals.containsKey(punishmentId);
    }

    public Appeal get(String punishmentId) {
        return appeals.get(punishmentId);
    }

    public List<Appeal> getPending() {
        List<Appeal> list = new ArrayList<>();
        for (Appeal a : appeals.values()) {
            if ("pending".equals(a.status)) list.add(a);
        }
        list.sort(Comparator.comparingLong(a -> a.submittedAt));
        return list;
    }

    public void resolve(String punishmentId, String resolvedBy, boolean accepted, String resolveReason) {
        Appeal a = appeals.get(punishmentId);
        if (a == null) return;
        a.status = accepted ? "accepted" : "denied";
        a.resolvedBy = resolvedBy;
        a.resolvedAt = System.currentTimeMillis();
        a.resolveReason = resolveReason;
        save();
    }

    public static class Appeal {
        public String punishmentId;
        public String playerName;
        public String reason;
        public long submittedAt;
        public String status;
        public String resolvedBy;
        public long resolvedAt;
        public String resolveReason;
    }
}
