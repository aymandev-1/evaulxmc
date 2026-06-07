package dev.evaulx.core.listeners;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

public class CommandSpyListener implements Listener {

    private final EvaulxCore plugin;

    public CommandSpyListener(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("staff-tools.command-spy.enabled", true)) return;
        if (event.getPlayer().hasPermission("evaulx.commandspy.exempt")) return;
        if (plugin.getStaffRequestManager().canReceiveStaffAlerts(event.getPlayer())
                && plugin.getConfig().getBoolean("staff-tools.command-spy.hide-staff-commands", true)) {
            return;
        }

        String command = rootCommand(event.getMessage());
        for (String blocked : plugin.getConfig().getStringList("staff-tools.command-spy.blocked-commands")) {
            if (command.equalsIgnoreCase(blocked)) return;
        }

        plugin.getStaffRequestManager().sendCommandSpy(event.getPlayer(), event.getMessage());
    }

    private String rootCommand(String message) {
        String raw = message == null ? "" : message.trim();
        if (raw.startsWith("/")) raw = raw.substring(1);
        int space = raw.indexOf(' ');
        if (space >= 0) raw = raw.substring(0, space);
        int namespace = raw.indexOf(':');
        if (namespace >= 0 && namespace + 1 < raw.length()) raw = raw.substring(namespace + 1);
        return raw.toLowerCase(Locale.ENGLISH);
    }
}
