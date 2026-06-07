package dev.evaulx.core.listeners;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyProtectionListener implements Listener {

    private final EvaulxCore plugin;
    private final Map<UUID, Long> lastDenyMessage = new ConcurrentHashMap<>();

    public LobbyProtectionListener(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (protects(event.getBlock().getLocation()) && !canBuild(event.getPlayer())) deny(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (protects(event.getBlock().getLocation()) && !canBuild(event.getPlayer())) deny(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (protects(event.getBlockClicked().getLocation()) && !canBuild(event.getPlayer())) deny(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (protects(event.getBlockClicked().getLocation()) && !canBuild(event.getPlayer())) deny(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        World world = clicked != null ? clicked.getWorld() : player.getWorld();
        if (!protects(clicked != null ? clicked.getLocation() : player.getLocation()) || canBuild(player)) return;

        if (event.getAction() == Action.PHYSICAL
                && plugin.getConfig().getBoolean("lobby-protection.prevent-physical-interact", true)) {
            deny(player, event);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && clicked != null
                && plugin.getConfig().getBoolean("lobby-protection.prevent-block-interact", true)
                && isProtectedInteractBlock(clicked.getType())) {
            deny(player, event);
            return;
        }

        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && plugin.getConfig().getBoolean("lobby-protection.prevent-dangerous-item-use", true)
                && event.getItem() != null
                && isDangerousItem(event.getItem().getType())) {
            deny(player, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.getConfig().getBoolean("lobby-protection.prevent-entity-interact", true)) return;
        if (protects(event.getRightClicked().getLocation())
                && isProtectedEntity(event.getRightClicked())
                && !canBuild(event.getPlayer())) {
            deny(event.getPlayer(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-entity-interact", true)
                && protects(event.getRightClicked().getLocation())
                && !canBuild(event.getPlayer())) {
            deny(event.getPlayer(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (protects(event.getEntity().getLocation()) && !canBuild(event.getPlayer())) deny(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Player player = playerFrom(event.getRemover());
        if (protects(event.getEntity().getLocation()) && (player == null || !canBuild(player))) deny(player, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-pistons", true) && protects(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-pistons", true) && protects(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-fire", true) && protects(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-fire", true) && protects(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-spread", true) && protects(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-liquid-flow", true) && protects(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-growth", true) && protects(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-growth", true) && protects(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-block-form", true) && protects(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-block-fade", true) && protects(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-leaf-decay", true) && protects(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-explosions", true) && protects(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-explosions", true) && protects(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-entity-block-change", true)
                && protects(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!plugin.getConfig().getBoolean("lobby-protection.no-mobs", true)) return;
        if (protects(event.getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSlimeSplit(SlimeSplitEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.no-mobs", true) && protects(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.no-mob-targeting", true) && protects(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfig().getBoolean("lobby-protection.prevent-damage", true)) return;
        if (event instanceof EntityDamageByEntityEvent) return;
        if (protects(event.getEntity().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("lobby-protection.prevent-damage", true)) return;
        if (!protects(event.getEntity().getLocation())) return;

        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
            return;
        }

        Player damager = playerFrom(event.getDamager());
        if (damager == null || !canBuild(damager)) deny(damager, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!plugin.getConfig().getBoolean("lobby-protection.prevent-projectiles", true)) return;
        if (!protects(event.getEntity().getLocation())) return;

        ProjectileSource source = event.getEntity().getShooter();
        if (source instanceof Player && !canBuild((Player) source)) {
            deny((Player) source, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-world-item-spawns", false)
                && protects(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-item-drops", true)
                && protects(event.getPlayer().getLocation()) && !canBuild(event.getPlayer())) {
            deny(event.getPlayer(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-item-pickup", true)
                && protects(event.getPlayer().getLocation()) && !canBuild(event.getPlayer())) {
            deny(event.getPlayer(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-hunger", true)
                && protects(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (plugin.getConfig().getBoolean("lobby-protection.prevent-portals", false)
                && protects(event.getPlayer().getLocation())
                && !canBuild(event.getPlayer())) {
            deny(event.getPlayer(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("lobby-protection.void-rescue.enabled", true)) return;
        if (event.getTo() == null || event.getFrom().getBlockY() == event.getTo().getBlockY()) return;
        if (!protects(event.getPlayer().getLocation())) return;

        double rescueY = plugin.getConfig().getDouble("lobby-protection.void-rescue.y-level", 0.0D);
        if (event.getTo().getY() > rescueY) return;

        event.getPlayer().teleport(resolveRescueLocation(event.getPlayer().getWorld()));
        event.getPlayer().sendMessage(CC.color(plugin.getConfig().getString("lobby-protection.void-rescue.message",
                "&cYou were returned to the lobby.")));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWeather(WeatherChangeEvent event) {
        if (!plugin.getConfig().getBoolean("lobby-protection.prevent-weather", true)) return;
        if (event.toWeatherState() && protects(event.getWorld())) event.setCancelled(true);
    }

    private boolean protects(World world) {
        if (world == null || !plugin.getConfig().getBoolean("lobby-protection.enabled", true)) return false;
        List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
        if (worlds.isEmpty()) return true;
        for (String configured : worlds) {
            if (world.getName().equalsIgnoreCase(configured)) return true;
        }
        return false;
    }

    private boolean protects(Location location) {
        if (location == null || location.getWorld() == null) return false;
        if (protects(location.getWorld())) return true;
        return isInsideRegion(location);
    }

    private boolean isInsideRegion(Location location) {
        if (!plugin.getConfig().getBoolean("lobby-protection.regions.enabled", true)) return false;
        org.bukkit.configuration.ConfigurationSection regions = plugin.getConfig().getConfigurationSection("lobby-protection.regions.entries");
        if (regions == null) return false;

        for (String key : regions.getKeys(false)) {
            String path = "lobby-protection.regions.entries." + key + ".";
            String world = plugin.getConfig().getString(path + "world", "");
            if (!location.getWorld().getName().equalsIgnoreCase(world)) continue;

            double minX = plugin.getConfig().getDouble(path + "min-x");
            double minY = plugin.getConfig().getDouble(path + "min-y");
            double minZ = plugin.getConfig().getDouble(path + "min-z");
            double maxX = plugin.getConfig().getDouble(path + "max-x");
            double maxY = plugin.getConfig().getDouble(path + "max-y");
            double maxZ = plugin.getConfig().getDouble(path + "max-z");
            if (location.getX() >= minX && location.getX() <= maxX
                    && location.getY() >= minY && location.getY() <= maxY
                    && location.getZ() >= minZ && location.getZ() <= maxZ) {
                return true;
            }
        }
        return false;
    }

    private Location resolveRescueLocation(World fallbackWorld) {
        String configuredWorld = plugin.getConfig().getString("spawn.world", "");
        World world = configuredWorld == null || configuredWorld.isEmpty() ? fallbackWorld : Bukkit.getWorld(configuredWorld);
        if (world == null) world = fallbackWorld;

        if (plugin.getConfig().getString("spawn.world", "").isEmpty()) {
            return world.getSpawnLocation();
        }

        return new Location(world,
                plugin.getConfig().getDouble("spawn.x", world.getSpawnLocation().getX()),
                plugin.getConfig().getDouble("spawn.y", world.getSpawnLocation().getY()),
                plugin.getConfig().getDouble("spawn.z", world.getSpawnLocation().getZ()),
                (float) plugin.getConfig().getDouble("spawn.yaw", 0.0D),
                (float) plugin.getConfig().getDouble("spawn.pitch", 0.0D));
    }

    private boolean canBuild(Player player) {
        if (player == null) return false;
        if (player.hasPermission("evaulx.protection.bypass")) return true;
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        return profile != null && profile.isBuildMode();
    }

    private void deny(Player player, Cancellable event) {
        event.setCancelled(true);
        if (player == null || !plugin.getConfig().getBoolean("lobby-protection.notify-on-deny", true)) return;

        long now = System.currentTimeMillis();
        Long last = lastDenyMessage.get(player.getUniqueId());
        long cooldown = plugin.getConfig().getLong("lobby-protection.deny-message-cooldown-ms", 1500L);
        if (last != null && now - last < cooldown) return;

        lastDenyMessage.put(player.getUniqueId(), now);
        player.sendMessage(CC.color(plugin.getConfig().getString("lobby-protection.deny-message",
                "&cYou cannot modify the lobby. Use &f/buildmode &cif you are staff.")));
    }

    private boolean isProtectedEntity(Entity entity) {
        return entity instanceof Hanging || entity instanceof ArmorStand;
    }

    private boolean isProtectedInteractBlock(Material material) {
        String name = material.name();
        if (name.endsWith("_DOOR") || name.endsWith("_DOOR_BLOCK")
                || name.endsWith("_FENCE_GATE")
                || name.contains("BUTTON")
                || name.contains("PRESSURE_PLATE")) {
            return true;
        }

        switch (material) {
            case ANVIL:
            case BEACON:
            case BED_BLOCK:
            case BREWING_STAND:
            case BURNING_FURNACE:
            case CAKE_BLOCK:
            case CHEST:
            case DAYLIGHT_DETECTOR:
            case DIODE_BLOCK_OFF:
            case DIODE_BLOCK_ON:
            case DISPENSER:
            case DROPPER:
            case ENCHANTMENT_TABLE:
            case ENDER_CHEST:
            case FURNACE:
            case HOPPER:
            case JUKEBOX:
            case LEVER:
            case NOTE_BLOCK:
            case REDSTONE_COMPARATOR_OFF:
            case REDSTONE_COMPARATOR_ON:
            case TRAPPED_CHEST:
            case TRAP_DOOR:
            case WORKBENCH:
                return true;
            default:
                return false;
        }
    }

    private boolean isDangerousItem(Material material) {
        String name = material.name();
        return name.contains("BUCKET")
                || name.equals("FLINT_AND_STEEL")
                || name.equals("FIREBALL")
                || name.equals("MONSTER_EGG")
                || name.equals("MONSTER_EGGS")
                || name.equals("ARMOR_STAND")
                || name.endsWith("MINECART")
                || name.equals("BOAT")
                || name.equals("EXPLOSIVE_MINECART")
                || name.equals("ENDER_PEARL");
    }

    private Player playerFrom(Entity entity) {
        if (entity instanceof Player) return (Player) entity;
        if (entity instanceof Projectile) {
            ProjectileSource source = ((Projectile) entity).getShooter();
            if (source instanceof Player) return (Player) source;
        }
        return null;
    }
}
