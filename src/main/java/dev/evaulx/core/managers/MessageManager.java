package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.ActionBarUtil;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class MessageManager {

    private final EvaulxCore plugin;
    private final Map<String, String> defaults = new HashMap<>();
    private final List<YamlConfiguration> customMessages = new ArrayList<>();

    private YamlConfiguration messages;
    private int actionBarTaskId = -1;

    public MessageManager(EvaulxCore plugin) {
        this.plugin = plugin;
        loadDefaults();
    }

    public void load() {
        File baseFile = new File(plugin.getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(baseFile);
        applyMissingDefaults(baseFile);

        customMessages.clear();
        File customDir = new File(plugin.getCustomFolder(), "messages");
        File[] files = customDir.listFiles((dir, name) -> name.toLowerCase(Locale.ENGLISH).endsWith(".yml"));
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File file : files) customMessages.add(YamlConfiguration.loadConfiguration(file));
        }
    }

    public void reload() {
        load();
        startActionBarTask();
    }

    private void applyMissingDefaults(File baseFile) {
        boolean changed = false;
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            if (messages.contains(entry.getKey())) continue;
            messages.set(entry.getKey(), entry.getValue());
            changed = true;
        }

        if (!changed) return;
        try {
            messages.save(baseFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to migrate messages.yml defaults: " + e.getMessage());
        }
    }

    public void shutdown() {
        stopActionBarTask();
    }

    public void startActionBarTask() {
        stopActionBarTask();
        if (!getBoolean("action-bars.enabled", true)) return;
        if (!getBoolean("action-bars.status.enabled", true)) return;

        long interval = Math.max(10L, getLong("action-bars.status.interval-ticks", 40L));
        actionBarTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (plugin.getPlayerManager() == null) return;
            for (Player player : Bukkit.getOnlinePlayers()) sendStatusActionBar(player);
        }, interval, interval);
    }

    public void stopActionBarTask() {
        if (actionBarTaskId == -1) return;
        Bukkit.getScheduler().cancelTask(actionBarTaskId);
        actionBarTaskId = -1;
    }

    public String get(String path, String fallback) {
        return get(path, fallback, null);
    }

    public String get(String path, String fallback, Map<String, String> placeholders) {
        String raw = getRawString(path);
        if (raw == null) raw = defaults.get(path);
        if (raw == null) raw = fallback;
        return CC.color(applyPlaceholders(raw, placeholders));
    }

    public void send(CommandSender sender, String path, String fallback) {
        send(sender, path, fallback, null);
    }

    public void send(CommandSender sender, String path, String fallback, Map<String, String> placeholders) {
        if (sender == null) return;

        List<String> lines = getRawStringList(path);
        if (lines == null || lines.isEmpty()) {
            String message = get(path, fallback, placeholders);
            if (!message.trim().isEmpty()) sender.sendMessage(message);
            return;
        }

        for (String line : lines) {
            String message = CC.color(applyPlaceholders(line, placeholders));
            if (!message.trim().isEmpty()) sender.sendMessage(message);
        }
    }

    public void actionBar(Player player, String path, String fallback) {
        actionBar(player, path, fallback, null);
    }

    public void actionBar(Player player, String path, String fallback, Map<String, String> placeholders) {
        if (player == null || !getBoolean("action-bars.enabled", true)) return;
        String message = get(path, fallback, placeholders);
        if (message.trim().isEmpty()) return;
        ActionBarUtil.send(player, message);
    }

    public Map<String, String> placeholders(String... values) {
        Map<String, String> placeholders = new HashMap<>();
        if (values == null) return placeholders;
        for (int i = 0; i + 1 < values.length; i += 2) {
            placeholders.put(values[i], values[i + 1] == null ? "" : values[i + 1]);
        }
        return placeholders;
    }

    private void sendStatusActionBar(Player player) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null) return;

        boolean staffMode = profile.isStaffMode();
        boolean vanished = profile.isVanished();
        boolean disguised = profile.isDisguised();
        if (getBoolean("action-bars.status.only-when-active", true) && !staffMode && !vanished && !disguised) return;

        String disguiseName = disguised && profile.getDisguiseName() != null ? profile.getDisguiseName() : "none";
        String format = get("action-bars.status.format",
                "&cStaff Mode: {staffmode} &8| &cVanish: {vanish} &8| &cNick: &f{nick}",
                placeholders(
                        "{staffmode}", stateText(staffMode),
                        "{vanish}", stateText(vanished),
                        "{disguise}", disguised ? "&aON" : "&cOFF",
                        "{nick}", disguiseName
                ));
        if (!format.trim().isEmpty()) ActionBarUtil.send(player, format);
    }

    private String stateText(boolean state) {
        return state ? get("state.enabled", "&aON") : get("state.disabled", "&cOFF");
    }

    private boolean getBoolean(String path, boolean fallback) {
        Object value = findValue(path);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return fallback;
    }

    private long getLong(String path, long fallback) {
        Object value = findValue(path);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String getRawString(String path) {
        Object value = findValue(path);
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private List<String> getRawStringList(String path) {
        Object value = findValue(path);
        if (!(value instanceof List)) return null;

        List<String> lines = new ArrayList<>();
        for (Object entry : (List<Object>) value) lines.add(String.valueOf(entry));
        return lines;
    }

    private Object findValue(String path) {
        for (int i = customMessages.size() - 1; i >= 0; i--) {
            YamlConfiguration custom = customMessages.get(i);
            if (custom.contains(path)) return custom.get(path);
        }

        if (messages != null && messages.contains(path)) return messages.get(path);
        return null;
    }

    private String applyPlaceholders(String message, Map<String, String> placeholders) {
        if (message == null) return "";
        String rendered = message;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                rendered = rendered.replace(entry.getKey(), entry.getValue());
            }
        }
        return rendered;
    }

    private void loadDefaults() {
        defaults.put("prefix", "&7[&cEvaulx&7] &r");
        defaults.put("no-permission", "&cYou do not have permission to perform this action.");
        defaults.put("players-only", "&cOnly players can use this command.");
        defaults.put("player-not-found", "&cPlayer not found.");
        defaults.put("player-not-online", "&cPlayer not online.");
        defaults.put("profile-loading", "&cThat player's profile is still loading.");
        defaults.put("state.enabled", "&aON");
        defaults.put("state.disabled", "&cOFF");
        defaults.put("state.enabled-word", "&aenabled");
        defaults.put("state.disabled-word", "&cdisabled");
        defaults.put("staffmode.usage", "&cUsage: /staffmode [player] [on|off]");
        defaults.put("staffmode.enabled", "&aStaff mode enabled&7. You are now in creative mode.");
        defaults.put("staffmode.disabled", "&7Staff mode &cdisabled&7.");
        defaults.put("staffmode.already", "&7Staff mode is already {state} &7for &f{target}&7.");
        defaults.put("staffmode.set-other", "&7Set staff mode for &f{target} &7to {state}&7.");
        defaults.put("staffrecover.no-snapshot", "&cNo staff mode recovery snapshot exists for &f{target}&c.");
        defaults.put("staffrecover.failed", "&cCould not restore staff mode snapshot for &f{target}&c.");
        defaults.put("staffrecover.restored", "&7Recovered staff mode inventory/state for &f{target}&7.");
        defaults.put("staffrecover.restored-target", "&7Your staff mode inventory/state was recovered by &f{staff}&7.");
        defaults.put("commandspy.enabled", "&7Command spy &aenabled&7.");
        defaults.put("commandspy.disabled", "&7Command spy &cdisabled&7.");
        defaults.put("vanish.usage", "&cUsage: /vanish [player] [on|off]");
        defaults.put("vanish.enabled", "&7You are now &cvanished&7.");
        defaults.put("vanish.disabled", "&7You are now &fvisible&7.");
        defaults.put("vanish.already", "&7Vanish is already {state} &7for &f{target}&7.");
        defaults.put("vanish.set-other", "&7Set vanish for &f{target} &7to {state}&7.");
        defaults.put("vanish.staff-notify", "&8[&cStaff&8] &f{player} &7is now {state}&7.");
        defaults.put("disguise.disabled", "&cDisguises are currently disabled.");
        defaults.put("disguise.usage", "&cUsage: /{command} <name|random|reload|clearskincache|debug|refresh|status|menu> [skin] [rank]");
        defaults.put("disguise.skin-usage", "&cUsage: /{command} <skin>");
        defaults.put("disguise.debug-usage", "&cUsage: /{command} debug <player>");
        defaults.put("disguise.refresh-usage", "&cUsage: /{command} refresh <player>");
        defaults.put("disguise.invalid-name", "&cDisguise name must be 3-16 letters, numbers, or underscores.");
        defaults.put("disguise.duplicate-name", "&cThat disguise name is already in use.");
        defaults.put("disguise.blacklisted-name", "&cThat disguise name is blocked.");
        defaults.put("disguise.restricted-name", "&cYou can only use names from your disguise name pool.");
        defaults.put("disguise.restricted-skin", "&cYou can only use skins from your disguise skin pool.");
        defaults.put("disguise.restricted-rank", "&cYou cannot disguise with that rank.");
        defaults.put("disguise.cooldown", "&cPlease wait {seconds}s before changing your disguise again.");
        defaults.put("disguise.rank-not-found", "&cRank not found.");
        defaults.put("disguise.skin-permission", "&cYou do not have permission to choose disguise skins.");
        defaults.put("disguise.rank-permission", "&cYou do not have permission to choose disguise ranks.");
        defaults.put("disguise.enabled", "&8[&cDisguise&8] &fYou are now disguised as &c{name}&f.");
        defaults.put("disguise.disabled-self", "&8[&cDisguise&8] &fYou are no longer disguised.");
        defaults.put("disguise.not-disguised", "&cYou are not disguised.");
        defaults.put("disguise.reloaded", "&aDisguise settings reloaded.");
        defaults.put("disguise.skin-cache-cleared", "&aDisguise skin cache cleared. &7Removed &f{count}&7 cached skin(s).");
        defaults.put("disguise.skin-loading", "&7Loading skin &f{skin}&7...");
        defaults.put("disguise.skin-loaded", "&aSkin loaded: &f{skin}&a.");
        defaults.put("disguise.skin-failed", "&cCould not load skin &f{skin}&c. Use a real Minecraft username.");
        defaults.put("disguise.skin-changed", "&aUpdated disguise skin to &f{skin}&a.");
        defaults.put("disguise.refreshed", "&aRefreshed disguise for &f{target}&a.");
        defaults.put("disguise.staff-alert", "&8[&cStaff&8] &f{player} &7used &f/{command} &7as &c{name} &7skin &f{skin} &7rank &f{rank}&7 on &f{server}&7.");
        defaults.put("disguise.staff-undisguise-alert", "&8[&cStaff&8] &f{player} &7removed their disguise on &f{server}&7.");
        defaults.put("disguise.name-cancelled", "&cDisguise name selection cancelled.");
        defaults.put("disguise.name-prompt", "&7Type your disguise name in chat, or type &ccancel&7.");
        defaults.put("disguise.name-invalid", "&cName must be 3-16 letters, numbers, or underscores. Type another name or 'cancel'.");
        defaults.put("disguise.selection-expired", "&cYour disguise selection expired. Use /disguise again.");
        defaults.put("disguise.forced-undisguise", "&7Removed &f{target}&7's disguise.");
        defaults.put("disguise.force-not-disguised", "&cThat player is not disguised.");
        defaults.put("disguise.realname-not-found", "&cNo online disguised player matched &f{query}&c.");
        defaults.put("staff-alerts.join-local", "&8[&cStaff&8] &f{player} &7joined this server.");
        defaults.put("staff-alerts.leave-local", "&8[&cStaff&8] &f{player} &7left this server.");
        defaults.put("staff-alerts.join-network", "&8[&cNetwork&8] &f{player} &7joined &f{server}&7.");
        defaults.put("staff-alerts.leave-network", "&8[&cNetwork&8] &f{player} &7left &f{server}&7.");
        defaults.put("staff-tools.no-target-block", "&cNo target block found.");
        defaults.put("staff-tools.teleport-compass", "&7Teleported to target block.");
        defaults.put("staff-tools.no-random-target", "&cNo non-staff players online.");
        defaults.put("staff-tools.random-teleport", "&7Randomly teleported to &f{target}&7.");
        defaults.put("staff-tools.inspect", "&7Opened &f{target}&7's inventory.");
        defaults.put("staff-tools.frozen-reminder", "&cYou are frozen. &7Reason: &f{reason}");
        defaults.put("action-bars.staffmode.enabled", "&aStaff mode enabled &8| &fGamemode: &cCREATIVE");
        defaults.put("action-bars.staffmode.disabled", "&cStaff mode disabled");
        defaults.put("action-bars.vanish.enabled", "&cVanish enabled &8| &fHidden from players");
        defaults.put("action-bars.vanish.disabled", "&aVanish disabled &8| &fYou are visible");
        defaults.put("action-bars.disguise.enabled", "&cDisguised as &f{name}");
        defaults.put("action-bars.disguise.disabled", "&cDisguise removed");
        defaults.put("action-bars.disguise.skin-loaded", "&aSkin loaded: &f{skin}");
        defaults.put("action-bars.disguise.skin-failed", "&cSkin failed: &f{skin}");
    }
}
