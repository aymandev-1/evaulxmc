package dev.evaulx.core.commands.staff;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.models.PlayerProfile; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class DisguiseInfoCommand implements CommandExecutor {
    private final EvaulxCore plugin; public DisguiseInfoCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.disguise")){plugin.getMessageManager().send(s,"no-permission","&cNo permission.");return true;}
        s.sendMessage(CC.color("&8&m------------------------------"));
        s.sendMessage(CC.color("&cDisguised Players:"));
        for(Player p:Bukkit.getOnlinePlayers()){
            PlayerProfile profile=plugin.getPlayerManager().getProfile(p);
            if(profile!=null&&profile.isDisguised()) s.sendMessage(CC.color("  &f"+profile.getName()+" &7-> &c"+profile.getDisguiseName()+(profile.getDisguiseRank()!=null?" &7(rank: &f"+profile.getDisguiseRank()+"&7)":"")));
        }
        s.sendMessage(CC.color("&8&m------------------------------")); return true;
    }
}
