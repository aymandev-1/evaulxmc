package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class FreezeCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public FreezeCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.freeze")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /" + label + " <player> [reason]"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not online."));
            return true;
        }

        if (target.equals(sender)) {
            sender.sendMessage(CC.color("&cYou cannot freeze yourself."));
            return true;
        }

        String reason = args.length > 1 ? clean(join(args, 1), 160) : "No reason specified";
        boolean explicitUnfreeze = label.equalsIgnoreCase("unfreeze") || label.equalsIgnoreCase("thaw");

        if (explicitUnfreeze || plugin.getStaffRequestManager().isFrozen(target)) {
            boolean unfrozen = plugin.getStaffRequestManager().unfreeze(sender.getName(), target, reason);
            sender.sendMessage(CC.color(unfrozen ? "&7Unfroze &f" + target.getName() + "&7." : "&cThat player is not frozen."));
            return true;
        }

        boolean frozen = plugin.getStaffRequestManager().freeze(sender.getName(), target, reason);
        sender.sendMessage(CC.color(frozen ? "&7Froze &f" + target.getName() + "&7." : "&cThat player is already frozen."));
        return true;
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private String clean(String input, int maxLength) {
        String cleaned = CC.strip(input).replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }
}
