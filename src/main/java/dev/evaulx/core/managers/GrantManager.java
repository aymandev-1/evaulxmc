package dev.evaulx.core.managers;

import com.google.gson.*;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Grant;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GrantManager {

    private final EvaulxCore plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Grant> grants = new ConcurrentHashMap<>();
    private final Map<String, PendingGrant> pendingGrants = new ConcurrentHashMap<>();
    private final Set<String> expiryReminders = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private File grantsFile;
    private File pendingFile;
    private BukkitTask expiryTask;

    public GrantManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File dataDir = plugin.getEvaulxDataFolder();
        if (!dataDir.exists()) dataDir.mkdirs();
        grantsFile = new File(dataDir, "grants.json");
        pendingFile = new File(dataDir, "pending-grants.json");
        loadGrants();
        loadPendingGrants();
        startExpiryTask();
    }

    public void shutdown() {
        if (expiryTask != null) expiryTask.cancel();
        saveGrants();
        savePendingGrants();
    }

    public Grant grant(CommandSender issuer, OfflinePlayer target, Rank rank, long expiresAt, String reason) {
        PlayerProfile profile = getProfile(target);
        profile.addExtraRank(rank.getName());
        profile.setName(safeName(target));
        saveProfile(profile);

        Grant grant = new Grant(target.getUniqueId(), safeName(target), rank.getName(), issuer.getName(), reason, expiresAt);
        grants.put(grant.getId(), grant);
        saveGrants();

        applyOnline(target.getUniqueId(), profile, grant);
        plugin.getDiscordManager().sendGrant(grant);
        if (plugin.getRedisSyncManager() != null) plugin.getRedisSyncManager().publishGrant(grant, false);
        return grant;
    }

    public PendingGrant requestGrant(CommandSender issuer, OfflinePlayer target, Rank rank, long expiresAt, String reason) {
        PendingGrant pending = new PendingGrant(target.getUniqueId(), safeName(target), rank.getName(), issuer.getName(), reason, expiresAt);
        pendingGrants.put(pending.getId(), pending);
        savePendingGrants();
        plugin.getStaffRequestManager().broadcastStaff("&8[&6Grant Request&8] &f" + issuer.getName()
                + " &7requested &f" + rank.getName() + " &7for &f" + safeName(target)
                + " &8(ID " + pending.getId() + ")", "evaulx.grant.approve");
        plugin.getStaffRequestManager().logAction(issuer.getName(), "REQUEST_GRANT", safeName(target),
                rank.getName() + " | " + reason);
        return pending;
    }

    public PendingGrant getPendingGrant(String id) {
        if (id == null) return null;
        return pendingGrants.get(id.toUpperCase(Locale.ENGLISH));
    }

    public List<PendingGrant> getPendingGrants() {
        List<PendingGrant> list = new ArrayList<>(pendingGrants.values());
        list.sort((a, b) -> Long.compare(b.getRequestedAt(), a.getRequestedAt()));
        return list;
    }

    public Grant approveGrant(String id, CommandSender approver) {
        PendingGrant pending = getPendingGrant(id);
        if (pending == null) return null;

        Rank rank = plugin.getRankManager().getRank(pending.getRankName());
        if (rank == null) return null;

        OfflinePlayer target = Bukkit.getOfflinePlayer(pending.getTarget());
        Grant grant = grant(approver, target, rank, pending.getExpiresAt(),
                pending.getReason() + " | approved request " + pending.getId() + " by " + approver.getName());
        pendingGrants.remove(pending.getId());
        savePendingGrants();
        plugin.getStaffRequestManager().logAction(approver.getName(), "APPROVE_GRANT", pending.getTargetName(),
                pending.getRankName() + " | request " + pending.getId());
        return grant;
    }

    public boolean denyGrant(String id, CommandSender denier, String reason) {
        PendingGrant pending = getPendingGrant(id);
        if (pending == null) return false;
        pendingGrants.remove(pending.getId());
        savePendingGrants();
        plugin.getStaffRequestManager().logAction(denier.getName(), "DENY_GRANT", pending.getTargetName(),
                pending.getRankName() + " | " + reason);
        return true;
    }

    public boolean removeGrant(String id, CommandSender remover, String reason) {
        Grant grant = grants.get(id.toUpperCase());
        if (grant == null || !grant.isStoredActive()) return false;

        grant.setActive(false);
        saveGrants();
        removeRankIfUnused(grant.getTarget(), grant.getTargetName(), grant.getRankName());

        plugin.getStaffRequestManager().logAction(remover.getName(), "REMOVE_GRANT", grant.getTargetName(),
                grant.getRankName() + " | " + reason);
        if (plugin.getRedisSyncManager() != null) plugin.getRedisSyncManager().publishGrant(grant, true);
        return true;
    }

    public void reconcileProfile(PlayerProfile profile) {
        boolean changed = false;
        for (Grant grant : getGrants(profile.getUuid())) {
            if (!grant.isStoredActive()) continue;
            if (grant.isExpired()) {
                grant.setActive(false);
                if (!hasActiveGrant(profile.getUuid(), grant.getRankName(), grant.getId())) {
                    profile.removeExtraRank(grant.getRankName());
                }
                changed = true;
                continue;
            }
            if (!profile.getExtraRanks().contains(grant.getRankName())) {
                profile.addExtraRank(grant.getRankName());
                changed = true;
            }
        }

        if (changed) {
            saveGrants();
            plugin.getPlayerManager().saveProfile(profile);
        }
    }

    public List<Grant> getGrants(UUID uuid) {
        List<Grant> list = new ArrayList<>();
        for (Grant grant : grants.values()) {
            if (grant.getTarget().equals(uuid)) list.add(grant);
        }
        list.sort((a, b) -> Long.compare(b.getIssuedAt(), a.getIssuedAt()));
        return list;
    }

    public List<Grant> getActiveGrants() {
        List<Grant> list = new ArrayList<>();
        for (Grant grant : grants.values()) {
            if (grant.isActive()) list.add(grant);
        }
        list.sort((a, b) -> Long.compare(a.getExpiresAt(), b.getExpiresAt()));
        return list;
    }

    public List<Grant> searchGrants(String query, int max) {
        String lowered = query.toLowerCase(Locale.ENGLISH);
        List<Grant> list = new ArrayList<>();
        for (Grant grant : grants.values()) {
            if (matches(grant.getTargetName(), lowered)
                    || matches(grant.getRankName(), lowered)
                    || matches(grant.getIssuerName(), lowered)
                    || matches(grant.getReason(), lowered)
                    || matches(grant.getId(), lowered)) {
                list.add(grant);
                if (list.size() >= max) break;
            }
        }
        list.sort((a, b) -> Long.compare(b.getIssuedAt(), a.getIssuedAt()));
        return list;
    }

    public int getActiveGrantCount() {
        int count = 0;
        for (Grant grant : grants.values()) {
            if (grant.isActive()) count++;
        }
        return count;
    }

    public int getStoredGrantCount() {
        return grants.size();
    }

    public int cleanupInvalidRanks(Set<String> validRankNames) {
        Set<String> valid = new HashSet<>();
        if (validRankNames != null) {
            for (String rankName : validRankNames) {
                if (rankName != null) valid.add(rankName.toLowerCase(Locale.ENGLISH));
            }
        }

        int changed = 0;
        for (Grant grant : grants.values()) {
            if (!grant.isStoredActive()) continue;
            if (valid.contains(grant.getRankName().toLowerCase(Locale.ENGLISH))) continue;
            grant.setActive(false);
            changed++;
        }

        Iterator<Map.Entry<String, PendingGrant>> iterator = pendingGrants.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingGrant pending = iterator.next().getValue();
            if (valid.contains(pending.getRankName().toLowerCase(Locale.ENGLISH))) continue;
            iterator.remove();
            changed++;
        }

        if (changed > 0) {
            saveGrants();
            savePendingGrants();
        }
        return changed;
    }

    public int countInvalidRankReferences(Set<String> validRankNames) {
        Set<String> valid = new HashSet<>();
        if (validRankNames != null) {
            for (String rankName : validRankNames) {
                if (rankName != null) valid.add(rankName.toLowerCase(Locale.ENGLISH));
            }
        }

        int count = 0;
        for (Grant grant : grants.values()) {
            if (grant.isStoredActive() && !valid.contains(grant.getRankName().toLowerCase(Locale.ENGLISH))) count++;
        }
        for (PendingGrant pending : pendingGrants.values()) {
            if (!valid.contains(pending.getRankName().toLowerCase(Locale.ENGLISH))) count++;
        }
        return count;
    }

    private PlayerProfile getProfile(OfflinePlayer target) {
        Player online = target.getPlayer();
        PlayerProfile loaded = online != null ? plugin.getPlayerManager().getProfile(online) : plugin.getPlayerManager().getProfile(target.getUniqueId());
        if (loaded != null) return loaded;
        return plugin.getDatabaseManager().loadProfile(target.getUniqueId(), safeName(target));
    }

    private void saveProfile(PlayerProfile profile) {
        if (plugin.getPlayerManager().isLoaded(profile.getUuid())) {
            plugin.getPlayerManager().saveProfile(profile);
            return;
        }
        TaskUtil.async(() -> plugin.getDatabaseManager().saveProfile(profile));
    }

    private void applyOnline(UUID uuid, PlayerProfile profile, Grant grant) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        plugin.getPlayerManager().applyPermissions(player, profile);
        plugin.getNameTagManager().applyNameTag(player);
        if (grant != null) {
            String rankName = grant.getRankName();
            String duration = grant.getExpiresAt() > 0 ? grant.getDurationString() : "Permanent";
            String issuer = grant.getIssuerName();
            String grantId = grant.getId();
            player.sendMessage(CC.color(" "));
            player.sendMessage(CC.color("&8&m----------------------------------------"));
            player.sendMessage(CC.color(" "));
            player.sendMessage(CC.color("   &6&l✦ NEW RANK GRANTED!"));
            player.sendMessage(CC.color(" "));
            player.sendMessage(CC.color("   &7Rank: &6&l" + rankName));
            player.sendMessage(CC.color("   &7Duration: &f" + duration));
            player.sendMessage(CC.color("   &7Granted by: &f" + issuer));
            player.sendMessage(CC.color("   &7Grant ID: &8#&f" + grantId));
            player.sendMessage(CC.color(" "));
            player.sendMessage(CC.color("   &7Your new rank is now active. Enjoy!"));
            player.sendMessage(CC.color(" "));
            player.sendMessage(CC.color("&8&m----------------------------------------"));
            player.sendMessage(CC.color(" "));
            player.sendTitle(CC.color("&6&l✦ Rank Granted!"), CC.color("&7You received &6" + rankName));
        } else {
            player.sendMessage(CC.color("&8[&6Grant&8] &7Your rank access has been updated."));
        }
    }

    private void removeRankIfUnused(UUID uuid, String name, String rankName) {
        if (hasActiveGrant(uuid, rankName, null)) return;

        PlayerProfile profile;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) profile = plugin.getPlayerManager().getProfile(player);
        else profile = plugin.getDatabaseManager().loadProfile(uuid, name);

        if (profile == null) return;
        profile.removeExtraRank(rankName);
        saveProfile(profile);
        applyOnline(uuid, profile, null);
        if (player != null) {
            player.sendMessage(CC.color("&8[&6Grant&8] &7Your &f" + rankName + " &7grant was removed."));
        }
    }

    private boolean hasActiveGrant(UUID uuid, String rankName, String ignoredId) {
        for (Grant grant : grants.values()) {
            if (ignoredId != null && grant.getId().equalsIgnoreCase(ignoredId)) continue;
            if (grant.getTarget().equals(uuid)
                    && grant.getRankName().equalsIgnoreCase(rankName)
                    && grant.isActive()) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String value, String query) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains(query);
    }

    private String safeName(OfflinePlayer target) {
        return target.getName() == null ? target.getUniqueId().toString() : target.getName();
    }

    private void startExpiryTask() {
        if (expiryTask != null) expiryTask.cancel();
        expiryTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            boolean changed = false;
            long now = System.currentTimeMillis();
            long reminderWindow = Math.max(0L, plugin.getConfig().getLong(
                    "staff-tools.grants.expiry-reminder-minutes", 60L)) * 60_000L;
            for (Grant grant : grants.values()) {
                if (!grant.isStoredActive()) continue;
                if (grant.isExpired()) {
                    grant.setActive(false);
                    expiryReminders.remove(grant.getId());
                    removeRankIfUnused(grant.getTarget(), grant.getTargetName(), grant.getRankName());
                    plugin.getStaffRequestManager().broadcastStaff("&8[&cGrant&8] &f" + grant.getTargetName()
                            + "'s &f" + grant.getRankName() + " &7grant expired. &8(ID: &f" + grant.getId() + "&8)");
                    // Notify target if online (removeRankIfUnused sends the general message; this is grant-specific)
                    Player grantedPlayer = Bukkit.getPlayer(grant.getTarget());
                    if (grantedPlayer != null) {
                        grantedPlayer.sendMessage(CC.color("&8[&6Grant&8] &7Your &f" + grant.getRankName()
                                + " &7grant has expired. &8(ID: &f" + grant.getId() + "&8)"));
                    }
                    changed = true;
                    continue;
                }
                if (reminderWindow <= 0L || grant.getExpiresAt() <= 0L || grant.getExpiresAt() == -1L) continue;
                long remaining = grant.getExpiresAt() - now;
                if (remaining > reminderWindow || !expiryReminders.add(grant.getId())) continue;
                plugin.getStaffRequestManager().broadcastStaff("&8[&eGrant&8] &f" + grant.getTargetName()
                        + "'s &f" + grant.getRankName() + " &7grant expires in &f"
                        + grant.getDurationString() + "&7.");
            }
            if (changed) saveGrants();
        }, 20L * 60L, 20L * 60L);
    }

    private void loadGrants() {
        if (grantsFile == null || !grantsFile.exists()) return;
        grants.clear();
        try (Reader reader = new InputStreamReader(new FileInputStream(grantsFile), StandardCharsets.UTF_8)) {
            JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                Grant grant = fromJson(element.getAsJsonObject());
                grants.put(grant.getId(), grant);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load grants: " + e.getMessage());
        }
    }

    private void loadPendingGrants() {
        if (pendingFile == null || !pendingFile.exists()) return;
        pendingGrants.clear();
        try (Reader reader = new InputStreamReader(new FileInputStream(pendingFile), StandardCharsets.UTF_8)) {
            JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                PendingGrant pending = pendingFromJson(element.getAsJsonObject());
                pendingGrants.put(pending.getId(), pending);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load pending grants: " + e.getMessage());
        }
    }

    private void saveGrants() {
        if (grantsFile == null) return;
        JsonArray array = new JsonArray();
        for (Grant grant : grants.values()) {
            array.add(toJson(grant));
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(grantsFile), StandardCharsets.UTF_8)) {
            gson.toJson(array, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save grants: " + e.getMessage());
        }
    }

    private void savePendingGrants() {
        if (pendingFile == null) return;
        JsonArray array = new JsonArray();
        for (PendingGrant pending : pendingGrants.values()) {
            array.add(pendingToJson(pending));
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(pendingFile), StandardCharsets.UTF_8)) {
            gson.toJson(array, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save pending grants: " + e.getMessage());
        }
    }

    private JsonObject toJson(Grant grant) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", grant.getId());
        obj.addProperty("target", grant.getTarget().toString());
        obj.addProperty("targetName", grant.getTargetName());
        obj.addProperty("rankName", grant.getRankName());
        obj.addProperty("issuerName", grant.getIssuerName());
        obj.addProperty("reason", grant.getReason());
        obj.addProperty("issuedAt", grant.getIssuedAt());
        obj.addProperty("expiresAt", grant.getExpiresAt());
        obj.addProperty("active", grant.isStoredActive());
        return obj;
    }

    private Grant fromJson(JsonObject obj) {
        Grant grant = new Grant(
                UUID.fromString(obj.get("target").getAsString()),
                getString(obj, "targetName", "Unknown"),
                getString(obj, "rankName", "default"),
                getString(obj, "issuerName", "CONSOLE"),
                getString(obj, "reason", "No reason specified"),
                getLong(obj, "expiresAt", -1L)
        );
        grant.setId(getString(obj, "id", grant.getId()));
        grant.setIssuedAt(getLong(obj, "issuedAt", System.currentTimeMillis()));
        grant.setActive(!obj.has("active") || obj.get("active").getAsBoolean());
        return grant;
    }

    private JsonObject pendingToJson(PendingGrant pending) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", pending.getId());
        obj.addProperty("target", pending.getTarget().toString());
        obj.addProperty("targetName", pending.getTargetName());
        obj.addProperty("rankName", pending.getRankName());
        obj.addProperty("requesterName", pending.getRequesterName());
        obj.addProperty("reason", pending.getReason());
        obj.addProperty("requestedAt", pending.getRequestedAt());
        obj.addProperty("expiresAt", pending.getExpiresAt());
        return obj;
    }

    private PendingGrant pendingFromJson(JsonObject obj) {
        PendingGrant pending = new PendingGrant(
                UUID.fromString(obj.get("target").getAsString()),
                getString(obj, "targetName", "Unknown"),
                getString(obj, "rankName", "default"),
                getString(obj, "requesterName", "CONSOLE"),
                getString(obj, "reason", "No reason specified"),
                getLong(obj, "expiresAt", -1L)
        );
        pending.setId(getString(obj, "id", pending.getId()));
        pending.setRequestedAt(getLong(obj, "requestedAt", System.currentTimeMillis()));
        return pending;
    }

    private String getString(JsonObject obj, String key, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsString();
    }

    private long getLong(JsonObject obj, String key, long fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsLong();
    }

    public static class PendingGrant {
        private String id;
        private final UUID target;
        private final String targetName;
        private final String rankName;
        private final String requesterName;
        private final String reason;
        private long requestedAt;
        private final long expiresAt;

        public PendingGrant(UUID target, String targetName, String rankName, String requesterName, String reason, long expiresAt) {
            this.id = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ENGLISH);
            this.target = target;
            this.targetName = targetName;
            this.rankName = rankName;
            this.requesterName = requesterName;
            this.reason = reason;
            this.requestedAt = System.currentTimeMillis();
            this.expiresAt = expiresAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id == null ? this.id : id.toUpperCase(Locale.ENGLISH); }
        public UUID getTarget() { return target; }
        public String getTargetName() { return targetName; }
        public String getRankName() { return rankName; }
        public String getRequesterName() { return requesterName; }
        public String getReason() { return reason; }
        public long getRequestedAt() { return requestedAt; }
        public void setRequestedAt(long requestedAt) { this.requestedAt = requestedAt; }
        public long getExpiresAt() { return expiresAt; }
    }
}
