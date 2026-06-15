package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ForceChatCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public ForceChatCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.forcechat")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /forcechat <player> <message>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "player-not-online", "&cPlayer not online.");
            return true;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String senderName = sender instanceof Player ? sender.getName() : "Console";

        target.chat(message);

        sender.sendMessage(CC.color("&8[&cAdmin&8] &7Forced &f" + target.getName() + " &7to say: &f" + message));
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("evaulx.staff") && !staff.getUniqueId().equals(target.getUniqueId())) {
                staff.sendMessage(CC.color("&8[&cAdmin&8] &f" + senderName + " &7forced &f" + target.getName() + " &7to say: &f" + message));
            }
        }

        plugin.getStaffRequestManager().logAction(senderName, "FORCE_CHAT", target.getName(), message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.forcechat")) return Collections.emptyList();
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ENGLISH);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ENGLISH).startsWith(input))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
