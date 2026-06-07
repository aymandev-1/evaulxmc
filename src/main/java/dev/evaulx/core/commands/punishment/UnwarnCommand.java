package dev.evaulx.core.commands.punishment;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
public class UnwarnCommand implements CommandExecutor {
    private final EvaulxCore plugin;
    public UnwarnCommand(EvaulxCore plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.unwarn")) { sender.sendMessage(CC.color("&cNo permission.")); return true; }
        if (args.length < 1) { sender.sendMessage(CC.color("&cUsage: /unwarn <player>")); return true; }
        OfflinePlayer target=PlayerUtil.getOfflinePlayer(args[0]);
        if (target==null) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }
        boolean removed=plugin.getPunishmentManager().removePunishment(target.getUniqueId(),Punishment.Type.WARN,sender,"Warn removed");
        sender.sendMessage(CC.color(removed?"&aRemoved a warning from &f"+args[0]:"&cNo active warning found."));
        return true;
    }
}
