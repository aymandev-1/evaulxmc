package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*; import org.bukkit.entity.Player;
import java.util.UUID;
public class ReplyCommand implements CommandExecutor {
    private final EvaulxCore plugin; public ReplyCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!(s instanceof Player)){s.sendMessage("Players only.");return true;}
        if(a.length<1){s.sendMessage(CC.color("&cUsage: /r <message>"));return true;}
        Player p=(Player)s; UUID lastUUID=plugin.getChatManager().getLastMessaged(p.getUniqueId());
        if(lastUUID==null){p.sendMessage(CC.color("&cNo one to reply to."));return true;}
        Player t=Bukkit.getPlayer(lastUUID); if(t==null){p.sendMessage(CC.color("&cThat player is no longer online."));return true;}
        StringBuilder sb=new StringBuilder(); for(int i=0;i<a.length;i++){if(i>0)sb.append(" ");sb.append(a[i]);}
        String senderName=plugin.getDisguiseManager().getVisibleName(p);
        String targetName=plugin.getDisguiseManager().getVisibleName(t);
        p.sendMessage(CC.color("&7[You -> &f"+targetName+"&7] &f"+sb));
        t.sendMessage(CC.color("&7[&f"+senderName+" &7-> You] &f"+sb));
        plugin.getChatManager().setLastMessaged(p.getUniqueId(),t.getUniqueId()); return true;
    }
}
