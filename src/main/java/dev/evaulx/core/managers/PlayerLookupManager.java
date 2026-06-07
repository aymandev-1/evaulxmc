package dev.evaulx.core.managers;

import com.google.gson.*;
import dev.evaulx.core.EvaulxCore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerLookupManager {

    private final EvaulxCore plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, LookupEntry> byName = new ConcurrentHashMap<>();
    private final Map<UUID, LookupEntry> byUuid = new ConcurrentHashMap<>();
    private File cacheFile;

    public PlayerLookupManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File cacheDir = new File(plugin.getEvaulxDataFolder(), "cache");
        if (!cacheDir.exists()) cacheDir.mkdirs();
        cacheFile = new File(cacheDir, "player-lookup.json");
        loadCache();
        scanFlatFileProfiles();
    }

    public void shutdown() {
        save();
    }

    public void remember(UUID uuid, String name) {
        if (uuid == null || name == null || name.trim().isEmpty()) return;
        LookupEntry entry = new LookupEntry(uuid, name, System.currentTimeMillis());
        byUuid.put(uuid, entry);
        byName.put(name.toLowerCase(Locale.ENGLISH), entry);
        save();
    }

    public OfflinePlayer find(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String value = input.trim();
        try {
            UUID uuid = UUID.fromString(value);
            LookupEntry entry = byUuid.get(uuid);
            return Bukkit.getOfflinePlayer(entry != null ? entry.getUuid() : uuid);
        } catch (IllegalArgumentException ignored) {
        }

        PlayerExactMatch match = exactName(value);
        if (match.online != null) return match.online;
        LookupEntry entry = byName.get(value.toLowerCase(Locale.ENGLISH));
        if (entry != null) {
            @SuppressWarnings("deprecation")
            OfflinePlayer cached = Bukkit.getOfflinePlayer(entry.getName());
            return cached;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer fallback = Bukkit.getOfflinePlayer(value);
        if (fallback != null && (fallback.hasPlayedBefore() || fallback.isOnline())) {
            remember(fallback.getUniqueId(), fallback.getName() == null ? value : fallback.getName());
            return fallback;
        }
        return null;
    }

    public List<String> suggest(String prefix, int limit) {
        String lowered = prefix == null ? "" : prefix.toLowerCase(Locale.ENGLISH);
        List<String> names = new ArrayList<>();
        for (LookupEntry entry : byName.values()) {
            if (!entry.getName().toLowerCase(Locale.ENGLISH).startsWith(lowered)) continue;
            names.add(entry.getName());
            if (names.size() >= limit) break;
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public Collection<LookupEntry> entries() {
        List<LookupEntry> entries = new ArrayList<>(byUuid.values());
        entries.sort((first, second) -> Long.compare(second.getLastSeen(), first.getLastSeen()));
        return entries;
    }

    public int size() {
        return byUuid.size();
    }

    private PlayerExactMatch exactName(String name) {
        OfflinePlayer online = Bukkit.getPlayerExact(name);
        return new PlayerExactMatch(online);
    }

    private void loadCache() {
        if (cacheFile == null || !cacheFile.exists()) return;
        try (Reader reader = new InputStreamReader(new FileInputStream(cacheFile), StandardCharsets.UTF_8)) {
            JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject obj = element.getAsJsonObject();
                UUID uuid = UUID.fromString(getString(obj, "uuid", ""));
                String name = getString(obj, "name", "");
                long lastSeen = getLong(obj, "lastSeen", 0L);
                add(new LookupEntry(uuid, name, lastSeen));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load player lookup cache: " + e.getMessage());
        }
    }

    private void scanFlatFileProfiles() {
        File playersDir = new File(plugin.getEvaulxDataFolder(), "players");
        File[] files = playersDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                JsonObject obj = new JsonParser().parse(reader).getAsJsonObject();
                UUID uuid = UUID.fromString(getString(obj, "uuid", file.getName().replace(".json", "")));
                String name = getString(obj, "name", "");
                long lastSeen = getLong(obj, "lastSeen", file.lastModified());
                add(new LookupEntry(uuid, name, lastSeen));
            } catch (Exception ignored) {
            }
        }
        save();
    }

    private void add(LookupEntry entry) {
        if (entry == null || entry.getName().isEmpty()) return;
        byUuid.put(entry.getUuid(), entry);
        byName.put(entry.getName().toLowerCase(Locale.ENGLISH), entry);
    }

    private void save() {
        if (cacheFile == null) return;
        JsonArray array = new JsonArray();
        for (LookupEntry entry : byUuid.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("uuid", entry.getUuid().toString());
            obj.addProperty("name", entry.getName());
            obj.addProperty("lastSeen", entry.getLastSeen());
            array.add(obj);
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(cacheFile), StandardCharsets.UTF_8)) {
            gson.toJson(array, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save player lookup cache: " + e.getMessage());
        }
    }

    private String getString(JsonObject obj, String key, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsString();
    }

    private long getLong(JsonObject obj, String key, long fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsLong();
    }

    private static class PlayerExactMatch {
        private final OfflinePlayer online;

        private PlayerExactMatch(OfflinePlayer online) {
            this.online = online;
        }
    }

    public static class LookupEntry {
        private final UUID uuid;
        private final String name;
        private final long lastSeen;

        public LookupEntry(UUID uuid, String name, long lastSeen) {
            this.uuid = uuid;
            this.name = name == null ? "" : name;
            this.lastSeen = lastSeen;
        }

        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public long getLastSeen() { return lastSeen; }
    }
}
