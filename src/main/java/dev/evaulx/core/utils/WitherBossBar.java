package dev.evaulx.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A 1.8-compatible boss bar built from an invisible, client-side-only wither entity.
 *
 * <p>On Minecraft 1.8 the boss bar is driven by a nearby wither's health/name. This util spawns a
 * fake wither (packets only — it is never added to the world, so it has no AI and cannot be hit)
 * a fixed distance in front of each viewer and keeps it there, so the bar stays on screen as the
 * player moves. All reflection is guarded: if anything is unavailable the bar simply does nothing,
 * exactly like the previous behaviour, so it can never throw into the caller.</p>
 *
 * <p>Note: real wither boss bars only render on 1.8 clients — which is the server's target.</p>
 */
public final class WitherBossBar {

    private static final double DISTANCE = 32.0;
    private static final float DEFAULT_MAX_HEALTH = 300.0f;

    private final boolean ready;

    // Cached reflection
    private Constructor<?> witherCtor;
    private Constructor<?> spawnCtor;
    private Constructor<?> teleportCtor;
    private Constructor<?> metadataCtor;
    private Constructor<?> destroyCtor;
    private Method getWorldHandle;
    private Method setLocation;
    private Method setInvisible;
    private Method setCustomName;
    private Method setCustomNameVisible;
    private Method setHealth;
    private Method getMaxHealth;
    private Method getId;
    private Method getDataWatcher;
    private Method getEntityHandle;
    private java.lang.reflect.Field playerConnectionField;
    private Method sendPacket;
    private Class<?> dataWatcherClass;

    private volatile String title = "";
    private volatile float progress = 1.0f; // 0..1
    private volatile boolean visible;

    private final Set<UUID> audience = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Object> withers = new ConcurrentHashMap<>();
    private BukkitTask followTask;

