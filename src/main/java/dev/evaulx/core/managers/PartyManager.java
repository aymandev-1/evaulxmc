package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {

    public static final class Party {
        private UUID leader;
        private final Set<UUID> members;
        private final Set<UUID> pending;
        private boolean pvpEnabled;
        private boolean openJoin;

        public Party(UUID leader) {
            this.leader = leader;
            this.members = new HashSet<>();
            this.members.add(leader);
            this.pending = new HashSet<>();
            this.pvpEnabled = false;
            this.openJoin = false;
        }

        public UUID getLeader() { return leader; }
        public void setLeader(UUID leader) { this.leader = leader; }
        public Set<UUID> getMembers() { return members; }
        public Set<UUID> getPending() { return pending; }
        public boolean isPvpEnabled() { return pvpEnabled; }
        public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
        public boolean isOpenJoin() { return openJoin; }
        public void setOpenJoin(boolean openJoin) { this.openJoin = openJoin; }
        public int size() { return members.size(); }
    }

    private final EvaulxCore plugin;
    private final Map<UUID, Party> memberIndex = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> inviteExpiry = new ConcurrentHashMap<>();
    private final Set<UUID> partyChatMode = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    public PartyManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public boolean isInParty(UUID uuid) {
        return memberIndex.containsKey(uuid);
    }

    public Party getParty(UUID uuid) {
        return memberIndex.get(uuid);
    }

    public boolean isLeader(UUID uuid) {
        Party party = getParty(uuid);
        return party != null && party.getLeader().equals(uuid);
    }

    public boolean isPartyChatMode(UUID uuid) {
        return partyChatMode.contains(uuid);
    }

    public void togglePartyChatMode(UUID uuid) {
        if (partyChatMode.contains(uuid)) {
            partyChatMode.remove(uuid);
        } else {
            partyChatMode.add(uuid);
        }
    }

    public Party createParty(Player leader) {
        Party party = new Party(leader.getUniqueId());
        memberIndex.put(leader.getUniqueId(), party);
        return party;
    }

    public void disbandParty(UUID leaderUUID) {
        Party party = memberIndex.get(leaderUUID);
        if (party == null) return;
        List<UUID> toRemove = new ArrayList<>(party.getMembers());
        for (UUID member : toRemove) {
            memberIndex.remove(member);
            partyChatMode.remove(member);
            Player p = Bukkit.getPlayer(member);
            if (p != null && !member.equals(leaderUUID)) {
                p.sendMessage(CC.color(CC.SEPARATOR));
                p.sendMessage(CC.color("  &cThe party has been disbanded."));
                p.sendMessage(CC.color(CC.SEPARATOR));
            }
        }
        party.getMembers().clear();
        party.getPending().clear();
    }

    public void invitePlayer(UUID inviterUUID, UUID targetUUID) {
        Party party = memberIndex.get(inviterUUID);
        if (party == null) return;
        party.getPending().add(targetUUID);
        BukkitTask existing = inviteExpiry.get(targetUUID);
        if (existing != null) existing.cancel();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            public void run() {
                party.getPending().remove(targetUUID);
                inviteExpiry.remove(targetUUID);
                Player target = Bukkit.getPlayer(targetUUID);
                if (target != null) {
                    target.sendMessage(CC.color("&7Party invite expired."));
                }
                Player inviter = Bukkit.getPlayer(inviterUUID);
                if (inviter != null) {
                    inviter.sendMessage(CC.color("&7Your party invite to &f" +
                        (target != null ? target.getName() : "player") + " &7expired."));
                }
            }
        }, 600L); // 30 seconds
        inviteExpiry.put(targetUUID, task);
    }

    public boolean hasPendingInvite(UUID targetUUID, UUID fromLeader) {
        Party party = memberIndex.get(fromLeader);
        return party != null && party.getPending().contains(targetUUID);
    }

    public Party getPartyWithPendingInvite(UUID targetUUID) {
        for (Party party : memberIndex.values()) {
            if (party.getPending().contains(targetUUID)) return party;
        }
        return null;
    }

    public boolean acceptInvite(UUID targetUUID) {
        Party party = getPartyWithPendingInvite(targetUUID);
        if (party == null) return false;
        party.getPending().remove(targetUUID);
        BukkitTask task = inviteExpiry.remove(targetUUID);
        if (task != null) task.cancel();
        party.getMembers().add(targetUUID);
        memberIndex.put(targetUUID, party);
        return true;
    }

    public boolean denyInvite(UUID targetUUID) {
        Party party = getPartyWithPendingInvite(targetUUID);
        if (party == null) return false;
        party.getPending().remove(targetUUID);
        BukkitTask task = inviteExpiry.remove(targetUUID);
        if (task != null) task.cancel();
        return true;
    }

    public void leaveParty(UUID uuid) {
        Party party = memberIndex.remove(uuid);
        if (party == null) return;
        partyChatMode.remove(uuid);
        party.getMembers().remove(uuid);
        if (party.getMembers().isEmpty()) return;
        if (party.getLeader().equals(uuid)) {
            UUID newLeader = party.getMembers().iterator().next();
            party.setLeader(newLeader);
            Player newLeaderPlayer = Bukkit.getPlayer(newLeader);
            broadcastToParty(party, "&e" + (newLeaderPlayer != null ? newLeaderPlayer.getName() : "A player") +
                    " &7is now the party leader.");
        }
        Player left = Bukkit.getPlayer(uuid);
        broadcastToParty(party, "&f" + (left != null ? left.getName() : uuid.toString().substring(0, 8)) +
                " &7has left the party.");
    }

    public boolean kickFromParty(UUID leaderUUID, UUID targetUUID) {
        Party party = memberIndex.get(leaderUUID);
        if (party == null || !party.getLeader().equals(leaderUUID)) return false;
        if (!party.getMembers().contains(targetUUID)) return false;
        party.getMembers().remove(targetUUID);
        memberIndex.remove(targetUUID);
        partyChatMode.remove(targetUUID);
        Player kicked = Bukkit.getPlayer(targetUUID);
        if (kicked != null) {
            kicked.sendMessage(CC.color(CC.SEPARATOR));
            kicked.sendMessage(CC.color("  &cYou have been kicked from the party."));
            kicked.sendMessage(CC.color(CC.SEPARATOR));
        }
        return true;
    }

    public boolean promoteLeader(UUID leaderUUID, UUID newLeaderUUID) {
        Party party = memberIndex.get(leaderUUID);
        if (party == null || !party.getLeader().equals(leaderUUID)) return false;
        if (!party.getMembers().contains(newLeaderUUID)) return false;
        party.setLeader(newLeaderUUID);
        return true;
    }

    public void warpParty(UUID leaderUUID) {
        Party party = memberIndex.get(leaderUUID);
        if (party == null) return;
        Player leader = Bukkit.getPlayer(leaderUUID);
        if (leader == null) return;
        broadcastToParty(party, "&aParty warp initiated! Teleporting all members...");
        for (UUID member : party.getMembers()) {
            if (member.equals(leaderUUID)) continue;
            Player p = Bukkit.getPlayer(member);
            if (p != null) {
                p.teleport(leader.getLocation());
            }
        }
    }

    public void broadcastToParty(Party party, String message) {
        String formatted = CC.color("  &8[&6Party&8] &7" + message);
        for (UUID member : party.getMembers()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null) {
                p.sendMessage(formatted);
            }
        }
    }

    public void broadcastChatToParty(Party party, Player sender, String message) {
        String formatted = CC.color("  &8[&6Party&8] &f" + sender.getName() + " &8» &7" + message);
        for (UUID member : party.getMembers()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null) {
                p.sendMessage(formatted);
            }
        }
    }

    public void cleanupPlayer(UUID uuid) {
        partyChatMode.remove(uuid);
        BukkitTask task = inviteExpiry.remove(uuid);
        if (task != null) task.cancel();
    }
}
