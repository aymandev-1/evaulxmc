package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.managers.PartyManager;
import dev.evaulx.core.managers.PartyManager.Party;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PartyCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    private static final List<String> SUBS = Arrays.asList(
            "create", "invite", "accept", "deny", "kick", "leave",
            "disband", "promote", "info", "list", "chat", "warp",
            "pvp", "open", "help"
    );

    public PartyCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("evaulx.party")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length == 0) {
            return showHelp(player);
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":   return handleCreate(player);
            case "invite":   return handleInvite(player, args);
            case "accept":   return handleAccept(player);
            case "deny":     return handleDeny(player);
            case "kick":     return handleKick(player, args);
            case "leave":    return handleLeave(player);
            case "disband":  return handleDisband(player);
            case "promote":
            case "transfer": return handlePromote(player, args);
            case "info":
            case "list":     return handleInfo(player);
            case "chat":     return handleChat(player, args);
            case "warp":     return handleWarp(player);
            case "pvp":      return handlePvp(player);
            case "open":     return handleOpen(player);
            default:         return showHelp(player);
        }
    }

    private boolean handleCreate(Player player) {
        PartyManager pm = plugin.getPartyManager();
        if (pm.isInParty(player.getUniqueId())) {
            player.sendMessage(CC.color("&cYou are already in a party. Leave it first with &f/party leave&c."));
            return true;
        }
        Party party = pm.createParty(player);
        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &aParty created!"));
        player.sendMessage(CC.color("  &7Invite players with &f/party invite <player>&7."));
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleInvite(Player player, String[] args) {
        PartyManager pm = plugin.getPartyManager();
        if (!pm.isInParty(player.getUniqueId())) {
            player.sendMessage(CC.color("&cYou are not in a party. Create one with &f/party create&c."));
            return true;
        }
        if (!pm.isLeader(player.getUniqueId())) {
            player.sendMessage(CC.color("&cOnly the party leader can invite players."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /party invite <player>"));
            return true;
        }
        Party party = pm.getParty(player.getUniqueId());
        if (party.size() >= 10) {
            player.sendMessage(CC.color("&cYour party is full &8(max 10 members)&c."));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || target.equals(player)) {
            player.sendMessage(CC.color("&cPlayer not found or online."));
            return true;
        }
        if (pm.isInParty(target.getUniqueId())) {
            player.sendMessage(CC.color("&f" + target.getName() + " &7is already in a party."));
            return true;
        }
        if (party.getPending().contains(target.getUniqueId())) {
            player.sendMessage(CC.color("&f" + target.getName() + " &7has already been invited."));
            return true;
        }
        pm.invitePlayer(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(CC.color("&aInvite sent to &f" + target.getName() + "&a."));
        target.sendMessage(CC.color(CC.SEPARATOR));
        target.sendMessage(CC.color("  &f" + player.getName() + " &7has invited you to their party!"));
        target.sendMessage(CC.color("  &aType &f/party accept &aor &c/party deny&7. Expires in &f30s&7."));
        target.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean handleAccept(Player player) {
        PartyManager pm = plugin.getPartyManager();
        if (pm.isInParty(player.getUniqueId())) {
            player.sendMessage(CC.color("&cYou are already in a party."));
            return true;
        }
        Party party = pm.getPartyWithPendingInvite(player.getUniqueId());
        if (party == null) {
            player.sendMessage(CC.color("&cYou have no pending party invite."));
            return true;
        }
        UUID leaderUUID = party.getLeader();
        Player leader = Bukkit.getPlayer(leaderUUID);
        String leaderName = leader != null ? leader.getName() : "Unknown";
        pm.acceptInvite(player.getUniqueId());
        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &aYou joined &f" + leaderName + "&a's party!"));
        player.sendMessage(CC.color("  &7Members: &f" + party.size()));
        player.sendMessage(CC.color(CC.SEPARATOR));
        pm.broadcastToParty(party, "&f" + player.getName() + " &7has joined the party!");
        return true;
    }

    private boolean handleDeny(Player player) {
        PartyManager pm = plugin.getPartyManager();
        Party party = pm.getPartyWithPendingInvite(player.getUniqueId());
        if (party == null) {
            player.sendMessage(CC.color("&cYou have no pending party invite."));
            return true;
        }
        UUID leaderUUID = party.getLeader();
        Player leader = Bukkit.getPlayer(leaderUUID);
        pm.denyInvite(player.getUniqueId());
        player.sendMessage(CC.color("&7Party invite declined."));
        if (leader != null) {
            leader.sendMessage(CC.color("&f" + player.getName() + " &7declined the party invite."));
        }
        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        PartyManager pm = plugin.getPartyManager();
        if (!pm.isLeader(player.getUniqueId())) {
            player.sendMessage(CC.color("&cOnly the party leader can kick members."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /party kick <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || target.equals(player)) {
            player.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }
        boolean kicked = pm.kickFromParty(player.getUniqueId(), target.getUniqueId());
        if (!kicked) {
            player.sendMessage(CC.color("&f" + target.getName() + " &7is not in your party."));
            return true;
        }
        player.sendMessage(CC.color("&7Kicked &f" + target.getName() + " &7from the party."));
        Party party = pm.getParty(player.getUniqueId());
        if (party != null) {
            pm.broadcastToParty(party, "&f" + target.getName() + " &7was kicked from the party.");
        }
        return true;
    }

    private boolean handleLeave(Player player) {
        PartyManager pm = plugin.getPartyManager();
        if (!pm.isInParty(player.getUniqueId())) {
            player.sendMessage(CC.color("&cYou are not in a party."));
            return true;
        }
        if (pm.isLeader(player.getUniqueId())) {
            Party party = pm.getParty(player.getUniqueId());
            if (party != null && party.size() > 1) {
                player.sendMessage(CC.color("&cYou are the leader. Transfer leadership first with &f/party promote <player> &cor use &f/party disband&c."));
                return true;
            }
        }
        pm.leaveParty(player.getUniqueId());
        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &7You left the party."));
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean handleDisband(Player player) {
        PartyManager pm = plugin.getPartyManager();
        if (!pm.isLeader(player.getUniqueId())) {
            player.sendMessage(CC.color("&cOnly the party leader can disband the party."));
            return true;
        }
        Party party = pm.getParty(player.getUniqueId());
        if (party == null) { player.sendMessage(CC.color("&cYou are not in a party.")); return true; }
        pm.broadcastToParty(party, "&cThe party has been disbanded by &f" + player.getName() + "&c.");
        pm.disbandParty(player.getUniqueId());
        player.sendMessage(CC.color("&7Party disbanded."));
        return true;
    }

    private boolean handlePromote(Player player, String[] args) {
        PartyManager pm = plugin.getPartyManager();
        if (!pm.isLeader(player.getUniqueId())) {
            player.sendMessage(CC.color("&cOnly the party leader can promote members."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /party promote <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || target.equals(player)) {
            player.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }
        boolean promoted = pm.promoteLeader(player.getUniqueId(), target.getUniqueId());
        if (!promoted) {
            player.sendMessage(CC.color("&f" + target.getName() + " &7is not in your party."));
            return true;
        }
        Party party = pm.getParty(target.getUniqueId());
        if (party != null) {
            pm.broadcastToParty(party, "&f" + target.getName() + " &7is now the party leader!");
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleInfo(Player player) {
        PartyManager pm = plugin.getPartyManager();
        if (!pm.isInParty(player.getUniqueId())) {
            player.sendMessage(CC.color("&cYou are not in a party."));
            return true;
        }
        Party party = pm.getParty(player.getUniqueId());
        Player leader = Bukkit.getPlayer(party.getLeader());
        String leaderName = leader != null ? leader.getName() : Bukkit.getOfflinePlayer(party.getLeader()).getName();
        if (leaderName == null) leaderName = "Unknown";

        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &6Party Info"));
        player.sendMessage(CC.color("  &7Leader: &f" + leaderName));
        player.sendMessage(CC.color("  &7Members &8(&f" + party.size() + "/10&8)&7:"));
        for (UUID member : party.getMembers()) {
            Player p = Bukkit.getPlayer(member);
            String role = party.getLeader().equals(member) ? " &6[Leader]" : "";
            String status = p != null ? "&a● " : "&8○ ";
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(member).getName();
            if (name == null) name = member.toString().substring(0, 8);
            player.sendMessage(CC.color("  " + status + "&f" + name + role));
        }
        player.sendMessage(CC.color("  &7PvP: " + (party.isPvpEnabled() ? "&aEnabled" : "&cDisabled") +
                "  &7Open: " + (party.isOpenJoin() ? "&aYes" : "&cNo")));
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean handleChat(Player player, String[] args) {
        PartyManager pm = plugin.getPartyManager();
        if (!pm.isInParty(player.getUniqueId())) {
            player.sendMessage(CC.color("&cYou are not in a party."));
            return true;
        }
        if (args.length == 1) {
            pm.togglePartyChatMode(player.getUniqueId());
            boolean enabled = pm.isPartyChatMode(player.getUniqueId());
            player.sendMessage(CC.color("&7Party chat &f" + (enabled ? "&aTOGGLED ON" : "&cTOGGLED OFF") +
                    "&7. " + (enabled ? "All messages go to party." : "Back to global chat.")));
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) sb.append(" ");
            sb.append(args[i]);
        }
        Party party = pm.getParty(player.getUniqueId());
        if (party != null) {
            pm.broadcastChatToParty(party, player, sb.toString());
        }
        return true;
    }

    private boolean handleWarp(Player player) {
        PartyManager pm = plugin.getPartyManager();
        if (!pm.isLeader(player.getUniqueId())) {
            player.sendMessage(CC.color("&cOnly the party leader can warp the party."));
            return true;
        }
        pm.warpParty(player.getUniqueId());
        return true;
    }

    private boolean handlePvp(Player player) {
        PartyManager pm = plugin.getPartyManager();
        if (!pm.isLeader(player.getUniqueId())) {
            player.sendMessage(CC.color("&cOnly the party leader can change PvP settings."));
            return true;
        }
        Party party = pm.getParty(player.getUniqueId());
        if (party == null) { player.sendMessage(CC.color("&cYou are not in a party.")); return true; }
        party.setPvpEnabled(!party.isPvpEnabled());
        pm.broadcastToParty(party, "&7Party PvP (friendly fire) is now " +
                (party.isPvpEnabled() ? "&aENABLED" : "&cDISABLED") + "&7.");
        return true;
    }

    private boolean handleOpen(Player player) {
        PartyManager pm = plugin.getPartyManager();
        if (!pm.isLeader(player.getUniqueId())) {
            player.sendMessage(CC.color("&cOnly the party leader can change party settings."));
            return true;
        }
        Party party = pm.getParty(player.getUniqueId());
        if (party == null) { player.sendMessage(CC.color("&cYou are not in a party.")); return true; }
        party.setOpenJoin(!party.isOpenJoin());
        pm.broadcastToParty(party, "&7Open join is now " +
                (party.isOpenJoin() ? "&aENABLED &7— anyone can use /party join" : "&cDISABLED") + "&7.");
        return true;
    }

    private boolean showHelp(Player player) {
        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &6Party Commands"));
        player.sendMessage(CC.color("  &f/party create &8— &7Create a new party"));
        player.sendMessage(CC.color("  &f/party invite <player> &8— &7Invite someone"));
        player.sendMessage(CC.color("  &f/party accept &8— &7Accept an invite"));
        player.sendMessage(CC.color("  &f/party deny &8— &7Deny an invite"));
        player.sendMessage(CC.color("  &f/party kick <player> &8— &7Kick a member (leader)"));
        player.sendMessage(CC.color("  &f/party promote <player> &8— &7Transfer leadership"));
        player.sendMessage(CC.color("  &f/party leave &8— &7Leave the party"));
        player.sendMessage(CC.color("  &f/party disband &8— &7Disband party (leader)"));
        player.sendMessage(CC.color("  &f/party warp &8— &7Warp all members to you (leader)"));
        player.sendMessage(CC.color("  &f/party chat [msg] &8— &7Toggle or send party chat"));
        player.sendMessage(CC.color("  &f/party pvp &8— &7Toggle friendly fire (leader)"));
        player.sendMessage(CC.color("  &f/party info &8— &7Show party info"));
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("evaulx.party")) {
            return Collections.emptyList();
        }
        Player player = (Player) sender;
        PartyManager pm = plugin.getPartyManager();
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("invite") || sub.equals("kick") || sub.equals("promote") || sub.equals("transfer")) {
                return Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.equals(player))
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
