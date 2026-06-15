package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class WeatherCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public WeatherCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.weather")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /weather <clear|rain|storm>"));
            return true;
        }

        World world = sender instanceof Player ? ((Player) sender).getWorld() : null;
        if (world == null) {
            sender.sendMessage(CC.color("&cMust be a player to use this command."));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "clear":
            case "sun":
                world.setStorm(false);
                world.setThundering(false);
                world.setWeatherDuration(Integer.MAX_VALUE);
                sender.sendMessage(CC.color("&7Weather set to &fclear&7."));
                break;
            case "rain":
            case "rainy":
                world.setStorm(true);
                world.setThundering(false);
                world.setWeatherDuration(Integer.MAX_VALUE);
                sender.sendMessage(CC.color("&7Weather set to &frain&7."));
                break;
            case "storm":
            case "thunder":
            case "thunderstorm":
                world.setStorm(true);
                world.setThundering(true);
                world.setThunderDuration(Integer.MAX_VALUE);
                world.setWeatherDuration(Integer.MAX_VALUE);
                sender.sendMessage(CC.color("&7Weather set to &fthunderstorm&7."));
                break;
            default:
                sender.sendMessage(CC.color("&cUsage: /weather <clear|rain|storm>"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.weather")) return Collections.emptyList();
        if (args.length == 1) return Arrays.asList("clear", "rain", "storm");
        return Collections.emptyList();
    }
}
