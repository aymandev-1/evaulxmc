package dev.evaulx.core.utils;

import org.bukkit.ChatColor;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class DisplayUtil {

    private static final Map<String, String> COLORS = new LinkedHashMap<>();

    static {
        COLORS.put("black", "&0");
        COLORS.put("darkblue", "&1");
        COLORS.put("darkgreen", "&2");
        COLORS.put("darkaqua", "&3");
        COLORS.put("darkred", "&4");
        COLORS.put("purple", "&5");
        COLORS.put("gold", "&6");
        COLORS.put("gray", "&7");
        COLORS.put("darkgray", "&8");
        COLORS.put("blue", "&9");
        COLORS.put("green", "&a");
        COLORS.put("aqua", "&b");
        COLORS.put("red", "&c");
        COLORS.put("pink", "&d");
        COLORS.put("yellow", "&e");
        COLORS.put("white", "&f");
    }

    private DisplayUtil() {
    }

    public static String colorCode(String input) {
        if (input == null) return null;
        String normalized = input.toLowerCase(Locale.ENGLISH).replace("_", "").replace("-", "");
        if (normalized.equals("reset") || normalized.equals("none") || normalized.equals("clear")) return "";
        if (COLORS.containsKey(normalized)) return COLORS.get(normalized);
        if (normalized.matches("[0-9a-f]")) return "&" + normalized;
        if (normalized.matches("&[0-9a-f]")) return normalized;
        if (normalized.matches("§[0-9a-f]")) return "&" + normalized.charAt(1);
        return null;
    }

    public static String colorNames() {
        return String.join(", ", COLORS.keySet());
    }

    public static String stripFormat(String input) {
        if (input == null) return "";
        String colored = ChatColor.translateAlternateColorCodes('&', input);
        return ChatColor.stripColor(colored);
    }

    public static String limitMinecraftText(String input, int maxLength) {
        if (input == null) return "";
        if (maxLength <= 0) return "";
        if (input.length() <= maxLength) return input;

        StringBuilder builder = new StringBuilder(maxLength);
        for (int i = 0; i < input.length() && builder.length() < maxLength; i++) {
            char current = input.charAt(i);
            if ((current == ChatColor.COLOR_CHAR || current == '&') && i + 1 < input.length()) {
                if (builder.length() + 2 > maxLength) break;
                builder.append(current).append(input.charAt(++i));
                continue;
            }
            builder.append(current);
        }

        if (builder.length() > 0) {
            char last = builder.charAt(builder.length() - 1);
            if (last == ChatColor.COLOR_CHAR || last == '&') {
                builder.deleteCharAt(builder.length() - 1);
            }
        }
        return builder.toString();
    }

    public static String limitWithTrailingColor(String input, String trailingColor, int maxLength) {
        if (input == null) input = "";
        if (trailingColor == null) trailingColor = "";
        if (maxLength <= 0) return "";
        if (trailingColor.length() >= maxLength) return limitMinecraftText(trailingColor, maxLength);

        int baseLength = maxLength - trailingColor.length();
        return limitMinecraftText(input, baseLength) + trailingColor;
    }
}
