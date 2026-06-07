package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.GameMode; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class GamemodeCommand implements CommandExecutor {
    private final EvaulxCore plugin; public GamemodeCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.gamemode")){s.sendMessage(CC.color("&cNo permission."));return true;}
        if(a.length<1){s.sendMessage(CC.color("&cUsage: /gm <0|1|2|3> [player]"));return true;}
        Player t=(a.length>1)?Bukkit.getPlayer(a[1]):(s instanceof Player?(Player)s:null);
        if(t==null){s.sendMessage(CC.color("&cPlayer not found."));return true;}
        GameMode gm=parseGameMode(a[0]);
        if(gm==null){s.sendMessage(CC.color("&cInvalid gamemode."));return true;}
        t.setGameMode(gm); t.sendMessage(CC.color("&7Your gamemode is now &c"+gm.name()));
        if(!t.equals(s)) s.sendMessage(CC.color("&aSet &f"+t.getName()+"&a's gamemode to &c"+gm.name())); return true;
    }
    private GameMode parseGameMode(String input){
        try{
            switch(Integer.parseInt(input)){
                case 0: return GameMode.SURVIVAL;
                case 1: return GameMode.CREATIVE;
                case 2: return GameMode.ADVENTURE;
                case 3: return GameMode.SPECTATOR;
                default: return null;
            }
        }catch(NumberFormatException ignored){
            return null;
        }
    }
}
