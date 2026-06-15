package dev.evaulx.core.gui;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.managers.AppealManager.Appeal;
import dev.evaulx.core.managers.GrantTemplateManager.GrantTemplate;
import dev.evaulx.core.utils.TaskUtil;
import dev.evaulx.core.managers.GrantManager.PendingGrant;
import dev.evaulx.core.managers.PunishmentPresetManager.Preset;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.PlayerNote;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.staff.StaffRequestManager.StaffAction;
import dev.evaulx.core.staff.StaffRequestManager.StaffRequest;
import dev.evaulx.core.staff.StaffRequestManager.StaffSession;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.DisplayUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuiManager implements Listener {

    private final EvaulxCore plugin;

    private final Map<UUID, UUID> punishTargets        = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> grantTargets         = new ConcurrentHashMap<>();
    private final Map<UUID, String> grantTargetNames   = new ConcurrentHashMap<>();
    private final Map<UUID, String> grantRanks         = new ConcurrentHashMap<>();
    private final Map<UUID, String> grantDurations     = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> grantRankSlots     = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> grantTemplateSlots = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> grantDurationSlots = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> grantReasonSlots   = new ConcurrentHashMap<>();
    private final Map<UUID, Integer>  tagPages         = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, TagGuiEntry>> tagSlots = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerAction> pickerActions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> profileTargets       = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> punHistTargets       = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> punHistPages      = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> rankSlots = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> rankPages         = new ConcurrentHashMap<>();
    private final Map<UUID, String> editingRanks       = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> rankCategorySlots = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> appealListPages   = new ConcurrentHashMap<>();
    private final Map<UUID, String>  appealDetailIds   = new ConcurrentHashMap<>();

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MMM d, yyyy");

    // 1.8-compatible Material resolution — tries modern name first, falls back to legacy.
    private static Material mat(String modern, String legacy) {
        try { Material m = Material.valueOf(modern); if (m != null) return m; } catch (Throwable e) {}
        try { Material m = Material.valueOf(legacy);  if (m != null) return m; } catch (Throwable e) {}
        return Material.STONE;
    }
    private static final Material MAT_GRAY_DYE              = mat("GRAY_DYE",                     "INK_SACK");
    private static final Material MAT_LIME_DYE               = mat("LIME_DYE",                     "INK_SACK");
    private static final Material MAT_RED_DYE                = mat("RED_DYE",                      "INK_SACK");
    private static final Material MAT_YELLOW_DYE             = mat("YELLOW_DYE",                   "INK_SACK");
    private static final Material MAT_GREEN_DYE              = mat("GREEN_DYE",                    "INK_SACK");
    private static final Material MAT_MAGENTA_DYE            = mat("MAGENTA_DYE",                  "INK_SACK");
    private static final Material MAT_LIME_GLASS_PANE        = mat("LIME_STAINED_GLASS_PANE",      "STAINED_GLASS_PANE");
    private static final Material MAT_REDSTONE_TORCH         = mat("REDSTONE_TORCH",               "REDSTONE_TORCH_ON");
    private static final Material MAT_INK_SAC                = mat("INK_SAC",                      "INK_SACK");
    private static final Material MAT_ENDER_EYE              = mat("ENDER_EYE",                    "EYE_OF_ENDER");
    private static final Material MAT_WRITABLE_BOOK          = mat("WRITABLE_BOOK",                "BOOK_AND_QUILL");
    private static final Material MAT_GRAY_GLASS_PANE        = mat("GRAY_STAINED_GLASS_PANE",      "STAINED_GLASS_PANE");
    private static final Material MAT_ORANGE_GLASS_PANE      = mat("ORANGE_STAINED_GLASS_PANE",    "STAINED_GLASS_PANE");
    private static final Material MAT_BLUE_GLASS_PANE        = mat("BLUE_STAINED_GLASS_PANE",      "STAINED_GLASS_PANE");
    private static final Material MAT_CYAN_GLASS_PANE        = mat("CYAN_STAINED_GLASS_PANE",      "STAINED_GLASS_PANE");
    private static final Material MAT_BLACK_GLASS_PANE       = mat("BLACK_STAINED_GLASS_PANE",     "STAINED_GLASS_PANE");
    private static final Material MAT_YELLOW_GLASS_PANE      = mat("YELLOW_STAINED_GLASS_PANE",    "STAINED_GLASS_PANE");
    private static final Material MAT_LIGHT_BLUE_GLASS_PANE  = mat("LIGHT_BLUE_STAINED_GLASS_PANE","STAINED_GLASS_PANE");
    private static final Material MAT_RED_GLASS_PANE         = mat("RED_STAINED_GLASS_PANE",       "STAINED_GLASS_PANE");
    private static final Material MAT_PURPLE_GLASS_PANE      = mat("PURPLE_STAINED_GLASS_PANE",    "STAINED_GLASS_PANE");
    private static final Material MAT_GREEN_GLASS_PANE       = mat("GREEN_STAINED_GLASS_PANE",     "STAINED_GLASS_PANE");
    private static final Material MAT_MAGENTA_GLASS          = mat("MAGENTA_STAINED_GLASS",         "STAINED_GLASS");
    private static final Material MAT_MAGENTA_GLASS_PANE     = mat("MAGENTA_STAINED_GLASS_PANE",   "STAINED_GLASS_PANE");
    private static final Material MAT_PLAYER_HEAD            = mat("PLAYER_HEAD",                   "SKULL_ITEM");
    private static final Material MAT_CLOCK                  = mat("CLOCK",                         "WATCH");
    private static final Material MAT_SPYGLASS               = mat("SPYGLASS",                      "COMPASS");
    private static final Material MAT_LIME_CONCRETE          = mat("LIME_CONCRETE",                 "EMERALD_BLOCK");
    private static final Material MAT_RED_CONCRETE           = mat("RED_CONCRETE",                  "REDSTONE_BLOCK");
    private static final Material MAT_GRASS_BLOCK            = mat("GRASS_BLOCK",                   "GRASS");
    private static final Material MAT_SPAWN_EGG              = mat("ZOMBIE_SPAWN_EGG",              "MONSTER_EGG");
    private static final Material MAT_GRAY_WOOL              = mat("GRAY_WOOL",                     "WOOL");
    private static final Material MAT_FIRE_CHARGE            = mat("FIRE_CHARGE",                   "SULPHUR");

    @SuppressWarnings("unchecked")
    private static int safeGetPing(Player p) {
        try {
            Object handle = p.getClass().getMethod("getHandle").invoke(p);
            return handle.getClass().getField("ping").getInt(handle);
        } catch (Throwable ignored) { return 0; }
    }

    // Slots used for paginated content inside a bordered 54-slot inventory
    private static final int[] CONTENT_SLOTS_54 = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34,
        37,38,39,40,41,42,43
    };

    public GuiManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  OPEN METHODS
    // =====================================================================

    public void openStaffPanel(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, staffTitle());
        fillBorder(inv, MAT_GRAY_GLASS_PANE);

        inv.setItem(4, item(Material.NETHER_STAR, "&b&lEvaulxMC &7— Staff Panel",
            Arrays.asList(
                "&7Online: &f" + Bukkit.getOnlinePlayers().size() + " &8/ &f" + Bukkit.getMaxPlayers(),
                "&7Staff online: &f" + onlineStaffCount(),
                "&7Open reports: &f" + plugin.getStaffRequestManager().getReports().size(),
                "&7HelpOP queue: &f" + plugin.getStaffRequestManager().getHelpOps().size(),
                "&7Frozen: &f" + plugin.getStaffRequestManager().getFrozenCount(),
                "&7Pending grants: &f" + plugin.getGrantManager().getPendingGrants().size())));

        // Row 2 — player management (10-16)
        inv.setItem(10, badge(Material.BOOK, "&c&lReports", plugin.getStaffRequestManager().getReports().size(),
            "&7Claim and close open player reports."));
        inv.setItem(11, badge(Material.PAPER, "&e&lHelpOP", plugin.getStaffRequestManager().getHelpOps().size(),
            "&7View and handle help requests."));
        inv.setItem(12, item(Material.DIAMOND_SWORD, "&c&lPunish Player",
            Arrays.asList("&7Select a player and punishment preset.", "&aClick to open.")));
        inv.setItem(13, badge(Material.PACKED_ICE, "&b&lFreeze Player",
            plugin.getStaffRequestManager().getFrozenCount(), "&7Freeze or unfreeze a player."));
        inv.setItem(14, item(Material.CHEST, "&6&lInspect Inventory",
            Arrays.asList("&7View a player's inventory in real time.", "&aClick to open.")));
        inv.setItem(15, item(MAT_WRITABLE_BOOK, "&e&lPlayer Notes",
            Arrays.asList("&7View or add staff notes for a player.", "&aClick to open.")));
        inv.setItem(16, item(MAT_CLOCK, "&7&lMod Logs",
            Arrays.asList("&7View the moderation action log.", "&aClick to open.")));

        // Row 3 — admin tools (19-25)
        inv.setItem(19, badge(Material.GOLD_INGOT, "&6&lGrant Rank",
            plugin.getGrantManager().getPendingGrants().size(), "&7Grant a rank to any player."));
        inv.setItem(20, item(MAT_ENDER_EYE,
            "&7&lToggle Vanish", Arrays.asList("&7Disappear from other players' view.", "&aClick to toggle.")));
        inv.setItem(21, item(Material.COMPASS,
            "&a&lStaff Mode", Arrays.asList("&7Equip staff tools and bypass permissions.", "&aClick to toggle.")));
        inv.setItem(22, item(MAT_PLAYER_HEAD, "&f&lPlayer Profile",
            Arrays.asList("&7Full info panel: punishments, notes, grants.", "&7Includes quick-action buttons.", "&aClick to select player.")));
        inv.setItem(23, item(Material.IRON_DOOR,
            (plugin.getConfig().getBoolean("lobby-protection.enabled", true) ? "&a" : "&c") + "&lLobby Protection",
            Arrays.asList("&7Manage lobby protection rules.",
                "&7Status: " + enabledText(plugin.getConfig().getBoolean("lobby-protection.enabled", true)),
                "&aClick to open.")));
        inv.setItem(24, item(Material.BEACON, "&b&lStaff Dashboard",
            Arrays.asList("&7Live overview: reports, grants, sessions.", "&aClick to open.")));
        inv.setItem(25, item(MAT_REDSTONE_TORCH,
            (plugin.getConfig().getBoolean("maintenance.enabled", false) ? "&a" : "&c") + "&lMaintenance",
            Arrays.asList("&7Toggle and configure maintenance mode.",
                "&7Status: " + enabledText(plugin.getConfig().getBoolean("maintenance.enabled", false)),
                "&aClick to open.")));

        // Row 4 — utilities (28-34)
        inv.setItem(28, badge(Material.BOOK, "&f&lStaff Sessions",
            plugin.getStaffRequestManager().getActiveStaffSessionCount(), "&7View active and recent staff sessions."));
        inv.setItem(29, item(MAT_MAGENTA_GLASS, "&d&lStreamer Mode",
            Arrays.asList("&7Toggle or configure your streamer alias.", "&aClick to open.")));
        inv.setItem(30, badge(Material.NAME_TAG, "&e&lRanks",
            plugin.getRankManager().getRanks().size(), "&7View and edit all loaded ranks."));
        inv.setItem(31, item(MAT_SPYGLASS, "&f&lWhois Lookup",
            Arrays.asList("&7Full info output for any player.", "&aClick to select player.")));
        inv.setItem(32, item(Material.ARROW, "&a&lPing Check",
            Arrays.asList("&7Check a player's current ping.", "&aClick to select player.")));
        inv.setItem(33, item(MAT_ENDER_EYE, "&c&lIP Check",
            Arrays.asList("&7View IP and online alt accounts.", "&aClick to select player.")));
        inv.setItem(34, badge(Material.GOLD_NUGGET, "&6&lPending Grants",
            plugin.getGrantManager().getPendingGrants().size(), "&7Approve or deny pending rank grants."));

        // Row 5 — extra tools (37-43)
        int pendingAppeals = plugin.getAppealManager().getPending().size();
        inv.setItem(37, badge(MAT_WRITABLE_BOOK, "&6&lAppeals", pendingAppeals, "&7Review and resolve player appeals."));

        inv.setItem(49, item(Material.BARRIER, "&c&lClose", Collections.singletonList("&7Close this panel.")));
        player.openInventory(inv);
    }

    public void openAppealsGui(Player player) {
        List<Appeal> pending = plugin.getAppealManager().getPending();
        int page = appealListPages.getOrDefault(player.getUniqueId(), 0);
        int maxPages = Math.max(1, (int) Math.ceil((double) pending.size() / CONTENT_SLOTS_54.length));
        if (page < 0) { page = 0; appealListPages.put(player.getUniqueId(), 0); }
        if (page >= maxPages) { page = maxPages - 1; appealListPages.put(player.getUniqueId(), page); }

        Inventory inv = Bukkit.createInventory(null, 54, appealsTitle());
        fillBorder(inv, MAT_ORANGE_GLASS_PANE);

        inv.setItem(4, item(Material.GOLD_INGOT, "&6&lAppeals",
            Arrays.asList(
                "&7Pending appeals: &f" + pending.size(),
                "&7Page &f" + (page + 1) + " &7of &f" + maxPages,
                "",
                "&7Click an appeal to view details.")));

        int start = page * CONTENT_SLOTS_54.length;
        for (int i = 0; i < CONTENT_SLOTS_54.length; i++) {
            int idx = start + i;
            if (idx >= pending.size()) break;
            Appeal a = pending.get(idx);
            inv.setItem(CONTENT_SLOTS_54[i], item(Material.PAPER,
                "&f" + a.playerName,
                Arrays.asList(
                    "&7ID: &f" + a.punishmentId,
                    "&7Reason: &f" + clip(a.reason, 40),
                    "&7Submitted: &f" + formatDate(a.submittedAt),
                    "",
                    "&eClick &7to review.")));
        }

        if (page > 0) inv.setItem(45, item(Material.ARROW, "&7Previous Page", Collections.singletonList("&7Page " + page)));
        inv.setItem(49, item(Material.BARRIER, "&c&lClose", Collections.singletonList("&7Close.")));
        if (page < maxPages - 1) inv.setItem(53, item(Material.ARROW, "&7Next Page", Collections.singletonList("&7Page " + (page + 2))));
        player.openInventory(inv);
    }

    public void openAppealDetailGui(Player player, Appeal appeal) {
        appealDetailIds.put(player.getUniqueId(), appeal.punishmentId);
        Inventory inv = Bukkit.createInventory(null, 27, appealDetailPrefix() + appeal.playerName);
        fillBorder(inv, MAT_ORANGE_GLASS_PANE);

        inv.setItem(13, item(Material.BOOK, "&6&lAppeal Details",
            Arrays.asList(
                "&7Player: &f" + appeal.playerName,
                "&7Punishment ID: &f" + appeal.punishmentId,
                "&7Reason: &f" + appeal.reason,
                "&7Submitted: &f" + formatDate(appeal.submittedAt))));

        inv.setItem(11, item(MAT_LIME_CONCRETE, "&a&lAccept",
            Arrays.asList("&7Pardon the punishment and mark appeal accepted.", "&aClick to accept.")));

        inv.setItem(15, item(MAT_RED_CONCRETE, "&c&lDeny",
            Arrays.asList("&7Reject the appeal. Punishment remains active.",
                "&cClick to deny.",
                "&7For a custom reason use:",
                "&f/appeal deny " + appeal.punishmentId + " <reason>")));

        inv.setItem(22, item(Material.ARROW, "&7Back", Collections.singletonList("&7Return to appeals list.")));
        player.openInventory(inv);
    }

    public void openLobbyProtection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, lobbyProtectionTitle());
        fillBorder(inv, MAT_BLUE_GLASS_PANE);
        String world = player.getWorld().getName();

        inv.setItem(4, item(Material.BEACON, "&b&lLobby Protection",
            Arrays.asList(
                "&7World: &f" + world,
                "&7Scope: &f" + protectionMode(),
                "&7This world protected: " + enabledText(isProtectedWorld(world)),
                "&7Master status: " + enabledText(plugin.getConfig().getBoolean("lobby-protection.enabled", true)))));

        // Row 2: world management (10-16)
        boolean masterOn = plugin.getConfig().getBoolean("lobby-protection.enabled", true);
        inv.setItem(10, item(masterOn ? Material.REDSTONE_BLOCK : Material.EMERALD_BLOCK,
            masterOn ? "&c&lDisable Protection" : "&a&lEnable Protection",
            Arrays.asList("&7Toggles all lobby protection globally.",
                "&7Current: " + enabledText(masterOn), "&aClick to toggle.")));
        inv.setItem(11, item(Material.ANVIL, "&6&lEnforce This World",
            Arrays.asList("&7Enables every protection option for &f" + world + "&7.", "&aClick to enforce.")));
        inv.setItem(12, item(MAT_GRASS_BLOCK, "&a&lProtect This World",
            Arrays.asList("&7Adds &f" + world + " &7to the protected world list.", "&aClick to add.")));
        inv.setItem(13, item(Material.DIRT, "&c&lUnprotect This World",
            Arrays.asList("&7Removes &f" + world + " &7from the protected world list.", "&cClick to remove.")));
        inv.setItem(14, item(Material.COMPASS, "&e&lProtect All Worlds",
            Arrays.asList("&7Clears the world list so every world is protected.", "&aClick to apply.")));
        inv.setItem(15, item(plugin.getConfig().getBoolean("lobby-protection.notify-on-deny", true) ? MAT_GREEN_DYE : MAT_GRAY_DYE,
            "&7&lDeny Notifications", Arrays.asList(
                "&7Notify players when an action is denied.",
                "&7Current: " + enabledText(plugin.getConfig().getBoolean("lobby-protection.notify-on-deny", true)),
                "&aClick to toggle.")));
        inv.setItem(16, item(Material.DIAMOND_PICKAXE, "&6&lBuild Mode",
            Arrays.asList("&7Toggle your personal build bypass.", "&aClick to toggle.")));

        // Row 3: feature toggles (19-25)
        inv.setItem(19, toggleItem(Material.IRON_SWORD, "Damage", "prevent-damage"));
        inv.setItem(20, toggleItem(Material.TNT, "Explosions", "prevent-explosions"));
        inv.setItem(21, toggleItem(MAT_SPAWN_EGG, "Mobs", "no-mobs"));
        inv.setItem(22, toggleItem(Material.CHEST, "Item Drops/Pickup", "prevent-item-drops"));
        inv.setItem(23, toggleItem(Material.LEVER, "Interactions", "prevent-block-interact"));
        inv.setItem(24, toggleItem(Material.WATER_BUCKET, "Liquid/Growth", "prevent-liquid-flow"));
        inv.setItem(25, toggleItem(Material.ENDER_PEARL, "Void Rescue", "void-rescue.enabled"));

        // Row 4: extra toggles (28-34)
        inv.setItem(28, toggleItem(Material.WHEAT, "Hunger", "prevent-hunger"));
        inv.setItem(29, toggleItem(MAT_GRAY_WOOL, "Weather Changes", "prevent-weather"));
        inv.setItem(30, toggleItem(Material.ARROW, "Projectiles", "prevent-projectiles"));
        inv.setItem(31, toggleItem(MAT_FIRE_CHARGE, "Fire Spread", "prevent-fire"));
        inv.setItem(32, item(Material.BOOK, "&7&lPrint Status",
            Arrays.asList("&7Print the full protection status to chat.", "&aClick to print.")));

        inv.setItem(49, item(Material.BARRIER, "&c&lClose", Collections.singletonList("&7Close this menu.")));
        player.openInventory(inv);
    }

    public void openRankPresetConfirm(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, rankPresetConfirmTitle());
        fillBorder(inv, MAT_RED_GLASS_PANE);
        inv.setItem(13, item(Material.TNT, "&c&l! WARNING !",
            Arrays.asList(
                "&7This will wipe every stored rank.",
                "&7A backup file will be created first.",
                "&7The EvaulxMC preset ranks will be installed.",
                "",
                "&cThis cannot be undone.")));
        inv.setItem(11, item(Material.EMERALD_BLOCK, "&a&lConfirm Reset",
            Arrays.asList("&7Wipe ranks and install presets.", "&aClick to confirm.")));
        inv.setItem(15, item(Material.REDSTONE_BLOCK, "&c&lCancel",
            Collections.singletonList("&7Return without changing ranks.")));
        player.openInventory(inv);
    }

    public void openStaffDashboard(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, staffDashboardTitle());
        fillBorder(inv, MAT_CYAN_GLASS_PANE);

        inv.setItem(4, item(Material.NETHER_STAR, "&b&lStaff Dashboard",
            Arrays.asList(
                "&7Staff online: &f" + onlineStaffCount() + " &8/ &f" + Bukkit.getOnlinePlayers().size(),
                "&7Open reports: &f" + plugin.getStaffRequestManager().getReports().size(),
                "&7HelpOP queue: &f" + plugin.getStaffRequestManager().getHelpOps().size(),
                "&7Frozen: &f" + plugin.getStaffRequestManager().getFrozenCount(),
                "&7Pending grants: &f" + plugin.getGrantManager().getPendingGrants().size(),
                "&7Active sessions: &f" + plugin.getStaffRequestManager().getActiveStaffSessionCount(),
                "&7Maintenance: " + enabledText(plugin.getConfig().getBoolean("maintenance.enabled", false)))));

        // Quick-access row (10-16)
        inv.setItem(10, badge(Material.BOOK, "&c&lReports", plugin.getStaffRequestManager().getReports().size(), "&7Open report queue."));
        inv.setItem(11, badge(Material.PAPER, "&e&lHelpOP", plugin.getStaffRequestManager().getHelpOps().size(), "&7Open help request queue."));
        inv.setItem(12, badge(Material.GOLD_INGOT, "&6&lPending Grants", plugin.getGrantManager().getPendingGrants().size(), "&7Review pending rank grants."));
        inv.setItem(13, badge(Material.PACKED_ICE, "&b&lFrozen Players", plugin.getStaffRequestManager().getFrozenCount(), "&7Currently frozen player count."));
        inv.setItem(14, badge(Material.EMERALD, "&a&lStaff Sessions", plugin.getStaffRequestManager().getActiveStaffSessionCount(), "&7View active staff sessions."));
        inv.setItem(15, item(MAT_REDSTONE_TORCH,
            (plugin.getConfig().getBoolean("maintenance.enabled", false) ? "&a" : "&c") + "&lMaintenance",
            Arrays.asList("&7Manage maintenance mode.", "&aClick to open.")));

        // Separator
        ItemStack sep = glassPane(MAT_BLACK_GLASS_PANE);
        for (int s = 18; s <= 26; s++) inv.setItem(s, sep);

        // Recent actions (rows 4-5: 28-34, 37-43)
        int slot = 28;
        for (StaffAction action : plugin.getStaffRequestManager().getRecentActions(14)) {
            if (slot > 43) break;
            if (slot == 35) slot = 37;
            if (slot > 43) break;
            inv.setItem(slot++, item(actionMaterial(action.getAction()), "&7" + action.getAction(),
                Arrays.asList(
                    "&7Actor: &f" + action.getActor(),
                    "&7Target: &f" + (action.getTarget() == null ? "none" : action.getTarget()),
                    "&7" + action.getFormattedTime(),
                    "&8" + clip(action.getDetail(), 40))));
            if (slot == 35) slot = 37;
        }

        inv.setItem(49, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Return to staff panel.")));
        player.openInventory(inv);
    }

    public void openMaintenance(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, maintenanceTitle());
        fillBorder(inv, MAT_ORANGE_GLASS_PANE);

        boolean enabled = plugin.getConfig().getBoolean("maintenance.enabled", false);
        String reason = plugin.getConfig().getString("maintenance.reason", "Server maintenance");
        int allowed = plugin.getConfig().getStringList("maintenance.allowed-players").size();

        inv.setItem(13, item(MAT_REDSTONE_TORCH, "&c&lMaintenance",
            Arrays.asList(
                "&7Status: " + enabledText(enabled),
                "&7Reason: &f" + reason,
                "&7Allowed players: &f" + allowed)));

        inv.setItem(10, item(enabled ? Material.REDSTONE_BLOCK : Material.EMERALD_BLOCK,
            enabled ? "&c&lDisable Maintenance" : "&a&lEnable Maintenance",
            Arrays.asList("&7Current: " + enabledText(enabled), "&aClick to switch.")));
        inv.setItem(11, item(Material.PAPER, "&7&lPrint Status",
            Collections.singletonList("&7Print maintenance status to chat.")));
        inv.setItem(12, item(MAT_PLAYER_HEAD, "&e&lAllowed Players &7(" + allowed + ")",
            Arrays.asList("&7Players who bypass maintenance.", "&7Add: &f/maintenance allow <player>")));
        inv.setItem(14, item(Material.BOOK, "&f&lStaff Sessions",
            Collections.singletonList("&7Open staff session tracking.")));

        inv.setItem(22, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Return to dashboard.")));
        player.openInventory(inv);
    }

    public void openStaffSessions(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, staffSessionsTitle());
        fillBorder(inv, MAT_CYAN_GLASS_PANE);

        List<StaffSession> active = plugin.getStaffRequestManager().getActiveStaffSessions();
        List<StaffSession> recent = plugin.getStaffRequestManager().getRecentStaffSessions(250);

        inv.setItem(4, item(Material.BOOK, "&b&lStaff Sessions",
            Arrays.asList(
                "&7Active sessions: &f" + active.size(),
                "&7Recent total: &f" + recent.size())));

        // Active sessions in row 2 (10-16)
        int slot = 10;
        for (StaffSession session : active) {
            if (slot > 16) break;
            inv.setItem(slot++, item(Material.EMERALD, "&a&l" + session.getName(),
                Arrays.asList("&7Status: &aOnline",
                    "&7Started: &f" + session.getStartedFormatted(),
                    "&7Duration: &f" + session.getDurationString())));
        }
        while (slot <= 16) inv.setItem(slot++, glassPane(MAT_GRAY_GLASS_PANE));

        // Separator
        ItemStack sep = glassPane(MAT_BLACK_GLASS_PANE);
        for (int s = 18; s <= 26; s++) inv.setItem(s, sep);

        // Recent sessions in rows 4-5 (28-34, 37-43)
        slot = 28;
        for (StaffSession session : recent) {
            if (slot > 43) break;
            if (slot == 35) slot = 37;
            if (slot > 43) break;
            inv.setItem(slot++, item(Material.PAPER, "&7" + session.getName(),
                Arrays.asList("&7Started: &f" + session.getStartedFormatted(),
                    "&7Duration: &f" + session.getDurationString())));
            if (slot == 35) slot = 37;
        }

        inv.setItem(49, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Return to dashboard.")));
        player.openInventory(inv);
    }

    public void openPendingGrants(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, pendingGrantsTitle());
        fillBorder(inv, MAT_YELLOW_GLASS_PANE);

        List<PendingGrant> pending = plugin.getGrantManager().getPendingGrants();
        inv.setItem(4, item(Material.GOLD_INGOT, "&6&lPending Grants &7(" + pending.size() + ")",
            Arrays.asList(
                "&7Grants awaiting approval.",
                "",
                "&aLeft-click &7a grant to approve.",
                "&cRight-click &7a grant to deny.")));

        int slot = 10;
        for (PendingGrant pg : pending) {
            if (slot > 43) break;
            if (slot == 17) slot = 19;
            else if (slot == 26) slot = 28;
            else if (slot == 35) slot = 37;
            if (slot > 43) break;
            inv.setItem(slot++, item(Material.GOLD_INGOT, "&6&l" + pg.getId(),
                Arrays.asList(
                    "&7Target: &f" + pg.getTargetName(),
                    "&7Rank: &f" + pg.getRankName(),
                    "&7Requester: &f" + pg.getRequesterName(),
                    "&7Reason: &f" + pg.getReason(),
                    "",
                    "&aLeft-click &7to approve.",
                    "&cRight-click &7to deny.")));
            if (slot == 17) slot = 19;
            else if (slot == 26) slot = 28;
            else if (slot == 35) slot = 37;
        }

        inv.setItem(49, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Return to dashboard.")));
        player.openInventory(inv);
    }

    public void openPlayerProfile(Player viewer, OfflinePlayer target) {
        profileTargets.put(viewer.getUniqueId(), target.getUniqueId());
        PlayerProfile profile = target.isOnline()
                ? plugin.getPlayerManager().getProfile(target.getPlayer())
                : plugin.getDatabaseManager().loadProfile(target.getUniqueId(), target.getName());
        if (profile == null) {
            viewer.sendMessage(CC.color("&cProfile not found."));
            return;
        }

        List<Punishment> history = plugin.getPunishmentManager().getHistory(target.getUniqueId());
        long activeBans   = countActive(history, true);
        long activeMutes  = countActive(history, false);
        int noteCount     = plugin.getNoteManager().getNoteCount(target.getUniqueId());
        int grantCount    = plugin.getGrantManager().getGrants(target.getUniqueId()).size();
        boolean online    = target.isOnline();
        boolean frozen    = online && plugin.getStaffRequestManager().isFrozen(target.getPlayer());
        int ping          = online ? safeGetPing(target.getPlayer()) : -1;
        String name       = safeName(target.getName());

        Inventory inv = Bukkit.createInventory(null, 54,
                playerProfileTitle().replace("{player}", name));
        fillBorder(inv, MAT_LIGHT_BLUE_GLASS_PANE);

        // Header (slot 4 overrides the top-border center)
        inv.setItem(4, item(MAT_PLAYER_HEAD, (online ? "&a" : "&7") + "&l" + name,
            Arrays.asList(
                "&7UUID: &f" + target.getUniqueId(),
                "&7Status: " + (online ? "&aOnline" + (ping >= 0 ? " &8(&f" + ping + "ms&8)" : "") : "&cOffline"),
                "&7IP: &f" + (profile.getIp() == null ? "unknown" : profile.getIp()),
                "&7Rank: &f" + profile.getRankName(),
                "&7First join: &f" + formatDate(profile.getFirstJoin()),
                "&7Last seen: &f" + formatDate(profile.getLastSeen()))));

        // Stats row (10-16)
        inv.setItem(10, item(Material.NAME_TAG, "&e&lRanks",
            Arrays.asList(
                "&7Primary: &f" + profile.getRankName(),
                "&7Extra: &f" + (profile.getExtraRanks().isEmpty() ? "none" : joinText(profile.getExtraRanks())),
                "", "&aClick &7to open grant GUI.")));
        inv.setItem(11, item(activeBans > 0 || activeMutes > 0 ? Material.BARRIER : Material.BOOK,
            (activeBans > 0 || activeMutes > 0 ? "&c" : "&7") + "&lPunishments &8(" + history.size() + ")",
            Arrays.asList(
                "&7Total: &f" + history.size(),
                "&7Active bans: " + (activeBans > 0 ? "&c" : "&a") + activeBans,
                "&7Active mutes: " + (activeMutes > 0 ? "&c" : "&a") + activeMutes,
                "", "&aClick &7to view history.")));
        inv.setItem(12, item(MAT_WRITABLE_BOOK, "&e&lNotes &8(" + noteCount + ")",
            Arrays.asList("&7Staff notes on this player.", "&7Count: &f" + noteCount,
                "", "&aClick &7to view notes.")));
        inv.setItem(13, item(Material.GOLD_INGOT, "&6&lGrants &8(" + grantCount + ")",
            Collections.singletonList("&7Stored rank grants: &f" + grantCount)));
        inv.setItem(14, item(MAT_CLOCK, "&7&lPlaytime",
            Arrays.asList(
                "&7First join: &f" + formatDate(profile.getFirstJoin()),
                "&7Last seen: &f" + formatDate(profile.getLastSeen()))));
        inv.setItem(15, item(MAT_ENDER_EYE, "&7&lStaff Flags",
            Arrays.asList(
                "&7Vanished: " + boolColor(profile.isVanished()),
                "&7Staff mode: " + boolColor(profile.isStaffMode()),
                "&7God mode: " + boolColor(profile.isGodMode()),
                "&7Disguised: " + boolColor(profile.isDisguised()),
                "&7Frozen: " + boolColor(frozen),
                "&7Streamer: " + boolColor(profile.isStreamerMode()))));
        inv.setItem(16, item(Material.COMPASS, "&7&lIP Info",
            Arrays.asList(
                "&7IP: &f" + (profile.getIp() == null ? "unknown" : profile.getIp()),
                "", "&aClick &7to run IP check.")));

        // Quick-action row A (19-25)
        inv.setItem(19, item(Material.GOLD_INGOT, "&6&lGrant Rank",
            Arrays.asList("&7Grant a rank to this player.", "&aClick to open grant GUI.")));
        inv.setItem(20, item(Material.DIAMOND_SWORD, "&c&lPunish",
            Arrays.asList("&7Open the punishment preset menu.", "&aClick to punish.")));
        inv.setItem(21, item(online ? Material.FEATHER : MAT_GRAY_DYE, online ? "&e&lKick" : "&8Kick",
            Arrays.asList(online ? "&7Kick this player from the server." : "&7Player is offline.",
                online ? "&aClick to kick." : "&cUnavailable.")));
        inv.setItem(22, item(activeBans > 0 ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
            activeBans > 0 ? "&a&lUnban" : "&c&lBan",
            Arrays.asList(activeBans > 0 ? "&7Remove the active ban." : "&7Permanently ban this player.",
                "&aClick to " + (activeBans > 0 ? "unban." : "ban."))));
        inv.setItem(23, item(activeMutes > 0 ? MAT_LIME_DYE : MAT_RED_DYE,
            activeMutes > 0 ? "&a&lUnmute" : "&c&lMute",
            Arrays.asList(activeMutes > 0 ? "&7Remove the active mute." : "&7Permanently mute this player.",
                "&aClick to " + (activeMutes > 0 ? "unmute." : "mute."))));
        inv.setItem(24, item(frozen ? MAT_FIRE_CHARGE : Material.PACKED_ICE,
            frozen ? "&a&lUnfreeze" : "&b&lFreeze",
            Arrays.asList(frozen ? "&7Remove freeze on this player." : "&7Freeze this player in place.",
                online ? "&aClick to " + (frozen ? "unfreeze." : "freeze.") : "&cPlayer is offline.")));
        inv.setItem(25, item(online ? Material.CHEST : MAT_GRAY_DYE,
            online ? "&6&lInspect Inventory" : "&8Inspect Inventory",
            Arrays.asList(online ? "&7View this player's inventory." : "&7Player is offline.",
                online ? "&aClick to inspect." : "&cUnavailable.")));

        // Quick-action row B (28-34)
        inv.setItem(28, item(MAT_WRITABLE_BOOK, "&e&lOpen Notes",
            Arrays.asList("&7View staff notes for this player.", "&aClick to open.")));
        inv.setItem(29, item(MAT_CLOCK, "&7&lMod Logs",
            Arrays.asList("&7View moderation log for this player.", "&aClick to view.")));
        inv.setItem(30, item(MAT_ENDER_EYE, "&c&lIP Check",
            Arrays.asList("&7View IP address and online alts.", "&aClick to run.")));
        inv.setItem(31, item(MAT_SPYGLASS, "&f&lWhois",
            Arrays.asList("&7Full info output to your chat.", "&aClick to run.")));
        inv.setItem(32, item(online ? Material.ENDER_PEARL : MAT_GRAY_DYE,
            online ? "&b&lTeleport To" : "&8Teleport To",
            Arrays.asList(online ? "&7Teleport to this player's location." : "&7Player is offline.",
                online ? "&aClick to teleport." : "&cUnavailable.")));

        inv.setItem(49, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Return to staff panel.")));
        viewer.openInventory(inv);
    }

    public void openPunishmentHistory(Player viewer, OfflinePlayer target) {
        punHistTargets.put(viewer.getUniqueId(), target.getUniqueId());
        List<Punishment> history = plugin.getPunishmentManager().getHistory(target.getUniqueId());
        int page = punHistPages.getOrDefault(viewer.getUniqueId(), 1);
        int perPage = CONTENT_SLOTS_54.length;
        int pages   = Math.max(1, (int) Math.ceil(history.size() / (double) perPage));
        page = Math.max(1, Math.min(page, pages));
        punHistPages.put(viewer.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54,
                CC.color("&8Punishments: &f" + safeName(target.getName()) + " &8[" + page + "/" + pages + "]"));
        fillBorder(inv, MAT_RED_GLASS_PANE);

        inv.setItem(4, item(Material.BARRIER, "&c&lPunishment History",
            Arrays.asList(
                "&7Player: &f" + safeName(target.getName()),
                "&7Total: &f" + history.size(),
                "&7Page: &f" + page + " &8/ &f" + pages,
                "",
                "&cRed &7= active ban &8| &eYellow &7= active mute &8| &7Gray &7= expired")));

        int from = (page - 1) * perPage;
        for (int i = 0; i < CONTENT_SLOTS_54.length && from + i < history.size(); i++) {
            Punishment pun = history.get(from + i);
            boolean active = pun.isActive();
            Material mat;
            if (!active) mat = MAT_GRAY_DYE;
            else if (pun.getType().isBan()) mat = Material.BARRIER;
            else if (pun.getType().isMute()) mat = MAT_YELLOW_DYE;
            else mat = Material.PAPER;

            List<String> lore = new ArrayList<>();
            lore.add("&7Type: &f" + pun.getType().name());
            lore.add("&7By: &f" + pun.getPunisherName());
            lore.add("&7Reason: &f" + pun.getReason());
            lore.add("&7Issued: &f" + formatDate(pun.getIssued()));
            if (pun.getExpires() > 0) lore.add("&7Duration: &f" + pun.getDurationString());
            lore.add("&7Status: " + (active ? "&cActive" : "&7Expired/Removed"));
            inv.setItem(CONTENT_SLOTS_54[i], item(mat, (active ? "&c" : "&7") + pun.getId(), lore));
        }

        if (page > 1)   inv.setItem(45, item(Material.ARROW, "&ePrevious", Collections.singletonList("&7Page " + (page - 1))));
        if (page < pages) inv.setItem(53, item(Material.ARROW, "&eNext", Collections.singletonList("&7Page " + (page + 1))));
        inv.setItem(49, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Return to player profile.")));
        viewer.openInventory(inv);
    }

    public void openReports(Player player, boolean helpOps) {
        String title = helpOps ? helpOpTitle() : reportsTitle();
        List<StaffRequest> requests = helpOps
                ? plugin.getStaffRequestManager().getHelpOps()
                : plugin.getStaffRequestManager().getReports();
        Inventory inv = Bukkit.createInventory(null, 54, title);
        fillBorder(inv, MAT_RED_GLASS_PANE);

        inv.setItem(4, item(helpOps ? Material.PAPER : Material.BOOK,
            (helpOps ? "&e&lHelpOP" : "&c&lReports") + " &8(" + requests.size() + ")",
            Arrays.asList(
                "&7Open requests: &f" + requests.size(),
                "",
                "&aLeft-click &7to claim a request.",
                "&cRight-click &7to close a request.")));

        int slot = 10;
        for (StaffRequest request : requests) {
            if (slot > 43) break;
            if (slot == 17) slot = 19;
            else if (slot == 26) slot = 28;
            else if (slot == 35) slot = 37;
            if (slot > 43) break;
            List<String> lore = new ArrayList<>();
            lore.add("&7From: &f" + request.getSender());
            if (request.getTarget() != null) lore.add("&7Target: &f" + request.getTarget());
            lore.add("&7" + request.getMessage());
            lore.add("&7Status: &f" + request.getStatus().name());
            lore.add("");
            lore.add("&aLeft-click &7to claim.");
            lore.add("&cRight-click &7to close.");
            inv.setItem(slot++, item(Material.PAPER, "&c#" + request.getId() + " &f" + request.getType().name(), lore));
            if (slot == 17) slot = 19;
            else if (slot == 26) slot = 28;
            else if (slot == 35) slot = 37;
        }

        inv.setItem(49, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Return to staff panel.")));
        player.openInventory(inv);
    }

    public void openPunishMenu(Player player, OfflinePlayer target) {
        punishTargets.put(player.getUniqueId(), target.getUniqueId());
        String title = punishTitle().replace("{player}", safeName(target.getName()));
        Inventory inv = Bukkit.createInventory(null, 54, title);
        fillBorder(inv, MAT_RED_GLASS_PANE);

        boolean online = target.isOnline();
        PlayerProfile profile = online
                ? plugin.getPlayerManager().getProfile(target.getPlayer())
                : plugin.getDatabaseManager().loadProfile(target.getUniqueId(), target.getName());
        int total = plugin.getPunishmentManager().getHistory(target.getUniqueId()).size();

        inv.setItem(4, item(MAT_PLAYER_HEAD, "&c&lPunishing: &f" + safeName(target.getName()),
            Arrays.asList(
                "&7Status: " + (online ? "&aOnline" : "&cOffline"),
                "&7Rank: &f" + (profile != null ? profile.getRankName() : "unknown"),
                "&7Past punishments: &f" + total,
                "",
                "&7Select a punishment type below.")));

        List<Preset> presets = plugin.getPunishmentPresetManager().getPresets();
        int[] presetSlots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25};
        for (int i = 0; i < presets.size() && i < presetSlots.length; i++) {
            Preset preset = presets.get(i);
            List<String> lore = new ArrayList<>(preset.getLore());
            if (lore.isEmpty()) lore.add("&7Click to apply the next ladder step.");
            lore.add("");
            lore.add("&aClick &7to apply.");
            inv.setItem(presetSlots[i], item(material(preset.getMaterial(), "PAPER"), preset.getDisplayName(), lore));
        }

        inv.setItem(49, item(Material.BARRIER, "&c&lCancel", Collections.singletonList("&7Cancel this punishment.")));
        player.openInventory(inv);
    }

    public void openPlayerPicker(Player player, PlayerAction action) {
        pickerActions.put(player.getUniqueId(), action);
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        Inventory inv = Bukkit.createInventory(null, 54, playerPickerTitle(action));
        fillBorder(inv, MAT_GRAY_GLASS_PANE);

        inv.setItem(4, item(MAT_PLAYER_HEAD, "&f&lSelect Player",
            Arrays.asList(
                "&7Action: &e" + action.getLabel(),
                "&7Online players: &f" + online.size(),
                "",
                "&aClick &7a player to select.")));

        int slot = 10;
        for (Player target : online) {
            if (slot > 43) break;
            if (slot == 17) slot = 19;
            else if (slot == 26) slot = 28;
            else if (slot == 35) slot = 37;
            if (slot > 43) break;
            if (target.equals(player) && action == PlayerAction.FREEZE) { slot++; continue; }
            PlayerProfile tp = plugin.getPlayerManager().getProfile(target);
            inv.setItem(slot++, item(MAT_PLAYER_HEAD, "&f" + target.getName(),
                Arrays.asList(
                    "&7Rank: &f" + (tp != null ? tp.getRankName() : "unknown"),
                    "&7Ping: &f" + safeGetPing(target) + "ms",
                    "",
                    "&aClick to " + action.getLabel().toLowerCase() + ".")));
            if (slot == 17) slot = 19;
            else if (slot == 26) slot = 28;
            else if (slot == 35) slot = 37;
        }

        inv.setItem(49, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Return to staff panel.")));
        player.openInventory(inv);
    }

    public void openGrantRankPicker(Player player, OfflinePlayer target) {
        grantTargets.put(player.getUniqueId(), target.getUniqueId());
        grantTargetNames.put(player.getUniqueId(), safeName(target.getName()));
        grantRanks.remove(player.getUniqueId());
        grantDurations.remove(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 54,
                grantTitle().replace("{player}", safeName(target.getName())));
        fillBorder(inv, MAT_YELLOW_GLASS_PANE);

        inv.setItem(4, item(Material.GOLD_INGOT, "&6&lGranting Rank To: &f" + safeName(target.getName()),
            Arrays.asList(
                "&7Templates are shown in the first row.",
                "&7Select a rank below to choose duration.")));

        // Templates (row 2: 10-16)
        Map<Integer, String> templateSlots = new HashMap<>();
        List<GrantTemplate> templates = plugin.getGrantTemplateManager().getTemplates();
        int[] tSlots = {10,11,12,13,14,15,16};
        for (int i = 0; i < templates.size() && i < tSlots.length; i++) {
            GrantTemplate tmpl = templates.get(i);
            templateSlots.put(tSlots[i], tmpl.getId());
            inv.setItem(tSlots[i], item(material(tmpl.getMaterial(), "GOLD_INGOT"), "&6" + tmpl.getDisplayName(),
                Arrays.asList(
                    "&7Template: &f" + tmpl.getId(),
                    "&7Rank: &f" + tmpl.getRankName(),
                    "&7Duration: &f" + tmpl.getDuration(),
                    "&7Reason: &f" + tmpl.getReason(),
                    "", "&aClick &7to apply this template.")));
        }

        // Ranks (rows 3-5: 19-25, 28-34, 37-43)
        int[] rankSlotArr = {19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        Map<Integer, String> slots = new HashMap<>();
        List<Rank> ranks = plugin.getRankManager().getVisibleRanksByWeight();
        for (int i = 0; i < ranks.size() && i < rankSlotArr.length; i++) {
            Rank rank = ranks.get(i);
            slots.put(rankSlotArr[i], rank.getName());
            inv.setItem(rankSlotArr[i], item(Material.NAME_TAG, rank.getDisplayName(),
                Arrays.asList(
                    "&7Weight: &f" + rank.getWeight(),
                    "&7Permissions: &f" + rank.getPermissions().size(),
                    "", "&aClick &7to choose duration.")));
        }

        grantRankSlots.put(player.getUniqueId(), slots);
        grantTemplateSlots.put(player.getUniqueId(), templateSlots);
        inv.setItem(49, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Pick another player.")));
        player.openInventory(inv);
    }

    public void openGrantDurationPicker(Player player) {
        UUID targetUuid = grantTargets.get(player.getUniqueId());
        String rankName = grantRanks.get(player.getUniqueId());
        if (targetUuid == null || rankName == null) { openPlayerPicker(player, PlayerAction.GRANT); return; }

        String targetName = grantTargetNames.getOrDefault(player.getUniqueId(), Bukkit.getOfflinePlayer(targetUuid).getName());
        Inventory inv = Bukkit.createInventory(null, 27,
                grantDurationTitle().replace("{player}", safeName(targetName)));
        fillBorder(inv, MAT_YELLOW_GLASS_PANE);

        inv.setItem(13, item(Material.GOLD_INGOT, "&6&lSelect Duration",
            Arrays.asList("&7Granting: &f" + rankName, "&7To: &f" + safeName(targetName))));

        Map<Integer, String> slots = new HashMap<>();
        List<String> durations = grantDurationOptions();
        int[] dSlots = {10,11,12,14,15,16};
        for (int i = 0; i < durations.size() && i < dSlots.length; i++) {
            String dur = durations.get(i);
            slots.put(dSlots[i], dur);
            inv.setItem(dSlots[i], item(MAT_CLOCK, "&e" + dur,
                Arrays.asList("&7Rank: &f" + rankName, "&7Target: &f" + safeName(targetName),
                    "", "&aClick &7to choose this duration.")));
        }

        grantDurationSlots.put(player.getUniqueId(), slots);
        inv.setItem(19, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Choose another rank.")));
        inv.setItem(22, item(Material.BARRIER, "&c&lCancel", Collections.singletonList("&7Cancel this grant.")));
        player.openInventory(inv);
    }

    public void openGrantReasonPicker(Player player) {
        UUID targetUuid = grantTargets.get(player.getUniqueId());
        String rankName = grantRanks.get(player.getUniqueId());
        String duration = grantDurations.get(player.getUniqueId());
        if (targetUuid == null || rankName == null || duration == null) { openPlayerPicker(player, PlayerAction.GRANT); return; }

        String targetName = grantTargetNames.getOrDefault(player.getUniqueId(), Bukkit.getOfflinePlayer(targetUuid).getName());
        Inventory inv = Bukkit.createInventory(null, 27,
                grantReasonTitle().replace("{player}", safeName(targetName)));
        fillBorder(inv, MAT_YELLOW_GLASS_PANE);

        inv.setItem(13, item(Material.PAPER, "&6&lSelect Reason",
            Arrays.asList("&7Rank: &f" + rankName, "&7Duration: &f" + duration, "&7To: &f" + safeName(targetName))));

        Map<Integer, String> slots = new HashMap<>();
        List<String> reasons = grantReasonOptions();
        int[] rSlots = {10,11,12,14,15,16};
        for (int i = 0; i < reasons.size() && i < rSlots.length; i++) {
            String reason = reasons.get(i);
            slots.put(rSlots[i], reason);
            inv.setItem(rSlots[i], item(Material.PAPER, "&f" + reason,
                Arrays.asList("&7Rank: &f" + rankName, "&7Duration: &f" + duration,
                    "", "&aClick &7to confirm the grant.")));
        }

        grantReasonSlots.put(player.getUniqueId(), slots);
        inv.setItem(19, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Choose another duration.")));
        inv.setItem(22, item(Material.BARRIER, "&c&lCancel", Collections.singletonList("&7Cancel this grant.")));
        player.openInventory(inv);
    }

    public void openTagMenu(Player player, int page) {
        List<TagGuiEntry> entries = loadTagEntries();
        int perPage = CONTENT_SLOTS_54.length;
        int pages   = Math.max(1, (int) Math.ceil(entries.size() / (double) perPage));
        page = Math.max(1, Math.min(page, pages));
        tagPages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54,
                tagTitle().replace("{page}", String.valueOf(page)).replace("{pages}", String.valueOf(pages)));
        fillBorder(inv, MAT_PURPLE_GLASS_PANE);

        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        String current = profile == null || profile.getTag() == null || profile.getTag().isEmpty() ? "&7None" : profile.getTag();
        int unlocked   = unlockedTagCount(player, entries);

        inv.setItem(4, item(Material.NETHER_STAR, "&d&lYour Tags",
            Arrays.asList(
                "&7Current: " + current,
                "&7Unlocked: &f" + unlocked + " &8/ &f" + entries.size(),
                "&7Page: &f" + page + " &8/ &f" + pages)));

        // Navigation slots: 45=prev, 47=random, 49=clear, 51=close, 53=next
        if (page > 1)   inv.setItem(45, item(Material.ARROW, "&ePrevious", Collections.singletonList("&7Page " + (page - 1))));
        inv.setItem(47, item(Material.ENDER_CHEST, "&dRandom Tag", Collections.singletonList("&7Equip a random unlocked tag.")));
        inv.setItem(49, item(Material.BARRIER, "&cClear Tag", Collections.singletonList("&7Remove your current tag.")));
        inv.setItem(51, item(Material.ARROW, "&cClose", Collections.singletonList("&7Close this menu.")));
        if (page < pages) inv.setItem(53, item(Material.ARROW, "&eNext", Collections.singletonList("&7Page " + (page + 1))));

        int from = (page - 1) * perPage;
        Map<Integer, TagGuiEntry> pageSlots = new HashMap<>();
        for (int i = 0; i < CONTENT_SLOTS_54.length && from + i < entries.size(); i++) {
            TagGuiEntry tag   = entries.get(from + i);
            boolean canUse    = canUseTag(player, tag);
            List<String> lore = new ArrayList<>();
            lore.add("&7ID: &f" + tag.id);
            lore.add("&7Category: &f" + tag.category);
            lore.add("&7Rarity: &f" + tag.rarity);
            if (!tag.ranks.isEmpty()) lore.add("&7Rank unlocks: &f" + joinText(tag.ranks));
            if (!tag.description.isEmpty()) { lore.add(""); lore.add("&7" + tag.description); }
            lore.add("");
            lore.add(canUse ? "&aLeft-click &7to equip." : "&cLocked: &f" + requiredTagPermission(tag));
            lore.add("&eRight-click &7to preview.");
            pageSlots.put(CONTENT_SLOTS_54[i], tag);
            inv.setItem(CONTENT_SLOTS_54[i], item(canUse ? Material.NAME_TAG : MAT_INK_SAC,
                tag.display + (canUse ? "" : " &8(Locked)"), lore));
        }
        tagSlots.put(player.getUniqueId(), pageSlots);
        player.openInventory(inv);
    }

    public void openNotes(Player viewer, OfflinePlayer target) {
        List<PlayerNote> notes = plugin.getNoteManager().getNotes(target.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54,
                CC.color(plugin.getConfig().getString("gui.notes.title", "&8Notes: {player}")
                        .replace("{player}", safeName(target.getName()))));
        fillBorder(inv, MAT_GREEN_GLASS_PANE);

        inv.setItem(4, item(MAT_WRITABLE_BOOK, "&e&lNotes: &f" + safeName(target.getName()),
            Arrays.asList(
                "&7Notes recorded: &f" + notes.size(),
                "",
                "&cRight-click &7a note to delete it.",
                "&7Add notes with &f/note " + safeName(target.getName()) + " <text>")));

        int slot = 10;
        for (PlayerNote note : notes) {
            if (slot > 43) break;
            if (slot == 17) slot = 19;
            else if (slot == 26) slot = 28;
            else if (slot == 35) slot = 37;
            if (slot > 43) break;
            inv.setItem(slot++, item(Material.PAPER, "&e" + note.getId(),
                Arrays.asList(
                    "&7By: &f" + note.getIssuerName(),
                    "&7At: &f" + note.getFormattedTime(),
                    "&7" + note.getNote(),
                    "",
                    "&cRight-click &7to delete.")));
            if (slot == 17) slot = 19;
            else if (slot == 26) slot = 28;
            else if (slot == 35) slot = 37;
        }

        inv.setItem(49, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Return to staff panel.")));
        viewer.openInventory(inv);
    }

    public void openRanks(Player viewer) {
        openRanks(viewer, rankPages.containsKey(viewer.getUniqueId()) ? rankPages.get(viewer.getUniqueId()) : 1);
    }

    private void openRanks(Player viewer, int requestedPage) {
        List<Rank> ranks = plugin.getRankManager().getVisibleRanksByWeight();
        int pageSize = CONTENT_SLOTS_54.length;
        int maxPage  = Math.max(1, (int) Math.ceil(ranks.size() / (double) pageSize));
        int page     = Math.min(maxPage, Math.max(1, requestedPage));
        rankPages.put(viewer.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54,
                CC.color(plugin.getConfig().getString("gui.ranks.title", "&8Ranks")));
        fillBorder(inv, MAT_LIME_GLASS_PANE);

        inv.setItem(4, item(Material.NAME_TAG, "&a&lRanks",
            Arrays.asList(
                "&7Visible ranks: &f" + ranks.size(),
                "&7Page: &f" + page + " &8/ &f" + maxPage,
                "",
                "&aClick &7a rank to edit it.")));

        int start = (page - 1) * pageSize;
        Map<Integer, String> slots = new HashMap<>();
        for (int i = 0; i < CONTENT_SLOTS_54.length && start + i < ranks.size(); i++) {
            Rank rank = ranks.get(start + i);
            List<String> lore = new ArrayList<>();
            lore.add("&7Category: &f" + plugin.getRankManager().getRankCategory(rank));
            lore.add("&7Permission: &f" + (rank.getPermission().isEmpty() ? "none" : rank.getPermission()));
            lore.add("&7Weight: &f" + rank.getWeight());
            lore.add("&7Default: &f" + rank.isDefault() + " &8| Staff: &f" + rank.isStaff() + " &8| Hidden: &f" + rank.isHidden());
            lore.add("&7Direct perms: &f" + rank.getPermissions().size() + " &8| All: &f" + plugin.getRankManager().getAllPermissions(rank).size());
            lore.add("&7Inherits: &f" + (rank.getInheritance().isEmpty() ? "none" : joinText(rank.getInheritance())));
            lore.add(""); lore.add("&aClick &7to edit.");
            slots.put(CONTENT_SLOTS_54[i], rank.getName());
            inv.setItem(CONTENT_SLOTS_54[i], item(Material.NAME_TAG, rank.getDisplayName(), lore));
        }
        rankSlots.put(viewer.getUniqueId(), slots);

        if (page > 1)     inv.setItem(45, item(Material.ARROW, "&ePrevious", Collections.singletonList("&7Page " + (page - 1) + " / " + maxPage)));
        if (page < maxPage) inv.setItem(53, item(Material.ARROW, "&eNext", Collections.singletonList("&7Page " + (page + 1) + " / " + maxPage)));
        inv.setItem(49, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Return to staff panel.")));
        viewer.openInventory(inv);
    }

    public void openRankEditor(Player viewer, Rank rank) {
        editingRanks.put(viewer.getUniqueId(), rank.getName());
        Inventory inv = Bukkit.createInventory(null, 27, rankEditorTitle().replace("{rank}", rank.getName()));
        fillBorder(inv, MAT_LIME_GLASS_PANE);

        inv.setItem(13, item(Material.NAME_TAG, rank.getDisplayName(),
            Arrays.asList(
                "&7Display: &f" + (rank.getDisplay().isEmpty() ? "none" : rank.getDisplay()),
                "&7Category: &f" + plugin.getRankManager().getRankCategory(rank),
                "&7Permission: &f" + (rank.getPermission().isEmpty() ? "none" : rank.getPermission()),
                "&7Weight: &f" + rank.getWeight() + " &8| Color: &f" + rank.getColor(),
                "&7Staff: &f" + rank.isStaff() + " &8| Hidden: &f" + rank.isHidden() + " &8| Default: &f" + rank.isDefault(),
                "&7Permissions: &f" + rank.getPermissions().size())));

        inv.setItem(10, item(rank.isDefault() ? Material.NETHER_STAR : MAT_GRAY_DYE,
            rank.isDefault() ? "&a&lDefault &8(ON)" : "&7Default &8(OFF)",
            Collections.singletonList("&7Click to set this as the default rank.")));
        inv.setItem(11, item(rank.isStaff() ? Material.IRON_SWORD : MAT_GRAY_DYE,
            rank.isStaff() ? "&a&lStaff &8(ON)" : "&7Staff &8(OFF)",
            Collections.singletonList("&7Click to toggle staff status.")));
        inv.setItem(12, item(rank.isHidden() ? MAT_INK_SAC : MAT_GRAY_DYE,
            rank.isHidden() ? "&7Hidden &8(ON)" : "&7Hidden &8(OFF)",
            Collections.singletonList("&7Click to toggle visibility.")));
        inv.setItem(14, item(Material.PAPER, "&7&lCategory",
            Arrays.asList("&7Current: &f" + plugin.getRankManager().getRankCategory(rank),
                "&aClick &7to open category picker.")));
        inv.setItem(15, item(Material.BOOK, "&e&lBackup Ranks",
            Collections.singletonList("&7Save a backup of all current ranks.")));
        inv.setItem(16, item(Material.TNT, "&c&lReset to Presets",
            Collections.singletonList("&7Open the preset reset confirmation.")));

        inv.setItem(22, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Return to ranks list.")));
        viewer.openInventory(inv);
    }

    public void openStreamerMode(Player player) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        boolean enabled = profile != null && profile.isStreamerMode();
        String alias    = profile != null && profile.getStreamerAlias() != null ? profile.getStreamerAlias() : "none";

        Inventory inv = Bukkit.createInventory(null, 27, streamerModeTitle());
        fillBorder(inv, MAT_MAGENTA_GLASS_PANE);

        inv.setItem(13, item(MAT_MAGENTA_GLASS, "&d&lStreamer Mode",
            Arrays.asList(
                "&7Status: " + (enabled ? "&aON &8(alias: &f" + alias + "&8)" : "&cOFF"),
                "",
                "&7Hides your real name in chat and tab.",
                "&7Staff can always see your real name.")));

        inv.setItem(10, item(enabled ? MAT_GRAY_DYE : MAT_LIME_DYE,
            enabled ? "&8Enable &8(already on)" : "&a&lEnable Streamer Mode",
            Arrays.asList("&7Assigns you a random alias.",
                enabled ? "&7Already active." : "&aClick to enable.")));
        inv.setItem(11, item(enabled ? MAT_RED_DYE : MAT_GRAY_DYE,
            enabled ? "&c&lDisable Streamer Mode" : "&8Disable &8(already off)",
            Arrays.asList("&7Reveals your real name again.",
                enabled ? "&cClick to disable." : "&7Already inactive.")));
        inv.setItem(14, item(enabled ? Material.ENDER_PEARL : MAT_GRAY_DYE,
            enabled ? "&e&lNew Random Alias" : "&8New Random Alias",
            Arrays.asList("&7Replaces your alias with a new random one.",
                enabled ? "&eClick to randomize." : "&7Enable streamer mode first.")));
        inv.setItem(15, item(enabled ? Material.NAME_TAG : MAT_GRAY_DYE,
            enabled ? "&b&lSet Custom Alias" : "&8Set Custom Alias",
            Arrays.asList("&7Set your own alias (3-16 chars).",
                enabled ? "&7Close and type: &f/streamermode alias <name>" : "&7Enable streamer mode first.")));

        inv.setItem(22, item(Material.BARRIER, "&c&lClose", Collections.singletonList("&7Close this menu.")));
        player.openInventory(inv);
    }

    // ---------------------------------------------------------------------
    //  Admin & Owner control panels
    // ---------------------------------------------------------------------

    public void openAdminPanel(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, adminPanelTitle());
        fillBorder(inv, MAT_RED_GLASS_PANE);

        Runtime rt = Runtime.getRuntime();
        long mb = 1024L * 1024L;
        long usedMem = (rt.totalMemory() - rt.freeMemory()) / mb;
        long maxMem = rt.maxMemory() / mb;

        inv.setItem(4, item(Material.NETHER_STAR, "&c&lAdmin Control Panel",
            Arrays.asList(
                "&7Online: &f" + Bukkit.getOnlinePlayers().size() + " &8/ &f" + Bukkit.getMaxPlayers(),
                "&7Staff online: &f" + onlineStaffCount(),
                "&7Memory: &f" + usedMem + " &8/ &f" + maxMem + " MB",
                "&7Maintenance: " + enabledText(plugin.getConfig().getBoolean("maintenance.enabled", false)))));

        // Row 2 — rank & player administration (10-16)
        inv.setItem(10, item(Material.NAME_TAG, "&6&lRank Manager",
            Arrays.asList("&7Create, edit and inspect ranks.", "&aClick to open.")));
        inv.setItem(11, badge(Material.GOLD_INGOT, "&6&lPending Grants",
            plugin.getGrantManager().getPendingGrants().size(), "&7Review pending rank grants."));
        inv.setItem(12, item(Material.EMERALD, "&a&lGrant Rank",
            Arrays.asList("&7Grant a rank to any player.", "&aClick to open.")));
        inv.setItem(13, item(MAT_PLAYER_HEAD, "&f&lPlayer Profile",
            Arrays.asList("&7Inspect a player's full profile.", "&aClick to open.")));
        inv.setItem(14, item(MAT_REDSTONE_TORCH,
            (plugin.getConfig().getBoolean("maintenance.enabled", false) ? "&a" : "&c") + "&lMaintenance",
            Arrays.asList("&7Manage maintenance mode.", "&aClick to open.")));
        inv.setItem(15, item(Material.IRON_DOOR, "&e&lLobby Protection",
            Arrays.asList("&7Configure lobby protection rules.", "&aClick to open.")));
        inv.setItem(16, badge(Material.BOOK, "&c&lReports",
            plugin.getStaffRequestManager().getReports().size(), "&7Open the report queue."));

        // Row 3 — server diagnostics & actions (19-25)
        inv.setItem(19, item(MAT_CLOCK, "&b&lServer Info",
            Arrays.asList("&7View detailed server information.", "&aClick to run /serverinfo.")));
        inv.setItem(20, item(Material.REDSTONE_BLOCK, "&d&lTPS",
            Arrays.asList("&7View current server TPS.", "&aClick to run /tps.")));
        inv.setItem(21, item(MAT_SPAWN_EGG, "&e&lEntity Report",
            Arrays.asList("&7Per-world entity counts.", "&aClick to run /entitycount.")));
        inv.setItem(22, item(MAT_FIRE_CHARGE, "&6&lClear Lag",
            Arrays.asList("&7Remove ground items & stray entities.", "&aClick to run /clearlag.")));
        inv.setItem(23, item(Material.IRON_FENCE, "&c&lLockdown",
            Arrays.asList("&7Toggle server lockdown.", "&aClick to run /lockdown.")));
        inv.setItem(24, item(Material.PAPER, "&f&lBroadcast",
            Arrays.asList("&7Send a server-wide broadcast.", "&aClick for the command.")));
        inv.setItem(25, item(Material.EMERALD_BLOCK, "&a&lStaff Dashboard",
            Arrays.asList("&7Open the staff overview dashboard.", "&aClick to open.")));

        if (player.hasPermission("evaulx.owner.panel")) {
            inv.setItem(40, item(Material.BEACON, "&4&lOwner Panel",
                Arrays.asList("&7Open the owner control panel.", "&aClick to open.")));
        }

        inv.setItem(49, item(Material.BARRIER, "&c&lClose", Collections.singletonList("&7Close this menu.")));
        player.openInventory(inv);
    }

    public void openOwnerPanel(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ownerPanelTitle());
        fillBorder(inv, MAT_BLACK_GLASS_PANE);

        inv.setItem(4, item(Material.BEACON, "&4&lOwner Control Panel",
            Arrays.asList(
                "&7Online: &f" + Bukkit.getOnlinePlayers().size() + " &8/ &f" + Bukkit.getMaxPlayers(),
                "&7Staff online: &f" + onlineStaffCount(),
                "&7Server: &f" + Bukkit.getServerName(),
                "&7Version: &f" + Bukkit.getVersion())));

        // Row 2 — broadcasts & alerts (10-16)
        inv.setItem(10, item(MAT_RED_DYE, "&4&lOwner Alert",
            Arrays.asList("&7Send a full-screen owner alert.", "&aClick for the command.")));
        inv.setItem(11, item(Material.PAPER, "&c&lOwner Broadcast",
            Arrays.asList("&7Send a styled owner broadcast.", "&aClick for the command.")));
        inv.setItem(12, item(Material.BOOK, "&6&lForce Chat",
            Arrays.asList("&7Force a message into chat as a player.", "&aClick for the command.")));
        inv.setItem(13, item(Material.PACKED_ICE, "&b&lServer Freeze",
            Arrays.asList("&7Freeze or unfreeze the whole server.", "&aClick to run /serverfreeze.")));
        inv.setItem(14, item(Material.IRON_FENCE, "&c&lLockdown",
            Arrays.asList("&7Toggle server lockdown.", "&aClick to run /lockdown.")));
        inv.setItem(15, item(MAT_REDSTONE_TORCH,
            (plugin.getConfig().getBoolean("maintenance.enabled", false) ? "&a" : "&c") + "&lMaintenance",
            Arrays.asList("&7Manage maintenance mode.", "&aClick to open.")));
        inv.setItem(16, item(Material.TNT, "&4&lScheduled Shutdown",
            Arrays.asList("&7Schedule a graceful shutdown.", "&aClick for the command.")));

        // Row 3 — administration (19-25)
        inv.setItem(19, item(Material.NAME_TAG, "&6&lRank Manager",
            Arrays.asList("&7Create, edit and inspect ranks.", "&aClick to open.")));
        inv.setItem(20, badge(Material.GOLD_INGOT, "&6&lPending Grants",
            plugin.getGrantManager().getPendingGrants().size(), "&7Review pending rank grants."));
        inv.setItem(21, item(Material.EMERALD, "&a&lEconomy Admin",
            Arrays.asList("&7Manage player coin balances.", "&aClick for the command.")));
        inv.setItem(22, item(Material.EMERALD_BLOCK, "&a&lStaff Dashboard",
            Arrays.asList("&7Open the staff overview dashboard.", "&aClick to open.")));
        inv.setItem(23, item(Material.COMMAND, "&5&lReload Plugin",
            Arrays.asList("&7Reload EvaulxMC configuration.", "&cUse with care.", "&aClick to run /reloadplugin.")));
        inv.setItem(24, item(MAT_RED_CONCRETE, "&4&lKick All",
            Arrays.asList("&7Kick every non-exempt player.", "&cUse with care.", "&aClick for the command.")));
        inv.setItem(25, item(MAT_CLOCK, "&b&lServer Info",
            Arrays.asList("&7View detailed server information.", "&aClick to run /serverinfo.")));

        inv.setItem(49, item(Material.BARRIER, "&c&lClose", Collections.singletonList("&7Close this menu.")));
        player.openInventory(inv);
    }

    /** Closes the menu and tells the player which command to type for actions that need arguments. */
    private void promptCommand(Player player, String usage) {
        player.closeInventory();
        player.sendMessage(CC.color("&8[&cEvaulxMC&8] &7Type: &f" + usage));
    }

    // =====================================================================
    //  CLICK DISPATCHER
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title  = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();

        if (title.equals(staffTitle())) {
            event.setCancelled(true); handleStaffPanelClick(player, event.getSlot()); return;
        }
        if (title.startsWith(playerPickerPrefix())) {
            event.setCancelled(true); handlePlayerPickerClick(player, event); return;
        }
        if (title.startsWith(grantTitlePrefix())) {
            event.setCancelled(true); handleGrantRankClick(player, event); return;
        }
        if (title.startsWith(grantDurationTitlePrefix())) {
            event.setCancelled(true); handleGrantDurationClick(player, event); return;
        }
        if (title.startsWith(grantReasonTitlePrefix())) {
            event.setCancelled(true); handleGrantReasonClick(player, event); return;
        }
        if (title.startsWith(tagTitlePrefix())) {
            event.setCancelled(true); handleTagClick(player, event); return;
        }
        if (title.equals(lobbyProtectionTitle())) {
            event.setCancelled(true); handleLobbyProtectionClick(player, event.getSlot()); return;
        }
        if (title.equals(rankPresetConfirmTitle())) {
            event.setCancelled(true); handleRankPresetConfirmClick(player, event.getSlot()); return;
        }
        if (title.equals(staffDashboardTitle())) {
            event.setCancelled(true); handleStaffDashboardClick(player, event.getSlot()); return;
        }
        if (title.equals(adminPanelTitle())) {
            event.setCancelled(true); handleAdminPanelClick(player, event.getSlot()); return;
        }
        if (title.equals(ownerPanelTitle())) {
            event.setCancelled(true); handleOwnerPanelClick(player, event.getSlot()); return;
        }
        if (title.equals(maintenanceTitle())) {
            event.setCancelled(true); handleMaintenanceClick(player, event.getSlot()); return;
        }
        if (title.equals(staffSessionsTitle())) {
            event.setCancelled(true); handleStaffSessionsClick(player, event.getSlot()); return;
        }
        if (title.equals(pendingGrantsTitle())) {
            event.setCancelled(true); handlePendingGrantClick(player, event); return;
        }
        if (title.startsWith(playerProfileTitlePrefix())) {
            event.setCancelled(true); handleProfileClick(player, event); return;
        }
        if (title.equals(reportsTitle()) || title.equals(helpOpTitle())) {
            event.setCancelled(true); handleReportsClick(player, event); return;
        }
        if (title.equals(streamerModeTitle())) {
            event.setCancelled(true); handleStreamerModeClick(player, event.getSlot()); return;
        }
        if (title.startsWith(punishmentHistoryPrefix())) {
            event.setCancelled(true); handlePunishmentHistoryClick(player, event); return;
        }
        if (title.startsWith(CC.color(plugin.getConfig().getString("gui.punish.title-prefix", "&8Punish: ")))) {
            event.setCancelled(true); handlePunishClick(player, event.getSlot()); return;
        }
        if (title.startsWith(CC.color(plugin.getConfig().getString("gui.notes.title-prefix", "&8Notes: ")))) {
            event.setCancelled(true); handleNotesClick(player, event); return;
        }
        if (title.equals(CC.color(plugin.getConfig().getString("gui.ranks.title", "&8Ranks")))) {
            event.setCancelled(true); handleRanksClick(player, event.getSlot()); return;
        }
        if (title.startsWith(rankCategoryTitlePrefix())) {
            event.setCancelled(true); handleRankCategoryClick(player, event.getSlot()); return;
        }
        if (title.startsWith(rankEditorTitlePrefix())) {
            event.setCancelled(true); handleRankEditorClick(player, event.getSlot()); return;
        }
        if (title.equals(appealsTitle())) {
            event.setCancelled(true); handleAppealsClick(player, event); return;
        }
        if (title.startsWith(appealDetailPrefix())) {
            event.setCancelled(true); handleAppealDetailClick(player, event.getSlot());
        }
    }

    // =====================================================================
    //  HANDLE METHODS
    // =====================================================================

    private void handleStaffPanelClick(Player player, int slot) {
        switch (slot) {
            case 10: openReports(player, false); break;
            case 11: openReports(player, true); break;
            case 12: openPlayerPicker(player, PlayerAction.PUNISH); break;
            case 13: openPlayerPicker(player, PlayerAction.FREEZE); break;
            case 14: openPlayerPicker(player, PlayerAction.INSPECT); break;
            case 15: openPlayerPicker(player, PlayerAction.NOTES); break;
            case 16: openPlayerPicker(player, PlayerAction.MODLOGS); break;
            case 19: openPlayerPicker(player, PlayerAction.GRANT); break;
            case 20: player.closeInventory(); Bukkit.dispatchCommand(player, "vanish"); break;
            case 21: player.closeInventory(); Bukkit.dispatchCommand(player, "staffmode"); break;
            case 22: openPlayerPicker(player, PlayerAction.PROFILE); break;
            case 23: openLobbyProtection(player); break;
            case 24: openStaffDashboard(player); break;
            case 25: openMaintenance(player); break;
            case 28: openStaffSessions(player); break;
            case 29: openStreamerMode(player); break;
            case 30: openRanks(player); break;
            case 31: openPlayerPicker(player, PlayerAction.WHOIS); break;
            case 32: openPlayerPicker(player, PlayerAction.PING); break;
            case 33: openPlayerPicker(player, PlayerAction.IPCHECK); break;
            case 34: openPendingGrants(player); break;
            case 37: openAppealsGui(player); break;
            case 49: player.closeInventory(); break;
            default: break;
        }
    }

    private void handleAdminPanelClick(Player player, int slot) {
        switch (slot) {
            case 10: openRanks(player); break;
            case 11: openPendingGrants(player); break;
            case 12: openPlayerPicker(player, PlayerAction.GRANT); break;
            case 13: openPlayerPicker(player, PlayerAction.PROFILE); break;
            case 14: openMaintenance(player); break;
            case 15: openLobbyProtection(player); break;
            case 16: openReports(player, false); break;
            case 19: player.closeInventory(); Bukkit.dispatchCommand(player, "serverinfo"); break;
            case 20: player.closeInventory(); Bukkit.dispatchCommand(player, "tps"); break;
            case 21: player.closeInventory(); Bukkit.dispatchCommand(player, "entitycount"); break;
            case 22: player.closeInventory(); Bukkit.dispatchCommand(player, "clearlag"); break;
            case 23: player.closeInventory(); Bukkit.dispatchCommand(player, "lockdown"); break;
            case 24: promptCommand(player, "/broadcast <message>"); break;
            case 25: openStaffDashboard(player); break;
            case 40: if (player.hasPermission("evaulx.owner.panel")) openOwnerPanel(player); break;
            case 49: player.closeInventory(); break;
            default: break;
        }
    }

    private void handleOwnerPanelClick(Player player, int slot) {
        switch (slot) {
            case 10: promptCommand(player, "/owneralert <message>"); break;
            case 11: promptCommand(player, "/ownerbc <message>"); break;
            case 12: promptCommand(player, "/forcechat <player> <message>"); break;
            case 13: player.closeInventory(); Bukkit.dispatchCommand(player, "serverfreeze"); break;
            case 14: player.closeInventory(); Bukkit.dispatchCommand(player, "lockdown"); break;
            case 15: openMaintenance(player); break;
            case 16: promptCommand(player, "/shutdown <seconds> [reason]"); break;
            case 19: openRanks(player); break;
            case 20: openPendingGrants(player); break;
            case 21: promptCommand(player, "/coins <add|remove|set> <player> <amount>"); break;
            case 22: openStaffDashboard(player); break;
            case 23: player.closeInventory(); Bukkit.dispatchCommand(player, "reloadplugin"); break;
            case 24: promptCommand(player, "/kickall [reason]"); break;
            case 25: player.closeInventory(); Bukkit.dispatchCommand(player, "serverinfo"); break;
            case 49: player.closeInventory(); break;
            default: break;
        }
    }

    private void handleAppealsClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == 45) { appealListPages.merge(player.getUniqueId(), -1, Integer::sum); openAppealsGui(player); return; }
        if (slot == 53) { appealListPages.merge(player.getUniqueId(), 1, Integer::sum); openAppealsGui(player); return; }
        if (slot == 49) { player.closeInventory(); return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;

        List<Appeal> pending = plugin.getAppealManager().getPending();
        int page = appealListPages.getOrDefault(player.getUniqueId(), 0);
        int start = page * CONTENT_SLOTS_54.length;

        for (int i = 0; i < CONTENT_SLOTS_54.length; i++) {
            if (CONTENT_SLOTS_54[i] == slot) {
                int idx = start + i;
                if (idx < pending.size()) openAppealDetailGui(player, pending.get(idx));
                return;
            }
        }
    }

    private void handleAppealDetailClick(Player player, int slot) {
        if (slot == 22) { appealDetailIds.remove(player.getUniqueId()); openAppealsGui(player); return; }

        String id = appealDetailIds.get(player.getUniqueId());
        if (id == null) { player.closeInventory(); return; }

        Appeal appeal = plugin.getAppealManager().get(id);
        if (appeal == null || !"pending".equals(appeal.status)) {
            player.sendMessage(CC.color("&cThis appeal is no longer pending.")); openAppealsGui(player); return;
        }

        if (slot == 11) {
            if (!player.hasPermission("evaulx.appeals.manage")) { player.sendMessage(CC.color("&cNo permission.")); return; }
            plugin.getAppealManager().resolve(id, player.getName(), true, "Accepted");
            player.sendMessage(CC.color("&aAccepted appeal &f" + id + " &afor &f" + appeal.playerName + "&a."));
            Player target = Bukkit.getPlayer(appeal.playerName);
            if (target != null) target.sendMessage(CC.color("&a&lAppeal Accepted &7— Punishment &f(" + id + ") &7has been pardoned."));
            UUID staffUUID = player.getUniqueId(); String staffName = player.getName(); String pName = appeal.playerName;
            TaskUtil.async(() -> {
                OfflinePlayer op = plugin.getPlayerLookupManager().find(pName);
                if (op == null) return;
                List<Punishment> hist = plugin.getPunishmentManager().getHistory(op.getUniqueId());
                for (Punishment p : hist) {
                    if (!p.getId().equals(id)) continue;
                    p.setActive(false); p.setAppealStatus("accepted");
                    p.setRemovedBy(staffUUID); p.setRemovedReason("Appeal accepted by " + staffName);
                    p.setRemovedAt(System.currentTimeMillis()); plugin.getDatabaseManager().updatePunishment(p); break;
                }
            });
            plugin.getStaffRequestManager().broadcastStaff("&8[&6Appeal&8] &f" + staffName + " &aaccepted &7appeal &f" + id + " &7for &f" + pName, "evaulx.appeals.see");
            plugin.getStaffRequestManager().logAction(staffName, "APPEAL_ACCEPTED", id, "Accepted for " + pName);
            appealDetailIds.remove(player.getUniqueId());
            openAppealsGui(player);
        }

        if (slot == 15) {
            if (!player.hasPermission("evaulx.appeals.manage")) { player.sendMessage(CC.color("&cNo permission.")); return; }
            plugin.getAppealManager().resolve(id, player.getName(), false, "Staff reviewed your appeal");
            player.sendMessage(CC.color("&cDenied appeal &f" + id + " &cfor &f" + appeal.playerName + "&c."));
            Player target = Bukkit.getPlayer(appeal.playerName);
            if (target != null) target.sendMessage(CC.color("&c&lAppeal Denied &7— Punishment &f(" + id + ") &7remains active."));
            plugin.getStaffRequestManager().broadcastStaff("&8[&6Appeal&8] &f" + player.getName() + " &cdenied &7appeal &f" + id + " &7for &f" + appeal.playerName, "evaulx.appeals.see");
            plugin.getStaffRequestManager().logAction(player.getName(), "APPEAL_DENIED", id, "Denied for " + appeal.playerName);
            appealDetailIds.remove(player.getUniqueId());
            openAppealsGui(player);
        }
    }

    private void handleStreamerModeClick(Player player, int slot) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        boolean enabled = profile != null && profile.isStreamerMode();
        switch (slot) {
            case 10:
                if (!enabled) { player.closeInventory(); Bukkit.dispatchCommand(player, "streamermode on"); }
                else openStreamerMode(player);
                break;
            case 11:
                if (enabled) { player.closeInventory(); Bukkit.dispatchCommand(player, "streamermode off"); }
                else openStreamerMode(player);
                break;
            case 14:
                player.closeInventory();
                Bukkit.dispatchCommand(player, "streamermode refresh");
                break;
            case 15:
                player.closeInventory();
                player.sendMessage(CC.color("&8[&dStreamer&8] &7Type: &f/streamermode alias <name>"));
                break;
            case 22:
                player.closeInventory();
                break;
            default: break;
        }
    }

    private void handleLobbyProtectionClick(Player player, int slot) {
        String world = player.getWorld().getName();
        switch (slot) {
            case 10: toggleConfig("lobby-protection.enabled", true); player.sendMessage(CC.color("&7Lobby protection: " + enabledText(plugin.getConfig().getBoolean("lobby-protection.enabled", true)))); openLobbyProtection(player); break;
            case 11: enforceLobbyProtection(world); player.sendMessage(CC.color("&aLobby protection enforced for &f" + world + "&a.")); openLobbyProtection(player); break;
            case 12: addProtectedWorld(world); plugin.saveConfig(); player.sendMessage(CC.color("&aProtected lobby world: &f" + world)); openLobbyProtection(player); break;
            case 13: removeProtectedWorld(world); plugin.saveConfig(); player.sendMessage(CC.color("&cUnprotected lobby world: &f" + world)); openLobbyProtection(player); break;
            case 14: plugin.getConfig().set("lobby-protection.worlds", new ArrayList<String>()); plugin.saveConfig(); player.sendMessage(CC.color("&aLobby protection now applies to all loaded worlds.")); openLobbyProtection(player); break;
            case 15: toggleConfig("lobby-protection.notify-on-deny", true); openLobbyProtection(player); break;
            case 16: player.closeInventory(); Bukkit.dispatchCommand(player, "buildmode"); break;
            case 19: toggleConfig("lobby-protection.prevent-damage", true); openLobbyProtection(player); break;
            case 20: toggleConfig("lobby-protection.prevent-explosions", true); openLobbyProtection(player); break;
            case 21: toggleConfig("lobby-protection.no-mobs", true); openLobbyProtection(player); break;
            case 22:
                toggleConfig("lobby-protection.prevent-item-drops", true);
                plugin.getConfig().set("lobby-protection.prevent-item-pickup", plugin.getConfig().getBoolean("lobby-protection.prevent-item-drops", true));
                plugin.saveConfig(); openLobbyProtection(player); break;
            case 23:
                toggleConfig("lobby-protection.prevent-block-interact", true);
                plugin.getConfig().set("lobby-protection.prevent-entity-interact", plugin.getConfig().getBoolean("lobby-protection.prevent-block-interact", true));
                plugin.getConfig().set("lobby-protection.prevent-dangerous-item-use", plugin.getConfig().getBoolean("lobby-protection.prevent-block-interact", true));
                plugin.saveConfig(); openLobbyProtection(player); break;
            case 24:
                toggleConfig("lobby-protection.prevent-liquid-flow", true);
                plugin.getConfig().set("lobby-protection.prevent-growth", plugin.getConfig().getBoolean("lobby-protection.prevent-liquid-flow", true));
                plugin.getConfig().set("lobby-protection.prevent-spread", plugin.getConfig().getBoolean("lobby-protection.prevent-liquid-flow", true));
                plugin.saveConfig(); openLobbyProtection(player); break;
            case 25: toggleConfig("lobby-protection.void-rescue.enabled", true); openLobbyProtection(player); break;
            case 28: toggleConfig("lobby-protection.prevent-hunger", true); openLobbyProtection(player); break;
            case 29: toggleConfig("lobby-protection.prevent-weather", true); openLobbyProtection(player); break;
            case 30: toggleConfig("lobby-protection.prevent-projectiles", true); openLobbyProtection(player); break;
            case 31: toggleConfig("lobby-protection.prevent-fire", true); openLobbyProtection(player); break;
            case 32: player.closeInventory(); Bukkit.dispatchCommand(player, "lobbyprotect status"); break;
            case 49: player.closeInventory(); break;
            default: break;
        }
    }

    private void handleRankPresetConfirmClick(Player player, int slot) {
        if (slot == 11) { player.closeInventory(); Bukkit.dispatchCommand(player, "rank presets confirm"); }
        else if (slot == 15) openRanks(player);
    }

    private void handleStaffDashboardClick(Player player, int slot) {
        switch (slot) {
            case 10: openReports(player, false); break;
            case 11: openReports(player, true); break;
            case 12: openPendingGrants(player); break;
            case 14: openStaffSessions(player); break;
            case 15: openMaintenance(player); break;
            case 49: openStaffPanel(player); break;
            default: break;
        }
    }

    private void handleMaintenanceClick(Player player, int slot) {
        switch (slot) {
            case 10:
                player.closeInventory();
                Bukkit.dispatchCommand(player, plugin.getConfig().getBoolean("maintenance.enabled", false) ? "maintenance off" : "maintenance on GUI toggle");
                break;
            case 11: player.closeInventory(); Bukkit.dispatchCommand(player, "maintenance status"); break;
            case 14: openStaffSessions(player); break;
            case 22: openStaffDashboard(player); break;
            default: break;
        }
    }

    private void handleStaffSessionsClick(Player player, int slot) {
        if (slot == 49) openStaffDashboard(player);
    }

    private void handlePendingGrantClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 49) { openStaffDashboard(player); return; }
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String id = CC.strip(item.getItemMeta().getDisplayName()).trim();
        if (id.isEmpty()) return;
        player.closeInventory();
        if (event.isRightClick()) Bukkit.dispatchCommand(player, "grant deny " + id + " Denied from GUI");
        else Bukkit.dispatchCommand(player, "grant approve " + id);
    }

    private void handleProfileClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        UUID uuid = profileTargets.get(player.getUniqueId());
        if (uuid == null) { player.closeInventory(); return; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
        boolean online = target.isOnline();

        switch (slot) {
            case 10: openGrantRankPicker(player, target); break;
            case 11: openPunishmentHistory(player, target); break;
            case 12: openNotes(player, target); break;
            case 16:
                player.closeInventory();
                Bukkit.dispatchCommand(player, "ipcheck " + safeName(target.getName()));
                break;
            case 19: openGrantRankPicker(player, target); break;
            case 20: openPunishMenu(player, target); break;
            case 21:
                if (online) { player.closeInventory(); Bukkit.dispatchCommand(player, "kick " + target.getName() + " Kicked by staff"); }
                break;
            case 22:
                player.closeInventory();
                List<Punishment> h = plugin.getPunishmentManager().getHistory(uuid);
                if (countActive(h, true) > 0) Bukkit.dispatchCommand(player, "unban " + safeName(target.getName()));
                else Bukkit.dispatchCommand(player, "ban " + safeName(target.getName()) + " Banned by staff");
                break;
            case 23:
                player.closeInventory();
                List<Punishment> hm = plugin.getPunishmentManager().getHistory(uuid);
                if (countActive(hm, false) > 0) Bukkit.dispatchCommand(player, "unmute " + safeName(target.getName()));
                else Bukkit.dispatchCommand(player, "mute " + safeName(target.getName()) + " Muted by staff");
                break;
            case 24:
                if (online) {
                    player.closeInventory();
                    Bukkit.dispatchCommand(player, "freeze " + target.getName() + " Staff profile action");
                }
                break;
            case 25:
                if (online) { player.closeInventory(); Bukkit.dispatchCommand(player, "invsee " + target.getName()); }
                break;
            case 28: openNotes(player, target); break;
            case 29: player.closeInventory(); Bukkit.dispatchCommand(player, "modlogs " + safeName(target.getName())); break;
            case 30: player.closeInventory(); Bukkit.dispatchCommand(player, "ipcheck " + safeName(target.getName())); break;
            case 31: player.closeInventory(); Bukkit.dispatchCommand(player, "whois " + safeName(target.getName())); break;
            case 32:
                if (online) {
                    player.teleport(target.getPlayer().getLocation());
                    player.closeInventory();
                    player.sendMessage(CC.color("&7Teleported to &f" + target.getName() + "&7."));
                }
                break;
            case 49: openStaffPanel(player); break;
            default: break;
        }
    }

    private void handlePunishmentHistoryClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        UUID uuid = punHistTargets.get(player.getUniqueId());
        if (uuid == null) { player.closeInventory(); return; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
        int page = punHistPages.getOrDefault(player.getUniqueId(), 1);

        if (slot == 45) { punHistPages.put(player.getUniqueId(), page - 1); openPunishmentHistory(player, target); }
        else if (slot == 53) { punHistPages.put(player.getUniqueId(), page + 1); openPunishmentHistory(player, target); }
        else if (slot == 49) openPlayerProfile(player, target);
    }

    private void handleNotesClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 49) { openStaffPanel(player); return; }
        if (!event.isRightClick()) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String noteId = CC.strip(item.getItemMeta().getDisplayName()).trim();
        if (noteId.isEmpty()) return;
        UUID target = profileTargets.get(player.getUniqueId());
        String targetName = target != null ? Bukkit.getOfflinePlayer(target).getName() : null;
        if (targetName != null) {
            Bukkit.dispatchCommand(player, "note delete " + noteId);
            openNotes(player, Bukkit.getOfflinePlayer(target));
        }
    }

    private void handleRanksClick(Player player, int slot) {
        int page   = rankPages.getOrDefault(player.getUniqueId(), 1);
        int maxPage = Math.max(1, (int) Math.ceil(plugin.getRankManager().getVisibleRanksByWeight().size() / (double) CONTENT_SLOTS_54.length));
        if (slot == 45 && page > 1)       { openRanks(player, page - 1); return; }
        if (slot == 53 && page < maxPage) { openRanks(player, page + 1); return; }
        if (slot == 49) { openStaffPanel(player); return; }
        Map<Integer, String> slots = rankSlots.get(player.getUniqueId());
        if (slots == null || !slots.containsKey(slot)) return;
        Rank rank = plugin.getRankManager().getRank(slots.get(slot));
        if (rank != null) openRankEditor(player, rank);
    }

    private void handleRankEditorClick(Player player, int slot) {
        String rankName = editingRanks.get(player.getUniqueId());
        Rank rank = rankName == null ? null : plugin.getRankManager().getRank(rankName);
        if (rank == null) { openRanks(player); return; }

        switch (slot) {
            case 10:
                for (Rank other : plugin.getRankManager().getRanks()) {
                    other.setDefault(other.getName().equalsIgnoreCase(rank.getName()));
                    plugin.getRankManager().saveRank(other);
                }
                openRankEditor(player, rank);
                break;
            case 11:
                rank.setStaff(!rank.isStaff());
                if (rank.isStaff()) rank.setCategory("Staff");
                else if (rank.getCategory().equalsIgnoreCase("Staff")) rank.setCategory(Rank.inferCategory(rank));
                plugin.getRankManager().saveRank(rank);
                openRankEditor(player, rank);
                break;
            case 12:
                rank.setHidden(!rank.isHidden());
                if (rank.isHidden()) rank.setCategory("Hidden");
                else if (rank.getCategory().equalsIgnoreCase("Hidden")) rank.setCategory(Rank.inferCategory(rank));
                plugin.getRankManager().saveRank(rank);
                openRankEditor(player, rank);
                break;
            case 14: openRankCategoryEditor(player, rank); break;
            case 15:
                plugin.getRankManager().backupRanks("gui");
                player.sendMessage(CC.color("&aCreated a rank backup."));
                openRankEditor(player, rank);
                break;
            case 16: openRankPresetConfirm(player); break;
            case 22: openRanks(player); break;
            default: break;
        }
    }

    private void openRankCategoryEditor(Player viewer, Rank rank) {
        editingRanks.put(viewer.getUniqueId(), rank.getName());
        Inventory inv = Bukkit.createInventory(null, 27, rankCategoryTitle().replace("{rank}", rank.getName()));
        fillBorder(inv, MAT_LIME_GLASS_PANE);
        Map<Integer, String> slots = new HashMap<>();
        addRankCategoryItem(inv, slots, 10, "Staff",   Material.IRON_SWORD,    "&c&lStaff",   rank);
        addRankCategoryItem(inv, slots, 11, "Media",   MAT_REDSTONE_TORCH,     "&b&lMedia",   rank);
        addRankCategoryItem(inv, slots, 12, "Store",   Material.EMERALD,       "&a&lStore",   rank);
        addRankCategoryItem(inv, slots, 13, "Hidden",  MAT_INK_SAC,            "&8&lHidden",  rank);
        addRankCategoryItem(inv, slots, 14, "Default", Material.PAPER,           "&7&lDefault", rank);
        inv.setItem(22, item(Material.ARROW, "&c&lBack", Collections.singletonList("&7Return to rank editor.")));
        rankCategorySlots.put(viewer.getUniqueId(), slots);
        viewer.openInventory(inv);
    }

    private void addRankCategoryItem(Inventory inv, Map<Integer, String> slots, int slot,
                                     String category, Material mat, String name, Rank rank) {
        boolean sel = plugin.getRankManager().getRankCategory(rank).equalsIgnoreCase(category);
        inv.setItem(slot, item(mat, name, Arrays.asList(
            "&7Current: " + boolColor(sel),
            "&aClick &7to assign this category.")));
        slots.put(slot, category);
    }

    private void handleRankCategoryClick(Player player, int slot) {
        String rankName = editingRanks.get(player.getUniqueId());
        Rank rank = rankName == null ? null : plugin.getRankManager().getRank(rankName);
        if (rank == null) { openRanks(player); return; }
        if (slot == 22) { openRankEditor(player, rank); return; }
        Map<Integer, String> slots = rankCategorySlots.get(player.getUniqueId());
        if (slots == null || !slots.containsKey(slot)) return;
        applyRankCategory(rank, slots.get(slot));
        plugin.getRankManager().saveRank(rank);
        player.sendMessage(CC.color("&aSet &f" + rank.getName() + " &acategory to &f" + rank.getCategory() + "&a."));
        openRankEditor(player, rank);
    }

    private void handleReportsClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 49) { openStaffPanel(player); return; }
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String name = CC.strip(item.getItemMeta().getDisplayName());
        if (!name.startsWith("#")) return;
        String id = name.substring(1).split(" ")[0];
        if (event.isRightClick()) Bukkit.dispatchCommand(player, "reports close " + id + " Handled from GUI");
        else Bukkit.dispatchCommand(player, "reports claim " + id);
        openReports(player, event.getView().getTitle().equals(helpOpTitle()));
    }

    private void handlePunishClick(Player player, int slot) {
        if (slot == 49) { player.closeInventory(); return; }
        UUID targetUuid = punishTargets.get(player.getUniqueId());
        if (targetUuid == null) { player.closeInventory(); return; }
        List<Preset> presets = plugin.getPunishmentPresetManager().getPresets();
        int[] presetSlots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25};
        for (int i = 0; i < presetSlots.length; i++) {
            if (presetSlots[i] == slot && i < presets.size()) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
                player.closeInventory();
                plugin.getPunishmentPresetManager().executePreset(player, target, presets.get(i).getKey(), null);
                return;
            }
        }
    }

    private void handlePlayerPickerClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 49) { openStaffPanel(player); return; }
        PlayerAction action = pickerActions.get(player.getUniqueId());
        ItemStack item = event.getCurrentItem();
        if (action == null || item == null || !item.hasItemMeta()) return;
        String targetName = CC.strip(item.getItemMeta().getDisplayName());
        Player target = Bukkit.getPlayer(targetName);

        switch (action) {
            case PROFILE:
                if (target != null) openPlayerProfile(player, target);
                else { player.sendMessage(CC.color("&cThat player is no longer online.")); openPlayerPicker(player, action); }
                break;
            case PUNISH:
                if (target != null) openPunishMenu(player, target);
                else { player.sendMessage(CC.color("&cThat player is no longer online.")); openPlayerPicker(player, action); }
                break;
            case FREEZE:
                if (target != null) { player.closeInventory(); Bukkit.dispatchCommand(player, "freeze " + target.getName() + " Staff panel"); }
                else { player.sendMessage(CC.color("&cThat player is no longer online.")); openPlayerPicker(player, action); }
                break;
            case INSPECT:
                if (target != null) { player.closeInventory(); Bukkit.dispatchCommand(player, "invsee " + target.getName()); }
                else { player.sendMessage(CC.color("&cThat player is no longer online.")); openPlayerPicker(player, action); }
                break;
            case NOTES:
                if (target != null) openNotes(player, target);
                else { player.sendMessage(CC.color("&cThat player is no longer online.")); openPlayerPicker(player, action); }
                break;
            case MODLOGS:
                player.closeInventory();
                Bukkit.dispatchCommand(player, "modlogs " + targetName);
                break;
            case GRANT:
                if (target != null) openGrantRankPicker(player, target);
                else { player.sendMessage(CC.color("&cThat player is no longer online.")); openPlayerPicker(player, action); }
                break;
            case WHOIS:
                player.closeInventory();
                Bukkit.dispatchCommand(player, "whois " + targetName);
                break;
            case PING:
                player.closeInventory();
                Bukkit.dispatchCommand(player, "ping " + targetName);
                break;
            case IPCHECK:
                player.closeInventory();
                Bukkit.dispatchCommand(player, "ipcheck " + targetName);
                break;
            default: break;
        }
    }

    private void handleGrantRankClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 49) { openPlayerPicker(player, PlayerAction.GRANT); return; }
        Map<Integer, String> templates = grantTemplateSlots.get(player.getUniqueId());
        if (templates != null && templates.containsKey(event.getSlot())) {
            String targetName = grantTargetNames.get(player.getUniqueId());
            if (targetName == null) { player.closeInventory(); player.sendMessage(CC.color("&cGrant session expired.")); return; }
            player.closeInventory();
            Bukkit.dispatchCommand(player, "grant template " + targetName + " " + templates.get(event.getSlot()));
            return;
        }
        Map<Integer, String> slots = grantRankSlots.get(player.getUniqueId());
        if (slots == null || !slots.containsKey(event.getSlot())) return;
        grantRanks.put(player.getUniqueId(), slots.get(event.getSlot()));
        openGrantDurationPicker(player);
    }

    private void handleGrantDurationClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 19) {
            UUID tu = grantTargets.get(player.getUniqueId());
            if (tu == null) openPlayerPicker(player, PlayerAction.GRANT);
            else openGrantRankPicker(player, Bukkit.getOfflinePlayer(tu));
            return;
        }
        if (event.getSlot() == 22) { player.closeInventory(); return; }
        Map<Integer, String> slots = grantDurationSlots.get(player.getUniqueId());
        if (slots == null || !slots.containsKey(event.getSlot())) return;
        grantDurations.put(player.getUniqueId(), slots.get(event.getSlot()));
        openGrantReasonPicker(player);
    }

    private void handleGrantReasonClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 19) { openGrantDurationPicker(player); return; }
        if (event.getSlot() == 22) { player.closeInventory(); return; }
        Map<Integer, String> slots = grantReasonSlots.get(player.getUniqueId());
        if (slots == null || !slots.containsKey(event.getSlot())) return;
        String targetName = grantTargetNames.get(player.getUniqueId());
        String rankName   = grantRanks.get(player.getUniqueId());
        String duration   = grantDurations.get(player.getUniqueId());
        String reason     = slots.get(event.getSlot());
        if (targetName == null || rankName == null || duration == null || reason == null) {
            player.closeInventory(); player.sendMessage(CC.color("&cGrant session expired.")); return;
        }
        player.closeInventory();
        Bukkit.dispatchCommand(player, "grant " + targetName + " " + rankName + " " + duration + " " + reason);
    }

    private void handleTagClick(Player player, InventoryClickEvent event) {
        int page = tagPages.getOrDefault(player.getUniqueId(), 1);
        int slot = event.getSlot();
        if (slot == 45) { openTagMenu(player, page - 1); return; }
        if (slot == 53) { openTagMenu(player, page + 1); return; }
        if (slot == 51) { player.closeInventory(); return; }
        if (slot == 49) { setPlayerTag(player, ""); openTagMenu(player, page); return; }
        if (slot == 47) {
            List<TagGuiEntry> unlocked = new ArrayList<>();
            for (TagGuiEntry t : loadTagEntries()) if (canUseTag(player, t)) unlocked.add(t);
            if (unlocked.isEmpty()) { player.sendMessage(CC.color("&cNo unlocked tags.")); return; }
            TagGuiEntry picked = unlocked.get(new Random().nextInt(unlocked.size()));
            setPlayerTag(player, picked.display);
            openTagMenu(player, page);
            return;
        }
        Map<Integer, TagGuiEntry> slots = tagSlots.get(player.getUniqueId());
        if (slots == null || !slots.containsKey(slot)) return;
        TagGuiEntry tag = slots.get(slot);
        if (event.isRightClick()) {
            player.sendMessage(CC.color("&7Preview: " + tag.display + " &f" + plugin.getDisguiseManager().getVisibleName(player) + "&7: &fHello!"));
            return;
        }
        if (!canUseTag(player, tag)) { player.sendMessage(CC.color("&cLocked. Required: &f" + requiredTagPermission(tag))); return; }
        setPlayerTag(player, tag.display);
        openTagMenu(player, page);
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================

    private void fillBorder(Inventory inv, Material glass) {
        ItemStack pane = glassPane(glass);
        int size  = inv.getSize();
        int rows  = size / 9;
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = size - 9; i < size; i++) inv.setItem(i, pane);
        for (int row = 1; row < rows - 1; row++) {
            inv.setItem(row * 9, pane);
            inv.setItem(row * 9 + 8, pane);
        }
    }

    private ItemStack glassPane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack badge(Material mat, String name, int count, String desc) {
        String cnt = count > 0 ? " &8[&f" + count + "&8]" : "";
        return item(mat, name + cnt, Arrays.asList(desc, "", "&aClick to open."));
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) coloredLore.add(CC.color(line));
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack toggleItem(Material material, String label, String setting) {
        boolean enabled = plugin.getConfig().getBoolean("lobby-protection." + setting, true);
        return item(material, (enabled ? "&a" : "&c") + label,
            Arrays.asList("&7Current: " + enabledText(enabled), "&aClick &7to toggle."));
    }

    private void toggleConfig(String path, boolean fallback) {
        plugin.getConfig().set(path, !plugin.getConfig().getBoolean(path, fallback));
        plugin.saveConfig();
    }

    private void enforceLobbyProtection(String world) {
        plugin.getConfig().set("lobby-protection.enabled", true);
        for (String setting : lobbyEnforcedSettings()) plugin.getConfig().set("lobby-protection." + setting, true);
        addProtectedWorld(world);
        plugin.saveConfig();
    }

    private List<String> lobbyEnforcedSettings() {
        return Arrays.asList(
            "prevent-physical-interact", "prevent-block-interact", "prevent-dangerous-item-use",
            "prevent-entity-interact", "prevent-pistons", "prevent-fire", "prevent-spread",
            "prevent-liquid-flow", "prevent-growth", "prevent-block-form", "prevent-block-fade",
            "prevent-leaf-decay", "prevent-explosions", "prevent-entity-block-change",
            "no-mobs", "no-mob-targeting", "prevent-damage", "prevent-projectiles",
            "prevent-item-drops", "prevent-item-pickup", "prevent-hunger",
            "prevent-weather", "notify-on-deny", "void-rescue.enabled");
    }

    private void addProtectedWorld(String world) {
        List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
        for (String w : worlds) if (w.equalsIgnoreCase(world)) return;
        worlds.add(world);
        plugin.getConfig().set("lobby-protection.worlds", worlds);
    }

    private void removeProtectedWorld(String world) {
        List<String> worlds  = plugin.getConfig().getStringList("lobby-protection.worlds");
        List<String> updated = new ArrayList<>();
        for (String w : worlds) if (!w.equalsIgnoreCase(world)) updated.add(w);
        plugin.getConfig().set("lobby-protection.worlds", updated);
    }

    private boolean isProtectedWorld(String world) {
        if (!plugin.getConfig().getBoolean("lobby-protection.enabled", true)) return false;
        List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
        if (worlds.isEmpty()) return true;
        for (String w : worlds) if (w.equalsIgnoreCase(world)) return true;
        return false;
    }

    private String protectionMode() {
        List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
        return worlds.isEmpty() ? "All worlds" : joinText(worlds);
    }

    private void setPlayerTag(Player player, String tag) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null) { player.sendMessage(CC.color("&cProfile not loaded.")); return; }
        profile.setTag(tag);
        plugin.getPlayerManager().saveProfile(profile);
        plugin.getNameTagManager().applyNameTag(player);
        player.sendMessage(CC.color(tag == null || tag.isEmpty() ? "&7Your tag was cleared." : "&7Your tag is now " + tag + "&7."));
    }

    private List<TagGuiEntry> loadTagEntries() {
        List<TagGuiEntry> tags = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        ConfigurationSection catalog = plugin.getConfig().getConfigurationSection("tags.catalog");
        if (catalog != null) {
            for (String key : catalog.getKeys(false)) {
                TagGuiEntry tag = readTagEntry(catalog, key);
                if (tag != null && seen.add(normalizeTag(tag.id))) tags.add(tag);
            }
        }
        for (String display : plugin.getConfig().getStringList("tags.available")) {
            if (display == null || display.trim().isEmpty()) continue;
            String id = normalizeTag(display);
            if (id.isEmpty() || !seen.add(id)) continue;
            tags.add(new TagGuiEntry(id, display, "Public", "Unlocked", "", "", Collections.<String>emptyList(), 1000 + tags.size()));
        }
        tags.sort(Comparator.comparingInt(t -> t.order));
        return tags;
    }

    private TagGuiEntry readTagEntry(ConfigurationSection catalog, String key) {
        String id = normalizeTag(key);
        if (id.isEmpty()) return null;
        if (catalog.isString(key)) {
            String display = catalog.getString(key, key);
            return new TagGuiEntry(id, display, "Available", "Common", "", "", Collections.<String>emptyList(), 0);
        }
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("tags.catalog." + key);
        if (section == null) return null;
        String display = section.getString("display", key);
        if (display == null || display.trim().isEmpty()) return null;
        return new TagGuiEntry(id, display,
            section.getString("category", "Available"),
            section.getString("rarity", "Common"),
            section.getString("description", ""),
            section.getString("permission", ""),
            section.getStringList("ranks"),
            section.getInt("order", 500));
    }

    private int unlockedTagCount(Player player, List<TagGuiEntry> tags) {
        int count = 0;
        for (TagGuiEntry t : tags) if (canUseTag(player, t)) count++;
        return count;
    }

    private boolean canUseTag(Player player, TagGuiEntry tag) {
        if (player.hasPermission("evaulx.tag.all")) return true;
        if (!plugin.getConfig().getBoolean("tags.require-per-tag-permission", true)) return true;
        if (!tag.permission.isEmpty() && player.hasPermission(tag.permission)) return true;
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile != null) {
            for (String rank : tag.ranks) {
                if (profile.getRankName().equalsIgnoreCase(rank)) return true;
                for (String extra : profile.getExtraRanks()) if (extra.equalsIgnoreCase(rank)) return true;
            }
        }
        return tag.permission.isEmpty() && tag.ranks.isEmpty();
    }

    private String requiredTagPermission(TagGuiEntry tag) {
        return tag.permission.isEmpty() ? "evaulx.tag.all" : tag.permission;
    }

    private String normalizeTag(String input) {
        return DisplayUtil.stripFormat(input).toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", "");
    }

    private void applyRankCategory(Rank rank, String category) {
        String normalized = Rank.normalizeCategory(category);
        rank.setCategory(normalized);
        rank.setHidden(normalized.equals("Hidden"));
        rank.setStaff(normalized.equals("Staff"));
    }

    private List<String> grantDurationOptions() {
        List<String> d = plugin.getConfig().getStringList("gui.grants.duration-options");
        if (d.isEmpty()) d = Arrays.asList("perm", "1h", "1d", "7d", "30d", "90d");
        return d;
    }

    private List<String> grantReasonOptions() {
        List<String> r = plugin.getConfig().getStringList("gui.grants.reason-options");
        if (r.isEmpty()) r = Arrays.asList("Promotion", "Staff trial", "Donor package", "Event reward", "Correction", "Manual grant");
        return r;
    }

    private Material materialFromConfig(String configPath, String fallback) {
        return material(plugin.getConfig().getString(configPath, fallback), fallback);
    }

    private Material material(String raw, String fallback) {
        Material m = Material.matchMaterial(raw == null ? fallback : raw);
        if (m != null) return m;
        m = Material.matchMaterial(fallback);
        return m == null ? Material.PAPER : m;
    }

    private String safeName(String name) {
        return name == null || name.trim().isEmpty() ? "Unknown" : name;
    }

    private String enabledText(boolean enabled) {
        return enabled ? "&aenabled" : "&cdisabled";
    }

    private String boolColor(boolean value) {
        return value ? "&atrue" : "&cfalse";
    }

    private String formatDate(long millis) {
        if (millis <= 0) return "never";
        return DATE_FMT.format(new Date(millis));
    }

    private String clip(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private String joinText(List<String> values) {
        StringBuilder b = new StringBuilder();
        for (String v : values) { if (b.length() > 0) b.append(", "); b.append(v); }
        return b.toString();
    }

    private int onlineStaffCount() {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) if (plugin.getStaffRequestManager().canReceiveStaffAlerts(p)) count++;
        return count;
    }

    private long countActive(List<Punishment> history, boolean bans) {
        long count = 0;
        for (Punishment p : history) {
            if (p.isActive() && (bans ? p.getType().isBan() : p.getType().isMute())) count++;
        }
        return count;
    }

    private Material actionMaterial(String action) {
        if (action == null) return Material.PAPER;
        String u = action.toUpperCase(Locale.ENGLISH);
        if (u.contains("BAN"))      return Material.BARRIER;
        if (u.contains("MUTE"))     return MAT_RED_DYE;
        if (u.contains("KICK"))     return Material.FEATHER;
        if (u.contains("WARN"))     return MAT_YELLOW_DYE;
        if (u.contains("GRANT"))    return Material.GOLD_INGOT;
        if (u.contains("FREEZE"))   return Material.PACKED_ICE;
        if (u.contains("STAFF"))    return Material.COMPASS;
        if (u.contains("STREAMER")) return MAT_MAGENTA_DYE;
        if (u.contains("VANISH"))   return MAT_ENDER_EYE;
        if (u.contains("NOTE"))     return MAT_WRITABLE_BOOK;
        return Material.PAPER;
    }

    // =====================================================================
    //  TITLE METHODS
    // =====================================================================

    private String streamerModeTitle()   { return CC.color(plugin.getConfig().getString("gui.streamer-mode.title", "&8[&dStreamer&8] Settings")); }
    private String staffTitle()          { return CC.color(plugin.getConfig().getString("gui.staff-panel.title", "&8Staff Panel")); }
    private String reportsTitle()        { return CC.color(plugin.getConfig().getString("gui.reports.title", "&8Reports")); }
    private String helpOpTitle()         { return CC.color(plugin.getConfig().getString("gui.helpop.title", "&8HelpOP")); }
    private String punishTitle()         { return CC.color(plugin.getConfig().getString("gui.punish.title", "&8Punish: {player}")); }
    private String playerPickerPrefix()  { return CC.color(plugin.getConfig().getString("gui.player-picker.title-prefix", "&8Pick: ")); }
    private String playerPickerTitle(PlayerAction action) { return playerPickerPrefix() + action.getLabel(); }
    private String grantTitle()          { return CC.color(plugin.getConfig().getString("gui.grants.title", "&8Grant: {player}")); }
    private String grantTitlePrefix()    { return CC.color(plugin.getConfig().getString("gui.grants.title-prefix", "&8Grant: ")); }
    private String grantDurationTitle()  { return CC.color(plugin.getConfig().getString("gui.grants.duration-title", "&8Grant Time: {player}")); }
    private String grantDurationTitlePrefix() { return CC.color(plugin.getConfig().getString("gui.grants.duration-title-prefix", "&8Grant Time: ")); }
    private String grantReasonTitle()    { return CC.color(plugin.getConfig().getString("gui.grants.reason-title", "&8Grant Reason: {player}")); }
    private String grantReasonTitlePrefix() { return CC.color(plugin.getConfig().getString("gui.grants.reason-title-prefix", "&8Grant Reason: ")); }
    private String tagTitle()            { return CC.color(plugin.getConfig().getString("tags.gui.title", "&8Tags {page}/{pages}")); }
    private String tagTitlePrefix() {
        String raw = plugin.getConfig().getString("tags.gui.title", "&8Tags {page}/{pages}");
        int idx = raw.indexOf("{page}");
        return CC.color(idx >= 0 ? raw.substring(0, idx) : raw);
    }
    private String lobbyProtectionTitle()    { return CC.color(plugin.getConfig().getString("gui.lobby-protection.title", "&8Lobby Protection")); }
    private String rankPresetConfirmTitle()  { return CC.color(plugin.getConfig().getString("gui.rank-preset-confirm.title", "&8Reset Ranks?")); }
    private String rankEditorTitle()         { return CC.color(plugin.getConfig().getString("gui.rank-editor.title", "&8Rank: {rank}")); }
    private String rankEditorTitlePrefix() {
        String raw = plugin.getConfig().getString("gui.rank-editor.title", "&8Rank: {rank}");
        int idx = raw.indexOf("{rank}");
        return CC.color(idx >= 0 ? raw.substring(0, idx) : raw);
    }
    private String rankCategoryTitle()  { return CC.color(plugin.getConfig().getString("gui.rank-category.title", "&8Rank Category: {rank}")); }
    private String rankCategoryTitlePrefix() {
        String raw = plugin.getConfig().getString("gui.rank-category.title", "&8Rank Category: {rank}");
        int idx = raw.indexOf("{rank}");
        return CC.color(idx >= 0 ? raw.substring(0, idx) : raw);
    }
    private String staffDashboardTitle()  { return CC.color(plugin.getConfig().getString("gui.staff-dashboard.title", "&8Staff Dashboard")); }
    private String adminPanelTitle()      { return CC.color(plugin.getConfig().getString("gui.admin-panel.title", "&8Admin Panel")); }
    private String ownerPanelTitle()      { return CC.color(plugin.getConfig().getString("gui.owner-panel.title", "&8Owner Panel")); }
    private String maintenanceTitle()     { return CC.color(plugin.getConfig().getString("gui.maintenance.title", "&8Maintenance")); }
    private String staffSessionsTitle()   { return CC.color(plugin.getConfig().getString("gui.staff-sessions.title", "&8Staff Sessions")); }
    private String pendingGrantsTitle()   { return CC.color(plugin.getConfig().getString("gui.pending-grants.title", "&8Pending Grants")); }
    private String playerProfileTitle()   { return CC.color(plugin.getConfig().getString("gui.player-profile.title", "&8Profile: {player}")); }
    private String playerProfileTitlePrefix() {
        String raw = plugin.getConfig().getString("gui.player-profile.title", "&8Profile: {player}");
        int idx = raw.indexOf("{player}");
        return CC.color(idx >= 0 ? raw.substring(0, idx) : raw);
    }
    private String punishmentHistoryPrefix() { return CC.color("&8Punishments: "); }
    private String appealsTitle()            { return CC.color("&8Appeals"); }
    private String appealDetailPrefix()      { return CC.color("&8Appeal: "); }

    // =====================================================================
    //  ENUMS + INNER CLASSES
    // =====================================================================

    public enum PlayerAction {
        PUNISH("Punish"),
        FREEZE("Freeze"),
        INSPECT("Inspect"),
        NOTES("Notes"),
        MODLOGS("ModLogs"),
        GRANT("Grant"),
        PROFILE("Profile"),
        WHOIS("Whois"),
        PING("Ping"),
        IPCHECK("IPCheck");

        private final String label;
        PlayerAction(String label) { this.label = label; }
        public String getLabel()   { return label; }
    }

    private static class TagGuiEntry {
        final String id, display, category, rarity, description, permission;
        final List<String> ranks;
        final int order;

        TagGuiEntry(String id, String display, String category, String rarity,
                    String description, String permission, List<String> ranks, int order) {
            this.id          = id;
            this.display     = display;
            this.category    = category    == null ? "" : category;
            this.rarity      = rarity      == null ? "" : rarity;
            this.description = description == null ? "" : description;
            this.permission  = permission  == null ? "" : permission;
            this.ranks       = ranks       == null ? Collections.<String>emptyList() : ranks;
            this.order       = order;
        }
    }
}
