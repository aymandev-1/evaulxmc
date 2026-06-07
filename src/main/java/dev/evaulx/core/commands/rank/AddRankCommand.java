package dev.evaulx.core.commands.rank;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.models.*; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class AddRankCommand implements CommandExecutor {
    private final EvaulxCore plugin; public AddRankCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.rank.add")){s.sendMessage(CC.color("&cNo permission."));return true;}
        if(a.length<2){s.sendMessage(CC.color("&cUsage: /addrank <player> <rank>"));return true;}
        Player t=Bukkit.getPlayer(a[0]); if(t==null){s.sendMessage(CC.color("&cPlayer must be online."));return true;}
        Rank rank=plugin.getRankManager().getRank(a[1]); if(rank==null){s.sendMessage(CC.color("&cRank not found."));return true;}
        PlayerProfile profile=plugin.getPlayerManager().getProfile(t);
        if(profile==null){s.sendMessage(CC.color("&cProfile not loaded."));return true;}
        profile.addExtraRank(rank.getName()); plugin.getPlayerManager().saveProfile(profile); plugin.getPlayerManager().applyPermissions(t,profile);
        if(plugin.getRedisSyncManager()!=null) plugin.getRedisSyncManager().publishRankChange(t.getName(),t.getUniqueId(),"extra",rank.getName(),s.getName());
        s.sendMessage(CC.color("&aAdded rank "+rank.getDisplayName()+" &ato &f"+t.getName())); return true;
    }
}
