package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class NightVisionCommand implements CommandExecutor, TabCompleter {

    private static final int DURATION_TICKS = Integer.MAX_VALUE / 2;
    private final EvaulxCore plugin;

    public NightVisionCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.nightvision")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player target = null;
        Boolean forceEnable = null;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("on") || arg.equalsIgnoreCase("enable")) {
                forceEnable = true;
            } else if (arg.equalsIgnoreCase("off") || arg.equalsIgnoreCase("disable")) {
                forceEnable = false;
            } else {
                Player found = Bukkit.getPlayer(arg);
                if (found != null) target = found;
            }
        }

        if (target != null && !sender.hasPermission("evaulx.nightvision.others")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission to toggle night vision for others.");
            return true;
        }

        if (target == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(CC.color("&cUsage: /nightvision [on|off] [player]"));
                return true;
            }
            target = (Player) sender;
        }

        boolean hasEffect = target.hasPotionEffect(PotionEffectType.NIGHT_VISION);
        boolean enable = forceEnable != null ? forceEnable : !hasEffect;

        boolean isSelf = target.equals(sender);

        if (enable) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, DURATION_TICKS, 0, false, false));
            target.sendMessage(CC.color("&8[&eNV&8] &eNight vision &aenabled&7."));
            if (!isSelf) sender.sendMessage(CC.color("&8[&eNV&8] &aEnabled &7night vision for &f" + target.getName() + "&7."));
        } else {
            target.removePotionEffect(PotionEffectType.NIGHT_VISION);
            target.sendMessage(CC.color("&8[&eNV&8] &7Night vision &cdisabled&7."));
            if (!isSelf) sender.sendMessage(CC.color("&8[&eNV&8] &cDisabled &7night vision for &f" + target.getName() + "&7."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.nightvision")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("on", "off"));
            if (sender.hasPermission("evaulx.nightvision.others")) {
                for (Player p : Bukkit.getOnlinePlayers()) options.add(p.getName());
            }
            String input = args[0].toLowerCase(Locale.ENGLISH);
            options.removeIf(o -> !o.toLowerCase(Locale.ENGLISH).startsWith(input));
            return options;
        }
        return Collections.emptyList();
    }
}
