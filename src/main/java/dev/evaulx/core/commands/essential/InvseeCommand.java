package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class InvseeCommand implements CommandExecutor {
    private final EvaulxCore plugin; public InvseeCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!(s instanceof Player)){s.sendMessage("Players only.");return true;}
        if(!s.hasPermission("evaulx.invsee")){s.sendMessage(CC.color("&cNo permission."));return true;}
        if(a.length<1){s.sendMessage(CC.color("&cUsage: /invsee <player>"));return true;}
        Player t=Bukkit.getPlayer(a[0]); if(t==null){s.sendMessage(CC.color("&cPlayer not online."));return true;}
        ((Player)s).openInventory(t.getInventory()); return true;
    }
}
