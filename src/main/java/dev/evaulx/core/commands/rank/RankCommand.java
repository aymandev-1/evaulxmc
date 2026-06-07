package dev.evaulx.core.commands.rank;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Grant;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class RankCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "help", "list", "ladder", "gui", "info", "create", "delete", "display", "permission", "prefix", "suffix", "color", "namecolor",
            "weight", "default", "staff", "hidden", "category", "perm", "inherit", "clone", "set", "addextra",
            "removeextra", "player", "reload", "presets", "cleanup", "import", "export", "backup", "backups", "rollback", "audit"
    );
    private static final List<String> RANK_CATEGORY_ORDER = Arrays.asList(
            "Staff", "Media", "Store", "Hidden", "Default"
    );

    private final EvaulxCore plugin;

    public RankCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] routed = route(command.getName(), args);
        if (routed.length == 0 || routed[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = routed[0].toLowerCase(Locale.ENGLISH);
        if (sub.equals("gui")) return openGui(sender);
        if (sub.equals("list")) return listRanks(sender, routed);
        if (sub.equals("ladder")) return rankLadder(sender);
        if (sub.equals("info")) return rankInfo(sender, routed);
        if (sub.equals("create")) return createRank(sender, routed);
        if (sub.equals("delete") || sub.equals("remove")) return deleteRank(sender, routed);
        if (sub.equals("display")) return setText(sender, routed, "display");
        if (sub.equals("permission")) return setRankPermission(sender, routed);
        if (sub.equals("prefix")) return setText(sender, routed, "prefix");
        if (sub.equals("suffix")) return setText(sender, routed, "suffix");
        if (sub.equals("color") || sub.equals("namecolor")) return setText(sender, routed, "color");
        if (sub.equals("weight")) return setWeight(sender, routed);
        if (sub.equals("default")) return setDefault(sender, routed);
        if (sub.equals("staff")) return setStaff(sender, routed);
        if (sub.equals("hidden")) return setHidden(sender, routed);
        if (sub.equals("category")) return setCategory(sender, routed);
        if (sub.equals("perm") || sub.equals("permission")) return editPermissions(sender, routed);
        if (sub.equals("inherit") || sub.equals("inheritance")) return editInheritance(sender, routed);
        if (sub.equals("clone") || sub.equals("copy")) return cloneRank(sender, routed);
        if (sub.equals("set")) return setPlayerRank(sender, routed);
        if (sub.equals("addextra")) return addExtraRank(sender, routed);
        if (sub.equals("removeextra")) return removeExtraRank(sender, routed);
        if (sub.equals("player")) return playerInfo(sender, routed);
        if (sub.equals("reload")) return reloadRanks(sender);
        if (sub.equals("presets") || sub.equals("preset")) return installPresets(sender, routed);
        if (sub.equals("cleanup")) return cleanupRanks(sender, routed);
        if (sub.equals("import")) return importRanks(sender, routed);
        if (sub.equals("export")) return exportRanks(sender, routed);
        if (sub.equals("backup")) return createBackup(sender, routed);
        if (sub.equals("backups")) return listBackups(sender);
        if (sub.equals("rollback")) return rollbackRanks(sender, routed);
        if (sub.equals("audit")) {
            Bukkit.dispatchCommand(sender, "permaudit all");
            return true;
        }

        sender.sendMessage(CC.color("&cUnknown rank command. Use /rank help."));
        return true;
    }

    private String[] route(String commandName, String[] args) {
        String name = commandName.toLowerCase(Locale.ENGLISH);
        if (name.equals("rank")) return args;
        if (name.equals("listranks")) return prepend("list", args);
        if (name.equals("createrank")) return prepend("create", args);
        if (name.equals("deleterank")) return prepend("delete", args);
        if (name.equals("rankprefix")) return prepend("prefix", args);
        if (name.equals("ranksuffix")) return prepend("suffix", args);
        if (name.equals("rankcolor")) return prepend("color", args);
        if (name.equals("ranknamecolor")) return prepend("namecolor", args);
        if (name.equals("rankdisplay")) return prepend("display", args);
        if (name.equals("rankpermission")) return prepend("permission", args);
        if (name.equals("rankladder")) return prepend("ladder", args);
        if (name.equals("rankweight")) return prepend("weight", args);
        if (name.equals("rankdefault")) return prepend("default", args);
        if (name.equals("rankstaff")) return prepend("staff", args);
        if (name.equals("rankperm")) return prepend("perm", args);
        if (name.equals("rankinherit")) return prepend("inherit", args);
        if (name.equals("rankclone")) return prepend("clone", args);
        if (name.equals("rankreload")) return prepend("reload", args);
        if (name.equals("playerrank")) return prepend("player", args);
        return args;
    }

    private String[] prepend(String value, String[] args) {
        String[] routed = new String[args.length + 1];
        routed[0] = value;
        System.arraycopy(args, 0, routed, 1, args.length);
        return routed;
    }

    private void sendHelp(CommandSender sender) {
        if (!can(sender, "evaulx.rank")) return;
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cRank Commands"));
        sender.sendMessage(CC.color("&7/rank list [category|page] [page] &8- &fList ranks"));
        sender.sendMessage(CC.color("&7/rank ladder &8- &fView grouped rank hierarchy"));
        sender.sendMessage(CC.color("&7/rank info <rank> &8- &fView rank details"));
        sender.sendMessage(CC.color("&7/rank create <rank> [weight] &8- &fCreate a rank"));
        sender.sendMessage(CC.color("&7/rank delete <rank> &8- &fDelete a rank"));
        sender.sendMessage(CC.color("&7/rank display <rank> <display|none> &8- &fSet display name"));
        sender.sendMessage(CC.color("&7/rank permission <rank> <permission|none> &8- &fSet rank node"));
        sender.sendMessage(CC.color("&7/rank prefix <rank> <prefix|none> &8- &fSet prefix"));
        sender.sendMessage(CC.color("&7/rank suffix <rank> <suffix|none> &8- &fSet suffix"));
        sender.sendMessage(CC.color("&7/rank color <rank> <color> &8- &fSet chat/name color"));
        sender.sendMessage(CC.color("&7/rank weight <rank> <number> &8- &fSet priority"));
        sender.sendMessage(CC.color("&7/rank default <rank> &8- &fSet default rank"));
        sender.sendMessage(CC.color("&7/rank staff <rank> <true|false> &8- &fToggle staff flag"));
        sender.sendMessage(CC.color("&7/rank category <rank> <Staff|Media|Store|Hidden|Default> &8- &fSet saved category"));
        sender.sendMessage(CC.color("&7/rank perm <rank> <add|remove|list|clear> [perm]"));
        sender.sendMessage(CC.color("&7/rank inherit <rank> <add|remove|list|clear> [parent]"));
        sender.sendMessage(CC.color("&7/rank set <player> <rank> &8- &fSet primary rank"));
        sender.sendMessage(CC.color("&7/rank addextra <player> <rank> &8- &fAdd extra rank"));
        sender.sendMessage(CC.color("&7/rank removeextra <player> <rank> &8- &fRemove extra rank"));
        sender.sendMessage(CC.color("&7/rank player <player> &8- &fView player ranks"));
        sender.sendMessage(CC.color("&7/rank clone <from> <new> &8- &fCopy a rank"));
        sender.sendMessage(CC.color("&7/rank reload &8- &fReload ranks from storage"));
        sender.sendMessage(CC.color("&7/rank cleanup confirm &8- &fRemove deleted ranks from profiles and grants"));
        sender.sendMessage(CC.color("&7/rank backup [reason] &8- &fCreate a rank backup"));
        sender.sendMessage(CC.color("&7/rank backups &8- &fList restorable rank backups"));
        sender.sendMessage(CC.color("&7/rank rollback <backup-file> confirm &8- &fRestore a rank backup"));
        sender.sendMessage(CC.color("&7/rank audit &8- &fRun permission audit"));
        sender.sendMessage(CC.color("&7/rank presets &8- &fOpen reset confirmation"));
        sender.sendMessage(CC.color("&7/rank presets confirm &8- &fDelete all ranks and install only EvaulxMC ranks"));
        sender.sendMessage(CC.color("&7/rank import <file.yml> &8- &fImport ranks from plugins/EvaulxMC/imports"));
        sender.sendMessage(CC.color("&7/rank export [file.yml] &8- &fExport ranks as YAML"));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private boolean openGui(CommandSender sender) {
        if (!can(sender, "evaulx.rank")) return true;
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cPlayers only."));
            return true;
        }
        plugin.getGuiManager().openRanks((Player) sender);
        return true;
    }

    private boolean listRanks(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank")) return true;
        String categoryFilter = null;
        int pageArg = 1;
        if (args.length > 1 && isCategory(args[1])) {
            categoryFilter = Rank.normalizeCategory(args[1]);
            pageArg = 2;
        }

        List<Rank> ranks = new ArrayList<>(categoryFilter != null && categoryFilter.equalsIgnoreCase("Hidden")
                ? plugin.getRankManager().getRanksByWeight()
                : plugin.getRankManager().getVisibleRanksByWeight());
        if (categoryFilter != null) {
            Iterator<Rank> iterator = ranks.iterator();
            while (iterator.hasNext()) {
                if (!rankCategory(iterator.next()).equalsIgnoreCase(categoryFilter)) iterator.remove();
            }
        }
        int pageSize = 8;
        int maxPage = Math.max(1, (int) Math.ceil(ranks.size() / (double) pageSize));
        int page = Math.min(maxPage, Math.max(1, parseInt(args.length > pageArg ? args[pageArg] : "1", 1)));
        int start = (page - 1) * pageSize;
        int end = Math.min(ranks.size(), start + pageSize);

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cRanks" + (categoryFilter == null ? "" : " &7- &f" + categoryFilter)
                + " &7(" + ranks.size() + " total, page " + page + "/" + maxPage + ")"));
        for (int i = start; i < end; i++) {
            Rank rank = ranks.get(i);
            sender.sendMessage(CC.color("&8" + (i + 1) + ". &f" + rank.getDisplayName()
                    + " &7cat:&f" + rankCategory(rank)
                    + " &7weight:&f" + rank.getWeight()
                    + " &7perms:&f" + rank.getPermissions().size()
                    + " &7inherits:&f" + rank.getInheritance().size()
                    + (rank.isDefault() ? " &a[DEFAULT]" : "")
                    + (rank.isStaff() ? " &c[STAFF]" : "")));
        }
        if (ranks.isEmpty()) sender.sendMessage(CC.color("&7No ranks found."));
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean rankLadder(CommandSender sender) {
        if (!can(sender, "evaulx.rank")) return true;

        Map<String, List<Rank>> grouped = new LinkedHashMap<>();
        for (String category : RANK_CATEGORY_ORDER) grouped.put(category, new ArrayList<Rank>());

        for (Rank rank : plugin.getRankManager().getRanksByWeight()) {
            String category = rankCategory(rank);
            if (!grouped.containsKey(category)) category = "Default";
            grouped.get(category).add(rank);
        }

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cRank Ladder &7(" + plugin.getRankManager().getRanks().size() + " total)"));
        for (Map.Entry<String, List<Rank>> entry : grouped.entrySet()) {
            sender.sendMessage(CC.color("&c" + entry.getKey() + " &8(" + entry.getValue().size() + ")"));
            for (Rank rank : entry.getValue()) {
                List<String> allPermissions = plugin.getRankManager().getAllPermissions(rank);
                sender.sendMessage(CC.color("  &8- &f" + rank.getDisplayName()
                        + " &7w:&f" + rank.getWeight()
                        + " &7direct:&f" + rank.getPermissions().size()
                        + " &7all:&f" + allPermissions.size()
                        + " &7inherits:&f" + (rank.getInheritance().isEmpty() ? "none" : String.join("/", rank.getInheritance()))));
            }
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean rankInfo(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank")) return true;
        if (args.length < 2) return usage(sender, "/rank info <rank>");

        Rank rank = getRank(sender, args[1]);
        if (rank == null) return true;

        List<String> allPermissions = plugin.getRankManager().getAllPermissions(rank);
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cRank: " + rank.getDisplayName()));
        sender.sendMessage(CC.color("&7Name: &f" + rank.getName()));
        sender.sendMessage(CC.color("&7Display: &f") + printable(rank.getDisplay())
                + CC.color(" &7Permission: &f") + printable(rank.getPermission()));
        sender.sendMessage(CC.color("&7Category: &f" + rankCategory(rank)
                + " &7Position: &f#" + rankPosition(rank)
                + " &7Visible: &f" + !rank.isHidden()));
        sender.sendMessage(CC.color("&7Prefix: &f") + printable(rank.getPrefix())
                + CC.color(" &7Preview: ") + CC.color(rank.getPrefix() + rank.getName()));
        sender.sendMessage(CC.color("&7Suffix: &f") + printable(rank.getSuffix())
                + CC.color(" &7Color: &f") + printable(rank.getColor()));
        sender.sendMessage(CC.color("&7Weight: &f" + rank.getWeight()
                + " &7Default: &f" + rank.isDefault()
                + " &7Staff: &f" + rank.isStaff()
                + " &7Hidden: &f" + rank.isHidden()));
        sender.sendMessage(CC.color("&7Inheritance: &f" + (rank.getInheritance().isEmpty() ? "none" : String.join(", ", rank.getInheritance()))));
        sender.sendMessage(CC.color("&7Direct permissions: &f" + rank.getPermissions().size()
                + " &7All permissions: &f" + allPermissions.size()));
        for (String permission : rank.getPermissions()) sender.sendMessage(CC.color("  &8- &7" + permission));
        List<String> inheritedPermissions = inheritedPermissionPreview(rank, allPermissions);
        if (!inheritedPermissions.isEmpty()) {
            sender.sendMessage(CC.color("&7Inherited permission preview:"));
            int limit = Math.min(8, inheritedPermissions.size());
            for (int i = 0; i < limit; i++) sender.sendMessage(CC.color("  &8- &7" + inheritedPermissions.get(i)));
            if (inheritedPermissions.size() > limit) {
                sender.sendMessage(CC.color("  &8... &7" + (inheritedPermissions.size() - limit) + " more inherited permissions"));
            }
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean createRank(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.create")) return true;
        if (args.length < 2) return usage(sender, "/rank create <rank> [weight]");
        if (!isValidRankName(args[1])) {
            sender.sendMessage(CC.color("&cRank names can only use letters, numbers, dashes, and underscores."));
            return true;
        }
        if (plugin.getRankManager().rankExists(args[1])) {
            sender.sendMessage(CC.color("&cThat rank already exists."));
            return true;
        }

        Rank rank = new Rank(args[1]);
        rank.setDisplay(rank.getColor() + rank.getName());
        rank.setPermission(rankPermission(rank.getName()));
        if (!rank.getPermission().isEmpty()) rank.addPermission(rank.getPermission());
        if (args.length > 2) rank.setWeight(parseInt(args[2], 0));
        plugin.getRankManager().saveRank(rank);
        sender.sendMessage(CC.color("&aCreated rank &f" + rank.getName() + " &awith weight &f" + rank.getWeight() + "&a."));
        return true;
    }

    private boolean deleteRank(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.delete")) return true;
        if (args.length < 2) return usage(sender, "/rank delete <rank>");

        Rank rank = getRank(sender, args[1]);
        if (rank == null) return true;
        if (rank.isDefault()) {
            sender.sendMessage(CC.color("&cSet another default rank before deleting this one."));
            return true;
        }
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            sender.sendMessage(CC.color("&eThis deletes rank &f" + rank.getName() + "&e. Run &f/rank delete "
                    + rank.getName() + " confirm &eto continue."));
            return true;
        }

        String name = rank.getName();
        plugin.getRankManager().deleteRank(name);
        for (Rank other : plugin.getRankManager().getRanks()) {
            if (removeIgnoreCase(other.getInheritance(), name)) plugin.getRankManager().saveRank(other);
        }
        repairOnlineProfiles(name);
        sender.sendMessage(CC.color("&aDeleted rank &f" + name + "&a."));
        return true;
    }

    private boolean setText(CommandSender sender, String[] args, String field) {
        if (!can(sender, "evaulx.rank.edit")) return true;
        if (args.length < 3) return usage(sender, "/rank " + field + " <rank> <value|none>");

        Rank rank = getRank(sender, args[1]);
        if (rank == null) return true;
        String value = join(args, 2);
        if (value.equalsIgnoreCase("none")) value = "";

        if (field.equals("display")) rank.setDisplay(value);
        else if (field.equals("prefix")) rank.setPrefix(value);
        else if (field.equals("suffix")) rank.setSuffix(value);
        else rank.setColor(value);

        plugin.getRankManager().saveRank(rank);
        refreshOnlineForRank(rank.getName());
        sender.sendMessage(CC.color("&aUpdated " + field + " for &f" + rank.getName() + "&a."));
        return true;
    }

    private boolean setRankPermission(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.edit")) return true;
        if (args.length < 3) return usage(sender, "/rank permission <rank> <permission|none>");

        Rank rank = getRank(sender, args[1]);
        if (rank == null) return true;
        String oldPermission = rank.getPermission();
        String permission = args[2].equalsIgnoreCase("none") ? "" : args[2];

        if (oldPermission != null && !oldPermission.isEmpty()) removeIgnoreCase(rank.getPermissions(), oldPermission);
        rank.setPermission(permission);
        if (!permission.isEmpty()) rank.addPermission(permission);

        plugin.getRankManager().saveRank(rank);
        refreshOnlineForRank(rank.getName());
        sender.sendMessage(CC.color("&aUpdated rank permission for &f" + rank.getName() + "&a to &f" + printable(permission) + "&a."));
        return true;
    }

    private boolean setWeight(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.edit")) return true;
        if (args.length < 3) return usage(sender, "/rank weight <rank> <number>");

        Rank rank = getRank(sender, args[1]);
        if (rank == null) return true;
        rank.setWeight(parseInt(args[2], rank.getWeight()));
        plugin.getRankManager().saveRank(rank);
        refreshOnlineForRank(rank.getName());
        sender.sendMessage(CC.color("&aSet &f" + rank.getName() + " &aweight to &f" + rank.getWeight() + "&a."));
        return true;
    }

    private boolean setDefault(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.default")) return true;
        if (args.length < 2) return usage(sender, "/rank default <rank>");

        Rank selected = getRank(sender, args[1]);
        if (selected == null) return true;

        for (Rank rank : plugin.getRankManager().getRanks()) {
            rank.setDefault(rank.getName().equalsIgnoreCase(selected.getName()));
            plugin.getRankManager().saveRank(rank);
        }
        sender.sendMessage(CC.color("&aDefault rank set to &f" + selected.getName() + "&a."));
        return true;
    }

    private boolean setStaff(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.staff")) return true;
        if (args.length < 3) return usage(sender, "/rank staff <rank> <true|false>");

        Rank rank = getRank(sender, args[1]);
        Boolean value = parseBoolean(args[2]);
        if (rank == null) return true;
        if (value == null) {
            sender.sendMessage(CC.color("&cUse true or false."));
            return true;
        }

        rank.setStaff(value);
        if (value) rank.setCategory("Staff");
        else if (rank.getCategory().equalsIgnoreCase("Staff")) rank.setCategory(Rank.inferCategory(rank));
        plugin.getRankManager().saveRank(rank);
        refreshOnlineForRank(rank.getName());
        sender.sendMessage(CC.color("&aSet &f" + rank.getName() + " &astaff to &f" + value + "&a."));
        return true;
    }

    private boolean setHidden(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.hidden")) return true;
        if (args.length < 3) return usage(sender, "/rank hidden <rank> <true|false>");

        Rank rank = getRank(sender, args[1]);
        Boolean value = parseBoolean(args[2]);
        if (rank == null) return true;
        if (value == null) {
            sender.sendMessage(CC.color("&cUse true or false."));
            return true;
        }

        rank.setHidden(value);
        if (value) rank.setCategory("Hidden");
        else if (rank.getCategory().equalsIgnoreCase("Hidden")) rank.setCategory(Rank.inferCategory(rank));
        plugin.getRankManager().saveRank(rank);
        refreshOnlineForRank(rank.getName());
        sender.sendMessage(CC.color("&aSet &f" + rank.getName() + " &ahidden to &f" + value + "&a."));
        return true;
    }

    private boolean setCategory(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.category")) return true;
        if (args.length < 3) return usage(sender, "/rank category <rank> <Staff|Media|Store|Hidden|Default>");

        Rank rank = getRank(sender, args[1]);
        if (rank == null) return true;
        if (!isCategory(args[2])) {
            sender.sendMessage(CC.color("&cCategory must be one of: &f" + String.join(", ", Rank.getAllowedCategories())));
            return true;
        }

        applyCategory(rank, Rank.normalizeCategory(args[2]));
        plugin.getRankManager().saveRank(rank);
        refreshOnlineForRank(rank.getName());
        sender.sendMessage(CC.color("&aSet &f" + rank.getName() + " &acategory to &f" + rank.getCategory() + "&a."));
        return true;
    }

    private boolean editPermissions(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.perm")) return true;
        if (args.length < 3) return usage(sender, "/rank perm <rank> <add|remove|list|clear> [permission]");

        Rank rank = getRank(sender, args[1]);
        if (rank == null) return true;
        String action = args[2].toLowerCase(Locale.ENGLISH);

        if (action.equals("list")) {
            sender.sendMessage(CC.color("&cPermissions for &f" + rank.getName() + "&7:"));
            if (rank.getPermissions().isEmpty()) sender.sendMessage(CC.color("&7No direct permissions."));
            for (String permission : rank.getPermissions()) sender.sendMessage(CC.color("  &8- &7" + permission));
            return true;
        }

        if (action.equals("clear")) {
            rank.getPermissions().clear();
            plugin.getRankManager().saveRank(rank);
            refreshOnlineForRank(rank.getName());
            sender.sendMessage(CC.color("&aCleared permissions from &f" + rank.getName() + "&a."));
            return true;
        }

        if (args.length < 4) return usage(sender, "/rank perm <rank> <add|remove> <permission>");
        String permission = args[3];
        if (action.equals("add")) {
            rank.addPermission(permission);
            sender.sendMessage(CC.color("&aAdded &f" + permission + " &ato &f" + rank.getName() + "&a."));
        } else if (action.equals("remove")) {
            removeIgnoreCase(rank.getPermissions(), permission);
            sender.sendMessage(CC.color("&aRemoved &f" + permission + " &afrom &f" + rank.getName() + "&a."));
        } else {
            sender.sendMessage(CC.color("&cUse add, remove, list, or clear."));
            return true;
        }

        plugin.getRankManager().saveRank(rank);
        refreshOnlineForRank(rank.getName());
        return true;
    }

    private boolean editInheritance(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.inherit")) return true;
        if (args.length < 3) return usage(sender, "/rank inherit <rank> <add|remove|list|clear> [parent]");

        Rank rank = getRank(sender, args[1]);
        if (rank == null) return true;
        String action = args[2].toLowerCase(Locale.ENGLISH);

        if (action.equals("list")) {
            sender.sendMessage(CC.color("&cInheritance for &f" + rank.getName() + "&7: &f"
                    + (rank.getInheritance().isEmpty() ? "none" : String.join(", ", rank.getInheritance()))));
            return true;
        }

        if (action.equals("clear")) {
            rank.getInheritance().clear();
            plugin.getRankManager().saveRank(rank);
            refreshOnlineForRank(rank.getName());
            sender.sendMessage(CC.color("&aCleared inheritance from &f" + rank.getName() + "&a."));
            return true;
        }

        if (args.length < 4) return usage(sender, "/rank inherit <rank> <add|remove> <parent>");
        Rank parent = getRank(sender, args[3]);
        if (parent == null) return true;

        if (action.equals("add")) {
            if (rank.getName().equalsIgnoreCase(parent.getName()) || inheritsFrom(parent, rank.getName(), new HashSet<String>())) {
                sender.sendMessage(CC.color("&cThat inheritance would create a cycle."));
                return true;
            }
            rank.addInheritance(parent.getName());
            sender.sendMessage(CC.color("&aAdded inheritance &f" + parent.getName() + " &ato &f" + rank.getName() + "&a."));
        } else if (action.equals("remove")) {
            removeIgnoreCase(rank.getInheritance(), parent.getName());
            sender.sendMessage(CC.color("&aRemoved inheritance &f" + parent.getName() + " &afrom &f" + rank.getName() + "&a."));
        } else {
            sender.sendMessage(CC.color("&cUse add, remove, list, or clear."));
            return true;
        }

        plugin.getRankManager().saveRank(rank);
        refreshOnlineForRank(rank.getName());
        return true;
    }

    private boolean cloneRank(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.clone")) return true;
        if (args.length < 3) return usage(sender, "/rank clone <from> <new>");
        Rank source = getRank(sender, args[1]);
        if (source == null) return true;
        if (!isValidRankName(args[2])) {
            sender.sendMessage(CC.color("&cRank names can only use letters, numbers, dashes, and underscores."));
            return true;
        }
        if (plugin.getRankManager().rankExists(args[2])) {
            sender.sendMessage(CC.color("&cThat rank already exists."));
            return true;
        }

        Rank clone = new Rank(args[2]);
        clone.setDisplay(source.getDisplay());
        clone.setCategory(source.getCategory());
        clone.setPermission(rankPermission(clone.getName()));
        clone.setPrefix(source.getPrefix());
        clone.setSuffix(source.getSuffix());
        clone.setColor(source.getColor());
        clone.setWeight(source.getWeight());
        clone.setStaff(source.isStaff());
        clone.setHidden(source.isHidden());
        clone.setPermissions(new ArrayList<>(source.getPermissions()));
        if (!source.getPermission().isEmpty()) removeIgnoreCase(clone.getPermissions(), source.getPermission());
        if (!clone.getPermission().isEmpty()) clone.addPermission(clone.getPermission());
        clone.setInheritance(new ArrayList<>(source.getInheritance()));
        plugin.getRankManager().saveRank(clone);
        sender.sendMessage(CC.color("&aCloned &f" + source.getName() + " &ato &f" + clone.getName() + "&a."));
        return true;
    }

    private boolean setPlayerRank(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.set")) return true;
        if (args.length < 3) return usage(sender, "/rank set <player> <rank>");
        OfflinePlayer target = getTarget(sender, args[1]);
        Rank rank = getRank(sender, args[2]);
        if (target == null || rank == null) return true;

        PlayerProfile profile = loadProfile(target);
        String old = profile.getRankName();
        profile.setName(safeName(target, args[1]));
        profile.setRankName(rank.getName());
        saveProfile(profile);
        applyProfile(profile, true);

        if (plugin.getRedisSyncManager() != null) {
            plugin.getRedisSyncManager().publishRankChange(profile.getName(), profile.getUuid(), old, rank.getName(), sender.getName());
        }
        plugin.getDiscordManager().sendRankChange(profile.getName(), old, rank.getName(), sender.getName());
        sender.sendMessage(CC.color("&aSet &f" + profile.getName() + " &arank to " + rank.getDisplayName() + "&a."));
        return true;
    }

    private boolean addExtraRank(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.add")) return true;
        if (args.length < 3) return usage(sender, "/rank addextra <player> <rank>");
        OfflinePlayer target = getTarget(sender, args[1]);
        Rank rank = getRank(sender, args[2]);
        if (target == null || rank == null) return true;

        PlayerProfile profile = loadProfile(target);
        profile.setName(safeName(target, args[1]));
        profile.addExtraRank(rank.getName());
        saveProfile(profile);
        applyProfile(profile, true);
        if (plugin.getRedisSyncManager() != null) {
            plugin.getRedisSyncManager().publishRankChange(profile.getName(), profile.getUuid(), "extra", rank.getName(), sender.getName());
        }
        sender.sendMessage(CC.color("&aAdded extra rank " + rank.getDisplayName() + " &ato &f" + profile.getName() + "&a."));
        return true;
    }

    private boolean removeExtraRank(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.remove")) return true;
        if (args.length < 3) return usage(sender, "/rank removeextra <player> <rank>");
        OfflinePlayer target = getTarget(sender, args[1]);
        Rank rank = getRank(sender, args[2]);
        if (target == null || rank == null) return true;

        PlayerProfile profile = loadProfile(target);
        profile.setName(safeName(target, args[1]));
        removeIgnoreCase(profile.getExtraRanks(), rank.getName());
        saveProfile(profile);
        applyProfile(profile, true);
        if (plugin.getRedisSyncManager() != null) {
            plugin.getRedisSyncManager().publishRankChange(profile.getName(), profile.getUuid(), rank.getName(), "removed", sender.getName());
        }
        sender.sendMessage(CC.color("&aRemoved extra rank &f" + rank.getName() + " &afrom &f" + profile.getName() + "&a."));
        return true;
    }

    private boolean playerInfo(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.player")) return true;
        if (args.length < 2) return usage(sender, "/rank player <player>");
        OfflinePlayer target = getTarget(sender, args[1]);
        if (target == null) return true;
        PlayerProfile profile = loadProfile(target);

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cRanks for &f" + safeName(target, args[1])));
        sender.sendMessage(CC.color("&7Primary: &f" + profile.getRankName()));
        sender.sendMessage(CC.color("&7Extra: &f" + (profile.getExtraRanks().isEmpty() ? "none" : String.join(", ", profile.getExtraRanks()))));
        sender.sendMessage(CC.color("&7Player permissions: &f" + profile.getPermissions().size()));

        List<Grant> grants = plugin.getGrantManager().getGrants(profile.getUuid());
        int active = 0;
        for (Grant grant : grants) if (grant.isActive()) active++;
        sender.sendMessage(CC.color("&7Active grants: &f" + active + " &7Stored grants: &f" + grants.size()));
        for (Grant grant : grants) {
            if (!grant.isActive()) continue;
            sender.sendMessage(CC.color("  &8" + grant.getId() + " &f" + grant.getRankName()
                    + " &7" + grant.getDurationString()));
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean reloadRanks(CommandSender sender) {
        if (!can(sender, "evaulx.rank.reload")) return true;
        plugin.getRankManager().loadRanks();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (profile == null) continue;
            plugin.getPlayerManager().applyPermissions(player, profile);
            plugin.getNameTagManager().applyNameTag(player);
        }
        sender.sendMessage(CC.color("&aReloaded &f" + plugin.getRankManager().getRanks().size() + " &aranks."));
        return true;
    }

    private boolean installPresets(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.edit")) return true;
        if (args.length > 1 && args[1].equalsIgnoreCase("confirm")) {
            return installPresetsNow(sender);
        }
        if (sender instanceof Player) {
            plugin.getGuiManager().openRankPresetConfirm((Player) sender);
            return true;
        }
        return installPresetsNow(sender);
    }

    public boolean installPresetsNow(CommandSender sender) {
        if (!can(sender, "evaulx.rank.edit")) return true;
        int installed = plugin.getRankManager().installEvaulxPresetRanks();
        int cleanedOnline = sanitizeOnlineProfiles();
        int cleanedStored = cleanupStoredProfiles();
        int cleanedGrants = plugin.getGrantManager().cleanupInvalidRanks(plugin.getRankManager().getRankNameSet());
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (profile == null) continue;
            plugin.getPlayerManager().applyPermissions(player, profile);
            plugin.getNameTagManager().applyNameTag(player);
        }
        sender.sendMessage(CC.color("&aDeleted all ranks and installed only &f" + installed + " &aEvaulxMC ranks."));
        sender.sendMessage(CC.color("&7Cleanup: &f" + cleanedOnline + " &7online, &f" + cleanedStored
                + " &7stored profiles, &f" + cleanedGrants + " &7grant references."));
        plugin.getStaffRequestManager().logAction(sender.getName(), "RESET_RANKS", "Ranks", "Installed EvaulxMC presets only");
        return true;
    }

    private boolean cleanupRanks(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.cleanup")) return true;
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage(CC.color("&eThis removes deleted rank names from stored profiles and grant records."));
            sender.sendMessage(CC.color("&eRun &f/rank cleanup confirm &eto continue."));
            return true;
        }

        int cleanedOnline = sanitizeOnlineProfiles();
        int cleanedStored = cleanupStoredProfiles();
        int cleanedGrants = plugin.getGrantManager().cleanupInvalidRanks(plugin.getRankManager().getRankNameSet());
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (profile == null) continue;
            plugin.getPlayerManager().applyPermissions(player, profile);
            plugin.getNameTagManager().applyNameTag(player);
        }

        plugin.getStaffRequestManager().logAction(sender.getName(), "CLEANUP_RANKS", "Ranks",
                cleanedOnline + " online, " + cleanedStored + " stored profiles, " + cleanedGrants + " grants");
        sender.sendMessage(CC.color("&aRank cleanup complete. &7Online: &f" + cleanedOnline
                + " &7Stored profiles: &f" + cleanedStored + " &7Grant refs: &f" + cleanedGrants));
        return true;
    }

    private boolean importRanks(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.edit")) return true;
        if (args.length < 2) return usage(sender, "/rank import <file.yml>");

        File importsDir = new File(plugin.getDataFolder(), "imports");
        if (!importsDir.exists()) importsDir.mkdirs();
        File file = new File(importsDir, args[1]);
        if (!file.exists()) {
            sender.sendMessage(CC.color("&cImport file not found: &f" + file.getPath()));
            return true;
        }

        plugin.getRankManager().backupRanks("before-import");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int imported = 0;
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) continue;
            String name = section.getString("name", key);
            if (!isValidRankName(name)) continue;

            Rank rank = plugin.getRankManager().getRank(name);
            if (rank == null) rank = new Rank(name);
            rank.setName(name);
            rank.setDisplay(section.getString("display", rank.getDisplay()));
            String permission = section.getString("permission", rank.getPermission());
            if (rank.getPermission() != null && !rank.getPermission().isEmpty()) removeIgnoreCase(rank.getPermissions(), rank.getPermission());
            rank.setPermission(permission == null ? "" : permission);
            rank.setPrefix(section.getString("prefix", rank.getPrefix()));
            rank.setSuffix(section.getString("suffix", rank.getSuffix()));
            rank.setColor(colorFromRankColor(section.getString("name-color",
                    section.getString("rank-color", section.getString("color", rank.getColor())))));
            rank.setWeight(section.getInt("priority", section.getInt("weight", rank.getWeight())));
            rank.setStaff(section.getBoolean("staff", rank.isStaff()));
            rank.setDefault(section.getBoolean("default", rank.isDefault()) || name.equalsIgnoreCase("Default"));
            rank.setHidden(section.getBoolean("hidden", rank.isHidden()));
            String category = section.getString("category", "");
            rank.setCategory(category.isEmpty() ? Rank.inferCategory(rank) : category);
            if (!rank.getPermission().isEmpty()) rank.addPermission(rank.getPermission());
            for (String directPermission : section.getStringList("permissions")) rank.addPermission(directPermission);
            for (String inherited : section.getStringList("inheritance")) rank.addInheritance(inherited);
            plugin.getRankManager().saveRank(rank);
            imported++;
        }

        plugin.getStaffRequestManager().logAction(sender.getName(), "IMPORT_RANKS", file.getName(), imported + " ranks");
        sender.sendMessage(CC.color("&aImported/updated &f" + imported + " &aranks from &f" + file.getName() + "&a."));
        return true;
    }

    private boolean exportRanks(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.edit")) return true;
        File exportsDir = new File(plugin.getDataFolder(), "exports");
        if (!exportsDir.exists()) exportsDir.mkdirs();

        String fileName = args.length > 1 ? args[1] : "ranks-export.yml";
        fileName = fileName.replaceAll("[^A-Za-z0-9_.-]", "");
        if (fileName.isEmpty()) fileName = "ranks-export.yml";
        if (!fileName.endsWith(".yml") && !fileName.endsWith(".yaml")) fileName += ".yml";

        File file = new File(exportsDir, fileName);
        YamlConfiguration yaml = new YamlConfiguration();
        for (Rank rank : plugin.getRankManager().getRanksByWeight()) {
            String base = rank.getName();
            yaml.set(base + ".name", rank.getName());
            yaml.set(base + ".display", rank.getDisplay());
            yaml.set(base + ".category", rank.getCategory());
            yaml.set(base + ".permission", rank.getPermission());
            yaml.set(base + ".prefix", rank.getPrefix());
            yaml.set(base + ".suffix", rank.getSuffix());
            yaml.set(base + ".color", rank.getColor());
            yaml.set(base + ".weight", rank.getWeight());
            yaml.set(base + ".priority", rank.getWeight());
            yaml.set(base + ".default", rank.isDefault());
            yaml.set(base + ".staff", rank.isStaff());
            yaml.set(base + ".hidden", rank.isHidden());
            yaml.set(base + ".permissions", rank.getPermissions());
            yaml.set(base + ".inheritance", rank.getInheritance());
        }

        try {
            yaml.save(file);
        } catch (Exception e) {
            sender.sendMessage(CC.color("&cRank export failed: &f" + e.getMessage()));
            return true;
        }

        plugin.getStaffRequestManager().logAction(sender.getName(), "EXPORT_RANKS", file.getName(),
                plugin.getRankManager().getRanks().size() + " ranks");
        sender.sendMessage(CC.color("&aExported &f" + plugin.getRankManager().getRanks().size()
                + " &aranks to &f" + file.getPath() + "&a."));
        return true;
    }

    private boolean createBackup(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.edit")) return true;
        String reason = args.length > 1 ? args[1].replaceAll("[^A-Za-z0-9_-]", "") : "manual";
        if (reason.isEmpty()) reason = "manual";
        File file = plugin.getRankManager().backupRanks(reason);
        sender.sendMessage(CC.color("&aCreated rank backup: &f" + file.getName()));
        return true;
    }

    private boolean listBackups(CommandSender sender) {
        if (!can(sender, "evaulx.rank.edit")) return true;
        List<File> backups = plugin.getRankManager().listBackups();
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cRank Backups"));
        if (backups.isEmpty()) {
            sender.sendMessage(CC.color("&7No rank backups exist yet."));
        } else {
            int shown = 0;
            for (File backup : backups) {
                if (shown++ >= 10) break;
                sender.sendMessage(CC.color("&f" + backup.getName() + " &7(" + (backup.length() / 1024L) + " KB)"));
            }
            sender.sendMessage(CC.color("&7Restore with &f/rank rollback <file> confirm&7."));
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private boolean rollbackRanks(CommandSender sender, String[] args) {
        if (!can(sender, "evaulx.rank.edit")) return true;
        if (args.length < 2) return usage(sender, "/rank rollback <backup-file> confirm");
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            sender.sendMessage(CC.color("&eThis replaces every current rank from a backup."));
            sender.sendMessage(CC.color("&eRun &f/rank rollback " + args[1] + " confirm &eto continue."));
            return true;
        }

        int restored = plugin.getRankManager().restoreBackup(args[1]);
        if (restored < 0) {
            sender.sendMessage(CC.color("&cBackup not found or could not be restored."));
            return true;
        }
        sanitizeOnlineProfiles();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (profile == null) continue;
            plugin.getPlayerManager().applyPermissions(player, profile);
            plugin.getNameTagManager().applyNameTag(player);
        }
        plugin.getStaffRequestManager().logAction(sender.getName(), "ROLLBACK_RANKS", args[1], restored + " ranks restored");
        sender.sendMessage(CC.color("&aRestored &f" + restored + " &aranks from &f" + args[1] + "&a."));
        return true;
    }

    private String colorFromRankColor(String color) {
        if (color == null) return "&f";
        String normalized = color.toUpperCase(Locale.ENGLISH).replace(' ', '_').replace('-', '_');
        if (normalized.startsWith("&")) return normalized;
        if (normalized.equals("DARK_RED")) return "&4";
        if (normalized.equals("RED")) return "&c";
        if (normalized.equals("LIGHT_PURPLE")) return "&d";
        if (normalized.equals("DARK_PURPLE")) return "&5";
        if (normalized.equals("BLUE")) return "&9";
        if (normalized.equals("AQUA")) return "&b";
        if (normalized.equals("GOLD")) return "&6";
        if (normalized.equals("GREEN")) return "&a";
        if (normalized.equals("GREY") || normalized.equals("GRAY")) return "&7";
        if (normalized.equals("DARK_GRAY") || normalized.equals("DARK_GREY")) return "&8";
        if (normalized.equals("WHITE")) return "&f";
        return color;
    }

    private String rankCategory(Rank rank) {
        return plugin.getRankManager().getRankCategory(rank);
    }

    private String rankPermission(String name) {
        if (name == null || name.equalsIgnoreCase("Default")) return "";
        return "evaulxmc.rank." + name.toLowerCase(Locale.ENGLISH).replace("_", "-");
    }

    private int rankPosition(Rank rank) {
        List<Rank> ranks = plugin.getRankManager().getRanksByWeight();
        for (int i = 0; i < ranks.size(); i++) {
            if (ranks.get(i).getName().equalsIgnoreCase(rank.getName())) return i + 1;
        }
        return ranks.size();
    }

    private List<String> inheritedPermissionPreview(Rank rank, List<String> allPermissions) {
        List<String> inherited = new ArrayList<>();
        for (String permission : allPermissions) {
            if (!containsIgnoreCase(rank.getPermissions(), permission)) inherited.add(permission);
        }
        return inherited;
    }

    private int sanitizeOnlineProfiles() {
        Rank defaultRank = plugin.getRankManager().getDefaultRank();
        int changedProfiles = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (profile == null) continue;

            boolean changed = false;
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

            if (changed) {
                plugin.getPlayerManager().saveProfile(profile);
                changedProfiles++;
            }
        }
        return changedProfiles;
    }

    private int cleanupStoredProfiles() {
        Rank defaultRank = plugin.getRankManager().getDefaultRank();
        if (defaultRank == null) return 0;
        return plugin.getDatabaseManager().cleanupProfiles(plugin.getRankManager().getRankNameSet(), defaultRank.getName());
    }

    private Rank getRank(CommandSender sender, String name) {
        Rank rank = plugin.getRankManager().getRank(name);
        if (rank == null) sender.sendMessage(CC.color("&cRank not found."));
        return rank;
    }

    private OfflinePlayer getTarget(CommandSender sender, String name) {
        OfflinePlayer target = plugin.getPlayerLookupManager().find(name);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return null;
        }
        return target;
    }

    private PlayerProfile loadProfile(OfflinePlayer target) {
        Player online = target.getPlayer();
        if (online != null) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(online);
            if (profile != null) return profile;
        }

        PlayerProfile loaded = plugin.getPlayerManager().getProfile(target.getUniqueId());
        if (loaded != null) return loaded;
        return plugin.getDatabaseManager().loadProfile(target.getUniqueId(), safeName(target, "Unknown"));
    }

    private void saveProfile(PlayerProfile profile) {
        if (plugin.getPlayerManager().isLoaded(profile.getUuid())) plugin.getPlayerManager().saveProfile(profile);
        else plugin.getDatabaseManager().saveProfile(profile);
    }

    private void applyProfile(PlayerProfile profile, boolean notify) {
        Player player = Bukkit.getPlayer(profile.getUuid());
        if (player == null) return;
        plugin.getPlayerManager().applyPermissions(player, profile);
        plugin.getNameTagManager().applyNameTag(player);
        if (notify) player.sendMessage(CC.color("&7Your ranks were updated."));
    }

    private void refreshOnlineForRank(String rankName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (profile == null || !profileUsesRank(profile, rankName)) continue;
            plugin.getPlayerManager().applyPermissions(player, profile);
            plugin.getNameTagManager().applyNameTag(player);
        }
    }

    private void repairOnlineProfiles(String deletedRank) {
        Rank defaultRank = plugin.getRankManager().getDefaultRank();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (profile == null) continue;
            boolean changed = false;
            if (profile.getRankName().equalsIgnoreCase(deletedRank) && defaultRank != null) {
                profile.setRankName(defaultRank.getName());
                changed = true;
            }
            if (removeIgnoreCase(profile.getExtraRanks(), deletedRank)) changed = true;
            if (changed) {
                saveProfile(profile);
                applyProfile(profile, true);
            }
        }
    }

    private boolean profileUsesRank(PlayerProfile profile, String rankName) {
        if (profile.getRankName().equalsIgnoreCase(rankName)) return true;
        for (String extra : profile.getExtraRanks()) if (extra.equalsIgnoreCase(rankName)) return true;
        return false;
    }

    private boolean inheritsFrom(Rank start, String targetName, Set<String> visited) {
        if (start == null) return false;
        String key = start.getName().toLowerCase(Locale.ENGLISH);
        if (!visited.add(key)) return false;
        for (String inherited : start.getInheritance()) {
            if (inherited.equalsIgnoreCase(targetName)) return true;
            if (inheritsFrom(plugin.getRankManager().getRank(inherited), targetName, visited)) return true;
        }
        return false;
    }

    private boolean removeIgnoreCase(List<String> values, String value) {
        boolean removed = false;
        Iterator<String> iterator = values.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().equalsIgnoreCase(value)) continue;
            iterator.remove();
            removed = true;
        }
        return removed;
    }

    private boolean containsIgnoreCase(List<String> values, String value) {
        for (String current : values) {
            if (current.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    private void applyCategory(Rank rank, String category) {
        String normalized = Rank.normalizeCategory(category);
        rank.setCategory(normalized);
        rank.setHidden(normalized.equals("Hidden"));
        rank.setStaff(normalized.equals("Staff"));
    }

    private boolean isCategory(String value) {
        return Rank.isAllowedCategory(value);
    }

    private boolean can(CommandSender sender, String permission) {
        if (sender.hasPermission("evaulx.rank") || sender.hasPermission(permission)) return true;
        sender.sendMessage(CC.color("&cNo permission."));
        return false;
    }

    private boolean usage(CommandSender sender, String usage) {
        sender.sendMessage(CC.color("&cUsage: " + usage));
        return true;
    }

    private boolean isValidRankName(String name) {
        return name != null && name.matches("[A-Za-z0-9_-]{1,32}");
    }

    private int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Boolean parseBoolean(String input) {
        if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("on")) return true;
        if (input.equalsIgnoreCase("false") || input.equalsIgnoreCase("no") || input.equalsIgnoreCase("off")) return false;
        return null;
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private String printable(String value) {
        return value == null || value.isEmpty() ? "none" : value;
    }

    private String safeName(OfflinePlayer target, String fallback) {
        return target.getName() == null ? fallback : target.getName();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String[] routed = route(command.getName(), args);
        if (command.getName().equalsIgnoreCase("rank") && routed.length == 1) {
            return match(routed[0], SUB_COMMANDS);
        }
        if (routed.length == 0) return Collections.emptyList();

        String sub = routed[0].toLowerCase(Locale.ENGLISH);
        if (routed.length == 2) {
            if (sub.equals("set") || sub.equals("addextra") || sub.equals("removeextra") || sub.equals("player")) {
                return match(routed[1], onlineNames());
            }
            if (sub.equals("list")) return match(routed[1], Rank.getAllowedCategories());
            if (sub.equals("cleanup")) return match(routed[1], Collections.singletonList("confirm"));
            if (sub.equals("rollback")) return match(routed[1], backupNames());
            if (expectsRankSecond(sub)) return match(routed[1], rankNames());
        }
        if (routed.length == 3) {
            if (sub.equals("set") || sub.equals("addextra") || sub.equals("removeextra")) return match(routed[2], rankNames());
            if (sub.equals("delete") || sub.equals("remove") || sub.equals("rollback") || sub.equals("cleanup")) return match(routed[2], Collections.singletonList("confirm"));
            if (sub.equals("display")) return match(routed[2], Arrays.asList("none", "&cAdmin", "&4Owner", "&8[&cRank&8]"));
            if (sub.equals("permission")) return match(routed[2], Arrays.asList("none", rankPermission(routed[1])));
            if (sub.equals("category")) return match(routed[2], Rank.getAllowedCategories());
            if (sub.equals("perm") || sub.equals("permission") || sub.equals("inherit") || sub.equals("inheritance")) {
                return match(routed[2], Arrays.asList("add", "remove", "list", "clear"));
            }
            if (sub.equals("staff") || sub.equals("hidden")) return match(routed[2], Arrays.asList("true", "false"));
        }
        if (routed.length == 4 && (sub.equals("inherit") || sub.equals("inheritance"))) return match(routed[3], rankNames());
        return Collections.emptyList();
    }

    private boolean expectsRankSecond(String sub) {
        return sub.equals("info") || sub.equals("delete") || sub.equals("remove") || sub.equals("prefix")
                || sub.equals("suffix") || sub.equals("color") || sub.equals("namecolor") || sub.equals("display") || sub.equals("permission")
                || sub.equals("weight") || sub.equals("default")
                || sub.equals("staff") || sub.equals("hidden") || sub.equals("category") || sub.equals("perm") || sub.equals("permission") || sub.equals("inherit")
                || sub.equals("inheritance") || sub.equals("clone") || sub.equals("copy");
    }

    private List<String> rankNames() {
        List<String> names = new ArrayList<>();
        for (Rank rank : plugin.getRankManager().getRanks()) names.add(rank.getName());
        return names;
    }

    private List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
        names.addAll(plugin.getPlayerLookupManager().suggest("", 20));
        return names;
    }

    private List<String> backupNames() {
        List<String> names = new ArrayList<>();
        for (File file : plugin.getRankManager().listBackups()) names.add(file.getName());
        return names;
    }

    private List<String> match(String input, List<String> options) {
        String lowered = input.toLowerCase(Locale.ENGLISH);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ENGLISH).startsWith(lowered)) matches.add(option);
        }
        return matches;
    }
}
