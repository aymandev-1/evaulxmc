package dev.evaulx.core.utils;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.Bukkit;

public class TaskUtil {

    public static void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(EvaulxCore.getInstance(), runnable);
    }

    public static void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(EvaulxCore.getInstance(), runnable);
    }

    public static void syncLater(Runnable runnable, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(EvaulxCore.getInstance(), runnable, delayTicks);
    }

    public static void asyncRepeat(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(EvaulxCore.getInstance(), runnable, delay, period);
    }

    public static void syncRepeat(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimer(EvaulxCore.getInstance(), runnable, delay, period);
    }
}
