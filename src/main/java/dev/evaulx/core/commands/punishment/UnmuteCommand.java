package dev.evaulx.core.commands.punishment;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
public class UnmuteCommand implements CommandExecutor {
    private final EvaulxCore plugin;
    public UnmuteCommand(EvaulxCore plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.unmute")) { sender.sendMessage(CC.color("&cNo permission.")); return true; }
        if (args.length < 1) { sender.sendMessage(CC.color("&cUsage: /unmute <player>")); return true; }
        OfflinePlayer target=PlayerUtil.getOfflinePlayer(args[0]);
        if (target==null) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }
        boolean removed=false;
        for (Punishment.Type t:new Punishment.Type[]{Punishment.Type.MUTE,Punishment.Type.TEMPMUTE})
            if (plugin.getPunishmentManager().removePunishment(target.getUniqueId(),t,sender,"Manually unmuted")) removed=true;
        sender.sendMessage(CC.color(removed?"&aUnmuted &f"+args[0]:"&c"+args[0]+" is not muted."));
        return true;
    }
}
