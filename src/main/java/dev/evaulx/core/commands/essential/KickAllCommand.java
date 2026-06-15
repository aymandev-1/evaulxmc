package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class KickAllCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public KickAllCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.kickall")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        String reason = args.length > 0 ? String.join(" ", args) : "You have been kicked.";
        String kickMessage = CC.color("&cKicked by server.\n&7Reason: &f" + reason);

        List<Player> toKick = new ArrayList<>(Bukkit.getOnlinePlayers());
        int count = 0;
        for (Player target : toKick) {
            if (target.equals(sender)) continue;
            if (target.hasPermission("evaulx.kickall.exempt")) continue;
            target.kickPlayer(kickMessage);
            count++;
        }

        sender.sendMessage(CC.color("&7Kicked &f" + count + " &7player" + (count == 1 ? "" : "s") + "."));
        plugin.getStaffRequestManager().broadcastStaff(
                "&8[&cKickAll&8] &f" + sender.getName() + " &7kicked all players. Reason: &f" + reason,
                "evaulx.staff");
        plugin.getStaffRequestManager().logAction(sender.getName(), "KICKALL", "all (" + count + " players)", reason);
        return true;
    }
}
