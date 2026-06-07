package dev.evaulx.core.managers;

import com.google.gson.*;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.StoredLocation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player named homes, persisted to {@code data/homes.json}.
 *
 * <p>The home limit is the highest of: {@code evaulx.homes.<n>} permission nodes the player has,
 * or {@code homes.default-limit} from config. {@code evaulx.homes.unlimited} removes the cap.
 */
public class HomesManager {

    private final EvaulxCore plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, Map<String, StoredLocation>> homes = new ConcurrentHashMap<>();
    private File file;

    public HomesManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getEvaulxDataFolder(), "homes.json");
        homes.clear();
        if (!file.exists()) return;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonElement parsed = new JsonParser().parse(reader);
            if (!parsed.isJsonObject()) return;
            for (Map.Entry<String, JsonElement> playerEntry : parsed.getAsJsonObject().entrySet()) {
                if (!playerEntry.getValue().isJsonObject()) continue;
                UUID uuid;
                try {
                    uuid = UUID.fromString(playerEntry.getKey());
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                Map<String, StoredLocation> playerHomes = new ConcurrentHashMap<>();
                for (Map.Entry<String, JsonElement> homeEntry : playerEntry.getValue().getAsJsonObject().entrySet()) {
                    if (!homeEntry.getValue().isJsonObject()) continue;
                    StoredLocation location = StoredLocation.fromJson(homeEntry.getValue().getAsJsonObject());
                    if (location != null) playerHomes.put(homeEntry.getKey().toLowerCase(Locale.ENGLISH), location);
                }
                if (!playerHomes.isEmpty()) homes.put(uuid, playerHomes);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load homes: " + e.getMessage());
        }
    }

    /** SetResult communicates why a home could not be set. */
    public enum SetResult { OK, LIMIT_REACHED, INVALID }

    public SetResult setHome(Player player, String name, Location location) {
        if (name == null || name.trim().isEmpty() || location == null || location.getWorld() == null) return SetResult.INVALID;
        String key = name.toLowerCase(Locale.ENGLISH);
        Map<String, StoredLocation> playerHomes = homes.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());

        // Overwriting an existing home never counts against the limit.
        if (!playerHomes.containsKey(key)) {
            int limit = getHomeLimit(player);
            if (limit >= 0 && playerHomes.size() >= limit) return SetResult.LIMIT_REACHED;
        }
        playerHomes.put(key, StoredLocation.of(name.trim(), location));
        save();
        return SetResult.OK;
    }

    public boolean deleteHome(UUID uuid, String name) {
        if (name == null) return false;
        Map<String, StoredLocation> playerHomes = homes.get(uuid);
        if (playerHomes == null) return false;
        boolean removed = playerHomes.remove(name.toLowerCase(Locale.ENGLISH)) != null;
        if (removed) {
            if (playerHomes.isEmpty()) homes.remove(uuid);
            save();
        }
        return removed;
    }

    public Location getHome(UUID uuid, String name) {
        if (name == null) return null;
        Map<String, StoredLocation> playerHomes = homes.get(uuid);
        if (playerHomes == null) return null;
        StoredLocation stored = playerHomes.get(name.toLowerCase(Locale.ENGLISH));
        return stored == null ? null : stored.toBukkit();
    }

    public List<String> getHomeNames(UUID uuid) {
        Map<String, StoredLocation> playerHomes = homes.get(uuid);
        if (playerHomes == null) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (StoredLocation stored : playerHomes.values()) names.add(stored.getName());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public int getHomeCount(UUID uuid) {
        Map<String, StoredLocation> playerHomes = homes.get(uuid);
        return playerHomes == null ? 0 : playerHomes.size();
    }

    /** @return the player's home limit, or {@code -1} for unlimited. */
    public int getHomeLimit(Player player) {
        if (player.hasPermission("evaulx.homes.unlimited")) return -1;
        int best = -2;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String node = info.getPermission().toLowerCase(Locale.ENGLISH);
            if (!node.startsWith("evaulx.homes.")) continue;
            String suffix = node.substring("evaulx.homes.".length());
            try {
                best = Math.max(best, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
            }
        }
        if (best >= 0) return best;
        return Math.max(0, plugin.getConfig().getInt("homes.default-limit", 3));
    }

    private void save() {
        if (file == null) return;
        JsonObject root = new JsonObject();
        for (Map.Entry<UUID, Map<String, StoredLocation>> playerEntry : homes.entrySet()) {
            JsonObject playerObject = new JsonObject();
            for (Map.Entry<String, StoredLocation> homeEntry : playerEntry.getValue().entrySet()) {
                playerObject.add(homeEntry.getKey(), homeEntry.getValue().toJson());
            }
            root.add(playerEntry.getKey().toString(), playerObject);
        }
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
            gson.toJson(root, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save homes: " + e.getMessage());
            tmp.delete();
            return;
        }
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to finalize homes save: " + e.getMessage());
        }
    }
}
