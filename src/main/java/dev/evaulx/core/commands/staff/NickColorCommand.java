package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NickColorCommand implements CommandExecutor, TabCompleter {

    private static final List<String> COLOR_SUGGESTIONS = Arrays.asList(
            "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7",
            "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f",
            "&l", "&o", "&n", "&m", "reset"
    );

    private final EvaulxCore plugin;

    public NickColorCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }

        if (!sender.hasPermission("evaulx.nick.color") && !sender.hasPermission("evaulx.disguise.color")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null || !profile.isDisguised()) {
            plugin.getMessageManager().send(player, "disguise.not-disguised", "&cYou are not disguised.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(CC.color("&cUsage: /nickcolor <color|reset>"));
            player.sendMessage(CC.color("&7Colors: &0■&1■&2■&3■&4■&5■&6■&7■&8■&9■&a■&b■&c■&d■&e■&f■"));
            player.sendMessage(CC.color("&7Styles: &lBold &oBold-Italic &nUnderline &mStrike &r(reset)"));
            return true;
        }

        String input = args[0];
        String colorCode;
        if (input.equalsIgnoreCase("reset") || input.equalsIgnoreCase("none")) {
            colorCode = "";
        } else {
            // Accept both "&c" and "c" and "§c" forms
            String normalized = input.startsWith("&") ? input
                    : input.startsWith("§") ? "&" + input.substring(1)
                    : "&" + input;
            if (!normalized.matches("&[0-9a-fklmnorA-FKLMNOR]")) {
                player.sendMessage(CC.color("&cInvalid color. Use &f/nickcolor &c<0-9, a-f, l, o, n, m, or reset>&c."));
                return true;
            }
            colorCode = normalized.toLowerCase();
        }

        profile.setNameColor(colorCode);
        plugin.getPlayerManager().saveProfile(profile);
        plugin.getNameTagManager().applyNameTag(player);

        if (colorCode.isEmpty()) {
            player.sendMessage(CC.color("&7Your disguise name color has been &freset&7."));
        } else {
            player.sendMessage(CC.color("&7Disguise name color set to " + colorCode + "this color&7."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        String input = args[0].toLowerCase();
        return COLOR_SUGGESTIONS.stream()
                .filter(c -> c.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}
