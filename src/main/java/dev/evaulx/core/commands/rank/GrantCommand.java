package dev.evaulx.core.commands.rank;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.managers.GrantTemplateManager.GrantTemplate;
import dev.evaulx.core.managers.GrantManager.PendingGrant;
import dev.evaulx.core.models.Grant;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GrantCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public GrantCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.grant")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("pending")) {
            if (!sender.hasPermission("evaulx.grant.approve")) {
                sender.sendMessage(CC.color("&cNo permission."));
                return true;
            }
            sendPending(sender);
            return true;
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("approve")) {
            if (!sender.hasPermission("evaulx.grant.approve")) {
                sender.sendMessage(CC.color("&cNo permission."));
                return true;
            }
            Grant grant = plugin.getGrantManager().approveGrant(args[1], sender);
            if (grant == null) sender.sendMessage(CC.color("&cPending grant not found or rank no longer exists."));
            else sender.sendMessage(CC.color("&aApproved grant request &f" + args[1].toUpperCase(Locale.ENGLISH) + "&a."));
            return true;
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("deny")) {
            if (!sender.hasPermission("evaulx.grant.approve")) {
                sender.sendMessage(CC.color("&cNo permission."));
                return true;
            }
            boolean denied = plugin.getGrantManager().denyGrant(args[1], sender,
                    args.length > 2 ? join(args, 2) : "Denied by staff");
            sender.sendMessage(CC.color(denied ? "&cDenied grant request &f" + args[1].toUpperCase(Locale.ENGLISH) + "&c." : "&cPending grant not found."));
            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("templates") || args[0].equalsIgnoreCase("template-list"))) {
            sendTemplates(sender);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("template")) {
            return grantTemplate(sender, args);
        }

        if (args.length == 0 && sender instanceof Player) {
            plugin.getGuiManager().openPlayerPicker((Player) sender, dev.evaulx.core.gui.GuiManager.PlayerAction.GRANT);
            return true;
        }

        if (args.length == 1 && sender instanceof Player) {
            OfflinePlayer target = plugin.getPlayerLookupManager().find(args[0]);
            if (target == null) {
                sender.sendMessage(CC.color("&cPlayer not found."));
                return true;
            }
            plugin.getGuiManager().openGrantRankPicker((Player) sender, target);
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(CC.color("&cUsage: /grant <player> <rank> <duration|perm> [reason]"));
            sender.sendMessage(CC.color("&cUsage: /grant template <player> <template> [reason]"));
            if (sender instanceof Player) sender.sendMessage(CC.color("&7Tip: use &f/grant &7to open the grant GUI."));
            return true;
        }

        OfflinePlayer target = plugin.getPlayerLookupManager().find(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        Rank rank = plugin.getRankManager().getRank(args[1]);
        if (rank == null) {
            sender.sendMessage(CC.color("&cRank not found."));
            return true;
        }

        long expiresAt = TimeUtil.parseDuration(args[2]);
        if (expiresAt == -1L && !isPermanent(args[2])) {
            sender.sendMessage(CC.color("&cInvalid duration. Example: 7d, 12h, perm"));
            return true;
        }

        String reason = args.length > 3 ? join(args, 3) : "Granted by staff";
        if (plugin.getConfig().getBoolean("grant-approvals.enabled", false)
                && !sender.hasPermission("evaulx.grant.approve")
                && !sender.hasPermission("evaulx.grant.bypass-approval")) {
            PendingGrant pending = plugin.getGrantManager().requestGrant(sender, target, rank, expiresAt, reason);
            sender.sendMessage(CC.color("&eGrant request created for approval. &8(ID " + pending.getId() + ")"));
            return true;
        }

        Grant grant = plugin.getGrantManager().grant(sender, target, rank, expiresAt, reason);
        plugin.getStaffRequestManager().logAction(sender.getName(), "GRANT", safeName(target),
                rank.getName() + " | " + grant.getDurationString() + " | " + reason);
        sender.sendMessage(CC.color("&aGranted &f" + safeName(target) + " &arank " + rank.getDisplayName()
                + " &afor &f" + grant.getDurationString() + " &8(ID " + grant.getId() + ")"));
        return true;
    }

    private void sendPending(CommandSender sender) {
        List<PendingGrant> pending = plugin.getGrantManager().getPendingGrants();
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cPending Grant Requests"));
        if (pending.isEmpty()) {
            sender.sendMessage(CC.color("&7No pending grant requests."));
        } else {
            for (PendingGrant grant : pending) {
                sender.sendMessage(CC.color("&8" + grant.getId() + " &f" + grant.getTargetName()
                        + " &7-> &f" + grant.getRankName()
                        + " &7by &f" + grant.getRequesterName()));
            }
        }
        sender.sendMessage(CC.color("&7Use &f/grant approve <id> &7or &f/grant deny <id> [reason]&7."));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void sendTemplates(CommandSender sender) {
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cGrant Templates"));
        List<GrantTemplate> templates = plugin.getGrantTemplateManager().getTemplates();
        if (templates.isEmpty()) {
            sender.sendMessage(CC.color("&7No grant templates configured."));
        } else {
            for (GrantTemplate template : templates) {
                sender.sendMessage(CC.color("&f" + template.getId()
                        + " &7-> &f" + template.getRankName()
                        + " &7for &f" + template.getDuration()
                        + " &8- &7" + template.getReason()));
            }
        }
        sender.sendMessage(CC.color("&7Use &f/grant template <player> <template> [reason]&7."));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private boolean grantTemplate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(CC.color("&cUsage: /grant template <player> <template> [reason]"));
            return true;
        }

        OfflinePlayer target = plugin.getPlayerLookupManager().find(args[1]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        GrantTemplate template = plugin.getGrantTemplateManager().getTemplate(args[2]);
        if (template == null) {
            sender.sendMessage(CC.color("&cGrant template not found. Use &f/grant templates&c."));
            return true;
        }
        if (!template.getPermission().isEmpty() && !sender.hasPermission(template.getPermission())) {
            sender.sendMessage(CC.color("&cNo permission for that grant template."));
            return true;
        }

        Rank rank = plugin.getRankManager().getRank(template.getRankName());
        if (rank == null) {
            sender.sendMessage(CC.color("&cTemplate rank no longer exists: &f" + template.getRankName()));
            return true;
        }

        long expiresAt = TimeUtil.parseDuration(template.getDuration());
        if (expiresAt == -1L && !isPermanent(template.getDuration())) {
            sender.sendMessage(CC.color("&cTemplate duration is invalid: &f" + template.getDuration()));
            return true;
        }

        String reason = args.length > 3 ? join(args, 3) : template.getReason();
        if (plugin.getConfig().getBoolean("grant-approvals.enabled", false)
                && !sender.hasPermission("evaulx.grant.approve")
                && !sender.hasPermission("evaulx.grant.bypass-approval")) {
            PendingGrant pending = plugin.getGrantManager().requestGrant(sender, target, rank, expiresAt, reason);
            sender.sendMessage(CC.color("&eGrant template request created for approval. &8(ID " + pending.getId() + ")"));
            return true;
        }

        Grant grant = plugin.getGrantManager().grant(sender, target, rank, expiresAt, reason);
        plugin.getStaffRequestManager().logAction(sender.getName(), "GRANT_TEMPLATE", safeName(target),
                template.getId() + " | " + rank.getName() + " | " + grant.getDurationString() + " | " + reason);
        sender.sendMessage(CC.color("&aApplied template &f" + template.getId() + " &ato &f" + safeName(target)
                + " &8(ID " + grant.getId() + ")"));
        return true;
    }

    private boolean isPermanent(String input) {
        return input.equalsIgnoreCase("perm")
                || input.equalsIgnoreCase("permanent")
                || input.equalsIgnoreCase("-1");
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private String safeName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.grant")) return Collections.emptyList();
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            Collections.addAll(suggestions, "pending", "approve", "deny", "templates", "template");
            for (Player player : Bukkit.getOnlinePlayers()) suggestions.add(player.getName());
            return filter(suggestions, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("approve") || args[0].equalsIgnoreCase("deny"))) {
            for (PendingGrant grant : plugin.getGrantManager().getPendingGrants()) suggestions.add(grant.getId());
            return filter(suggestions, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("template")) {
            for (Player player : Bukkit.getOnlinePlayers()) suggestions.add(player.getName());
            suggestions.addAll(plugin.getPlayerLookupManager().suggest(args[1], 20));
            return filter(suggestions, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("template")) {
            suggestions.addAll(plugin.getGrantTemplateManager().getTemplateIds());
            return filter(suggestions, args[2]);
        }
        if (args.length == 2) {
            for (Rank rank : plugin.getRankManager().getVisibleRanksByWeight()) suggestions.add(rank.getName());
            return filter(suggestions, args[1]);
        }
        if (args.length == 3) {
            Collections.addAll(suggestions, "perm", "1h", "1d", "7d", "30d", "90d");
            return filter(suggestions, args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ENGLISH);
        List<String> filtered = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ENGLISH).startsWith(lower)) filtered.add(value);
        }
        Collections.sort(filtered);
        return filtered;
    }
}
