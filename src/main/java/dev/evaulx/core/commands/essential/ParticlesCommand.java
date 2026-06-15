package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ParticlesCommand implements CommandExecutor, TabCompleter {

    // Basic tier — VIP+
    private static final Map<String, String> BASIC = new LinkedHashMap<>();
    // Premium tier — Legend+
    private static final Map<String, String> PREMIUM = new LinkedHashMap<>();

    static {
        BASIC.put("hearts",    "HEART");
        BASIC.put("notes",     "NOTE");
        BASIC.put("flame",     "FLAME");
        BASIC.put("snowflake", "SNOWFLAKE");
        BASIC.put("enchant",   "ENCHANT");

        PREMIUM.put("portal",   "PORTAL");
        PREMIUM.put("dragon",   "DRAGON_BREATH");
        PREMIUM.put("witch",    "WITCH");
        PREMIUM.put("stars",    "END_ROD");
        PREMIUM.put("firework", "FIREWORK");
        PREMIUM.put("soul",     "SOUL_FIRE_FLAME");
        PREMIUM.put("lava",     "DRIPPING_LAVA");
    }

    private static final Map<UUID, BukkitTask> TASKS = new ConcurrentHashMap<>();
    private static final Map<UUID, String>     ACTIVE = new ConcurrentHashMap<>();

    private final EvaulxCore plugin;

    public ParticlesCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    private static boolean spawnParticleReflect(String particleName, Location loc, int count,
                                                 double offX, double offY, double offZ, double speed) {
        try {
            Class<?> particleClass = Class.forName("org.bukkit.Particle");
            Object particle = Enum.valueOf((Class<Enum>) particleClass, particleName);
            loc.getWorld().getClass()
                    .getMethod("spawnParticle", particleClass, Location.class, int.class,
                            double.class, double.class, double.class, double.class)
                    .invoke(loc.getWorld(), particle, loc, count, offX, offY, offZ, speed);
            return true;
        } catch (Throwable ignored) { return false; }
    }

    @SuppressWarnings("unchecked")
    private static boolean particleExists(String particleName) {
        try {
            Class<?> particleClass = Class.forName("org.bukkit.Particle");
            Enum.valueOf((Class<Enum>) particleClass, particleName);
            return true;
        } catch (Throwable ignored) { return false; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }

        if (!sender.hasPermission("evaulx.particles")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            String current = ACTIVE.getOrDefault(player.getUniqueId(), "none");
            player.sendMessage(CC.color("&8[&6Particles&8] &7Current: &f" + current));
            player.sendMessage(CC.color("&7Basic &8— &fhearts, notes, flame, snowflake, enchant"));
            if (player.hasPermission("evaulx.particles.premium")) {
                player.sendMessage(CC.color("&7Premium &8— &fportal, dragon, witch, stars, firework, soul, lava"));
            }
            player.sendMessage(CC.color("&7Use &f/particles <type|off> &7to change."));
            return true;
        }

        String typeName = args[0].toLowerCase(Locale.ENGLISH);

        if (typeName.equals("off") || typeName.equals("none") || typeName.equals("clear")) {
            cancelTask(player.getUniqueId());
            player.sendMessage(CC.color("&8[&6Particles&8] &7Particle effect &cdisabled&7."));
            try { player.playSound(player.getLocation(), Sound.valueOf("BLOCK_FIRE_EXTINGUISH"), 0.5f, 1.2f); } catch (Throwable ignored) {}
            return true;
        }

        String particleName = BASIC.get(typeName);
        boolean isPremium = false;

        if (particleName == null) {
            particleName = PREMIUM.get(typeName);
            isPremium = particleName != null;
        }

        if (particleName == null) {
            player.sendMessage(CC.color("&cUnknown particle type: &f" + typeName));
            player.sendMessage(CC.color("&7Use &f/particles &7to see available types."));
            return true;
        }

        if (isPremium && !player.hasPermission("evaulx.particles.premium")) {
            player.sendMessage(CC.color("&cThis particle effect requires &6Legend &cor higher."));
            return true;
        }

        if (!particleExists(particleName)) {
            player.sendMessage(CC.color("&cThis particle type is not available on this server version."));
            return true;
        }

        cancelTask(player.getUniqueId());

        final String fParticleName = particleName;
        UUID uuid = player.getUniqueId();
        ACTIVE.put(uuid, typeName);

        BukkitTask[] holder = {null};
        holder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Player online = Bukkit.getPlayer(uuid);
            if (online == null || !online.isOnline()) {
                ACTIVE.remove(uuid);
                TASKS.remove(uuid);
                if (holder[0] != null) holder[0].cancel();
                return;
            }
            spawnParticleReflect(fParticleName, online.getLocation().add(0, 0.1, 0), 6, 0.4, 0.5, 0.4, 0.01);
        }, 0L, 10L);

        TASKS.put(uuid, holder[0]);

        player.sendMessage(CC.color("&8[&6Particles&8] &7Particle effect set to &f" + typeName + "&7."));
        try { player.playSound(player.getLocation(), Sound.valueOf("ENTITY_EXPERIENCE_ORB_PICKUP"), 0.6f, 1.6f); } catch (Throwable ignored) {}
        return true;
    }

    private void cancelTask(UUID uuid) {
        BukkitTask task = TASKS.remove(uuid);
        if (task != null) task.cancel();
        ACTIVE.remove(uuid);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.particles")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("off");
            options.addAll(BASIC.keySet());
            if (sender.hasPermission("evaulx.particles.premium")) options.addAll(PREMIUM.keySet());
            String input = args[0].toLowerCase(Locale.ENGLISH);
            options.removeIf(o -> !o.startsWith(input));
            return options;
        }
        return Collections.emptyList();
    }
}
