package dev.evaulx.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class PlayerUtil {

    private PlayerUtil() {
    }

    @SuppressWarnings("deprecation")
    public static OfflinePlayer getOfflinePlayer(String name) {
        return Bukkit.getOfflinePlayer(name);
    }
}
