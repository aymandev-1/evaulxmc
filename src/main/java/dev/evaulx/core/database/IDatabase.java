package dev.evaulx.core.database;

import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.models.Rank;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IDatabase {
    boolean connect();
    void disconnect();

    PlayerProfile loadProfile(UUID uuid, String name);
    void saveProfile(PlayerProfile profile);
    int cleanupProfiles(Set<String> validRankNames, String defaultRankName);

    void savePunishment(Punishment punishment);
    void updatePunishment(Punishment punishment);
    List<Punishment> getPunishments(UUID uuid);
    List<Punishment> getPunishmentsByIp(String ip);
    Punishment getActivePunishment(UUID uuid, Punishment.Type type);

    void saveRank(Rank rank);
    void deleteRank(String name);
    List<Rank> loadRanks();
}
