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
        alias("sharpness", Enchantment.DAMAGE_ALL);
        alias("smite", Enchantment.DAMAGE_UNDEAD);
        alias("bane", Enchantment.DAMAGE_ARTHROPODS);
        alias("efficiency", Enchantment.DIG_SPEED);
        alias("unbreaking", Enchantment.DURABILITY);
        alias("fortune", Enchantment.LOOT_BONUS_BLOCKS);
        alias("looting", Enchantment.LOOT_BONUS_MOBS);
        alias("protection", Enchantment.PROTECTION_ENVIRONMENTAL);
        alias("fireprotection", Enchantment.PROTECTION_FIRE);
        alias("blastprotection", Enchantment.PROTECTION_EXPLOSIONS);
        alias("projectileprotection", Enchantment.PROTECTION_PROJECTILE);
        alias("featherfalling", Enchantment.PROTECTION_FALL);
        alias("fireaspect", Enchantment.FIRE_ASPECT);
        alias("knockback", Enchantment.KNOCKBACK);
        alias("silktouch", Enchantment.SILK_TOUCH);
        alias("thorns", Enchantment.THORNS);
        alias("respiration", Enchantment.OXYGEN);
        alias("aquaaffinity", Enchantment.WATER_WORKER);
        alias("power", Enchantment.ARROW_DAMAGE);
        alias("punch", Enchantment.ARROW_KNOCKBACK);
        alias("flame", Enchantment.ARROW_FIRE);
        alias("infinity", Enchantment.ARROW_INFINITE);
        alias("luck", Enchantment.LUCK);
        alias("lure", Enchantment.LURE);

        alias("0", Enchantment.PROTECTION_ENVIRONMENTAL);
        alias("1", Enchantment.PROTECTION_FIRE);
        alias("2", Enchantment.PROTECTION_FALL);
        alias("3", Enchantment.PROTECTION_EXPLOSIONS);
        alias("4", Enchantment.PROTECTION_PROJECTILE);
        alias("5", Enchantment.OXYGEN);
        alias("6", Enchantment.WATER_WORKER);
        alias("7", Enchantment.THORNS);
        alias("16", Enchantment.DAMAGE_ALL);
        alias("17", Enchantment.DAMAGE_UNDEAD);
        alias("18", Enchantment.DAMAGE_ARTHROPODS);
        alias("19", Enchantment.KNOCKBACK);
        alias("20", Enchantment.FIRE_ASPECT);
        alias("21", Enchantment.LOOT_BONUS_MOBS);
        alias("32", Enchantment.DIG_SPEED);
        alias("33", Enchantment.SILK_TOUCH);
        alias("34", Enchantment.DURABILITY);
        alias("35", Enchantment.LOOT_BONUS_BLOCKS);
        alias("48", Enchantment.ARROW_DAMAGE);
        alias("49", Enchantment.ARROW_KNOCKBACK);
        alias("50", Enchantment.ARROW_FIRE);
        alias("51", Enchantment.ARROW_INFINITE);
        alias("61", Enchantment.LUCK);
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
