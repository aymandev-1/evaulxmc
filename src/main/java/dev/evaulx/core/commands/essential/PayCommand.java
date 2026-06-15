package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public PayCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        Player from = (Player) sender;
        if (!sender.hasPermission("evaulx.pay")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }
        if (args.length < 2) { from.sendMessage(CC.color("&cUsage: /pay <player> <amount>")); return true; }

        Player to = Bukkit.getPlayerExact(args[0]);
        if (to == null || to.equals(from)) { from.sendMessage(CC.color("&cPlayer not found or cannot pay yourself.")); return true; }

        long amount;
        try { amount = Long.parseLong(args[1]); } catch (NumberFormatException e) { from.sendMessage(CC.color("&cAmount must be a number.")); return true; }
        if (amount <= 0) { from.sendMessage(CC.color("&cAmount must be positive.")); return true; }

        if (!plugin.getCoinsManager().removeCoins(from.getUniqueId(), amount)) {
            from.sendMessage(CC.color("&cNot enough coins. You have &6" + String.format("%,d", plugin.getCoinsManager().getCoins(from.getUniqueId())) + "&c."));
            return true;
        }

        plugin.getCoinsManager().addCoins(to.getUniqueId(), amount);
        from.sendMessage(CC.color("&aSent &6" + String.format("%,d", amount) + " &acoins to &f" + to.getName() + "&a."));
        to.sendMessage(CC.color("&f" + from.getName() + " &asent you &6" + String.format("%,d", amount) + " &acoins."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.pay")) return Collections.emptyList();
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().filter(p -> !p.equals(sender))
                    .map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
