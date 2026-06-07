package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.managers.ConfigMigrationManager;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class EvaulxCommand implements CommandExecutor, TabCompleter {

    private static final List<String> PLACEHOLDERS = Arrays.asList(
            "%evaulx_player_name%",
            "%evaulx_visible_name%",
            "%evaulx_player_real_name%",
            "%evaulx_real_name%",
            "%evaulx_player_rank%",
            "%evaulx_player_rank_display%",
            "%evaulx_player_rank_display_raw%",
            "%evaulx_player_rank_category%",
            "%evaulx_player_rank_permission%",
            "%evaulx_player_rank_priority%",
            "%evaulx_player_rank_weight%",
            "%evaulx_player_rank_staff%",
            "%evaulx_player_rank_default%",
            "%evaulx_player_rank_hidden%",
            "%evaulx_player_prefix%",
            "%evaulx_player_prefix_plain%",
            "%evaulx_player_suffix%",
            "%evaulx_player_suffix_plain%",
            "%evaulx_player_color%",
            "%evaulx_player_name_color_rank%",
            "%evaulx_player_tag%",
            "%evaulx_player_tag_plain%",
            "%evaulx_player_has_tag%",
            "%evaulx_player_chat_color%",
            "%evaulx_player_name_color%",
            "%evaulx_player_buildmode%",
            "%evaulx_player_vanished%",
            "%evaulx_player_staffmode%",
            "%evaulx_player_godmode%",
            "%evaulx_player_disguised%",
            "%evaulx_player_disguise_name%",
            "%evaulx_player_disguise_skin%",
            "%evaulx_player_disguise_rank%",
            "%evaulx_player_first_join%",
            "%evaulx_player_last_seen%",
            "%evaulx_server_name%",
            "%evaulx_server_id%",
            "%evaulx_online%",
            "%evaulx_ranks_loaded%",
            "%evaulx_ranks_visible%",
            "%evaulx_nametags_enabled%",
            "%evaulx_nametags_scoreboard_enabled%",
            "%evaulx_tablist_enabled%",
            "%evaulx_disguise_online%",
            "%evaulx_disguise_skin_cache_size%",
            "%evaulx_disguise_safe_mode%",
            "%evaulx_grants_active%",
            "%evaulx_grants_total%",
            "%evaulx_pending_grants%",
            "%evaulx_lookup_cache_size%",
            "%evaulx_staff_sessions_active%",
            "%evaulx_maintenance_enabled%",
            "%evaulx_maintenance_reason%",
            "%evaulx_lobby_protection_enabled%",
            "%evaulx_lobby_protection_world%",
            "%evaulx_lobby_protection_worlds%",
            "%evaulx_lobby_protection_mode%"
    );

    private final EvaulxCore plugin;

    public EvaulxCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.admin")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                new ConfigMigrationManager(plugin).apply();
                plugin.getMessageManager().reload();
                plugin.getRankManager().loadRanks();
                plugin.getGrantManager().load();
                plugin.getNoteManager().load();
                plugin.getPlayerLookupManager().load();
                if (plugin.getHubHook() != null) plugin.getHubHook().load();
                if (plugin.getProtocolLibHook() != null) plugin.getProtocolLibHook().load();
                if (plugin.getVaultHook() != null) {
                    plugin.getVaultHook().unload();
                    plugin.getVaultHook().load();
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    plugin.getPlayerManager().applyPermissions(p, plugin.getPlayerManager().getProfile(p));
                    plugin.getNameTagManager().applyNameTag(p);
                }
                plugin.getMessageManager().send(sender, "config-reloaded", "&aConfiguration reloaded.");
                plugin.getStaffRequestManager().logAction(sender.getName(), "RELOAD", "EvaulxMC", "Reloaded config, messages, ranks, hooks, grants, notes, and lookup cache");
                break;

            case "debug":
                sender.sendMessage(CC.color("&8&m------------------------------"));
                sender.sendMessage(CC.color("&cEvaulxMC Debug Info"));
                sender.sendMessage(CC.color("&8&m------------------------------"));
                sender.sendMessage(CC.color("&7Version: &f" + plugin.getDescription().getVersion()));
                sender.sendMessage(CC.color("&7Database: &f" + plugin.getDatabaseManager().getType().name()));
                sender.sendMessage(CC.color("&7Redis: &f" + (plugin.getRedisSyncManager() != null && plugin.getRedisSyncManager().isEnabled() ? "enabled" : "disabled")));
                sender.sendMessage(CC.color("&7EvaulxMCHub: &f" + (plugin.getHubHook() != null && plugin.getHubHook().isHooked()
                        ? "hooked (" + plugin.getHubHook().getHookMode() + ")"
                        : "not hooked")));
                sender.sendMessage(CC.color("&7PlaceholderAPI: &f" + (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? "hooked" : "not installed")));
                sender.sendMessage(CC.color("&7Ranks loaded: &f" + plugin.getRankManager().getRanks().size()));
                sender.sendMessage(CC.color("&7Stored grants: &f" + plugin.getGrantManager().getStoredGrantCount()
                        + " &7(active &f" + plugin.getGrantManager().getActiveGrantCount() + "&7)"));
                sender.sendMessage(CC.color("&7Pending grants: &f" + plugin.getGrantManager().getPendingGrants().size()));
                sender.sendMessage(CC.color("&7Lookup cache: &f" + plugin.getPlayerLookupManager().size()));
                sender.sendMessage(CC.color("&7Staff sessions active: &f" + plugin.getStaffRequestManager().getActiveStaffSessionCount()));
                sender.sendMessage(CC.color("&7Maintenance: &f" + plugin.getConfig().getBoolean("maintenance.enabled", false)));
                sender.sendMessage(CC.color("&7Stored notes: &f" + plugin.getNoteManager().getStoredNoteCount()));
                sender.sendMessage(CC.color("&7Punishment presets: &f" + plugin.getPunishmentPresetManager().getPresets().size()));
                sender.sendMessage(CC.color("&7Online profiles: &f" + plugin.getPlayerManager().getOnlineProfiles().size()));
                sender.sendMessage(CC.color("&7Online players: &f" + Bukkit.getOnlinePlayers().size()));
                sendWarnings(sender);
                sender.sendMessage(CC.color("&8&m------------------------------"));
                break;

            case "doctor":
                sendDoctor(sender);
                break;

            case "setup":
                sendSetup(sender, args);
                break;

            case "export":
            case "backup":
            case "backupdata":
                exportData(sender);
                break;

            case "restore":
                if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                    sender.sendMessage(CC.color("&cUsage: /evaulxmc restore <export-file.zip> confirm"));
                    sender.sendMessage(CC.color("&7Only data/ and staff/ files are restored. A safety export is created first."));
                    break;
                }
                restoreDataExport(sender, args[1]);
                break;

            case "commands":
            case "cmds":
                sendCommands(sender, args.length > 1 ? parsePage(args[1]) : 1);
                break;

            case "placeholders":
            case "papi":
                sendPlaceholders(sender, args.length > 1 ? parsePage(args[1]) : 1);
                break;

            case "version":
            case "ver":
                sender.sendMessage(CC.color("&cEvaulxMC &fv" + plugin.getDescription().getVersion() + " &7by EvaulxMC"));
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(CC.color("&8&m------------------------------"));
        s.sendMessage(CC.color("&cEvaulxMC &fv" + plugin.getDescription().getVersion()));
        s.sendMessage(CC.color("&8&m------------------------------"));
        s.sendMessage(CC.color("  &c/evaulxmc reload &7- Reload config and ranks"));
        s.sendMessage(CC.color("  &c/evaulxmc debug &7- Show debug info"));
        s.sendMessage(CC.color("  &c/evaulxmc doctor &7- Check setup and config issues"));
        s.sendMessage(CC.color("  &c/evaulxmc setup [quick] &7- Show or apply setup defaults"));
        s.sendMessage(CC.color("  &c/evaulxmc export &7- Zip config, custom files, and flatfile data"));
        s.sendMessage(CC.color("  &c/evaulxmc restore <zip> confirm &7- Restore exported data files"));
        s.sendMessage(CC.color("  &c/evaulxmc commands [page] &7- List plugin commands"));
        s.sendMessage(CC.color("  &c/evaulxmc placeholders [page] &7- List PlaceholderAPI placeholders"));
        s.sendMessage(CC.color("  &c/evaulxmc version &7- Show version"));
        s.sendMessage(CC.color("&8&m------------------------------"));
    }

    private void sendSetup(CommandSender sender, String[] args) {
        if (args.length > 1 && args[1].equalsIgnoreCase("quick")) {
            plugin.getConfig().set("database.type", "FLATFILE");
            plugin.getConfig().set("redis.enabled", false);
            plugin.getConfig().set("hooks.vault.enabled", true);
            plugin.getConfig().set("discord.enabled", false);
            plugin.getConfig().set("disguise.enabled", true);
            plugin.getConfig().set("nametags.enabled", true);
            plugin.getConfig().set("staff-tools.action-log.enabled", true);
            plugin.saveConfig();
            new ConfigMigrationManager(plugin).apply();
            reloadRuntimeState();
            plugin.getStaffRequestManager().logAction(sender.getName(), "SETUP_QUICK", "EvaulxMC", "Applied safe flatfile defaults");
            sender.sendMessage(CC.color("&aApplied quick setup defaults. &7Review database, Discord, and Redis before production use."));
            return;
        }

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cEvaulxMC Setup"));
        sender.sendMessage(CC.color("&7Database: &f" + plugin.getDatabaseManager().getType().name()));
        sender.sendMessage(CC.color("&7Redis: &f" + (plugin.getRedisSyncManager() != null && plugin.getRedisSyncManager().isEnabled())));
        sender.sendMessage(CC.color("&7Discord: &f" + plugin.getConfig().getBoolean("discord.enabled", false)));
        sender.sendMessage(CC.color("&7Vault: &f" + (plugin.getVaultHook() != null && plugin.getVaultHook().isHooked())));
        sender.sendMessage(CC.color("&7PlaceholderAPI: &f" + Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")));
        sender.sendMessage(CC.color("&7ProtocolLib: &f" + (plugin.getProtocolLibHook() != null && plugin.getProtocolLibHook().isHooked())));
        sender.sendMessage(CC.color("&7Default rank: &f" + (plugin.getRankManager().getDefaultRank() == null ? "missing" : plugin.getRankManager().getDefaultRank().getName())));
        sender.sendMessage(CC.color("&7Ranks loaded: &f" + plugin.getRankManager().getRanks().size()));
        sender.sendMessage(CC.color("&7Action logs: &f" + plugin.getConfig().getBoolean("staff-tools.action-log.enabled", true)
                + " &7JSONL: &f" + plugin.getConfig().getBoolean("staff-tools.action-log.write-jsonl", true)));
        sender.sendMessage(CC.color("&7Use &f/evaulxmc setup quick &7for safe local defaults."));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void exportData(CommandSender sender) {
        File file;
        try {
            file = createDataExport();
        } catch (IOException e) {
            sender.sendMessage(CC.color("&cData export failed: &f" + e.getMessage()));
            return;
        }

        sender.sendMessage(CC.color("&aCreated EvaulxMC data export: &f" + file.getName()
                + " &7(" + (file.length() / 1024L) + " KB)"));
        sender.sendMessage(CC.color("&7Saved in &f" + file.getParentFile().getPath()));
    }

    private void restoreDataExport(CommandSender sender, String fileName) {
        File exportsDir = new File(plugin.getDataFolder(), "exports");
        File source = new File(exportsDir, fileName);
        try {
            String exportsPath = exportsDir.getCanonicalPath();
            String sourcePath = source.getCanonicalPath();
            if (!sourcePath.startsWith(exportsPath) || !source.exists() || !source.getName().endsWith(".zip")) {
                sender.sendMessage(CC.color("&cExport file not found in &f" + exportsDir.getPath() + "&c."));
                return;
            }

            File safety = createDataExport();
            int restored = restoreZipEntries(source);
            reloadRuntimeState();
            plugin.getStaffRequestManager().logAction(sender.getName(), "RESTORE_DATA", source.getName(),
                    restored + " files restored; safety backup " + safety.getName());
            sender.sendMessage(CC.color("&aRestored &f" + restored + " &adata file(s) from &f" + source.getName() + "&a."));
            sender.sendMessage(CC.color("&7Safety export created first: &f" + safety.getName()));
        } catch (IOException e) {
            sender.sendMessage(CC.color("&cRestore failed: &f" + e.getMessage()));
        }
    }

    private int restoreZipEntries(File source) throws IOException {
        int restored = 0;
        String rootPath = plugin.getDataFolder().getCanonicalPath();
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(source))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || !isRestorableEntry(name)) continue;
                File target = new File(plugin.getDataFolder(), name);
                String targetPath = target.getCanonicalPath();
                if (!targetPath.startsWith(rootPath)) continue;
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Could not create " + parent.getPath());
                }
                try (FileOutputStream output = new FileOutputStream(target)) {
                    int read;
                    while ((read = zip.read(buffer)) != -1) output.write(buffer, 0, read);
                }
                restored++;
            }
        }
        return restored;
    }

    private boolean isRestorableEntry(String name) {
        return name.startsWith("data/players/")
                || name.startsWith("data/punishments/")
                || name.startsWith("data/ranks/")
                || name.equals("data/grants.json")
                || name.equals("data/pending-grants.json")
                || name.startsWith("staff/");
    }

    private void reloadRuntimeState() {
        plugin.getRankManager().loadRanks();
        plugin.getGrantManager().load();
        plugin.getNoteManager().load();
        plugin.getPlayerLookupManager().load();
        plugin.getStaffRequestManager().load();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().loadProfile(player);
            plugin.getPlayerManager().applyPermissions(player, profile);
            plugin.getNameTagManager().applyNameTag(player);
        }
    }

    private File createDataExport() throws IOException {
        File exportsDir = new File(plugin.getDataFolder(), "exports");
        if (!exportsDir.exists() && !exportsDir.mkdirs()) {
            throw new IOException("Could not create exports folder");
        }

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File exportFile = new File(exportsDir, "evaulxmc-export-" + stamp + ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(exportFile))) {
            addMetadata(zip);
            zipDirectory(plugin.getDataFolder(), plugin.getDataFolder(), exportsDir, zip);
        }
        rotateExports(exportsDir);
        return exportFile;
    }

    private void addMetadata(ZipOutputStream zip) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("EvaulxMC export\n");
        builder.append("createdAt=").append(System.currentTimeMillis()).append('\n');
        builder.append("version=").append(plugin.getDescription().getVersion()).append('\n');
        builder.append("database=").append(plugin.getDatabaseManager().getType().name()).append('\n');
        builder.append("ranks=").append(plugin.getRankManager().getRanks().size()).append('\n');
        builder.append("visibleRanks=").append(plugin.getRankManager().getVisibleRanks().size()).append('\n');
        builder.append("storedGrants=").append(plugin.getGrantManager().getStoredGrantCount()).append('\n');
        builder.append("storedNotes=").append(plugin.getNoteManager().getStoredNoteCount()).append('\n');
        builder.append("databaseNote=Flatfile profile and punishment files are included when database.type is FLATFILE.\n");

        ZipEntry entry = new ZipEntry("export-info.txt");
        zip.putNextEntry(entry);
        zip.write(builder.toString().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void zipDirectory(File root, File current, File exportsDir, ZipOutputStream zip) throws IOException {
        if (current.getCanonicalPath().equals(exportsDir.getCanonicalPath())) return;

        File[] files = current.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(root, file, exportsDir, zip);
                continue;
            }
            String relative = root.toURI().relativize(file.toURI()).getPath();
            if (relative == null || relative.trim().isEmpty()) continue;
            ZipEntry entry = new ZipEntry(relative);
            zip.putNextEntry(entry);
            try (FileInputStream input = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    zip.write(buffer, 0, read);
                }
            }
            zip.closeEntry();
        }
    }

    private void rotateExports(File exportsDir) {
        int keep = plugin.getConfig().getInt("exports.keep", 10);
        if (keep < 1) return;

        File[] files = exportsDir.listFiles((dir, name) -> name.startsWith("evaulxmc-export-") && name.endsWith(".zip"));
        if (files == null || files.length <= keep) return;
        List<File> exports = new ArrayList<>(Arrays.asList(files));
        exports.sort((first, second) -> Long.compare(second.lastModified(), first.lastModified()));
        for (int i = keep; i < exports.size(); i++) {
            if (!exports.get(i).delete()) {
                plugin.getLogger().warning("Could not delete old export: " + exports.get(i).getName());
            }
        }
    }

    private void sendCommands(CommandSender sender, int page) {
        Map<String, Map<String, Object>> commands = plugin.getDescription().getCommands();
        List<String> names = new ArrayList<>(commands.keySet());
        Collections.sort(names);
        int perPage = 12;
        int pages = Math.max(1, (int) Math.ceil(names.size() / (double) perPage));
        page = Math.max(1, Math.min(page, pages));
        int from = Math.min((page - 1) * perPage, names.size());
        int to = Math.min(from + perPage, names.size());

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cEvaulxMC Commands &7(Page " + page + "/" + pages + ")"));
        for (String name : names.subList(from, to)) {
            Object description = commands.get(name).get("description");
            sender.sendMessage(CC.color("&f/" + name + " &7- " + (description == null ? "No description" : description.toString())));
        }
        if (page < pages) sender.sendMessage(CC.color("&7Next page: &f/evaulxmc commands " + (page + 1)));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void sendPlaceholders(CommandSender sender, int page) {
        int perPage = 10;
        int pages = Math.max(1, (int) Math.ceil(PLACEHOLDERS.size() / (double) perPage));
        page = Math.max(1, Math.min(page, pages));
        int from = Math.min((page - 1) * perPage, PLACEHOLDERS.size());
        int to = Math.min(from + perPage, PLACEHOLDERS.size());

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cEvaulxMC Placeholders &7(Page " + page + "/" + pages + ")"));
        for (String placeholder : PLACEHOLDERS.subList(from, to)) {
            sender.sendMessage(CC.color("&f" + placeholder));
        }
        if (page < pages) sender.sendMessage(CC.color("&7Next page: &f/evaulxmc placeholders " + (page + 1)));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private int parsePage(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private String printable(String value) {
        return value == null || value.trim().isEmpty() ? "none" : value;
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(value);
        }
        return builder.toString();
    }

    private void sendWarnings(CommandSender sender) {
        if (plugin.getConfig().getBoolean("redis.enabled", false)
                && (plugin.getRedisSyncManager() == null || !plugin.getRedisSyncManager().isEnabled())) {
            sender.sendMessage(CC.color("&cWarning: Redis is enabled in config but not connected."));
        }
        if (plugin.getConfig().getBoolean("discord.enabled", false)
                && plugin.getConfig().getString("discord.webhooks.punishments", "").isEmpty()) {
            sender.sendMessage(CC.color("&eWarning: Discord is enabled but punishment webhook is empty."));
        }
        if (plugin.getPunishmentPresetManager().getPresets().isEmpty()) {
            sender.sendMessage(CC.color("&eWarning: No punishment presets are configured."));
        }
        if (plugin.getConfig().getBoolean("hub-hook.enabled", true)
                && (plugin.getHubHook() == null || !plugin.getHubHook().isHooked())) {
            sender.sendMessage(CC.color("&eWarning: EvaulxMCHub hook is enabled but EvaulxMCHub is not loaded."));
        }
    }

    private void sendDoctor(CommandSender sender) {
        int issues = 0;

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cEvaulxMC Doctor"));
        sender.sendMessage(CC.color("&7Version: &f" + plugin.getDescription().getVersion()));

        issues += doctorLine(sender,
                plugin.getDatabaseManager() != null && plugin.getDatabaseManager().getDatabase() != null,
                "Database is connected using &f" + plugin.getDatabaseManager().getType().name());

        boolean redisConfigured = plugin.getConfig().getBoolean("redis.enabled", false);
        boolean redisConnected = plugin.getRedisSyncManager() != null && plugin.getRedisSyncManager().isEnabled();
        issues += doctorLine(sender, !redisConfigured || redisConnected,
                redisConfigured ? "Redis is connected for network sync." : "Redis is disabled.");
        issues += checkRedisConfig(sender);
        issues += checkDiscordWebhooks(sender);

        boolean protocolEnabled = plugin.getConfig().getBoolean("disguise.protocollib.enabled", true);
        boolean protocolInstalled = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
        boolean protocolHooked = plugin.getProtocolLibHook() != null && plugin.getProtocolLibHook().isHooked();
        issues += doctorLine(sender, !protocolEnabled || protocolHooked,
                protocolEnabled
                        ? "ProtocolLib disguise hook: &f" + (protocolHooked ? "hooked" : protocolInstalled ? "installed but hook failed" : "not installed")
                        : "ProtocolLib disguise hook disabled.");

        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            sender.sendMessage(CC.color("&eWARN &7PlaceholderAPI is not installed, placeholders will not register."));
        } else {
            sender.sendMessage(CC.color("&aOK &7PlaceholderAPI is installed."));
        }

        boolean vaultConfigured = plugin.getConfig().getBoolean("hooks.vault.enabled", true);
        boolean vaultInstalled = Bukkit.getPluginManager().isPluginEnabled("Vault");
        boolean vaultHooked = plugin.getVaultHook() != null && plugin.getVaultHook().isHooked();
        issues += doctorLine(sender, !vaultConfigured || vaultHooked,
                vaultConfigured
                        ? "Vault rank/chat provider: &f" + (vaultHooked ? "hooked" : vaultInstalled ? "installed but not hooked" : "not installed")
                        : "Vault hook disabled.");

        issues += doctorLine(sender, plugin.getRankManager().getRanks().size() > 0,
                "Ranks loaded: &f" + plugin.getRankManager().getRanks().size());
        issues += checkDefaultRank(sender);
        issues += checkRankCategories(sender);
        issues += checkPresetRankSet(sender);
        issues += doctorLine(sender, plugin.getNameTagManager() != null,
                "Nametag manager loaded. Scoreboard teams: &f" + plugin.getNameTagManager().isScoreboardEnabled()
                        + " &7Tab list: &f" + plugin.getNameTagManager().isTabListEnabled());
        issues += doctorLine(sender, plugin.getDisguiseManager() != null,
                "Disguise manager loaded with &f" + plugin.getDisguiseManager().getSkinCacheSize() + " &7cached skins.");

        issues += checkMaterial(sender, "disguise.gui.border.material");
        issues += checkMaterial(sender, "disguise.gui.rank.material");
        issues += checkMaterial(sender, "disguise.gui.name.custom-item.material");
        issues += checkMaterial(sender, "disguise.gui.name.random-item.material");
        issues += checkStaffModeItems(sender);
        issues += checkDisguisePatterns(sender);
        issues += checkRankPools(sender);
        issues += checkTagRankReferences(sender);
        issues += checkGrantRankReferences(sender);
        issues += checkExportFolder(sender);

        sender.sendMessage(CC.color(issues == 0
                ? "&aDoctor complete: no blocking issues found."
                : "&cDoctor complete: found " + issues + " issue(s) to fix."));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private int doctorLine(CommandSender sender, boolean ok, String message) {
        sender.sendMessage(CC.color((ok ? "&aOK &7" : "&cISSUE &7") + message));
        return ok ? 0 : 1;
    }

    private int checkRedisConfig(CommandSender sender) {
        int issues = 0;
        boolean legacyKeys = plugin.getConfig().contains("redis.host")
                || plugin.getConfig().contains("redis.password")
                || plugin.getConfig().contains("redis.channel");
        issues += doctorLine(sender, !legacyKeys, "Redis config uses only &fenabled&7, &fip&7, and &fport&7.");
        if (!plugin.getConfig().getBoolean("redis.enabled", false)) return issues;
        String ip = plugin.getConfig().getString("redis.ip", "");
        int port = plugin.getConfig().getInt("redis.port", -1);
        issues += doctorLine(sender, ip != null && !ip.trim().isEmpty(),
                "Redis IP is configured as &f" + printable(ip));
        issues += doctorLine(sender, port > 0 && port <= 65535,
                "Redis port is in range: &f" + port);
        return issues;
    }

    private int checkDiscordWebhooks(CommandSender sender) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) {
            sender.sendMessage(CC.color("&aOK &7Discord webhooks are disabled."));
            return 0;
        }

        int issues = 0;
        String[] keys = {"punishments", "rank-changes", "helpop", "reports", "staff-actions", "system", "security"};
        for (String key : keys) {
            String value = plugin.getConfig().getString("discord.webhooks." + key, "");
            boolean ok = value != null && (value.isEmpty() || value.startsWith("https://"));
            issues += doctorLine(sender, ok, "Discord webhook &f" + key + " &7is "
                    + (value == null || value.isEmpty() ? "empty" : "configured"));
        }
        return issues;
    }

    private int checkDefaultRank(CommandSender sender) {
        int defaults = 0;
        String defaultName = "none";
        for (Rank rank : plugin.getRankManager().getRanks()) {
            if (!rank.isDefault()) continue;
            defaults++;
            defaultName = rank.getName();
        }
        return doctorLine(sender, defaults == 1,
                "Default rank count: &f" + defaults + " &7selected: &f" + defaultName);
    }

    private int checkRankCategories(CommandSender sender) {
        int issues = 0;
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (String category : Rank.getAllowedCategories()) counts.put(category, 0);

        for (Rank rank : plugin.getRankManager().getRanks()) {
            String category = plugin.getRankManager().getRankCategory(rank);
            if (!counts.containsKey(category)) {
                issues += doctorLine(sender, false, "Rank &f" + rank.getName() + " &7has invalid category &f" + category);
                continue;
            }
            counts.put(category, counts.get(category) + 1);
        }

        sender.sendMessage(CC.color("&aOK &7Rank categories: &fStaff " + counts.get("Staff")
                + "&7, &fMedia " + counts.get("Media")
                + "&7, &fStore " + counts.get("Store")
                + "&7, &fHidden " + counts.get("Hidden")
                + "&7, &fDefault " + counts.get("Default")));
        return issues;
    }

    private int checkPresetRankSet(CommandSender sender) {
        List<String> missing = plugin.getRankManager().getMissingPresetRanks();
        List<String> unexpected = plugin.getRankManager().getUnexpectedPresetRanks();
        int issues = 0;
        issues += doctorLine(sender, missing.isEmpty(),
                "Preset ranks present" + (missing.isEmpty() ? "." : ": missing &f" + join(missing)));
        issues += doctorLine(sender, unexpected.isEmpty(),
                "Only EvaulxMC preset ranks are loaded" + (unexpected.isEmpty() ? "." : ": extra &f" + join(unexpected)));
        return issues;
    }

    private int checkMaterial(CommandSender sender, String path) {
        String materialName = plugin.getConfig().getString(path, "");
        return doctorLine(sender, materialName != null && !materialName.trim().isEmpty()
                        && Material.matchMaterial(materialName) != null,
                "Material &f" + path + " &7= &f" + materialName);
    }

    private int checkStaffModeItems(CommandSender sender) {
        int issues = 0;
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("staff-tools.staffmode-items.items");
        if (section == null) {
            return doctorLine(sender, false, "No staff mode items are configured.");
        }

        Set<Integer> slots = new HashSet<>();
        for (String key : section.getKeys(false)) {
            String base = "staff-tools.staffmode-items.items." + key;
            if (!plugin.getConfig().getBoolean(base + ".enabled", true)) continue;

            int slot = plugin.getConfig().getInt(base + ".slot", -1);
            issues += doctorLine(sender, slot >= 0 && slot <= 8 && slots.add(slot),
                    "Staff mode item &f" + key + " &7slot &f" + slot);
            issues += checkMaterial(sender, base + ".material");
        }
        return issues;
    }

    private int checkDisguisePatterns(CommandSender sender) {
        int issues = 0;
        for (String pattern : plugin.getConfig().getStringList("disguise.blacklist.blocked-patterns")) {
            try {
                Pattern.compile(pattern);
                sender.sendMessage(CC.color("&aOK &7Disguise blacklist pattern: &f" + pattern));
            } catch (PatternSyntaxException e) {
                issues += doctorLine(sender, false, "Invalid disguise blacklist pattern: &f" + pattern);
            }
        }
        return issues;
    }

    private int checkRankPools(CommandSender sender) {
        int issues = 0;
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("disguise.rank-pools");
        if (section == null) return 0;

        for (String rankName : section.getKeys(false)) {
            issues += doctorLine(sender, plugin.getRankManager().getRank(rankName) != null,
                    "Disguise rank pool references rank &f" + rankName);
        }
        return issues;
    }

    private int checkTagRankReferences(CommandSender sender) {
        int issues = 0;
        ConfigurationSection catalog = plugin.getConfig().getConfigurationSection("tags.catalog");
        if (catalog == null) return 0;

        for (String id : catalog.getKeys(false)) {
            for (String rankName : plugin.getConfig().getStringList("tags.catalog." + id + ".ranks")) {
                issues += doctorLine(sender, plugin.getRankManager().getRank(rankName) != null,
                        "Tag &f" + id + " &7references rank &f" + rankName);
            }
        }
        return issues;
    }

    private int checkGrantRankReferences(CommandSender sender) {
        int invalid = plugin.getGrantManager().countInvalidRankReferences(plugin.getRankManager().getRankNameSet());
        return doctorLine(sender, invalid == 0,
                "Grant records reference loaded ranks" + (invalid == 0 ? "." : ": &f" + invalid + " &7invalid reference(s)"));
    }

    private int checkExportFolder(CommandSender sender) {
        File exports = new File(plugin.getDataFolder(), "exports");
        boolean ok = (exports.exists() || exports.mkdirs()) && exports.canWrite();
        return doctorLine(sender, ok, "Exports folder is writable: &f" + exports.getPath());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.admin")) return Collections.emptyList();
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            Collections.addAll(suggestions, "reload", "debug", "doctor", "setup", "export", "backup", "restore",
                    "commands", "placeholders", "version");
            return filter(suggestions, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
            Collections.addAll(suggestions, "quick");
            return filter(suggestions, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("restore")) {
            File exports = new File(plugin.getDataFolder(), "exports");
            File[] files = exports.listFiles((dir, name) -> name.startsWith("evaulxmc-export-") && name.endsWith(".zip"));
            if (files != null) for (File file : files) suggestions.add(file.getName());
            return filter(suggestions, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("restore")) {
            Collections.addAll(suggestions, "confirm");
            return filter(suggestions, args[2]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("commands") || args[0].equalsIgnoreCase("placeholders"))) {
            Collections.addAll(suggestions, "1", "2", "3", "4", "5");
            return filter(suggestions, args[1]);
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
