package dev.evaulx.core.listeners;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.creator.ContentCreatorProfile;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ContentCreatorListener implements Listener {

    private final EvaulxCore plugin;
    private final Map<UUID, BukkitTask> ownerAuraTasks = new ConcurrentHashMap<>();

    public ContentCreatorListener(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Add to boss bar regardless of CC status
        plugin.getContentCreatorManager().addPlayerToBossBar(player);

        // Server join message (sent 1 second after join so it lands clearly)
        if (plugin.getConfig().getBoolean("server-message.enabled", false)) {
            String text = plugin.getConfig().getString("server-message.text", "");
            if (!text.isEmpty()) {
                TaskUtil.syncLater(() -> {
                    if (player.isOnline()) player.sendMessage(CC.color(text));
                }, 20L);
            }
        }

        // Owner join effect — dramatic title + sound for all, plus a FLAME particle aura
        if (player.hasPermission("evaulx.owner.aura")) {
            TaskUtil.syncLater(() -> {
                if (!player.isOnline()) return;
                String title = CC.color("&4&l★ OWNER ONLINE ★");
                String sub = CC.color("&c" + player.getName() + " &7has entered the server");
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendTitle(title, sub);
                    try {
                        online.playSound(online.getLocation(), Sound.valueOf("ENTITY_WITHER_SPAWN"), 0.5f, 1.5f);
                    } catch (Throwable ignored) {
                        try {
                            online.playSound(online.getLocation(), Sound.valueOf("ENTITY_ELDER_GUARDIAN_CURSE"), 0.5f, 1.0f);
                        } catch (Throwable ignored2) {}
                    }
                }
                startOwnerAura(player);
            }, 30L);
        }

        if (!plugin.getContentCreatorManager().isCreator(player.getUniqueId())) return;

        // Grant CC command permissions immediately so they work from the moment of join.
        plugin.getContentCreatorManager().applyCreatorPermissions(player);

        // Delay the visible effects so the player is visible to everyone and their profile is confirmed loaded.
        TaskUtil.syncLater(() -> {
            if (!player.isOnline()) return;
            plugin.getContentCreatorManager().onCreatorJoin(player);
            plugin.getContentCreatorManager().announceJoin(player);
        }, 30L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getContentCreatorManager().removePlayerFromBossBar(player);
        stopOwnerAura(player.getUniqueId());
        if (!plugin.getContentCreatorManager().isCreator(player.getUniqueId())) return;
        plugin.getContentCreatorManager().onCreatorQuit(player);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        boolean victimCC = plugin.getContentCreatorManager().isCreator(victim.getUniqueId());
        boolean killerCC = killer != null && plugin.getContentCreatorManager().isCreator(killer.getUniqueId());
        if (!victimCC && !killerCC) return;

        if (killerCC) {
            ContentCreatorProfile kp = plugin.getContentCreatorManager().getProfile(killer.getUniqueId());
            String kName = kp.effectiveDisplayName();
            boolean live = plugin.getContentCreatorManager().isLive(killer.getUniqueId());
            String liveStr = live ? " &c[LIVE]" : "";
            Bukkit.broadcastMessage(CC.color("&8[&6CC Kill&8] &e" + kName + liveStr
                    + " &7eliminated &f" + victim.getName() + "&7!"));
            if (live && !kp.getTwitch().isEmpty())
                Bukkit.broadcastMessage(CC.color("  &7Watch it live at &5twitch.tv/" + kp.getTwitch()));
            else if (live && !kp.getYoutube().isEmpty())
                Bukkit.broadcastMessage(CC.color("  &7Watch it live at &cyoutube.com/" + kp.getYoutube()));
            else if (live && !kp.getTiktok().isEmpty())
                Bukkit.broadcastMessage(CC.color("  &7Watch it live at &ftiktok.com/@" + kp.getTiktok()));
        } else {
            ContentCreatorProfile vp = plugin.getContentCreatorManager().getProfile(victim.getUniqueId());
            String vName = vp.effectiveDisplayName();
            String killerName = killer != null ? killer.getName() : "the environment";
            Bukkit.broadcastMessage(CC.color("&8[&6CC&8] &e" + vName
                    + " &7was eliminated by &f" + killerName + "&7! F in the chat."));
        }
    }

    // Runs after ChatListener (EventPriority.HIGH) — prepends the CC tag to public chat.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getContentCreatorManager().isCreator(event.getPlayer().getUniqueId())) return;
        String tag = CC.color(plugin.getContentCreatorManager().getCCChatTag());
        event.setFormat(tag + event.getFormat());
    }

    private void startOwnerAura(Player player) {
        stopOwnerAura(player.getUniqueId());
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) { stopOwnerAura(player.getUniqueId()); return; }
            try {
                Location loc = player.getLocation().add(0, 0.1, 0);
                Class<?> partCls = Class.forName("org.bukkit.Particle");
                Object flame = Enum.valueOf((Class<Enum>) partCls, "FLAME");
                loc.getWorld().getClass()
                        .getMethod("spawnParticle", partCls, Location.class, int.class,
                                double.class, double.class, double.class, double.class)
                        .invoke(loc.getWorld(), flame, loc, 8, 0.4, 0.5, 0.4, 0.01);
            } catch (Throwable ignored) {}
        }, 0L, 5L);
        ownerAuraTasks.put(player.getUniqueId(), task);
    }

    private void stopOwnerAura(UUID uuid) {
        BukkitTask t = ownerAuraTasks.remove(uuid);
        if (t != null) t.cancel();
    }
}
