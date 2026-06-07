package dev.evaulx.core.commands.punishment;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
public class UnblacklistCommand implements CommandExecutor {
    private final EvaulxCore plugin;
    public UnblacklistCommand(EvaulxCore plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.blacklist")) { sender.sendMessage(CC.color("&cNo permission.")); return true; }
        if (args.length < 1) { sender.sendMessage(CC.color("&cUsage: /unblacklist <player>")); return true; }
        OfflinePlayer target=PlayerUtil.getOfflinePlayer(args[0]);
        if (target==null) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }
        boolean removed=plugin.getPunishmentManager().removePunishment(target.getUniqueId(),Punishment.Type.BLACKLIST,sender,"Unblacklisted");
        sender.sendMessage(CC.color(removed?"&aSuccessfully unblacklisted &f"+args[0]:"&cNot blacklisted."));
        return true;
    }
}
