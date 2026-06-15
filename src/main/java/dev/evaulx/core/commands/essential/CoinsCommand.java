package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CoinsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ADMIN_SUBS = Arrays.asList("add", "remove", "set", "reset", "top");

    private final EvaulxCore plugin;

    public CoinsCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.coins")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        // /coins — own balance
        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage(CC.color("&cUsage: /coins <player|top>")); return true; }
            Player player = (Player) sender;
            showBalance(sender, player.getUniqueId(), player.getName());
            return true;
        }

        String sub = args[0].toLowerCase();

        // /coins top
        if (sub.equals("top")) {
            return showTop(sender);
        }

        // Admin subcommands
        if (ADMIN_SUBS.contains(sub) && !sub.equals("top")) {
            if (!sender.hasPermission("evaulx.coins.admin")) {
                plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
                return true;
            }
            if (args.length < 2) { sender.sendMessage(CC.color("&cUsage: /coins <add|remove|set|reset> <player> [amount]")); return true; }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage(CC.color("&cPlayer not online.")); return true; }

            if (sub.equals("reset")) {
                plugin.getCoinsManager().setCoins(target.getUniqueId(), 0);
                sender.sendMessage(CC.color("&aReset &f" + target.getName() + "&a's balance to 0."));
                target.sendMessage(CC.color("&7Your coin balance was reset to 0."));
                return true;
            }

            if (args.length < 3) { sender.sendMessage(CC.color("&cUsage: /coins " + sub + " <player> <amount>")); return true; }
            long amount;
            try { amount = Long.parseLong(args[2]); } catch (NumberFormatException e) { sender.sendMessage(CC.color("&cAmount must be a number.")); return true; }
            if (amount < 0) { sender.sendMessage(CC.color("&cAmount cannot be negative.")); return true; }

            switch (sub) {
                case "add":
                    plugin.getCoinsManager().addCoins(target.getUniqueId(), amount);
                    sender.sendMessage(CC.color("&aAdded &6" + fmt(amount) + " &acoins to &f" + target.getName() + "&a."));
                    target.sendMessage(CC.color("&6+" + fmt(amount) + " &7coins added."));
                    break;
                case "remove":
                    plugin.getCoinsManager().removeCoins(target.getUniqueId(), amount);
                    sender.sendMessage(CC.color("&aRemoved &6" + fmt(amount) + " &acoins from &f" + target.getName() + "&a."));
                    target.sendMessage(CC.color("&c-" + fmt(amount) + " &7coins removed."));
                    break;
                case "set":
                    plugin.getCoinsManager().setCoins(target.getUniqueId(), amount);
                    sender.sendMessage(CC.color("&aSet &f" + target.getName() + "&a's coins to &6" + fmt(amount) + "&a."));
                    target.sendMessage(CC.color("&7Your coin balance was set to &6" + fmt(amount) + "&7."));
                    break;
            }
            return true;
        }

        // /coins <player> — view another's balance
        if (!sender.hasPermission("evaulx.coins.others")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { sender.sendMessage(CC.color("&cPlayer not online.")); return true; }
        showBalance(sender, target.getUniqueId(), target.getName());
        return true;
    }

    private void showBalance(CommandSender sender, java.util.UUID uuid, String name) {
        long bal = plugin.getCoinsManager().getCoins(uuid);
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("  &6" + name + "'s Coins: &f" + fmt(bal)));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private boolean showTop(CommandSender sender) {
        List<Map.Entry<UUID, Long>> top = plugin.getCoinsManager().getTopBalances(10);
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("  &6Top Coin Balances"));
        if (top.isEmpty()) {
            sender.sendMessage(CC.color("  &7No data yet."));
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Long> e : top) {
                String name = Bukkit.getOfflinePlayer(e.getKey()).getName();
                if (name == null) name = e.getKey().toString().substring(0, 8);
                String medal = rank == 1 ? "&6#1" : rank == 2 ? "&7#2" : rank == 3 ? "&c#3" : "&8#" + rank;
                sender.sendMessage(CC.color("  " + medal + " &f" + name + " &8— &6" + fmt(e.getValue())));
                rank++;
            }
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    private String fmt(long n) { return String.format("%,d", n); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.coins")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> opts = new ArrayList<>();
            if (sender.hasPermission("evaulx.coins.admin")) opts.addAll(ADMIN_SUBS);
            opts.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            return opts.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && ADMIN_SUBS.contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
