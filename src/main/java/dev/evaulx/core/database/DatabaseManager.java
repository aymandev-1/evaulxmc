package dev.evaulx.core.database;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.database.flatfile.FlatFileDatabase;
import dev.evaulx.core.database.mongo.MongoDatabase;
import dev.evaulx.core.database.mysql.MySQLDatabase;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.models.Rank;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DatabaseManager {

    public enum DBType { MONGODB, MYSQL, FLATFILE }

    private final EvaulxCore plugin;
    private IDatabase database;
    private DBType type;

    public DatabaseManager(EvaulxCore plugin) {
        this.plugin = plugin;
        String raw = plugin.getConfig().getString("database.type", "FLATFILE").toUpperCase();
        try {
            this.type = DBType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown database type '" + raw + "', defaulting to FLATFILE.");
            this.type = DBType.FLATFILE;
        }
    }

    public boolean connect() {
        switch (type) {
            case MONGODB:
                database = new MongoDatabase(plugin);
                break;
            case MYSQL:
                database = new MySQLDatabase(plugin);
                break;
            default:
                database = new FlatFileDatabase(plugin);
                break;
        }
        boolean ok = database.connect();
        if (ok) plugin.getLogger().info("Connected to database: " + type.name());
        return ok;
    }

    public void disconnect() {
        if (database != null) database.disconnect();
    }

    public DBType getType() { return type; }
    public IDatabase getDatabase() { return database; }

    // Delegate methods
    public PlayerProfile loadProfile(UUID uuid, String name) { return database.loadProfile(uuid, name); }
    public void saveProfile(PlayerProfile profile) { database.saveProfile(profile); }
    public int cleanupProfiles(Set<String> validRankNames, String defaultRankName) {
        return database.cleanupProfiles(validRankNames, defaultRankName);
    }

    public void savePunishment(Punishment punishment) { database.savePunishment(punishment); }
    public void updatePunishment(Punishment punishment) { database.updatePunishment(punishment); }
    public List<Punishment> getPunishments(UUID uuid) { return database.getPunishments(uuid); }
    public List<Punishment> getPunishmentsByIp(String ip) { return database.getPunishmentsByIp(ip); }
    public Punishment getActivePunishment(UUID uuid, Punishment.Type type) { return database.getActivePunishment(uuid, type); }

    public void saveRank(Rank rank) { database.saveRank(rank); }
    public void deleteRank(String name) { database.deleteRank(name); }
    public List<Rank> loadRanks() { return database.loadRanks(); }
}
