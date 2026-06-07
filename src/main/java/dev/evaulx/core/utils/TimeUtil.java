package dev.evaulx.core.utils;

public class TimeUtil {

    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) return -1L;
        input = input.trim();
        String lower = input.toLowerCase();
        if (lower.equals("permanent") || lower.equals("perm") || lower.equals("-1")) return -1L;

        // NOTE: case is preserved here on purpose. Lowercase 'm' means minutes while
        // uppercase 'M' means months; every other unit is treated case-insensitively.
        long total = 0;
        StringBuilder num = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                if (num.length() == 0) continue;
                long value = Long.parseLong(num.toString());
                num.setLength(0);
                switch (c) {
                    case 's': case 'S': total += value * 1000L; break;
                    case 'm':           total += value * 60_000L; break;
                    case 'h': case 'H': total += value * 3_600_000L; break;
                    case 'd': case 'D': total += value * 86_400_000L; break;
                    case 'w': case 'W': total += value * 604_800_000L; break;
                    case 'M':           total += value * 2_592_000_000L; break;
                    case 'y': case 'Y': total += value * 31_536_000_000L; break;
                }
            }
        }
        return total == 0 ? -1L : System.currentTimeMillis() + total;
    }

    public static String formatDuration(long expires) {
        if (expires == -1L) return "Permanent";
        long diff = expires - System.currentTimeMillis();
        if (diff <= 0) return "Expired";

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours   = minutes / 60;
        long days    = hours / 24;
        long weeks   = days / 7;

        if (weeks > 0)   return weeks + "w " + (days % 7) + "d";
        if (days > 0)    return days + "d " + (hours % 24) + "h";
        if (hours > 0)   return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }

    public static boolean isExpired(long expires) {
        return expires != -1L && System.currentTimeMillis() >= expires;
    }
}
