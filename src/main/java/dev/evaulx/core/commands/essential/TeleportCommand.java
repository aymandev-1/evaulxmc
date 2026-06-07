package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class TeleportCommand implements CommandExecutor {
    private final EvaulxCore plugin; public TeleportCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.tp")){s.sendMessage(CC.color("&cNo permission."));return true;}
        if(a.length<1){s.sendMessage(CC.color("&cUsage: /tp <player> [target]"));return true;}
        Player from=(a.length>1)?(s instanceof Player?(Player)s:null):((s instanceof Player)?(Player)s:null);
        Player to=(a.length>1)?Bukkit.getPlayer(a[1]):Bukkit.getPlayer(a[0]);
        if(a.length==2) from=Bukkit.getPlayer(a[0]);
        if(from==null||to==null){s.sendMessage(CC.color("&cPlayer not found."));return true;}
        plugin.getEssentialsManager().setBackLocation(from, from.getLocation());
        from.teleport(to.getLocation());
        plugin.getStaffRequestManager().logAction(s.getName(), "TELEPORT", from.getName(), "to " + to.getName());
        from.sendMessage(CC.color("&7Teleported to &c"+to.getName()));
        if(!from.equals(s)) s.sendMessage(CC.color("&aTeleported &f"+from.getName()+" &ato &f"+to.getName())); return true;
    }
}
