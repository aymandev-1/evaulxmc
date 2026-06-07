package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*;
public class AlertCommand implements CommandExecutor {
    private final EvaulxCore plugin; public AlertCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.alert")){s.sendMessage(CC.color("&cNo permission."));return true;}
        if(a.length<1){s.sendMessage(CC.color("&cUsage: /alert <message>"));return true;}
        StringBuilder sb=new StringBuilder(); for(String w:a){if(sb.length()>0)sb.append(" ");sb.append(w);}
        Bukkit.broadcastMessage(CC.color("&4&l[ALERT] &c"+sb)); return true;
    }
}
