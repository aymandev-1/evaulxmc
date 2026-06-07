package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.command.*; import org.bukkit.entity.Player;
public class SpeedCommand implements CommandExecutor {
    private final EvaulxCore plugin; public SpeedCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!(s instanceof Player)){s.sendMessage("Players only.");return true;}
        if(!s.hasPermission("evaulx.speed")){s.sendMessage(CC.color("&cNo permission."));return true;}
        if(a.length<1){s.sendMessage(CC.color("&cUsage: /speed <0-10>"));return true;}
        Player p=(Player)s; float speed; try{speed=Float.parseFloat(a[0]);} catch(Exception e){s.sendMessage(CC.color("&cInvalid number."));return true;}
        if(speed<0||speed>10){s.sendMessage(CC.color("&cSpeed must be 0-10."));return true;}
        p.setWalkSpeed(speed/10f*0.2f); p.setFlySpeed(speed/10f*0.1f);
        p.sendMessage(CC.color("&7Speed set to &c"+speed)); return true;
    }
}
