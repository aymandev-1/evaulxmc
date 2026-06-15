package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DailyCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public DailyCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (!sender.hasPermission("evaulx.daily")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (!plugin.getDailyRewardManager().canClaim(player.getUniqueId())) {
            long remaining = plugin.getDailyRewardManager().getTimeUntilNext(player.getUniqueId());
            long h = TimeUnit.MILLISECONDS.toHours(remaining);
            long m = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;
            long s = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;
            player.sendMessage(CC.color("&cAlready claimed! Come back in &f" + h + "h " + m + "m " + s + "s&c."));
            return true;
        }

        int coins = plugin.getDailyRewardManager().getCoinsReward();
        plugin.getDailyRewardManager().setClaimed(player.getUniqueId());
        plugin.getCoinsManager().addCoins(player.getUniqueId(), coins);

        long total = plugin.getCoinsManager().getCoins(player.getUniqueId());

        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &6&lDaily Reward Claimed!"));
        player.sendMessage(CC.color("  &7You received &6+" + String.format("%,d", coins) + " coins&7."));
        player.sendMessage(CC.color("  &7Balance: &6" + String.format("%,d", total)));
        player.sendMessage(CC.color(CC.SEPARATOR));

        try { player.playSound(player.getLocation(), Sound.valueOf("ENTITY_PLAYER_LEVELUP"), 0.8f, 1.2f); } catch (Throwable ignored) {}
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
