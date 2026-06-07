package dev.evaulx.core.database.flatfile;

import com.google.gson.*;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.database.IDatabase;
import dev.evaulx.core.models.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class FlatFileDatabase implements IDatabase {

    private final EvaulxCore plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File playersDir, punishmentsDir, ranksDir;

    public FlatFileDatabase(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean connect() {
        File baseDir = resolveBaseDir(plugin.getConfig().getString("database.flatfile.path", "data"));
        playersDir     = new File(baseDir, "players");
        punishmentsDir = new File(baseDir, "punishments");
        ranksDir       = new File(baseDir, "ranks");
        playersDir.mkdirs();
        punishmentsDir.mkdirs();
        ranksDir.mkdirs();
        return true;
    }

    private File resolveBaseDir(String configured) {
        if (configured == null || configured.trim().isEmpty()) {
            return plugin.getEvaulxDataFolder();
        }

        String normalized = configured.trim().replace('\\', '/');
        if (normalized.equalsIgnoreCase("data") || normalized.startsWith("data/")) {
            return new File(plugin.getDataFolder(), configured.trim());
        }

        if (normalized.equalsIgnoreCase("plugins/EvaulxMC/data")
                || normalized.equalsIgnoreCase("plugins/EvaulxMC/data/")) {
            return plugin.getEvaulxDataFolder();
        }

        File file = new File(configured.trim());
        if (file.isAbsolute()) {
            plugin.getLogger().warning("Ignoring external flatfile path '" + configured + "'. EvaulxMC stores flatfile data inside its own plugin folder.");
            return plugin.getEvaulxDataFolder();
        }
        return new File(plugin.getDataFolder(), configured.trim());
    }

    @Override
    public void disconnect() {}

    // ---- Profiles ----

    @Override
    public PlayerProfile loadProfile(UUID uuid, String name) {
        File f = new File(playersDir, uuid + ".json");
        if (!f.exists()) return new PlayerProfile(uuid, name);
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            JsonObject obj = gson.fromJson(r, JsonObject.class);
            PlayerProfile p = new PlayerProfile(uuid, name);
            p.setName(obj.get("name").getAsString());
            p.setIp(obj.has("ip") ? obj.get("ip").getAsString() : null);
            p.setRankName(obj.get("rank").getAsString());
            p.setChatColor(obj.has("chatColor") && !obj.get("chatColor").isJsonNull() ? obj.get("chatColor").getAsString() : "");
            p.setNameColor(obj.has("nameColor") && !obj.get("nameColor").isJsonNull() ? obj.get("nameColor").getAsString() : "");
            p.setTag(obj.has("tag") && !obj.get("tag").isJsonNull() ? obj.get("tag").getAsString() : "");
            p.setBuildMode(obj.has("buildMode") && obj.get("buildMode").getAsBoolean());
            if (persist("vanish")) p.setVanished(getBoolean(obj, "vanished", false));
            if (persist("god")) p.setGodMode(getBoolean(obj, "godMode", false));
            if (persist("social-spy")) p.setSocialSpy(getBoolean(obj, "socialSpy", false));
            if (persist("msg-toggle")) p.setMsgToggled(getBoolean(obj, "msgToggled", true));
            if (persist("staff-mode")) p.setStaffMode(getBoolean(obj, "staffMode", false));
            if (persist("disguise")) {
                p.setDisguised(getBoolean(obj, "disguised", false));
                p.setDisguiseName(getString(obj, "disguiseName", null));
                p.setDisguiseSkin(getString(obj, "disguiseSkin", null));
                p.setDisguiseRank(getString(obj, "disguiseRank", null));
            }
            p.setFirstJoin(obj.get("firstJoin").getAsLong());
            p.setLastSeen(obj.get("lastSeen").getAsLong());
            if (obj.has("extraRanks")) {
                obj.getAsJsonArray("extraRanks").forEach(e -> p.addExtraRank(e.getAsString()));
            }
            if (obj.has("permissions")) {
                obj.getAsJsonArray("permissions").forEach(e -> p.addPermission(e.getAsString()));
            }
            return p;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load profile " + uuid + ": " + e.getMessage());
            return new PlayerProfile(uuid, name);
        }
    }

    @Override
    public void saveProfile(PlayerProfile p) {
        File f = new File(playersDir, p.getUuid() + ".json");
        JsonObject obj = new JsonObject();
        obj.addProperty("uuid", p.getUuid().toString());
        obj.addProperty("name", p.getName());
        obj.addProperty("ip", p.getIp());
        obj.addProperty("rank", p.getRankName());
        obj.addProperty("chatColor", p.getChatColor());
        obj.addProperty("nameColor", p.getNameColor());
        obj.addProperty("tag", p.getTag());
        obj.addProperty("buildMode", p.isBuildMode());
        obj.addProperty("vanished", persist("vanish") && p.isVanished());
        obj.addProperty("godMode", persist("god") && p.isGodMode());
        obj.addProperty("socialSpy", persist("social-spy") && p.isSocialSpy());
        obj.addProperty("msgToggled", !persist("msg-toggle") || p.isMsgToggled());
        obj.addProperty("staffMode", persist("staff-mode") && p.isStaffMode());
        obj.addProperty("disguised", persist("disguise") && p.isDisguised());
        obj.addProperty("disguiseName", persist("disguise") ? p.getDisguiseName() : null);
        obj.addProperty("disguiseSkin", persist("disguise") ? p.getDisguiseSkin() : null);
        obj.addProperty("disguiseRank", persist("disguise") ? p.getDisguiseRank() : null);
        obj.addProperty("firstJoin", p.getFirstJoin());
        obj.addProperty("lastSeen", p.getLastSeen());
        JsonArray extras = new JsonArray();
        p.getExtraRanks().forEach(extra -> extras.add(new JsonPrimitive(extra)));
        obj.add("extraRanks", extras);
        JsonArray perms = new JsonArray();
        p.getPermissions().forEach(permission -> perms.add(new JsonPrimitive(permission)));
        obj.add("permissions", perms);
        write(f, obj);
    }

    @Override
    public int cleanupProfiles(Set<String> validRankNames, String defaultRankName) {
        Set<String> valid = normalizeRankSet(validRankNames);
        File[] files = playersDir == null ? null : playersDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return 0;

        int changedProfiles = 0;
        for (File f : files) {
            try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                JsonObject obj = gson.fromJson(r, JsonObject.class);
                if (obj == null) continue;

                boolean changed = false;
                String rank = getString(obj, "rank", defaultRankName);
                if (rank == null || !valid.contains(rank.toLowerCase(Locale.ENGLISH))) {
                    obj.addProperty("rank", defaultRankName);
                    changed = true;
                }

                JsonArray cleaned = new JsonArray();
                Set<String> seen = new HashSet<>();
                JsonArray extras = obj.has("extraRanks") && obj.get("extraRanks").isJsonArray()
                        ? obj.getAsJsonArray("extraRanks") : new JsonArray();
                for (JsonElement element : extras) {
                    if (element == null || element.isJsonNull()) continue;
                    String extra = element.getAsString();
                    String key = extra.toLowerCase(Locale.ENGLISH);
                    if (!valid.contains(key) || !seen.add(key)) {
                        changed = true;
                        continue;
                    }
                    cleaned.add(new JsonPrimitive(extra));
                }
                if (cleaned.size() != extras.size()) changed = true;
                obj.add("extraRanks", cleaned);

                if (changed) {
                    write(f, obj);
                    changedProfiles++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to clean profile " + f.getName() + ": " + e.getMessage());
            }
        }
        return changedProfiles;
    }

    // ---- Punishments ----

    @Override
    public void savePunishment(Punishment pun) {
        File f = new File(punishmentsDir, pun.getId() + ".json");
        write(f, punishmentToJson(pun));
    }

    @Override
    public void updatePunishment(Punishment pun) {
        savePunishment(pun);
    }

    @Override
    public List<Punishment> getPunishments(UUID uuid) {
        List<Punishment> list = new ArrayList<>();
        File[] files = punishmentsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return list;
        for (File f : files) {
            try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                JsonObject obj = gson.fromJson(r, JsonObject.class);
                if (obj.get("target").getAsString().equals(uuid.toString())) {
                    list.add(jsonToPunishment(obj));
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    @Override
    public List<Punishment> getPunishmentsByIp(String ip) {
        List<Punishment> list = new ArrayList<>();
        File[] files = punishmentsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return list;
        for (File f : files) {
            try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                JsonObject obj = gson.fromJson(r, JsonObject.class);
                if (obj.has("targetIp") && obj.get("targetIp").getAsString().equals(ip)) {
                    list.add(jsonToPunishment(obj));
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    @Override
    public Punishment getActivePunishment(UUID uuid, Punishment.Type type) {
        for (Punishment p : getPunishments(uuid)) {
            if (p.getType() == type && p.isActive()) return p;
        }
        return null;
    }

    // ---- Ranks ----

    @Override
    public void saveRank(Rank rank) {
        File f = new File(ranksDir, rank.getName().toLowerCase() + ".json");
        JsonObject obj = new JsonObject();
        obj.addProperty("name", rank.getName());
        obj.addProperty("display", rank.getDisplay());
        obj.addProperty("category", rank.getCategory());
        obj.addProperty("permission", rank.getPermission());
        obj.addProperty("prefix", rank.getPrefix());
        obj.addProperty("suffix", rank.getSuffix());
        obj.addProperty("color", rank.getColor());
        obj.addProperty("weight", rank.getWeight());
        obj.addProperty("default", rank.isDefault());
        obj.addProperty("staff", rank.isStaff());
        obj.addProperty("hidden", rank.isHidden());
        JsonArray perms = new JsonArray();
        rank.getPermissions().forEach(permission -> perms.add(new JsonPrimitive(permission)));
        obj.add("permissions", perms);
        JsonArray inh = new JsonArray();
        rank.getInheritance().forEach(inheritance -> inh.add(new JsonPrimitive(inheritance)));
        obj.add("inheritance", inh);
        write(f, obj);
    }

    @Override
    public void deleteRank(String name) {
        new File(ranksDir, name.toLowerCase() + ".json").delete();
    }

    @Override
    public List<Rank> loadRanks() {
        List<Rank> list = new ArrayList<>();
        File[] files = ranksDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return list;
        for (File f : files) {
            try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                JsonObject obj = gson.fromJson(r, JsonObject.class);
                Rank rank = new Rank(obj.get("name").getAsString());
                String category = obj.has("category") && !obj.get("category").isJsonNull() ? obj.get("category").getAsString() : "";
                rank.setDisplay(obj.has("display") && !obj.get("display").isJsonNull() ? obj.get("display").getAsString() : "");
                rank.setPermission(obj.has("permission") && !obj.get("permission").isJsonNull() ? obj.get("permission").getAsString() : "");
                rank.setPrefix(obj.has("prefix") ? obj.get("prefix").getAsString() : "");
                rank.setSuffix(obj.has("suffix") ? obj.get("suffix").getAsString() : "");
                rank.setColor(obj.has("color") ? obj.get("color").getAsString() : "&f");
                rank.setWeight(obj.has("weight") ? obj.get("weight").getAsInt() : 0);
                rank.setDefault(obj.has("default") && obj.get("default").getAsBoolean());
                rank.setStaff(obj.has("staff") && obj.get("staff").getAsBoolean());
                rank.setHidden(obj.has("hidden") && obj.get("hidden").getAsBoolean());
                rank.setCategory(category.isEmpty() ? Rank.inferCategory(rank) : category);
                if (obj.has("permissions")) obj.getAsJsonArray("permissions").forEach(e -> rank.addPermission(e.getAsString()));
                if (obj.has("inheritance")) obj.getAsJsonArray("inheritance").forEach(e -> rank.addInheritance(e.getAsString()));
                list.add(rank);
            } catch (Exception ignored) {}
        }
        return list;
    }

    // ---- Helpers ----

    private void write(File f, JsonObject obj) {
        // Write to a temp file first, then atomically replace the target. This guarantees
        // readers never observe a half-written (corrupt) file, even if the server crashes
        // mid-write or two async saves race on the same profile.
        File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
            gson.toJson(obj, w);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to write " + f.getName() + ": " + e.getMessage());
            tmp.delete();
            return;
        }
        try {
            Files.move(tmp.toPath(), f.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception atomicFail) {
            // Some filesystems don't support ATOMIC_MOVE across the same dir; fall back.
            try {
                Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to finalize write for " + f.getName() + ": " + e.getMessage());
                tmp.delete();
            }
        }
    }

    private boolean persist(String key) {
        return plugin.getConfig().getBoolean("profile-persistence." + key, false);
    }

    private boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsBoolean();
    }

    private Set<String> normalizeRankSet(Set<String> rankNames) {
        Set<String> normalized = new HashSet<>();
        if (rankNames == null) return normalized;
        for (String rankName : rankNames) {
            if (rankName != null) normalized.add(rankName.toLowerCase(Locale.ENGLISH));
        }
        return normalized;
    }

    private String getString(JsonObject obj, String key, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsString();
    }

    private JsonObject punishmentToJson(Punishment p) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", p.getId());
        obj.addProperty("target", p.getTarget().toString());
        obj.addProperty("targetName", p.getTargetName());
        obj.addProperty("targetIp", p.getTargetIp());
        obj.addProperty("punisher", p.getPunisher() != null ? p.getPunisher().toString() : "CONSOLE");
        obj.addProperty("punisherName", p.getPunisherName());
        obj.addProperty("type", p.getType().name());
        obj.addProperty("reason", p.getReason());
        obj.addProperty("issued", p.getIssued());
        obj.addProperty("expires", p.getExpires());
        obj.addProperty("active", p.isActive());
        obj.addProperty("silent", p.isSilent());
        obj.addProperty("evidenceUrl", p.getEvidenceUrl());
        obj.addProperty("appealStatus", p.getAppealStatus());
        obj.addProperty("staffNote", p.getStaffNote());
        obj.addProperty("internalNote", p.getInternalNote());
        if (p.getRemovedBy() != null) obj.addProperty("removedBy", p.getRemovedBy().toString());
        if (p.getRemovedReason() != null) obj.addProperty("removedReason", p.getRemovedReason());
        obj.addProperty("removedAt", p.getRemovedAt());
        return obj;
    }

    private Punishment jsonToPunishment(JsonObject obj) {
        UUID target = UUID.fromString(obj.get("target").getAsString());
        String targetName = obj.get("targetName").getAsString();
        String punisherStr = obj.get("punisher").getAsString();
        UUID punisher = punisherStr.equals("CONSOLE") ? null : UUID.fromString(punisherStr);
        String punisherName = obj.get("punisherName").getAsString();
        Punishment.Type type = Punishment.Type.valueOf(obj.get("type").getAsString());
        String reason = obj.get("reason").getAsString();
        long expires = obj.get("expires").getAsLong();
        boolean silent = obj.has("silent") && obj.get("silent").getAsBoolean();

        Punishment p = new Punishment(target, targetName, punisher, punisherName, type, reason, expires, silent);
        p.setId(obj.get("id").getAsString());
        p.setIssued(obj.get("issued").getAsLong());
        p.setActive(obj.get("active").getAsBoolean());
        if (obj.has("targetIp") && !obj.get("targetIp").isJsonNull()) p.setTargetIp(obj.get("targetIp").getAsString());
        if (obj.has("evidenceUrl") && !obj.get("evidenceUrl").isJsonNull()) p.setEvidenceUrl(obj.get("evidenceUrl").getAsString());
        if (obj.has("appealStatus") && !obj.get("appealStatus").isJsonNull()) p.setAppealStatus(obj.get("appealStatus").getAsString());
        if (obj.has("staffNote") && !obj.get("staffNote").isJsonNull()) p.setStaffNote(obj.get("staffNote").getAsString());
        if (obj.has("internalNote") && !obj.get("internalNote").isJsonNull()) p.setInternalNote(obj.get("internalNote").getAsString());
        if (obj.has("removedBy") && !obj.get("removedBy").isJsonNull()) p.setRemovedBy(UUID.fromString(obj.get("removedBy").getAsString()));
        if (obj.has("removedReason")) p.setRemovedReason(obj.get("removedReason").getAsString());
        if (obj.has("removedAt")) p.setRemovedAt(obj.get("removedAt").getAsLong());
        return p;
    }
}
