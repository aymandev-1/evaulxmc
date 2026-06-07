package dev.evaulx.core.commands.punishment;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
public class MuteCommand implements CommandExecutor {
    private final EvaulxCore plugin;
    public MuteCommand(EvaulxCore plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.mute")) { sender.sendMessage(CC.color("&cNo permission.")); return true; }
        if (args.length < 1) { sender.sendMessage(CC.color("&cUsage: /mute <player> [reason]")); return true; }
        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target==null||!target.hasPlayedBefore()) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }
        if (plugin.getPunishmentManager().isMuted(target.getUniqueId())) { sender.sendMessage(CC.color("&cAlready muted.")); return true; }
        String reason=args.length>1?join(args,1):"Muted by staff";
        String ip=target.isOnline()?((Player)target).getAddress().getAddress().getHostAddress():null;
        plugin.getPunishmentManager().punish(sender,target.getUniqueId(),target.getName(),ip,Punishment.Type.MUTE,reason,-1L,false);
        return true;
    }
    private String join(String[] a,int s){StringBuilder sb=new StringBuilder();for(int i=s;i<a.length;i++){if(i>s)sb.append(" ");sb.append(a[i]);}return sb.toString();}
}
