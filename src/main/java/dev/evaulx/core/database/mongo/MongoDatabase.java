package dev.evaulx.core.database.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.database.IDatabase;
import dev.evaulx.core.models.*;
import org.bson.Document;

import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class MongoDatabase implements IDatabase {

    private final EvaulxCore plugin;
    private MongoClient client;
    private com.mongodb.client.MongoDatabase db;
    private MongoCollection<Document> players, punishments, ranks;

    public MongoDatabase(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean connect() {
        try {
            String host = plugin.getConfig().getString("database.mongodb.host", "localhost");
            int port    = plugin.getConfig().getInt("database.mongodb.port", 27017);
            String database = plugin.getConfig().getString("database.mongodb.database", "evaulxmc");
            String user  = plugin.getConfig().getString("database.mongodb.username", "");
            String pass  = plugin.getConfig().getString("database.mongodb.password", "");

            String uri;
            if (!user.isEmpty() && !pass.isEmpty()) {
                String authDb = plugin.getConfig().getString("database.mongodb.auth-database", "admin");
                uri = "mongodb://" + user + ":" + pass + "@" + host + ":" + port + "/" + authDb;
            } else {
                uri = "mongodb://" + host + ":" + port;
            }

            client = new MongoClient(new MongoClientURI(uri));
            db = client.getDatabase(database);
            players     = db.getCollection("players");
            punishments = db.getCollection("punishments");
            ranks       = db.getCollection("ranks");
            // Test connection
            db.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("MongoDB connection failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void disconnect() {
        if (client != null) client.close();
    }

    // ---- Profiles ----

    @Override
    public PlayerProfile loadProfile(UUID uuid, String name) {
        Document doc = players.find(eq("uuid", uuid.toString())).first();
        if (doc == null) return new PlayerProfile(uuid, name);
        PlayerProfile p = new PlayerProfile(uuid, name);
        p.setName(doc.getString("name"));
        p.setIp(doc.getString("ip"));
        p.setRankName(doc.getString("rank"));
        p.setChatColor(doc.getString("chatColor"));
        p.setNameColor(doc.getString("nameColor"));
        p.setTag(doc.getString("tag"));
        p.setBuildMode(Boolean.TRUE.equals(doc.getBoolean("buildMode")));
        if (persist("vanish")) p.setVanished(Boolean.TRUE.equals(doc.getBoolean("vanished")));
        if (persist("god")) p.setGodMode(Boolean.TRUE.equals(doc.getBoolean("godMode")));
        if (persist("social-spy")) p.setSocialSpy(Boolean.TRUE.equals(doc.getBoolean("socialSpy")));
        if (persist("msg-toggle") && doc.containsKey("msgToggled")) p.setMsgToggled(Boolean.TRUE.equals(doc.getBoolean("msgToggled")));
        if (persist("staff-mode")) p.setStaffMode(Boolean.TRUE.equals(doc.getBoolean("staffMode")));
        if (persist("disguise")) {
            p.setDisguised(Boolean.TRUE.equals(doc.getBoolean("disguised")));
            p.setDisguiseName(doc.getString("disguiseName"));
            p.setDisguiseSkin(doc.getString("disguiseSkin"));
            p.setDisguiseRank(doc.getString("disguiseRank"));
        }
        p.setFirstJoin(getLongSafe(doc, "firstJoin", 0L));
        p.setLastSeen(getLongSafe(doc, "lastSeen", 0L));
        List<String> extras = doc.getList("extraRanks", String.class);
        if (extras != null) extras.forEach(p::addExtraRank);
        List<String> perms = doc.getList("permissions", String.class);
        if (perms != null) perms.forEach(p::addPermission);
        return p;
    }

    @Override
    public void saveProfile(PlayerProfile p) {
        Document doc = new Document("uuid", p.getUuid().toString())
                .append("name", p.getName())
                .append("ip", p.getIp())
                .append("rank", p.getRankName())
                .append("chatColor", p.getChatColor())
                .append("nameColor", p.getNameColor())
                .append("tag", p.getTag())
                .append("buildMode", p.isBuildMode())
                .append("vanished", persist("vanish") && p.isVanished())
                .append("godMode", persist("god") && p.isGodMode())
                .append("socialSpy", persist("social-spy") && p.isSocialSpy())
                .append("msgToggled", !persist("msg-toggle") || p.isMsgToggled())
                .append("staffMode", persist("staff-mode") && p.isStaffMode())
                .append("disguised", persist("disguise") && p.isDisguised())
                .append("disguiseName", persist("disguise") ? p.getDisguiseName() : null)
                .append("disguiseSkin", persist("disguise") ? p.getDisguiseSkin() : null)
                .append("disguiseRank", persist("disguise") ? p.getDisguiseRank() : null)
                .append("firstJoin", p.getFirstJoin())
                .append("lastSeen", p.getLastSeen())
                .append("extraRanks", p.getExtraRanks())
                .append("permissions", p.getPermissions());
        players.replaceOne(eq("uuid", p.getUuid().toString()), doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    public int cleanupProfiles(Set<String> validRankNames, String defaultRankName) {
        Set<String> valid = normalizeRankSet(validRankNames);
        int changedProfiles = 0;

        try (MongoCursor<Document> cursor = players.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                boolean changed = false;
                String rank = doc.getString("rank");
                if (rank == null || !valid.contains(rank.toLowerCase(Locale.ENGLISH))) {
                    doc.put("rank", defaultRankName);
                    changed = true;
                }

                List<String> extras = doc.getList("extraRanks", String.class);
                List<String> cleaned = cleanRankList(extras, valid);
                if (!Objects.equals(extras == null ? Collections.emptyList() : extras, cleaned)) {
                    doc.put("extraRanks", cleaned);
                    changed = true;
                }

                if (!changed) continue;
                players.replaceOne(eq("uuid", doc.getString("uuid")), doc);
                changedProfiles++;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to clean Mongo profiles: " + e.getMessage());
        }
        return changedProfiles;
    }

    // ---- Punishments ----

    @Override
    public void savePunishment(Punishment pun) {
        punishments.insertOne(punishmentToDoc(pun));
    }

    @Override
    public void updatePunishment(Punishment pun) {
        punishments.replaceOne(eq("id", pun.getId()), punishmentToDoc(pun));
    }

    @Override
    public List<Punishment> getPunishments(UUID uuid) {
        List<Punishment> list = new ArrayList<>();
        try (MongoCursor<Document> cursor = punishments.find(eq("target", uuid.toString())).iterator()) {
            while (cursor.hasNext()) list.add(docToPunishment(cursor.next()));
        }
        return list;
    }

    @Override
    public List<Punishment> getPunishmentsByIp(String ip) {
        List<Punishment> list = new ArrayList<>();
        try (MongoCursor<Document> cursor = punishments.find(eq("targetIp", ip)).iterator()) {
            while (cursor.hasNext()) list.add(docToPunishment(cursor.next()));
        }
        return list;
    }

    @Override
    public Punishment getActivePunishment(UUID uuid, Punishment.Type type) {
        Document doc = punishments.find(
            and(eq("target", uuid.toString()), eq("type", type.name()), eq("active", true))
        ).first();
        return doc == null ? null : docToPunishment(doc);
    }

    // ---- Ranks ----

    @Override
    public void saveRank(Rank rank) {
        Document doc = rankToDoc(rank);
        ranks.replaceOne(eq("name", rank.getName()), doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    public void deleteRank(String name) {
        ranks.deleteOne(eq("name", name));
    }

    @Override
    public List<Rank> loadRanks() {
        List<Rank> list = new ArrayList<>();
        try (MongoCursor<Document> cursor = ranks.find().iterator()) {
            while (cursor.hasNext()) list.add(docToRank(cursor.next()));
        }
        return list;
    }

    // ---- Helpers ----

    private Document punishmentToDoc(Punishment p) {
        return new Document("id", p.getId())
                .append("target", p.getTarget().toString())
                .append("targetName", p.getTargetName())
                .append("targetIp", p.getTargetIp())
                .append("punisher", p.getPunisher() != null ? p.getPunisher().toString() : "CONSOLE")
                .append("punisherName", p.getPunisherName())
                .append("type", p.getType().name())
                .append("reason", p.getReason())
                .append("issued", p.getIssued())
                .append("expires", p.getExpires())
                .append("active", p.isActive())
                .append("silent", p.isSilent())
                .append("evidenceUrl", p.getEvidenceUrl())
                .append("appealStatus", p.getAppealStatus())
                .append("staffNote", p.getStaffNote())
                .append("internalNote", p.getInternalNote())
                .append("removedBy", p.getRemovedBy() != null ? p.getRemovedBy().toString() : null)
                .append("removedReason", p.getRemovedReason())
                .append("removedAt", p.getRemovedAt());
    }

    private Punishment docToPunishment(Document doc) {
        UUID target = UUID.fromString(doc.getString("target"));
        String targetName = doc.getString("targetName");
        String punisherStr = doc.getString("punisher");
        UUID punisher = (punisherStr == null || punisherStr.equals("CONSOLE")) ? null : UUID.fromString(punisherStr);
        String punisherName = doc.getString("punisherName");
        Punishment.Type type = Punishment.Type.valueOf(doc.getString("type"));
        String reason = doc.getString("reason");
        long expires = getLongSafe(doc, "expires", -1L);
        boolean silent = Boolean.TRUE.equals(doc.getBoolean("silent"));

        Punishment p = new Punishment(target, targetName, punisher, punisherName, type, reason, expires, silent);
        p.setId(doc.getString("id"));
        p.setIssued(getLongSafe(doc, "issued", 0L));
        p.setActive(Boolean.TRUE.equals(doc.getBoolean("active")));
        p.setTargetIp(doc.getString("targetIp"));
        p.setEvidenceUrl(doc.getString("evidenceUrl"));
        p.setAppealStatus(doc.getString("appealStatus"));
        p.setStaffNote(doc.getString("staffNote"));
        p.setInternalNote(doc.getString("internalNote"));
        String removedBy = doc.getString("removedBy");
        if (removedBy != null) p.setRemovedBy(UUID.fromString(removedBy));
        p.setRemovedReason(doc.getString("removedReason"));
        Long removedAt = doc.getLong("removedAt");
        if (removedAt != null) p.setRemovedAt(removedAt);
        return p;
    }

    private Document rankToDoc(Rank rank) {
        return new Document("name", rank.getName())
                .append("display", rank.getDisplay())
                .append("category", rank.getCategory())
                .append("permission", rank.getPermission())
                .append("prefix", rank.getPrefix())
                .append("suffix", rank.getSuffix())
                .append("color", rank.getColor())
                .append("weight", rank.getWeight())
                .append("default", rank.isDefault())
                .append("staff", rank.isStaff())
                .append("hidden", rank.isHidden())
                .append("permissions", rank.getPermissions())
                .append("inheritance", rank.getInheritance());
    }

    private Rank docToRank(Document doc) {
        Rank rank = new Rank(doc.getString("name"));
        String category = doc.getString("category");
        rank.setDisplay(doc.getString("display") != null ? doc.getString("display") : "");
        rank.setPermission(doc.getString("permission") != null ? doc.getString("permission") : "");
        rank.setPrefix(doc.getString("prefix") != null ? doc.getString("prefix") : "");
        rank.setSuffix(doc.getString("suffix") != null ? doc.getString("suffix") : "");
        rank.setColor(doc.getString("color") != null ? doc.getString("color") : "&f");
        rank.setWeight(doc.getInteger("weight", 0));
        rank.setDefault(Boolean.TRUE.equals(doc.getBoolean("default")));
        rank.setStaff(Boolean.TRUE.equals(doc.getBoolean("staff")));
        rank.setHidden(Boolean.TRUE.equals(doc.getBoolean("hidden")));
        rank.setCategory(category == null || category.trim().isEmpty() ? Rank.inferCategory(rank) : category);
        List<String> perms = doc.getList("permissions", String.class);
        if (perms != null) perms.forEach(rank::addPermission);
        List<String> inh = doc.getList("inheritance", String.class);
        if (inh != null) inh.forEach(rank::addInheritance);
        return rank;
    }

    private boolean persist(String key) {
        return plugin.getConfig().getBoolean("profile-persistence." + key, false);
    }

    /**
     * Reads a long from a Mongo document without unboxing NPEs. {@link Document#getLong(Object)}
     * returns a boxed {@code Long} that is {@code null} when the field is missing (e.g. legacy or
     * externally imported documents); assigning that to a primitive throws. This returns the
     * supplied default instead.
     */
    private long getLongSafe(Document doc, String key, long def) {
        Object value = doc.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        return def;
    }

    private Set<String> normalizeRankSet(Set<String> rankNames) {
        Set<String> normalized = new HashSet<>();
        if (rankNames == null) return normalized;
        for (String rankName : rankNames) {
            if (rankName != null) normalized.add(rankName.toLowerCase(Locale.ENGLISH));
        }
        return normalized;
    }

    private List<String> cleanRankList(List<String> ranks, Set<String> valid) {
        if (ranks == null || ranks.isEmpty()) return Collections.emptyList();
        List<String> cleaned = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String rank : ranks) {
            if (rank == null) continue;
            String key = rank.toLowerCase(Locale.ENGLISH);
            if (!valid.contains(key) || !seen.add(key)) continue;
            cleaned.add(rank);
        }
        return cleaned;
    }
}
