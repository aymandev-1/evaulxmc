package dev.evaulx.core.managers;

import com.google.gson.*;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.StoredLocation;
import org.bukkit.Location;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-wide named warps (admin-defined), persisted to {@code data/warps.json}.
 */
public class WarpManager {

    private final EvaulxCore plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, StoredLocation> warps = new ConcurrentHashMap<>();
    private File file;

    public WarpManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getEvaulxDataFolder(), "warps.json");
        warps.clear();
        if (!file.exists()) return;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonElement parsed = new JsonParser().parse(reader);
            if (!parsed.isJsonObject()) return;
            for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                StoredLocation location = StoredLocation.fromJson(entry.getValue().getAsJsonObject());
                if (location != null) warps.put(entry.getKey().toLowerCase(Locale.ENGLISH), location);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load warps: " + e.getMessage());
        }
    }

    public boolean setWarp(String name, Location location) {
        if (name == null || name.trim().isEmpty() || location == null || location.getWorld() == null) return false;
        warps.put(name.toLowerCase(Locale.ENGLISH), StoredLocation.of(name.trim(), location));
        save();
        return true;
    }

    public boolean deleteWarp(String name) {
        if (name == null) return false;
        boolean removed = warps.remove(name.toLowerCase(Locale.ENGLISH)) != null;
        if (removed) save();
        return removed;
    }

    public Location getWarp(String name) {
        if (name == null) return null;
        StoredLocation stored = warps.get(name.toLowerCase(Locale.ENGLISH));
        return stored == null ? null : stored.toBukkit();
    }

    public String getDisplayName(String name) {
        if (name == null) return "";
        StoredLocation stored = warps.get(name.toLowerCase(Locale.ENGLISH));
        return stored == null ? name : stored.getName();
    }

    public boolean exists(String name) {
        return name != null && warps.containsKey(name.toLowerCase(Locale.ENGLISH));
    }

    public List<String> getWarpNames() {
        List<String> names = new ArrayList<>();
        for (StoredLocation stored : warps.values()) names.add(stored.getName());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private void save() {
        if (file == null) return;
        JsonObject root = new JsonObject();
        for (Map.Entry<String, StoredLocation> entry : warps.entrySet()) {
            root.add(entry.getKey(), entry.getValue().toJson());
        }
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
            gson.toJson(root, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save warps: " + e.getMessage());
            tmp.delete();
            return;
        }
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to finalize warps save: " + e.getMessage());
        }
    }
}
