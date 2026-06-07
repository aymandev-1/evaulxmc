package dev.evaulx.core.commands.punishment;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
public class UnbanCommand implements CommandExecutor {
    private final EvaulxCore plugin;
    public UnbanCommand(EvaulxCore plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.unban")) { sender.sendMessage(CC.color("&cNo permission.")); return true; }
        if (args.length < 1) { sender.sendMessage(CC.color("&cUsage: /unban <player>")); return true; }
        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target == null) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }
        boolean removed = false;
        for (Punishment.Type t : new Punishment.Type[]{Punishment.Type.BAN,Punishment.Type.TEMPBAN,Punishment.Type.IPBAN,Punishment.Type.BLACKLIST})
            if (plugin.getPunishmentManager().removePunishment(target.getUniqueId(),t,sender,"Manually unbanned")) removed=true;
        sender.sendMessage(CC.color(removed?"&aSuccessfully unbanned &f"+args[0]:"&c"+args[0]+" is not banned."));
        return true;
    }
}
