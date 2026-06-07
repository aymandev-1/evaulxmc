package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.CC;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class PermissionAuditCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public PermissionAuditCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.permaudit")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("all")) {
            auditAll(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("rank")) {
            if (args.length < 2) return usage(sender, "/permaudit rank <rank>");
            Rank rank = plugin.getRankManager().getRank(args[1]);
            if (rank == null) {
                sender.sendMessage(CC.color("&cRank not found."));
                return true;
            }
            auditRank(sender, rank);
            return true;
        }

        if (args[0].equalsIgnoreCase("player")) {
            if (args.length < 2) return usage(sender, "/permaudit player <player>");
            auditPlayer(sender, args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("cleanup")) {
            return cleanupAudit(sender, args);
        }

        sendHelp(sender);
        return true;
    }

    private void auditAll(CommandSender sender) {
        List<String> warnings = new ArrayList<>();
        Rank defaultRank = plugin.getRankManager().getDefaultRank();
        if (defaultRank == null) warnings.add("&cNo default rank exists.");
        else if (containsDangerous(plugin.getRankManager().getAllPermissions(defaultRank))) {
            warnings.add("&eDefault rank has wildcard or admin-level permissions.");
        }

        Set<String> rankNames = new HashSet<>();
        for (Rank rank : plugin.getRankManager().getRanks()) rankNames.add(rank.getName().toLowerCase(Locale.ENGLISH));

        for (Rank rank : plugin.getRankManager().getRanks()) {
            for (String inherited : rank.getInheritance()) {
                if (!rankNames.contains(inherited.toLowerCase(Locale.ENGLISH))) {
                    warnings.add("&e" + rank.getName() + " inherits missing rank " + inherited + ".");
                }
            }
            if (!rank.isStaff() && containsDangerous(plugin.getRankManager().getAllPermissions(rank))) {
                warnings.add("&e" + rank.getName() + " is not marked staff but has dangerous permissions.");
            }
            if (hasCycle(rank, rank.getName(), new HashSet<String>())) {
                warnings.add("&c" + rank.getName() + " has circular inheritance.");
            }
        }

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cPermission Audit"));
        sender.sendMessage(CC.color("&7Ranks checked: &f" + plugin.getRankManager().getRanks().size()));
        sender.sendMessage(CC.color("&7Op bypass: &f" + plugin.getConfig().getBoolean("permissions.op-bypass", true)));
        if (warnings.isEmpty()) {
            sender.sendMessage(CC.color("&aNo rank permission problems found."));
        } else {
            for (String warning : warnings) sender.sendMessage(CC.color(warning));
        }
        sender.sendMessage(CC.color("&7Use &f/permaudit rank <rank> &7or &f/permaudit player <player>&7 for details."));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void auditRank(CommandSender sender, Rank rank) {
        List<String> permissions = plugin.getRankManager().getAllPermissions(rank);
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cPermission Audit: &f" + rank.getName()));
        sender.sendMessage(CC.color("&7Weight: &f" + rank.getWeight() + " &7Staff: &f" + rank.isStaff() + " &7Default: &f" + rank.isDefault()));
        sender.sendMessage(CC.color("&7Direct permissions: &f" + rank.getPermissions().size()));
        sender.sendMessage(CC.color("&7Effective permissions: &f" + permissions.size()));
        sender.sendMessage(CC.color("&7Inheritance: &f" + (rank.getInheritance().isEmpty() ? "none" : join(rank.getInheritance()))));
        List<String> dangerous = dangerous(permissions);
        if (dangerous.isEmpty()) sender.sendMessage(CC.color("&aNo wildcard/admin permissions found."));
        else sender.sendMessage(CC.color("&eDangerous: &f" + join(dangerous)));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void auditPlayer(CommandSender sender, String input) {
        OfflinePlayer target = plugin.getPlayerLookupManager().find(input);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found in lookup cache."));
            return;
        }

        PlayerProfile profile = target.isOnline()
                ? plugin.getPlayerManager().getProfile(target.getPlayer())
                : plugin.getDatabaseManager().loadProfile(target.getUniqueId(), target.getName() == null ? input : target.getName());
        if (profile == null) {
            sender.sendMessage(CC.color("&cProfile not found."));
            return;
        }

        List<String> permissions = new ArrayList<>();
        Rank primary = plugin.getRankManager().getRank(profile.getRankName());
        if (primary != null) permissions.addAll(plugin.getRankManager().getAllPermissions(primary));
        for (String extraName : profile.getExtraRanks()) {
            Rank extra = plugin.getRankManager().getRank(extraName);
            if (extra != null) permissions.addAll(plugin.getRankManager().getAllPermissions(extra));
        }
        permissions.addAll(profile.getPermissions());

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cPermission Audit: &f" + (target.getName() == null ? input : target.getName())));
        sender.sendMessage(CC.color("&7Primary rank: &f" + profile.getRankName()));
        sender.sendMessage(CC.color("&7Extra ranks: &f" + (profile.getExtraRanks().isEmpty() ? "none" : join(profile.getExtraRanks()))));
        sender.sendMessage(CC.color("&7Player-specific permissions: &f" + profile.getPermissions().size()));
        sender.sendMessage(CC.color("&7Effective tracked permissions: &f" + new LinkedHashSet<>(permissions).size()));
        List<String> dangerous = dangerous(permissions);
        if (!dangerous.isEmpty()) sender.sendMessage(CC.color("&eDangerous: &f" + join(dangerous)));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private boolean hasCycle(Rank current, String target, Set<String> visited) {
        if (current == null) return false;
        if (!visited.add(current.getName().toLowerCase(Locale.ENGLISH))) return false;
        for (String parentName : current.getInheritance()) {
            if (parentName.equalsIgnoreCase(target)) return true;
            if (hasCycle(plugin.getRankManager().getRank(parentName), target, visited)) return true;
        }
        return false;
    }

    private boolean cleanupAudit(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage(CC.color("&eThis removes broken rank inheritance links and duplicate rank entries."));
            sender.sendMessage(CC.color("&eRun &f/permaudit cleanup confirm &eto continue."));
            return true;
        }

        Set<String> rankNames = new HashSet<>();
        for (Rank rank : plugin.getRankManager().getRanks()) rankNames.add(rank.getName().toLowerCase(Locale.ENGLISH));

        int changedRanks = 0;
        int removedInheritance = 0;
        int duplicateEntries = 0;
        for (Rank rank : plugin.getRankManager().getRanks()) {
            int beforePerms = rank.getPermissions().size();
            int beforeInheritance = rank.getInheritance().size();
            dedupe(rank.getPermissions());
            dedupe(rank.getInheritance());
            duplicateEntries += beforePerms - rank.getPermissions().size();
            duplicateEntries += beforeInheritance - rank.getInheritance().size();

            Iterator<String> iterator = rank.getInheritance().iterator();
            while (iterator.hasNext()) {
                String parent = iterator.next();
                if (rankNames.contains(parent.toLowerCase(Locale.ENGLISH))) continue;
                iterator.remove();
                removedInheritance++;
            }

            if (beforePerms != rank.getPermissions().size()
                    || beforeInheritance != rank.getInheritance().size()) {
                plugin.getRankManager().saveRank(rank);
                changedRanks++;
            }
        }

        sender.sendMessage(CC.color("&aPermission audit cleanup complete. &7Ranks changed: &f" + changedRanks
                + " &7Removed inheritance: &f" + removedInheritance
                + " &7Duplicate entries: &f" + duplicateEntries));
        return true;
    }

    private void dedupe(List<String> values) {
        Set<String> seen = new HashSet<>();
        Iterator<String> iterator = values.iterator();
        while (iterator.hasNext()) {
            String value = iterator.next();
            if (value == null || !seen.add(value.toLowerCase(Locale.ENGLISH))) iterator.remove();
        }
    }

    private boolean containsDangerous(List<String> permissions) {
        return !dangerous(permissions).isEmpty();
    }

    private List<String> dangerous(List<String> permissions) {
        List<String> matches = new ArrayList<>();
        for (String permission : permissions) {
            String normalized = permission.startsWith("-") ? permission.substring(1) : permission;
            if (normalized.equals("*") || normalized.endsWith(".*")
                    || normalized.equalsIgnoreCase("evaulx.*")
                    || normalized.equalsIgnoreCase("bukkit.command.op")
                    || normalized.equalsIgnoreCase("minecraft.command.op")) {
                if (!matches.contains(permission)) matches.add(permission);
            }
        }
        return matches;
    }

    private boolean usage(CommandSender sender, String usage) {
        sender.sendMessage(CC.color("&cUsage: " + usage));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(CC.color("&cUsage: /permaudit [all|rank <rank>|player <player>|cleanup confirm]"));
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(value);
        }
        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.permaudit")) return Collections.emptyList();
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            Collections.addAll(suggestions, "all", "rank", "player", "cleanup");
            return filter(suggestions, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("cleanup")) return filter(Collections.singletonList("confirm"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("rank")) {
            for (Rank rank : plugin.getRankManager().getRanks()) suggestions.add(rank.getName());
            return filter(suggestions, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) suggestions.add(player.getName());
            suggestions.addAll(plugin.getPlayerLookupManager().suggest(args[1], 20));
            return filter(suggestions, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ENGLISH);
        List<String> filtered = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ENGLISH).startsWith(lower) && !filtered.contains(value)) filtered.add(value);
        }
        Collections.sort(filtered, String.CASE_INSENSITIVE_ORDER);
        return filtered;
    }
}
