package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class RealNameCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public RealNameCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.realname")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /realname <nick>"));
            return true;
        }

        String query = args[0];
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (profile == null) continue;

            // Check streamer mode first
            if (profile.isStreamerMode() && profile.getStreamerAlias() != null) {
                if (query.equalsIgnoreCase(profile.getStreamerAlias())
                        || query.equalsIgnoreCase(profile.getName())
                        || query.equalsIgnoreCase(player.getName())) {
                    sender.sendMessage(CC.color("&8[&dStreamer&8] &f" + profile.getStreamerAlias()
                            + " &7is &f" + profile.getName() + " &7(streamer mode)&7."));
                    plugin.getStaffRequestManager().logAction(sender.getName(), "REALNAME_LOOKUP", profile.getName(),
                            "streamer alias '" + query + "' -> " + profile.getName());
                    return true;
                }
            }

            // Check disguise
            if (!profile.isDisguised()) continue;
            if (query.equalsIgnoreCase(profile.getDisguiseName())
                    || query.equalsIgnoreCase(profile.getName())
                    || query.equalsIgnoreCase(player.getName())) {
                sender.sendMessage(CC.color("&8[&cDisguise&8] &f" + profile.getDisguiseName()
                        + " &7is &f" + profile.getName()
                        + " &7skin &f" + nullToNone(profile.getDisguiseSkin())
                        + " &7rank &f" + nullToNone(profile.getDisguiseRank()) + "&7."));
                plugin.getStaffRequestManager().logAction(sender.getName(), "REALNAME_LOOKUP", profile.getName(),
                        "resolved query '" + query + "' to " + profile.getDisguiseName());
                return true;
            }
        }

        plugin.getMessageManager().send(sender, "disguise.realname-not-found",
                "&cNo online disguised player matched &f{query}&c.",
                plugin.getMessageManager().placeholders("{query}", query));
        return true;
    }

    private String nullToNone(String value) {
        return value == null || value.isEmpty() ? "none" : value;
    }
}
