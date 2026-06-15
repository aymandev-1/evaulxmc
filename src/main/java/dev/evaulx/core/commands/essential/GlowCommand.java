package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class GlowCommand implements CommandExecutor, TabCompleter {

    // Works on 1.8 (DURABILITY) and 1.9+ (UNBREAKING)
    private static final Enchantment GLOW_ENCH = resolveEnchantment("DURABILITY", "UNBREAKING");

    private static Enchantment resolveEnchantment(String... names) {
        for (String name : names) {
            try {
                Enchantment e = Enchantment.getByName(name);
                if (e != null) return e;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private final EvaulxCore plugin;

    public GlowCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (!sender.hasPermission("evaulx.glow")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (GLOW_ENCH == null) {
            player.sendMessage(CC.color("&cGlow is not available on this server version."));
            return true;
        }

        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() == Material.AIR) { player.sendMessage(CC.color("&cYou must hold an item.")); return true; }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;

        boolean hasGlow = meta.hasEnchant(GLOW_ENCH) && meta.getItemFlags().contains(ItemFlag.HIDE_ENCHANTS);
        boolean forceOff = args.length > 0 && args[0].equalsIgnoreCase("off");
        boolean forceOn  = args.length > 0 && args[0].equalsIgnoreCase("on");

        if (forceOff || (!forceOn && hasGlow)) {
            meta.removeEnchant(GLOW_ENCH);
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
            player.sendMessage(CC.color("&7Glow &cremoved&7."));
        } else {
            meta.addEnchant(GLOW_ENCH, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
            player.sendMessage(CC.color("&7Glow &aapplied&7."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.glow")) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("on", "off").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}

