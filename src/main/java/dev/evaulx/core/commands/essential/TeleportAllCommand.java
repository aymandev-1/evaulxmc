package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class TeleportAllCommand implements CommandExecutor {
    private final EvaulxCore plugin; public TeleportAllCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!(s instanceof Player)){s.sendMessage("Players only.");return true;}
        if(!s.hasPermission("evaulx.tpall")){s.sendMessage(CC.color("&cNo permission."));return true;}
        Player p=(Player)s;
        for(Player o:Bukkit.getOnlinePlayers()) if(!o.equals(p)){plugin.getEssentialsManager().setBackLocation(o,o.getLocation());o.teleport(p.getLocation());o.sendMessage(CC.color("&7Teleported to &c"+p.getName()));}
        plugin.getStaffRequestManager().logAction(s.getName(), "TELEPORT_ALL", p.getName(), "Teleported online players to self");
        p.sendMessage(CC.color("&aTeleported all players to you.")); return true;
    }
}
