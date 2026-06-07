package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TaskUtil;
import org.bukkit.Bukkit;

import java.util.List;

public class TipsManager {

    private final EvaulxCore plugin;
    private int index = 0;

    public TipsManager(EvaulxCore plugin) {
        this.plugin = plugin;
        start();
    }

    private void start() {
        if (!plugin.getConfig().getBoolean("tips.enabled", true)) return;
        long interval = plugin.getConfig().getLong("tips.interval", 300) * 20L;
        TaskUtil.syncRepeat(() -> {
            List<String> messages = plugin.getConfig().getStringList("tips.messages");
            if (messages.isEmpty()) return;
            if (index >= messages.size()) index = 0;
            Bukkit.broadcastMessage(CC.color(messages.get(index++)));
        }, interval, interval);
    }
}
