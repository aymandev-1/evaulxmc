package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.models.PlayerProfile; import dev.evaulx.core.utils.CC;
import org.bukkit.command.*; import org.bukkit.entity.Player;
public class GodCommand implements CommandExecutor {
    private final EvaulxCore plugin; public GodCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!(s instanceof Player)){s.sendMessage("Players only.");return true;}
        if(!s.hasPermission("evaulx.god")){s.sendMessage(CC.color("&cNo permission."));return true;}
        Player p=(Player)s; PlayerProfile profile=plugin.getPlayerManager().getProfile(p);
        if(profile==null)return true;
        profile.setGodMode(!profile.isGodMode()); plugin.getPlayerManager().saveProfile(profile);
        p.sendMessage(CC.color(profile.isGodMode()?"&aGod mode &aenabled&7.":"&7God mode &cdisabled&7.")); return true;
    }
}
