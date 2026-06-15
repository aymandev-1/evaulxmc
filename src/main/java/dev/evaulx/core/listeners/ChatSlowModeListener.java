package dev.evaulx.core.listeners;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.commands.staff.SlowChatCommand;
import dev.evaulx.core.utils.CC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces the global chat slow mode toggled by {@link SlowChatCommand}.
 * Players with {@code evaulx.slowchat.bypass} are exempt.
 */
public class ChatSlowModeListener implements Listener {

    private final EvaulxCore plugin;
    private final ConcurrentHashMap<UUID, Long> lastMessage = new ConcurrentHashMap<>();

    public ChatSlowModeListener(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        int slow = SlowChatCommand.getSlowSeconds();
        if (slow <= 0) return;

        Player player = event.getPlayer();
        if (player.hasPermission("evaulx.slowchat.bypass")) return;

        long now = System.currentTimeMillis();
        Long last = lastMessage.get(player.getUniqueId());
        if (last != null) {
            long remaining = (slow * 1000L) - (now - last);
            if (remaining > 0) {
                event.setCancelled(true);
                long secs = (remaining / 1000) + 1;
                player.sendMessage(CC.color("&8[&6SlowChat&8] &7Please wait &e" + secs + "s &7before chatting again."));
                return;
            }
        }
        lastMessage.put(player.getUniqueId(), now);
    }
}
