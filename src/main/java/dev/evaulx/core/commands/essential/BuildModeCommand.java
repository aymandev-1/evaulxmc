package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class BuildModeCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public BuildModeCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.buildmode")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        Player target = resolveTarget(sender, args);
        if (target == null) return true;

        PlayerProfile profile = plugin.getPlayerManager().getProfile(target);
        if (profile == null) {
            sender.sendMessage(CC.color("&cProfile not loaded."));
            return true;
        }

        Boolean requested = requestedState(sender, target, args);
        boolean enabled = requested != null ? requested : !profile.isBuildMode();
        profile.setBuildMode(enabled);
        plugin.getPlayerManager().saveProfile(profile);

        target.sendMessage(CC.color(enabled ? "&aBuild mode enabled." : "&cBuild mode disabled."));
        if (!target.equals(sender)) {
            sender.sendMessage(CC.color((enabled ? "&aEnabled" : "&cDisabled") + " &f" + target.getName() + "&7's build mode."));
        }
        return true;
    }

    private Player resolveTarget(CommandSender sender, String[] args) {
        if (args.length > 0 && !isState(args[0])) {
            if (!sender.hasPermission("evaulx.buildmode.others")) {
                sender.sendMessage(CC.color("&cNo permission to edit other players."));
                return null;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) sender.sendMessage(CC.color("&cPlayer must be online."));
            return target;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cUsage: /buildmode <player> [on|off]"));
            return null;
        }
        return (Player) sender;
    }

    private Boolean requestedState(CommandSender sender, Player target, String[] args) {
        if (args.length == 0) return null;
        if (target.equals(sender) && isState(args[0])) return parseState(args[0]);
        if (args.length > 1 && isState(args[1])) return parseState(args[1]);
        return null;
    }

    private boolean isState(String value) {
        return value.equalsIgnoreCase("on")
                || value.equalsIgnoreCase("off")
                || value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("false");
    }

    private Boolean parseState(String value) {
        return value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true");
    }
}
