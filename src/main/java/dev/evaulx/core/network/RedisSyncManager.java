package dev.evaulx.core.network;

import com.google.gson.*;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.database.redis.RedisManager;
import dev.evaulx.core.models.Grant;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TaskUtil;
import dev.evaulx.core.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RedisSyncManager {

    private final EvaulxCore plugin;
    private final Gson gson = new Gson();
    private final RedisManager redis;
    private final String serverId;
    private final Map<String, RemoteStaffEntry> remoteStaff = new ConcurrentHashMap<>();

    public RedisSyncManager(EvaulxCore plugin) {
        this.plugin = plugin;
        this.redis = new RedisManager(plugin);
        this.serverId = plugin.getConfig().getString("server.server-id", "hub");
    }

    public boolean start() {
        boolean connected = redis.connect();
        if (!connected || !redis.isEnabled()) return connected;
        redis.subscribe(this::handleMessage);
        return true;
    }

    public void shutdown() {
        redis.disconnect();
    }

    public boolean isEnabled() {
        return redis.isEnabled();
    }

    public Collection<RemoteStaffEntry> getRemoteStaff() {
        return new ArrayList<>(remoteStaff.values());
    }

    public void publishPunishment(Punishment punishment) {
        JsonObject data = new JsonObject();
        data.addProperty("id", punishment.getId());
        data.addProperty("target", punishment.getTarget().toString());
        data.addProperty("targetName", punishment.getTargetName());
        data.addProperty("targetIp", punishment.getTargetIp());
        data.addProperty("punisherName", punishment.getPunisherName());
        data.addProperty("type", punishment.getType().name());
        data.addProperty("reason", punishment.getReason());
        data.addProperty("issued", punishment.getIssued());
        data.addProperty("expires", punishment.getExpires());
        data.addProperty("silent", punishment.isSilent());
        data.addProperty("appealStatus", punishment.getAppealStatus());
        data.addProperty("evidenceUrl", punishment.getEvidenceUrl());
        publish("PUNISHMENT", data);
    }

    public void publishRankChange(String playerName, UUID uuid, String oldRank, String newRank, String issuer) {
        JsonObject data = new JsonObject();
        data.addProperty("player", playerName);
        data.addProperty("uuid", uuid.toString());
        data.addProperty("oldRank", oldRank);
        data.addProperty("newRank", newRank);
        data.addProperty("issuer", issuer);
        publish("RANK_CHANGE", data);
    }

    public void publishGrant(Grant grant, boolean removed) {
        JsonObject data = new JsonObject();
        data.addProperty("player", grant.getTargetName());
        data.addProperty("uuid", grant.getTarget().toString());
        data.addProperty("rank", grant.getRankName());
        data.addProperty("issuer", grant.getIssuerName());
        data.addProperty("removed", removed);
        data.addProperty("duration", grant.getDurationString());
        publish("GRANT", data);
    }

    public void publishMaintenance(boolean enabled, String actor, String reason) {
        JsonObject data = new JsonObject();
        data.addProperty("enabled", enabled);
        data.addProperty("actor", actor);
        data.addProperty("reason", reason);
        publish("MAINTENANCE", data);
    }

    public void publishFreeze(UUID targetUuid, String targetName, boolean frozen, String actorName) {
        JsonObject data = new JsonObject();
        data.addProperty("uuid", targetUuid.toString());
        data.addProperty("targetName", targetName);
        data.addProperty("frozen", frozen);
        data.addProperty("actor", actorName);
        publish("FREEZE", data);
    }

    public void publishStaffChat(String playerName, String message) {
        JsonObject data = new JsonObject();
        data.addProperty("player", playerName);
        data.addProperty("message", message);
        publish("STAFF_CHAT", data);
    }

    public void publishStaffNotice(String type, String message) {
        JsonObject data = new JsonObject();
        data.addProperty("noticeType", type);
        data.addProperty("message", message);
        publish("STAFF_NOTICE", data);
    }

    public void publishStaffServerEvent(String playerName, UUID uuid, boolean joined) {
        JsonObject data = new JsonObject();
        data.addProperty("player", playerName);
        data.addProperty("uuid", uuid.toString());
        data.addProperty("joined", joined);
        addStaffStatus(data, uuid);
        publish("STAFF_SERVER_EVENT", data);
    }

    public void publishStaffStatus(Player player) {
        if (player == null || !plugin.getStaffRequestManager().canReceiveStaffAlerts(player)) return;
        JsonObject data = new JsonObject();
        data.addProperty("player", player.getName());
        data.addProperty("uuid", player.getUniqueId().toString());
        addStaffStatus(data, player.getUniqueId());
        publish("STAFF_STATUS", data);
    }

    public void publishBroadcast(String message) {
        JsonObject data = new JsonObject();
        data.addProperty("message", message);
        publish("BROADCAST", data);
    }

    public void publishVanish(String playerName, UUID uuid, boolean vanished) {
        JsonObject data = new JsonObject();
        data.addProperty("player", playerName);
        data.addProperty("uuid", uuid.toString());
        data.addProperty("vanished", vanished);
        publish("VANISH", data);
    }

    public void publishDisguise(String playerName, UUID uuid, String disguiseName, String skinName, String rankName, boolean enabled) {
        JsonObject data = new JsonObject();
        data.addProperty("player", playerName);
        data.addProperty("uuid", uuid.toString());
        data.addProperty("enabled", enabled);
        data.addProperty("disguiseName", disguiseName);
        data.addProperty("skinName", skinName);
        data.addProperty("rankName", rankName);
        publish("DISGUISE", data);
    }

    private void publish(String type, JsonObject data) {
        if (!redis.isEnabled()) return;
        JsonObject root = new JsonObject();
        root.addProperty("source", serverId);
        root.addProperty("type", type);
        root.addProperty("createdAt", System.currentTimeMillis());
        root.add("data", data);
        redis.publish(gson.toJson(root));
    }

    private void handleMessage(String raw) {
        try {
            JsonObject root = new JsonParser().parse(raw).getAsJsonObject();
            String source = getString(root, "source", "");
            if (serverId.equals(source)) return;

            String type = getString(root, "type", "");
            JsonObject data = root.getAsJsonObject("data");
            if (data == null) return;

            switch (type) {
                case "STAFF_CHAT":
                    handleStaffChat(data, source);
                    break;
                case "STAFF_NOTICE":
                    handleStaffNotice(data);
                    break;
                case "STAFF_SERVER_EVENT":
                    handleStaffServerEvent(data, source);
                    break;
                case "STAFF_STATUS":
                    handleStaffStatus(data, source);
                    break;
                case "BROADCAST":
                    TaskUtil.sync(() -> Bukkit.broadcastMessage(CC.color(getString(data, "message", ""))));
                    break;
                case "RANK_CHANGE":
                    handleRankChange(data);
                    break;
                case "GRANT":
                    handleGrant(data);
                    break;
                case "PUNISHMENT":
                    handlePunishment(data);
                    break;
                case "VANISH":
                    handleVanish(data);
                    break;
                case "DISGUISE":
                    handleDisguise(data, source);
                    break;
                case "MAINTENANCE":
                    handleMaintenance(data);
                    break;
                case "FREEZE":
                    handleFreeze(data);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to handle Redis sync message: " + e.getMessage());
        }
    }

    private void handleStaffChat(JsonObject data, String source) {
        String player = getString(data, "player", "Unknown");
        String message = getString(data, "message", "");
        TaskUtil.sync(() -> plugin.getStaffRequestManager().sendStaffChat(player + "@" + source, message, false));
    }

    private void handleStaffNotice(JsonObject data) {
        String message = getString(data, "message", "");
        TaskUtil.sync(() -> plugin.getStaffRequestManager().broadcastStaff(message));
    }

    private void handleStaffServerEvent(JsonObject data, String source) {
        String playerName = getString(data, "player", "Unknown");
        UUID uuid = UUID.fromString(getString(data, "uuid", "00000000-0000-0000-0000-000000000000"));
        boolean joined = data.has("joined") && data.get("joined").getAsBoolean();
        String key = source + ":" + uuid;
        if (joined) {
            remoteStaff.put(key, remoteStaffEntry(data, playerName, uuid, source));
        } else {
            remoteStaff.remove(key);
        }
        TaskUtil.sync(() -> {
            String messageKey = joined ? "staff-alerts.join-network" : "staff-alerts.leave-network";
            String fallback = joined
                    ? "&8[&cNetwork&8] &f{player} &7joined &f{server}&7."
                    : "&8[&cNetwork&8] &f{player} &7left &f{server}&7.";
            String message = plugin.getMessageManager().get(messageKey, fallback,
                    plugin.getMessageManager().placeholders("{player}", playerName, "{server}", source));
            plugin.getStaffRequestManager().broadcastStaff(message);
        });
    }

    private void handleStaffStatus(JsonObject data, String source) {
        String playerName = getString(data, "player", "Unknown");
        UUID uuid = UUID.fromString(getString(data, "uuid", "00000000-0000-0000-0000-000000000000"));
        remoteStaff.put(source + ":" + uuid, remoteStaffEntry(data, playerName, uuid, source));
    }

    private void handleRankChange(JsonObject data) {
        UUID uuid = UUID.fromString(getString(data, "uuid", ""));
        String playerName = getString(data, "player", "Unknown");
        String newRank = getString(data, "newRank", "");
        String issuer = getString(data, "issuer", "Unknown");

        TaskUtil.sync(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                PlayerProfile profile = plugin.getPlayerManager().loadProfile(player);
                plugin.getGrantManager().reconcileProfile(profile);
                plugin.getPlayerManager().applyPermissions(player, profile);
                plugin.getNameTagManager().applyNameTag(player);
            }
            plugin.getStaffRequestManager().broadcastStaff("&8[&cNetwork&8] &f" + playerName
                    + " &7rank updated to &f" + newRank + " &7by &f" + issuer + "&7.");
        });
    }

    private void handleGrant(JsonObject data) {
        String playerName = getString(data, "player", "Unknown");
        UUID uuid = UUID.fromString(getString(data, "uuid", "00000000-0000-0000-0000-000000000000"));
        String rank = getString(data, "rank", "Unknown");
        boolean removed = data.has("removed") && data.get("removed").getAsBoolean();

        TaskUtil.sync(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                PlayerProfile profile = plugin.getPlayerManager().loadProfile(player);
                plugin.getGrantManager().reconcileProfile(profile);
                plugin.getPlayerManager().applyPermissions(player, profile);
                plugin.getNameTagManager().applyNameTag(player);
            }
            plugin.getStaffRequestManager().broadcastStaff("&8[&cNetwork&8] &f" + playerName
                    + (removed ? " &7had grant removed: &f" : " &7received grant: &f") + rank);
        });
    }

    private void handlePunishment(JsonObject data) {
        UUID target = UUID.fromString(getString(data, "target", ""));
        String targetName = getString(data, "targetName", "Unknown");
        Punishment.Type type = Punishment.Type.valueOf(getString(data, "type", "WARN"));
        String reason = getString(data, "reason", "No reason specified");
        long expires = getLong(data, "expires", -1L);
        String punisher = getString(data, "punisherName", "Unknown");

        TaskUtil.sync(() -> {
            plugin.getStaffRequestManager().broadcastStaff("&8[&cNetwork Punishment&8] &f" + targetName
                    + " &7was punished by &f" + punisher + " &8- &7" + reason);
            Player player = Bukkit.getPlayer(target);
            if (player == null) return;
            if (type.isBan() || type == Punishment.Type.KICK) {
                player.kickPlayer(CC.color("&cYou have been punished.\n&7Reason: &f" + reason
                        + "\n&7Duration: &f" + TimeUtil.formatDuration(expires)));
            } else if (type.isMute()) {
                player.sendMessage(CC.color("&cYou have been muted. &7Reason: &f" + reason));
            }
        });
    }

    private void handleVanish(JsonObject data) {
        String playerName = getString(data, "player", "Unknown");
        boolean vanished = data.has("vanished") && data.get("vanished").getAsBoolean();
        TaskUtil.sync(() -> plugin.getStaffRequestManager().broadcastStaff("&8[&cNetwork&8] &f" + playerName
                + " &7is now " + (vanished ? "&avanished" : "&cvisible") + "&7."));
    }

    private void handleDisguise(JsonObject data, String source) {
        String playerName = getString(data, "player", "Unknown");
        boolean enabled = getBoolean(data, "enabled", false);
        String disguiseName = getString(data, "disguiseName", "none");
        String skinName = getString(data, "skinName", "none");
        String rankName = getString(data, "rankName", "none");
        TaskUtil.sync(() -> plugin.getStaffRequestManager().broadcastStaff("&8[&cNetwork&8] &f" + playerName
                + (enabled
                ? " &7disguised on &f" + source + " &7as &f" + disguiseName + " &7skin &f" + skinName + " &7rank &f" + rankName
                : " &7removed disguise on &f" + source)));
    }

    private void handleMaintenance(JsonObject data) {
        boolean enabled = getBoolean(data, "enabled", false);
        String actor = getString(data, "actor", "Unknown");
        String reason = getString(data, "reason", "");
        TaskUtil.sync(() -> {
            plugin.getConfig().set("maintenance.enabled", enabled);
            if (enabled && !reason.isEmpty()) plugin.getConfig().set("maintenance.reason", reason);
            plugin.saveConfig();
            plugin.getStaffRequestManager().broadcastStaff(
                    "&8[&cNetwork&8] &f" + actor + " &7" + (enabled ? "&cenabled" : "&adisabled")
                            + " &7maintenance mode" + (enabled && !reason.isEmpty() ? " &8(&f" + reason + "&8)" : "") + "&7.");
        });
    }

    private void handleFreeze(JsonObject data) {
        UUID uuid = UUID.fromString(getString(data, "uuid", "00000000-0000-0000-0000-000000000000"));
        String targetName = getString(data, "targetName", "Unknown");
        boolean frozen = getBoolean(data, "frozen", false);
        String actor = getString(data, "actor", "Unknown");
        TaskUtil.sync(() -> {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
                if (frozen) {
                    plugin.getStaffRequestManager().freeze(actor, target, "Network freeze by " + actor);
                } else {
                    plugin.getStaffRequestManager().unfreeze(actor, target, "Network unfreeze by " + actor);
                }
            }
            plugin.getStaffRequestManager().broadcastStaff(
                    "&8[&cNetwork&8] &f" + targetName + " &7was " + (frozen ? "&cfrozen" : "&athawed")
                            + " &7by &f" + actor + "&7.");
        });
    }

    private void addStaffStatus(JsonObject data, UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        PlayerProfile profile = player != null ? plugin.getPlayerManager().getProfile(player) : null;
        data.addProperty("vanished", profile != null && profile.isVanished());
        data.addProperty("staffMode", profile != null && profile.isStaffMode());
        data.addProperty("disguised", profile != null && profile.isDisguised());
        data.addProperty("disguiseName", profile != null ? profile.getDisguiseName() : null);
    }

    private RemoteStaffEntry remoteStaffEntry(JsonObject data, String playerName, UUID uuid, String source) {
        return new RemoteStaffEntry(
                playerName,
                uuid,
                source,
                getBoolean(data, "vanished", false),
                getBoolean(data, "staffMode", false),
                getBoolean(data, "disguised", false),
                getString(data, "disguiseName", null),
                System.currentTimeMillis()
        );
    }

    private String getString(JsonObject obj, String key, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsString();
    }

    private long getLong(JsonObject obj, String key, long fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsLong();
    }

    private boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsBoolean();
    }

    public static class RemoteStaffEntry {
        private final String name;
        private final UUID uuid;
        private final String serverId;
        private final boolean vanished;
        private final boolean staffMode;
        private final boolean disguised;
        private final String disguiseName;
        private final long updatedAt;

        private RemoteStaffEntry(String name, UUID uuid, String serverId, boolean vanished,
                                 boolean staffMode, boolean disguised, String disguiseName, long updatedAt) {
            this.name = name;
            this.uuid = uuid;
            this.serverId = serverId;
            this.vanished = vanished;
            this.staffMode = staffMode;
            this.disguised = disguised;
            this.disguiseName = disguiseName;
            this.updatedAt = updatedAt;
        }

        public String getName() { return name; }
        public UUID getUuid() { return uuid; }
        public String getServerId() { return serverId; }
        public boolean isVanished() { return vanished; }
        public boolean isStaffMode() { return staffMode; }
        public boolean isDisguised() { return disguised; }
        public String getDisguiseName() { return disguiseName; }
        public long getUpdatedAt() { return updatedAt; }
    }
}
