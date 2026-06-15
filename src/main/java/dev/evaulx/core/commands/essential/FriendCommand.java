package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.managers.FriendManager;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class FriendCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    private static final List<String> SUBS = Arrays.asList(
            "add", "remove", "accept", "deny", "list", "online",
            "message", "msg", "favorite", "fav",
            "block", "unblock", "blocked", "notifications", "help"
    );

    public FriendCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("evaulx.friends")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) return handleList(player);

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "add":
            case "request":     return handleAdd(player, args);
            case "remove":
            case "unfriend":    return handleRemove(player, args);
            case "accept":      return handleAccept(player);
            case "deny":
            case "decline":     return handleDeny(player);
            case "online":      return handleOnline(player);
            case "message":
            case "msg":         return handleMessage(player, args);
            case "favorite":
            case "fav":         return handleFavorite(player, args);
            case "block":       return handleBlock(player, args);
            case "unblock":     return handleUnblock(player, args);
            case "blocked":     return handleBlockedList(player);
            case "notifications": return handleNotifications(player, args);
            default:
                player.sendMessage(CC.color("&cUsage: /friend <add|remove|accept|deny|list|online|message|favorite|block|unblock|blocked|notifications>"));
                return true;
        }
    }

    @SuppressWarnings("deprecation")
    private boolean handleList(Player player) {
        FriendManager fm = plugin.getFriendManager();
        Set<UUID> set = fm.getFriends(player.getUniqueId());
        Set<UUID> favSet = fm.getFavorites(player.getUniqueId());

        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &aFriends &8(&f" + set.size() + "&8)  " +
                "&7Online: &a" + fm.getOnlineFriendsCount(player.getUniqueId())));
        if (set.isEmpty()) {
            player.sendMessage(CC.color("  &7No friends yet. Use &f/friend add <player>&7."));
        } else {
            List<UUID> sorted = new ArrayList<>(set);
            // Favorites first, then online, then offline
            sorted.sort(new Comparator<UUID>() {
                public int compare(UUID a, UUID b) {
                    boolean aFav = favSet.contains(a);
                    boolean bFav = favSet.contains(b);
                    if (aFav != bFav) return aFav ? -1 : 1;
                    boolean aOnline = Bukkit.getPlayer(a) != null;
                    boolean bOnline = Bukkit.getPlayer(b) != null;
                    if (aOnline != bOnline) return aOnline ? -1 : 1;
                    return 0;
                }
            });
            for (UUID fid : sorted) {
                Player online = Bukkit.getPlayer(fid);
                String star = favSet.contains(fid) ? "&6★ " : "  ";
                String status = online != null ? "&a●" : "&8○";
                String name = online != null ? online.getName() : Bukkit.getOfflinePlayer(fid).getName();
                if (name == null) name = fid.toString().substring(0, 8);
                player.sendMessage(CC.color(star + status + " &f" + name));
            }
        }
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean handleAdd(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(CC.color("&cUsage: /friend add <player>")); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || target.equals(player)) { player.sendMessage(CC.color("&cPlayer not found.")); return true; }

        FriendManager fm = plugin.getFriendManager();
        if (fm.hasBlockedPlayer(target.getUniqueId(), player.getUniqueId())) {
            player.sendMessage(CC.color("&cYou cannot send a friend request to this player."));
            return true;
        }
        if (fm.areFriends(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(CC.color("&f" + target.getName() + " &7is already your friend."));
            return true;
        }
        // Mutual auto-accept
        if (fm.hasPendingFrom(target.getUniqueId(), player.getUniqueId())) {
            fm.addFriends(player.getUniqueId(), target.getUniqueId());
            fm.cancelRequest(target.getUniqueId());
            player.sendMessage(CC.color("&aYou are now friends with &f" + target.getName() + "&a!"));
            if (fm.hasNotificationsEnabled(target.getUniqueId())) {
                target.sendMessage(CC.color("&f" + player.getName() + " &aaccepted your friend request!"));
            }
            return true;
        }
        fm.sendRequest(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(CC.color("&aFriend request sent to &f" + target.getName() + "&a."));
        if (fm.hasNotificationsEnabled(target.getUniqueId())) {
            target.sendMessage(CC.color(CC.SEPARATOR));
            target.sendMessage(CC.color("  &f" + player.getName() + " &7sent you a friend request."));
            target.sendMessage(CC.color("  &aType &f/friend accept &aor &c/friend deny&a."));
            target.sendMessage(CC.color(CC.SEPARATOR));
        }
        return true;
    }

    private boolean handleRemove(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(CC.color("&cUsage: /friend remove <player>")); return true; }
        FriendManager fm = plugin.getFriendManager();
        Player onlineTarget = Bukkit.getPlayerExact(args[1]);
        UUID targetUUID = onlineTarget != null ? onlineTarget.getUniqueId()
                : resolveFromSet(fm.getFriends(player.getUniqueId()), args[1]);
        if (targetUUID == null || !fm.areFriends(player.getUniqueId(), targetUUID)) {
            player.sendMessage(CC.color("&cThat player is not in your friends list."));
            return true;
        }
        String targetName = resolveName(onlineTarget, targetUUID, args[1]);
        fm.removeFriend(player.getUniqueId(), targetUUID);
        player.sendMessage(CC.color("&7Removed &f" + targetName + " &7from your friends."));
        if (onlineTarget != null && fm.hasNotificationsEnabled(targetUUID)) {
            onlineTarget.sendMessage(CC.color("&f" + player.getName() + " &7removed you as a friend."));
        }
        return true;
    }

    private boolean handleAccept(Player player) {
        FriendManager fm = plugin.getFriendManager();
        UUID fromUUID = fm.getIncomingRequest(player.getUniqueId());
        if (fromUUID == null) { player.sendMessage(CC.color("&cNo pending friend requests.")); return true; }
        Player from = Bukkit.getPlayer(fromUUID);
        fm.addFriends(player.getUniqueId(), fromUUID);
        fm.cancelRequest(fromUUID);
        String fname = from != null ? from.getName() : fromUUID.toString().substring(0, 8);
        player.sendMessage(CC.color("&aYou are now friends with &f" + fname + "&a!"));
        if (from != null && fm.hasNotificationsEnabled(fromUUID)) {
            from.sendMessage(CC.color("&f" + player.getName() + " &aaccepted your friend request!"));
        }
        return true;
    }

    private boolean handleDeny(Player player) {
        FriendManager fm = plugin.getFriendManager();
        UUID fromUUID = fm.getIncomingRequest(player.getUniqueId());
        if (fromUUID == null) { player.sendMessage(CC.color("&cNo pending friend requests.")); return true; }
        Player from = Bukkit.getPlayer(fromUUID);
        fm.cancelRequest(fromUUID);
        player.sendMessage(CC.color("&7Friend request denied."));
        if (from != null) from.sendMessage(CC.color("&f" + player.getName() + " &7denied your friend request."));
        return true;
    }

    private boolean handleOnline(Player player) {
        FriendManager fm = plugin.getFriendManager();
        List<Player> online = fm.getOnlineFriends(player.getUniqueId());
        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &aOnline Friends &8(&f" + online.size() + "&8)"));
        if (online.isEmpty()) {
            player.sendMessage(CC.color("  &7None of your friends are online."));
        } else {
            for (Player p : online) {
                boolean fav = fm.isFavorite(player.getUniqueId(), p.getUniqueId());
                String star = fav ? "&6★ " : "  ";
                player.sendMessage(CC.color(star + "&a● &f" + p.getName()));
            }
        }
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean handleMessage(Player player, String[] args) {
        if (args.length < 3) { player.sendMessage(CC.color("&cUsage: /friend message <player> <message>")); return true; }
        FriendManager fm = plugin.getFriendManager();
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { player.sendMessage(CC.color("&cThat player is not online.")); return true; }
        if (!fm.areFriends(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(CC.color("&f" + target.getName() + " &7is not your friend."));
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) sb.append(" ");
            sb.append(args[i]);
        }
        String msg = sb.toString();
        String format = CC.color("&8[&6Friend&8] &f" + player.getName() + " &7→ &f" + target.getName() + " &8» &7" + msg);
        player.sendMessage(format);
        target.sendMessage(format);
        return true;
    }

    private boolean handleFavorite(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(CC.color("&cUsage: /friend favorite <player>")); return true; }
        FriendManager fm = plugin.getFriendManager();
        Player onlineTarget = Bukkit.getPlayerExact(args[1]);
        UUID targetUUID = onlineTarget != null ? onlineTarget.getUniqueId()
                : resolveFromSet(fm.getFriends(player.getUniqueId()), args[1]);
        if (targetUUID == null || !fm.areFriends(player.getUniqueId(), targetUUID)) {
            player.sendMessage(CC.color("&cThat player is not in your friends list."));
            return true;
        }
        String name = resolveName(onlineTarget, targetUUID, args[1]);
        fm.toggleFavorite(player.getUniqueId(), targetUUID);
        boolean isFav = fm.isFavorite(player.getUniqueId(), targetUUID);
        player.sendMessage(CC.color(isFav ? "&6★ &f" + name + " &7added to favorites." : "&7★ &f" + name + " &7removed from favorites."));
        return true;
    }

    private boolean handleBlock(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(CC.color("&cUsage: /friend block <player>")); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || target.equals(player)) { player.sendMessage(CC.color("&cPlayer not found.")); return true; }
        FriendManager fm = plugin.getFriendManager();
        if (fm.hasBlockedPlayer(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(CC.color("&f" + target.getName() + " &7is already blocked."));
            return true;
        }
        fm.blockPlayer(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(CC.color("&7Blocked &f" + target.getName() + "&7. They can no longer send you friend requests."));
        return true;
    }

    private boolean handleUnblock(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(CC.color("&cUsage: /friend unblock <player>")); return true; }
        FriendManager fm = plugin.getFriendManager();
        Player onlineTarget = Bukkit.getPlayerExact(args[1]);
        UUID targetUUID = onlineTarget != null ? onlineTarget.getUniqueId()
                : resolveFromSet(fm.getBlocked(player.getUniqueId()), args[1]);
        if (targetUUID == null || !fm.hasBlockedPlayer(player.getUniqueId(), targetUUID)) {
            player.sendMessage(CC.color("&cThat player is not in your block list."));
            return true;
        }
        String name = resolveName(onlineTarget, targetUUID, args[1]);
        fm.unblockPlayer(player.getUniqueId(), targetUUID);
        player.sendMessage(CC.color("&7Unblocked &f" + name + "&7."));
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleBlockedList(Player player) {
        FriendManager fm = plugin.getFriendManager();
        Set<UUID> blockedSet = fm.getBlocked(player.getUniqueId());
        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &cBlocked Players &8(&f" + blockedSet.size() + "&8)"));
        if (blockedSet.isEmpty()) {
            player.sendMessage(CC.color("  &7No blocked players."));
        } else {
            for (UUID bid : blockedSet) {
                Player p = Bukkit.getPlayer(bid);
                String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(bid).getName();
                if (name == null) name = bid.toString().substring(0, 8);
                player.sendMessage(CC.color("  &7▸ &f" + name));
            }
        }
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean handleNotifications(Player player, String[] args) {
        FriendManager fm = plugin.getFriendManager();
        boolean current = fm.hasNotificationsEnabled(player.getUniqueId());
        boolean newState;
        if (args.length >= 2) {
            newState = args[1].equalsIgnoreCase("on");
        } else {
            newState = !current;
        }
        fm.setNotifications(player.getUniqueId(), newState);
        player.sendMessage(CC.color("&7Friend notifications &f" + (newState ? "&aTURNED ON" : "&cTURNED OFF") + "&7."));
        return true;
    }

    /** Finds a UUID in the given set whose offline player name matches the given name (case-insensitive). */
    @SuppressWarnings("deprecation")
    private UUID resolveFromSet(Set<UUID> uuids, String name) {
        for (UUID uuid : uuids) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && online.getName().equalsIgnoreCase(name)) return uuid;
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (op.getName() != null && op.getName().equalsIgnoreCase(name)) return uuid;
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private String resolveName(Player online, UUID uuid, String fallback) {
        if (online != null) return online.getName();
        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : fallback;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("evaulx.friends")) {
            return Collections.emptyList();
        }
        Player player = (Player) sender;
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("remove") || sub.equals("message") ||
                    sub.equals("msg") || sub.equals("favorite") || sub.equals("fav") ||
                    sub.equals("block") || sub.equals("unblock")) {
                return Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.equals(player))
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (sub.equals("notifications")) {
                return Arrays.asList("on", "off").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
