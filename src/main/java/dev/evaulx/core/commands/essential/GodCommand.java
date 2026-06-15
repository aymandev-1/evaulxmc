package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.models.PlayerProfile; import dev.evaulx.core.utils.CC;
import org.bukkit.command.*; import org.bukkit.entity.Player;
public class GodCommand implements CommandExecutor {
    private final EvaulxCore plugin; public GodCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.god")){s.sendMessage(CC.color("&cNo permission."));return true;}
        Player t;
        if(a.length>0){
            if(!s.hasPermission("evaulx.god.others")){s.sendMessage(CC.color("&cNo permission to toggle god mode for others."));return true;}
            t=org.bukkit.Bukkit.getPlayer(a[0]); if(t==null){s.sendMessage(CC.color("&cPlayer not found."));return true;}
        } else {
            if(!(s instanceof Player)){s.sendMessage(CC.color("&cUsage: /god <player>"));return true;}
            t=(Player)s;
        }
        PlayerProfile profile=plugin.getPlayerManager().getProfile(t);
        if(profile==null){s.sendMessage(CC.color("&cProfile not loaded."));return true;}
        profile.setGodMode(!profile.isGodMode()); plugin.getPlayerManager().saveProfile(profile);
        t.sendMessage(CC.color(profile.isGodMode()?"&aGod mode &aenabled&7.":"&7God mode &cdisabled&7."));
        if(!t.equals(s)) s.sendMessage(CC.color("&aToggled god mode for &f"+t.getName()+": "+(profile.isGodMode()?"&aON":"&cOFF")));
        return true;
    }
}
