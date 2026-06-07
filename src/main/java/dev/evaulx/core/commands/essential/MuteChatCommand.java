package dev.evaulx.core.commands.essential;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit; import org.bukkit.command.*;
public class MuteChatCommand implements CommandExecutor {
    private final EvaulxCore plugin; public MuteChatCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.mutechat")){s.sendMessage(CC.color("&cNo permission."));return true;}
        boolean nowMuted = !plugin.getChatManager().isChatMuted();
        plugin.getChatManager().setChatMuted(nowMuted);
        Bukkit.broadcastMessage(CC.color(nowMuted?"&cChat has been muted by &f"+s.getName():"&aChat has been unmuted by &f"+s.getName())); return true;
    }
}
