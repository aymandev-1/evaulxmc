package dev.evaulx.core.utils;

import org.bukkit.ChatColor;

public class CC {

    public static final String PRIMARY   = "&c";
    public static final String SECONDARY = "&7";
    public static final String SUCCESS   = "&a";
    public static final String ERROR     = "&c";
    public static final String GOLD      = "&6";
    public static final String WHITE     = "&f";
    public static final String SEPARATOR = "&8&m----------------------------------------";

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String strip(String message) {
        return ChatColor.stripColor(color(message));
    }

    public static String prefix(String module) {
        return color("&8[&c" + module + "&8] &f");
    }
}
