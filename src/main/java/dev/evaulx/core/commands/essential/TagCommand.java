package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.DisplayUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class TagCommand implements CommandExecutor, TabCompleter {

    private static final int TAGS_PER_PAGE = 8;
    private static final Random RANDOM = new Random();

    private final EvaulxCore plugin;

    public TagCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.tag")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            sendTags(sender, 1, null);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ENGLISH);
        if (sub.equals("list") || sub.equals("tags")) {
            int page = args.length > 1 ? parsePage(args[1]) : 1;
            sendTags(sender, page, null);
            return true;
        }

        if (sub.equals("menu") || sub.equals("gui")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(CC.color("&cOnly players can open the tag menu."));
                return true;
            }
            plugin.getGuiManager().openTagMenu((Player) sender, 1);
            return true;
        }

        if (sub.equals("stats")) {
            sendStats(sender);
            return true;
        }

        if (sub.equals("search")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /tag search <text>"));
                return true;
            }
            sendTags(sender, 1, join(args, 1));
            return true;
        }

        if (sub.equals("info")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /tag info <tag>"));
                return true;
            }
            TagDefinition tag = findConfiguredTag(args[1]);
            if (tag == null) {
                sender.sendMessage(CC.color("&cTag not found. Use /tag list."));
                return true;
            }
            sendTagInfo(sender, tag);
            return true;
        }

        if (sub.equals("preview")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /tag preview <tag>"));
                return true;
            }
            String tag = resolveTag(sender, args[1]);
            if (tag == null) return true;
            String name = sender instanceof Player ? plugin.getDisguiseManager().getVisibleName((Player) sender) : sender.getName();
            sender.sendMessage(CC.color("&7Preview: " + tag + " &f" + name + "&7: &fHello!"));
            return true;
        }

        if (sub.equals("current")) {
            Player target = resolveTarget(sender, args.length > 1 ? args[1] : null, "/tag current <player>");
            if (target == null) return true;
            sendCurrent(sender, target);
            return true;
        }

        if (sub.equals("random")) {
            Player target = resolveTarget(sender, args.length > 1 ? args[1] : null, "/tag random <player>");
            if (target == null) return true;
            setRandomTag(sender, target);
            return true;
        }

        if (sub.equals("clear") || sub.equals("reset") || sub.equals("none")) {
            Player target = resolveTarget(sender, args.length > 1 ? args[1] : null, "/tag clear <player>");
            if (target == null) return true;
            setTag(sender, target, "");
            return true;
        }

        if (sub.equals("set")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /tag set <tag> [player]"));
                return true;
            }
            Player target = resolveTarget(sender, args.length > 2 ? args[2] : null, "/tag set <tag> <player>");
            if (target == null) return true;
            String tag = resolveTag(sender, args[1]);
            if (tag == null) return true;
            setTag(sender, target, tag);
            return true;
        }

        Player target = resolveTarget(sender, args.length > 1 ? args[1] : null, "/tag <tag> <player>");
        if (target == null) return true;
        String tag = resolveTag(sender, args[0]);
        if (tag == null) return true;
        setTag(sender, target, tag);
        return true;
    }

    private void sendTags(CommandSender sender, int page, String search) {
        List<TagDefinition> tags = loadTags();
        List<TagDefinition> visible = new ArrayList<>();
        boolean showLocked = plugin.getConfig().getBoolean("tags.show-locked", true);
        String normalizedSearch = search == null ? "" : normalize(search);

        for (TagDefinition tag : tags) {
            if (!normalizedSearch.isEmpty()
                    && !normalize(tag.id + " " + tag.display + " " + tag.category + " " + tag.description).contains(normalizedSearch)) {
                continue;
            }
            if (showLocked || canUse(sender, tag)) {
                visible.add(tag);
            }
        }

        int pages = Math.max(1, (int) Math.ceil(visible.size() / (double) TAGS_PER_PAGE));
        page = Math.max(1, Math.min(page, pages));
        int from = Math.min((page - 1) * TAGS_PER_PAGE, visible.size());
        int to = Math.min(from + TAGS_PER_PAGE, visible.size());

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cAvailable Tags &7(Page " + page + "/" + pages + ")"));
        sender.sendMessage(CC.color("&7Unlocked: &f" + unlockedCount(sender, tags) + "&8/&f" + tags.size()
                + " &7Current: " + currentTag(sender)));
        if (visible.isEmpty()) {
            sender.sendMessage(CC.color(search == null ? "&7No configured tags." : "&7No tags matched your search."));
        } else {
            for (TagDefinition tag : visible.subList(from, to)) {
                String status = canUse(sender, tag) ? "&aUnlocked" : "&cLocked";
                String category = tag.category.isEmpty() ? "" : " &8[" + tag.category + "]";
                String rarity = tag.rarity.isEmpty() ? "" : " &7" + tag.rarity;
                sender.sendMessage(CC.color(" &8- " + tag.display + " &7/" + tag.id + category + rarity + " " + status));
            }
        }
        sender.sendMessage(CC.color("&7Use &f/tag menu&7, &f/tag set <tag>&7, &f/tag preview <tag>&7, &f/tag info <tag>&7, or &f/tag random&7."));
        if (pages > page) sender.sendMessage(CC.color("&7Next page: &f/tag list " + (page + 1)));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void sendTagInfo(CommandSender sender, TagDefinition tag) {
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cTag Info &8- " + tag.display));
        sender.sendMessage(CC.color("&7ID: &f" + tag.id));
        sender.sendMessage(CC.color("&7Plain: &f" + DisplayUtil.stripFormat(tag.display)));
        if (!tag.category.isEmpty()) sender.sendMessage(CC.color("&7Category: &f" + tag.category));
        if (!tag.rarity.isEmpty()) sender.sendMessage(CC.color("&7Rarity: &f" + tag.rarity));
        if (!tag.description.isEmpty()) sender.sendMessage(CC.color("&7Description: &f" + tag.description));
        sender.sendMessage(CC.color("&7Permission: &f" + (tag.permission.isEmpty() ? "public" : tag.permission)));
        if (!tag.ranks.isEmpty()) sender.sendMessage(CC.color("&7Rank unlocks: &f" + join(tag.ranks)));
        sender.sendMessage(CC.color("&7Status: " + (canUse(sender, tag) ? "&aUnlocked" : "&cLocked")));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void sendStats(CommandSender sender) {
        List<TagDefinition> tags = loadTags();
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cTag Collection"));
        sender.sendMessage(CC.color("&7Unlocked: &f" + unlockedCount(sender, tags) + "&8/&f" + tags.size()));
        sender.sendMessage(CC.color("&7Current: " + currentTag(sender)));

        List<String> categories = new ArrayList<>();
        for (TagDefinition tag : tags) {
            if (!tag.category.isEmpty() && !categories.contains(tag.category)) categories.add(tag.category);
        }
        Collections.sort(categories);
        sender.sendMessage(CC.color("&7Categories: &f" + (categories.isEmpty() ? "None" : join(categories))));
        sender.sendMessage(CC.color("&7Open the picker with &f/tag menu&7."));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void sendCurrent(CommandSender sender, Player target) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(target);
        if (profile == null) {
            sender.sendMessage(CC.color("&cProfile not loaded."));
            return;
        }

        String tag = profile.getTag();
        sender.sendMessage(CC.color(tag == null || tag.isEmpty()
                ? "&7" + target.getName() + " has no active tag."
                : "&7" + target.getName() + "'s active tag is " + tag + "&7."));
    }

    private void setRandomTag(CommandSender sender, Player target) {
        List<TagDefinition> unlocked = new ArrayList<>();
        for (TagDefinition tag : loadTags()) {
            if (canUse(sender, tag)) unlocked.add(tag);
        }

        if (unlocked.isEmpty()) {
            sender.sendMessage(CC.color("&cYou do not have any unlocked tags."));
            return;
        }

        TagDefinition picked = unlocked.get(RANDOM.nextInt(unlocked.size()));
        setTag(sender, target, picked.display);
    }

    private String resolveTag(CommandSender sender, String input) {
        if (input.equalsIgnoreCase("none") || input.equalsIgnoreCase("clear") || input.equalsIgnoreCase("reset")) return "";

        TagDefinition configured = findConfiguredTag(input);
        if (configured != null) {
            if (!canUse(sender, configured)) {
                String permission = configured.permission.isEmpty() ? "evaulx.tag.all" : configured.permission;
                sender.sendMessage(CC.color("&cThat tag is locked. Required permission: &f" + permission));
                return null;
            }
            return configured.display;
        }

        if (sender.hasPermission("evaulx.tag.custom")) {
            return sanitizeCustomTag(sender, input);
        }

        sender.sendMessage(CC.color("&cTag not found. Use /tag list."));
        return null;
    }

    private String sanitizeCustomTag(CommandSender sender, String input) {
        String tag = input.replace("_", " ").trim();
        if (tag.contains("\n") || tag.contains("\r")) {
            sender.sendMessage(CC.color("&cCustom tags cannot contain line breaks."));
            return null;
        }

        int maxRaw = plugin.getConfig().getInt("tags.custom-max-raw-length", 48);
        int maxVisible = plugin.getConfig().getInt("tags.custom-max-visible-length", 16);
        if (tag.length() > maxRaw) {
            sender.sendMessage(CC.color("&cCustom tags can only be " + maxRaw + " characters before color formatting."));
            return null;
        }

        if (DisplayUtil.stripFormat(tag).length() > maxVisible) {
            sender.sendMessage(CC.color("&cCustom tags can only be " + maxVisible + " visible characters."));
            return null;
        }

        return tag;
    }

    private void setTag(CommandSender sender, Player target, String tag) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(target);
        if (profile == null) {
            sender.sendMessage(CC.color("&cProfile not loaded."));
            return;
        }

        profile.setTag(tag);
        plugin.getPlayerManager().saveProfile(profile);
        plugin.getNameTagManager().applyNameTag(target);
        target.sendMessage(CC.color(tag.isEmpty() ? "&7Your tag was cleared." : "&7Your tag is now " + tag + "&7."));
        if (!target.equals(sender)) {
            sender.sendMessage(CC.color("&aUpdated &f" + target.getName() + "&a's tag."));
        }
    }

    private Player resolveTarget(CommandSender sender, String targetName, String consoleUsage) {
        if (targetName != null) {
            if (!sender.hasPermission("evaulx.tag.others")) {
                sender.sendMessage(CC.color("&cNo permission to edit other players."));
                return null;
            }
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) sender.sendMessage(CC.color("&cPlayer must be online."));
            return target;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cUsage: " + consoleUsage));
            return null;
        }
        return (Player) sender;
    }

    private List<TagDefinition> loadTags() {
        List<TagDefinition> tags = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        ConfigurationSection catalog = plugin.getConfig().getConfigurationSection("tags.catalog");
        if (catalog != null) {
            for (String key : catalog.getKeys(false)) {
                TagDefinition tag = readCatalogTag(catalog, key);
                if (tag != null && seen.add(normalize(tag.id))) tags.add(tag);
            }
        }

        for (String display : plugin.getConfig().getStringList("tags.available")) {
            if (display == null || display.trim().isEmpty()) continue;
            String id = normalize(display);
            if (id.isEmpty() || !seen.add(id)) continue;
            tags.add(new TagDefinition(id, display, "Available", "Public", "", "", Collections.<String>emptyList(), 1000 + tags.size()));
        }

        Collections.sort(tags, Comparator.comparingInt(tag -> tag.order));
        return tags;
    }

    private TagDefinition readCatalogTag(ConfigurationSection catalog, String key) {
        String path = "tags.catalog." + key;
        String id = normalize(key);
        if (id.isEmpty()) return null;

        if (catalog.isString(key)) {
            String display = catalog.getString(key, key);
            return new TagDefinition(id, display, "Available", "Common", "", "", Collections.<String>emptyList(), 0);
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) return null;

        String display = section.getString("display", key);
        if (display == null || display.trim().isEmpty()) return null;
        String category = section.getString("category", "Available");
        String rarity = section.getString("rarity", "Common");
        String description = section.getString("description", "");
        String permission = section.getString("permission", "");
        List<String> ranks = section.getStringList("ranks");
        int order = section.getInt("order", 500);
        return new TagDefinition(id, display, category, rarity, description, permission, ranks, order);
    }

    private TagDefinition findConfiguredTag(String input) {
        String normalized = normalize(input);
        for (TagDefinition tag : loadTags()) {
            if (tag.id.equalsIgnoreCase(input)
                    || normalize(tag.id).equals(normalized)
                    || normalize(tag.display).equals(normalized)
                    || normalize(DisplayUtil.stripFormat(tag.display)).equals(normalized)) {
                return tag;
            }
        }
        return null;
    }

    private boolean canUse(CommandSender sender, TagDefinition tag) {
        if (sender.hasPermission("evaulx.tag.all")) return true;
        if (!plugin.getConfig().getBoolean("tags.require-per-tag-permission", true)) return true;
        if (!tag.permission.isEmpty() && sender.hasPermission(tag.permission)) return true;
        if (sender instanceof Player && hasUnlockRank((Player) sender, tag)) return true;
        return tag.permission.isEmpty() && tag.ranks.isEmpty();
    }

    private boolean hasUnlockRank(Player player, TagDefinition tag) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null) return false;
        for (String rank : tag.ranks) {
            if (profile.getRankName().equalsIgnoreCase(rank)) return true;
            for (String extra : profile.getExtraRanks()) {
                if (extra.equalsIgnoreCase(rank)) return true;
            }
        }
        return false;
    }

    private int parsePage(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private String normalize(String input) {
        return DisplayUtil.stripFormat(input)
                .toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9]+", "");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.tag")) return Collections.emptyList();

        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            Collections.addAll(suggestions, "menu", "list", "search", "stats", "set", "clear", "current", "preview", "info", "random");
            for (TagDefinition tag : loadTags()) {
                if (canUse(sender, tag)) suggestions.add(tag.id);
            }
            return filter(suggestions, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ENGLISH);
        if (args.length == 2 && (sub.equals("set") || sub.equals("preview") || sub.equals("info"))) {
            for (TagDefinition tag : loadTags()) {
                if (sub.equals("info") || canUse(sender, tag)) suggestions.add(tag.id);
            }
            suggestions.add("clear");
            suggestions.add("reset");
            return filter(suggestions, args[1]);
        }

        if (args.length == 2 && (sub.equals("clear") || sub.equals("current") || sub.equals("random"))) {
            return sender.hasPermission("evaulx.tag.others") ? playerSuggestions(args[1]) : Collections.emptyList();
        }

        if (args.length == 2 && !isSubCommand(sub)) {
            return sender.hasPermission("evaulx.tag.others") ? playerSuggestions(args[1]) : Collections.emptyList();
        }

        if (args.length == 3 && sub.equals("set")) {
            return sender.hasPermission("evaulx.tag.others") ? playerSuggestions(args[2]) : Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private boolean isSubCommand(String input) {
        return input.equals("list") || input.equals("tags") || input.equals("search") || input.equals("set")
                || input.equals("menu") || input.equals("gui") || input.equals("stats")
                || input.equals("clear") || input.equals("reset") || input.equals("none")
                || input.equals("current") || input.equals("preview") || input.equals("info") || input.equals("random");
    }

    private int unlockedCount(CommandSender sender, List<TagDefinition> tags) {
        int count = 0;
        for (TagDefinition tag : tags) {
            if (canUse(sender, tag)) count++;
        }
        return count;
    }

    private String currentTag(CommandSender sender) {
        if (!(sender instanceof Player)) return "&7Console";
        PlayerProfile profile = plugin.getPlayerManager().getProfile((Player) sender);
        if (profile == null || profile.getTag() == null || profile.getTag().isEmpty()) return "&7None";
        return profile.getTag();
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(value);
        }
        return builder.toString();
    }

    private List<String> playerSuggestions(String input) {
        List<String> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.add(player.getName());
        }
        return filter(players, input);
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

    private static class TagDefinition {
        private final String id;
        private final String display;
        private final String category;
        private final String rarity;
        private final String description;
        private final String permission;
        private final List<String> ranks;
        private final int order;

        private TagDefinition(String id, String display, String category, String rarity, String description, String permission, List<String> ranks, int order) {
            this.id = id;
            this.display = display;
            this.category = category == null ? "" : category;
            this.rarity = rarity == null ? "" : rarity;
            this.description = description == null ? "" : description;
            this.permission = permission == null ? "" : permission;
            this.ranks = ranks == null ? Collections.<String>emptyList() : ranks;
            this.order = order;
        }
    }
}
