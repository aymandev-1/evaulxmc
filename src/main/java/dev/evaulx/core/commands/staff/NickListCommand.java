package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class NickListCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public NickListCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.nicklist")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        sender.sendMessage(CC.color(CC.SEPARATOR));
        String server = plugin.getConfig().getString("server.server-id", "hub");
        sender.sendMessage(CC.color("&cDisguised Players &8- &f" + server));

        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
            if (profile == null || !profile.isDisguised()) continue;
            count++;
            sender.sendMessage(CC.color("&f" + profile.getName()
                    + " &7-> &c" + profile.getDisguiseName()
                    + " &8(&7online: &f" + player.getName() + "&8)"
                    + " &7skin &f" + nullToNone(profile.getDisguiseSkin())
                    + " &7rank &f" + nullToNone(profile.getDisguiseRank())
                    + " &7server &f" + server));
        }

        if (count == 0) sender.sendMessage(CC.color("&7No disguised players online."));
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private String nullToNone(String value) {
        return value == null || value.isEmpty() ? "none" : value;
    }
}
