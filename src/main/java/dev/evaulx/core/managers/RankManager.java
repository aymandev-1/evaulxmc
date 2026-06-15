package dev.evaulx.core.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Rank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class RankManager {

    private static final List<String> EVAULX_PRESET_RANKS = Collections.unmodifiableList(Arrays.asList(
            "Owner", "Platform-Admin", "Admin", "Developer", "Senior-Mod", "Mod", "Builder",
            "Youtuber", "Twitch", "Phase", "Zecon", "Evaulx", "VIP", "Default"
    ));

    private final EvaulxCore plugin;
    private final Map<String, Rank> ranks = new LinkedHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public RankManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void loadRanks() {
        ranks.clear();
        List<Rank> loaded = plugin.getDatabaseManager().loadRanks();
        for (Rank r : loaded) ranks.put(r.getName().toLowerCase(), r);

        if (ranks.isEmpty()) {
            installEvaulxPresetRanks();
            plugin.getLogger().info("Created EvaulxMC preset ranks.");
        }
        plugin.getLogger().info("Loaded " + ranks.size() + " ranks.");
    }

    public int installEvaulxPresetRanks() {
        backupRanks("preset-reset");
        deleteAllRanks();
        int installed = 0;
        installed += savePreset("Owner", "Staff", "&7[&4Owner&7] &4&o", "", "&4", 1000, true, false,
                perms("evaulx.*", "evaulx.admin", "evaulx.rank", "evaulx.staff", "evaulx.staffchat"), inherits());
        installed += savePreset("Platform-Admin", "Staff", "&7[&cPlatform-Admin&7] &c", "", "&c", 950, true, false,
                perms("evaulx.admin", "evaulx.rank", "evaulx.perm", "evaulx.grant", "evaulx.staff"), inherits());
        installed += savePreset("Admin", "Staff", "&7[&cAdmin&7] &c&o", "", "&c", 900, true, false,
                perms("evaulx.rank", "evaulx.perm", "evaulx.grant", "evaulx.maintenance", "evaulx.staff"), inherits());
        installed += savePreset("Developer", "Staff", "&7[&1Developer&7] &1&o", "", "&1", 850, true, false,
                perms("evaulx.admin", "evaulx.permaudit", "evaulx.staffdashboard", "evaulx.buildmode", "evaulx.staff"), inherits());
        installed += savePreset("Senior-Mod", "Staff", "&7[&dSenior-Mod&7] &d", "", "&d", 750, true, false,
                perms("evaulx.ban", "evaulx.tempban", "evaulx.unban", "evaulx.staff"), inherits());
        installed += savePreset("Mod", "Staff", "&7[&5Mod&7] &5&o", "", "&5", 650, true, false,
                perms("evaulx.kick", "evaulx.warn", "evaulx.mute", "evaulx.staff"), inherits());
        installed += savePreset("Builder", "Staff", "&7[&bBuilder&7] &b", "", "&b", 550, true, false,
                perms("evaulx.buildmode", "evaulx.protection.bypass", "evaulx.setspawn", "evaulx.staff"), inherits());
        installed += savePreset("Youtuber", "Media", "&7[&cYOU&fTUBE&7] &c", "", "&c", 450, false, false,
                perms(), inherits());
        installed += savePreset("Twitch", "Media", "&7[&5Twitch&7] &f", "", "&5", 425, false, false,
                perms(), inherits());
        installed += savePreset("Phase", "Store", "&7[&8Phase&7] &8", "", "&8", 415, false, false,
                perms(), inherits());
        installed += savePreset("Zecon", "Store", "&7[&3Zecon&7] &3", "", "&3", 413, false, false,
                perms(), inherits());
        installed += savePreset("Evaulx", "Store", "&7[&6Evaulx&7] &6", "", "&6", 350, false, false,
                perms(), inherits());
        installed += savePreset("VIP", "Store", "&7[&aVIP&7] &a", "", "&a", 100, false, false,
                perms(), inherits());
        installed += savePreset("Default", "Default", "&7", "", "&7", 0, false, false,
                perms(), inherits());
        return installed;
    }

    public File backupRanks(String reason) {
        File backupDir = new File(plugin.getEvaulxDataFolder(), "backups");
        if (!backupDir.exists()) backupDir.mkdirs();

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File file = new File(backupDir, "ranks-" + stamp + "-" + reason + ".json");
        JsonObject root = new JsonObject();
        root.addProperty("createdAt", System.currentTimeMillis());
        root.addProperty("reason", reason);

        JsonArray array = new JsonArray();
        for (Rank rank : ranks.values()) {
            array.add(toJson(rank));
        }
        root.add("ranks", array);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(root, writer);
            rotateBackups();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to back up ranks: " + e.getMessage());
        }
        return file;
    }

    public List<File> listBackups() {
        File backupDir = getBackupDirectory();
        File[] files = backupDir.listFiles((dir, name) -> name.startsWith("ranks-") && name.endsWith(".json"));
        List<File> backups = files == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(files));
        backups.sort((first, second) -> {
            int time = Long.compare(second.lastModified(), first.lastModified());
            if (time != 0) return time;
            return second.getName().compareToIgnoreCase(first.getName());
        });
        return backups;
    }

    public int restoreBackup(String fileName) {
        File file;
        try {
            file = resolveBackup(fileName);
        } catch (IOException e) {
            plugin.getLogger().warning("Blocked invalid rank backup path: " + fileName);
            return -1;
        }
        if (file == null || !file.exists() || !file.isFile()) return -1;

        List<Rank> restored = new ArrayList<>();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("ranks");
            if (array == null) return -1;
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                Rank rank = fromJson(element.getAsJsonObject());
                if (rank != null) restored.add(rank);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read rank backup " + file.getName() + ": " + e.getMessage());
            return -1;
        }

        backupRanks("before-rollback");
        deleteAllRanks();
        for (Rank rank : restored) {
            saveRank(rank);
        }
        return restored.size();
    }

    private File getBackupDirectory() {
        File backupDir = new File(plugin.getEvaulxDataFolder(), "backups");
        if (!backupDir.exists()) backupDir.mkdirs();
        return backupDir;
    }

    private File resolveBackup(String fileName) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) return null;
        File backupDir = getBackupDirectory();
        File file = new File(backupDir, fileName.trim());
        String dirPath = backupDir.getCanonicalPath();
        String filePath = file.getCanonicalPath();
        if (!filePath.startsWith(dirPath + File.separator)) throw new IOException("Path outside backup directory");
        return file;
    }

    private void rotateBackups() {
        int keep = plugin.getConfig().getInt("rank-backups.keep", 25);
        if (keep < 1) return;

        List<File> backups = listBackups();
        for (int i = keep; i < backups.size(); i++) {
            File file = backups.get(i);
            if (!file.delete()) {
                plugin.getLogger().warning("Could not delete old rank backup: " + file.getName());
            }
        }
    }

    private void deleteAllRanks() {
        List<String> names = new ArrayList<>();
        for (Rank rank : ranks.values()) {
            names.add(rank.getName());
        }
        for (String name : names) {
            deleteRank(name);
        }
        ranks.clear();
    }

    private int savePreset(String name, String category, String prefix, String suffix, String color, int weight, boolean staff, boolean hidden,
                           List<String> permissions, List<String> inheritance) {
        Rank rank = getRank(name);
        if (rank == null) rank = new Rank(name);
        rank.setName(name);
        rank.setDisplay(defaultDisplay(name, color));
        rank.setCategory(category);
        rank.setPermission(rankPermission(name));
        rank.setPrefix(prefix);
        rank.setSuffix(suffix);
        rank.setColor(color);
        rank.setWeight(weight);
        rank.setStaff(staff);
        rank.setDefault(name.equalsIgnoreCase("Default"));
        rank.setHidden(hidden);
        rank.getPermissions().clear();
        rank.getInheritance().clear();

        for (String permission : permissions) {
            rank.addPermission(permission);
        }
        for (String parent : inheritance) {
            rank.addInheritance(parent);
        }

        saveRank(rank);
        return 1;
    }

    private String defaultDisplay(String name, String color) {
        if (name.equalsIgnoreCase("Owner")) return "&7[&4Owner&7] &4&oOwner";
        if (name.equalsIgnoreCase("Platform-Admin")) return "&7[&cPlatform-Admin&7] &cPlatform-Admin";
        if (name.equalsIgnoreCase("Admin")) return "&7[&cAdmin&7] &c&oAdmin";
        if (name.equalsIgnoreCase("Developer")) return "&7[&1Developer&7] &1&oDeveloper";
        if (name.equalsIgnoreCase("Phase")) return "&7[&8Phase&7] &8Phase";
        if (name.equalsIgnoreCase("Zecon")) return "&7[&3Zecon&7] &3Zecon";
        if (name.equalsIgnoreCase("Senior-Mod")) return "&7[&dSenior-Mod&7] &dSenior-Mod";
        if (name.equalsIgnoreCase("Mod")) return "&7[&5Mod&7] &5&oMod";
        if (name.equalsIgnoreCase("Builder")) return "&7[&bBuilder&7] &bBuilder";
        if (name.equalsIgnoreCase("Youtuber")) return "&7[&cYOU&fTUBE&7] &cYoutuber";
        if (name.equalsIgnoreCase("Twitch")) return "&7[&5Twitch&7] &fTwitch";
        if (name.equalsIgnoreCase("Evaulx")) return "&7[&6Evaulx&7] &6Evaulx";
        if (name.equalsIgnoreCase("VIP")) return "&7[&aVIP&7] &aVIP";
        if (name.equalsIgnoreCase("Default")) return "&7Default";
        return color + name;
    }

    private String rankPermission(String name) {
        if (name.equalsIgnoreCase("Default")) return "";
        return "evaulxmc.rank." + name.toLowerCase(Locale.ENGLISH).replace("_", "-");
    }

    private List<String> perms(String... permissions) {
        return Arrays.asList(permissions);
    }

    private List<String> inherits(String... inheritance) {
        return Arrays.asList(inheritance);
    }

    private JsonObject toJson(Rank rank) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", rank.getName());
        obj.addProperty("display", rank.getDisplay());
        obj.addProperty("category", rank.getCategory());
        obj.addProperty("permission", rank.getPermission());
        obj.addProperty("prefix", rank.getPrefix());
        obj.addProperty("suffix", rank.getSuffix());
        obj.addProperty("color", rank.getColor());
        obj.addProperty("weight", rank.getWeight());
        obj.addProperty("default", rank.isDefault());
        obj.addProperty("staff", rank.isStaff());
        obj.addProperty("hidden", rank.isHidden());

        JsonArray permissions = new JsonArray();
        for (String permission : rank.getPermissions()) permissions.add(new JsonPrimitive(permission));
        obj.add("permissions", permissions);

        JsonArray inheritance = new JsonArray();
        for (String parent : rank.getInheritance()) inheritance.add(new JsonPrimitive(parent));
        obj.add("inheritance", inheritance);
        return obj;
    }

    private Rank fromJson(JsonObject obj) {
        if (!obj.has("name") || obj.get("name").isJsonNull()) return null;
        Rank rank = new Rank(obj.get("name").getAsString());
        String category = getString(obj, "category", "");
        rank.setDisplay(getString(obj, "display", ""));
        rank.setPermission(getString(obj, "permission", ""));
        rank.setPrefix(getString(obj, "prefix", ""));
        rank.setSuffix(getString(obj, "suffix", ""));
        rank.setColor(getString(obj, "color", "&f"));
        rank.setWeight(getInt(obj, "weight", 0));
        rank.setDefault(getBoolean(obj, "default", false));
        rank.setStaff(getBoolean(obj, "staff", false));
        rank.setHidden(getBoolean(obj, "hidden", false));
        rank.setCategory(category.isEmpty() ? Rank.inferCategory(rank) : category);

        JsonArray permissions = obj.getAsJsonArray("permissions");
        if (permissions != null) {
            for (JsonElement element : permissions) {
                if (!element.isJsonNull()) rank.addPermission(element.getAsString());
            }
        }

        JsonArray inheritance = obj.getAsJsonArray("inheritance");
        if (inheritance != null) {
            for (JsonElement element : inheritance) {
                if (!element.isJsonNull()) rank.addInheritance(element.getAsString());
            }
        }
        return rank;
    }

    private String getString(JsonObject obj, String key, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsString();
    }

    private int getInt(JsonObject obj, String key, int fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsInt();
    }

    private boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsBoolean();
    }

    public void saveRank(Rank rank) {
        String category = getRankCategory(rank);
        rank.setCategory(category);
        rank.setHidden(category.equals("Hidden"));
        if (category.equals("Staff")) rank.setStaff(true);
        ranks.put(rank.getName().toLowerCase(), rank);
        plugin.getDatabaseManager().saveRank(rank);
    }

    public void deleteRank(String name) {
        ranks.remove(name.toLowerCase());
        plugin.getDatabaseManager().deleteRank(name);
    }

    public Rank getRank(String name) {
        return ranks.get(name.toLowerCase());
    }

    public Rank getDefaultRank() {
        return ranks.values().stream().filter(Rank::isDefault).findFirst()
                .orElse(ranks.values().stream().findFirst().orElse(null));
    }

    public Collection<Rank> getRanks() {
        return ranks.values();
    }

    public Collection<Rank> getVisibleRanks() {
        List<Rank> visible = new ArrayList<>();
        for (Rank r : ranks.values()) if (!r.isHidden()) visible.add(r);
        return visible;
    }

    public List<Rank> getVisibleRanksByWeight() {
        List<Rank> sorted = new ArrayList<>();
        for (Rank r : ranks.values()) if (!r.isHidden()) sorted.add(r);
        sorted.sort((first, second) -> {
            int weight = Integer.compare(second.getWeight(), first.getWeight());
            if (weight != 0) return weight;
            return first.getName().compareToIgnoreCase(second.getName());
        });
        return sorted;
    }

    public List<Rank> getRanksByWeight() {
        List<Rank> sorted = new ArrayList<>(ranks.values());
        sorted.sort((first, second) -> {
            int weight = Integer.compare(second.getWeight(), first.getWeight());
            if (weight != 0) return weight;
            return first.getName().compareToIgnoreCase(second.getName());
        });
        return sorted;
    }

    public boolean rankExists(String name) {
        return ranks.containsKey(name.toLowerCase());
    }

    public List<String> getAllowedCategories() {
        return Rank.getAllowedCategories();
    }

    public String getRankCategory(Rank rank) {
        if (rank == null) return "Default";
        if (rank.isHidden()) return "Hidden";
        return Rank.normalizeCategory(rank.getCategory());
    }

    public List<String> getEvaulxPresetRankNames() {
        return EVAULX_PRESET_RANKS;
    }

    public Set<String> getRankNameSet() {
        Set<String> names = new LinkedHashSet<>();
        for (Rank rank : ranks.values()) names.add(rank.getName());
        return names;
    }

    public List<String> getMissingPresetRanks() {
        List<String> missing = new ArrayList<>();
        for (String name : EVAULX_PRESET_RANKS) {
            if (!rankExists(name)) missing.add(name);
        }
        return missing;
    }

    public List<String> getUnexpectedPresetRanks() {
        List<String> unexpected = new ArrayList<>();
        Set<String> allowed = new HashSet<>();
        for (String name : EVAULX_PRESET_RANKS) allowed.add(name.toLowerCase(Locale.ENGLISH));
        for (Rank rank : ranks.values()) {
            if (!allowed.contains(rank.getName().toLowerCase(Locale.ENGLISH))) unexpected.add(rank.getName());
        }
        return unexpected;
    }

    public List<String> getAllPermissions(Rank rank) {
        LinkedHashSet<String> permissions = new LinkedHashSet<>();
        collectPermissions(rank, permissions, new HashSet<String>());
        return new ArrayList<>(permissions);
    }

    private void collectPermissions(Rank rank, Set<String> permissions, Set<String> visited) {
        if (rank == null) return;
        String key = rank.getName().toLowerCase(Locale.ENGLISH);
        if (!visited.add(key)) return;

        permissions.addAll(rank.getPermissions());
        for (String inherited : rank.getInheritance()) {
            collectPermissions(getRank(inherited), permissions, visited);
        }
    }
}
