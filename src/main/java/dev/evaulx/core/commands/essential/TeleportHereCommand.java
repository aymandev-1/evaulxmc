package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class TeleportHereCommand implements CommandExecutor {
    private final EvaulxCore plugin; public TeleportHereCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!(s instanceof Player)){s.sendMessage("Players only.");return true;}
        if(!s.hasPermission("evaulx.tp")){s.sendMessage(CC.color("&cNo permission."));return true;}
        if(a.length<1){s.sendMessage(CC.color("&cUsage: /tphere <player>"));return true;}
        Player t=Bukkit.getPlayer(a[0]); if(t==null){s.sendMessage(CC.color("&cPlayer not found."));return true;}
        plugin.getEssentialsManager().setBackLocation(t, t.getLocation());
        t.teleport(((Player)s).getLocation()); t.sendMessage(CC.color("&7You were teleported to &c"+s.getName()));
        plugin.getStaffRequestManager().logAction(s.getName(), "TELEPORT_HERE", t.getName(), "to " + s.getName());
        s.sendMessage(CC.color("&aTeleported &f"+t.getName()+" &ato you.")); return true;
    }
}
