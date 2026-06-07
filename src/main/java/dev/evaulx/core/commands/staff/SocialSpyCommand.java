package dev.evaulx.core.commands.staff;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.models.PlayerProfile; import dev.evaulx.core.utils.CC;
import org.bukkit.command.*; import org.bukkit.entity.Player;
public class SocialSpyCommand implements CommandExecutor {
    private final EvaulxCore plugin; public SocialSpyCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!(s instanceof Player)){s.sendMessage("Players only.");return true;}
        if(!s.hasPermission("evaulx.socialspy")){s.sendMessage(CC.color("&cNo permission."));return true;}
        Player p=(Player)s; PlayerProfile profile=plugin.getPlayerManager().getProfile(p);
        if(profile==null)return true;
        profile.setSocialSpy(!profile.isSocialSpy());
        plugin.getPlayerManager().saveProfile(profile);
        p.sendMessage(CC.color(profile.isSocialSpy()?"&aSocialSpy &aenabled&7.":"&7SocialSpy &cdisabled&7.")); return true;
    }
}
