package dev.evaulx.core.listeners;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.chat.ChatManager;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TaskUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Iterator;

public class ChatListener implements Listener {

    private final EvaulxCore plugin;

    public ChatListener(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();

        if (plugin.getStaffRequestManager().isStaffChatEnabled(player)) {
            e.setCancelled(true);
            if (!player.hasPermission("evaulx.staffchat")) {
                plugin.getStaffRequestManager().toggleStaffChat(player);
                player.sendMessage(CC.color("&cStaff chat disabled because you no longer have permission."));
                return;
            }

            String message = CC.strip(e.getMessage());
            TaskUtil.sync(() -> plugin.getStaffRequestManager().sendStaffChat(player, message));
            return;
        }

        // Mute check
        Punishment mute = plugin.getPunishmentManager().getActiveMute(player.getUniqueId());
        if (mute != null) {
            e.setCancelled(true);
            player.sendMessage(CC.color(plugin.getConfig().getString("punishments.mute-message",
                    "&cYou are muted. Reason: &f{reason} &cExpires: &f{duration}")
                    .replace("{reason}", mute.getReason())
                    .replace("{duration}", mute.getDurationString())
                    .replace("{id}", mute.getId())));
            return;
        }

        // Muted chat
        if (plugin.getChatManager().isChatMuted() && !player.hasPermission("evaulx.chat.bypass")) {
            e.setCancelled(true);
            player.sendMessage(CC.color("&cChat is currently muted."));
            return;
        }

        ChatManager.ModerationResult result = plugin.getChatManager().moderate(player, e.getMessage());
        if (!result.isAllowed()) {
            e.setCancelled(true);
            player.sendMessage(CC.color(result.getMessage()));
            return;
        }

        e.setMessage(result.getMessage());
        applyChatRange(e);
        e.setFormat(escapeFormat(plugin.getChatManager().formatChat(player, result.getMessage())));
    }

    private String escapeFormat(String format) {
        return format == null ? "" : format.replace("%", "%%");
    }

    private void applyChatRange(AsyncPlayerChatEvent event) {
        int range = plugin.getConfig().getInt("chat.range", -1);
        if (range < 0) return;

        Player sender = event.getPlayer();
        double maxDistanceSquared = range * range;
        Iterator<Player> iterator = event.getRecipients().iterator();
        while (iterator.hasNext()) {
            Player recipient = iterator.next();
            if (recipient.hasPermission("evaulx.chat.spy")) continue;
            if (!recipient.getWorld().equals(sender.getWorld())
                    || recipient.getLocation().distanceSquared(sender.getLocation()) > maxDistanceSquared) {
                iterator.remove();
            }
        }
    }
}
