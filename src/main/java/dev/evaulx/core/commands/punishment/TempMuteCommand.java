package dev.evaulx.core.commands.punishment;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import dev.evaulx.core.utils.TimeUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
public class TempMuteCommand implements CommandExecutor {
    private final EvaulxCore plugin;
    public TempMuteCommand(EvaulxCore plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.tempmute")) { sender.sendMessage(CC.color("&cNo permission.")); return true; }
        if (args.length < 2) { sender.sendMessage(CC.color("&cUsage: /tempmute <player> <duration> [reason]")); return true; }
        long expires=TimeUtil.parseDuration(args[1]);
        if (expires==-1L) { sender.sendMessage(CC.color("&cInvalid duration.")); return true; }
        OfflinePlayer target=PlayerUtil.getOfflinePlayer(args[0]);
        if (target==null||!target.hasPlayedBefore()) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }
        String reason=args.length>2?join(args,2):"Temp muted";
        String ip=target.isOnline()?((Player)target).getAddress().getAddress().getHostAddress():null;
        plugin.getPunishmentManager().punish(sender,target.getUniqueId(),target.getName(),ip,Punishment.Type.TEMPMUTE,reason,expires,false);
        return true;
    }
    private String join(String[] a,int s){StringBuilder sb=new StringBuilder();for(int i=s;i<a.length;i++){if(i>s)sb.append(" ");sb.append(a[i]);}return sb.toString();}
}
