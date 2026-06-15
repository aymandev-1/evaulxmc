package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.creator.ContentCreatorProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class ShoutoutCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public ShoutoutCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("evaulx.shoutout")) {
            player.sendMessage(CC.color("&cYou don't have permission to use this command."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(CC.color("&cUsage: /shoutout <player>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(CC.color("&cPlayer &e" + args[0] + " &cis not online."));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(CC.color("&cYou can't shoutout yourself!"));
            return true;
        }

        ContentCreatorProfile ccProfile = plugin.getContentCreatorManager().getProfile(player.getUniqueId());
        String ccName = ccProfile != null ? ccProfile.effectiveDisplayName() : player.getName();

        Bukkit.broadcastMessage(CC.color(" "));
        Bukkit.broadcastMessage(CC.color("&8&m        &r &6&l✦ SHOUTOUT &8&m        "));
        Bukkit.broadcastMessage(CC.color("  &e" + ccName + " &7is shouting out &b" + target.getName() + "&7!"));
        Bukkit.broadcastMessage(CC.color("  &7Go show &b" + target.getName() + " &7some love!"));
        if (ccProfile != null && !ccProfile.getYoutube().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &7Watch &e" + ccName + " &7at &cyoutube.com/" + ccProfile.getYoutube()));
        if (ccProfile != null && !ccProfile.getTwitch().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &7Watch live at &5twitch.tv/" + ccProfile.getTwitch()));
        Bukkit.broadcastMessage(CC.color("&8&m                                  "));
        Bukkit.broadcastMessage(CC.color(" "));

        target.sendMessage(CC.color("&6&l✦ You just got shouted out by &e" + ccName + "&6&l! Check your chat!"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ENGLISH).startsWith(args[0].toLowerCase(Locale.ENGLISH)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}






