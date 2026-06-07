package dev.evaulx.core.listeners;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.projectiles.ProjectileSource;

public class StaffModeListener implements Listener {

    private final EvaulxCore plugin;

    public StaffModeListener(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        PlayerProfile p = plugin.getPlayerManager().getProfile(e.getPlayer());
        if (p != null && p.isStaffMode()) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        PlayerProfile p = plugin.getPlayerManager().getProfile(e.getPlayer());
        if (p != null && p.isStaffMode()) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Player damager = getDamagingPlayer(e);
        if (damager != null && isInStaffMode(damager)) e.setCancelled(true);

        if (e.getEntity() instanceof Player) {
            Player damaged = (Player) e.getEntity();
            if (isInStaffMode(damaged)) e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && isInStaffMode((Player) e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent e) {
        PlayerProfile p = plugin.getPlayerManager().getProfile(e.getPlayer());
        if (p != null && p.isStaffMode()) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        PlayerProfile p = plugin.getPlayerManager().getProfile(e.getPlayer());
        if (p != null && p.isStaffMode()) e.setCancelled(true);
    }

    private Player getDamagingPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) return (Player) event.getDamager();

        if (event.getDamager() instanceof Projectile) {
            ProjectileSource source = ((Projectile) event.getDamager()).getShooter();
            if (source instanceof Player) return (Player) source;
        }

        return null;
    }

    private boolean isInStaffMode(Player player) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        return profile != null && profile.isStaffMode();
    }
}
