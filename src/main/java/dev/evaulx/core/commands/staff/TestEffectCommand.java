package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TestEffectCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public TestEffectCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }

        if (!sender.hasPermission("evaulx.testeffect")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(CC.color("&cUsage: /testeffect <effect> [seconds] [amplifier]"));
            player.sendMessage(CC.color("&7Example: &f/testeffect speed 30 2"));
            player.sendMessage(CC.color("&7Use &f/testeffect clear &7to remove all effects."));
            return true;
        }

        if (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("off")) {
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.sendMessage(CC.color("&8[&9Dev&8] &7All potion effects cleared."));
            return true;
        }

        PotionEffectType type = findEffect(args[0]);
        if (type == null) {
            player.sendMessage(CC.color("&cUnknown effect: &f" + args[0]));
            player.sendMessage(CC.color("&7Valid examples: &fspeed, slowness, strength, night_vision, jump_boost, regeneration"));
            return true;
        }

        int seconds = 30;
        int amplifier = 0;

        if (args.length > 1) {
            try { seconds = Math.max(1, Math.min(3600, Integer.parseInt(args[1]))); }
            catch (NumberFormatException e) {
                player.sendMessage(CC.color("&cInvalid duration: &f" + args[1]));
                return true;
            }
        }
        if (args.length > 2) {
            try { amplifier = Math.max(0, Math.min(255, Integer.parseInt(args[2]))); }
            catch (NumberFormatException e) {
                player.sendMessage(CC.color("&cInvalid amplifier: &f" + args[2]));
                return true;
            }
        }

        player.addPotionEffect(new PotionEffect(type, seconds * 20, amplifier, false, true));
        player.sendMessage(CC.color("&8[&9Dev&8] &7Applied &f" + formatName(type) + " &7(level &f" + (amplifier + 1) + "&7, &f" + seconds + "s&7)."));
        return true;
    }

    private PotionEffectType findEffect(String name) {
        String normalized = name.toUpperCase(Locale.ENGLISH).replace("-", "_").replace(" ", "_");
        for (PotionEffectType type : PotionEffectType.values()) {
            if (type.getName().equalsIgnoreCase(normalized)) return type;
            if (type.getName().equalsIgnoreCase(name)) return type;
        }
        return null;
    }

    private String formatName(PotionEffectType type) {
        String key = type.getName().toLowerCase(Locale.ENGLISH);
        StringBuilder sb = new StringBuilder();
        for (String part : key.split("_")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1).toLowerCase(Locale.ENGLISH));
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.testeffect")) return Collections.emptyList();
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ENGLISH);
            List<String> effects = new ArrayList<>();
            effects.add("clear");
            for (PotionEffectType type : PotionEffectType.values()) {
                String key = type.getName().toLowerCase(Locale.ENGLISH);
                if (key.startsWith(input)) effects.add(key);
            }
            Collections.sort(effects);
            return effects;
        }
        if (args.length == 2) return Arrays.asList("30", "60", "120", "300");
        if (args.length == 3) return Arrays.asList("0", "1", "2", "4");
        return Collections.emptyList();
    }
}
