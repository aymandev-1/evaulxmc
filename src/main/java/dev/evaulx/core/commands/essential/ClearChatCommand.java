package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*;
public class ClearChatCommand implements CommandExecutor {
    private final EvaulxCore plugin; public ClearChatCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.clearchat")){s.sendMessage(CC.color("&cNo permission."));return true;}
        for(int i=0;i<100;i++) Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage(CC.color("&8[&cEvaulxMC&8] &7Chat has been cleared by &f"+s.getName())); return true;
    }
}
