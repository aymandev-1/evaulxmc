package dev.evaulx.core.listeners;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.commands.staff.VanishCommand;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.staff.StaffRequestManager.FreezeInfo;
import dev.evaulx.core.staff.StaffRequestManager.StaffModeItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class StaffToolListener implements Listener {

    private final EvaulxCore plugin;
    private final Random random = new Random();

    public StaffToolListener(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrozenMove(PlayerMoveEvent event) {
        if (!plugin.getStaffRequestManager().isFrozen(event.getPlayer())) return;
        if (sameBlock(event.getFrom(), event.getTo())) return;

        event.setTo(event.getFrom());
        remindFrozen(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrozenInteract(PlayerInteractEvent event) {
        if (plugin.getStaffRequestManager().isFrozen(event.getPlayer())) {
            event.setCancelled(true);
            remindFrozen(event.getPlayer());
            return;
        }

        handleStaffModeItem(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrozenBlockBreak(BlockBreakEvent event) {
        if (plugin.getStaffRequestManager().isFrozen(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrozenBlockPlace(BlockPlaceEvent event) {
        if (plugin.getStaffRequestManager().isFrozen(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrozenDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && plugin.getStaffRequestManager().isFrozen((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrozenDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && plugin.getStaffRequestManager().isFrozen((Player) event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onStaffInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!plugin.getConfig().getBoolean("staff-tools.staffmode-items.prevent-item-moving", true)) return;
        Player player = (Player) event.getWhoClicked();
        if (!isStaffMode(player)) return;

        if (plugin.getStaffRequestManager().getStaffModeItem(event.getCurrentItem()) != null
                || plugin.getStaffRequestManager().getStaffModeItem(event.getCursor()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInspect(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;
        if (!isStaffMode(event.getPlayer())) return;

        ItemStack item = event.getPlayer().getInventory().getItemInHand();
        StaffModeItem staffItem = plugin.getStaffRequestManager().getStaffModeItem(item);
        if (staffItem != StaffModeItem.INSPECT_PLAYER) return;

        Player target = (Player) event.getRightClicked();
        event.setCancelled(true);
        event.getPlayer().openInventory(target.getInventory());
        plugin.getMessageManager().send(event.getPlayer(), "staff-tools.inspect",
                "&7Opened &f{target}&7's inventory.",
                plugin.getMessageManager().placeholders("{target}", target.getName()));
        plugin.getStaffRequestManager().logAction(event.getPlayer().getName(), "INSPECT", target.getName(), "Opened inventory");
    }

    private void handleStaffModeItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isStaffMode(player)) return;

        ItemStack hand = event.getItem();
        if (hand == null) hand = player.getItemInHand();
        StaffModeItem item = plugin.getStaffRequestManager().getStaffModeItem(hand);
        if (item == null) return;

        event.setCancelled(true);
        switch (item) {
            case TELEPORT_COMPASS:
                teleportCompass(player);
                break;
            case RANDOM_TELEPORT:
                randomTeleport(player);
                break;
            case TOGGLE_VANISH:
                toggleVanish(player);
                break;
            case REPORTS:
                plugin.getGuiManager().openReports(player, false);
                break;
            default:
                break;
        }
    }

    private void teleportCompass(Player player) {
        Block target = player.getTargetBlock((Set<Material>) null, 100);
        if (target == null || target.getType() == Material.AIR) {
            plugin.getMessageManager().send(player, "staff-tools.no-target-block", "&cNo target block found.");
            return;
        }

        Location location = target.getLocation().add(0.5, 1.0, 0.5);
        location.setYaw(player.getLocation().getYaw());
        location.setPitch(player.getLocation().getPitch());
        player.teleport(location);
        plugin.getMessageManager().send(player, "staff-tools.teleport-compass", "&7Teleported to target block.");
        plugin.getStaffRequestManager().logAction(player.getName(), "STAFFMODE_COMPASS", null, "Teleported to target block");
    }

    private void randomTeleport(Player player) {
        List<Player> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (plugin.getStaffRequestManager().canReceiveStaffAlerts(online)) continue;
            candidates.add(online);
        }

        if (candidates.isEmpty()) {
            plugin.getMessageManager().send(player, "staff-tools.no-random-target", "&cNo non-staff players online.");
            return;
        }

        Player target = candidates.get(random.nextInt(candidates.size()));
        player.teleport(target.getLocation());
        plugin.getMessageManager().send(player, "staff-tools.random-teleport",
                "&7Randomly teleported to &f{target}&7.",
                plugin.getMessageManager().placeholders("{target}", target.getName()));
        plugin.getStaffRequestManager().logAction(player.getName(), "RANDOM_TELEPORT", target.getName(), "Staffmode item");
    }

    private void toggleVanish(Player player) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null) {
            plugin.getMessageManager().send(player, "profile-loading", "&cThat player's profile is still loading.");
            return;
        }

        boolean vanished = !profile.isVanished();
        VanishCommand vanishCommand = new VanishCommand(plugin);
        vanishCommand.setVanished(player, profile, vanished);
        vanishCommand.sendVanishFeedback(player, vanished);
        plugin.getStaffRequestManager().logAction(player.getName(), "VANISH", player.getName(), vanished ? "enabled" : "disabled");
    }

    private boolean isStaffMode(Player player) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        return profile != null && profile.isStaffMode();
    }

    private boolean sameBlock(Location from, Location to) {
        return to != null
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ();
    }

    private void remindFrozen(Player player) {
        FreezeInfo info = plugin.getStaffRequestManager().getFreezeInfo(player.getUniqueId());
        String reason = info != null ? info.getReason() : "No reason specified";
        plugin.getMessageManager().send(player, "staff-tools.frozen-reminder",
                "&cYou are frozen. &7Reason: &f{reason}",
                plugin.getMessageManager().placeholders("{reason}", reason));
    }
}
