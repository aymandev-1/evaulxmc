package dev.evaulx.core.nametags;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NameTagManager {

    private static final String TEAM_PREFIX = "evx";

    private final EvaulxCore plugin;
    private final Map<UUID, String> lastScoreboardEntries = new ConcurrentHashMap<>();

    public NameTagManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void applyNameTag(Player player) {
        if (!plugin.getConfig().getBoolean("nametags.enabled", true)) {
            removeNameTag(player);
            return;
        }

        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null) return;

        if (plugin.getHubHook() != null && plugin.getHubHook().handlesNameTags() && !profile.isDisguised()) {
            plugin.getHubHook().refreshPlayer(player);
            return;
        }

        Rank rank = resolveRank(profile);
        String visibleName = plugin.getDisguiseManager().getVisibleName(player);
        String format = plugin.getConfig().getString("nametags.format", "{tag}{prefix}{namecolor}{player}{suffix}");
        String formatted = CC.color(renderPlaceholders(format, player, profile, rank));
        player.setDisplayName(formatted);

        if (isTabListEnabled()) {
            applyTabName(player, profile, rank, visibleName);
        } else {
            player.setPlayerListName(null);
        }

        if (isScoreboardEnabled()) {
            applyScoreboardTeams(player, profile, rank, visibleName);
        }
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyNameTag(player);
        }
    }

    public void removeNameTag(Player player) {
        if (player != null) {
            removeFromManagedTeams(player);
            lastScoreboardEntries.remove(player.getUniqueId());
            player.setDisplayName(player.getName());
            player.setPlayerListName(null);
        }
    }

    public boolean isScoreboardEnabled() {
        return plugin.getConfig().getBoolean("nametags.scoreboard.enabled", true);
    }

    public boolean isTabListEnabled() {
        return plugin.getConfig().getBoolean("nametags.tab-list.enabled", true);
    }

    private Rank resolveRank(PlayerProfile profile) {
        Rank rank = null;
        if (profile.isDisguised() && profile.getDisguiseRank() != null) {
            rank = plugin.getRankManager().getRank(profile.getDisguiseRank());
        }
        if (rank == null) rank = plugin.getRankManager().getRank(profile.getRankName());
        if (rank == null) rank = plugin.getRankManager().getDefaultRank();
        return rank;
    }

    private void applyTabName(Player player, PlayerProfile profile, Rank rank, String visibleName) {
        String format = plugin.getConfig().getString("nametags.tab-list.format", "{prefix}{namecolor}{player}");
        int limit = plugin.getConfig().getInt("nametags.tab-list.max-visible-length", 16);
        String rendered = renderPlaceholders(format, player, profile, rank);
        try {
            player.setPlayerListName(fitTabName(CC.color(rendered), visibleName, profile, rank, Math.max(1, limit)));
        } catch (Exception ignored) {
            player.setPlayerListName(trimMinecraft(visibleName == null ? player.getName() : visibleName, 16));
        }
    }

    private void applyScoreboardTeams(Player target, PlayerProfile profile, Rank rank, String entryName) {
        String previousEntry = lastScoreboardEntries.put(target.getUniqueId(), entryName);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = viewer.getScoreboard();
            if (scoreboard == null) scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            if (scoreboard == null) continue;

            removeFromManagedTeams(scoreboard, target, previousEntry, entryName);
            Team team = getOrCreateTeam(scoreboard, target, profile, rank, entryName);
            addEntry(team, target, entryName);
        }
    }

    private Team getOrCreateTeam(Scoreboard scoreboard, Player player, PlayerProfile profile, Rank rank, String entryName) {
        String teamName = teamName(rank, profile, entryName);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) team = scoreboard.registerNewTeam(teamName);

        String prefixFormat = plugin.getConfig().getString("nametags.scoreboard.prefix-format", "{tag}{prefix}{namecolor}");
        String suffixFormat = plugin.getConfig().getString("nametags.scoreboard.suffix-format", "{suffix}");
        team.setPrefix(trimMinecraft(CC.color(renderPlaceholders(prefixFormat, player, profile, rank)), 16));
        team.setSuffix(trimMinecraft(CC.color(renderPlaceholders(suffixFormat, player, profile, rank)), 16));
        return team;
    }

    private String teamName(Rank rank, PlayerProfile profile, String entryName) {
        int weight = rank == null ? 0 : rank.getWeight();
        int sort = Math.max(0, Math.min(9999, 9999 - weight));
        String key = (rank == null ? "default" : rank.getName())
                + "|" + (entryName == null ? "" : entryName)
                + "|" + (profile == null ? "" : profile.getTag())
                + "|" + resolveNameColor(profile, rank);
        String hash = Integer.toHexString(key.toLowerCase(Locale.ENGLISH).hashCode());
        return TEAM_PREFIX + String.format(Locale.ENGLISH, "%04d", sort) + hash;
    }

    private void removeFromManagedTeams(Player player) {
        String previousEntry = lastScoreboardEntries.get(player.getUniqueId());
        String visibleEntry = plugin.getDisguiseManager().getVisibleName(player);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = viewer.getScoreboard();
            if (scoreboard != null) removeFromManagedTeams(scoreboard, player, previousEntry, visibleEntry);
        }
        Scoreboard main = Bukkit.getScoreboardManager() == null ? null : Bukkit.getScoreboardManager().getMainScoreboard();
        if (main != null) removeFromManagedTeams(main, player, previousEntry, visibleEntry);
    }

    private void removeFromManagedTeams(Scoreboard scoreboard, Player player, String... entries) {
        for (Team team : scoreboard.getTeams()) {
            if (!team.getName().startsWith(TEAM_PREFIX)) continue;
            removePlayer(team, player);
            removeEntry(team, player.getName());
            if (entries == null) continue;
            for (String entry : entries) {
                removeEntry(team, entry);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void addEntry(Team team, Player player, String entry) {
        if (entry != null && !entry.trim().isEmpty()
                && (invokeTeamEntry(team, "hasEntry", entry) || invokeTeamEntry(team, "addEntry", entry))) {
            return;
        }
        if (!team.hasPlayer(player)) team.addPlayer(player);
    }

    @SuppressWarnings("deprecation")
    private void removePlayer(Team team, OfflinePlayer player) {
        if (team.hasPlayer(player)) team.removePlayer(player);
    }

    private void removeEntry(Team team, String entry) {
        if (entry == null || entry.trim().isEmpty()) return;
        invokeTeamEntry(team, "removeEntry", entry);
    }

    private boolean invokeTeamEntry(Team team, String methodName, String entry) {
        try {
            Method method = team.getClass().getMethod(methodName, String.class);
            Object result = method.invoke(team, entry);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String renderPlaceholders(String format, Player player, PlayerProfile profile, Rank rank) {
        String tag = profile.getTag() == null || profile.getTag().isEmpty() ? "" : profile.getTag() + " ";
        String prefix = rank != null ? rank.getPrefix() : "";
        String suffix = rank != null ? rank.getSuffix() : "";
        String color = rank != null ? rank.getColor() : "&f";
        String display = rank != null ? rank.getDisplay() : "";
        String permission = rank != null ? rank.getPermission() : "";
        String weight = rank != null ? String.valueOf(rank.getWeight()) : "0";
        String nameColor = resolveNameColor(profile, rank);
        String visibleName = plugin.getDisguiseManager().getVisibleName(player);

        return format
                .replace("{tag}", tag)
                .replace("{prefix}", prefix)
                .replace("{suffix}", suffix)
                .replace("{color}", color)
                .replace("{namecolor}", nameColor)
                .replace("{name_color}", nameColor)
                .replace("{player}", visibleName)
                .replace("{rank}", rank != null ? rank.getName() : "default")
                .replace("{rank_display}", display.isEmpty() && rank != null ? rank.getDisplayName() : display)
                .replace("{rank_permission}", permission)
                .replace("{priority}", weight)
                .replace("{weight}", weight);
    }

    private String resolveNameColor(PlayerProfile profile, Rank rank) {
        String rankColor = rank != null && rank.getColor() != null ? rank.getColor() : "&f";
        if (profile != null && profile.isDisguised() && profile.getDisguiseRank() != null) {
            return rankColor;
        }
        return profile == null || profile.getNameColor() == null || profile.getNameColor().isEmpty()
                ? rankColor
                : profile.getNameColor();
    }

    private String fitTabName(String rendered, String visibleName, PlayerProfile profile, Rank rank, int limit) {
        if (rendered != null && rendered.length() <= limit) return rendered;

        String safeName = visibleName == null || visibleName.isEmpty() ? "Player" : visibleName;
        String coloredName = CC.color(resolveNameColor(profile, rank) + safeName);
        if (coloredName.length() <= limit) return coloredName;
        return trimMinecraft(safeName, limit);
    }

    private String trimMinecraft(String value, int limit) {
        if (value == null) return "";
        if (value.length() <= limit) return value;
        String trimmed = value.substring(0, limit);
        if (!trimmed.isEmpty() && trimmed.charAt(trimmed.length() - 1) == ChatColor.COLOR_CHAR) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
