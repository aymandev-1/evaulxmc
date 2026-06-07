package dev.evaulx.core.commands.punishment;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
public class KickCommand implements CommandExecutor {
    private final EvaulxCore plugin;
    public KickCommand(EvaulxCore plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("evaulx.kick")) { sender.sendMessage(CC.color("&cNo permission.")); return true; }
        if (args.length < 1) { sender.sendMessage(CC.color("&cUsage: /kick <player> [reason]")); return true; }
        Player target=Bukkit.getPlayer(args[0]);
        if (target==null) { sender.sendMessage(CC.color("&cPlayer not online.")); return true; }
        String reason=args.length>1?join(args,1):"Kicked by staff";
        plugin.getPunishmentManager().punish(sender,target.getUniqueId(),target.getName(),target.getAddress().getAddress().getHostAddress(),Punishment.Type.KICK,reason,0L,false);
        return true;
    }
    private String join(String[] a,int s){StringBuilder sb=new StringBuilder();for(int i=s;i<a.length;i++){if(i>s)sb.append(" ");sb.append(a[i]);}return sb.toString();}
}
