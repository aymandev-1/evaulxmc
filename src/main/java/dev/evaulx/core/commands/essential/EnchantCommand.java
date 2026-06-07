package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EnchantCommand implements CommandExecutor {

    private static final Map<String, Enchantment> ALIASES = new HashMap<>();

    static {
        alias("sharpness", Enchantment.SHARPNESS);
        alias("smite", Enchantment.SMITE);
        alias("bane", Enchantment.BANE_OF_ARTHROPODS);
        alias("efficiency", Enchantment.EFFICIENCY);
        alias("unbreaking", Enchantment.UNBREAKING);
        alias("fortune", Enchantment.FORTUNE);
        alias("looting", Enchantment.LOOTING);
        alias("protection", Enchantment.PROTECTION);
        alias("fireprotection", Enchantment.FIRE_PROTECTION);
        alias("blastprotection", Enchantment.BLAST_PROTECTION);
        alias("projectileprotection", Enchantment.PROJECTILE_PROTECTION);
        alias("featherfalling", Enchantment.FEATHER_FALLING);
        alias("fireaspect", Enchantment.FIRE_ASPECT);
        alias("knockback", Enchantment.KNOCKBACK);
        alias("silktouch", Enchantment.SILK_TOUCH);
        alias("thorns", Enchantment.THORNS);
        alias("respiration", Enchantment.RESPIRATION);
        alias("aquaaffinity", Enchantment.AQUA_AFFINITY);
        alias("power", Enchantment.POWER);
        alias("punch", Enchantment.PUNCH);
        alias("flame", Enchantment.FLAME);
        alias("infinity", Enchantment.INFINITY);
        alias("luck", Enchantment.LUCK_OF_THE_SEA);
        alias("lure", Enchantment.LURE);

        alias("0", Enchantment.PROTECTION);
        alias("1", Enchantment.FIRE_PROTECTION);
        alias("2", Enchantment.FEATHER_FALLING);
        alias("3", Enchantment.BLAST_PROTECTION);
        alias("4", Enchantment.PROJECTILE_PROTECTION);
        alias("5", Enchantment.RESPIRATION);
        alias("6", Enchantment.AQUA_AFFINITY);
        alias("7", Enchantment.THORNS);
        alias("16", Enchantment.SHARPNESS);
        alias("17", Enchantment.SMITE);
        alias("18", Enchantment.BANE_OF_ARTHROPODS);
        alias("19", Enchantment.KNOCKBACK);
        alias("20", Enchantment.FIRE_ASPECT);
        alias("21", Enchantment.LOOTING);
        alias("32", Enchantment.EFFICIENCY);
        alias("33", Enchantment.SILK_TOUCH);
        alias("34", Enchantment.UNBREAKING);
        alias("35", Enchantment.FORTUNE);
        alias("48", Enchantment.POWER);
        alias("49", Enchantment.PUNCH);
        alias("50", Enchantment.FLAME);
        alias("51", Enchantment.INFINITY);
        alias("61", Enchantment.LUCK_OF_THE_SEA);
        alias("62", Enchantment.LURE);
    }

    private final EvaulxCore plugin;

    public EnchantCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!sender.hasPermission("evaulx.enchant")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /enchant <enchantment> <level>"));
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(CC.color("&cHold an item to enchant."));
            return true;
        }

        Enchantment enchantment = findEnchantment(args[0]);
        if (enchantment == null) {
            sender.sendMessage(CC.color("&cUnknown enchantment."));
            return true;
        }

        Integer level = parseLevel(args[1]);
        if (level == null) {
            sender.sendMessage(CC.color("&cLevel must be a positive number."));
            return true;
        }

        item.addUnsafeEnchantment(enchantment, level);
        player.updateInventory();
        sender.sendMessage(CC.color("&7Added &c" + enchantment.getName() + " " + level + " &7to your held item."));
        return true;
    }

    private Enchantment findEnchantment(String input) {
        String key = normalize(input);
        Enchantment alias = ALIASES.get(key);
        if (alias != null) return alias;

        Enchantment byName = Enchantment.getByName(input.toUpperCase(Locale.ENGLISH));
        if (byName != null) return byName;
        return null;
    }

    private Integer parseLevel(String input) {
        try {
            int level = Integer.parseInt(input);
            return level > 0 ? level : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void alias(String name, Enchantment enchantment) {
        ALIASES.put(normalize(name), enchantment);
    }

    private static String normalize(String input) {
        return input.toLowerCase(Locale.ENGLISH).replace("_", "").replace("-", "").replace(" ", "");
    }
}
