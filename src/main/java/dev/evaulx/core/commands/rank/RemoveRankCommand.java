package dev.evaulx.core.commands.rank;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.models.PlayerProfile; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class RemoveRankCommand implements CommandExecutor {
    private final EvaulxCore plugin; public RemoveRankCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.rank.remove")){s.sendMessage(CC.color("&cNo permission."));return true;}
        if(a.length<2){s.sendMessage(CC.color("&cUsage: /removerank <player> <rank>"));return true;}
        Player t=Bukkit.getPlayer(a[0]); if(t==null){s.sendMessage(CC.color("&cPlayer must be online."));return true;}
        PlayerProfile profile=plugin.getPlayerManager().getProfile(t); if(profile==null){s.sendMessage(CC.color("&cProfile not loaded."));return true;}
        profile.removeExtraRank(a[1]); plugin.getPlayerManager().saveProfile(profile); plugin.getPlayerManager().applyPermissions(t,profile);
        if(plugin.getRedisSyncManager()!=null) plugin.getRedisSyncManager().publishRankChange(t.getName(),t.getUniqueId(),a[1],"removed",s.getName());
        s.sendMessage(CC.color("&aRemoved rank &f"+a[1]+" &afrom &f"+t.getName())); return true;
    }
}
