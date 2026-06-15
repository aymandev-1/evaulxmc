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
        // Each entry tries the 1.8 legacy name first, then the modern (1.13+) name.
        // getByName() exists on all server versions and is safe to call.
        safeAlias("sharpness",            "DAMAGE_ALL",               "SHARPNESS");
        safeAlias("smite",                "DAMAGE_UNDEAD",            "SMITE");
        safeAlias("bane",                 "DAMAGE_ARTHROPODS",        "BANE_OF_ARTHROPODS");
        safeAlias("efficiency",           "DIG_SPEED",                "EFFICIENCY");
        safeAlias("unbreaking",           "DURABILITY",               "UNBREAKING");
        safeAlias("fortune",              "LOOT_BONUS_BLOCKS",        "FORTUNE");
        safeAlias("looting",              "LOOT_BONUS_MOBS",          "LOOTING");
        safeAlias("protection",           "PROTECTION_ENVIRONMENTAL", "PROTECTION");
        safeAlias("fireprotection",       "PROTECTION_FIRE",          "FIRE_PROTECTION");
        safeAlias("blastprotection",      "PROTECTION_EXPLOSIONS",    "BLAST_PROTECTION");
        safeAlias("projectileprotection", "PROTECTION_PROJECTILE",    "PROJECTILE_PROTECTION");
        safeAlias("featherfalling",       "PROTECTION_FALL",          "FEATHER_FALLING");
        safeAlias("fireaspect",           "FIRE_ASPECT");
        safeAlias("knockback",            "KNOCKBACK");
        safeAlias("silktouch",            "SILK_TOUCH");
        safeAlias("thorns",               "THORNS");
        safeAlias("respiration",          "OXYGEN",                   "RESPIRATION");
        safeAlias("aquaaffinity",         "WATER_WORKER",             "AQUA_AFFINITY");
        safeAlias("power",                "ARROW_DAMAGE",             "POWER");
        safeAlias("punch",                "ARROW_KNOCKBACK",          "PUNCH");
        safeAlias("flame",                "ARROW_FIRE",               "FLAME");
        safeAlias("infinity",             "ARROW_INFINITE",           "INFINITY");
        safeAlias("luck",                 "LUCK",                     "LUCK_OF_THE_SEA");
        safeAlias("lure",                 "LURE");

        // Numeric ID aliases (1.8 enchantment IDs)
        safeAlias("0",  "PROTECTION_ENVIRONMENTAL", "PROTECTION");
        safeAlias("1",  "PROTECTION_FIRE",          "FIRE_PROTECTION");
        safeAlias("2",  "PROTECTION_FALL",          "FEATHER_FALLING");
        safeAlias("3",  "PROTECTION_EXPLOSIONS",    "BLAST_PROTECTION");
        safeAlias("4",  "PROTECTION_PROJECTILE",    "PROJECTILE_PROTECTION");
        safeAlias("5",  "OXYGEN",                   "RESPIRATION");
        safeAlias("6",  "WATER_WORKER",             "AQUA_AFFINITY");
        safeAlias("7",  "THORNS");
        safeAlias("16", "DAMAGE_ALL",               "SHARPNESS");
        safeAlias("17", "DAMAGE_UNDEAD",            "SMITE");
        safeAlias("18", "DAMAGE_ARTHROPODS",        "BANE_OF_ARTHROPODS");
        safeAlias("19", "KNOCKBACK");
        safeAlias("20", "FIRE_ASPECT");
        safeAlias("21", "LOOT_BONUS_MOBS",          "LOOTING");
        safeAlias("32", "DIG_SPEED",                "EFFICIENCY");
        safeAlias("33", "SILK_TOUCH");
        safeAlias("34", "DURABILITY",               "UNBREAKING");
        safeAlias("35", "LOOT_BONUS_BLOCKS",        "FORTUNE");
        safeAlias("48", "ARROW_DAMAGE",             "POWER");
        safeAlias("49", "ARROW_KNOCKBACK",          "PUNCH");
        safeAlias("50", "ARROW_FIRE",               "FLAME");
        safeAlias("51", "ARROW_INFINITE",           "INFINITY");
        safeAlias("61", "LUCK",                     "LUCK_OF_THE_SEA");
        safeAlias("62", "LURE");
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
        Enchantment alias = ALIASES.get(normalize(input));
        if (alias != null) return alias;
        try {
            Enchantment byName = Enchantment.getByName(input.toUpperCase(Locale.ENGLISH));
            if (byName != null) return byName;
        } catch (Throwable ignored) {}
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

    // Tries each enchantment name in order; on 1.8 the legacy name works, on modern the new name works.
    private static void safeAlias(String alias, String... enchNames) {
        for (String enchName : enchNames) {
            try {
                Enchantment e = Enchantment.getByName(enchName);
                if (e != null) {
                    ALIASES.put(normalize(alias), e);
                    return;
                }
            } catch (Throwable ignored) {}
        }
    }

    private static String normalize(String input) {
        return input.toLowerCase(Locale.ENGLISH).replace("_", "").replace("-", "").replace(" ", "");
    }
}

