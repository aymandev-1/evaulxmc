package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;

public class RenameCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public RenameCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        if (!sender.hasPermission("evaulx.rename")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getItemInHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(CC.color("&cYou must hold an item."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(CC.color("&cUsage: /rename <name|reset>"));
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) { player.sendMessage(CC.color("&cThis item cannot be renamed.")); return true; }

        if (args[0].equalsIgnoreCase("reset")) {
            meta.setDisplayName(null);
            item.setItemMeta(meta);
            player.sendMessage(CC.color("&aItem name reset."));
            return true;
        }

        boolean colorAllowed = sender.hasPermission("evaulx.rename.color");
        String name = String.join(" ", args);
        if (!colorAllowed) name = name.replaceAll("&[0-9a-fA-FkKlLmMnNoOrR]", "");

        meta.setDisplayName(CC.color(name));
        item.setItemMeta(meta);
        player.sendMessage(CC.color("&aRenamed to: " + CC.color(name)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}

