package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FriendManager {

    private final EvaulxCore plugin;
    private final File file;
    private YamlConfiguration config;

    private final Map<UUID, Set<UUID>> friends = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> blocked = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> favorites = new ConcurrentHashMap<>();
    private final Set<UUID> notificationsDisabled = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final Map<UUID, UUID> pending = new ConcurrentHashMap<>(); // requester -> target

    public FriendManager(EvaulxCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/friends.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
        friends.clear();
        blocked.clear();
        favorites.clear();
        notificationsDisabled.clear();

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);

                Set<UUID> friendSet = new HashSet<>();
                for (String s : config.getStringList(key + ".friends")) {
                    try { friendSet.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                }
                if (!friendSet.isEmpty()) friends.put(uuid, friendSet);

                Set<UUID> blockedSet = new HashSet<>();
                for (String s : config.getStringList(key + ".blocked")) {
                    try { blockedSet.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                }
                if (!blockedSet.isEmpty()) blocked.put(uuid, blockedSet);

                Set<UUID> favoriteSet = new HashSet<>();
                for (String s : config.getStringList(key + ".favorites")) {
                    try { favoriteSet.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                }
                if (!favoriteSet.isEmpty()) favorites.put(uuid, favoriteSet);

                if (!config.getBoolean(key + ".notifications", true)) {
                    notificationsDisabled.add(uuid);
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    // ── Friends ───────────────────────────────────────────────────────────────

    public Set<UUID> getFriends(UUID uuid) {
        return friends.computeIfAbsent(uuid, k -> new HashSet<UUID>());
    }

    public boolean areFriends(UUID a, UUID b) {
        return getFriends(a).contains(b);
    }

    public void addFriends(UUID a, UUID b) {
        getFriends(a).add(b);
        getFriends(b).add(a);
        save(a);
        save(b);
    }

    public void removeFriend(UUID a, UUID b) {
        getFriends(a).remove(b);
        getFriends(b).remove(a);
        favorites.computeIfAbsent(a, k -> new HashSet<UUID>()).remove(b);
        favorites.computeIfAbsent(b, k -> new HashSet<UUID>()).remove(a);
        save(a);
        save(b);
    }

    public int getOnlineFriendsCount(UUID uuid) {
        int count = 0;
        for (UUID fid : getFriends(uuid)) {
            if (Bukkit.getPlayer(fid) != null) count++;
        }
        return count;
    }

    public List<Player> getOnlineFriends(UUID uuid) {
        List<Player> online = new ArrayList<>();
        for (UUID fid : getFriends(uuid)) {
            Player p = Bukkit.getPlayer(fid);
            if (p != null) online.add(p);
        }
        return online;
    }

    // ── Pending requests ─────────────────────────────────────────────────────

    public void sendRequest(UUID from, UUID to) {
        pending.put(from, to);
    }

    public boolean hasPendingFrom(UUID from, UUID to) {
        return to.equals(pending.get(from));
    }

    public UUID getIncomingRequest(UUID target) {
        for (Map.Entry<UUID, UUID> e : pending.entrySet()) {
            if (e.getValue().equals(target)) return e.getKey();
        }
        return null;
    }

    public void cancelRequest(UUID from) {
        pending.remove(from);
    }

    // ── Block list ───────────────────────────────────────────────────────────

    public Set<UUID> getBlocked(UUID uuid) {
        return blocked.computeIfAbsent(uuid, k -> new HashSet<UUID>());
    }

    public boolean isBlocked(UUID viewer, UUID target) {
        return getBlocked(viewer).contains(target) || getBlocked(target).contains(viewer);
    }

    public boolean hasBlockedPlayer(UUID blocker, UUID target) {
        return getBlocked(blocker).contains(target);
    }

    public void blockPlayer(UUID blocker, UUID target) {
        getBlocked(blocker).add(target);
        if (areFriends(blocker, target)) removeFriend(blocker, target);
        pending.remove(blocker);
        save(blocker);
    }

    public void unblockPlayer(UUID blocker, UUID target) {
        getBlocked(blocker).remove(target);
        save(blocker);
    }

    // ── Favorites ────────────────────────────────────────────────────────────

    public Set<UUID> getFavorites(UUID uuid) {
        return favorites.computeIfAbsent(uuid, k -> new HashSet<UUID>());
    }

    public boolean isFavorite(UUID player, UUID friend) {
        return getFavorites(player).contains(friend);
    }

    public void toggleFavorite(UUID player, UUID friend) {
        Set<UUID> favSet = getFavorites(player);
        if (favSet.contains(friend)) {
            favSet.remove(friend);
        } else {
            favSet.add(friend);
        }
        save(player);
    }

    // ── Notifications ────────────────────────────────────────────────────────

    public boolean hasNotificationsEnabled(UUID uuid) {
        return !notificationsDisabled.contains(uuid);
    }

    public void setNotifications(UUID uuid, boolean enabled) {
        if (enabled) {
            notificationsDisabled.remove(uuid);
        } else {
            notificationsDisabled.add(uuid);
        }
        save(uuid);
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private void save(UUID uuid) {
        List<String> friendList = new ArrayList<>();
        for (UUID f : getFriends(uuid)) friendList.add(f.toString());
        config.set(uuid + ".friends", friendList);

        List<String> blockedList = new ArrayList<>();
        for (UUID b : getBlocked(uuid)) blockedList.add(b.toString());
        config.set(uuid + ".blocked", blockedList);

        List<String> favoriteList = new ArrayList<>();
        for (UUID fav : getFavorites(uuid)) favoriteList.add(fav.toString());
        config.set(uuid + ".favorites", favoriteList);

        config.set(uuid + ".notifications", hasNotificationsEnabled(uuid));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            public void run() {
                try { config.save(file); } catch (IOException e) {
                    plugin.getLogger().warning("Failed to save friends: " + e.getMessage());
                }
            }
        });
    }
}
