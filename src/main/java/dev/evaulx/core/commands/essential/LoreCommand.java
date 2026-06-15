package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class LoreCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public LoreCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (!sender.hasPermission("evaulx.lore")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        ItemStack item = player.getItemInHand();
        if (item.getType() == Material.AIR) { player.sendMessage(CC.color("&cYou must hold an item.")); return true; }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) { player.sendMessage(CC.color("&cThis item has no meta.")); return true; }
        if (args.length == 0) { player.sendMessage(CC.color("&cUsage: /lore <set|add|remove|clear>")); return true; }

        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        boolean color = sender.hasPermission("evaulx.lore.color");
        String sub = args[0].toLowerCase();

        switch (sub) {
            case "set": {
                if (args.length < 3) { player.sendMessage(CC.color("&cUsage: /lore set <line> <text>")); return true; }
                int line = parseInt(player, args[1]); if (line < 0) return true;
                String text = applyColor(joinFrom(args, 2), color);
                while (lore.size() < line - 1) lore.add("");
                if (line - 1 < lore.size()) lore.set(line - 1, text); else lore.add(text);
                meta.setLore(lore); item.setItemMeta(meta);
                player.sendMessage(CC.color("&aLore line &f" + line + " &aset."));
                break;
            }
            case "add":
                if (args.length < 2) { player.sendMessage(CC.color("&cUsage: /lore add <text>")); return true; }
                lore.add(applyColor(joinFrom(args, 1), color));
                meta.setLore(lore); item.setItemMeta(meta);
                player.sendMessage(CC.color("&aLore line added."));
                break;
            case "remove": {
                if (args.length < 2) { player.sendMessage(CC.color("&cUsage: /lore remove <line>")); return true; }
                int line = parseInt(player, args[1]); if (line < 0) return true;
                if (line > lore.size()) { player.sendMessage(CC.color("&cLine " + line + " does not exist.")); return true; }
                lore.remove(line - 1);
                meta.setLore(lore); item.setItemMeta(meta);
                player.sendMessage(CC.color("&aLore line &f" + line + " &aremoved."));
                break;
            }
            case "clear":
                meta.setLore(null); item.setItemMeta(meta);
                player.sendMessage(CC.color("&aLore cleared."));
                break;
            default:
                player.sendMessage(CC.color("&cUsage: /lore <set|add|remove|clear>"));
                break;
        }
        return true;
    }

    private int parseInt(Player p, String raw) {
        try {
            int n = Integer.parseInt(raw);
            if (n < 1) { p.sendMessage(CC.color("&cLine must be at least 1.")); return -1; }
            return n;
        } catch (NumberFormatException e) {
            p.sendMessage(CC.color("&cLine must be a number."));
            return -1;
        }
    }

    private String joinFrom(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    private String applyColor(String text, boolean allowed) {
        return allowed ? CC.color(text) : text.replaceAll("&[0-9a-fA-FkKlLmMnNoOrR]", "");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.lore")) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("set", "add", "remove", "clear").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}

