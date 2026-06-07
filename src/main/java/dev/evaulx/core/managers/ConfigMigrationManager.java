package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;

public class ConfigMigrationManager {

    private static final int CONFIG_VERSION = 15;

    private final EvaulxCore plugin;

    public ConfigMigrationManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void apply() {
        boolean changed = normalizeRedisConfig();
        plugin.getConfig().options().copyDefaults(true);
        int current = plugin.getConfig().getInt("config-version", 1);
        if (current < CONFIG_VERSION) {
            if (current < 4) {
                plugin.getConfig().set("scoreboard", null);
                plugin.getConfig().set("nametags.tab-enabled", null);
                plugin.getConfig().set("nametags.tab-format", null);
                plugin.getConfig().set("hub-hook.handles-scoreboard", null);
                plugin.getConfig().set("hub-hook.handles-tab", null);
                if (plugin.getConfig().getString("database.flatfile.path", "").equalsIgnoreCase("plugins/EvaulxMC/data/")) {
                    plugin.getConfig().set("database.flatfile.path", "data");
                }
            }
            if (current < 9) {
                changed |= setIfMissing("staff-tools.clickable-alerts", true);
                changed |= setIfMissing("staff-tools.command-spy.enabled", true);
                changed |= setIfMissing("staff-tools.command-spy.hide-staff-commands", true);
                changed |= setIfMissing("staff-tools.command-spy.blocked-commands",
                        java.util.Arrays.asList("login", "register", "changepassword", "password"));
                changed |= setIfMissing("staff-tools.command-spy.format",
                        "&8[&cCommandSpy&8] &f{player}&8: &7/{command}");
                changed |= setIfMissing("staff-tools.staffmode.recovery-snapshots", true);
                changed |= setIfMissing("staff-tools.staff-list.network", true);
            }
            if (current < 10) {
                changed |= setIfMissing("disguise.safe-mode", false);
                changed |= setIfMissing("disguise.self-refresh", true);
                changed |= setIfMissing("disguise.require-real-skin", false);
                changed |= setIfMissing("disguise.persist-on-relog", false);
                changed |= setIfMissing("disguise.auto-undisguise-on-quit", true);
                changed |= setIfMissing("disguise.skin-cache-ttl-minutes", 1440);
                changed |= setIfMissing("disguise.restrictions.enabled", true);
                changed |= setIfMissing("disguise.restrictions.pool-only-names", false);
                changed |= setIfMissing("disguise.restrictions.pool-only-skins", false);
                changed |= setIfMissing("disguise.restrictions.allowed-ranks", java.util.Collections.emptyList());
                changed |= setIfMissing("disguise.blacklist.block-rank-names", true);
                changed |= setIfMissing("disguise.blacklist.reserved-names",
                        java.util.Arrays.asList("Console", "Server", "Mojang", "Minecraft", "Hypixel", "Mineplex",
                                "Notch", "Herobrine", "Dream", "Technoblade", "TommyInnit", "PrestonPlayz", "Unspeakable"));
                changed |= setIfMissing("disguise.blacklist.blocked-contains", java.util.Collections.emptyList());
                changed |= setIfMissing("disguise.gui.active-title", "&8Active Disguises");
            }
            if (current < 11) {
                changed |= setIfMissing("nametags.scoreboard.enabled", true);
                changed |= setIfMissing("nametags.scoreboard.prefix-format", "{tag}{prefix}{namecolor}");
                changed |= setIfMissing("nametags.scoreboard.suffix-format", "{suffix}");
                changed |= setIfMissing("nametags.tab-list.enabled", true);
                changed |= setIfMissing("nametags.tab-list.format", "{prefix}{namecolor}{player}");
                changed |= setIfMissing("nametags.tab-list.max-visible-length", 16);
            }
            if (current < 12) {
                changed |= normalizeRedisConfig();
            }
            if (current < 13) {
                changed |= setIfMissing("exports.keep", 10);
                changed |= setIfMissing("gui.rank-category.title", "&8Rank Category: {rank}");
            }
            if (current < 14) {
                changed |= setIfMissing("hooks.vault.enabled", true);
                changed |= setIfMissing("hooks.luckperms.import-on-startup", false);
                changed |= setIfMissing("disguise.restrictions.require-rank-permission", false);
                changed |= setIfMissing("disguise.restrictions.rank-permission-pattern", "evaulx.disguise.rank.{rank}");
                changed |= setIfMissing("staff-tools.action-log.write-jsonl", true);
                changed |= setIfMissing("staff-tools.grants.expiry-reminder-minutes", 60);
                changed |= setIfMissing("punishments.default-appeal-status", "not-submitted");
            }
            if (current < 15) {
                changed |= setIfMissing("discord.player-head.enabled", true);
                changed |= setIfMissing("discord.player-head.url", "https://mc-heads.net/avatar/{id}/100");
            }
            plugin.getConfig().set("config-version", CONFIG_VERSION);
            changed = true;
        }
        changed |= normalizeRedisConfig();
        if (changed) plugin.saveConfig();
    }

    private boolean setIfMissing(String path, Object value) {
        if (plugin.getConfig().contains(path)) return false;
        plugin.getConfig().set(path, value);
        return true;
    }

    private boolean normalizeRedisConfig() {
        boolean changed = false;
        boolean hasIp = false;
        org.bukkit.configuration.ConfigurationSection redis = plugin.getConfig().getConfigurationSection("redis");
        if (redis != null) hasIp = redis.getKeys(false).contains("ip");

        String host = plugin.getConfig().getString("redis.host", null);
        if (!hasIp) {
            plugin.getConfig().set("redis.ip", host == null || host.trim().isEmpty() ? "localhost" : host);
            changed = true;
        }
        if (plugin.getConfig().contains("redis.host")) {
            plugin.getConfig().set("redis.host", null);
            changed = true;
        }
        if (plugin.getConfig().contains("redis.password")) {
            plugin.getConfig().set("redis.password", null);
            changed = true;
        }
        if (plugin.getConfig().contains("redis.channel")) {
            plugin.getConfig().set("redis.channel", null);
            changed = true;
        }
        return changed;
    }
}
