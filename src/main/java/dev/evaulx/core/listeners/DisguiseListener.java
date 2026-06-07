package dev.evaulx.core.listeners;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.TaskUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

public class DisguiseListener implements Listener {

    private final EvaulxCore plugin;

    public DisguiseListener(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getDisguiseManager().handleQuit(e.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        plugin.getDisguiseManager().handleMenuClick((Player) e.getWhoClicked(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (!plugin.getDisguiseManager().isAwaitingName(player)) return;

        e.setCancelled(true);
        String message = e.getMessage();
        TaskUtil.sync(() -> plugin.getDisguiseManager().handleNameChat(player, message));
    }
}
