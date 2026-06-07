package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final EvaulxCore plugin;
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

    public PlayerManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public PlayerProfile loadProfile(Player player) {
        PlayerProfile profile = plugin.getDatabaseManager().loadProfile(player.getUniqueId(), player.getName());
        normalizeRanks(profile);
        profiles.put(player.getUniqueId(), profile);
        return profile;
    }

    public void unloadProfile(UUID uuid) {
        PlayerProfile profile = profiles.remove(uuid);
        if (profile != null) {
            profile.setLastSeen(System.currentTimeMillis());
            plugin.getDatabaseManager().saveProfile(profile);
        }
        PermissionAttachment att = attachments.remove(uuid);
        if (att != null) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) try { p.removeAttachment(att); } catch (Exception ignored) {}
        }
    }

    public void saveProfile(PlayerProfile profile) {
        TaskUtil.async(() -> plugin.getDatabaseManager().saveProfile(profile));
    }

    public void saveAll() {
        for (PlayerProfile p : profiles.values()) {
            p.setLastSeen(System.currentTimeMillis());
            plugin.getDatabaseManager().saveProfile(p);
        }
    }

    public PlayerProfile getProfile(UUID uuid) { return profiles.get(uuid); }
    public PlayerProfile getProfile(Player player) { return profiles.get(player.getUniqueId()); }
    public boolean isLoaded(UUID uuid) { return profiles.containsKey(uuid); }

    public void applyPermissions(Player player, PlayerProfile profile) {
        // Remove old attachment
        PermissionAttachment old = attachments.get(player.getUniqueId());
        if (old != null) try { player.removeAttachment(old); } catch (Exception ignored) {}

        PermissionAttachment att = player.addAttachment(plugin);
        attachments.put(player.getUniqueId(), att);

        // Apply rank permissions
        Rank rank = plugin.getRankManager().getRank(profile.getRankName());
        if (rank != null) {
            List<String> perms = plugin.getRankManager().getAllPermissions(rank);
            for (String perm : perms) {
                if (perm.startsWith("-")) att.setPermission(perm.substring(1), false);
                else att.setPermission(perm, true);
            }
        }

        // Apply extra rank permissions
        for (String extraRankName : profile.getExtraRanks()) {
            Rank extra = plugin.getRankManager().getRank(extraRankName);
            if (extra != null) {
                List<String> perms = plugin.getRankManager().getAllPermissions(extra);
                for (String perm : perms) {
                    if (perm.startsWith("-")) att.setPermission(perm.substring(1), false);
                    else att.setPermission(perm, true);
                }
            }
        }

        // Apply player-specific permissions
        for (String perm : profile.getPermissions()) {
            if (perm.startsWith("-")) att.setPermission(perm.substring(1), false);
            else att.setPermission(perm, true);
        }

        player.recalculatePermissions();
    }

    public Collection<PlayerProfile> getOnlineProfiles() { return profiles.values(); }

    private void normalizeRanks(PlayerProfile profile) {
        boolean changed = false;
        Rank defaultRank = plugin.getRankManager().getDefaultRank();
        if (plugin.getRankManager().getRank(profile.getRankName()) == null && defaultRank != null) {
            profile.setRankName(defaultRank.getName());
            changed = true;
        }

        Iterator<String> iterator = profile.getExtraRanks().iterator();
        while (iterator.hasNext()) {
            String rankName = iterator.next();
            if (plugin.getRankManager().getRank(rankName) != null) continue;
            iterator.remove();
            changed = true;
        }

        if (changed) plugin.getDatabaseManager().saveProfile(profile);
    }
}
