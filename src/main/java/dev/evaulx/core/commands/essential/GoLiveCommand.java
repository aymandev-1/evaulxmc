package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class GoLiveCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;
    private final boolean isOffAir;

    public GoLiveCommand(EvaulxCore plugin, boolean isOffAir) {
        this.plugin = plugin;
        this.isOffAir = isOffAir;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("evaulx.golive")) {
            player.sendMessage(CC.color("&cYou don't have permission to use this command."));
            return true;
        }

        if (isOffAir) {
            if (!plugin.getContentCreatorManager().isLive(player.getUniqueId())) {
                player.sendMessage(CC.color("&cYou are not currently marked as live."));
                return true;
            }
            plugin.getContentCreatorManager().goOffAir(player);
            player.sendMessage(CC.color("&7You are now marked as &coffline&7. Thanks for streaming!"));
            return true;
        }

        String platform = args.length >= 1 ? args[0] : null;
        String error = plugin.getContentCreatorManager().goLive(player, platform);
        if (error != null) player.sendMessage(CC.color(error));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!isOffAir && args.length == 1) {
            return Arrays.asList("twitch", "youtube").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ENGLISH)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
