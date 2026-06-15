package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class FlyCommand implements CommandExecutor {
    private final EvaulxCore plugin; public FlyCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.fly")){s.sendMessage(CC.color("&cNo permission."));return true;}
        Player t;
        if(a.length>0){
            if(!s.hasPermission("evaulx.fly.others")){s.sendMessage(CC.color("&cNo permission to change others' fly."));return true;}
            t=Bukkit.getPlayer(a[0]); if(t==null){s.sendMessage(CC.color("&cPlayer not found."));return true;}
        } else {
            if(!(s instanceof Player)){s.sendMessage(CC.color("&cUsage: /fly <player>"));return true;}
            t=(Player)s;
        }
        boolean nowFlying = !t.getAllowFlight();
        t.setAllowFlight(nowFlying);
        if (!nowFlying) t.setFlying(false);
        t.sendMessage(CC.color(nowFlying ? "&aFly &aenabled&7." : "&7Fly &cdisabled&7."));
        if(!t.equals(s)) s.sendMessage(CC.color("&aToggled fly for &f"+t.getName()+"&a: "+(nowFlying?"&aON":"&cOFF"))); return true;
    }
}
