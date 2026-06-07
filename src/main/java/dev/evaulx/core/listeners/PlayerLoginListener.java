package dev.evaulx.core.listeners;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TimeUtil;
import org.bukkit.event.*;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class PlayerLoginListener implements Listener {

    private final EvaulxCore plugin;

    public PlayerLoginListener(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Ban and IP-ban checks read from the database, so they run here on the async pre-login
     * thread instead of {@link PlayerLoginEvent} (which is on the main thread). This keeps login
     * database I/O off the server tick, which matters most on the flat-file backend where the
     * IP-ban lookup scans every stored punishment.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        // UUID ban / tempban / blacklist
        Punishment ban = plugin.getPunishmentManager().getActiveBan(e.getUniqueId());
        if (ban != null) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, buildScreen(ban));
            return;
        }

        // IP ban
        String ip = e.getAddress() == null ? null : e.getAddress().getHostAddress();
        if (ip == null) return;
        for (Punishment p : plugin.getDatabaseManager().getPunishmentsByIp(ip)) {
            if (p.isActive() && p.getType() == Punishment.Type.IPBAN) {
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, buildScreen(p));
                return;
            }
        }
    }

    /**
     * Maintenance is checked here (not in pre-login) because it needs the player's permissions
     * and op status, which only exist once the {@link org.bukkit.entity.Player} is available.
     * This check is config-only, so it does not touch the database.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent e) {
        if (plugin.getConfig().getBoolean("maintenance.enabled", false) && !canJoinDuringMaintenance(e)) {
            String reason = plugin.getConfig().getString("maintenance.reason", "Server maintenance");
            String message = plugin.getConfig().getString("maintenance.kick-message",
                    "&cEvaulxMC is currently in maintenance.\n&7Reason: &f{reason}");
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, CC.color(message.replace("{reason}", reason)));
        }
    }

    private boolean canJoinDuringMaintenance(PlayerLoginEvent event) {
        String bypass = plugin.getConfig().getString("maintenance.bypass-permission", "evaulx.maintenance.bypass");
        if (event.getPlayer().hasPermission(bypass) || event.getPlayer().isOp()) return true;
        String name = event.getPlayer().getName().toLowerCase();
        for (String allowed : plugin.getConfig().getStringList("maintenance.allowed-players")) {
            if (name.equalsIgnoreCase(allowed)) return true;
        }
        return false;
    }

    private String buildScreen(Punishment ban) {
        String template;
        if (ban.getType() == Punishment.Type.BLACKLIST) {
            template = plugin.getConfig().getString("punishments.blacklist-message",
                    "&4You have been blacklisted.\n&7Reason: &f{reason}");
        } else {
            template = plugin.getConfig().getString("punishments.ban-message",
                    "&cYou have been banned.\n&7Reason: &f{reason}\n&7Duration: &f{duration}\n&7Appeal: &fevaulx.dev/appeal");
        }
        return CC.color(template
                .replace("{reason}", ban.getReason())
                .replace("{duration}", TimeUtil.formatDuration(ban.getExpires()))
                .replace("{id}", ban.getId())
                .replace("{punisher}", ban.getPunisherName()));
    }
}
