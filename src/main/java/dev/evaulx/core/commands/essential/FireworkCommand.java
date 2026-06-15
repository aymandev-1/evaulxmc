package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.command.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Store-rank perk: launches a colourful firework at the player's location.
 * Usage: /firework
 */
public class FireworkCommand implements CommandExecutor, TabCompleter {

    private static final Random RANDOM = new Random();
    private static final FireworkEffect.Type[] TYPES = FireworkEffect.Type.values();

    private final EvaulxCore plugin;

    public FireworkCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission("evaulx.store.firework")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .flicker(RANDOM.nextBoolean())
                .trail(true)
                .with(TYPES[RANDOM.nextInt(TYPES.length)])
                .withColor(Color.fromRGB(RANDOM.nextInt(0xFFFFFF)))
                .withFade(Color.fromRGB(RANDOM.nextInt(0xFFFFFF)))
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);

        player.sendMessage(CC.color("&8[&6Perks&8] &7Launched a firework! &e✦"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
