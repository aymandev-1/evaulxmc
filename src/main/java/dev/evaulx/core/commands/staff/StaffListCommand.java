package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.network.RedisSyncManager.RemoteStaffEntry;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StaffListCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public StaffListCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.stafflist")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        boolean canSeeVanished = !(sender instanceof Player)
                || sender.hasPermission("evaulx.vanish.see")
                || sender.hasPermission("evaulx.staff");
        List<String> names = new ArrayList<>();
        Set<java.util.UUID> localStaff = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getStaffRequestManager().canReceiveStaffAlerts(player)) continue;
            localStaff.add(player.getUniqueId());

            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            boolean vanished = profile != null && profile.isVanished();
            if (vanished && !canSeeVanished) continue;

            names.add((vanished ? "&8[V] &f" : "&f") + player.getName());
        }

        if (plugin.getConfig().getBoolean("staff-tools.staff-list.network", true)
                && plugin.getRedisSyncManager() != null
                && plugin.getRedisSyncManager().isEnabled()) {
            for (RemoteStaffEntry entry : plugin.getRedisSyncManager().getRemoteStaff()) {
                if (localStaff.contains(entry.getUuid())) continue;
                if (entry.isVanished() && !canSeeVanished) continue;

                String flags = "";
                if (entry.isStaffMode()) flags += "&8[&cSM&8] ";
                if (entry.isVanished()) flags += "&8[&7V&8] ";
                if (entry.isDisguised()) {
                    String nick = entry.getDisguiseName() == null || entry.getDisguiseName().isEmpty()
                            ? "D" : "D:" + entry.getDisguiseName();
                    flags += "&8[&d" + nick + "&8] ";
                }
                names.add(flags + "&f" + entry.getName() + "&7@&c" + entry.getServerId());
            }
        }

        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cOnline Staff &7(" + names.size() + ")"));
        sender.sendMessage(CC.color(names.isEmpty() ? "&7No staff online." : String.join("&7, ", names)));
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }
}
