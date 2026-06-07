package dev.evaulx.core.commands.rank;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.models.*; import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.Bukkit; import org.bukkit.OfflinePlayer; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class SetRankCommand implements CommandExecutor {
    private final EvaulxCore plugin; public SetRankCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.rank.set")){s.sendMessage(CC.color("&cNo permission."));return true;}
        if(a.length<2){s.sendMessage(CC.color("&cUsage: /setrank <player> <rank>"));return true;}
        OfflinePlayer t=PlayerUtil.getOfflinePlayer(a[0]);
        if(t==null||!t.hasPlayedBefore()){s.sendMessage(CC.color("&cPlayer not found."));return true;}
        Rank rank=plugin.getRankManager().getRank(a[1]);
        if(rank==null){s.sendMessage(CC.color("&cRank '"+a[1]+"' not found."));return true;}
        PlayerProfile profile=plugin.getPlayerManager().getProfile(t.getUniqueId());
        if(profile==null){s.sendMessage(CC.color("&cPlayer must be online."));return true;}
        String old=profile.getRankName(); profile.setRankName(rank.getName());
        plugin.getPlayerManager().saveProfile(profile);
        Player online=Bukkit.getPlayer(t.getUniqueId());
        if(online!=null){plugin.getPlayerManager().applyPermissions(online,profile);plugin.getNameTagManager().applyNameTag(online);}
        plugin.getDiscordManager().sendRankChange(t.getName(),old,rank.getName(),s.getName());
        if(plugin.getRedisSyncManager()!=null) plugin.getRedisSyncManager().publishRankChange(t.getName(),t.getUniqueId(),old,rank.getName(),s.getName());
        s.sendMessage(CC.color("&aSet &f"+t.getName()+" &arank to "+rank.getDisplayName())); return true;
    }
}
