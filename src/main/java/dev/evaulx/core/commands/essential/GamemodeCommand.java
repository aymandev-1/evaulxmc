package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.GameMode; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class GamemodeCommand implements CommandExecutor {
    private final EvaulxCore plugin; public GamemodeCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.gamemode")){s.sendMessage(CC.color("&cNo permission."));return true;}
        if(a.length<1){s.sendMessage(CC.color("&cUsage: /gm <survival|creative|adventure|spectator> [player]"));return true;}
        Player t;
        if(a.length>1){
            if(!s.hasPermission("evaulx.gamemode.others")){s.sendMessage(CC.color("&cNo permission to change others' gamemode."));return true;}
            t=Bukkit.getPlayer(a[1]); if(t==null){s.sendMessage(CC.color("&cPlayer not found."));return true;}
        } else { if(!(s instanceof Player)){s.sendMessage(CC.color("&cUsage: /gm <mode> <player>"));return true;} t=(Player)s; }
        GameMode gm=parseGameMode(a[0]);
        if(gm==null){s.sendMessage(CC.color("&cInvalid gamemode. Use survival, creative, adventure, or spectator."));return true;}
        t.setGameMode(gm); t.sendMessage(CC.color("&7Your gamemode is now &c"+gm.name()));
        if(!t.equals(s)) s.sendMessage(CC.color("&aSet &f"+t.getName()+"&a's gamemode to &c"+gm.name())); return true;
    }
    private GameMode parseGameMode(String input){
        switch(input.toLowerCase(java.util.Locale.ENGLISH)){
            case "0": case "survival":  case "s":  case "surv": return GameMode.SURVIVAL;
            case "1": case "creative":  case "c":  case "cre":  return GameMode.CREATIVE;
            case "2": case "adventure": case "a":  case "adv":  return GameMode.ADVENTURE;
            case "3": case "spectator": case "sp": case "spec": return GameMode.SPECTATOR;
            default: try{int i=Integer.parseInt(input);if(i>=0&&i<=3)return GameMode.values()[i];}catch(NumberFormatException ignored){} return null;
        }
    }
}
