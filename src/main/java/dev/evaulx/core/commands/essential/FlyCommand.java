package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class FlyCommand implements CommandExecutor {
    private final EvaulxCore plugin; public FlyCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.fly")){s.sendMessage(CC.color("&cNo permission."));return true;}
        Player t=(a.length>0)?Bukkit.getPlayer(a[0]):(s instanceof Player?(Player)s:null);
        if(t==null){s.sendMessage(CC.color("&cPlayer not found."));return true;}
        t.setAllowFlight(!t.getAllowFlight());
        t.sendMessage(CC.color(t.getAllowFlight()?"&aFly &aenabled&7.":"&7Fly &cdisabled&7."));
        if(!t.equals(s)) s.sendMessage(CC.color("&aToggled fly for &f"+t.getName())); return true;
    }
}
