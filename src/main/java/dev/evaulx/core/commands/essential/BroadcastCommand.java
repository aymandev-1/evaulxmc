package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
public class BroadcastCommand implements CommandExecutor {
    private final EvaulxCore plugin; public BroadcastCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.broadcast")){s.sendMessage(CC.color("&cNo permission."));return true;}
        if(a.length<1){s.sendMessage(CC.color("&cUsage: /bc <message>"));return true;}
        StringBuilder sb=new StringBuilder(); for(String w:a){if(sb.length()>0)sb.append(" ");sb.append(w);}
        plugin.getEssentialsManager().sendBroadcast(s.getName(), sb.toString()); return true;
    }
}
