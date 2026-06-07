package dev.evaulx.core.commands.rank;
import dev.evaulx.core.EvaulxCore; import dev.evaulx.core.models.Rank; import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
public class RankInfoCommand implements CommandExecutor {
    private final EvaulxCore plugin; public RankInfoCommand(EvaulxCore p){this.plugin=p;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("evaulx.rank")){s.sendMessage(CC.color("&cNo permission."));return true;}
        if(a.length<1){s.sendMessage(CC.color("&cUsage: /rankinfo <rank>"));return true;}
        Rank rank=plugin.getRankManager().getRank(a[0]); if(rank==null){s.sendMessage(CC.color("&cRank not found."));return true;}
        s.sendMessage(CC.color("&8&m------------------------------"));
        s.sendMessage(CC.color("&cRank: "+rank.getDisplayName()));
        s.sendMessage(CC.color("&7Display: &f"+(rank.getDisplay().isEmpty() ? "none" : rank.getDisplay())
                +" &7Permission: &f"+(rank.getPermission().isEmpty() ? "none" : rank.getPermission())));
        s.sendMessage(CC.color("&7Prefix: &f"+rank.getPrefix()+" &7Suffix: &f"+rank.getSuffix()));
        s.sendMessage(CC.color("&7Priority: &f"+rank.getWeight()+" &7Staff: &f"+rank.isStaff()+" &7Default: &f"+rank.isDefault()+" &7Hidden: &f"+rank.isHidden()));
        s.sendMessage(CC.color("&7Inheritance: &f"+(rank.getInheritance().isEmpty() ? "none" : String.join(", ", rank.getInheritance()))));
        s.sendMessage(CC.color("&7Permissions ("+rank.getPermissions().size()+" direct, "+plugin.getRankManager().getAllPermissions(rank).size()+" total):"));
        for(String p:rank.getPermissions()) s.sendMessage(CC.color("  &8- &7"+p));
        s.sendMessage(CC.color("&8&m------------------------------")); return true;
    }
}
