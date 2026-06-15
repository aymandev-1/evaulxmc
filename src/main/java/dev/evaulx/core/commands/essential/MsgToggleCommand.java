package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class MsgToggleCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public MsgToggleCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("evaulx.msgtoggle")) {
            player.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null) {
            player.sendMessage(CC.color("&cYour profile is not loaded."));
            return true;
        }

        boolean nowEnabled = !profile.isMsgToggled();
        profile.setMsgToggled(nowEnabled);
        plugin.getPlayerManager().saveProfile(profile);

        if (nowEnabled) {
            player.sendMessage(CC.color("&aPrivate messages &aenabled&7. Players can message you."));
        } else {
            player.sendMessage(CC.color("&7Private messages &cdisabled&7. Players cannot message you."));
        }
        return true;
    }
}
