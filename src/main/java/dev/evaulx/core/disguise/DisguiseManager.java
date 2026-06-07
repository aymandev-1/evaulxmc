package dev.evaulx.core.disguise;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class DisguiseManager {

    private static final String SKIN_TITLE = "Skin Select";
    private static final String RANK_TITLE = "Rank Select";
    private static final String NAME_TITLE = "Name Select";
    private static final String ACTIVE_TITLE = "Active Disguises";
    private static final int[] SELECT_SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
    private static final int NEXT_PAGE_SLOT = 19;
    private static final int PREVIOUS_PAGE_SLOT = 25;

    private final EvaulxCore plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, ProfileSnapshot> originalProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, ProfileSnapshot> visibleProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, DisguiseSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, SkinProperty> skinCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> refreshTokens = new ConcurrentHashMap<>();
    private final Map<UUID, Long> disguiseCooldowns = new ConcurrentHashMap<>();
    private final Deque<DisguiseHistoryEntry> history = new ArrayDeque<>();
    private final Set<UUID> awaitingName = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private File historyFile;
    private File skinCacheFile;

    public DisguiseManager(EvaulxCore plugin) {
        this.plugin = plugin;
        loadHistory();
        loadSkinCache();
    }

    public boolean disguise(Player player, String disguiseName, String skinName, String rankName) {
        return disguise(player, disguiseName, skinName, rankName, "disguise");
    }

    public boolean disguise(Player player, String disguiseName, String skinName, String rankName, String commandName) {
        if (!checkCooldown(player)) return false;

        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null) {
            plugin.getMessageManager().send(player, "profile-loading", "&cThat player's profile is still loading.");
            return false;
        }
        String realName = profile.getName();

        String cleanName = cleanName(disguiseName);
        if (cleanName == null) {
            plugin.getMessageManager().send(player, "disguise.invalid-name",
                    "&cDisguise name must be 3-16 letters, numbers, or underscores.");
            return false;
        }

        if (!canUseDisguiseName(player, cleanName)) return false;

        if (skinName == null || skinName.trim().isEmpty()) skinName = cleanName;
        String cleanSkin = cleanName(skinName);
        if (cleanSkin == null) {
            plugin.getMessageManager().send(player, "disguise.invalid-name",
                    "&cDisguise name must be 3-16 letters, numbers, or underscores.");
            return false;
        }
        if (!canUseRequestedSkin(player, cleanSkin)) return false;

        if (rankName != null && plugin.getRankManager().getRank(rankName) == null) {
            plugin.getMessageManager().send(player, "disguise.rank-not-found", "&cRank not found.");
            return false;
        }
        if (!canUseRequestedRank(player, rankName)) return false;

        if (plugin.getConfig().getBoolean("disguise.require-real-skin", false)) {
            List<SkinProperty> availableTextures = cachedTextures(cleanSkin);
            if (availableTextures != null && !availableTextures.isEmpty()) {
                applyDisguise(player, profile, realName, cleanName, cleanSkin, rankName, commandName, availableTextures);
                sendSkinLoaded(player, cleanSkin);
                return true;
            }

            sendSkinLoading(player, cleanSkin);
            TaskUtil.async(() -> {
                SkinProperty skin = fetchSkin(cleanSkin);
                TaskUtil.sync(() -> {
                    Player online = Bukkit.getPlayer(player.getUniqueId());
                    PlayerProfile current = online != null ? plugin.getPlayerManager().getProfile(online) : null;
                    if (online == null || current == null) return;

                    if (skin == null) {
                        sendSkinFailed(online, cleanSkin);
                        return;
                    }

                    applyDisguise(online, current, realName, cleanName, cleanSkin, rankName, commandName,
                            Collections.singletonList(skin));
                    sendSkinLoaded(online, cleanSkin);
                });
            });
            return true;
        }

        List<SkinProperty> cachedTextures = cachedTextures(cleanSkin);
        applyDisguise(player, profile, realName, cleanName, cleanSkin, rankName, commandName, cachedTextures);
        if (cachedTextures == null || cachedTextures.isEmpty()) {
            loadSkinAsync(player.getUniqueId(), cleanSkin, true);
        }

        return true;
    }

    public boolean randomDisguise(Player player, String rankName, String commandName) {
        String skin = getRandomSkin(player);
        return disguise(player, getRandomName(player), skin, rankName, commandName);
    }

    public void handleJoin(Player player) {
        if (player == null) return;

        TaskUtil.syncLater(() -> refreshDisguisesForViewer(player), 10L);

        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null || !profile.isDisguised()) return;

        if (!plugin.getConfig().getBoolean("disguise.persist-on-relog", false)) {
            clearDisguiseState(profile);
            plugin.getPlayerManager().saveProfile(profile);
            return;
        }

        String cleanName = cleanName(profile.getDisguiseName());
        String cleanSkin = cleanName(profile.getDisguiseSkin() == null ? cleanName : profile.getDisguiseSkin());
        if (cleanName == null || cleanSkin == null) {
            clearDisguiseState(profile);
            plugin.getPlayerManager().saveProfile(profile);
            return;
        }

        profile.setDisguiseName(cleanName);
        profile.setDisguiseSkin(cleanSkin);
        originalProfiles.put(player.getUniqueId(), captureProfile(player, profile.getName()));

        applyProfile(player, cleanName, cachedTextures(cleanSkin));
        plugin.getNameTagManager().applyNameTag(player);
        refreshPlayer(player);
        syncVanishVisibility(player);

        if (getCachedSkin(cleanSkin) == null) loadSkinAsync(player.getUniqueId(), cleanSkin, false);
    }

    public void handleQuit(Player player) {
        if (player == null) return;

        awaitingName.remove(player.getUniqueId());
        sessions.remove(player.getUniqueId());
        refreshTokens.remove(player.getUniqueId());

        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile != null && profile.isDisguised()) {
            if (plugin.getConfig().getBoolean("disguise.persist-on-relog", false)) {
                plugin.getPlayerManager().saveProfile(profile);
            } else if (plugin.getConfig().getBoolean("disguise.auto-undisguise-on-quit", true)) {
                undisguise(player);
            } else {
                clearDisguiseState(profile);
                plugin.getPlayerManager().saveProfile(profile);
            }
        }

        originalProfiles.remove(player.getUniqueId());
        visibleProfiles.remove(player.getUniqueId());
    }

    public boolean changeSkin(Player player, String skinName) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null || !profile.isDisguised()) {
            plugin.getMessageManager().send(player, "disguise.not-disguised", "&cYou are not disguised.");
            return false;
        }

        String cleanSkin = cleanName(skinName);
        if (cleanSkin == null) {
            plugin.getMessageManager().send(player, "disguise.invalid-name",
                    "&cDisguise name must be 3-16 letters, numbers, or underscores.");
            return false;
        }
        if (!canUseRequestedSkin(player, cleanSkin)) return false;

        String previousSkin = profile.getDisguiseSkin();
        profile.setDisguiseSkin(cleanSkin);

        SkinProperty cached = getAvailableSkin(cleanSkin);
        if (cached != null) {
            applyChangedSkin(player, profile, previousSkin, cleanSkin, cached);
            return true;
        }

        sendSkinLoading(player, cleanSkin);

        TaskUtil.async(() -> {
            SkinProperty skin = fetchSkin(cleanSkin);
            TaskUtil.sync(() -> {
                Player online = Bukkit.getPlayer(player.getUniqueId());
                PlayerProfile current = online != null ? plugin.getPlayerManager().getProfile(online) : null;
                if (online == null || current == null || !current.isDisguised()) return;
                if (!cleanSkin.equalsIgnoreCase(current.getDisguiseSkin())) return;

                if (skin == null) {
                    current.setDisguiseSkin(previousSkin);
                    sendSkinFailed(online, cleanSkin);
                    return;
                }

                applyChangedSkin(online, current, previousSkin, cleanSkin, skin);
            });
        });

        return true;
    }

    public boolean changeRank(Player player, String rankName) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null || !profile.isDisguised()) {
            plugin.getMessageManager().send(player, "disguise.not-disguised", "&cYou are not disguised.");
            return false;
        }

        if (rankName == null || rankName.trim().isEmpty() || rankName.equalsIgnoreCase("none")) {
            rankName = null;
        } else if (plugin.getRankManager().getRank(rankName) == null) {
            plugin.getMessageManager().send(player, "disguise.rank-not-found", "&cRank not found.");
            return false;
        }

        if (!canUseRequestedRank(player, rankName)) return false;

        String previousRank = profile.getDisguiseRank();
        profile.setDisguiseRank(rankName);
        plugin.getNameTagManager().applyNameTag(player);
        refreshPlayer(player);
        if (plugin.getConfig().getBoolean("disguise.persist-on-relog", false)) {
            plugin.getPlayerManager().saveProfile(profile);
        }

        Map<String, String> placeholders = plugin.getMessageManager().placeholders(
                "{rank}", rankName == null ? "none" : rankName,
                "{previous}", previousRank == null ? "none" : previousRank
        );
        plugin.getMessageManager().send(player, "disguise.rank-changed",
                "&aUpdated disguise rank to &f{rank}&a.", placeholders);
        plugin.getStaffRequestManager().logAction(profile.getName(), "DISGUISE_RANK",
                rankName == null ? "none" : rankName,
                "changed disguise rank from " + (previousRank == null ? "none" : previousRank));
        return true;
    }

    private void applyChangedSkin(Player player, PlayerProfile profile, String previousSkin,
                                  String cleanSkin, SkinProperty skin) {
        applyProfile(player, profile.getDisguiseName(), Collections.singletonList(skin));
        plugin.getNameTagManager().applyNameTag(player);
        refreshPlayer(player);
        sendSkinChanged(player, cleanSkin);
        if (plugin.getConfig().getBoolean("disguise.persist-on-relog", false)) {
            plugin.getPlayerManager().saveProfile(profile);
        }
        plugin.getStaffRequestManager().logAction(profile.getName(), "DISGUISE_SKIN", cleanSkin,
                "changed disguise skin from " + (previousSkin == null ? "none" : previousSkin));
    }

    private void applyDisguise(Player player, PlayerProfile profile, String realName, String cleanName, String cleanSkin,
                               String rankName, String commandName, List<SkinProperty> initialTextures) {
        originalProfiles.computeIfAbsent(player.getUniqueId(), uuid -> captureProfile(player, realName));

        profile.setDisguised(true);
        profile.setDisguiseName(cleanName);
        profile.setDisguiseSkin(cleanSkin);
        profile.setDisguiseRank(rankName);
        if (plugin.getConfig().getBoolean("disguise.persist-on-relog", false)) {
            plugin.getPlayerManager().saveProfile(profile);
        }

        applyProfile(player, cleanName, initialTextures);
        plugin.getNameTagManager().applyNameTag(player);
        refreshPlayer(player);

        Map<String, String> placeholders = plugin.getMessageManager().placeholders(
                "{name}", cleanName,
                "{skin}", profile.getDisguiseSkin(),
                "{rank}", rankName == null ? "" : rankName
        );
        plugin.getMessageManager().send(player, "disguise.enabled",
                "&8[&cDisguise&8] &fYou are now disguised as &c{name}&f.", placeholders);
        plugin.getMessageManager().actionBar(player, "action-bars.disguise.enabled",
                "&cDisguised as &f{name}", placeholders);
        addHistory(realName, cleanName, profile.getDisguiseSkin(), rankName, commandName);
        notifyStaffDisguise(realName, cleanName, profile.getDisguiseSkin(), rankName, commandName);
        plugin.getStaffRequestManager().logAction(realName, "DISGUISE", cleanName,
                "skin=" + profile.getDisguiseSkin() + ", rank=" + (rankName == null ? "none" : rankName));
        plugin.getDiscordManager().sendDisguise(realName, cleanName, profile.getDisguiseSkin(), rankName, true);
        if (plugin.getRedisSyncManager() != null) plugin.getRedisSyncManager().publishStaffStatus(player);
        if (plugin.getRedisSyncManager() != null) {
            plugin.getRedisSyncManager().publishDisguise(realName, player.getUniqueId(), cleanName,
                    profile.getDisguiseSkin(), rankName, true);
        }
    }

    private void loadSkinAsync(UUID playerId, String requestedSkin, boolean notify) {
        Player start = Bukkit.getPlayer(playerId);
        if (notify && start != null) sendSkinLoading(start, requestedSkin);

        TaskUtil.async(() -> {
            SkinProperty skin = fetchSkin(requestedSkin);
            TaskUtil.sync(() -> {
                Player online = Bukkit.getPlayer(playerId);
                PlayerProfile current = online != null ? plugin.getPlayerManager().getProfile(online) : null;
                if (online == null || current == null || !current.isDisguised()) return;
                if (!requestedSkin.equalsIgnoreCase(current.getDisguiseSkin())) return;

                if (skin == null) {
                    if (notify) sendSkinFailed(online, requestedSkin);
                    return;
                }

                applyProfile(online, current.getDisguiseName(), Collections.singletonList(skin));
                plugin.getNameTagManager().applyNameTag(online);
                refreshPlayer(online);
                if (notify) sendSkinLoaded(online, requestedSkin);
            });
        });
    }

    private void sendSkinLoading(Player player, String skinName) {
        sendSkinLoading((CommandSender) player, skinName);
    }

    private void sendSkinLoading(CommandSender sender, String skinName) {
        plugin.getMessageManager().send(sender, "disguise.skin-loading",
                "&7Loading skin &f{skin}&7...",
                plugin.getMessageManager().placeholders("{skin}", skinName == null ? "" : skinName));
    }

    private void sendSkinLoaded(Player player, String skinName) {
        Map<String, String> placeholders = plugin.getMessageManager().placeholders("{skin}", skinName == null ? "" : skinName);
        plugin.getMessageManager().send(player, "disguise.skin-loaded",
                "&aSkin loaded: &f{skin}&a.", placeholders);
        plugin.getMessageManager().actionBar(player, "action-bars.disguise.skin-loaded",
                "&aSkin loaded: &f{skin}", placeholders);
    }

    private void sendSkinFailed(Player player, String skinName) {
        Map<String, String> placeholders = plugin.getMessageManager().placeholders("{skin}", skinName == null ? "" : skinName);
        plugin.getMessageManager().send(player, "disguise.skin-failed",
                "&cCould not load skin &f{skin}&c. Use a real Minecraft username.", placeholders);
        plugin.getMessageManager().actionBar(player, "action-bars.disguise.skin-failed",
                "&cSkin failed: &f{skin}", placeholders);
    }

    private void sendSkinChanged(Player player, String skinName) {
        Map<String, String> placeholders = plugin.getMessageManager().placeholders("{skin}", skinName == null ? "" : skinName);
        plugin.getMessageManager().send(player, "disguise.skin-changed",
                "&aUpdated disguise skin to &f{skin}&a.", placeholders);
        plugin.getMessageManager().actionBar(player, "action-bars.disguise.skin-loaded",
                "&aSkin loaded: &f{skin}", placeholders);
    }

    public int clearSkinCache() {
        int size = skinCache.size();
        skinCache.clear();
        saveSkinCache();
        return size;
    }

    public int getSkinCacheSize() {
        purgeExpiredSkins();
        return skinCache.size();
    }

    public int getOnlineDisguiseCount() {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (profile != null && profile.isDisguised()) count++;
        }
        return count;
    }

    public boolean isSafeModeEnabled() {
        return plugin.getConfig().getBoolean("disguise.safe-mode", false);
    }

    public boolean refreshDisguise(CommandSender sender, String query) {
        Player target = findOnlinePlayer(query);
        if (target == null) {
            plugin.getMessageManager().send(sender, "disguise.realname-not-found",
                    "&cNo online disguised player matched &f{query}&c.",
                    plugin.getMessageManager().placeholders("{query}", query == null ? "" : query));
            return false;
        }

        PlayerProfile profile = plugin.getPlayerManager().getProfile(target);
        if (profile == null || !profile.isDisguised()) {
            plugin.getMessageManager().send(sender, "disguise.force-not-disguised", "&cThat player is not disguised.");
            return false;
        }

        String skin = cleanName(profile.getDisguiseSkin());
        applyProfile(target, profile.getDisguiseName(), cachedTextures(skin));
        plugin.getNameTagManager().applyNameTag(target);
        refreshPlayer(target);
        syncVanishVisibility(target);
        if (skin != null && getCachedSkin(skin) == null) loadSkinAsync(target.getUniqueId(), skin, false);

        plugin.getMessageManager().send(sender, "disguise.refreshed",
                "&aRefreshed disguise for &f{target}&a.",
                plugin.getMessageManager().placeholders("{target}", profile.getName(), "{name}", profile.getDisguiseName()));
        plugin.getStaffRequestManager().logAction(sender.getName(), "DISGUISE_REFRESH", profile.getName(),
                "refreshed " + profile.getDisguiseName());
        return true;
    }

    public void sendStatus(CommandSender sender) {
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cDisguise Status"));
        sender.sendMessage(CC.color("&7Enabled: &f" + plugin.getConfig().getBoolean("disguise.enabled", true)));
        sender.sendMessage(CC.color("&7Safe Mode: &f" + isSafeModeEnabled()));
        sender.sendMessage(CC.color("&7Packet Refresh: &f" + isPacketRefreshEnabled()));
        sender.sendMessage(CC.color("&7Self Refresh: &f" + plugin.getConfig().getBoolean("disguise.self-refresh", true)));
        sender.sendMessage(CC.color("&7ProtocolLib Hook: &f"
                + (plugin.getProtocolLibHook() != null && plugin.getProtocolLibHook().isHooked())));
        sender.sendMessage(CC.color("&7Strict Skin Mode: &f" + plugin.getConfig().getBoolean("disguise.require-real-skin", false)));
        sender.sendMessage(CC.color("&7Persist On Relog: &f" + plugin.getConfig().getBoolean("disguise.persist-on-relog", false)));
        sender.sendMessage(CC.color("&7Auto Undisguise On Quit: &f" + plugin.getConfig().getBoolean("disguise.auto-undisguise-on-quit", true)));
        sender.sendMessage(CC.color("&7Skin Cache TTL: &f" + plugin.getConfig().getLong("disguise.skin-cache-ttl-minutes", 1440L) + "m"));
        sender.sendMessage(CC.color("&7Cached Skins: &f" + getSkinCacheSize()));
        sender.sendMessage(CC.color("&7Online Disguised: &f" + getOnlineDisguiseCount()));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    public void sendDebug(CommandSender sender, String query) {
        Player target = findOnlinePlayer(query);
        if (target == null) {
            plugin.getMessageManager().send(sender, "disguise.realname-not-found",
                    "&cNo online disguised player matched &f{query}&c.",
                    plugin.getMessageManager().placeholders("{query}", query == null ? "" : query));
            return;
        }

        PlayerProfile profile = plugin.getPlayerManager().getProfile(target);
        boolean disguised = profile != null && profile.isDisguised();
        String skin = profile != null ? profile.getDisguiseSkin() : null;
        String cacheKey = skin == null ? null : skin.toLowerCase(Locale.ENGLISH);
        ProfileSnapshot visible = visibleProfiles.get(target.getUniqueId());
        ProfileSnapshot original = originalProfiles.get(target.getUniqueId());

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cDisguise Debug &8- &f" + target.getName()));
        sender.sendMessage(CC.color("&7Real Name: &f" + (profile == null ? target.getName() : profile.getName())));
        sender.sendMessage(CC.color("&7Disguised: &f" + disguised));
        sender.sendMessage(CC.color("&7Visible Name: &f" + (profile == null ? target.getName() : getVisibleName(target))));
        sender.sendMessage(CC.color("&7Skin: &f" + (skin == null ? "none" : skin)
                + " &8(cache: " + (cacheKey != null && getCachedSkin(cacheKey) != null ? "yes" : "no") + ")"));
        sender.sendMessage(CC.color("&7Rank: &f" + (profile == null || profile.getDisguiseRank() == null ? "none" : profile.getDisguiseRank())));
        sender.sendMessage(CC.color("&7Visible Profile Stored: &f" + (visible != null)));
        sender.sendMessage(CC.color("&7Original Profile Stored: &f" + (original != null)));
        sender.sendMessage(CC.color("&7Packet Refresh: &f" + isPacketRefreshEnabled()));
        sender.sendMessage(CC.color("&7Self Refresh: &f" + plugin.getConfig().getBoolean("disguise.self-refresh", true)));
        sender.sendMessage(CC.color("&7Safe Mode: &f" + isSafeModeEnabled()));
        sender.sendMessage(CC.color("&7Strict Skin Mode: &f" + plugin.getConfig().getBoolean("disguise.require-real-skin", false)));
        sender.sendMessage(CC.color("&7ProtocolLib Hook: &f"
                + (plugin.getProtocolLibHook() != null && plugin.getProtocolLibHook().isHooked())));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    public void testDisguise(CommandSender sender, String skinName, String rankName) {
        String cleanSkin = cleanName(skinName);
        if (cleanSkin == null) {
            plugin.getMessageManager().send(sender, "disguise.invalid-name",
                    "&cDisguise name must be 3-16 letters, numbers, or underscores.");
            return;
        }

        if (rankName != null && !rankName.trim().isEmpty() && plugin.getRankManager().getRank(rankName) == null) {
            plugin.getMessageManager().send(sender, "disguise.rank-not-found", "&cRank not found.");
            return;
        }

        SkinProperty available = getAvailableSkin(cleanSkin);
        if (available != null) {
            sendDisguiseTest(sender, cleanSkin, rankName, true, "cache/online");
            return;
        }

        sendSkinLoading(sender, cleanSkin);
        TaskUtil.async(() -> {
            SkinProperty fetched = fetchSkin(cleanSkin);
            TaskUtil.sync(() -> sendDisguiseTest(sender, cleanSkin, rankName, fetched != null, "mojang"));
        });
    }

    private void sendDisguiseTest(CommandSender sender, String skinName, String rankName,
                                  boolean skinLoaded, String skinSource) {
        boolean protocolHooked = plugin.getProtocolLibHook() != null && plugin.getProtocolLibHook().isHooked();
        boolean packetRefresh = isPacketRefreshEnabled();
        boolean selfRefresh = plugin.getConfig().getBoolean("disguise.self-refresh", true);
        int tabLimit = plugin.getConfig().getInt("nametags.tab-list.max-visible-length", 16);
        int visibleLength = skinName == null ? 0 : skinName.length();
        boolean tabFits = visibleLength <= tabLimit;
        boolean scoreboard = plugin.getNameTagManager() != null && plugin.getNameTagManager().isScoreboardEnabled();

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cDisguise Test"));
        sender.sendMessage(CC.color((skinLoaded ? "&aOK" : "&cISSUE") + " &7Skin &f" + skinName
                + " &7loaded via &f" + skinSource + "&7."));
        sender.sendMessage(CC.color((rankName == null || rankName.trim().isEmpty()
                || plugin.getRankManager().getRank(rankName) != null ? "&aOK" : "&cISSUE")
                + " &7Rank: &f" + (rankName == null || rankName.trim().isEmpty() ? "none" : rankName)));
        sender.sendMessage(CC.color((packetRefresh ? "&aOK" : "&eWARN") + " &7Packet refresh: &f" + packetRefresh));
        sender.sendMessage(CC.color((protocolHooked ? "&aOK" : "&eWARN") + " &7ProtocolLib hook: &f" + protocolHooked));
        sender.sendMessage(CC.color((selfRefresh ? "&aOK" : "&eWARN") + " &7Self refresh: &f" + selfRefresh));
        sender.sendMessage(CC.color((tabFits ? "&aOK" : "&eWARN") + " &7Tab name length: &f"
                + visibleLength + "/" + tabLimit));
        sender.sendMessage(CC.color((scoreboard ? "&aOK" : "&eWARN") + " &7Scoreboard teams: &f" + scoreboard));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    public long getCooldownRemaining(Player player) {
        Long last = disguiseCooldowns.get(player.getUniqueId());
        if (last == null) return 0L;
        long cooldown = plugin.getConfig().getLong("disguise.cooldown-seconds", 10L) * 1000L;
        return Math.max(0L, cooldown - (System.currentTimeMillis() - last));
    }

    public List<DisguiseHistoryEntry> getHistory(String realName, int max) {
        List<DisguiseHistoryEntry> results = new ArrayList<>();
        synchronized (history) {
            for (DisguiseHistoryEntry entry : history) {
                if (realName == null || realName.isEmpty()
                        || entry.getRealName().equalsIgnoreCase(realName)
                        || entry.getDisguiseName().equalsIgnoreCase(realName)) {
                    results.add(entry);
                    if (results.size() >= max) break;
                }
            }
        }
        return results;
    }

    public void openSkinSelector(Player player) {
        DisguiseSession session = sessions.computeIfAbsent(player.getUniqueId(), ignored -> new DisguiseSession());
        session.setCommandName("disguise");
        openSkinSelector(player, 0);
    }

    public void openSkinSelector(Player player, int page) {
        List<String> skins = getConfiguredSkins();
        int[] slots = getSelectSlots();
        int maxPage = Math.max(0, (int) Math.ceil(skins.size() / (double) slots.length) - 1);
        page = Math.max(0, Math.min(page, maxPage));

        DisguiseSession session = sessions.computeIfAbsent(player.getUniqueId(), ignored -> new DisguiseSession());
        session.setSkinPage(page);

        Inventory inventory = Bukkit.createInventory(null, 54, getSkinTitle());
        fillBorder(inventory);

        int start = page * slots.length;
        for (int i = 0; i < slots.length; i++) {
            int index = start + i;
            if (index >= skins.size()) break;
            String skin = skins.get(index);
            inventory.setItem(slots[i], createSkinItem(skin));
        }

        inventory.setItem(getNextPageSlot(), createNamedItem(Material.ENDER_EYE, "&aNext Page",
                "&7Click to go to the next skin page."));
        inventory.setItem(getPreviousPageSlot(), createNamedItem(Material.ENDER_EYE, "&cPrevious Page",
                "&7Click to go back a skin page."));

        player.openInventory(inventory);
    }

    public void openRankSelector(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, getRankTitle());
        fillBorder(inventory);

        List<dev.evaulx.core.models.Rank> ranks = plugin.getRankManager().getRanksByWeight();
        int[] slots = getSelectSlots();
        int limit = Math.min(ranks.size(), slots.length);
        for (int i = 0; i < limit; i++) {
            inventory.setItem(slots[i], createRankItem(ranks.get(i), i + 1));
        }

        player.openInventory(inventory);
    }

    public void openNameSelector(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, getNameTitle());
        fillBorder(inventory);
        inventory.setItem(plugin.getConfig().getInt("disguise.gui.name.custom-slot", 21),
                createConfiguredItem("disguise.gui.name.custom-item", Material.OAK_SIGN, "&cPick your name",
                        Arrays.asList("&7Click to write your own disguise name.", "&7You will type the name in chat."), null));
        inventory.setItem(plugin.getConfig().getInt("disguise.gui.name.random-slot", 23),
                createConfiguredItem("disguise.gui.name.random-item", Material.BOOK, "&cRandom Name",
                        Collections.singletonList("&7Click to use a random disguise name."), null));
        player.openInventory(inventory);
    }

    public void openNickSelector(Player player) {
        DisguiseSession session = sessions.computeIfAbsent(player.getUniqueId(), ignored -> new DisguiseSession());
        session.setSkin(player.getName());
        session.setRank(null);
        session.setCommandName("nick");
        openNameSelector(player);
    }

    public void openAdminGui(Player player) {
        openAdminGui(player, 0);
    }

    public void openAdminGui(Player player, int page) {
        List<Player> disguised = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(online);
            if (profile != null && profile.isDisguised()) disguised.add(online);
        }

        int[] slots = getSelectSlots();
        int maxPage = Math.max(0, (int) Math.ceil(disguised.size() / (double) slots.length) - 1);
        page = Math.max(0, Math.min(page, maxPage));

        DisguiseSession session = sessions.computeIfAbsent(player.getUniqueId(), ignored -> new DisguiseSession());
        session.setSkinPage(page);

        Inventory inventory = Bukkit.createInventory(null, 54, getActiveTitle());
        fillBorder(inventory);

        int start = page * slots.length;
        for (int i = 0; i < slots.length; i++) {
            int index = start + i;
            if (index >= disguised.size()) break;
            inventory.setItem(slots[i], createActiveDisguiseItem(disguised.get(index)));
        }

        inventory.setItem(getNextPageSlot(), createNamedItem(Material.ENDER_EYE, "&aNext Page",
                "&7Click to go to the next disguise page."));
        inventory.setItem(getPreviousPageSlot(), createNamedItem(Material.ENDER_EYE, "&cPrevious Page",
                "&7Click to go back a disguise page."));

        player.openInventory(inventory);
    }

    public boolean handleMenuClick(Player player, InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!getSkinTitle().equals(title) && !getRankTitle().equals(title)
                && !getNameTitle().equals(title) && !getActiveTitle().equals(title)) {
            return false;
        }

        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) return true;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return true;

        if (getSkinTitle().equals(title)) {
            handleSkinClick(player, event.getRawSlot(), item);
            return true;
        }

        if (getRankTitle().equals(title)) {
            handleRankClick(player, item);
            return true;
        }

        if (getActiveTitle().equals(title)) {
            handleAdminGuiClick(player, event.getRawSlot(), item, event.isRightClick());
            return true;
        }

        handleNameClick(player, event.getRawSlot());
        return true;
    }

    public boolean isAwaitingName(Player player) {
        return awaitingName.contains(player.getUniqueId());
    }

    public void handleNameChat(Player player, String message) {
        if (!awaitingName.contains(player.getUniqueId())) return;

        String name = cleanName(message);
        if (message.equalsIgnoreCase("cancel")) {
            awaitingName.remove(player.getUniqueId());
            sessions.remove(player.getUniqueId());
            plugin.getMessageManager().send(player, "disguise.name-cancelled", "&cDisguise name selection cancelled.");
            return;
        }

        if (name == null) {
            plugin.getMessageManager().send(player, "disguise.name-invalid",
                    "&cName must be 3-16 letters, numbers, or underscores. Type another name or 'cancel'.");
            return;
        }

        awaitingName.remove(player.getUniqueId());
        completeDisguise(player, name);
    }

    public boolean undisguise(Player player) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null || !profile.isDisguised()) return false;

        ProfileSnapshot snapshot = originalProfiles.remove(player.getUniqueId());
        String realName = snapshot != null ? snapshot.getName() : profile.getName();
        String oldDisguiseName = profile.getDisguiseName();
        String oldDisguiseSkin = profile.getDisguiseSkin();
        String oldDisguiseRank = profile.getDisguiseRank();

        clearDisguiseState(profile);
        plugin.getPlayerManager().saveProfile(profile);

        applyProfile(player, realName, snapshot != null ? snapshot.getTextures() : null);
        plugin.getNameTagManager().applyNameTag(player);
        refreshPlayer(player);

        plugin.getMessageManager().send(player, "disguise.disabled-self",
                "&8[&cDisguise&8] &fYou are no longer disguised.");
        plugin.getMessageManager().actionBar(player, "action-bars.disguise.disabled", "&cDisguise removed");
        plugin.getStaffRequestManager().logAction(realName, "UNDISGUISE", realName, "Removed disguise");
        plugin.getDiscordManager().sendDisguise(realName, oldDisguiseName, oldDisguiseSkin, oldDisguiseRank, false);
        if (plugin.getRedisSyncManager() != null) plugin.getRedisSyncManager().publishStaffStatus(player);
        if (plugin.getRedisSyncManager() != null) {
            plugin.getRedisSyncManager().publishDisguise(realName, player.getUniqueId(), oldDisguiseName,
                    oldDisguiseSkin, oldDisguiseRank, false);
        }
        notifyStaffUndisguise(realName);
        cleanupVisibleProfileLater(player.getUniqueId());
        return true;
    }

    public boolean isDisguised(Player player) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        return profile != null && profile.isDisguised();
    }

    public String getVisibleName(Player player) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile != null && profile.isDisguised() && profile.getDisguiseName() != null) {
            return profile.getDisguiseName();
        }
        return profile != null ? profile.getName() : player.getName();
    }

    public String getDisguisedName(Player player) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        return profile != null ? profile.getDisguiseName() : null;
    }

    public Player findOnlinePlayer(String query) {
        if (query == null || query.trim().isEmpty()) return null;

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (query.equalsIgnoreCase(player.getName())) return player;
            if (profile != null && query.equalsIgnoreCase(profile.getName())) return player;
            if (profile != null && profile.isDisguised()
                    && query.equalsIgnoreCase(profile.getDisguiseName())) {
                return player;
            }
        }

        return Bukkit.getPlayer(query);
    }

    private void notifyStaffDisguise(String realName, String disguiseName, String skinName, String rankName, String commandName) {
        if (!plugin.getConfig().getBoolean("disguise.staff-alerts", true)) return;
        String server = plugin.getConfig().getString("server.server-id", "hub");
        Map<String, String> placeholders = plugin.getMessageManager().placeholders(
                "{player}", realName,
                "{real}", realName,
                "{name}", disguiseName,
                "{skin}", skinName == null ? "" : skinName,
                "{rank}", rankName == null ? "none" : rankName,
                "{command}", commandName == null ? "disguise" : commandName,
                "{server}", server
        );
        String message = plugin.getMessageManager().get("disguise.staff-alert",
                "&8[&cStaff&8] &f{player} &7used &f/{command} &7as &c{name} &7skin &f{skin} &7rank &f{rank}&7 on &f{server}&7.",
                placeholders);
        plugin.getStaffRequestManager().broadcastStaff(message);
        if (plugin.getRedisSyncManager() != null) {
            plugin.getRedisSyncManager().publishStaffNotice("DISGUISE", message);
        }
    }

    private void notifyStaffUndisguise(String realName) {
        if (!plugin.getConfig().getBoolean("disguise.staff-alerts", true)) return;
        String server = plugin.getConfig().getString("server.server-id", "hub");
        String message = plugin.getMessageManager().get("disguise.staff-undisguise-alert",
                "&8[&cStaff&8] &f{player} &7removed their disguise on &f{server}&7.",
                plugin.getMessageManager().placeholders("{player}", realName, "{server}", server));
        plugin.getStaffRequestManager().broadcastStaff(message);
        if (plugin.getRedisSyncManager() != null) {
            plugin.getRedisSyncManager().publishStaffNotice("UNDISGUISE", message);
        }
    }

    private boolean checkCooldown(Player player) {
        if (player.hasPermission("evaulx.disguise.cooldown.bypass")
                || player.hasPermission("evaulx.nick.cooldown.bypass")
                || player.hasPermission("evaulx.disguise.admin")) {
            return true;
        }

        long remaining = getCooldownRemaining(player);
        if (remaining > 0L) {
            plugin.getMessageManager().send(player, "disguise.cooldown",
                    "&cPlease wait {seconds}s before changing your disguise again.",
                    plugin.getMessageManager().placeholders("{seconds}", String.valueOf((remaining + 999L) / 1000L)));
            return false;
        }

        disguiseCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        return true;
    }

    private void clearDisguiseState(PlayerProfile profile) {
        profile.setDisguised(false);
        profile.setDisguiseName(null);
        profile.setDisguiseSkin(null);
        profile.setDisguiseRank(null);
    }

    private void refreshDisguisesForViewer(Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(viewer.getUniqueId())) continue;
            PlayerProfile profile = plugin.getPlayerManager().getProfile(online);
            if (profile == null || !profile.isDisguised()) continue;

            long refreshToken = System.nanoTime();
            refreshTokens.put(online.getUniqueId(), refreshToken);
            if (profile.isVanished() && !canSeeVanished(viewer)) {
                viewer.hidePlayer(online);
                continue;
            }
            refreshViewer(viewer, online, refreshToken);
        }
    }

    private void syncVanishVisibility(Player player) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile != null && profile.isVanished()) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.getUniqueId().equals(player.getUniqueId()) && !canSeeVanished(viewer)) {
                    viewer.hidePlayer(player);
                }
            }
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;
            PlayerProfile onlineProfile = plugin.getPlayerManager().getProfile(online);
            if (onlineProfile != null && onlineProfile.isVanished() && !canSeeVanished(player)) {
                player.hidePlayer(online);
            }
        }
    }

    private List<SkinProperty> cachedTextures(String skinName) {
        SkinProperty cached = getAvailableSkin(skinName);
        return cached == null ? null : Collections.singletonList(cached);
    }

    private SkinProperty getAvailableSkin(String skinName) {
        String cleanName = cleanName(skinName);
        if (cleanName == null) return null;

        SkinProperty cached = getCachedSkin(cleanName);
        if (cached != null) return cached;

        SkinProperty onlineSkin = getOnlineSkin(cleanName);
        if (onlineSkin == null) return null;
        SkinProperty cachedOnline = onlineSkin.cacheAs(cleanName, skinCache);
        saveSkinCache();
        return cachedOnline;
    }

    private SkinProperty getCachedSkin(String skinName) {
        String cleanName = cleanName(skinName);
        if (cleanName == null) return null;

        String key = cleanName.toLowerCase(Locale.ENGLISH);
        SkinProperty cached = skinCache.get(key);
        if (cached == null) return null;

        long ttlMinutes = plugin.getConfig().getLong("disguise.skin-cache-ttl-minutes", 1440L);
        if (ttlMinutes > 0L && System.currentTimeMillis() - cached.getCachedAt() > ttlMinutes * 60_000L) {
            skinCache.remove(key);
            return null;
        }

        return cached;
    }

    private void purgeExpiredSkins() {
        if (plugin.getConfig().getLong("disguise.skin-cache-ttl-minutes", 1440L) <= 0L) return;
        for (String key : new ArrayList<>(skinCache.keySet())) getCachedSkin(key);
    }

    private boolean isPacketRefreshEnabled() {
        return plugin.getConfig().getBoolean("disguise.packet-refresh", true) && !isSafeModeEnabled();
    }

    private void cleanupVisibleProfileLater(UUID uuid) {
        long delay = Math.max(20L,
                plugin.getConfig().getLong("disguise.skin-load-delay-ticks", 6L) * 2L
                        + plugin.getConfig().getLong("disguise.entity-spawn-delay-ticks", 2L) * 2L
                        + plugin.getConfig().getLong("disguise.second-refresh-delay-ticks", 12L)
                        + 5L);
        TaskUtil.syncLater(() -> {
            Player online = Bukkit.getPlayer(uuid);
            PlayerProfile current = online != null ? plugin.getPlayerManager().getProfile(online) : null;
            if (current == null || !current.isDisguised()) visibleProfiles.remove(uuid);
        }, delay);
    }

    private void addHistory(String realName, String disguiseName, String skinName, String rankName, String commandName) {
        if (!plugin.getConfig().getBoolean("disguise.history.enabled", true)) return;

        int max = Math.max(1, plugin.getConfig().getInt("disguise.history.max-entries", 500));
        synchronized (history) {
            history.addFirst(new DisguiseHistoryEntry(realName, disguiseName, skinName, rankName, commandName,
                    plugin.getConfig().getString("server.server-id", "hub"), System.currentTimeMillis()));
            while (history.size() > max) history.removeLast();
        }
        saveHistory();
    }

    private void loadHistory() {
        historyFile = new File(plugin.getDataFolder(), "disguise-history.json");
        if (!historyFile.exists()) return;

        synchronized (history) {
            history.clear();
            try (Reader reader = new InputStreamReader(new FileInputStream(historyFile), StandardCharsets.UTF_8)) {
                JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) continue;
                    DisguiseHistoryEntry entry = historyFromJson(element.getAsJsonObject());
                    if (entry != null) history.addLast(entry);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load disguise history: " + e.getMessage());
            }
        }
    }

    private void saveHistory() {
        if (historyFile == null) return;
        JsonArray array = new JsonArray();
        synchronized (history) {
            for (DisguiseHistoryEntry entry : history) array.add(historyToJson(entry));
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(historyFile), StandardCharsets.UTF_8)) {
            gson.toJson(array, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save disguise history: " + e.getMessage());
        }
    }

    private void loadSkinCache() {
        skinCacheFile = new File(plugin.getDataFolder(), "skin-cache.json");
        if (!skinCacheFile.exists()) return;

        try (Reader reader = new InputStreamReader(new FileInputStream(skinCacheFile), StandardCharsets.UTF_8)) {
            JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                String name = getString(object, "name", null);
                String value = getString(object, "value", null);
                if (name == null || value == null || cleanName(name) == null) continue;

                long cachedAt = object.has("cachedAt") && !object.get("cachedAt").isJsonNull()
                        ? object.get("cachedAt").getAsLong()
                        : System.currentTimeMillis();
                skinCache.put(name.toLowerCase(Locale.ENGLISH), new SkinProperty(
                        value,
                        getString(object, "signature", null),
                        cachedAt
                ));
            }
            purgeExpiredSkins();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load skin cache: " + e.getMessage());
        }
    }

    private void saveSkinCache() {
        if (skinCacheFile == null) return;

        JsonArray array = new JsonArray();
        for (Map.Entry<String, SkinProperty> entry : skinCache.entrySet()) {
            SkinProperty property = entry.getValue();
            if (property == null || property.getValue() == null || property.getValue().isEmpty()) continue;

            JsonObject object = new JsonObject();
            object.addProperty("name", entry.getKey());
            object.addProperty("value", property.getValue());
            object.addProperty("signature", property.getSignature());
            object.addProperty("cachedAt", property.getCachedAt());
            array.add(object);
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(skinCacheFile), StandardCharsets.UTF_8)) {
            gson.toJson(array, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save skin cache: " + e.getMessage());
        }
    }

    private JsonObject historyToJson(DisguiseHistoryEntry entry) {
        JsonObject object = new JsonObject();
        object.addProperty("realName", entry.getRealName());
        object.addProperty("disguiseName", entry.getDisguiseName());
        object.addProperty("skinName", entry.getSkinName());
        object.addProperty("rankName", entry.getRankName());
        object.addProperty("commandName", entry.getCommandName());
        object.addProperty("serverId", entry.getServerId());
        object.addProperty("createdAt", entry.getCreatedAt());
        return object;
    }

    private DisguiseHistoryEntry historyFromJson(JsonObject object) {
        try {
            return new DisguiseHistoryEntry(
                    getString(object, "realName", "Unknown"),
                    getString(object, "disguiseName", "Unknown"),
                    getString(object, "skinName", ""),
                    getString(object, "rankName", ""),
                    getString(object, "commandName", "disguise"),
                    getString(object, "serverId", "unknown"),
                    object.has("createdAt") && !object.get("createdAt").isJsonNull()
                            ? object.get("createdAt").getAsLong()
                            : System.currentTimeMillis()
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getString(JsonObject object, String key, String fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) return fallback;
        return object.get(key).getAsString();
    }

    private void handleSkinClick(Player player, int slot, ItemStack item) {
        DisguiseSession session = sessions.computeIfAbsent(player.getUniqueId(), ignored -> new DisguiseSession());

        if (slot == getNextPageSlot()) {
            openSkinSelector(player, session.getSkinPage() + 1);
            return;
        }

        if (slot == getPreviousPageSlot()) {
            openSkinSelector(player, session.getSkinPage() - 1);
            return;
        }

        if (item.getType() != Material.PLAYER_HEAD) return;
        String skin = CC.strip(item.getItemMeta().getDisplayName()).trim();
        if (cleanName(skin) == null) return;

        session.setSkin(skin);
        openRankSelector(player);
    }

    private void handleRankClick(Player player, ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return;
        String rankName = null;

        for (String line : item.getItemMeta().getLore()) {
            String stripped = CC.strip(line);
            if (stripped.startsWith("EVAULX_RANK:")) {
                rankName = stripped.substring("EVAULX_RANK:".length());
                break;
            }
            if (stripped.startsWith("Rank: ")) {
                rankName = stripped.substring("Rank: ".length());
                break;
            }
        }

        if (rankName == null || plugin.getRankManager().getRank(rankName) == null) return;
        DisguiseSession session = sessions.computeIfAbsent(player.getUniqueId(), ignored -> new DisguiseSession());
        session.setRank(rankName);
        openNameSelector(player);
    }

    private void handleNameClick(Player player, int slot) {
        if (slot == plugin.getConfig().getInt("disguise.gui.name.custom-slot", 21)) {
            awaitingName.add(player.getUniqueId());
            player.closeInventory();
            plugin.getMessageManager().send(player, "disguise.name-prompt",
                    "&7Type your disguise name in chat, or type &ccancel&7.");
            return;
        }

        if (slot == plugin.getConfig().getInt("disguise.gui.name.random-slot", 23)) {
            completeDisguise(player, getRandomName(player));
        }
    }

    private void handleAdminGuiClick(Player player, int slot, ItemStack item, boolean rightClick) {
        if (!player.hasPermission("evaulx.disguise.admin")) return;
        DisguiseSession session = sessions.computeIfAbsent(player.getUniqueId(), ignored -> new DisguiseSession());

        if (slot == getNextPageSlot()) {
            openAdminGui(player, session.getSkinPage() + 1);
            return;
        }

        if (slot == getPreviousPageSlot()) {
            openAdminGui(player, session.getSkinPage() - 1);
            return;
        }

        String targetId = hiddenLoreValue(item, "EVAULX_PLAYER:");
        if (targetId == null) return;

        Player target;
        try {
            target = Bukkit.getPlayer(UUID.fromString(targetId));
        } catch (Exception ignored) {
            target = null;
        }

        if (target == null) {
            plugin.getMessageManager().send(player, "player-not-online", "&cPlayer not online.");
            openAdminGui(player, session.getSkinPage());
            return;
        }

        PlayerProfile profile = plugin.getPlayerManager().getProfile(target);
        if (profile == null || !profile.isDisguised()) {
            plugin.getMessageManager().send(player, "disguise.force-not-disguised", "&cThat player is not disguised.");
            openAdminGui(player, session.getSkinPage());
            return;
        }

        if (rightClick) {
            undisguise(target);
            plugin.getMessageManager().send(player, "disguise.forced-undisguise",
                    "&7Removed &f{target}&7's disguise.",
                    plugin.getMessageManager().placeholders("{target}", profile.getName()));
        } else {
            refreshDisguise(player, profile.getName());
        }

        openAdminGui(player, session.getSkinPage());
    }

    private void completeDisguise(Player player, String disguiseName) {
        DisguiseSession session = sessions.remove(player.getUniqueId());
        if (session == null || session.getSkin() == null) {
            plugin.getMessageManager().send(player, "disguise.selection-expired",
                    "&cYour disguise selection expired. Use /disguise again.");
            return;
        }

        player.closeInventory();
        disguise(player, disguiseName, session.getSkin(), session.getRank(),
                session.getCommandName() == null ? "disguise" : session.getCommandName());
    }

    private List<String> getConfiguredSkins() {
        List<String> skins = plugin.getConfig().getStringList("disguise.skins");
        if (skins.isEmpty()) {
            skins = Arrays.asList("Steve", "Alex", "Notch", "jeb_", "Dinnerbone", "Grumm", "Herobrine",
                    "Dream", "Technoblade", "TommyInnit", "Sapnap", "GeorgeNotFound", "BadBoyHalo",
                    "Skeppy", "CaptainSparklez");
        }

        List<String> clean = new ArrayList<>();
        for (String skin : skins) {
            String name = cleanName(skin);
            if (name != null) clean.add(name);
        }
        return clean;
    }

    private String getRandomSkin(Player player) {
        List<String> skins = getPooledList(player, "skins");
        if (skins.isEmpty()) return "Steve";
        return skins.get(new Random().nextInt(skins.size()));
    }

    private String getRandomName() {
        return getRandomName(null);
    }

    private String getRandomName(Player player) {
        List<String> names = getPooledList(player, "random-names");

        List<String> clean = new ArrayList<>();
        for (String name : names) {
            String valid = cleanName(name);
            if (valid != null) clean.add(valid);
        }
        if (clean.isEmpty()) return "Player" + new Random().nextInt(9999);
        return clean.get(new Random().nextInt(clean.size()));
    }

    private List<String> getPooledList(Player player, String key) {
        if (player != null) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (profile != null) {
                List<String> values = plugin.getConfig().getStringList("disguise.rank-pools." + profile.getRankName() + "." + key);
                if (!values.isEmpty()) return values;
                for (String extraRank : profile.getExtraRanks()) {
                    values = plugin.getConfig().getStringList("disguise.rank-pools." + extraRank + "." + key);
                    if (!values.isEmpty()) return values;
                }
            }
        }

        if (key.equals("skins")) return getConfiguredSkins();

        List<String> names = plugin.getConfig().getStringList("disguise.random-names");
        if (names.isEmpty()) {
            names = Arrays.asList("OakRunner", "StoneVale", "FrostByte", "SkyMason", "NovaCrafter",
                    "RiverAsh", "IronVex", "PixelBloom", "CloudRift", "EmberFox");
        }
        return names;
    }

    private String getSkinTitle() {
        return CC.color(plugin.getConfig().getString("disguise.gui.skin-title", SKIN_TITLE));
    }

    private String getRankTitle() {
        return CC.color(plugin.getConfig().getString("disguise.gui.rank-title", RANK_TITLE));
    }

    private String getNameTitle() {
        return CC.color(plugin.getConfig().getString("disguise.gui.name-title", NAME_TITLE));
    }

    private String getActiveTitle() {
        return CC.color(plugin.getConfig().getString("disguise.gui.active-title", ACTIVE_TITLE));
    }

    private int getNextPageSlot() {
        return plugin.getConfig().getInt("disguise.gui.next-page-slot", NEXT_PAGE_SLOT);
    }

    private int getPreviousPageSlot() {
        return plugin.getConfig().getInt("disguise.gui.previous-page-slot", PREVIOUS_PAGE_SLOT);
    }

    private int[] getSelectSlots() {
        List<Integer> configured = plugin.getConfig().getIntegerList("disguise.gui.selection-slots");
        if (configured.isEmpty()) return SELECT_SLOTS;

        List<Integer> clean = new ArrayList<>();
        for (Integer slot : configured) {
            if (slot != null && slot >= 0 && slot < 54) clean.add(slot);
        }
        if (clean.isEmpty()) return SELECT_SLOTS;

        int[] slots = new int[clean.size()];
        for (int i = 0; i < clean.size(); i++) slots[i] = clean.get(i);
        return slots;
    }

    private boolean canUseDisguiseName(Player player, String name) {
        boolean bypassNameCheck = player.hasPermission("evaulx.disguise.bypass-name-check")
                || player.hasPermission("evaulx.disguise.admin");

        if (plugin.getConfig().getBoolean("disguise.blacklist.enabled", true)
                && !player.hasPermission("evaulx.disguise.bypass-blacklist")
                && isBlacklistedName(name)) {
            plugin.getMessageManager().send(player, "disguise.blacklisted-name", "&cThat disguise name is blocked.");
            return false;
        }

        if (!canUseRequestedName(player, name)) return false;

        if (plugin.getConfig().getBoolean("disguise.prevent-duplicate-names", true)
                && !bypassNameCheck
                && isDuplicateName(player, name)) {
            plugin.getMessageManager().send(player, "disguise.duplicate-name", "&cThat disguise name is already in use.");
            return false;
        }

        return true;
    }

    private boolean canUseRequestedName(Player player, String name) {
        if (!plugin.getConfig().getBoolean("disguise.restrictions.enabled", true)) return true;
        if (player.hasPermission("evaulx.disguise.admin")) return true;
        if (!plugin.getConfig().getBoolean("disguise.restrictions.pool-only-names", false)) return true;

        if (containsCleanName(getPooledList(player, "random-names"), name)) return true;
        plugin.getMessageManager().send(player, "disguise.restricted-name",
                "&cYou can only use names from your disguise name pool.");
        return false;
    }

    private boolean canUseRequestedSkin(Player player, String skin) {
        if (!plugin.getConfig().getBoolean("disguise.restrictions.enabled", true)) return true;
        if (player.hasPermission("evaulx.disguise.admin")) return true;
        if (!plugin.getConfig().getBoolean("disguise.restrictions.pool-only-skins", false)) return true;

        if (containsCleanName(getPooledList(player, "skins"), skin)) return true;
        plugin.getMessageManager().send(player, "disguise.restricted-skin",
                "&cYou can only use skins from your disguise skin pool.");
        return false;
    }

    private boolean canUseRequestedRank(Player player, String rankName) {
        if (rankName == null || rankName.trim().isEmpty()) return true;
        if (!plugin.getConfig().getBoolean("disguise.restrictions.enabled", true)) return true;
        if (player.hasPermission("evaulx.disguise.admin")) return true;

        List<String> allowed = getAllowedDisguiseRanks(player);
        if (!allowed.isEmpty() && isRankAllowed(allowed, rankName)) return true;
        if (!allowed.isEmpty()) {
            plugin.getMessageManager().send(player, "disguise.restricted-rank",
                    "&cYou cannot disguise with that rank.");
            return false;
        }

        if (!plugin.getConfig().getBoolean("disguise.restrictions.require-rank-permission", false)) return true;
        String pattern = plugin.getConfig()
                .getString("disguise.restrictions.rank-permission-pattern", "evaulx.disguise.rank.{rank}");
        String permissionRank = rankName.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9_.-]", "-");
        if (pattern != null && player.hasPermission(pattern.replace("{rank}", permissionRank))) return true;

        plugin.getMessageManager().send(player, "disguise.restricted-rank",
                "&cYou cannot disguise with that rank.");
        return false;
    }

    private List<String> getAllowedDisguiseRanks(Player player) {
        List<String> allowed = new ArrayList<>(plugin.getConfig().getStringList("disguise.restrictions.allowed-ranks"));
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile != null) {
            allowed.addAll(plugin.getConfig().getStringList("disguise.rank-pools." + profile.getRankName() + ".allowed-ranks"));
            for (String extraRank : profile.getExtraRanks()) {
                allowed.addAll(plugin.getConfig().getStringList("disguise.rank-pools." + extraRank + ".allowed-ranks"));
            }
        }
        return allowed;
    }

    private boolean containsCleanName(List<String> values, String name) {
        for (String value : values) {
            String clean = cleanName(value);
            if (clean != null && clean.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private boolean containsIgnoreCase(List<String> values, String value) {
        for (String entry : values) {
            if (entry != null && entry.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    private boolean isRankAllowed(List<String> values, String rankName) {
        dev.evaulx.core.models.Rank requested = plugin.getRankManager().getRank(rankName);
        String category = requested == null ? "" : plugin.getRankManager().getRankCategory(requested);
        for (String entry : values) {
            if (entry == null) continue;
            String trimmed = entry.trim();
            if (trimmed.equals("*")) return true;
            if (trimmed.equalsIgnoreCase(rankName)) return true;
            if (!category.isEmpty() && trimmed.equalsIgnoreCase(category)) return true;
            if (!category.isEmpty() && trimmed.equalsIgnoreCase("category:" + category)) return true;
        }
        return false;
    }

    private boolean isDuplicateName(Player player, String name) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;

            PlayerProfile onlineProfile = plugin.getPlayerManager().getProfile(online);
            if (onlineProfile != null && onlineProfile.isDisguised()
                    && name.equalsIgnoreCase(onlineProfile.getDisguiseName())) {
                return true;
            }

            if (plugin.getConfig().getBoolean("disguise.prevent-online-player-names", true)
                    && (name.equalsIgnoreCase(online.getName())
                    || (onlineProfile != null && name.equalsIgnoreCase(onlineProfile.getName())))) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlacklistedName(String name) {
        for (String blocked : plugin.getConfig().getStringList("disguise.blacklist.names")) {
            if (blocked != null && name.equalsIgnoreCase(blocked.trim())) return true;
        }

        for (String blocked : plugin.getConfig().getStringList("disguise.blacklist.reserved-names")) {
            if (blocked != null && name.equalsIgnoreCase(blocked.trim())) return true;
        }

        String lowered = name.toLowerCase(Locale.ENGLISH);
        for (String blocked : plugin.getConfig().getStringList("disguise.blacklist.blocked-contains")) {
            if (blocked != null && !blocked.trim().isEmpty()
                    && lowered.contains(blocked.trim().toLowerCase(Locale.ENGLISH))) {
                return true;
            }
        }

        for (String rawPattern : plugin.getConfig().getStringList("disguise.blacklist.blocked-patterns")) {
            if (rawPattern == null || rawPattern.trim().isEmpty()) continue;
            try {
                if (Pattern.compile(rawPattern).matcher(name).matches()) return true;
            } catch (PatternSyntaxException e) {
                plugin.getLogger().warning("Invalid disguise blacklist pattern '" + rawPattern + "': " + e.getMessage());
            }
        }

        if (plugin.getConfig().getBoolean("disguise.blacklist.block-staff-names", true)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (plugin.getStaffRequestManager().canReceiveStaffAlerts(online)
                        && online.getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }

        if (plugin.getConfig().getBoolean("disguise.blacklist.block-rank-names", true)) {
            for (dev.evaulx.core.models.Rank rank : plugin.getRankManager().getRanks()) {
                if (rank.getName().equalsIgnoreCase(name)) return true;
                if (CC.strip(rank.getDisplayName()).equalsIgnoreCase(name)) return true;
            }
        }

        if (plugin.getConfig().getBoolean("disguise.blacklist.block-staff-rank-names", true)) {
            for (dev.evaulx.core.models.Rank rank : plugin.getRankManager().getRanks()) {
                if (rank.isStaff() && rank.getName().equalsIgnoreCase(name)) return true;
            }
        }

        return false;
    }

    private void fillBorder(Inventory inventory) {
        Material material = parseMaterial(plugin.getConfig().getString("disguise.gui.border.material", "STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);
        short data = (short) plugin.getConfig().getInt("disguise.gui.border.data", 15);
        String name = plugin.getConfig().getString("disguise.gui.border.name", " ");
        ItemStack filler = createNamedItem(new ItemStack(material, 1, data), name);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int col = slot % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) inventory.setItem(slot, filler);
        }
    }

    private ItemStack createSkinItem(String skin) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(skin);
        Map<String, String> placeholders = plugin.getMessageManager().placeholders("{skin}", skin);
        meta.setDisplayName(CC.color(applyPlaceholders(
                plugin.getConfig().getString("disguise.gui.skin.name", "&c{skin}"), placeholders)));
        meta.setLore(colorLines(applyPlaceholders(plugin.getConfig().getStringList("disguise.gui.skin.lore"), placeholders)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRankItem(dev.evaulx.core.models.Rank rank, int order) {
        Material material = parseMaterial(plugin.getConfig().getString("disguise.gui.rank.material", "STAINED_CLAY"), Material.GRAY_TERRACOTTA);
        ItemStack item = new ItemStack(material, 1, colorData(rank.getColor()));
        ItemMeta meta = item.getItemMeta();
        Map<String, String> placeholders = plugin.getMessageManager().placeholders(
                "{rank}", rank.getName(),
                "{rank_display}", rank.getDisplayName(),
                "{rank_color}", rank.getColor(),
                "{order}", String.valueOf(order),
                "{weight}", String.valueOf(rank.getWeight())
        );
        meta.setDisplayName(CC.color(applyPlaceholders(
                plugin.getConfig().getString("disguise.gui.rank.name", "{rank_display}"), placeholders)));
        List<String> lore = applyPlaceholders(plugin.getConfig().getStringList("disguise.gui.rank.lore"), placeholders);
        lore.add("&0EVAULX_RANK:" + rank.getName());
        meta.setLore(colorLines(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActiveDisguiseItem(Player target) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(target);
        String realName = profile == null ? target.getName() : profile.getName();
        String disguiseName = profile == null ? target.getName() : profile.getDisguiseName();
        String skin = profile == null ? "" : profile.getDisguiseSkin();
        String rank = profile == null ? "" : profile.getDisguiseRank();

        ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(disguiseName == null ? target.getName() : disguiseName);
        meta.setDisplayName(CC.color("&c" + nullToNone(disguiseName)));
        List<String> lore = new ArrayList<>();
        lore.add(CC.color("&7Real: &f" + realName));
        lore.add(CC.color("&7Online: &f" + target.getName()));
        lore.add(CC.color("&7Skin: &f" + nullToNone(skin)));
        lore.add(CC.color("&7Rank: &f" + nullToNone(rank)));
        lore.add(CC.color("&7Left-click to refresh."));
        lore.add(CC.color("&7Right-click to undisguise."));
        lore.add(CC.color("&0EVAULX_PLAYER:" + target.getUniqueId()));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private short colorData(String color) {
        if (color == null) return 0;
        if (color.contains("&0")) return 15;
        if (color.contains("&1")) return 11;
        if (color.contains("&2")) return 13;
        if (color.contains("&3")) return 9;
        if (color.contains("&4")) return 14;
        if (color.contains("&5")) return 10;
        if (color.contains("&6")) return 1;
        if (color.contains("&7")) return 8;
        if (color.contains("&8")) return 7;
        if (color.contains("&9")) return 11;
        if (color.contains("&a")) return 5;
        if (color.contains("&b")) return 3;
        if (color.contains("&c")) return 14;
        if (color.contains("&d")) return 2;
        if (color.contains("&e")) return 4;
        return 0;
    }

    private ItemStack createNamedItem(Material material, String name, String... lore) {
        return createNamedItem(new ItemStack(material), name, lore);
    }

    private ItemStack createConfiguredItem(String path, Material fallbackMaterial, String fallbackName,
                                           List<String> fallbackLore, Map<String, String> placeholders) {
        Material material = parseMaterial(plugin.getConfig().getString(path + ".material", fallbackMaterial.name()), fallbackMaterial);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(applyPlaceholders(plugin.getConfig().getString(path + ".name", fallbackName), placeholders)));
        List<String> lore = plugin.getConfig().getStringList(path + ".lore");
        if (lore.isEmpty() && fallbackLore != null) lore = fallbackLore;
        meta.setLore(colorLines(applyPlaceholders(lore, placeholders)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNamedItem(ItemStack item, String name, String... lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        if (lore != null && lore.length > 0) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) coloredLore.add(CC.color(line));
            meta.setLore(coloredLore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null || name.trim().isEmpty()) return fallback;
        Material material = Material.matchMaterial(name.trim());
        return material != null ? material : fallback;
    }

    private String applyPlaceholders(String input, Map<String, String> placeholders) {
        if (input == null) return "";
        String output = input;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                output = output.replace(entry.getKey(), entry.getValue());
            }
        }
        return output;
    }

    private List<String> applyPlaceholders(List<String> input, Map<String, String> placeholders) {
        List<String> output = new ArrayList<>();
        if (input == null) return output;
        for (String line : input) output.add(applyPlaceholders(line, placeholders));
        return output;
    }

    private List<String> colorLines(List<String> lines) {
        List<String> colored = new ArrayList<>();
        if (lines == null) return colored;
        for (String line : lines) colored.add(CC.color(line));
        return colored;
    }

    private String hiddenLoreValue(ItemStack item, String prefix) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String stripped = CC.strip(line);
            if (stripped.startsWith(prefix)) return stripped.substring(prefix.length());
        }
        return null;
    }

    private String nullToNone(String value) {
        return value == null || value.isEmpty() ? "none" : value;
    }

    private void applyProfile(Player player, String name, List<SkinProperty> textures) {
        List<SkinProperty> profileTextures = textures != null
                ? new ArrayList<>(textures)
                : captureProfile(player, name).getTextures();
        visibleProfiles.put(player.getUniqueId(), new ProfileSnapshot(name, profileTextures));

        if (isSafeModeEnabled()) return;

        try {
            Object gameProfile = getGameProfile(player);
            setFinalField(gameProfile, "name", name);

            Object properties = gameProfile.getClass().getMethod("getProperties").invoke(gameProfile);
            applyTextures(properties, profileTextures);
        } catch (Exception e) {
            plugin.getLogger().warning("Profile reflection failed for " + player.getUniqueId()
                    + "; using packet-only disguise refresh: " + e.getMessage());
        }
    }

    private ProfileSnapshot captureProfile(Player player, String fallbackName) {
        try {
            Object gameProfile = getGameProfile(player);
            String name = (String) gameProfile.getClass().getMethod("getName").invoke(gameProfile);
            Object properties = gameProfile.getClass().getMethod("getProperties").invoke(gameProfile);
            Collection<?> existing = (Collection<?>) properties.getClass().getMethod("get", Object.class).invoke(properties, "textures");
            List<SkinProperty> textures = new ArrayList<>();

            for (Object property : existing) {
                String value = (String) property.getClass().getMethod("getValue").invoke(property);
                String signature = (String) property.getClass().getMethod("getSignature").invoke(property);
                textures.add(new SkinProperty(value, signature));
            }

            return new ProfileSnapshot(name != null ? name : fallbackName, textures);
        } catch (Exception ignored) {
            return new ProfileSnapshot(fallbackName, Collections.emptyList());
        }
    }

    private Object getGameProfile(Player player) throws Exception {
        Object handle = getHandle(player);
        return handle.getClass().getMethod("getProfile").invoke(handle);
    }

    private Object getHandle(Player player) throws Exception {
        return player.getClass().getMethod("getHandle").invoke(player);
    }

    private void setFinalField(Object instance, String fieldName, Object value) throws Exception {
        Field field = findField(instance.getClass(), fieldName);
        field.setAccessible(true);

        try {
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (Exception ignored) {
            // Java 8 permits setting the accessible final field directly; newer runtimes may hide modifiers.
        }

        field.set(instance, value);
    }

    private Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private void refreshPlayer(Player player) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        long refreshToken = System.nanoTime();
        refreshTokens.put(player.getUniqueId(), refreshToken);

        refreshSelf(player, refreshToken);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(player.getUniqueId())) continue;

            if (profile != null && profile.isVanished() && !canSeeVanished(viewer)) {
                viewer.hidePlayer(player);
                continue;
            }

            refreshViewer(viewer, player, refreshToken);
        }

        syncVanishVisibility(player);
    }

    private void refreshViewer(Player viewer, Player player, long refreshToken) {
        boolean packetRemoveSent = sendPacketRemove(viewer, player);
        if (!packetRemoveSent) viewer.hidePlayer(player);

        long delay = Math.max(1L, plugin.getConfig().getLong("disguise.skin-load-delay-ticks", 6L));
        TaskUtil.syncLater(() -> {
            Player online = Bukkit.getPlayer(player.getUniqueId());
            Player onlineViewer = Bukkit.getPlayer(viewer.getUniqueId());
            if (online == null || onlineViewer == null) return;
            if (!Objects.equals(refreshTokens.get(online.getUniqueId()), refreshToken)) return;

            PlayerProfile current = plugin.getPlayerManager().getProfile(online);
            if (current != null && current.isVanished() && !canSeeVanished(onlineViewer)) {
                onlineViewer.hidePlayer(online);
                return;
            }

            boolean packetAddSent = packetRemoveSent && sendPacketAdd(onlineViewer, online);
            if (!packetAddSent) {
                onlineViewer.showPlayer(online);
            }

            long spawnDelay = Math.max(1L, plugin.getConfig().getLong("disguise.entity-spawn-delay-ticks", 2L));
            TaskUtil.syncLater(() -> {
                Player spawnOnline = Bukkit.getPlayer(player.getUniqueId());
                Player spawnViewer = Bukkit.getPlayer(viewer.getUniqueId());
                if (spawnOnline == null || spawnViewer == null) return;
                if (!Objects.equals(refreshTokens.get(spawnOnline.getUniqueId()), refreshToken)) return;
                PlayerProfile spawnProfile = plugin.getPlayerManager().getProfile(spawnOnline);
                if (spawnProfile != null && spawnProfile.isVanished() && !canSeeVanished(spawnViewer)) return;

                if (packetAddSent) sendPacketSpawn(spawnViewer, spawnOnline);
                if (plugin.getProtocolLibHook() != null) plugin.getProtocolLibHook().refreshEntity(spawnViewer, spawnOnline);
                plugin.getNameTagManager().applyNameTag(spawnOnline);
            }, spawnDelay);

            long secondDelay = plugin.getConfig().getLong("disguise.second-refresh-delay-ticks", 12L);
            if (secondDelay > 0L) {
                TaskUtil.syncLater(() -> {
                    Player secondOnline = Bukkit.getPlayer(player.getUniqueId());
                    Player secondViewer = Bukkit.getPlayer(viewer.getUniqueId());
                    if (secondOnline == null || secondViewer == null) return;
                    if (!Objects.equals(refreshTokens.get(secondOnline.getUniqueId()), refreshToken)) return;
                    PlayerProfile secondProfile = plugin.getPlayerManager().getProfile(secondOnline);
                    if (secondProfile != null && secondProfile.isVanished() && !canSeeVanished(secondViewer)) return;

                    sendPacketRemove(secondViewer, secondOnline);
                    TaskUtil.syncLater(() -> {
                        Player finalOnline = Bukkit.getPlayer(player.getUniqueId());
                        Player finalViewer = Bukkit.getPlayer(viewer.getUniqueId());
                        if (finalOnline == null || finalViewer == null) return;
                        if (!Objects.equals(refreshTokens.get(finalOnline.getUniqueId()), refreshToken)) return;
                        boolean finalAddSent = sendPacketAdd(finalViewer, finalOnline);
                        TaskUtil.syncLater(() -> {
                            Player finalSpawnOnline = Bukkit.getPlayer(player.getUniqueId());
                            Player finalSpawnViewer = Bukkit.getPlayer(viewer.getUniqueId());
                            if (finalSpawnOnline == null || finalSpawnViewer == null) return;
                            if (!Objects.equals(refreshTokens.get(finalSpawnOnline.getUniqueId()), refreshToken)) return;
                            if (finalAddSent) sendPacketSpawn(finalSpawnViewer, finalSpawnOnline);
                            if (plugin.getProtocolLibHook() != null) plugin.getProtocolLibHook().refreshEntity(finalSpawnViewer, finalSpawnOnline);
                            plugin.getNameTagManager().applyNameTag(finalSpawnOnline);
                        }, Math.max(1L, plugin.getConfig().getLong("disguise.entity-spawn-delay-ticks", 2L)));
                    }, Math.max(1L, plugin.getConfig().getLong("disguise.skin-load-delay-ticks", 6L)));
                }, secondDelay);
            }
        }, delay);
    }

    private void refreshSelf(Player player, long refreshToken) {
        if (!plugin.getConfig().getBoolean("disguise.self-refresh", true)) return;
        if (!isPacketRefreshEnabled()) return;

        sendPlayerInfoRemove(player, player);

        long delay = Math.max(1L, plugin.getConfig().getLong("disguise.skin-load-delay-ticks", 6L));
        TaskUtil.syncLater(() -> {
            Player online = Bukkit.getPlayer(player.getUniqueId());
            if (online == null) return;
            if (!Objects.equals(refreshTokens.get(online.getUniqueId()), refreshToken)) return;

            sendPacketAdd(online, online);
            sendPacketRespawn(online);
            sendPacketPosition(online);
            sendPacketAbilities(online);
            try {
                online.updateInventory();
            } catch (Exception ignored) {
            }
            plugin.getNameTagManager().applyNameTag(online);
        }, delay);
    }

    private boolean canSeeVanished(Player viewer) {
        return viewer.hasPermission("evaulx.staff") || viewer.hasPermission("evaulx.vanish.see");
    }

    private void setPlayerListName(Player player, String name) {
        try {
            player.setPlayerListName(name);
        } catch (Exception ignored) {
            // Bukkit enforces tab-list name limits; disguise profile refresh still handles the visible name.
        }
    }

    private boolean sendPacketRemove(Player viewer, Player player) {
        if (!isPacketRefreshEnabled()) return false;

        try {
            sendPlayerInfoRemove(viewer, player);
            Object handle = getHandle(player);
            sendPacket(viewer, destroyPacket(handle, player.getEntityId()));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean sendPlayerInfoRemove(Player viewer, Player player) {
        if (!isPacketRefreshEnabled()) return false;

        try {
            sendPacket(viewer, playerInfoPacket(getHandle(player), "REMOVE_PLAYER"));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean sendPacketAdd(Player viewer, Player player) {
        if (!isPacketRefreshEnabled()) return false;

        try {
            Object handle = getHandle(player);
            sendPacket(viewer, playerInfoAddPacket(player, handle));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean sendPacketRespawn(Player player) {
        try {
            Object handle = getHandle(player);
            String nmsPackage = handle.getClass().getPackage().getName();
            Object world = handle.getClass().getMethod("u").invoke(handle);
            Object difficulty = world.getClass().getMethod("getDifficulty").invoke(world);
            Object worldType = world.getClass().getMethod("G").invoke(world);
            Object interactManager = handle.getClass().getField("playerInteractManager").get(handle);
            Object gameMode = interactManager.getClass().getMethod("getGameMode").invoke(interactManager);
            Field worldProviderField = findField(world.getClass(), "worldProvider");
            worldProviderField.setAccessible(true);
            Object worldProvider = worldProviderField.get(world);
            int dimension = (Integer) worldProvider.getClass().getMethod("getDimension").invoke(worldProvider);

            Class<?> packetClass = Class.forName(nmsPackage + ".PacketPlayOutRespawn");
            Class<?> difficultyClass = Class.forName(nmsPackage + ".EnumDifficulty");
            Class<?> worldTypeClass = Class.forName(nmsPackage + ".WorldType");
            Class<?> gameModeClass = Class.forName(nmsPackage + ".WorldSettings$EnumGamemode");
            Object packet = packetClass
                    .getConstructor(int.class, difficultyClass, worldTypeClass, gameModeClass)
                    .newInstance(dimension, difficulty, worldType, gameMode);
            sendPacket(player, packet);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send self disguise respawn for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private boolean sendPacketPosition(Player player) {
        try {
            Object handle = getHandle(player);
            String nmsPackage = handle.getClass().getPackage().getName();
            Class<?> packetClass = Class.forName(nmsPackage + ".PacketPlayOutPosition");
            Object packet = packetClass
                    .getConstructor(double.class, double.class, double.class, float.class, float.class, Set.class)
                    .newInstance(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(),
                            player.getLocation().getYaw(), player.getLocation().getPitch(), Collections.emptySet());
            sendPacket(player, packet);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send self disguise position for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private boolean sendPacketAbilities(Player player) {
        try {
            Object handle = getHandle(player);
            String nmsPackage = handle.getClass().getPackage().getName();
            Field abilitiesField = findField(handle.getClass(), "abilities");
            abilitiesField.setAccessible(true);
            Object abilities = abilitiesField.get(handle);
            Class<?> abilitiesClass = Class.forName(nmsPackage + ".PlayerAbilities");
            Class<?> packetClass = Class.forName(nmsPackage + ".PacketPlayOutAbilities");
            Object packet = packetClass.getConstructor(abilitiesClass).newInstance(abilities);
            sendPacket(player, packet);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send self disguise abilities for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private boolean sendPacketSpawn(Player viewer, Player player) {
        if (!isPacketRefreshEnabled()) return false;

        try {
            Object handle = getHandle(player);
            sendPacket(viewer, namedSpawnPacket(handle));
            sendOptionalSpawnDetails(viewer, player, handle);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void sendOptionalSpawnDetails(Player viewer, Player player, Object handle) {
        try {
            sendPacket(viewer, metadataPacket(handle, player.getEntityId()));
        } catch (Exception ignored) {
        }

        for (int slot = 0; slot <= 4; slot++) {
            try {
                sendPacket(viewer, equipmentPacket(handle, player, slot));
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object playerInfoPacket(Object entityPlayer, String actionName) throws Exception {
        String nmsPackage = entityPlayer.getClass().getPackage().getName();
        Class<?> packetClass = Class.forName(nmsPackage + ".PacketPlayOutPlayerInfo");
        Class<?> actionClass = null;
        for (Class<?> inner : packetClass.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("EnumPlayerInfoAction")) {
                actionClass = inner;
                break;
            }
        }
        if (actionClass == null) throw new ClassNotFoundException("EnumPlayerInfoAction");

        Object action = Enum.valueOf((Class<Enum>) actionClass.asSubclass(Enum.class), actionName);
        Object players = Array.newInstance(entityPlayer.getClass(), 1);
        Array.set(players, 0, entityPlayer);
        Constructor<?> constructor = packetClass.getConstructor(actionClass, players.getClass());
        return constructor.newInstance(action, players);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object playerInfoAddPacket(Player player, Object entityPlayer) throws Exception {
        String nmsPackage = entityPlayer.getClass().getPackage().getName();
        Class<?> packetClass = Class.forName(nmsPackage + ".PacketPlayOutPlayerInfo");
        Class<?> actionClass = null;
        for (Class<?> inner : packetClass.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("EnumPlayerInfoAction")) {
                actionClass = inner;
                break;
            }
        }
        if (actionClass == null) throw new ClassNotFoundException("EnumPlayerInfoAction");

        Object packet = packetClass.getConstructor().newInstance();
        setFinalField(packet, "a", Enum.valueOf((Class<Enum>) actionClass.asSubclass(Enum.class), "ADD_PLAYER"));

        Field listField = findField(packetClass, "b");
        listField.setAccessible(true);
        List list = (List) listField.get(packet);

        Object gameProfile = createPacketProfile(player);
        int ping = entityPlayer.getClass().getField("ping").getInt(entityPlayer);
        Object interactManager = entityPlayer.getClass().getField("playerInteractManager").get(entityPlayer);
        Object gameMode = interactManager.getClass().getMethod("getGameMode").invoke(interactManager);
        Object listName = entityPlayer.getClass().getMethod("getPlayerListName").invoke(entityPlayer);

        Class<?> dataClass = Class.forName(nmsPackage + ".PacketPlayOutPlayerInfo$PlayerInfoData");
        Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
        Class<?> gameModeClass = Class.forName(nmsPackage + ".WorldSettings$EnumGamemode");
        Class<?> chatComponentClass = Class.forName(nmsPackage + ".IChatBaseComponent");
        Constructor<?> dataConstructor = dataClass.getConstructor(packetClass, gameProfileClass, int.class,
                gameModeClass, chatComponentClass);
        list.add(dataConstructor.newInstance(packet, gameProfile, ping, gameMode, listName));
        return packet;
    }

    private Object createPacketProfile(Player player) throws Exception {
        ProfileSnapshot snapshot = visibleProfiles.get(player.getUniqueId());
        if (snapshot == null) return getGameProfile(player);

        Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
        Object gameProfile = gameProfileClass
                .getConstructor(UUID.class, String.class)
                .newInstance(player.getUniqueId(), snapshot.getName());
        Object properties = gameProfileClass.getMethod("getProperties").invoke(gameProfile);
        applyTextures(properties, snapshot.getTextures());
        return gameProfile;
    }

    private void applyTextures(Object properties, List<SkinProperty> textures) throws Exception {
        Collection<?> existing = (Collection<?>) properties.getClass().getMethod("get", Object.class)
                .invoke(properties, "textures");
        existing.clear();

        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
        for (SkinProperty texture : textures) {
            Object property = texture.getSignature() == null || texture.getSignature().isEmpty()
                    ? propertyClass.getConstructor(String.class, String.class)
                    .newInstance("textures", texture.getValue())
                    : propertyClass.getConstructor(String.class, String.class, String.class)
                    .newInstance("textures", texture.getValue(), texture.getSignature());
            properties.getClass().getMethod("put", Object.class, Object.class).invoke(properties, "textures", property);
        }
    }

    private Object destroyPacket(Object entityPlayer, int entityId) throws Exception {
        String nmsPackage = entityPlayer.getClass().getPackage().getName();
        Class<?> packetClass = Class.forName(nmsPackage + ".PacketPlayOutEntityDestroy");
        return packetClass.getConstructor(int[].class).newInstance((Object) new int[]{entityId});
    }

    private Object namedSpawnPacket(Object entityPlayer) throws Exception {
        String nmsPackage = entityPlayer.getClass().getPackage().getName();
        Class<?> packetClass = Class.forName(nmsPackage + ".PacketPlayOutNamedEntitySpawn");
        for (Constructor<?> constructor : packetClass.getConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 1 && parameters[0].isAssignableFrom(entityPlayer.getClass())) {
                return constructor.newInstance(entityPlayer);
            }
        }
        throw new NoSuchMethodException("PacketPlayOutNamedEntitySpawn");
    }

    private Object metadataPacket(Object entityPlayer, int entityId) throws Exception {
        String nmsPackage = entityPlayer.getClass().getPackage().getName();
        Object watcher = entityPlayer.getClass().getMethod("getDataWatcher").invoke(entityPlayer);
        Class<?> watcherClass = Class.forName(nmsPackage + ".DataWatcher");
        Class<?> packetClass = Class.forName(nmsPackage + ".PacketPlayOutEntityMetadata");
        return packetClass.getConstructor(int.class, watcherClass, boolean.class)
                .newInstance(entityId, watcher, true);
    }

    private Object equipmentPacket(Object entityPlayer, Player player, int slot) throws Exception {
        String nmsPackage = entityPlayer.getClass().getPackage().getName();
        String craftPackage = player.getClass().getPackage().getName();
        Class<?> packetClass = Class.forName(nmsPackage + ".PacketPlayOutEntityEquipment");
        Class<?> itemStackClass = Class.forName(nmsPackage + ".ItemStack");
        Class<?> craftItemStackClass = Class.forName(craftPackage + ".inventory.CraftItemStack");

        ItemStack item;
        if (slot == 0) item = player.getItemInHand();
        else item = player.getInventory().getArmorContents()[slot - 1];

        Object nmsItem = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
        return packetClass.getConstructor(int.class, int.class, itemStackClass)
                .newInstance(player.getEntityId(), slot, nmsItem);
    }

    private void sendPacket(Player viewer, Object packet) throws Exception {
        Object viewerHandle = getHandle(viewer);
        Object connection = viewerHandle.getClass().getField("playerConnection").get(viewerHandle);
        for (Method method : connection.getClass().getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (!method.getName().equals("sendPacket") || parameters.length != 1) continue;
            if (!parameters[0].isAssignableFrom(packet.getClass())) continue;
            method.invoke(connection, packet);
            return;
        }
        throw new NoSuchMethodException("sendPacket");
    }

    private SkinProperty fetchSkin(String skinName) {
        String cleanName = cleanName(skinName);
        if (cleanName == null) return null;
        SkinProperty cached = getCachedSkin(cleanName);
        if (cached != null) return cached;

        try {
            JsonObject uuidResponse = readJson("https://api.mojang.com/users/profiles/minecraft/" + cleanName);
            if (uuidResponse == null || !uuidResponse.has("id")) {
                plugin.getLogger().warning("Could not find Mojang UUID for disguise skin '" + cleanName + "'.");
                return null;
            }

            String uuid = uuidResponse.get("id").getAsString();
            JsonObject profileResponse = readJson("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            if (profileResponse == null || !profileResponse.has("properties")) {
                plugin.getLogger().warning("Could not load Mojang texture profile for disguise skin '" + cleanName + "'.");
                return null;
            }

            JsonArray properties = profileResponse.getAsJsonArray("properties");
            for (JsonElement element : properties) {
                JsonObject property = element.getAsJsonObject();
                if (!"textures".equalsIgnoreCase(property.get("name").getAsString())) continue;
                SkinProperty skin = new SkinProperty(
                        property.get("value").getAsString(),
                        property.has("signature") ? property.get("signature").getAsString() : null
                ).cacheAs(cleanName, skinCache);
                saveSkinCache();
                return skin;
            }
            plugin.getLogger().warning("Mojang texture profile for disguise skin '" + cleanName + "' had no textures property.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch skin '" + cleanName + "': " + e.getMessage());
        }

        return null;
    }

    private SkinProperty getOnlineSkin(String cleanName) {
        if (cleanName == null) return null;

        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(online);
            String realName = profile == null ? online.getName() : profile.getName();
            if (!cleanName.equalsIgnoreCase(online.getName()) && !cleanName.equalsIgnoreCase(realName)) continue;

            ProfileSnapshot original = originalProfiles.get(online.getUniqueId());
            List<SkinProperty> textures = original != null
                    ? original.getTextures()
                    : captureProfile(online, realName).getTextures();
            if (textures == null || textures.isEmpty()) continue;
            SkinProperty texture = textures.get(0);
            if (texture != null && texture.getValue() != null && !texture.getValue().isEmpty()) return texture;
        }

        return null;
    }

    private String displayName(PlayerProfile profile, String visibleName) {
        dev.evaulx.core.models.Rank rank = null;
        if (profile.getDisguiseRank() != null) rank = plugin.getRankManager().getRank(profile.getDisguiseRank());
        if (rank == null) rank = plugin.getRankManager().getRank(profile.getRankName());
        String fallback = rank != null ? rank.getColor() : "&f";
        String nameColor = profile.getNameColor() == null || profile.getNameColor().isEmpty()
                ? fallback
                : profile.getNameColor();
        return nameColor + visibleName;
    }

    private JsonObject readJson(String rawUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(rawUrl).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "EvaulxMC/1.0");

        if (connection.getResponseCode() != 200) {
            connection.disconnect();
            return null;
        }

        try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        } finally {
            connection.disconnect();
        }
    }

    private String cleanName(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        if (!trimmed.matches("^[A-Za-z0-9_]{3,16}$")) return null;
        return trimmed;
    }

    private static class ProfileSnapshot {
        private final String name;
        private final List<SkinProperty> textures;

        private ProfileSnapshot(String name, List<SkinProperty> textures) {
            this.name = name;
            this.textures = textures;
        }

        private String getName() { return name; }
        private List<SkinProperty> getTextures() { return textures; }
    }

    private static class SkinProperty {
        private final String value;
        private final String signature;
        private final long cachedAt;

        private SkinProperty(String value, String signature) {
            this(value, signature, System.currentTimeMillis());
        }

        private SkinProperty(String value, String signature, long cachedAt) {
            this.value = value;
            this.signature = signature;
            this.cachedAt = cachedAt;
        }

        private String getValue() { return value; }
        private String getSignature() { return signature; }
        private long getCachedAt() { return cachedAt; }

        private SkinProperty cacheAs(String name, Map<String, SkinProperty> cache) {
            cache.put(name.toLowerCase(Locale.ENGLISH), this);
            return this;
        }
    }

    private static class DisguiseSession {
        private String skin;
        private String rank;
        private String commandName;
        private int skinPage;

        private String getSkin() { return skin; }
        private void setSkin(String skin) { this.skin = skin; }
        private String getRank() { return rank; }
        private void setRank(String rank) { this.rank = rank; }
        private String getCommandName() { return commandName; }
        private void setCommandName(String commandName) { this.commandName = commandName; }
        private int getSkinPage() { return skinPage; }
        private void setSkinPage(int skinPage) { this.skinPage = skinPage; }
    }

    public static class DisguiseHistoryEntry {
        private final String realName;
        private final String disguiseName;
        private final String skinName;
        private final String rankName;
        private final String commandName;
        private final String serverId;
        private final long createdAt;

        private DisguiseHistoryEntry(String realName, String disguiseName, String skinName, String rankName,
                                     String commandName, String serverId, long createdAt) {
            this.realName = realName == null ? "Unknown" : realName;
            this.disguiseName = disguiseName == null ? "Unknown" : disguiseName;
            this.skinName = skinName == null ? "" : skinName;
            this.rankName = rankName == null ? "" : rankName;
            this.commandName = commandName == null ? "disguise" : commandName;
            this.serverId = serverId == null ? "unknown" : serverId;
            this.createdAt = createdAt;
        }

        public String getRealName() { return realName; }
        public String getDisguiseName() { return disguiseName; }
        public String getSkinName() { return skinName; }
        public String getRankName() { return rankName; }
        public String getCommandName() { return commandName; }
        public String getServerId() { return serverId; }
        public long getCreatedAt() { return createdAt; }
    }
}
