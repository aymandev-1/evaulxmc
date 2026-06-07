package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class HealCommand implements CommandExecutor {
    private final EvaulxCore plugin; public HealCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.heal")){s.sendMessage(CC.color("&cNo permission."));return true;}
        Player t=(a.length>0)?Bukkit.getPlayer(a[0]):(s instanceof Player?(Player)s:null);
        if(t==null){s.sendMessage(CC.color("&cPlayer not found."));return true;}
        t.setHealth(t.getMaxHealth()); t.setFoodLevel(20); t.setFireTicks(0);
        t.sendMessage(CC.color("&aYou have been healed."));
        if(!t.equals(s)) s.sendMessage(CC.color("&aHealed &f"+t.getName())); return true;
    }
}
