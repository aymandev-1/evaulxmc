package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.models.PlayerProfile; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class MessageCommand implements CommandExecutor {
    private final EvaulxCore plugin; public MessageCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!(s instanceof Player)){s.sendMessage("Players only.");return true;}
        if(a.length<2){s.sendMessage(CC.color("&cUsage: /msg <player> <message>"));return true;}
        Player p=(Player)s;
        Player t=Bukkit.getPlayer(a[0]); if(t==null){s.sendMessage(CC.color("&cPlayer not online."));return true;}
        PlayerProfile sp=plugin.getPlayerManager().getProfile(p);
        PlayerProfile tp=plugin.getPlayerManager().getProfile(t);
        if(sp!=null&&sp.isIgnoring(t.getUniqueId())){s.sendMessage(CC.color("&cYou are ignoring &f"+t.getName()+"&c."));return true;}
        if(tp!=null&&tp.isIgnoring(p.getUniqueId())){s.sendMessage(CC.color("&cThat player is ignoring you."));return true;}
        if(tp!=null&&!tp.isMsgToggled()){s.sendMessage(CC.color("&cThat player has messages disabled."));return true;}
        StringBuilder sb=new StringBuilder(); for(int i=1;i<a.length;i++){if(i>1)sb.append(" ");sb.append(a[i]);}
        String senderName=plugin.getDisguiseManager().getVisibleName(p);
        String targetName=plugin.getDisguiseManager().getVisibleName(t);
        p.sendMessage(CC.color("&7[You -> &f"+targetName+"&7] &f"+sb));
        t.sendMessage(CC.color("&7[&f"+senderName+" &7-> You] &f"+sb));
        plugin.getChatManager().setLastMessaged(p.getUniqueId(),t.getUniqueId());
        // Social spy
        for(Player o:Bukkit.getOnlinePlayers()){
            PlayerProfile op=plugin.getPlayerManager().getProfile(o);
            if(op!=null&&op.isSocialSpy()&&!o.equals(p)&&!o.equals(t))
                o.sendMessage(CC.color("&8[SS] &7[&f"+senderName+" &7-> &f"+targetName+"&7] &f"+sb));
        }
        return true;
    }
}
