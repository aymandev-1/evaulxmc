package dev.evaulx.core.gui;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.managers.GrantTemplateManager.GrantTemplate;
import dev.evaulx.core.managers.GrantManager.PendingGrant;
import dev.evaulx.core.managers.PunishmentPresetManager.Preset;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.PlayerNote;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuiManager implements Listener {

    private final EvaulxCore plugin;
    private final Map<UUID, UUID> punishTargets = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> grantTargets = new ConcurrentHashMap<>();
    private final Map<UUID, String> grantTargetNames = new ConcurrentHashMap<>();
    private final Map<UUID, String> grantRanks = new ConcurrentHashMap<>();
    private final Map<UUID, String> grantDurations = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> grantRankSlots = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> grantTemplateSlots = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> grantDurationSlots = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> grantReasonSlots = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> tagPages = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, TagGuiEntry>> tagSlots = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerAction> pickerActions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> profileTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> rankSlots = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> rankPages = new ConcurrentHashMap<>();
    private final Map<UUID, String> editingRanks = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, String>> rankCategorySlots = new ConcurrentHashMap<>();

    public GuiManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void openStaffPanel(Player player) {
        String title = staffTitle();
        Inventory inventory = Bukkit.createInventory(null, 36, title);

        inventory.setItem(slotFromConfig("gui.staff-panel.items.reports.slot", 10), item(materialFromConfig("gui.staff-panel.items.reports.material", "BOOK"),
                configName("gui.staff-panel.items.reports.name", "&cReports"),
                Arrays.asList("&7View, claim, and close reports.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.helpop.slot", 11), item(materialFromConfig("gui.staff-panel.items.helpop.material", "PAPER"),
                configName("gui.staff-panel.items.helpop.name", "&cHelpOP"),
                Arrays.asList("&7View recent help requests.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.punish.slot", 12), item(materialFromConfig("gui.staff-panel.items.punish.material", "DIAMOND_SWORD"),
                configName("gui.staff-panel.items.punish.name", "&cPunish"),
                Arrays.asList("&7Pick a player and preset.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.freeze.slot", 13), item(materialFromConfig("gui.staff-panel.items.freeze.material", "PACKED_ICE"),
                configName("gui.staff-panel.items.freeze.name", "&cFreeze"),
                Arrays.asList("&7Toggle freeze for a player.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.inspect.slot", 14), item(materialFromConfig("gui.staff-panel.items.inspect.material", "CHEST"),
                configName("gui.staff-panel.items.inspect.name", "&cInspect"),
                Arrays.asList("&7Open a player's inventory.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.notes.slot", 15), item(materialFromConfig("gui.staff-panel.items.notes.material", "EMPTY_MAP"),
                configName("gui.staff-panel.items.notes.name", "&cNotes"),
                Arrays.asList("&7View notes for a player.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.modlogs.slot", 16), item(materialFromConfig("gui.staff-panel.items.modlogs.material", "WATCH"),
                configName("gui.staff-panel.items.modlogs.name", "&cMod Logs"),
                Arrays.asList("&7Open moderation logs.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.grants.slot", 20), item(materialFromConfig("gui.staff-panel.items.grants.material", "GOLD_INGOT"),
                configName("gui.staff-panel.items.grants.name", "&6Grant Rank"),
                Arrays.asList("&7Pick a player and rank.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.vanish.slot", 21), item(materialFromConfig("gui.staff-panel.items.vanish.material", "EYE_OF_ENDER"),
                configName("gui.staff-panel.items.vanish.name", "&cToggle Vanish"),
                Arrays.asList("&7Switch your vanish state.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.staffmode.slot", 22), item(materialFromConfig("gui.staff-panel.items.staffmode.material", "COMPASS"),
                configName("gui.staff-panel.items.staffmode.name", "&cStaff Mode"),
                Arrays.asList("&7Toggle staff mode.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.ranks.slot", 23), item(materialFromConfig("gui.staff-panel.items.ranks.material", "NAME_TAG"),
                configName("gui.staff-panel.items.ranks.name", "&cRanks"),
                Arrays.asList("&7View loaded ranks.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.lobby-protection.slot", 24), item(materialFromConfig("gui.staff-panel.items.lobby-protection.material", "IRON_DOOR"),
                configName("gui.staff-panel.items.lobby-protection.name", "&cLobby Protection"),
                Arrays.asList("&7Manage lobby protection.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.dashboard.slot", 25), item(materialFromConfig("gui.staff-panel.items.dashboard.material", "NETHER_STAR"),
                configName("gui.staff-panel.items.dashboard.name", "&cStaff Dashboard"),
                Arrays.asList("&7View live staff activity.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.maintenance.slot", 29), item(materialFromConfig("gui.staff-panel.items.maintenance.material", "REDSTONE_TORCH_ON"),
                configName("gui.staff-panel.items.maintenance.name", "&cMaintenance"),
                Arrays.asList("&7Toggle and inspect maintenance mode.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.sessions.slot", 30), item(materialFromConfig("gui.staff-panel.items.sessions.material", "BOOK"),
                configName("gui.staff-panel.items.sessions.name", "&cStaff Sessions"),
                Arrays.asList("&7View online and recent staff time.")));
        inventory.setItem(slotFromConfig("gui.staff-panel.items.close.slot", 31), item(Material.BARRIER, "&cClose", Collections.singletonList("&7Close this menu.")));

        player.openInventory(inventory);
    }

    public void openLobbyProtection(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, lobbyProtectionTitle());
        String world = player.getWorld().getName();

        inventory.setItem(4, item(Material.BEACON, "&cLobby Protection",
                Arrays.asList("&7Current world: &f" + world,
                        "&7Mode: &f" + protectionMode(),
                        "&7This world: " + enabledText(isProtectedWorld(world)))));
        inventory.setItem(10, item(Material.REDSTONE_TORCH, "&cToggle Protection",
                Arrays.asList("&7Current: " + enabledText(plugin.getConfig().getBoolean("lobby-protection.enabled", true)),
                        "&aClick &7to switch the master protection state.")));
        inventory.setItem(11, item(Material.ANVIL, "&cEnforce Current World",
                Arrays.asList("&7Enables protection and turns on strict options.",
                        "&7World: &f" + world)));
        inventory.setItem(12, item(Material.GRASS_BLOCK, "&cProtect Current World",
                Collections.singletonList("&7Adds &f" + world + " &7to protected worlds.")));
        inventory.setItem(13, item(Material.DIRT, "&cUnprotect Current World",
                Collections.singletonList("&7Removes &f" + world + " &7from protected worlds.")));
        inventory.setItem(14, item(Material.COMPASS, "&cProtect All Worlds",
                Collections.singletonList("&7Empty world list means every loaded world is protected.")));
        inventory.setItem(15, item(Material.PAPER, "&cDenied Messages",
                Arrays.asList("&7Current: " + enabledText(plugin.getConfig().getBoolean("lobby-protection.notify-on-deny", true)),
                        "&7Players get a cooldown-protected deny message.")));
        inventory.setItem(16, item(Material.DIAMOND_PICKAXE, "&cBuild Mode",
                Collections.singletonList("&7Toggle your staff build bypass.")));

        inventory.setItem(20, toggleItem(Material.IRON_SWORD, "Damage", "prevent-damage"));
        inventory.setItem(21, toggleItem(Material.TNT, "Explosions", "prevent-explosions"));
        inventory.setItem(22, toggleItem(Material.ZOMBIE_SPAWN_EGG, "Mobs", "no-mobs"));
        inventory.setItem(23, toggleItem(Material.CHEST, "Drops/Pickup", "prevent-item-drops"));
        inventory.setItem(24, toggleItem(Material.LEVER, "Interactions", "prevent-block-interact"));
        inventory.setItem(25, toggleItem(Material.WATER_BUCKET, "Liquid/Growth", "prevent-liquid-flow"));
        inventory.setItem(26, toggleItem(Material.ENDER_PEARL, "Void Rescue", "void-rescue.enabled"));

        inventory.setItem(31, item(Material.BOOK, "&cStatus",
                Arrays.asList("&7Protected worlds: &f" + protectionMode(),
                        "&7Use &f/lobbyprotect status &7for text output.")));
        inventory.setItem(40, item(Material.BARRIER, "&cClose", Collections.singletonList("&7Close this menu.")));
        player.openInventory(inventory);
    }

    public void openRankPresetConfirm(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, rankPresetConfirmTitle());
        inventory.setItem(11, item(Material.EMERALD_BLOCK, "&aConfirm Reset",
                Arrays.asList("&7Deletes every stored rank.",
                        "&7Backs up the current ranks first.",
                        "&7Installs only the EvaulxMC rank list.")));
        inventory.setItem(15, item(Material.REDSTONE_BLOCK, "&cCancel",
                Collections.singletonList("&7Return without changing ranks.")));
        player.openInventory(inventory);
    }

    public void openStaffDashboard(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, staffDashboardTitle());
        inventory.setItem(4, item(Material.NETHER_STAR, "&cStaff Dashboard",
                Arrays.asList("&7Online staff: &f" + onlineStaffCount(),
                        "&7Reports: &f" + plugin.getStaffRequestManager().getReports().size(),
                        "&7HelpOP: &f" + plugin.getStaffRequestManager().getHelpOps().size(),
                        "&7Frozen: &f" + plugin.getStaffRequestManager().getFrozenCount(),
                        "&7Pending grants: &f" + plugin.getGrantManager().getPendingGrants().size(),
                        "&7Staff sessions: &f" + plugin.getStaffRequestManager().getActiveStaffSessionCount(),
                        "&7Maintenance: &f" + plugin.getConfig().getBoolean("maintenance.enabled", false))));
        inventory.setItem(10, item(Material.BOOK, "&cReports", Collections.singletonList("&7Open report queue.")));
        inventory.setItem(11, item(Material.PAPER, "&cHelpOP", Collections.singletonList("&7Open help request queue.")));
        inventory.setItem(12, item(Material.GOLD_INGOT, "&6Pending Grants", Collections.singletonList("&7View grant requests.")));
        inventory.setItem(13, item(Material.PACKED_ICE, "&cFrozen Players", Collections.singletonList("&7Count: &f" + plugin.getStaffRequestManager().getFrozenCount())));
        inventory.setItem(14, item(Material.CLOCK, "&cRecent Actions", Collections.singletonList("&7Latest staff actions.")));
        inventory.setItem(15, item(Material.BOOK, "&cStaff Sessions", Collections.singletonList("&7View tracked staff time.")));
        inventory.setItem(16, item(Material.REDSTONE_TORCH, "&cMaintenance", Collections.singletonList("&7Open maintenance controls.")));

        int slot = 27;
        for (StaffAction action : plugin.getStaffRequestManager().getRecentActions(18)) {
            if (slot >= 45) break;
            inventory.setItem(slot++, item(Material.PAPER, "&c" + action.getAction(),
                    Arrays.asList("&7Actor: &f" + action.getActor(),
                            "&7Target: &f" + (action.getTarget() == null ? "none" : action.getTarget()),
                            "&7When: &f" + action.getFormattedTime(),
                            "&7" + action.getDetail())));
        }
        inventory.setItem(49, item(Material.ARROW, "&cBack", Collections.singletonList("&7Return to staff panel.")));
        player.openInventory(inventory);
    }

    public void openMaintenance(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, maintenanceTitle());
        boolean enabled = plugin.getConfig().getBoolean("maintenance.enabled", false);
        String reason = plugin.getConfig().getString("maintenance.reason", "Server maintenance");
        inventory.setItem(4, item(Material.REDSTONE_TORCH, "&cMaintenance",
                Arrays.asList("&7Enabled: " + enabledText(enabled),
                        "&7Reason: &f" + reason,
                        "&7Allowed players: &f" + plugin.getConfig().getStringList("maintenance.allowed-players").size())));
        inventory.setItem(10, item(enabled ? Material.REDSTONE_BLOCK : Material.EMERALD_BLOCK,
                enabled ? "&cDisable Maintenance" : "&aEnable Maintenance",
                Collections.singletonList("&7Click to switch the current mode.")));
        inventory.setItem(12, item(Material.PAPER, "&cStatus", Collections.singletonList("&7Print maintenance status to chat.")));
        inventory.setItem(14, item(Material.PLAYER_HEAD, "&cAllowed Players",
                Collections.singletonList("&7Use /maintenance allow <player> to add.")));
        inventory.setItem(16, item(Material.BOOK, "&cStaff Sessions", Collections.singletonList("&7Open session tracking.")));
        inventory.setItem(22, item(Material.ARROW, "&cBack", Collections.singletonList("&7Return to staff dashboard.")));
        player.openInventory(inventory);
    }

    public void openStaffSessions(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, staffSessionsTitle());
        inventory.setItem(4, item(Material.BOOK, "&cStaff Sessions",
                Arrays.asList("&7Active: &f" + plugin.getStaffRequestManager().getActiveStaffSessionCount(),
                        "&7Recent stored: &f" + plugin.getStaffRequestManager().getRecentStaffSessions(250).size())));

        int slot = 10;
        for (StaffSession session : plugin.getStaffRequestManager().getActiveStaffSessions()) {
            if (slot > 16) break;
            inventory.setItem(slot++, item(Material.EMERALD, "&a" + session.getName(),
                    Arrays.asList("&7Status: &aOnline",
                            "&7Started: &f" + session.getStartedFormatted(),
                            "&7Duration: &f" + session.getDurationString())));
        }

        slot = 27;
        for (StaffSession session : plugin.getStaffRequestManager().getRecentStaffSessions(18)) {
            if (slot >= 45) break;
            inventory.setItem(slot++, item(Material.PAPER, "&f" + session.getName(),
                    Arrays.asList("&7Started: &f" + session.getStartedFormatted(),
                            "&7Duration: &f" + session.getDurationString())));
        }
        inventory.setItem(49, item(Material.ARROW, "&cBack", Collections.singletonList("&7Return to staff dashboard.")));
        player.openInventory(inventory);
    }

    public void openPendingGrants(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, pendingGrantsTitle());
        int slot = 0;
        for (PendingGrant pending : plugin.getGrantManager().getPendingGrants()) {
            if (slot >= 45) break;
            inventory.setItem(slot++, item(Material.GOLD_INGOT, "&6" + pending.getId(),
                    Arrays.asList("&7Target: &f" + pending.getTargetName(),
                            "&7Rank: &f" + pending.getRankName(),
                            "&7Requester: &f" + pending.getRequesterName(),
                            "&7Reason: &f" + pending.getReason(),
                            "",
                            "&aLeft-click &7approve.",
                            "&cRight-click &7deny.")));
        }
        inventory.setItem(49, item(Material.ARROW, "&cBack", Collections.singletonList("&7Return to dashboard.")));
        player.openInventory(inventory);
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

        Inventory inventory = Bukkit.createInventory(null, 54, playerProfileTitle().replace("{player}", safeName(target.getName())));
        inventory.setItem(4, item(Material.PLAYER_HEAD, "&f" + safeName(target.getName()),
                Arrays.asList("&7UUID: &f" + target.getUniqueId(),
                        "&7Online: &f" + target.isOnline(),
                        "&7IP: &f" + (profile.getIp() == null ? "unknown" : profile.getIp()))));
        inventory.setItem(10, item(Material.NAME_TAG, "&cRanks",
                Arrays.asList("&7Primary: &f" + profile.getRankName(),
                        "&7Extra: &f" + (profile.getExtraRanks().isEmpty() ? "none" : joinText(profile.getExtraRanks())))));
        inventory.setItem(11, item(Material.GOLD_INGOT, "&6Grants",
                Collections.singletonList("&7Stored: &f" + plugin.getGrantManager().getGrants(target.getUniqueId()).size())));
        inventory.setItem(12, item(Material.BOOK, "&cPunishments",
                Collections.singletonList("&7Stored: &f" + plugin.getPunishmentManager().getHistory(target.getUniqueId()).size())));
        inventory.setItem(13, item(Material.MAP, "&cNotes",
                Collections.singletonList("&7Stored: &f" + plugin.getNoteManager().getNoteCount(target.getUniqueId()))));
        inventory.setItem(14, item(Material.NETHER_STAR, "&dTag",
                Collections.singletonList("&7Current: " + (profile.getTag().isEmpty() ? "&fnone" : profile.getTag()))));
        inventory.setItem(15, item(Material.ENDER_EYE, "&cStaff State",
                Arrays.asList("&7Vanished: &f" + profile.isVanished(),
                        "&7Staff mode: &f" + profile.isStaffMode(),
                        "&7God: &f" + profile.isGodMode(),
                        "&7Disguised: &f" + profile.isDisguised())));
        inventory.setItem(16, item(Material.ICE, "&cFreeze",
                Collections.singletonList("&7Frozen: &f" + (target.isOnline() && plugin.getStaffRequestManager().isFrozen(target.getPlayer())))));
        inventory.setItem(28, item(Material.GOLD_INGOT, "&6Grant Rank", Collections.singletonList("&7Open grant GUI.")));
        inventory.setItem(29, item(Material.MAP, "&cNotes", Collections.singletonList("&7Open notes.")));
        inventory.setItem(30, item(Material.CLOCK, "&cMod Logs", Collections.singletonList("&7Open mod logs.")));
        inventory.setItem(49, item(Material.BARRIER, "&cClose", Collections.singletonList("&7Close this profile.")));
        viewer.openInventory(inventory);
    }

    public void openReports(Player player, boolean helpOps) {
        String title = helpOps ? helpOpTitle() : reportsTitle();
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        List<StaffRequest> requests = helpOps ? plugin.getStaffRequestManager().getHelpOps() : plugin.getStaffRequestManager().getReports();

        int slot = 0;
        for (StaffRequest request : requests) {
            if (slot >= 45) break;
            List<String> lore = new ArrayList<>();
            lore.add("&7From: &f" + request.getSender());
            if (request.getTarget() != null) lore.add("&7Target: &f" + request.getTarget());
            lore.add("&7Message: &f" + request.getMessage());
            lore.add("&7Status: &f" + request.getStatus().name());
            lore.add("");
            lore.add("&aLeft-click &7to claim.");
            lore.add("&cRight-click &7to close.");
            inventory.setItem(slot++, item(Material.PAPER, "&c#" + request.getId() + " &f" + request.getType().name(), lore));
        }

        inventory.setItem(49, item(Material.ARROW, "&cBack", Collections.singletonList("&7Return to staff panel.")));
        player.openInventory(inventory);
    }

    public void openPunishMenu(Player player, OfflinePlayer target) {
        punishTargets.put(player.getUniqueId(), target.getUniqueId());
        String title = punishTitle().replace("{player}", target.getName());
        Inventory inventory = Bukkit.createInventory(null, 27, title);

        int slot = 10;
        for (Preset preset : plugin.getPunishmentPresetManager().getPresets()) {
            if (slot > 16) break;
            List<String> lore = new ArrayList<>(preset.getLore());
            if (lore.isEmpty()) {
                lore.add("&7Click to apply the next ladder step.");
            }
            inventory.setItem(slot++, item(material(preset.getMaterial(), "PAPER"), preset.getDisplayName(), lore));
        }

        inventory.setItem(22, item(Material.BARRIER, "&cClose", Collections.singletonList("&7Close this menu.")));
        player.openInventory(inventory);
    }

    public void openPlayerPicker(Player player, PlayerAction action) {
        pickerActions.put(player.getUniqueId(), action);
        Inventory inventory = Bukkit.createInventory(null, 54, playerPickerTitle(action));

        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) break;
            if (target.equals(player) && action == PlayerAction.FREEZE) continue;
            inventory.setItem(slot++, item(Material.PLAYER_HEAD, "&f" + target.getName(),
                    Arrays.asList("&7Click to " + action.getLabel() + ".")));
        }

        inventory.setItem(49, item(Material.ARROW, "&cBack", Collections.singletonList("&7Return to staff panel.")));
        player.openInventory(inventory);
    }

    public void openGrantRankPicker(Player player, OfflinePlayer target) {
        grantTargets.put(player.getUniqueId(), target.getUniqueId());
        grantTargetNames.put(player.getUniqueId(), safeName(target.getName()));
        grantRanks.remove(player.getUniqueId());
        grantDurations.remove(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(null, 54,
                grantTitle().replace("{player}", safeName(target.getName())));

        Map<Integer, String> templateSlots = new HashMap<>();
        int templateSlot = 1;
        for (GrantTemplate template : plugin.getGrantTemplateManager().getTemplates()) {
            if (templateSlot > 7) break;
            templateSlots.put(templateSlot, template.getId());
            inventory.setItem(templateSlot++, item(material(template.getMaterial(), "GOLD_INGOT"),
                    template.getDisplayName(),
                    Arrays.asList("&7Template: &f" + template.getId(),
                            "&7Rank: &f" + template.getRankName(),
                            "&7Duration: &f" + template.getDuration(),
                            "&7Reason: &f" + template.getReason(),
                            "",
                            "&aClick &7to apply this template.")));
        }

        Map<Integer, String> slots = new HashMap<>();
        int slot = 10;
        for (Rank rank : plugin.getRankManager().getVisibleRanksByWeight()) {
            if (slot > 43) break;
            if ((slot + 1) % 9 == 0) slot += 2;
            slots.put(slot, rank.getName());
            inventory.setItem(slot++, item(Material.NAME_TAG, rank.getDisplayName(),
                    Arrays.asList("&7Weight: &f" + rank.getWeight(),
                            "&7Primary permission count: &f" + rank.getPermissions().size(),
                            "",
                            "&aClick &7to choose duration.")));
        }

        grantRankSlots.put(player.getUniqueId(), slots);
        grantTemplateSlots.put(player.getUniqueId(), templateSlots);
        inventory.setItem(49, item(Material.ARROW, "&cBack", Collections.singletonList("&7Pick another player.")));
        player.openInventory(inventory);
    }

    public void openGrantDurationPicker(Player player) {
        UUID targetUuid = grantTargets.get(player.getUniqueId());
        String rankName = grantRanks.get(player.getUniqueId());
        if (targetUuid == null || rankName == null) {
            openPlayerPicker(player, PlayerAction.GRANT);
            return;
        }

        String targetName = grantTargetNames.getOrDefault(player.getUniqueId(), Bukkit.getOfflinePlayer(targetUuid).getName());
        Inventory inventory = Bukkit.createInventory(null, 27,
                grantDurationTitle().replace("{player}", safeName(targetName)));
        Map<Integer, String> slots = new HashMap<>();
        List<String> durations = grantDurationOptions();
        int[] durationSlots = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < durations.size() && i < durationSlots.length; i++) {
            String duration = durations.get(i);
            int slot = durationSlots[i];
            slots.put(slot, duration);
            inventory.setItem(slot, item(Material.CLOCK, "&c" + duration,
                    Arrays.asList("&7Rank: &f" + rankName,
                            "&7Target: &f" + safeName(targetName),
                            "",
                            "&aClick &7to choose this duration.")));
        }

        grantDurationSlots.put(player.getUniqueId(), slots);
        inventory.setItem(18, item(Material.ARROW, "&cBack", Collections.singletonList("&7Choose another rank.")));
        inventory.setItem(22, item(Material.BARRIER, "&cClose", Collections.singletonList("&7Cancel this grant.")));
        player.openInventory(inventory);
    }

    public void openGrantReasonPicker(Player player) {
        UUID targetUuid = grantTargets.get(player.getUniqueId());
        String rankName = grantRanks.get(player.getUniqueId());
        String duration = grantDurations.get(player.getUniqueId());
        if (targetUuid == null || rankName == null || duration == null) {
            openPlayerPicker(player, PlayerAction.GRANT);
            return;
        }

        String targetName = grantTargetNames.getOrDefault(player.getUniqueId(), Bukkit.getOfflinePlayer(targetUuid).getName());
        Inventory inventory = Bukkit.createInventory(null, 27,
                grantReasonTitle().replace("{player}", safeName(targetName)));
        Map<Integer, String> slots = new HashMap<>();
        List<String> reasons = grantReasonOptions();
        int[] reasonSlots = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < reasons.size() && i < reasonSlots.length; i++) {
            String reason = reasons.get(i);
            int slot = reasonSlots[i];
            slots.put(slot, reason);
            inventory.setItem(slot, item(Material.PAPER, "&c" + reason,
                    Arrays.asList("&7Rank: &f" + rankName,
                            "&7Duration: &f" + duration,
                            "",
                            "&aClick &7to confirm the grant.")));
        }

        grantReasonSlots.put(player.getUniqueId(), slots);
        inventory.setItem(18, item(Material.ARROW, "&cBack", Collections.singletonList("&7Choose another duration.")));
        inventory.setItem(22, item(Material.BARRIER, "&cClose", Collections.singletonList("&7Cancel this grant.")));
        player.openInventory(inventory);
    }

    public void openTagMenu(Player player, int page) {
        List<TagGuiEntry> entries = loadTagEntries();
        int perPage = 28;
        int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) perPage));
        page = Math.max(1, Math.min(page, pages));
        tagPages.put(player.getUniqueId(), page);

        Inventory inventory = Bukkit.createInventory(null, 54,
                tagTitle().replace("{page}", String.valueOf(page)).replace("{pages}", String.valueOf(pages)));
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        String current = profile == null || profile.getTag() == null || profile.getTag().isEmpty() ? "&7None" : profile.getTag();

        inventory.setItem(4, item(Material.NETHER_STAR, "&cYour Tags",
                Arrays.asList("&7Current: " + current,
                        "&7Unlocked: &f" + unlockedTagCount(player, entries) + "&8/&f" + entries.size())));
        inventory.setItem(47, item(Material.ENDER_CHEST, "&dRandom Tag", Collections.singletonList("&7Pick a random unlocked tag.")));
        inventory.setItem(49, item(Material.BARRIER, "&cClear Tag", Collections.singletonList("&7Remove your current tag.")));
        inventory.setItem(51, item(Material.ARROW, "&cClose", Collections.singletonList("&7Close this menu.")));
        if (page > 1) inventory.setItem(45, item(Material.ARROW, "&cPrevious", Collections.singletonList("&7Go to page " + (page - 1) + ".")));
        if (page < pages) inventory.setItem(53, item(Material.ARROW, "&cNext", Collections.singletonList("&7Go to page " + (page + 1) + ".")));

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int from = (page - 1) * perPage;
        Map<Integer, TagGuiEntry> pageSlots = new HashMap<>();
        for (int i = 0; i < slots.length && from + i < entries.size(); i++) {
            TagGuiEntry tag = entries.get(from + i);
            boolean unlocked = canUseTag(player, tag);
            List<String> lore = new ArrayList<>();
            lore.add("&7ID: &f" + tag.id);
            lore.add("&7Category: &f" + tag.category);
            lore.add("&7Rarity: &f" + tag.rarity);
            if (!tag.ranks.isEmpty()) lore.add("&7Rank unlocks: &f" + joinText(tag.ranks));
            if (!tag.description.isEmpty()) {
                lore.add("");
                lore.add("&7" + tag.description);
            }
            lore.add("");
            lore.add(unlocked ? "&aLeft-click &7to equip." : "&cLocked: &f" + requiredTagPermission(tag));
            lore.add("&eRight-click &7to preview.");
            pageSlots.put(slots[i], tag);
            inventory.setItem(slots[i], item(unlocked ? Material.NAME_TAG : Material.INK_SAC,
                    tag.display + (unlocked ? "" : " &8(Locked)"), lore));
        }
        tagSlots.put(player.getUniqueId(), pageSlots);
        player.openInventory(inventory);
    }

    public void openNotes(Player viewer, OfflinePlayer target) {
        Inventory inventory = Bukkit.createInventory(null, 54,
                CC.color(plugin.getConfig().getString("gui.notes.title", "&8Notes: {player}").replace("{player}", target.getName())));
        int slot = 0;
        for (PlayerNote note : plugin.getNoteManager().getNotes(target.getUniqueId())) {
            if (slot >= 45) break;
            inventory.setItem(slot++, item(Material.PAPER, "&c" + note.getId(),
                    Arrays.asList("&7By: &f" + note.getIssuerName(), "&7At: &f" + note.getFormattedTime(), "&7" + note.getNote())));
        }
        inventory.setItem(49, item(Material.ARROW, "&cBack", Collections.singletonList("&7Return to staff panel.")));
        viewer.openInventory(inventory);
    }

    public void openRanks(Player viewer) {
        openRanks(viewer, rankPages.containsKey(viewer.getUniqueId()) ? rankPages.get(viewer.getUniqueId()) : 1);
    }

    private void openRanks(Player viewer, int requestedPage) {
        Inventory inventory = Bukkit.createInventory(null, 54,
                CC.color(plugin.getConfig().getString("gui.ranks.title", "&8Ranks")));
        List<Rank> ranks = plugin.getRankManager().getVisibleRanksByWeight();
        int pageSize = 45;
        int maxPage = Math.max(1, (int) Math.ceil(ranks.size() / (double) pageSize));
        int page = Math.min(maxPage, Math.max(1, requestedPage));
        int start = (page - 1) * pageSize;
        int end = Math.min(ranks.size(), start + pageSize);
        int slot = 0;
        Map<Integer, String> slots = new HashMap<>();
        for (int index = start; index < end; index++) {
            Rank rank = ranks.get(index);
            List<String> lore = new ArrayList<>();
            lore.add("&7Category: &f" + plugin.getRankManager().getRankCategory(rank));
            lore.add("&7Permission: &f" + (rank.getPermission().isEmpty() ? "none" : rank.getPermission()));
            lore.add("&7Weight: &f" + rank.getWeight());
            lore.add("&7Default: &f" + rank.isDefault());
            lore.add("&7Staff: &f" + rank.isStaff());
            lore.add("&7Direct Permissions: &f" + rank.getPermissions().size());
            lore.add("&7All Permissions: &f" + plugin.getRankManager().getAllPermissions(rank).size());
            lore.add("&7Inheritance: &f" + (rank.getInheritance().isEmpty() ? "none" : joinText(rank.getInheritance())));
            lore.add("");
            lore.add("&aClick &7to edit.");
            slots.put(slot, rank.getName());
            inventory.setItem(slot++, item(Material.NAME_TAG, rank.getDisplayName(), lore));
        }
        rankSlots.put(viewer.getUniqueId(), slots);
        rankPages.put(viewer.getUniqueId(), page);
        if (page > 1) {
            inventory.setItem(45, item(Material.ARROW, "&ePrevious Page",
                    Collections.singletonList("&7Page &f" + (page - 1) + "&7/&f" + maxPage)));
        }
        if (page < maxPage) {
            inventory.setItem(53, item(Material.ARROW, "&eNext Page",
                    Collections.singletonList("&7Page &f" + (page + 1) + "&7/&f" + maxPage)));
        }
        inventory.setItem(49, item(Material.ARROW, "&cBack",
                Arrays.asList("&7Return to staff panel.",
                        "&7Page: &f" + page + "&7/&f" + maxPage,
                        "&7Visible ranks: &f" + ranks.size())));
        viewer.openInventory(inventory);
    }

    public void openRankEditor(Player viewer, Rank rank) {
        editingRanks.put(viewer.getUniqueId(), rank.getName());
        Inventory inventory = Bukkit.createInventory(null, 27, rankEditorTitle().replace("{rank}", rank.getName()));
        inventory.setItem(4, item(Material.NAME_TAG, rank.getDisplayName(),
                Arrays.asList("&7Display: &f" + (rank.getDisplay().isEmpty() ? "none" : rank.getDisplay()),
                        "&7Category: &f" + plugin.getRankManager().getRankCategory(rank),
                        "&7Permission: &f" + (rank.getPermission().isEmpty() ? "none" : rank.getPermission()),
                        "&7Weight: &f" + rank.getWeight(),
                        "&7Color: &f" + rank.getColor(),
                        "&7Permissions: &f" + rank.getPermissions().size())));
        inventory.setItem(10, item(Material.NETHER_STAR, "&cDefault",
                Collections.singletonList("&7Current: &f" + rank.isDefault())));
        inventory.setItem(11, item(Material.IRON_SWORD, "&cStaff",
                Collections.singletonList("&7Current: &f" + rank.isStaff())));
        inventory.setItem(12, item(Material.INK_SAC, "&cHidden",
                Collections.singletonList("&7Current: &f" + rank.isHidden())));
        inventory.setItem(13, item(Material.PAPER, "&cCategory",
                Arrays.asList("&7Current: &f" + plugin.getRankManager().getRankCategory(rank),
                        "&7Choose Staff, Media, Store, Hidden, or Default.")));
        inventory.setItem(14, item(Material.BOOK, "&cBackup Ranks",
                Collections.singletonList("&7Creates a rank backup file.")));
        inventory.setItem(15, item(Material.TNT, "&cReset Presets",
                Collections.singletonList("&7Open destructive reset confirmation.")));
        inventory.setItem(22, item(Material.ARROW, "&cBack", Collections.singletonList("&7Return to ranks.")));
        viewer.openInventory(inventory);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        if (title.equals(staffTitle())) {
            event.setCancelled(true);
            handleStaffPanelClick(player, event.getSlot());
            return;
        }

        if (title.startsWith(playerPickerPrefix())) {
            event.setCancelled(true);
            handlePlayerPickerClick(player, event);
            return;
        }

        if (title.startsWith(grantTitlePrefix())) {
            event.setCancelled(true);
            handleGrantRankClick(player, event);
            return;
        }

        if (title.startsWith(grantDurationTitlePrefix())) {
            event.setCancelled(true);
            handleGrantDurationClick(player, event);
            return;
        }

        if (title.startsWith(grantReasonTitlePrefix())) {
            event.setCancelled(true);
            handleGrantReasonClick(player, event);
            return;
        }

        if (title.startsWith(tagTitlePrefix())) {
            event.setCancelled(true);
            handleTagClick(player, event);
            return;
        }

        if (title.equals(lobbyProtectionTitle())) {
            event.setCancelled(true);
            handleLobbyProtectionClick(player, event.getSlot());
            return;
        }

        if (title.equals(rankPresetConfirmTitle())) {
            event.setCancelled(true);
            handleRankPresetConfirmClick(player, event.getSlot());
            return;
        }

        if (title.equals(staffDashboardTitle())) {
            event.setCancelled(true);
            handleStaffDashboardClick(player, event.getSlot());
            return;
        }

        if (title.equals(maintenanceTitle())) {
            event.setCancelled(true);
            handleMaintenanceClick(player, event.getSlot());
            return;
        }

        if (title.equals(staffSessionsTitle())) {
            event.setCancelled(true);
            handleStaffSessionsClick(player, event.getSlot());
            return;
        }

        if (title.equals(pendingGrantsTitle())) {
            event.setCancelled(true);
            handlePendingGrantClick(player, event);
            return;
        }

        if (title.startsWith(playerProfileTitlePrefix())) {
            event.setCancelled(true);
            handleProfileClick(player, event.getSlot());
            return;
        }

        if (title.equals(reportsTitle()) || title.equals(helpOpTitle())) {
            event.setCancelled(true);
            handleReportsClick(player, event);
            return;
        }

        if (title.startsWith(CC.color(plugin.getConfig().getString("gui.punish.title-prefix", "&8Punish: ")))) {
            event.setCancelled(true);
            handlePunishClick(player, event.getSlot());
            return;
        }

        if (title.startsWith(CC.color(plugin.getConfig().getString("gui.notes.title-prefix", "&8Notes: ")))) {
            event.setCancelled(true);
            if (event.getSlot() == 49) openStaffPanel(player);
            return;
        }

        if (title.equals(CC.color(plugin.getConfig().getString("gui.ranks.title", "&8Ranks")))) {
            event.setCancelled(true);
            handleRanksClick(player, event.getSlot());
            return;
        }

        if (title.startsWith(rankCategoryTitlePrefix())) {
            event.setCancelled(true);
            handleRankCategoryClick(player, event.getSlot());
            return;
        }

        if (title.startsWith(rankEditorTitlePrefix())) {
            event.setCancelled(true);
            handleRankEditorClick(player, event.getSlot());
        }
    }

    private void handleStaffPanelClick(Player player, int slot) {
        if (slot == slotFromConfig("gui.staff-panel.items.reports.slot", 10)) openReports(player, false);
        else if (slot == slotFromConfig("gui.staff-panel.items.helpop.slot", 11)) openReports(player, true);
        else if (slot == slotFromConfig("gui.staff-panel.items.punish.slot", 12)) openPlayerPicker(player, PlayerAction.PUNISH);
        else if (slot == slotFromConfig("gui.staff-panel.items.freeze.slot", 13)) openPlayerPicker(player, PlayerAction.FREEZE);
        else if (slot == slotFromConfig("gui.staff-panel.items.inspect.slot", 14)) openPlayerPicker(player, PlayerAction.INSPECT);
        else if (slot == slotFromConfig("gui.staff-panel.items.notes.slot", 15)) openPlayerPicker(player, PlayerAction.NOTES);
        else if (slot == slotFromConfig("gui.staff-panel.items.modlogs.slot", 16)) openPlayerPicker(player, PlayerAction.MODLOGS);
        else if (slot == slotFromConfig("gui.staff-panel.items.grants.slot", 20)) openPlayerPicker(player, PlayerAction.GRANT);
        else if (slot == slotFromConfig("gui.staff-panel.items.vanish.slot", 21)) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "vanish");
        } else if (slot == slotFromConfig("gui.staff-panel.items.staffmode.slot", 22)) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "staffmode");
        } else if (slot == slotFromConfig("gui.staff-panel.items.ranks.slot", 23)) openRanks(player);
        else if (slot == slotFromConfig("gui.staff-panel.items.lobby-protection.slot", 24)) openLobbyProtection(player);
        else if (slot == slotFromConfig("gui.staff-panel.items.dashboard.slot", 25)) openStaffDashboard(player);
        else if (slot == slotFromConfig("gui.staff-panel.items.maintenance.slot", 29)) openMaintenance(player);
        else if (slot == slotFromConfig("gui.staff-panel.items.sessions.slot", 30)) openStaffSessions(player);
        else if (slot == slotFromConfig("gui.staff-panel.items.close.slot", 31)) player.closeInventory();
    }

    private void handleLobbyProtectionClick(Player player, int slot) {
        String world = player.getWorld().getName();
        switch (slot) {
            case 10:
                toggleConfig("lobby-protection.enabled", true);
                player.sendMessage(CC.color("&7Lobby protection: " + enabledText(plugin.getConfig().getBoolean("lobby-protection.enabled", true))));
                openLobbyProtection(player);
                break;
            case 11:
                enforceLobbyProtection(world);
                player.sendMessage(CC.color("&aLobby protection enforced for &f" + world + "&a."));
                openLobbyProtection(player);
                break;
            case 12:
                addProtectedWorld(world);
                plugin.saveConfig();
                player.sendMessage(CC.color("&aProtected lobby world: &f" + world));
                openLobbyProtection(player);
                break;
            case 13:
                removeProtectedWorld(world);
                plugin.saveConfig();
                player.sendMessage(CC.color("&cUnprotected lobby world: &f" + world));
                openLobbyProtection(player);
                break;
            case 14:
                plugin.getConfig().set("lobby-protection.worlds", new ArrayList<String>());
                plugin.saveConfig();
                player.sendMessage(CC.color("&aLobby protection now applies to every loaded world."));
                openLobbyProtection(player);
                break;
            case 15:
                toggleConfig("lobby-protection.notify-on-deny", true);
                openLobbyProtection(player);
                break;
            case 16:
                player.closeInventory();
                Bukkit.dispatchCommand(player, "buildmode");
                break;
            case 20:
                toggleConfig("lobby-protection.prevent-damage", true);
                openLobbyProtection(player);
                break;
            case 21:
                toggleConfig("lobby-protection.prevent-explosions", true);
                openLobbyProtection(player);
                break;
            case 22:
                toggleConfig("lobby-protection.no-mobs", true);
                openLobbyProtection(player);
                break;
            case 23:
                toggleConfig("lobby-protection.prevent-item-drops", true);
                plugin.getConfig().set("lobby-protection.prevent-item-pickup",
                        plugin.getConfig().getBoolean("lobby-protection.prevent-item-drops", true));
                plugin.saveConfig();
                openLobbyProtection(player);
                break;
            case 24:
                toggleConfig("lobby-protection.prevent-block-interact", true);
                plugin.getConfig().set("lobby-protection.prevent-entity-interact",
                        plugin.getConfig().getBoolean("lobby-protection.prevent-block-interact", true));
                plugin.getConfig().set("lobby-protection.prevent-dangerous-item-use",
                        plugin.getConfig().getBoolean("lobby-protection.prevent-block-interact", true));
                plugin.saveConfig();
                openLobbyProtection(player);
                break;
            case 25:
                toggleConfig("lobby-protection.prevent-liquid-flow", true);
                plugin.getConfig().set("lobby-protection.prevent-growth",
                        plugin.getConfig().getBoolean("lobby-protection.prevent-liquid-flow", true));
                plugin.getConfig().set("lobby-protection.prevent-spread",
                        plugin.getConfig().getBoolean("lobby-protection.prevent-liquid-flow", true));
                plugin.saveConfig();
                openLobbyProtection(player);
                break;
            case 26:
                toggleConfig("lobby-protection.void-rescue.enabled", true);
                openLobbyProtection(player);
                break;
            case 31:
                player.closeInventory();
                Bukkit.dispatchCommand(player, "lobbyprotect status");
                break;
            case 40:
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    private void handleRankPresetConfirmClick(Player player, int slot) {
        if (slot == 11) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "rank presets confirm");
            return;
        }
        if (slot == 15) {
            openRanks(player);
        }
    }

    private void handleStaffDashboardClick(Player player, int slot) {
        if (slot == 10) {
            openReports(player, false);
            return;
        }
        if (slot == 11) {
            openReports(player, true);
            return;
        }
        if (slot == 12) {
            openPendingGrants(player);
            return;
        }
        if (slot == 15) {
            openStaffSessions(player);
            return;
        }
        if (slot == 16) {
            openMaintenance(player);
            return;
        }
        if (slot == 49) {
            openStaffPanel(player);
        }
    }

    private void handleMaintenanceClick(Player player, int slot) {
        if (slot == 10) {
            player.closeInventory();
            boolean enabled = plugin.getConfig().getBoolean("maintenance.enabled", false);
            Bukkit.dispatchCommand(player, enabled ? "maintenance off" : "maintenance on GUI toggle");
            return;
        }
        if (slot == 12) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "maintenance status");
            return;
        }
        if (slot == 16) {
            openStaffSessions(player);
            return;
        }
        if (slot == 22) {
            openStaffDashboard(player);
        }
    }

    private void handleStaffSessionsClick(Player player, int slot) {
        if (slot == 49) openStaffDashboard(player);
    }

    private void handlePendingGrantClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 49) {
            openStaffDashboard(player);
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String id = CC.strip(item.getItemMeta().getDisplayName()).trim();
        if (id.isEmpty()) return;
        player.closeInventory();
        if (event.isRightClick()) Bukkit.dispatchCommand(player, "grant deny " + id + " Denied from GUI");
        else Bukkit.dispatchCommand(player, "grant approve " + id);
    }

    private void handleProfileClick(Player player, int slot) {
        UUID uuid = profileTargets.get(player.getUniqueId());
        if (uuid == null) {
            player.closeInventory();
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
        if (slot == 28) {
            openGrantRankPicker(player, target);
            return;
        }
        if (slot == 29) {
            openNotes(player, target);
            return;
        }
        if (slot == 30) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "modlogs " + safeName(target.getName()));
            return;
        }
        if (slot == 49) {
            player.closeInventory();
        }
    }

    private void handleRanksClick(Player player, int slot) {
        int page = rankPages.containsKey(player.getUniqueId()) ? rankPages.get(player.getUniqueId()) : 1;
        int maxPage = Math.max(1, (int) Math.ceil(plugin.getRankManager().getVisibleRanksByWeight().size() / 45.0));
        if (slot == 45 && page > 1) {
            openRanks(player, page - 1);
            return;
        }
        if (slot == 53 && page < maxPage) {
            openRanks(player, page + 1);
            return;
        }
        if (slot == 49) {
            openStaffPanel(player);
            return;
        }
        Map<Integer, String> slots = rankSlots.get(player.getUniqueId());
        if (slots == null || !slots.containsKey(slot)) return;
        Rank rank = plugin.getRankManager().getRank(slots.get(slot));
        if (rank != null) openRankEditor(player, rank);
    }

    private void handleRankEditorClick(Player player, int slot) {
        String rankName = editingRanks.get(player.getUniqueId());
        Rank rank = rankName == null ? null : plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            openRanks(player);
            return;
        }

        if (slot == 10) {
            for (Rank other : plugin.getRankManager().getRanks()) {
                other.setDefault(other.getName().equalsIgnoreCase(rank.getName()));
                plugin.getRankManager().saveRank(other);
            }
            openRankEditor(player, rank);
            return;
        }
        if (slot == 11) {
            rank.setStaff(!rank.isStaff());
            if (rank.isStaff()) rank.setCategory("Staff");
            else if (rank.getCategory().equalsIgnoreCase("Staff")) rank.setCategory(Rank.inferCategory(rank));
            plugin.getRankManager().saveRank(rank);
            openRankEditor(player, rank);
            return;
        }
        if (slot == 12) {
            rank.setHidden(!rank.isHidden());
            if (rank.isHidden()) rank.setCategory("Hidden");
            else if (rank.getCategory().equalsIgnoreCase("Hidden")) rank.setCategory(Rank.inferCategory(rank));
            plugin.getRankManager().saveRank(rank);
            openRankEditor(player, rank);
            return;
        }
        if (slot == 13) {
            openRankCategoryEditor(player, rank);
            return;
        }
        if (slot == 14) {
            plugin.getRankManager().backupRanks("gui");
            player.sendMessage(CC.color("&aCreated a rank backup."));
            openRankEditor(player, rank);
            return;
        }
        if (slot == 15) {
            openRankPresetConfirm(player);
            return;
        }
        if (slot == 22) {
            openRanks(player);
        }
    }

    private void openRankCategoryEditor(Player viewer, Rank rank) {
        editingRanks.put(viewer.getUniqueId(), rank.getName());
        Inventory inventory = Bukkit.createInventory(null, 27, rankCategoryTitle().replace("{rank}", rank.getName()));
        Map<Integer, String> slots = new HashMap<>();
        addRankCategoryItem(inventory, slots, 10, "Staff", Material.IRON_SWORD, "&cStaff", rank);
        addRankCategoryItem(inventory, slots, 11, "Media", Material.REDSTONE_TORCH, "&cMedia", rank);
        addRankCategoryItem(inventory, slots, 12, "Store", Material.EMERALD, "&cStore", rank);
        addRankCategoryItem(inventory, slots, 13, "Hidden", Material.INK_SAC, "&cHidden", rank);
        addRankCategoryItem(inventory, slots, 14, "Default", Material.PAPER, "&cDefault", rank);
        inventory.setItem(22, item(Material.ARROW, "&cBack", Collections.singletonList("&7Return to rank editor.")));
        rankCategorySlots.put(viewer.getUniqueId(), slots);
        viewer.openInventory(inventory);
    }

    private void addRankCategoryItem(Inventory inventory, Map<Integer, String> slots, int slot, String category,
                                     Material material, String name, Rank rank) {
        boolean selected = plugin.getRankManager().getRankCategory(rank).equalsIgnoreCase(category);
        inventory.setItem(slot, item(material, name, Arrays.asList(
                "&7Current: &f" + (selected ? "yes" : "no"),
                "&aClick &7to assign this category.")));
        slots.put(slot, category);
    }

    private void handleRankCategoryClick(Player player, int slot) {
        String rankName = editingRanks.get(player.getUniqueId());
        Rank rank = rankName == null ? null : plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            openRanks(player);
            return;
        }
        if (slot == 22) {
            openRankEditor(player, rank);
            return;
        }

        Map<Integer, String> slots = rankCategorySlots.get(player.getUniqueId());
        if (slots == null || !slots.containsKey(slot)) return;
        applyRankCategory(rank, slots.get(slot));
        plugin.getRankManager().saveRank(rank);
        player.sendMessage(CC.color("&aSet &f" + rank.getName() + " &acategory to &f" + rank.getCategory() + "&a."));
        openRankEditor(player, rank);
    }

    private void handleReportsClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 49) {
            openStaffPanel(player);
            return;
        }

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
        if (slot == 22) {
            player.closeInventory();
            return;
        }

        UUID targetUuid = punishTargets.get(player.getUniqueId());
        if (targetUuid == null) {
            player.closeInventory();
            return;
        }

        List<Preset> presets = plugin.getPunishmentPresetManager().getPresets();
        int index = slot - 10;
        if (index < 0 || index >= presets.size()) return;

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        Preset preset = presets.get(index);
        player.closeInventory();
        plugin.getPunishmentPresetManager().executePreset(player, target, preset.getKey(), null);
    }

    private void handlePlayerPickerClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 49) {
            openStaffPanel(player);
            return;
        }

        PlayerAction action = pickerActions.get(player.getUniqueId());
        ItemStack item = event.getCurrentItem();
        if (action == null || item == null || !item.hasItemMeta()) return;

        String targetName = CC.strip(item.getItemMeta().getDisplayName());
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(CC.color("&cThat player is no longer online."));
            openPlayerPicker(player, action);
            return;
        }

        switch (action) {
            case PUNISH:
                openPunishMenu(player, target);
                break;
            case FREEZE:
                player.closeInventory();
                Bukkit.dispatchCommand(player, "freeze " + target.getName() + " Staff panel");
                break;
            case INSPECT:
                player.closeInventory();
                Bukkit.dispatchCommand(player, "invsee " + target.getName());
                break;
            case NOTES:
                openNotes(player, target);
                break;
            case MODLOGS:
                player.closeInventory();
                Bukkit.dispatchCommand(player, "modlogs " + target.getName());
                break;
            case GRANT:
                openGrantRankPicker(player, target);
                break;
            default:
                break;
        }
    }

    private void handleGrantRankClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 49) {
            openPlayerPicker(player, PlayerAction.GRANT);
            return;
        }

        Map<Integer, String> templates = grantTemplateSlots.get(player.getUniqueId());
        if (templates != null && templates.containsKey(event.getSlot())) {
            String targetName = grantTargetNames.get(player.getUniqueId());
            if (targetName == null) {
                player.closeInventory();
                player.sendMessage(CC.color("&cGrant session expired. Run /grant again."));
                return;
            }
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
        if (event.getSlot() == 18) {
            UUID targetUuid = grantTargets.get(player.getUniqueId());
            if (targetUuid == null) openPlayerPicker(player, PlayerAction.GRANT);
            else openGrantRankPicker(player, Bukkit.getOfflinePlayer(targetUuid));
            return;
        }
        if (event.getSlot() == 22) {
            player.closeInventory();
            return;
        }

        Map<Integer, String> slots = grantDurationSlots.get(player.getUniqueId());
        if (slots == null || !slots.containsKey(event.getSlot())) return;
        grantDurations.put(player.getUniqueId(), slots.get(event.getSlot()));
        openGrantReasonPicker(player);
    }

    private void handleGrantReasonClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 18) {
            openGrantDurationPicker(player);
            return;
        }
        if (event.getSlot() == 22) {
            player.closeInventory();
            return;
        }

        Map<Integer, String> slots = grantReasonSlots.get(player.getUniqueId());
        if (slots == null || !slots.containsKey(event.getSlot())) return;

        String targetName = grantTargetNames.get(player.getUniqueId());
        String rankName = grantRanks.get(player.getUniqueId());
        String duration = grantDurations.get(player.getUniqueId());
        String reason = slots.get(event.getSlot());
        if (targetName == null || rankName == null || duration == null || reason == null) {
            player.closeInventory();
            player.sendMessage(CC.color("&cGrant session expired. Run /grant again."));
            return;
        }

        player.closeInventory();
        Bukkit.dispatchCommand(player, "grant " + targetName + " " + rankName + " " + duration + " " + reason);
    }

    private void handleTagClick(Player player, InventoryClickEvent event) {
        int page = tagPages.getOrDefault(player.getUniqueId(), 1);
        if (event.getSlot() == 45) {
            openTagMenu(player, page - 1);
            return;
        }
        if (event.getSlot() == 53) {
            openTagMenu(player, page + 1);
            return;
        }
        if (event.getSlot() == 51) {
            player.closeInventory();
            return;
        }
        if (event.getSlot() == 49) {
            setPlayerTag(player, "");
            openTagMenu(player, page);
            return;
        }
        if (event.getSlot() == 47) {
            List<TagGuiEntry> unlocked = new ArrayList<>();
            for (TagGuiEntry tag : loadTagEntries()) {
                if (canUseTag(player, tag)) unlocked.add(tag);
            }
            if (unlocked.isEmpty()) {
                player.sendMessage(CC.color("&cYou do not have any unlocked tags."));
                return;
            }
            TagGuiEntry picked = unlocked.get(new Random().nextInt(unlocked.size()));
            setPlayerTag(player, picked.display);
            openTagMenu(player, page);
            return;
        }

        Map<Integer, TagGuiEntry> slots = tagSlots.get(player.getUniqueId());
        if (slots == null || !slots.containsKey(event.getSlot())) return;
        TagGuiEntry tag = slots.get(event.getSlot());
        if (event.isRightClick()) {
            player.sendMessage(CC.color("&7Preview: " + tag.display + " &f" + plugin.getDisguiseManager().getVisibleName(player)
                    + "&7: &fHello!"));
            return;
        }
        if (!canUseTag(player, tag)) {
            player.sendMessage(CC.color("&cThat tag is locked. Required permission: &f" + requiredTagPermission(tag)));
            return;
        }
        setPlayerTag(player, tag.display);
        openTagMenu(player, page);
    }

    private List<String> grantDurationOptions() {
        List<String> durations = plugin.getConfig().getStringList("gui.grants.duration-options");
        if (durations.isEmpty()) durations = Arrays.asList("perm", "1h", "1d", "7d", "30d", "90d");
        return durations;
    }

    private List<String> grantReasonOptions() {
        List<String> reasons = plugin.getConfig().getStringList("gui.grants.reason-options");
        if (reasons.isEmpty()) {
            reasons = Arrays.asList("Promotion", "Staff trial", "Donor package", "Event reward", "Correction", "Manual grant");
        }
        return reasons;
    }

    private void setPlayerTag(Player player, String tag) {
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        if (profile == null) {
            player.sendMessage(CC.color("&cProfile not loaded."));
            return;
        }

        profile.setTag(tag);
        plugin.getPlayerManager().saveProfile(profile);
        plugin.getNameTagManager().applyNameTag(player);
        player.sendMessage(CC.color(tag == null || tag.isEmpty()
                ? "&7Your tag was cleared."
                : "&7Your tag is now " + tag + "&7."));
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

        tags.sort(Comparator.comparingInt(tag -> tag.order));
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
        for (TagGuiEntry tag : tags) {
            if (canUseTag(player, tag)) count++;
        }
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
                for (String extra : profile.getExtraRanks()) {
                    if (extra.equalsIgnoreCase(rank)) return true;
                }
            }
        }
        return tag.permission.isEmpty() && tag.ranks.isEmpty();
    }

    private String requiredTagPermission(TagGuiEntry tag) {
        return tag.permission.isEmpty() ? "evaulx.tag.all" : tag.permission;
    }

    private String normalizeTag(String input) {
        return DisplayUtil.stripFormat(input)
                .toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9]+", "");
    }

    private String safeName(String name) {
        return name == null || name.trim().isEmpty() ? "Unknown" : name;
    }

    private void applyRankCategory(Rank rank, String category) {
        String normalized = Rank.normalizeCategory(category);
        rank.setCategory(normalized);
        rank.setHidden(normalized.equals("Hidden"));
        rank.setStaff(normalized.equals("Staff"));
    }

    private ItemStack toggleItem(Material material, String label, String setting) {
        boolean enabled = plugin.getConfig().getBoolean("lobby-protection." + setting, true);
        return item(material, (enabled ? "&a" : "&c") + label,
                Arrays.asList("&7Current: " + enabledText(enabled),
                        "&aClick &7to toggle."));
    }

    private void toggleConfig(String path, boolean fallback) {
        plugin.getConfig().set(path, !plugin.getConfig().getBoolean(path, fallback));
        plugin.saveConfig();
    }

    private void enforceLobbyProtection(String world) {
        plugin.getConfig().set("lobby-protection.enabled", true);
        for (String setting : lobbyEnforcedSettings()) {
            plugin.getConfig().set("lobby-protection." + setting, true);
        }
        addProtectedWorld(world);
        plugin.saveConfig();
    }

    private List<String> lobbyEnforcedSettings() {
        return Arrays.asList(
                "prevent-physical-interact",
                "prevent-block-interact",
                "prevent-dangerous-item-use",
                "prevent-entity-interact",
                "prevent-pistons",
                "prevent-fire",
                "prevent-spread",
                "prevent-liquid-flow",
                "prevent-growth",
                "prevent-block-form",
                "prevent-block-fade",
                "prevent-leaf-decay",
                "prevent-explosions",
                "prevent-entity-block-change",
                "no-mobs",
                "no-mob-targeting",
                "prevent-damage",
                "prevent-projectiles",
                "prevent-item-drops",
                "prevent-item-pickup",
                "prevent-hunger",
                "prevent-weather",
                "notify-on-deny",
                "void-rescue.enabled"
        );
    }

    private void addProtectedWorld(String world) {
        List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
        for (String configured : worlds) {
            if (configured.equalsIgnoreCase(world)) return;
        }
        worlds.add(world);
        plugin.getConfig().set("lobby-protection.worlds", worlds);
    }

    private void removeProtectedWorld(String world) {
        List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
        List<String> updated = new ArrayList<>();
        for (String configured : worlds) {
            if (!configured.equalsIgnoreCase(world)) updated.add(configured);
        }
        plugin.getConfig().set("lobby-protection.worlds", updated);
    }

    private boolean isProtectedWorld(String world) {
        if (!plugin.getConfig().getBoolean("lobby-protection.enabled", true)) return false;
        List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
        if (worlds.isEmpty()) return true;
        for (String configured : worlds) {
            if (configured.equalsIgnoreCase(world)) return true;
        }
        return false;
    }

    private String protectionMode() {
        List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
        return worlds.isEmpty() ? "All loaded worlds" : joinText(worlds);
    }

    private String joinText(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(value);
        }
        return builder.toString();
    }

    private String enabledText(boolean enabled) {
        return enabled ? "&aenabled" : "&cdisabled";
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) coloredLore.add(CC.color(line));
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    private Material materialFromConfig(String configPath, String fallback) {
        return material(plugin.getConfig().getString(configPath, fallback), fallback);
    }

    private Material material(String raw, String fallback) {
        Material material = Material.matchMaterial(raw == null ? fallback : raw);
        if (material != null) return material;
        material = Material.matchMaterial(fallback);
        return material == null ? Material.PAPER : material;
    }

    private String configName(String path, String fallback) {
        return plugin.getConfig().getString(path, fallback);
    }

    private int slotFromConfig(String path, int fallback) {
        int slot = plugin.getConfig().getInt(path, fallback);
        return Math.max(0, Math.min(35, slot));
    }

    private String staffTitle() {
        return CC.color(plugin.getConfig().getString("gui.staff-panel.title", "&8Staff Panel"));
    }

    private String reportsTitle() {
        return CC.color(plugin.getConfig().getString("gui.reports.title", "&8Reports"));
    }

    private String helpOpTitle() {
        return CC.color(plugin.getConfig().getString("gui.helpop.title", "&8HelpOP"));
    }

    private String punishTitle() {
        return CC.color(plugin.getConfig().getString("gui.punish.title", "&8Punish: {player}"));
    }

    private String playerPickerTitle(PlayerAction action) {
        return playerPickerPrefix() + action.getLabel();
    }

    private String playerPickerPrefix() {
        return CC.color(plugin.getConfig().getString("gui.player-picker.title-prefix", "&8Pick: "));
    }

    private String grantTitle() {
        return CC.color(plugin.getConfig().getString("gui.grants.title", "&8Grant: {player}"));
    }

    private String grantTitlePrefix() {
        return CC.color(plugin.getConfig().getString("gui.grants.title-prefix", "&8Grant: "));
    }

    private String grantDurationTitle() {
        return CC.color(plugin.getConfig().getString("gui.grants.duration-title", "&8Grant Time: {player}"));
    }

    private String grantDurationTitlePrefix() {
        return CC.color(plugin.getConfig().getString("gui.grants.duration-title-prefix", "&8Grant Time: "));
    }

    private String grantReasonTitle() {
        return CC.color(plugin.getConfig().getString("gui.grants.reason-title", "&8Grant Reason: {player}"));
    }

    private String grantReasonTitlePrefix() {
        return CC.color(plugin.getConfig().getString("gui.grants.reason-title-prefix", "&8Grant Reason: "));
    }

    private String tagTitle() {
        return CC.color(plugin.getConfig().getString("tags.gui.title", "&8Tags {page}/{pages}"));
    }

    private String tagTitlePrefix() {
        String raw = plugin.getConfig().getString("tags.gui.title", "&8Tags {page}/{pages}");
        int tokenIndex = raw.indexOf("{page}");
        if (tokenIndex >= 0) raw = raw.substring(0, tokenIndex);
        return CC.color(raw);
    }

    private String lobbyProtectionTitle() {
        return CC.color(plugin.getConfig().getString("gui.lobby-protection.title", "&8Lobby Protection"));
    }

    private String rankPresetConfirmTitle() {
        return CC.color(plugin.getConfig().getString("gui.rank-preset-confirm.title", "&8Reset Ranks?"));
    }

    private String rankEditorTitle() {
        return CC.color(plugin.getConfig().getString("gui.rank-editor.title", "&8Rank: {rank}"));
    }

    private String rankEditorTitlePrefix() {
        String raw = plugin.getConfig().getString("gui.rank-editor.title", "&8Rank: {rank}");
        int index = raw.indexOf("{rank}");
        if (index >= 0) raw = raw.substring(0, index);
        return CC.color(raw);
    }

    private String rankCategoryTitle() {
        return CC.color(plugin.getConfig().getString("gui.rank-category.title", "&8Rank Category: {rank}"));
    }

    private String rankCategoryTitlePrefix() {
        String raw = plugin.getConfig().getString("gui.rank-category.title", "&8Rank Category: {rank}");
        int index = raw.indexOf("{rank}");
        if (index >= 0) raw = raw.substring(0, index);
        return CC.color(raw);
    }

    private String staffDashboardTitle() {
        return CC.color(plugin.getConfig().getString("gui.staff-dashboard.title", "&8Staff Dashboard"));
    }

    private String maintenanceTitle() {
        return CC.color(plugin.getConfig().getString("gui.maintenance.title", "&8Maintenance"));
    }

    private String staffSessionsTitle() {
        return CC.color(plugin.getConfig().getString("gui.staff-sessions.title", "&8Staff Sessions"));
    }

    private String pendingGrantsTitle() {
        return CC.color(plugin.getConfig().getString("gui.pending-grants.title", "&8Pending Grants"));
    }

    private String playerProfileTitle() {
        return CC.color(plugin.getConfig().getString("gui.player-profile.title", "&8Profile: {player}"));
    }

    private String playerProfileTitlePrefix() {
        String raw = plugin.getConfig().getString("gui.player-profile.title", "&8Profile: {player}");
        int index = raw.indexOf("{player}");
        if (index >= 0) raw = raw.substring(0, index);
        return CC.color(raw);
    }

    private int onlineStaffCount() {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getStaffRequestManager().canReceiveStaffAlerts(player)) count++;
        }
        return count;
    }

    public enum PlayerAction {
        PUNISH("Punish"),
        FREEZE("Freeze"),
        INSPECT("Inspect"),
        NOTES("Notes"),
        MODLOGS("ModLogs"),
        GRANT("Grant");

        private final String label;

        PlayerAction(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static class TagGuiEntry {
        private final String id;
        private final String display;
        private final String category;
        private final String rarity;
        private final String description;
        private final String permission;
        private final List<String> ranks;
        private final int order;

        private TagGuiEntry(String id, String display, String category, String rarity, String description, String permission, List<String> ranks, int order) {
            this.id = id;
            this.display = display;
            this.category = category == null ? "" : category;
            this.rarity = rarity == null ? "" : rarity;
            this.description = description == null ? "" : description;
            this.permission = permission == null ? "" : permission;
            this.ranks = ranks == null ? Collections.<String>emptyList() : ranks;
            this.order = order;
        }
    }
}
