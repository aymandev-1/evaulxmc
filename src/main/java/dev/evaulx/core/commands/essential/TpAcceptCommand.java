package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TpAcceptCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public TpAcceptCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        Player target = (Player) sender;

        UUID requesterUUID = null;
        for (Map.Entry<UUID, UUID> e : TpaCommand.PENDING.entrySet()) {
            if (e.getValue().equals(target.getUniqueId())) {
                requesterUUID = e.getKey();
                break;
            }
        }

        if (requesterUUID == null) {
            target.sendMessage(CC.color("&cYou have no pending teleport requests."));
            return true;
        }

        boolean isHere = Boolean.TRUE.equals(TpaCommand.IS_HERE.get(requesterUUID));
        TpaCommand.PENDING.remove(requesterUUID);
        TpaCommand.IS_HERE.remove(requesterUUID);

        Player requester = Bukkit.getPlayer(requesterUUID);
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(CC.color("&cThe requester is no longer online."));
            return true;
        }

        if (isHere) {
            target.teleport(requester.getLocation());
            requester.sendMessage(CC.color("&f" + target.getName() + " &aaccepted and teleported to you."));
            target.sendMessage(CC.color("&aTeleported to &f" + requester.getName() + "&a."));
        } else {
            requester.teleport(target.getLocation());
            requester.sendMessage(CC.color("&f" + target.getName() + " &aaccepted your request."));
            target.sendMessage(CC.color("&aTeleport accepted."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
