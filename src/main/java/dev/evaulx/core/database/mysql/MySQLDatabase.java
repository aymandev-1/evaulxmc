package dev.evaulx.core.database.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.database.IDatabase;
import dev.evaulx.core.models.*;

import java.sql.*;
import java.util.*;

public class MySQLDatabase implements IDatabase {

    private final EvaulxCore plugin;
    private HikariDataSource ds;

    public MySQLDatabase(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean connect() {
        try {
            HikariConfig cfg = new HikariConfig();
            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port    = plugin.getConfig().getInt("database.mysql.port", 3306);
            String db   = plugin.getConfig().getString("database.mysql.database", "evaulxmc");
            String user = plugin.getConfig().getString("database.mysql.username", "root");
            String pass = plugin.getConfig().getString("database.mysql.password", "");
            int pool    = plugin.getConfig().getInt("database.mysql.pool-size", 10);

            cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&autoReconnect=true");
            cfg.setUsername(user);
            cfg.setPassword(pass);
            cfg.setMaximumPoolSize(pool);
            cfg.setConnectionTimeout(5000);
            cfg.setPoolName("EvaulxMC-Pool");
            ds = new HikariDataSource(cfg);
            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("MySQL connection failed: " + e.getMessage());
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS evaulx_players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16), ip VARCHAR(45), " +
                    "rank_name VARCHAR(32), first_join BIGINT, last_seen BIGINT, " +
                    "extra_ranks TEXT, permissions TEXT, chat_color VARCHAR(16), " +
                    "name_color VARCHAR(16), tag VARCHAR(64), build_mode TINYINT(1), " +
                    "vanished TINYINT(1), god_mode TINYINT(1), social_spy TINYINT(1), " +
                    "msg_toggled TINYINT(1), staff_mode TINYINT(1), disguised TINYINT(1), " +
                    "disguise_name VARCHAR(16), disguise_skin VARCHAR(16), disguise_rank VARCHAR(32))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS evaulx_punishments (" +
                    "id VARCHAR(16) PRIMARY KEY, target VARCHAR(36), target_name VARCHAR(16), " +
                    "target_ip VARCHAR(45), punisher VARCHAR(36), punisher_name VARCHAR(16), " +
                    "type VARCHAR(16), reason TEXT, issued BIGINT, expires BIGINT, " +
                    "active TINYINT(1), silent TINYINT(1), removed_by VARCHAR(36), " +
                    "removed_reason TEXT, removed_at BIGINT, evidence_url TEXT, appeal_status VARCHAR(32), " +
                    "staff_note TEXT, internal_note TEXT)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS evaulx_ranks (" +
                    "name VARCHAR(32) PRIMARY KEY, display VARCHAR(64), category VARCHAR(16), rank_permission VARCHAR(64), prefix VARCHAR(64), suffix VARCHAR(64), " +
                    "color VARCHAR(8), weight INT, is_default TINYINT(1), is_staff TINYINT(1), is_hidden TINYINT(1), " +
                    "permissions TEXT, inheritance TEXT)");

            addColumnIfMissing(st, "evaulx_players", "chat_color VARCHAR(16)");
            addColumnIfMissing(st, "evaulx_players", "name_color VARCHAR(16)");
            addColumnIfMissing(st, "evaulx_players", "tag VARCHAR(64)");
            addColumnIfMissing(st, "evaulx_players", "build_mode TINYINT(1)");
            addColumnIfMissing(st, "evaulx_players", "vanished TINYINT(1)");
            addColumnIfMissing(st, "evaulx_players", "god_mode TINYINT(1)");
            addColumnIfMissing(st, "evaulx_players", "social_spy TINYINT(1)");
            addColumnIfMissing(st, "evaulx_players", "msg_toggled TINYINT(1)");
            addColumnIfMissing(st, "evaulx_players", "staff_mode TINYINT(1)");
            addColumnIfMissing(st, "evaulx_players", "disguised TINYINT(1)");
            addColumnIfMissing(st, "evaulx_players", "disguise_name VARCHAR(16)");
            addColumnIfMissing(st, "evaulx_players", "disguise_skin VARCHAR(16)");
            addColumnIfMissing(st, "evaulx_players", "disguise_rank VARCHAR(32)");
            addColumnIfMissing(st, "evaulx_ranks", "display VARCHAR(64)");
            addColumnIfMissing(st, "evaulx_ranks", "category VARCHAR(16)");
            addColumnIfMissing(st, "evaulx_ranks", "rank_permission VARCHAR(64)");
            addColumnIfMissing(st, "evaulx_ranks", "is_hidden TINYINT(1)");
            addColumnIfMissing(st, "evaulx_punishments", "evidence_url TEXT");
            addColumnIfMissing(st, "evaulx_punishments", "appeal_status VARCHAR(32)");
            addColumnIfMissing(st, "evaulx_punishments", "staff_note TEXT");
            addColumnIfMissing(st, "evaulx_punishments", "internal_note TEXT");
        }
    }

    private void addColumnIfMissing(Statement st, String table, String definition) {
        try {
            st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + definition);
        } catch (SQLException ignored) {
        }
    }

    @Override
    public void disconnect() {
        if (ds != null && !ds.isClosed()) ds.close();
    }

    private Connection getConn() throws SQLException { return ds.getConnection(); }

    // ---- Profiles ----

    @Override
    public PlayerProfile loadProfile(UUID uuid, String name) {
        try (Connection con = getConn();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM evaulx_players WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return new PlayerProfile(uuid, name);
            PlayerProfile p = new PlayerProfile(uuid, name);
            p.setName(rs.getString("name"));
            p.setIp(rs.getString("ip"));
            p.setRankName(rs.getString("rank_name"));
            p.setChatColor(getOptionalString(rs, "chat_color"));
            p.setNameColor(getOptionalString(rs, "name_color"));
            p.setTag(getOptionalString(rs, "tag"));
            p.setBuildMode(getOptionalBoolean(rs, "build_mode"));
            if (persist("vanish")) p.setVanished(getOptionalBoolean(rs, "vanished"));
            if (persist("god")) p.setGodMode(getOptionalBoolean(rs, "god_mode"));
            if (persist("social-spy")) p.setSocialSpy(getOptionalBoolean(rs, "social_spy"));
            if (persist("msg-toggle")) p.setMsgToggled(getOptionalBoolean(rs, "msg_toggled", true));
            if (persist("staff-mode")) p.setStaffMode(getOptionalBoolean(rs, "staff_mode"));
            if (persist("disguise")) {
                p.setDisguised(getOptionalBoolean(rs, "disguised"));
                p.setDisguiseName(getOptionalString(rs, "disguise_name"));
                p.setDisguiseSkin(getOptionalString(rs, "disguise_skin"));
                p.setDisguiseRank(getOptionalString(rs, "disguise_rank"));
            }
            p.setFirstJoin(rs.getLong("first_join"));
            p.setLastSeen(rs.getLong("last_seen"));
            String extras = rs.getString("extra_ranks");
            if (extras != null && !extras.isEmpty()) Arrays.stream(extras.split(",")).forEach(p::addExtraRank);
            String perms = rs.getString("permissions");
            if (perms != null && !perms.isEmpty()) Arrays.stream(perms.split(",")).forEach(p::addPermission);
            return p;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load profile: " + e.getMessage());
            return new PlayerProfile(uuid, name);
        }
    }

    @Override
    public void saveProfile(PlayerProfile p) {
        try (Connection con = getConn();
             PreparedStatement ps = con.prepareStatement(
                "REPLACE INTO evaulx_players (uuid,name,ip,rank_name,first_join,last_seen,extra_ranks,permissions,chat_color,name_color,tag,build_mode,vanished,god_mode,social_spy,msg_toggled,staff_mode,disguised,disguise_name,disguise_skin,disguise_rank) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, p.getUuid().toString());
            ps.setString(2, p.getName());
            ps.setString(3, p.getIp());
            ps.setString(4, p.getRankName());
            ps.setLong(5, p.getFirstJoin());
            ps.setLong(6, p.getLastSeen());
            ps.setString(7, String.join(",", p.getExtraRanks()));
            ps.setString(8, String.join(",", p.getPermissions()));
            ps.setString(9, p.getChatColor());
            ps.setString(10, p.getNameColor());
            ps.setString(11, p.getTag());
            ps.setBoolean(12, p.isBuildMode());
            ps.setBoolean(13, persist("vanish") && p.isVanished());
            ps.setBoolean(14, persist("god") && p.isGodMode());
            ps.setBoolean(15, persist("social-spy") && p.isSocialSpy());
            ps.setBoolean(16, !persist("msg-toggle") || p.isMsgToggled());
            ps.setBoolean(17, persist("staff-mode") && p.isStaffMode());
            ps.setBoolean(18, persist("disguise") && p.isDisguised());
            ps.setString(19, persist("disguise") ? p.getDisguiseName() : null);
            ps.setString(20, persist("disguise") ? p.getDisguiseSkin() : null);
            ps.setString(21, persist("disguise") ? p.getDisguiseRank() : null);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save profile: " + e.getMessage());
        }
    }

    @Override
    public int cleanupProfiles(Set<String> validRankNames, String defaultRankName) {
        Set<String> valid = normalizeRankSet(validRankNames);
        int changedProfiles = 0;

        try (Connection con = getConn();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT uuid, rank_name, extra_ranks FROM evaulx_players")) {
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String rank = rs.getString("rank_name");
                String extras = rs.getString("extra_ranks");
                String cleanedRank = rank;
                String cleanedExtras = cleanRankCsv(extras, valid);

                boolean changed = false;
                if (rank == null || !valid.contains(rank.toLowerCase(Locale.ENGLISH))) {
                    cleanedRank = defaultRankName;
                    changed = true;
                }
                if (!Objects.equals(extras == null ? "" : extras, cleanedExtras)) changed = true;
                if (!changed) continue;

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE evaulx_players SET rank_name=?, extra_ranks=? WHERE uuid=?")) {
                    ps.setString(1, cleanedRank);
                    ps.setString(2, cleanedExtras);
                    ps.setString(3, uuid);
                    ps.executeUpdate();
                    changedProfiles++;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to clean MySQL profiles: " + e.getMessage());
        }
        return changedProfiles;
    }

    private String getOptionalString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException ignored) {
            return "";
        }
    }

    private boolean getOptionalBoolean(ResultSet rs, String column) {
        return getOptionalBoolean(rs, column, false);
    }

    private boolean getOptionalBoolean(ResultSet rs, String column, boolean fallback) {
        try {
            return rs.getBoolean(column);
        } catch (SQLException ignored) {
            return fallback;
        }
    }

    private boolean persist(String key) {
        return plugin.getConfig().getBoolean("profile-persistence." + key, false);
    }

    // ---- Punishments ----

    @Override
    public void savePunishment(Punishment pun) {
        try (Connection con = getConn();
             PreparedStatement ps = con.prepareStatement(
                "INSERT INTO evaulx_punishments (id,target,target_name,target_ip,punisher,punisher_name,type,reason,issued,expires,active,silent,removed_by,removed_reason,removed_at,evidence_url,appeal_status,staff_note,internal_note) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            setPunishmentParams(ps, pun);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save punishment: " + e.getMessage());
        }
    }

    @Override
    public void updatePunishment(Punishment pun) {
        try (Connection con = getConn();
             PreparedStatement ps = con.prepareStatement(
                "REPLACE INTO evaulx_punishments (id,target,target_name,target_ip,punisher,punisher_name,type,reason,issued,expires,active,silent,removed_by,removed_reason,removed_at,evidence_url,appeal_status,staff_note,internal_note) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            setPunishmentParams(ps, pun);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update punishment: " + e.getMessage());
        }
    }

    private void setPunishmentParams(PreparedStatement ps, Punishment p) throws SQLException {
        ps.setString(1, p.getId());
        ps.setString(2, p.getTarget().toString());
        ps.setString(3, p.getTargetName());
        ps.setString(4, p.getTargetIp());
        ps.setString(5, p.getPunisher() != null ? p.getPunisher().toString() : "CONSOLE");
        ps.setString(6, p.getPunisherName());
        ps.setString(7, p.getType().name());
        ps.setString(8, p.getReason());
        ps.setLong(9, p.getIssued());
        ps.setLong(10, p.getExpires());
        ps.setBoolean(11, p.isActive());
        ps.setBoolean(12, p.isSilent());
        ps.setString(13, p.getRemovedBy() != null ? p.getRemovedBy().toString() : null);
        ps.setString(14, p.getRemovedReason());
        ps.setLong(15, p.getRemovedAt());
        ps.setString(16, p.getEvidenceUrl());
        ps.setString(17, p.getAppealStatus());
        ps.setString(18, p.getStaffNote());
        ps.setString(19, p.getInternalNote());
    }

    @Override
    public List<Punishment> getPunishments(UUID uuid) {
        List<Punishment> list = new ArrayList<>();
        try (Connection con = getConn();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM evaulx_punishments WHERE target=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rsToPunishment(rs));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get punishments: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Punishment> getPunishmentsByIp(String ip) {
        List<Punishment> list = new ArrayList<>();
        try (Connection con = getConn();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM evaulx_punishments WHERE target_ip=?")) {
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rsToPunishment(rs));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get punishments by IP: " + e.getMessage());
        }
        return list;
    }

    @Override
    public Punishment getActivePunishment(UUID uuid, Punishment.Type type) {
        try (Connection con = getConn();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM evaulx_punishments WHERE target=? AND type=? AND active=1 LIMIT 1")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.name());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rsToPunishment(rs);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get active punishment: " + e.getMessage());
        }
        return null;
    }

    private Punishment rsToPunishment(ResultSet rs) throws SQLException {
        UUID target = UUID.fromString(rs.getString("target"));
        String targetName = rs.getString("target_name");
        String punisherStr = rs.getString("punisher");
        UUID punisher = (punisherStr == null || punisherStr.equals("CONSOLE")) ? null : UUID.fromString(punisherStr);
        Punishment.Type type = Punishment.Type.valueOf(rs.getString("type"));
        String reason = rs.getString("reason");
        long expires = rs.getLong("expires");
        boolean silent = rs.getBoolean("silent");
        Punishment p = new Punishment(target, targetName, punisher, rs.getString("punisher_name"), type, reason, expires, silent);
        p.setId(rs.getString("id"));
        p.setIssued(rs.getLong("issued"));
        p.setActive(rs.getBoolean("active"));
        p.setTargetIp(rs.getString("target_ip"));
        p.setEvidenceUrl(getOptionalString(rs, "evidence_url"));
        p.setAppealStatus(getOptionalString(rs, "appeal_status"));
        p.setStaffNote(getOptionalString(rs, "staff_note"));
        p.setInternalNote(getOptionalString(rs, "internal_note"));
        String removedBy = rs.getString("removed_by");
        if (removedBy != null) p.setRemovedBy(UUID.fromString(removedBy));
        p.setRemovedReason(rs.getString("removed_reason"));
        p.setRemovedAt(rs.getLong("removed_at"));
        return p;
    }

    // ---- Ranks ----

    @Override
    public void saveRank(Rank rank) {
        try (Connection con = getConn();
             PreparedStatement ps = con.prepareStatement(
                "REPLACE INTO evaulx_ranks (name,display,category,rank_permission,prefix,suffix,color,weight,is_default,is_staff,is_hidden,permissions,inheritance) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, rank.getName());
            ps.setString(2, rank.getDisplay());
            ps.setString(3, rank.getCategory());
            ps.setString(4, rank.getPermission());
            ps.setString(5, rank.getPrefix());
            ps.setString(6, rank.getSuffix());
            ps.setString(7, rank.getColor());
            ps.setInt(8, rank.getWeight());
            ps.setBoolean(9, rank.isDefault());
            ps.setBoolean(10, rank.isStaff());
            ps.setBoolean(11, rank.isHidden());
            ps.setString(12, String.join(",", rank.getPermissions()));
            ps.setString(13, String.join(",", rank.getInheritance()));
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save rank: " + e.getMessage());
        }
    }

    @Override
    public void deleteRank(String name) {
        try (Connection con = getConn();
             PreparedStatement ps = con.prepareStatement("DELETE FROM evaulx_ranks WHERE name=?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to delete rank: " + e.getMessage());
        }
    }

    @Override
    public List<Rank> loadRanks() {
        List<Rank> list = new ArrayList<>();
        try (Connection con = getConn();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM evaulx_ranks")) {
            while (rs.next()) {
                Rank rank = new Rank(rs.getString("name"));
                String category = getOptionalString(rs, "category");
                rank.setDisplay(getOptionalString(rs, "display"));
                rank.setPermission(getOptionalString(rs, "rank_permission"));
                rank.setPrefix(rs.getString("prefix"));
                rank.setSuffix(rs.getString("suffix"));
                rank.setColor(rs.getString("color"));
                rank.setWeight(rs.getInt("weight"));
                rank.setDefault(rs.getBoolean("is_default"));
                rank.setStaff(rs.getBoolean("is_staff"));
                rank.setHidden(rs.getBoolean("is_hidden"));
                rank.setCategory(category == null || category.isEmpty() ? Rank.inferCategory(rank) : category);
                String perms = rs.getString("permissions");
                if (perms != null && !perms.isEmpty()) Arrays.stream(perms.split(",")).forEach(rank::addPermission);
                String inh = rs.getString("inheritance");
                if (inh != null && !inh.isEmpty()) Arrays.stream(inh.split(",")).forEach(rank::addInheritance);
                list.add(rank);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load ranks: " + e.getMessage());
        }
        return list;
    }

    private Set<String> normalizeRankSet(Set<String> rankNames) {
        Set<String> normalized = new HashSet<>();
        if (rankNames == null) return normalized;
        for (String rankName : rankNames) {
            if (rankName != null) normalized.add(rankName.toLowerCase(Locale.ENGLISH));
        }
        return normalized;
    }

    private String cleanRankCsv(String value, Set<String> valid) {
        if (value == null || value.trim().isEmpty()) return "";
        List<String> cleaned = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String raw : value.split(",")) {
            String rank = raw.trim();
            if (rank.isEmpty()) continue;
            String key = rank.toLowerCase(Locale.ENGLISH);
            if (!valid.contains(key) || !seen.add(key)) continue;
            cleaned.add(rank);
        }
        return String.join(",", cleaned);
    }
}
