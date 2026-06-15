package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.creator.ContentCreatorManager;
import dev.evaulx.core.creator.ContentCreatorProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class ContentCreatorCommand implements CommandExecutor, TabCompleter {

    private static final String MANAGE_PERM = "evaulx.creator.manage";
    private static final String SELF_PERM   = "evaulx.creator.self";
    private static final Set<String> EDITABLE_FIELDS = new LinkedHashSet<>(
            Arrays.asList("youtube", "twitch", "tiktok", "twitter", "instagram", "discord", "displayname", "description", "code", "streamtitle"));

    private final EvaulxCore plugin;

    public ContentCreatorCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ENGLISH);

        switch (sub) {
            case "list":
                handleList(sender, args);
                break;
            case "profile":
                handleProfile(sender, args);
                break;
            case "grant":
                handleGrant(sender, args);
                break;
            case "revoke":
                handleRevoke(sender, args);
                break;
            case "set":
                handleSet(sender, args);
                break;
            case "setfield":
                handleSetField(sender, args);
                break;
            case "announce":
                handleAnnounce(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "stats":
                handleStats(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    // ── Subcommand handlers ───────────────────────────────────────────────────

    private void handleList(CommandSender sender, String[] args) {
        ContentCreatorManager mgr = plugin.getContentCreatorManager();
        List<ContentCreatorProfile> all = new ArrayList<>(mgr.getAllProfiles());
        if (all.isEmpty()) {
            sender.sendMessage(CC.color("&cNo content creators are registered yet."));
            return;
        }
        int page = 1;
        if (args.length >= 2) {
            try { page = Math.max(1, Integer.parseInt(args[1])); } catch (NumberFormatException ignored) {}
        }
        int perPage = 8;
        int totalPages = (int) Math.ceil(all.size() / (double) perPage);
        page = Math.min(page, totalPages);
        int start = (page - 1) * perPage;

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("  &6&l✦ Content Creators &8» &7Page " + page + "/" + totalPages));
        sender.sendMessage(CC.color(CC.SEPARATOR));
        for (int i = start; i < Math.min(start + perPage, all.size()); i++) {
            ContentCreatorProfile p = all.get(i);
            String platforms = buildPlatformLine(p);
            sender.sendMessage(CC.color("  &6" + p.effectiveDisplayName()
                    + (p.getName().isEmpty() ? "" : " &8(" + p.getName() + ")")
                    + (platforms.isEmpty() ? "" : " &8- " + platforms)));
        }
        if (totalPages > 1)
            sender.sendMessage(CC.color("  &7Use &b/creator list " + (page + 1) + " &7for next page."));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void handleProfile(CommandSender sender, String[] args) {
        ContentCreatorManager mgr = plugin.getContentCreatorManager();
        ContentCreatorProfile profile;
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                profile = mgr.getProfile(target.getUniqueId());
            } else {
                @SuppressWarnings("deprecation")
                OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                profile = offline != null ? mgr.getProfile(offline.getUniqueId()) : null;
            }
            if (profile == null) {
                sender.sendMessage(CC.color("&c" + args[1] + " is not a registered content creator."));
                return;
            }
        } else if (sender instanceof Player && mgr.isCreator(((Player) sender).getUniqueId())) {
            Player player = (Player) sender;
            profile = mgr.getProfile(player.getUniqueId());
        } else {
            sender.sendMessage(CC.color("&cUsage: /creator profile <player>"));
            return;
        }
        printProfile(sender, profile);
    }

    private void handleGrant(CommandSender sender, String[] args) {
        if (!sender.hasPermission(MANAGE_PERM)) {
            sender.sendMessage(CC.color("&cYou don't have permission to do that."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /creator grant <player>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(CC.color("&c" + args[1] + " is not online."));
            return;
        }
        ContentCreatorProfile p = plugin.getContentCreatorManager().grant(target.getUniqueId(), target.getName());
        sender.sendMessage(CC.color("&a✔ Granted content creator status to &e" + target.getName() + "&a."));
        target.sendMessage(CC.color(" "));
        target.sendMessage(CC.color("&8&m----------------------------------------"));
        target.sendMessage(CC.color(" "));
        target.sendMessage(CC.color("   &6&l✦ Welcome, Content Creator!"));
        target.sendMessage(CC.color(" "));
        target.sendMessage(CC.color("   &7You've been granted &6Content Creator &7status!"));
        target.sendMessage(CC.color("   &7You now have exclusive server perks and visibility."));
        target.sendMessage(CC.color(" "));
        target.sendMessage(CC.color("   &b/creator set <field> <value> &7— Set your profile"));
        target.sendMessage(CC.color("   &b/golive &7— Announce when you go live"));
        target.sendMessage(CC.color("   &b/shoutout <player> &7— Shoutout a viewer"));
        target.sendMessage(CC.color("   &b/ccgiveaway &7— Run a rank giveaway"));
        target.sendMessage(CC.color("   &b/creator profile &7— View your CC profile"));
        target.sendMessage(CC.color(" "));
        target.sendMessage(CC.color("&8&m----------------------------------------"));
        target.sendMessage(CC.color(" "));
        target.sendTitle(CC.color("&6&l✦ Content Creator"), CC.color("&7Welcome to the program!"));
        if (p.getRewardCode().isEmpty())
            sender.sendMessage(CC.color("  &7Tip: &b/creator setfield " + target.getName() + " code <code> &7to give them a subscriber code."));
    }

    private void handleRevoke(CommandSender sender, String[] args) {
        if (!sender.hasPermission(MANAGE_PERM)) {
            sender.sendMessage(CC.color("&cYou don't have permission to do that."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /creator revoke <player>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        UUID uuid;
        String name;
        if (target != null) {
            uuid = target.getUniqueId();
            name = target.getName();
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
            if (offline == null) {
                sender.sendMessage(CC.color("&cPlayer not found."));
                return;
            }
            uuid = offline.getUniqueId();
            name = offline.getName() != null ? offline.getName() : args[1];
        }
        if (!plugin.getContentCreatorManager().isCreator(uuid)) {
            sender.sendMessage(CC.color("&c" + name + " is not a content creator."));
            return;
        }
        plugin.getContentCreatorManager().revoke(uuid);
        sender.sendMessage(CC.color("&a✔ Revoked content creator status from &e" + name + "&a."));
        if (target != null)
            target.sendMessage(CC.color("&cYour content creator status has been removed."));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can use this."));
            return;
        }
        Player player = (Player) sender;
        ContentCreatorManager mgr = plugin.getContentCreatorManager();
        boolean isCreator = mgr.isCreator(player.getUniqueId());
        if (!isCreator && !player.hasPermission(SELF_PERM)) {
            sender.sendMessage(CC.color("&cYou are not a content creator."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(CC.color("&cUsage: /creator set <" + String.join("|", EDITABLE_FIELDS) + "> <value>"));
            return;
        }
        ContentCreatorProfile p = mgr.getProfile(player.getUniqueId());
        if (p == null) {
            sender.sendMessage(CC.color("&cYour creator profile is not loaded."));
            return;
        }
        String field = args[1].toLowerCase(Locale.ENGLISH);
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        applyField(sender, p, field, value, mgr);
    }

    private void handleSetField(CommandSender sender, String[] args) {
        if (!sender.hasPermission(MANAGE_PERM)) {
            sender.sendMessage(CC.color("&cYou don't have permission to do that."));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(CC.color("&cUsage: /creator setfield <player> <" + String.join("|", EDITABLE_FIELDS) + "> <value>"));
            return;
        }
        ContentCreatorManager mgr = plugin.getContentCreatorManager();
        Player target = Bukkit.getPlayer(args[1]);
        UUID uuid;
        if (target != null) {
            uuid = target.getUniqueId();
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
            uuid = offline != null ? offline.getUniqueId() : null;
        }
        if (uuid == null || !mgr.isCreator(uuid)) {
            sender.sendMessage(CC.color("&c" + args[1] + " is not a registered content creator."));
            return;
        }
        ContentCreatorProfile p = mgr.getProfile(uuid);
        String field = args[2].toLowerCase(Locale.ENGLISH);
        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        applyField(sender, p, field, value, mgr);
    }

    private void handleAnnounce(CommandSender sender, String[] args) {
        if (!sender.hasPermission(MANAGE_PERM)) {
            sender.sendMessage(CC.color("&cYou don't have permission to do that."));
            return;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(CC.color("&c" + args[1] + " is not online."));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(CC.color("&cUsage: /creator announce <player>"));
            return;
        }
        if (!plugin.getContentCreatorManager().isCreator(target.getUniqueId())) {
            sender.sendMessage(CC.color("&c" + target.getName() + " is not a content creator."));
            return;
        }
        plugin.getContentCreatorManager().announceJoin(target);
        sender.sendMessage(CC.color("&a✔ Announced &e" + target.getName() + "&a's join."));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(MANAGE_PERM)) {
            sender.sendMessage(CC.color("&cYou don't have permission to do that."));
            return;
        }
        plugin.getContentCreatorManager().load();
        sender.sendMessage(CC.color("&a✔ Content creator data reloaded."));
    }

    private void handleStats(CommandSender sender, String[] args) {
        ContentCreatorManager mgr = plugin.getContentCreatorManager();
        ContentCreatorProfile profile;
        if (args.length >= 2 && sender.hasPermission(MANAGE_PERM)) {
            Player target = Bukkit.getPlayer(args[1]);
            UUID uuid = null;
            if (target != null) {
                uuid = target.getUniqueId();
            } else {
                @SuppressWarnings("deprecation")
                OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                if (offline != null) uuid = offline.getUniqueId();
            }
            profile = uuid != null ? mgr.getProfile(uuid) : null;
            if (profile == null) {
                sender.sendMessage(CC.color("&c" + args[1] + " is not a registered content creator."));
                return;
            }
        } else if (sender instanceof Player && mgr.isCreator(((Player) sender).getUniqueId())) {
            profile = mgr.getProfile(((Player) sender).getUniqueId());
        } else {
            sender.sendMessage(CC.color("&cYou are not a content creator or no player specified."));
            return;
        }

        boolean live = mgr.isLive(profile.getUuid());
        String platforms = buildPlatformLine(profile);
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("  &6&l✦ CC Stats &8— &e" + profile.effectiveDisplayName()));
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("  &7Live: " + (live ? "&aYes &c[LIVE]" : "&cNo")));
        sender.sendMessage(CC.color("  &7Platforms: " + (platforms.isEmpty() ? "&8None set" : platforms)));
        sender.sendMessage(CC.color("  &7Reward Code: " + (profile.getRewardCode().isEmpty()
                ? "&8Not set" : "&b" + profile.getRewardCode().toUpperCase(java.util.Locale.ENGLISH))));
        sender.sendMessage(CC.color("  &7Description: " + (profile.getDescription().isEmpty()
                ? "&8Not set" : "&f" + profile.getDescription())));
        sender.sendMessage(CC.color("  &7Stream Title: " + (profile.getStreamTitle().isEmpty()
                ? "&8Not set" : "&f" + profile.getStreamTitle())));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyField(CommandSender sender, ContentCreatorProfile p, String field, String value, ContentCreatorManager mgr) {
        switch (field) {
            case "youtube":
                p.setYoutube(value);
                sender.sendMessage(CC.color("&a✔ YouTube set to &e" + value));
                break;
            case "twitch":
                p.setTwitch(value);
                sender.sendMessage(CC.color("&a✔ Twitch set to &e" + value));
                break;
            case "tiktok":
                p.setTiktok(value);
                sender.sendMessage(CC.color("&a✔ TikTok set to &e" + value));
                break;
            case "twitter":
                p.setTwitter(value);
                sender.sendMessage(CC.color("&a✔ Twitter set to &e" + value));
                break;
            case "instagram":
                p.setInstagram(value);
                sender.sendMessage(CC.color("&a✔ Instagram set to &e" + value));
                break;
            case "discord":
                p.setDiscord(value);
                sender.sendMessage(CC.color("&a✔ Discord set to &e" + value));
                break;
            case "displayname":
                p.setDisplayName(value);
                sender.sendMessage(CC.color("&a✔ Display name set to &e" + value));
                break;
            case "description":
                p.setDescription(value);
                sender.sendMessage(CC.color("&a✔ Description set to &e" + value));
                break;
            case "code":
                p.setRewardCode(value);
                sender.sendMessage(CC.color("&a✔ Reward code set to &b" + p.getRewardCode().toUpperCase(Locale.ENGLISH)));
                break;
            case "streamtitle":
            case "title":
                p.setStreamTitle(value);
                sender.sendMessage(CC.color("&a✔ Stream title set to &e" + value));
                break;
            case "clear":
                switch (value.toLowerCase(Locale.ENGLISH)) {
                    case "youtube":     p.setYoutube(""); break;
                    case "twitch":      p.setTwitch(""); break;
                    case "tiktok":      p.setTiktok(""); break;
                    case "twitter":     p.setTwitter(""); break;
                    case "instagram":   p.setInstagram(""); break;
                    case "discord":     p.setDiscord(""); break;
                    case "displayname": p.setDisplayName(""); break;
                    case "description": p.setDescription(""); break;
                    case "code":        p.setRewardCode(""); break;
                    case "streamtitle": p.setStreamTitle(""); break;
                    default:
                        sender.sendMessage(CC.color("&cUnknown field: &e" + value));
                        return;
                }
                sender.sendMessage(CC.color("&a✔ Cleared " + value + " from profile."));
                break;
            default:
                sender.sendMessage(CC.color("&cUnknown field: &e" + field));
                sender.sendMessage(CC.color("&7Valid fields: &f" + String.join(", ", EDITABLE_FIELDS)));
                return;
        }
        mgr.updateProfile(p);
    }

    private void printProfile(CommandSender sender, ContentCreatorProfile p) {
        sender.sendMessage(CC.color(CC.SEPARATOR));
        boolean live = plugin.getContentCreatorManager().isLive(p.getUuid());
        sender.sendMessage(CC.color("  &6&l✦ " + p.effectiveDisplayName()
                + " &8(Content Creator)" + (live ? " &c[LIVE]" : "")));
        if (!p.getName().isEmpty() && !p.getName().equals(p.effectiveDisplayName()))
            sender.sendMessage(CC.color("  &7MC Name: &f" + p.getName()));
        if (!p.getDescription().isEmpty())
            sender.sendMessage(CC.color("  &7" + p.getDescription()));
        if (!p.getStreamTitle().isEmpty())
            sender.sendMessage(CC.color("  &fStreaming: &e\"" + p.getStreamTitle() + "\""));
        sender.sendMessage(CC.color("  "));
        if (!p.getYoutube().isEmpty())
            sender.sendMessage(CC.color("  &cYouTube   &8» &7youtube.com/" + p.getYoutube()));
        if (!p.getTwitch().isEmpty())
            sender.sendMessage(CC.color("  &5Twitch    &8» &7twitch.tv/" + p.getTwitch()));
        if (!p.getTiktok().isEmpty())
            sender.sendMessage(CC.color("  &fTikTok    &8» &7tiktok.com/@" + p.getTiktok()));
        if (!p.getTwitter().isEmpty())
            sender.sendMessage(CC.color("  &9Twitter   &8» &7twitter.com/" + p.getTwitter()));
        if (!p.getInstagram().isEmpty())
            sender.sendMessage(CC.color("  &dInstagram &8» &7instagram.com/" + p.getInstagram()));
        if (!p.getDiscord().isEmpty())
            sender.sendMessage(CC.color("  &bDiscord   &8» &7" + p.getDiscord()));
        if (!p.getRewardCode().isEmpty())
            sender.sendMessage(CC.color("  &7Code: &b/redeemcode " + p.getRewardCode().toUpperCase(Locale.ENGLISH)));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private String buildPlatformLine(ContentCreatorProfile p) {
        List<String> parts = new ArrayList<>();
        if (!p.getYoutube().isEmpty()) parts.add("&cYT");
        if (!p.getTwitch().isEmpty()) parts.add("&5Twitch");
        if (!p.getTiktok().isEmpty()) parts.add("&fTikTok");
        if (!p.getTwitter().isEmpty()) parts.add("&9Twitter");
        if (!p.getInstagram().isEmpty()) parts.add("&dIG");
        if (!p.getDiscord().isEmpty()) parts.add("&bDiscord");
        return String.join(" &8| ", parts);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("  &6&l✦ Content Creator Commands"));
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("  &b/creator list &8[page] &7— List all content creators"));
        sender.sendMessage(CC.color("  &b/creator profile &8[player] &7— View a CC's social profile"));
        sender.sendMessage(CC.color("  &b/creator stats &8[player] &7— View CC stats &8(self or admin)"));
        if (sender instanceof Player && plugin.getContentCreatorManager().isCreator(((Player) sender).getUniqueId())) {
            sender.sendMessage(CC.color("  &b/creator set &8<field> <value> &7— Edit your own profile"));
            sender.sendMessage(CC.color("  &b/cchat &8<message> &7— Send in CC-only chat channel"));
            sender.sendMessage(CC.color("  &b/shoutout &8<player> &7— Shoutout a viewer on the server"));
            sender.sendMessage(CC.color("  &b/golive &8[platform] &7— Announce you are streaming live"));
            sender.sendMessage(CC.color("  &b/offair &7— End your live broadcast"));
            sender.sendMessage(CC.color("  &b/socials &7— Broadcast your social links"));
            sender.sendMessage(CC.color("  &b/milestone &8<message> &7— Announce a milestone"));
            sender.sendMessage(CC.color("  &b/ccgiveaway &7— Run a rank giveaway for viewers"));
        }
        if (sender.hasPermission(MANAGE_PERM)) {
            sender.sendMessage(CC.color("  "));
            sender.sendMessage(CC.color("  &c/creator grant &8<player> &7— Grant CC status"));
            sender.sendMessage(CC.color("  &c/creator revoke &8<player> &7— Revoke CC status"));
            sender.sendMessage(CC.color("  &c/creator setfield &8<player> <field> <value>"));
            sender.sendMessage(CC.color("  &c/creator announce &8[player] &7— Force join announcement"));
            sender.sendMessage(CC.color("  &c/creator reload &7— Reload CC data"));
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("list", "profile", "stats"));
            if (sender instanceof Player && plugin.getContentCreatorManager().isCreator(((Player) sender).getUniqueId()))
                subs.add("set");
            if (sender.hasPermission(MANAGE_PERM))
                subs.addAll(Arrays.asList("grant", "revoke", "setfield", "announce", "reload"));
            return filter(subs, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ENGLISH);
            if (sub.equals("profile") || sub.equals("grant") || sub.equals("revoke")
                    || sub.equals("announce") || sub.equals("setfield")) {
                return onlinePlayers(args[1]);
            }
            if (sub.equals("set")) {
                return filter(new ArrayList<>(EDITABLE_FIELDS), args[1]);
            }
            return Collections.emptyList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setfield")) {
            return filter(new ArrayList<>(EDITABLE_FIELDS), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> onlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase(Locale.ENGLISH).startsWith(prefix.toLowerCase(Locale.ENGLISH)))
                .collect(Collectors.toList());
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase(Locale.ENGLISH).startsWith(prefix.toLowerCase(Locale.ENGLISH)))
                .collect(Collectors.toList());
    }
}
