package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.managers.AppealManager.Appeal;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class AppealCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;
    private static final SimpleDateFormat FMT = new SimpleDateFormat("MMM d, yyyy h:mm a");

    public AppealCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // --- Staff: /appeal list ---
        if (args.length == 1 && args[0].equalsIgnoreCase("list") && sender.hasPermission("evaulx.appeals.manage")) {
            List<Appeal> pending = plugin.getAppealManager().getPending();
            if (pending.isEmpty()) {
                sender.sendMessage(CC.color("&7No pending appeals."));
                return true;
            }
            sender.sendMessage(CC.color("&8[&6Appeals&8] &7Pending: &f" + pending.size()));
            for (Appeal a : pending) {
                sender.sendMessage(CC.color("  &8» &f" + a.punishmentId + " &8| &7" + a.playerName + ": &f" + a.reason));
            }
            return true;
        }

        // --- Staff: /appeal accept <id> ---
        if (args.length == 2 && args[0].equalsIgnoreCase("accept") && sender.hasPermission("evaulx.appeals.manage")) {
            String id = args[1];
            Appeal appeal = plugin.getAppealManager().get(id);
            if (appeal == null || !"pending".equals(appeal.status)) {
                sender.sendMessage(CC.color("&cNo pending appeal found for &f" + id + "&c."));
                return true;
            }
            plugin.getAppealManager().resolve(id, sender.getName(), true, "Accepted");
            sender.sendMessage(CC.color("&aAccepted appeal &f" + id + " &afor &f" + appeal.playerName + "&a. Punishment pardoned."));
            Player target = Bukkit.getPlayer(appeal.playerName);
            if (target != null) {
                target.sendMessage(CC.color("&a&lAppeal Accepted &7— Your punishment &f(" + id + ") &7has been pardoned."));
            }
            UUID staffUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : new UUID(0L, 0L);
            String finalStaffName = sender.getName();
            String playerName = appeal.playerName;
            TaskUtil.async(() -> {
                OfflinePlayer op = plugin.getPlayerLookupManager().find(playerName);
                if (op == null) return;
                List<Punishment> history = plugin.getPunishmentManager().getHistory(op.getUniqueId());
                for (Punishment p : history) {
                    if (!p.getId().equals(id)) continue;
                    p.setActive(false);
                    p.setAppealStatus("accepted");
                    p.setRemovedBy(staffUUID);
                    p.setRemovedReason("Appeal accepted by " + finalStaffName);
                    p.setRemovedAt(System.currentTimeMillis());
                    plugin.getDatabaseManager().updatePunishment(p);
                    break;
                }
            });
            plugin.getStaffRequestManager().broadcastStaff(
                    "&8[&6Appeal&8] &f" + sender.getName() + " &aaccepted &7appeal for &f" + id + " &8(&f" + playerName + "&8)",
                    "evaulx.appeals.see");
            plugin.getStaffRequestManager().logAction(sender.getName(), "APPEAL_ACCEPTED", id, "Accepted appeal for " + playerName);
            return true;
        }

        // --- Staff: /appeal deny <id> <reason> ---
        if (args.length >= 3 && args[0].equalsIgnoreCase("deny") && sender.hasPermission("evaulx.appeals.manage")) {
            String id = args[1];
            String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            Appeal appeal = plugin.getAppealManager().get(id);
            if (appeal == null || !"pending".equals(appeal.status)) {
                sender.sendMessage(CC.color("&cNo pending appeal found for &f" + id + "&c."));
                return true;
            }
            plugin.getAppealManager().resolve(id, sender.getName(), false, reason);
            sender.sendMessage(CC.color("&cDenied appeal &f" + id + " &cfor &f" + appeal.playerName + "&c."));
            Player target = Bukkit.getPlayer(appeal.playerName);
            if (target != null) {
                target.sendMessage(CC.color("&c&lAppeal Denied &7— Punishment &f(" + id + ") &7remains active. Reason: &f" + reason));
            }
            plugin.getStaffRequestManager().broadcastStaff(
                    "&8[&6Appeal&8] &f" + sender.getName() + " &cdenied &7appeal for &f" + id
                            + " &8(&f" + appeal.playerName + "&8): &7" + reason,
                    "evaulx.appeals.see");
            plugin.getStaffRequestManager().logAction(sender.getName(), "APPEAL_DENIED", id, reason);
            return true;
        }

        // --- Player: /appeal <reason> ---
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can submit appeals. Staff: /appeal list | accept <id> | deny <id> <reason>"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("evaulx.appeal")) {
            player.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(CC.color("&cUsage: /appeal <reason>"));
            player.sendMessage(CC.color("&7Example: &f/appeal I was falsely banned."));
            return true;
        }

        String reason = String.join(" ", args);
        int maxLen = plugin.getConfig().getInt("appeals.max-reason-length", 200);
        if (reason.length() > maxLen) {
            player.sendMessage(CC.color("&cReason too long (max " + maxLen + " characters)."));
            return true;
        }

        TaskUtil.async(() -> {
            Punishment active = plugin.getPunishmentManager().getActiveBan(player.getUniqueId());
            if (active == null) active = plugin.getPunishmentManager().getActiveMute(player.getUniqueId());
            Punishment finalActive = active;
            TaskUtil.sync(() -> {
                if (finalActive == null) {
                    player.sendMessage(CC.color("&cYou have no active ban or mute to appeal."));
                    return;
                }
                String punId = finalActive.getId();
                if (plugin.getAppealManager().hasPending(punId)) {
                    player.sendMessage(CC.color("&cYou already have a pending appeal for &f" + punId + "&c."));
                    return;
                }
                if (plugin.getAppealManager().hasAny(punId)) {
                    Appeal existing = plugin.getAppealManager().get(punId);
                    player.sendMessage(CC.color("&cYour previous appeal for &f" + punId + " &cwas &f" + existing.status + "&c."));
                    return;
                }
                plugin.getAppealManager().submit(punId, player.getName(), reason);
                player.sendMessage(CC.color("&a&lAppeal submitted &7for punishment &f" + punId + "&7."));
                player.sendMessage(CC.color("&7Our staff team will review your appeal shortly."));
                plugin.getStaffRequestManager().broadcastStaff(
                        "&8[&6Appeal&8] &f" + player.getName() + " &7submitted an appeal for &f" + punId + "&7: &f" + reason,
                        "evaulx.appeals.see");
                plugin.getStaffRequestManager().logAction(player.getName(), "APPEAL_SUBMITTED", punId, reason);
            });
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<>();
            if (sender.hasPermission("evaulx.appeals.manage")) {
                opts.addAll(Arrays.asList("accept", "deny", "list"));
            }
            return filter(opts, args[0]);
        }
        if (args.length == 2 && sender.hasPermission("evaulx.appeals.manage")
                && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny"))) {
            List<String> ids = new ArrayList<>();
            for (Appeal a : plugin.getAppealManager().getPending()) ids.add(a.punishmentId);
            return filter(ids, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) return list;
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase(Locale.ENGLISH);
        for (String s : list) if (s.toLowerCase(Locale.ENGLISH).startsWith(lower)) result.add(s);
        return result;
    }
}
