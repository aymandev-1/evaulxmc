package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HeadCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public HeadCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }

        if (!sender.hasPermission("evaulx.head")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        String targetName;

        if (args.length > 0) {
            targetName = args[0];
        } else {
            targetName = player.getName();
        }

        // Validate Minecraft username format
        if (!targetName.matches("[A-Za-z0-9_]{1,16}")) {
            player.sendMessage(CC.color("&cInvalid player name: &f" + targetName));
            return true;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(CC.color("&cYour inventory is full."));
            return true;
        }

        Material headMat = Material.getMaterial("PLAYER_HEAD") != null ? Material.getMaterial("PLAYER_HEAD") : Material.getMaterial("SKULL_ITEM");
        ItemStack head = new ItemStack(headMat != null ? headMat : Material.PAPER);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwner(targetName);
        meta.setDisplayName(CC.color("&f" + targetName + "&7's Head"));
        head.setItemMeta(meta);

        player.getInventory().addItem(head);
        player.sendMessage(CC.color("&8[&aBuilder&8] &7Given head of &f" + targetName + "&7."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.head")) return Collections.emptyList();
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
