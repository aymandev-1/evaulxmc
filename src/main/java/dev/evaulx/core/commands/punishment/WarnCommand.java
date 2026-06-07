package dev.evaulx.core.commands.punishment;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.List;
public class WarnCommand implements CommandExecutor {
    private final EvaulxCore plugin;
    public WarnCommand(EvaulxCore plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.warn")) { sender.sendMessage(CC.color("&cNo permission.")); return true; }
        if (args.length < 1) { sender.sendMessage(CC.color("&cUsage: /warn <player> [reason]")); return true; }
        OfflinePlayer target=PlayerUtil.getOfflinePlayer(args[0]);
        if (target==null||!target.hasPlayedBefore()) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }
        String reason=args.length>1?join(args,1):"Warned by staff";
        String ip=target.isOnline()?((Player)target).getAddress().getAddress().getHostAddress():null;
        plugin.getPunishmentManager().punish(sender,target.getUniqueId(),target.getName(),ip,Punishment.Type.WARN,reason,-1L,false);
        List<Punishment> warns=plugin.getDatabaseManager().getPunishments(target.getUniqueId());
        int activeWarns=(int)warns.stream().filter(p->p.getType()==Punishment.Type.WARN&&p.isActive()).count();
        int threshold=plugin.getConfig().getInt("punishments.warn-threshold",5);
        if (activeWarns>=threshold) {
            String action=plugin.getConfig().getString("punishments.warn-threshold-action","tempban {player} 1d Auto-banned").replace("{player}",target.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),action);
        }
        return true;
    }
    private String join(String[] a,int s){StringBuilder sb=new StringBuilder();for(int i=s;i<a.length;i++){if(i>s)sb.append(" ");sb.append(a[i]);}return sb.toString();}
}
