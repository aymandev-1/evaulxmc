package dev.evaulx.core.commands.punishment;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class PunishCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public PunishCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.punish")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /punish <player> [preset] [step]"));
            return true;
        }

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[0]);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        if (args.length == 1 && sender instanceof Player) {
            plugin.getGuiManager().openPunishMenu((Player) sender, target);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(CC.color("&cConsole usage: /punish <player> <preset> [step]"));
            return true;
        }

        Integer step = null;
        if (args.length > 2) {
            try {
                step = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage(CC.color("&cStep must be a number."));
                return true;
            }
        }

        plugin.getPunishmentPresetManager().executePreset(sender, target, args[1], step);
        return true;
    }
}
