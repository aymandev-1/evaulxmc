package dev.evaulx.core.models;

import java.util.*;

public class Rank {

    private static final List<String> ALLOWED_CATEGORIES = Collections.unmodifiableList(Arrays.asList(
            "Staff", "Media", "Store", "Hidden", "Default"
    ));

    private String name;
    private String display;
    private String category;
    private String permission;
    private String prefix;
    private String suffix;
    private String color;
    private int weight;
    private boolean defaultRank;
    private boolean staff;
    private boolean hidden;
    private List<String> permissions;
    private List<String> inheritance;

    public Rank(String name) {
        this.name = name;
        this.display = "";
        this.category = "Default";
        this.permission = "";
        this.prefix = "";
        this.suffix = "";
        this.color = "&f";
        this.weight = 0;
        this.defaultRank = false;
        this.staff = false;
        this.hidden = false;
        this.permissions = new ArrayList<>();
        this.inheritance = new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplay() { return display == null ? "" : display; }
    public void setDisplay(String display) { this.display = display == null ? "" : display; }

    public String getCategory() { return normalizeCategory(category); }
    public void setCategory(String category) { this.category = normalizeCategory(category); }

    public String getPermission() { return permission == null ? "" : permission; }
    public void setPermission(String permission) { this.permission = permission == null ? "" : permission; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public boolean isDefault() { return defaultRank; }
    public void setDefault(boolean defaultRank) { this.defaultRank = defaultRank; }

    public boolean isStaff() { return staff; }
    public void setStaff(boolean staff) { this.staff = staff; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    public void addPermission(String perm) { if (!permissions.contains(perm)) permissions.add(perm); }
    public void removePermission(String perm) { permissions.remove(perm); }

    public List<String> getInheritance() { return inheritance; }
    public void setInheritance(List<String> inheritance) { this.inheritance = inheritance; }
    public void addInheritance(String rank) { if (!inheritance.contains(rank)) inheritance.add(rank); }

    public String getDisplayName() {
        String value = getDisplay().isEmpty() ? color + name : getDisplay();
        return dev.evaulx.core.utils.CC.color(value);
    }

    public static List<String> getAllowedCategories() {
        return ALLOWED_CATEGORIES;
    }

    public static boolean isAllowedCategory(String category) {
        if (category == null) return false;
        for (String allowed : ALLOWED_CATEGORIES) {
            if (allowed.equalsIgnoreCase(category.trim())) return true;
        }
        return false;
    }

    public static String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) return "Default";
        String cleaned = category.trim().replace('_', '-');
        for (String allowed : ALLOWED_CATEGORIES) {
            if (allowed.equalsIgnoreCase(cleaned)) return allowed;
        }
        return "Default";
    }

    public static String inferCategory(Rank rank) {
        if (rank == null) return "Default";
        if (rank.isHidden()) return "Hidden";
        if (rank.isDefault()) return "Default";
        if (rank.isStaff()) return "Staff";

        String name = rank.getName() == null ? "" : rank.getName().toLowerCase(Locale.ENGLISH);
        if (name.contains("youtuber") || name.contains("youtube") || name.contains("twitch")) return "Media";
        if (name.contains("evaulx") || name.contains("vip")) return "Store";
        return "Default";
    }
}