    public WitherBossBar(Plugin plugin, String initialTitle) {
        this.title = initialTitle == null ? "" : initialTitle;
        boolean ok;
        try {
            initReflection();
            ok = true;
        } catch (Throwable t) {
            ok = false;
        }
        this.ready = ok;
        if (ready) {
            // Reposition every 4 ticks so the bar follows the player's view.
            followTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 4L);
        }
    }

    private void initReflection() throws Exception {
        String pkg = Bukkit.getServer().getClass().getPackage().getName();
        String nms = "net.minecraft.server." + pkg.substring(pkg.lastIndexOf('.') + 1) + ".";

        Class<?> entityClass        = Class.forName(nms + "Entity");
        Class<?> entityLivingClass  = Class.forName(nms + "EntityLiving");
        Class<?> entityWitherClass  = Class.forName(nms + "EntityWither");
        Class<?> worldClass         = Class.forName(nms + "World");
        Class<?> packetClass        = Class.forName(nms + "Packet");
        dataWatcherClass            = Class.forName(nms + "DataWatcher");

        witherCtor   = entityWitherClass.getConstructor(worldClass);
        spawnCtor    = Class.forName(nms + "PacketPlayOutSpawnEntityLiving").getConstructor(entityLivingClass);
        teleportCtor = Class.forName(nms + "PacketPlayOutEntityTeleport").getConstructor(entityClass);
        metadataCtor = Class.forName(nms + "PacketPlayOutEntityMetadata").getConstructor(int.class, dataWatcherClass, boolean.class);
        destroyCtor  = Class.forName(nms + "PacketPlayOutEntityDestroy").getConstructor(int[].class);

        getWorldHandle       = Class.forName(pkg + ".CraftWorld").getMethod("getHandle");
        setLocation          = entityWitherClass.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
        setInvisible         = entityWitherClass.getMethod("setInvisible", boolean.class);
        setCustomName        = entityWitherClass.getMethod("setCustomName", String.class);
        setCustomNameVisible = entityWitherClass.getMethod("setCustomNameVisible", boolean.class);
        setHealth            = entityWitherClass.getMethod("setHealth", float.class);
        getMaxHealth         = entityWitherClass.getMethod("getMaxHealth");
        getId                = entityWitherClass.getMethod("getId");
        getDataWatcher       = entityWitherClass.getMethod("getDataWatcher");

        getEntityHandle      = Class.forName(pkg + ".entity.CraftPlayer").getMethod("getHandle");
        Class<?> entityPlayerClass = Class.forName(nms + "EntityPlayer");
        playerConnectionField = entityPlayerClass.getField("playerConnection");
        sendPacket           = Class.forName(nms + "PlayerConnection").getMethod("sendPacket", packetClass);
    }

    // ── Public API (mirrors the native BossBar operations the plugin needs) ─────

    public boolean isReady() { return ready; }

    public void addPlayer(Player player) {
        if (!ready || player == null) return;
        audience.add(player.getUniqueId());
        if (visible) spawnFor(player);
    }

    public void removePlayer(Player player) {
        if (player == null) return;
        audience.remove(player.getUniqueId());
        despawnFor(player.getUniqueId());
    }

    public void setTitle(String newTitle) {
        this.title = newTitle == null ? "" : newTitle;
        if (!ready || !visible) return;
        for (UUID id : withers.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) updateMetadata(p);
        }
    }

    public void setProgress(double value) {
        this.progress = (float) Math.max(0.0, Math.min(1.0, value));
        if (!ready || !visible) return;
        for (UUID id : withers.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) updateMetadata(p);
        }
    }

    public void setVisible(boolean show) {
        if (!ready || show == visible) return;
        visible = show;
        if (show) {
            for (UUID id : audience) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) spawnFor(p);
            }
        } else {
            for (UUID id : new java.util.HashSet<>(withers.keySet())) despawnFor(id);
        }
    }

    public void destroy() {
        if (followTask != null) followTask.cancel();
        for (UUID id : new java.util.HashSet<>(withers.keySet())) despawnFor(id);
        audience.clear();
    }

    // ── Internals ───────────────────────────────────────────────────────────────

    private void tick() {
        if (!visible) return;
        for (UUID id : audience) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) { despawnFor(id); continue; }
            if (!withers.containsKey(id)) { spawnFor(p); continue; }
            moveTo(p);
        }
        // Drop withers for players who left the audience or logged off.
        for (UUID id : new java.util.HashSet<>(withers.keySet())) {
            if (!audience.contains(id) || Bukkit.getPlayer(id) == null) despawnFor(id);
        }
    }

    private void spawnFor(Player player) {
        if (!ready) return;
        try {
            despawnFor(player.getUniqueId());
            Object nmsWorld = getWorldHandle.invoke(player.getWorld());
            Object wither = witherCtor.newInstance(nmsWorld);
            Location loc = frontOf(player);
            setLocation.invoke(wither, loc.getX(), loc.getY(), loc.getZ(), 0.0f, 0.0f);
            setInvisible.invoke(wither, true);
            try { setCustomNameVisible.invoke(wither, false); } catch (Throwable ignored) {}
            setCustomName.invoke(wither, clip(title));
            setHealth.invoke(wither, healthFor(wither));
            withers.put(player.getUniqueId(), wither);
            sendPacket(player, spawnCtor.newInstance(wither));
        } catch (Throwable ignored) {}
    }

    private void moveTo(Player player) {
        Object wither = withers.get(player.getUniqueId());
        if (wither == null) return;
        try {
            Location loc = frontOf(player);
            setLocation.invoke(wither, loc.getX(), loc.getY(), loc.getZ(), 0.0f, 0.0f);
            sendPacket(player, teleportCtor.newInstance(wither));
        } catch (Throwable ignored) {}
    }

    private void updateMetadata(Player player) {
        Object wither = withers.get(player.getUniqueId());
        if (wither == null) return;
        try {
            setCustomName.invoke(wither, clip(title));
            setHealth.invoke(wither, healthFor(wither));
            int id = (int) getId.invoke(wither);
            Object watcher = getDataWatcher.invoke(wither);
            sendPacket(player, metadataCtor.newInstance(id, watcher, true));
        } catch (Throwable ignored) {}
    }

    private void despawnFor(UUID uuid) {
        Object wither = withers.remove(uuid);
        if (wither == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        try {
            int id = (int) getId.invoke(wither);
            sendPacket(player, destroyCtor.newInstance((Object) new int[]{ id }));
        } catch (Throwable ignored) {}
    }

    private float healthFor(Object wither) {
        float max = DEFAULT_MAX_HEALTH;
        try { max = (float) getMaxHealth.invoke(wither); } catch (Throwable ignored) {}
        return Math.max(1.0f, progress * max);
    }

    private Location frontOf(Player player) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize().multiply(DISTANCE);
        return eye.add(dir);
    }

    private void sendPacket(Player player, Object packet) throws Exception {
        Object handle = getEntityHandle.invoke(player);
        Object connection = playerConnectionField.get(handle);
        sendPacket.invoke(connection, packet);
    }

    /** Wither custom names are capped at 64 chars on 1.8. */
    private String clip(String s) {
        if (s == null) return "";
        return s.length() > 64 ? s.substring(0, 64) : s;
    }
}
