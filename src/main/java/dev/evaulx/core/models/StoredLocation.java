package dev.evaulx.core.models;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * A serializable snapshot of a Bukkit {@link Location} used by warps and homes.
 * Stores the world by name so it survives restarts and resolves lazily.
 */
public class StoredLocation {

    private final String name;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public StoredLocation(String name, String world, double x, double y, double z, float yaw, float pitch) {
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static StoredLocation of(String name, Location location) {
        return new StoredLocation(name, location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    public String getName() { return name; }
    public String getWorld() { return world; }

    /** @return the resolved Bukkit location, or {@code null} if the world is not loaded. */
    public Location toBukkit() {
        World w = world == null ? null : Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("world", world);
        obj.addProperty("x", x);
        obj.addProperty("y", y);
        obj.addProperty("z", z);
        obj.addProperty("yaw", yaw);
        obj.addProperty("pitch", pitch);
        return obj;
    }

    public static StoredLocation fromJson(JsonObject obj) {
        if (obj == null || !obj.has("world")) return null;
        return new StoredLocation(
                obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString() : "",
                obj.get("world").getAsString(),
                obj.get("x").getAsDouble(),
                obj.get("y").getAsDouble(),
                obj.get("z").getAsDouble(),
                obj.has("yaw") ? obj.get("yaw").getAsFloat() : 0f,
                obj.has("pitch") ? obj.get("pitch").getAsFloat() : 0f
        );
    }
}
