package dev.evaulx.core.staff;

import com.google.gson.*;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class StaffRequestManager {

    private static final int DEFAULT_MAX_RECENT = 50;
    private static final int DEFAULT_MAX_ACTIONS = 200;

    private final EvaulxCore plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Deque<StaffRequest> reports = new ArrayDeque<>();
    private final Deque<StaffRequest> helpOps = new ArrayDeque<>();
    private final Deque<StaffAction> actions = new ArrayDeque<>();
    private final Map<UUID, Long> reportCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> helpOpCooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> staffChatToggles = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final Set<UUID> commandSpyToggles = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final Map<UUID, FreezeInfo> frozenPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack[]> savedInventories = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new ConcurrentHashMap<>();
    private final Map<UUID, GameMode> savedGameModes = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> savedAllowFlight = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> savedFlying = new ConcurrentHashMap<>();
    private final Set<UUID> staffModeAutoVanished = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final Map<UUID, StaffSession> activeStaffSessions = new ConcurrentHashMap<>();
    private final Deque<StaffSession> staffSessions = new ArrayDeque<>();
    private final AtomicInteger requestIds = new AtomicInteger();

    private File staffDir;
    private File requestsFile;
    private File actionsFile;
    private File actionLogFile;
    private File actionJsonLogFile;
    private File frozenFile;
    private File sessionsFile;
    private File recoveryDir;

    public StaffRequestManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        staffDir = new File(plugin.getDataFolder(), "staff");
        if (!staffDir.exists()) staffDir.mkdirs();

        requestsFile = new File(staffDir, "requests.json");
        actionsFile = new File(staffDir, "actions.json");
        actionLogFile = new File(staffDir, "actions.log");
        actionJsonLogFile = new File(staffDir, "actions.jsonl");
        frozenFile = new File(staffDir, "frozen.json");
        sessionsFile = new File(staffDir, "sessions.json");
        recoveryDir = new File(staffDir, "recovery");
        if (!recoveryDir.exists()) recoveryDir.mkdirs();

        loadRequests();
        loadActions();
        loadFrozen();
        loadSessions();
    }

    public void shutdown() {
        finishActiveSessions();
        saveRequests();
        saveActions();
        saveFrozen();
        saveSessions();
        restoreAllStaffModeItems();
    }

    public StaffRequest submitReport(Player reporter, String target, String reason) {
        StaffRequest request = new StaffRequest(
                requestIds.incrementAndGet(),
                StaffRequest.Type.REPORT,
                reporter.getName(),
                target,
                reason,
                System.currentTimeMillis()
        );

        addRequest(reports, request);
        reportCooldowns.put(reporter.getUniqueId(), System.currentTimeMillis());
        saveRequests();

        String staffMessage = "&8[&cReport #" + request.getId() + "&8] &f" + reporter.getName()
                + " &7reported &f" + target + " &8- &7" + reason;
        broadcastStaffRequest(request, staffMessage, "evaulx.reports");
        if (plugin.getRedisSyncManager() != null) plugin.getRedisSyncManager().publishStaffNotice("REPORT", staffMessage);
        Bukkit.getConsoleSender().sendMessage(CC.color(staffMessage));
        plugin.getDiscordManager().sendReport(reporter.getName(), target, reason);
        logAction(reporter.getName(), "REPORT", target, reason);
        return request;
    }

    public StaffRequest submitHelpOp(Player player, String message) {
        StaffRequest request = new StaffRequest(
                requestIds.incrementAndGet(),
                StaffRequest.Type.HELPOP,
                player.getName(),
                null,
                message,
                System.currentTimeMillis()
        );

        addRequest(helpOps, request);
        helpOpCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        saveRequests();

        String staffMessage = "&8[&cHelpOP #" + request.getId() + "&8] &f" + player.getName()
                + " &8- &7" + message;
        broadcastStaffRequest(request, staffMessage, "evaulx.helpop.receive");
        if (plugin.getRedisSyncManager() != null) plugin.getRedisSyncManager().publishStaffNotice("HELPOP", staffMessage);
        Bukkit.getConsoleSender().sendMessage(CC.color(staffMessage));
        plugin.getDiscordManager().sendHelpOp(player.getName(), message);
        logAction(player.getName(), "HELPOP", player.getName(), message);
        return request;
    }

    public RequestResult claimRequest(int id, String staffName) {
        StaffRequest request = findRequest(id);
        if (request == null) return RequestResult.NOT_FOUND;
        if (request.getStatus() == StaffRequest.Status.CLOSED) return RequestResult.CLOSED;

        request.setStatus(StaffRequest.Status.CLAIMED);
        request.setClaimedBy(staffName);
        request.setUpdatedAt(System.currentTimeMillis());
        saveRequests();

        broadcastStaff("&8[&cStaff&8] &f" + staffName + " &7claimed &f#" + id + "&7.", "evaulx.reports");
        logAction(staffName, "CLAIM_REQUEST", request.getTargetOrSender(), "#" + id);
        return RequestResult.UPDATED;
    }

    public RequestResult closeRequest(int id, String staffName, String reason) {
        StaffRequest request = findRequest(id);
        if (request == null) return RequestResult.NOT_FOUND;
        if (request.getStatus() == StaffRequest.Status.CLOSED) return RequestResult.CLOSED;

        request.setStatus(StaffRequest.Status.CLOSED);
        request.setClosedBy(staffName);
        request.setCloseReason(reason);
        request.setUpdatedAt(System.currentTimeMillis());
        saveRequests();

        broadcastStaff("&8[&cStaff&8] &f" + staffName + " &7closed &f#" + id
                + (reason == null || reason.isEmpty() ? "&7." : " &8- &7" + reason), "evaulx.reports");
        logAction(staffName, "CLOSE_REQUEST", request.getTargetOrSender(), "#" + id + " " + reason);
        return RequestResult.UPDATED;
    }

    public long getReportCooldownRemaining(Player player) {
        return getCooldownRemaining(reportCooldowns, player.getUniqueId(), getReportCooldownMillis());
    }

    public long getHelpOpCooldownRemaining(Player player) {
        return getCooldownRemaining(helpOpCooldowns, player.getUniqueId(), getHelpOpCooldownMillis());
    }

    public List<StaffRequest> getReports() {
        synchronized (reports) {
            return new ArrayList<>(reports);
        }
    }

    public List<StaffRequest> getHelpOps() {
        synchronized (helpOps) {
            return new ArrayList<>(helpOps);
        }
    }

    public List<StaffRequest> getRequestsFor(String name, int max) {
        List<StaffRequest> matches = new ArrayList<>();
        collectRequestsFor(matches, reports, name, max);
        collectRequestsFor(matches, helpOps, name, max);
        matches.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return matches.size() > max ? new ArrayList<>(matches.subList(0, max)) : matches;
    }

    public void clearReports() {
        synchronized (reports) {
            reports.clear();
        }
        saveRequests();
    }

    public void clearHelpOps() {
        synchronized (helpOps) {
            helpOps.clear();
        }
        saveRequests();
    }

    public boolean toggleStaffChat(Player player) {
        UUID uuid = player.getUniqueId();
        if (staffChatToggles.remove(uuid)) return false;
        staffChatToggles.add(uuid);
        return true;
    }

    public boolean isStaffChatEnabled(Player player) {
        return staffChatToggles.contains(player.getUniqueId());
    }

    public boolean toggleCommandSpy(Player player) {
        UUID uuid = player.getUniqueId();
        if (commandSpyToggles.remove(uuid)) return false;
        commandSpyToggles.add(uuid);
        return true;
    }

    public boolean isCommandSpyEnabled(Player player) {
        return commandSpyToggles.contains(player.getUniqueId());
    }

    public void sendCommandSpy(Player actor, String command) {
        if (actor == null || command == null || command.trim().isEmpty()) return;

        String cleanCommand = command.startsWith("/") ? command.substring(1) : command;
        String format = plugin.getConfig().getString("staff-tools.command-spy.format",
                "&8[&cCommandSpy&8] &f{player}&8: &7/{command}");
        String message = CC.color(format
                .replace("{player}", actor.getName())
                .replace("{command}", cleanCommand));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.equals(actor)) continue;
            if (!staff.hasPermission("evaulx.commandspy")) continue;
            if (!isCommandSpyEnabled(staff)) continue;
            staff.sendMessage(message);
        }
    }

    public void sendStaffChat(Player sender, String message) {
        sendStaffChat(sender.getName(), message, true);
    }

    public void sendStaffChat(String playerName, String message) {
        sendStaffChat(playerName, message, true);
    }

    public void sendStaffChat(String playerName, String message, boolean publish) {
        String format = plugin.getConfig().getString("staff-tools.staff-chat-format",
                "&8[&cStaffChat&8] &f{player}&8: &7{message}");
        String formatted = format.replace("{player}", playerName).replace("{message}", message);
        broadcastStaff(formatted, "evaulx.staffchat");
        Bukkit.getConsoleSender().sendMessage(CC.color(formatted));
        plugin.getDiscordManager().sendStaffChat(playerName, message);
        if (publish && plugin.getRedisSyncManager() != null) {
            plugin.getRedisSyncManager().publishStaffChat(playerName, message);
        }
    }

    public boolean freeze(Player staff, Player target, String reason) {
        return freeze(staff.getName(), target, reason);
    }

    public boolean freeze(String staffName, Player target, String reason) {
        if (frozenPlayers.containsKey(target.getUniqueId())) return false;

        FreezeInfo info = new FreezeInfo(target.getUniqueId(), target.getName(), staffName, reason, System.currentTimeMillis());
        frozenPlayers.put(target.getUniqueId(), info);
        saveFrozen();

        sendFreezeMessage(target, reason);
        broadcastStaff("&8[&cStaff&8] &f" + staffName + " &7froze &f" + target.getName()
                + " &8- &7" + reason, "evaulx.freeze");
        logAction(staffName, "FREEZE", target.getName(), reason);
        return true;
    }

    public boolean unfreeze(Player staff, Player target, String reason) {
        return unfreeze(staff.getName(), target, reason);
    }

    public boolean unfreeze(String staffName, Player target, String reason) {
        FreezeInfo removed = frozenPlayers.remove(target.getUniqueId());
        if (removed == null) return false;

        saveFrozen();
        target.sendMessage(CC.color("&aYou have been unfrozen."));
        broadcastStaff("&8[&cStaff&8] &f" + staffName + " &7unfroze &f" + target.getName()
                + (reason == null || reason.isEmpty() ? "&7." : " &8- &7" + reason), "evaulx.freeze");
        logAction(staffName, "UNFREEZE", target.getName(), reason);
        return true;
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.containsKey(player.getUniqueId());
    }

    public int getFrozenCount() {
        return frozenPlayers.size();
    }

    public void startStaffSession(Player player) {
        if (player == null || !canReceiveStaffAlerts(player)) return;
        UUID uuid = player.getUniqueId();
        if (activeStaffSessions.containsKey(uuid)) return;
        activeStaffSessions.put(uuid, new StaffSession(uuid, player.getName(), System.currentTimeMillis()));
    }

    public void endStaffSession(Player player) {
        if (player == null) return;
        StaffSession session = activeStaffSessions.remove(player.getUniqueId());
        if (session == null) return;
        session.setEndedAt(System.currentTimeMillis());
        addStaffSession(session);
        saveSessions();
        logAction(player.getName(), "STAFF_SESSION", player.getName(), session.getDurationString());
    }

    public List<StaffSession> getActiveStaffSessions() {
        List<StaffSession> sessions = new ArrayList<>(activeStaffSessions.values());
        sessions.sort((a, b) -> Long.compare(b.getStartedAt(), a.getStartedAt()));
        return sessions;
    }

    public List<StaffSession> getRecentStaffSessions(int max) {
        List<StaffSession> recent = new ArrayList<>();
        synchronized (staffSessions) {
            for (StaffSession session : staffSessions) {
                recent.add(session);
                if (recent.size() >= max) break;
            }
        }
        return recent;
    }

    public long getTotalStaffSessionTime(String name) {
        long total = 0L;
        synchronized (staffSessions) {
            for (StaffSession session : staffSessions) {
                if (session.getName().equalsIgnoreCase(name)) total += session.getDurationMillis();
            }
        }
        StaffSession active = null;
        for (StaffSession session : activeStaffSessions.values()) {
            if (session.getName().equalsIgnoreCase(name)) active = session;
        }
        if (active != null) total += active.getDurationMillis();
        return total;
    }

    public int getActiveStaffSessionCount() {
        return activeStaffSessions.size();
    }

    public FreezeInfo getFreezeInfo(UUID uuid) {
        return frozenPlayers.get(uuid);
    }

    public void applyStaffModeItems(Player player) {
        if (!plugin.getConfig().getBoolean("staff-tools.staffmode-items.enabled", true)) return;

        UUID uuid = player.getUniqueId();
        PlayerInventory inventory = player.getInventory();
        savedInventories.putIfAbsent(uuid, inventory.getContents().clone());
        savedArmor.putIfAbsent(uuid, inventory.getArmorContents().clone());
        saveStaffModeRecovery(player);

        if (plugin.getConfig().getBoolean("staff-tools.staffmode-items.clear-inventory", true)) {
            inventory.clear();
            inventory.setArmorContents(new ItemStack[4]);
        }

        for (StaffModeItem item : StaffModeItem.values()) {
            if (!plugin.getConfig().getBoolean(item.path() + ".enabled", true)) continue;
            int slot = plugin.getConfig().getInt(item.path() + ".slot", item.getDefaultSlot());
            if (slot < 0 || slot >= inventory.getSize()) continue;
            inventory.setItem(slot, createStaffItem(item));
        }
        player.updateInventory();
    }

    public void applyStaffModeState(Player player) {
        UUID uuid = player.getUniqueId();
        savedGameModes.putIfAbsent(uuid, player.getGameMode());
        savedAllowFlight.putIfAbsent(uuid, player.getAllowFlight());
        savedFlying.putIfAbsent(uuid, player.isFlying());

        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    public void restoreStaffModeItems(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack[] contents = savedInventories.remove(uuid);
        ItemStack[] armor = savedArmor.remove(uuid);
        if (contents == null && armor == null) return;

        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        if (contents != null) inventory.setContents(contents);
        if (armor != null) inventory.setArmorContents(armor);
        player.updateInventory();
    }

    public void restoreStaffModeState(Player player) {
        UUID uuid = player.getUniqueId();
        GameMode gameMode = savedGameModes.remove(uuid);
        Boolean allowFlight = savedAllowFlight.remove(uuid);
        Boolean flying = savedFlying.remove(uuid);
        if (gameMode == null && allowFlight == null && flying == null) return;

        if (gameMode != null) player.setGameMode(gameMode);
        if (allowFlight != null) player.setAllowFlight(allowFlight);

        if (flying != null && flying && player.getAllowFlight()) {
            player.setFlying(true);
        } else if (player.isFlying()) {
            player.setFlying(false);
        }
        clearStaffModeRecovery(player);
    }

    public void restoreAllStaffModeItems() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            restoreStaffModeItems(player);
            restoreStaffModeState(player);
            clearStaffModeRecovery(player);
        }
        savedGameModes.clear();
        savedAllowFlight.clear();
        savedFlying.clear();
        staffModeAutoVanished.clear();
    }

    public boolean recoverStaffMode(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        boolean hasMemorySnapshot = savedInventories.containsKey(uuid)
                || savedArmor.containsKey(uuid)
                || savedGameModes.containsKey(uuid)
                || savedAllowFlight.containsKey(uuid)
                || savedFlying.containsKey(uuid);

        if (restoreStaffModeRecovery(player)) return true;
        if (!hasMemorySnapshot) return false;

        restoreStaffModeItems(player);
        restoreStaffModeState(player);
        staffModeAutoVanished.remove(uuid);
        return true;
    }

    public boolean hasStaffModeRecovery(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        return savedInventories.containsKey(uuid)
                || savedArmor.containsKey(uuid)
                || savedGameModes.containsKey(uuid)
                || savedAllowFlight.containsKey(uuid)
                || savedFlying.containsKey(uuid)
                || recoveryFile(uuid).exists();
    }

    public void markStaffModeAutoVanished(Player player) {
        staffModeAutoVanished.add(player.getUniqueId());
    }

    public boolean consumeStaffModeAutoVanished(Player player) {
        return staffModeAutoVanished.remove(player.getUniqueId());
    }

    public StaffModeItem getStaffModeItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return null;
        if (item.getItemMeta().hasLore()) {
            for (String line : item.getItemMeta().getLore()) {
                String stripped = CC.strip(line);
                if (!stripped.startsWith("EVAULX_STAFF_ITEM:")) continue;
                String name = stripped.substring("EVAULX_STAFF_ITEM:".length());
                try {
                    return StaffModeItem.valueOf(name);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }

        String name = CC.strip(item.getItemMeta().getDisplayName());
        for (StaffModeItem staffItem : StaffModeItem.values()) {
            String configured = plugin.getConfig().getString(staffItem.path() + ".name", staffItem.getDefaultName());
            if (name.equalsIgnoreCase(CC.strip(configured)) || name.equalsIgnoreCase(CC.strip(staffItem.getDefaultName()))) {
                return staffItem;
            }
        }
        return null;
    }

    public void broadcastStaff(String message) {
        broadcastStaff(message, null);
    }

    public void broadcastStaff(String message, String extraPermission) {
        String colored = CC.color(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (canReceiveStaffAlerts(player) || (extraPermission != null && player.hasPermission(extraPermission))) {
                player.sendMessage(colored);
            }
        }
    }

    public void broadcastStaffRequest(StaffRequest request, String message, String extraPermission) {
        if (!plugin.getConfig().getBoolean("staff-tools.clickable-alerts", true)) {
            broadcastStaff(message, extraPermission);
            return;
        }

        TextComponent base = new TextComponent("");
        for (net.md_5.bungee.api.chat.BaseComponent component : TextComponent.fromLegacyText(CC.color(message))) {
            base.addExtra(component);
        }

        String target = request.getType() == StaffRequest.Type.REPORT ? request.getTarget() : request.getSender();
        if (target != null && !target.trim().isEmpty()) {
            base.addExtra(button(" &8[&aTP&8]", "/tp " + target, "Teleport to " + target));
        }
        base.addExtra(button(" &8[&eClaim&8]", "/reports claim " + request.getId(), "Claim request #" + request.getId()));
        base.addExtra(button(" &8[&cClose&8]", "/reports close " + request.getId() + " Handled", "Close request #" + request.getId()));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (canReceiveStaffAlerts(player) || (extraPermission != null && player.hasPermission(extraPermission))) {
                player.spigot().sendMessage(base);
            }
        }
    }

    private TextComponent button(String text, String command, String hover) {
        TextComponent component = new TextComponent(TextComponent.fromLegacyText(CC.color(text)));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(CC.color("&7" + hover + "\n&8" + command)).create()));
        return component;
    }

    public boolean canReceiveStaffAlerts(Player player) {
        String staffPermission = plugin.getConfig().getString("staff-tools.staff-permission", "evaulx.staff");
        return player.hasPermission(staffPermission);
    }

    public void logAction(String actor, String action, String target, String detail) {
        if (!plugin.getConfig().getBoolean("staff-tools.action-log.enabled", true)) return;

        StaffAction staffAction = new StaffAction(actor, action, target, detail, System.currentTimeMillis());
        synchronized (actions) {
            actions.addFirst(staffAction);
            int maxActions = Math.max(1, plugin.getConfig().getInt("staff-tools.action-log.max-recent", DEFAULT_MAX_ACTIONS));
            while (actions.size() > maxActions) actions.removeLast();
        }

        appendActionLog(staffAction);
        appendActionJsonLog(staffAction);
        saveActions();
        plugin.getDiscordManager().sendStaffLog(actor, action, target, detail);
    }

    public List<StaffAction> getActionsFor(String name, int max) {
        List<StaffAction> matches = new ArrayList<>();
        synchronized (actions) {
            for (StaffAction action : actions) {
                if (matchesName(action.getActor(), name) || matchesName(action.getTarget(), name)) {
                    matches.add(action);
                    if (matches.size() >= max) break;
                }
            }
        }
        return matches;
    }

    public List<StaffAction> getRecentActions(int max) {
        List<StaffAction> recent = new ArrayList<>();
        synchronized (actions) {
            for (StaffAction action : actions) {
                recent.add(action);
                if (recent.size() >= max) break;
            }
        }
        return recent;
    }

    public List<StaffAction> searchActions(String query, int max) {
        String lowered = query.toLowerCase(Locale.ENGLISH);
        List<StaffAction> matches = new ArrayList<>();
        synchronized (actions) {
            for (StaffAction action : actions) {
                if (contains(action.getActor(), lowered)
                        || contains(action.getAction(), lowered)
                        || contains(action.getTarget(), lowered)
                        || contains(action.getDetail(), lowered)) {
                    matches.add(action);
                    if (matches.size() >= max) break;
                }
            }
        }
        return matches;
    }

    private void collectRequestsFor(List<StaffRequest> matches, Deque<StaffRequest> source, String name, int max) {
        synchronized (source) {
            for (StaffRequest request : source) {
                if (matchesName(request.getSender(), name) || matchesName(request.getTarget(), name)) {
                    matches.add(request);
                    if (matches.size() >= max) return;
                }
            }
        }
    }

    private boolean matchesName(String value, String name) {
        return value != null && value.equalsIgnoreCase(name);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains(query);
    }

    private void addRequest(Deque<StaffRequest> requests, StaffRequest request) {
        synchronized (requests) {
            requests.addFirst(request);
            int maxRecent = Math.max(1, plugin.getConfig().getInt("staff-tools.max-recent-requests", DEFAULT_MAX_RECENT));
            while (requests.size() > maxRecent) {
                requests.removeLast();
            }
        }
    }

    private void addStaffSession(StaffSession session) {
        synchronized (staffSessions) {
            staffSessions.addFirst(session);
            int maxSessions = Math.max(1, plugin.getConfig().getInt("staff-tools.sessions.max-recent", 250));
            while (staffSessions.size() > maxSessions) staffSessions.removeLast();
        }
    }

    private void finishActiveSessions() {
        long now = System.currentTimeMillis();
        for (StaffSession session : activeStaffSessions.values()) {
            session.setEndedAt(now);
            addStaffSession(session);
        }
        activeStaffSessions.clear();
    }

    private StaffRequest findRequest(int id) {
        StaffRequest request = findRequest(reports, id);
        return request != null ? request : findRequest(helpOps, id);
    }

    private StaffRequest findRequest(Deque<StaffRequest> requests, int id) {
        synchronized (requests) {
            for (StaffRequest request : requests) {
                if (request.getId() == id) return request;
            }
        }
        return null;
    }

    private long getCooldownRemaining(Map<UUID, Long> cooldowns, UUID uuid, long cooldownMillis) {
        Long lastUsed = cooldowns.get(uuid);
        if (lastUsed == null || cooldownMillis <= 0L) return 0L;

        long remaining = cooldownMillis - (System.currentTimeMillis() - lastUsed);
        return Math.max(0L, remaining);
    }

    private long getReportCooldownMillis() {
        return plugin.getConfig().getLong("staff-tools.report-cooldown-seconds", 30L) * 1000L;
    }

    private long getHelpOpCooldownMillis() {
        return plugin.getConfig().getLong("staff-tools.helpop-cooldown-seconds", 15L) * 1000L;
    }

    private void saveStaffModeRecovery(Player player) {
        if (!plugin.getConfig().getBoolean("staff-tools.staffmode.recovery-snapshots", true)) return;
        if (recoveryDir == null) return;

        UUID uuid = player.getUniqueId();
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("uuid", uuid.toString());
            obj.addProperty("name", player.getName());
            obj.addProperty("createdAt", System.currentTimeMillis());
            obj.addProperty("inventory", encodeItems(savedInventories.getOrDefault(uuid, player.getInventory().getContents())));
            obj.addProperty("armor", encodeItems(savedArmor.getOrDefault(uuid, player.getInventory().getArmorContents())));
            GameMode gameMode = savedGameModes.getOrDefault(uuid, player.getGameMode());
            obj.addProperty("gameMode", gameMode.name());
            obj.addProperty("allowFlight", savedAllowFlight.getOrDefault(uuid, player.getAllowFlight()));
            obj.addProperty("flying", savedFlying.getOrDefault(uuid, player.isFlying()));
            writeJson(recoveryFile(uuid), obj);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save staff mode recovery for " + player.getName() + ": " + e.getMessage());
        }
    }

    private boolean restoreStaffModeRecovery(Player player) {
        File file = recoveryFile(player.getUniqueId());
        if (!file.exists()) return false;

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject obj = new JsonParser().parse(reader).getAsJsonObject();
            PlayerInventory inventory = player.getInventory();
            inventory.clear();
            inventory.setContents(decodeItems(getString(obj, "inventory", "")));
            inventory.setArmorContents(decodeItems(getString(obj, "armor", "")));

            String gameModeName = getString(obj, "gameMode", player.getGameMode().name());
            try {
                player.setGameMode(GameMode.valueOf(gameModeName));
            } catch (IllegalArgumentException ignored) {
                player.setGameMode(GameMode.SURVIVAL);
            }

            player.setAllowFlight(getBoolean(obj, "allowFlight", false));
            boolean flying = getBoolean(obj, "flying", false);
            if (flying && player.getAllowFlight()) player.setFlying(true);
            else if (player.isFlying()) player.setFlying(false);
            player.updateInventory();

            UUID uuid = player.getUniqueId();
            savedInventories.remove(uuid);
            savedArmor.remove(uuid);
            savedGameModes.remove(uuid);
            savedAllowFlight.remove(uuid);
            savedFlying.remove(uuid);
            staffModeAutoVanished.remove(uuid);
            clearStaffModeRecovery(player);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to restore staff mode recovery for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private File recoveryFile(UUID uuid) {
        return new File(recoveryDir == null ? staffDir : recoveryDir, uuid + ".json");
    }

    private void clearStaffModeRecovery(Player player) {
        File file = recoveryFile(player.getUniqueId());
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete staff mode recovery file: " + file.getPath());
        }
    }

    private String encodeItems(ItemStack[] items) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
            output.writeInt(items == null ? 0 : items.length);
            if (items != null) {
                for (ItemStack item : items) output.writeObject(item);
            }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    private ItemStack[] decodeItems(String encoded) throws IOException, ClassNotFoundException {
        if (encoded == null || encoded.trim().isEmpty()) return new ItemStack[0];
        byte[] bytes = Base64.getDecoder().decode(encoded);
        try (BukkitObjectInputStream input = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            int length = input.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) input.readObject();
            }
            return items;
        }
    }

    private ItemStack createStaffItem(StaffModeItem staffModeItem) {
        Material material = parseMaterial(plugin.getConfig().getString(staffModeItem.path() + ".material", staffModeItem.getDefaultMaterial().name()),
                staffModeItem.getDefaultMaterial());
        String name = plugin.getConfig().getString(staffModeItem.path() + ".name", staffModeItem.getDefaultName());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList(staffModeItem.path() + ".lore")) {
            lore.add(CC.color(line));
        }
        lore.add(CC.color("&0EVAULX_STAFF_ITEM:" + staffModeItem.name()));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null || name.trim().isEmpty()) return fallback;
        Material material = Material.matchMaterial(name.trim());
        return material != null ? material : fallback;
    }

    private void sendFreezeMessage(Player target, String reason) {
        List<String> lines = plugin.getConfig().getStringList("staff-tools.freeze-message");
        if (lines.isEmpty()) {
            lines = Arrays.asList(
                    "&cYou have been frozen by staff.",
                    "&7Do not log out. Reason: &f{reason}"
            );
        }

        for (String line : lines) {
            target.sendMessage(CC.color(line.replace("{reason}", reason)));
        }
    }

    private void appendActionLog(StaffAction action) {
        if (actionLogFile == null) return;
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(actionLogFile, true), StandardCharsets.UTF_8)) {
            writer.write(action.toLogLine());
            writer.write(System.lineSeparator());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to write staff action log: " + e.getMessage());
        }
    }

    private void appendActionJsonLog(StaffAction action) {
        if (actionJsonLogFile == null || !plugin.getConfig().getBoolean("staff-tools.action-log.write-jsonl", true)) return;
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(actionJsonLogFile, true), StandardCharsets.UTF_8)) {
            gson.toJson(actionToJson(action), writer);
            writer.write(System.lineSeparator());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to write staff action JSON log: " + e.getMessage());
        }
    }

    private void loadRequests() {
        if (requestsFile == null || !requestsFile.exists()) return;

        synchronized (reports) {
            synchronized (helpOps) {
                reports.clear();
                helpOps.clear();

                try (Reader reader = new InputStreamReader(new FileInputStream(requestsFile), StandardCharsets.UTF_8)) {
                    JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                    int maxId = root.has("nextId") ? root.get("nextId").getAsInt() : 0;
                    loadRequestArray(root.getAsJsonArray("reports"), reports);
                    loadRequestArray(root.getAsJsonArray("helpOps"), helpOps);
                    for (StaffRequest request : reports) maxId = Math.max(maxId, request.getId());
                    for (StaffRequest request : helpOps) maxId = Math.max(maxId, request.getId());
                    requestIds.set(maxId);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load staff requests: " + e.getMessage());
                }
            }
        }
    }

    private void loadRequestArray(JsonArray array, Deque<StaffRequest> target) {
        if (array == null) return;
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            StaffRequest request = requestFromJson(element.getAsJsonObject());
            if (request != null) target.addLast(request);
        }
    }

    private void saveRequests() {
        if (requestsFile == null) return;

        JsonObject root = new JsonObject();
        root.addProperty("nextId", requestIds.get());
        root.add("reports", requestsToJson(reports));
        root.add("helpOps", requestsToJson(helpOps));
        writeJson(requestsFile, root);
    }

    private JsonArray requestsToJson(Deque<StaffRequest> source) {
        JsonArray array = new JsonArray();
        synchronized (source) {
            for (StaffRequest request : source) {
                array.add(requestToJson(request));
            }
        }
        return array;
    }

    private void loadActions() {
        if (actionsFile == null || !actionsFile.exists()) return;

        synchronized (actions) {
            actions.clear();
            try (Reader reader = new InputStreamReader(new FileInputStream(actionsFile), StandardCharsets.UTF_8)) {
                JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) continue;
                    StaffAction action = actionFromJson(element.getAsJsonObject());
                    if (action != null) actions.addLast(action);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load staff actions: " + e.getMessage());
            }
        }
    }

    private void saveActions() {
        if (actionsFile == null) return;

        JsonArray array = new JsonArray();
        synchronized (actions) {
            for (StaffAction action : actions) {
                array.add(actionToJson(action));
            }
        }
        writeJson(actionsFile, array);
    }

    private void loadFrozen() {
        if (frozenFile == null || !frozenFile.exists()) return;

        frozenPlayers.clear();
        try (Reader reader = new InputStreamReader(new FileInputStream(frozenFile), StandardCharsets.UTF_8)) {
            JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                FreezeInfo info = freezeFromJson(element.getAsJsonObject());
                if (info != null) frozenPlayers.put(info.getUuid(), info);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load frozen players: " + e.getMessage());
        }
    }

    private void loadSessions() {
        if (sessionsFile == null || !sessionsFile.exists()) return;
        synchronized (staffSessions) {
            staffSessions.clear();
            try (Reader reader = new InputStreamReader(new FileInputStream(sessionsFile), StandardCharsets.UTF_8)) {
                JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) continue;
                    StaffSession session = sessionFromJson(element.getAsJsonObject());
                    if (session != null) staffSessions.addLast(session);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load staff sessions: " + e.getMessage());
            }
        }
    }

    private void saveFrozen() {
        if (frozenFile == null) return;

        JsonArray array = new JsonArray();
        for (FreezeInfo info : frozenPlayers.values()) {
            array.add(freezeToJson(info));
        }
        writeJson(frozenFile, array);
    }

    private void saveSessions() {
        if (sessionsFile == null) return;
        JsonArray array = new JsonArray();
        synchronized (staffSessions) {
            for (StaffSession session : staffSessions) {
                array.add(sessionToJson(session));
            }
        }
        writeJson(sessionsFile, array);
    }

    private void writeJson(File file, JsonElement element) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(element, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to write " + file.getName() + ": " + e.getMessage());
        }
    }

    private JsonObject requestToJson(StaffRequest request) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", request.getId());
        obj.addProperty("type", request.getType().name());
        obj.addProperty("sender", request.getSender());
        obj.addProperty("target", request.getTarget());
        obj.addProperty("message", request.getMessage());
        obj.addProperty("createdAt", request.getCreatedAt());
        obj.addProperty("status", request.getStatus().name());
        obj.addProperty("claimedBy", request.getClaimedBy());
        obj.addProperty("closedBy", request.getClosedBy());
        obj.addProperty("closeReason", request.getCloseReason());
        obj.addProperty("updatedAt", request.getUpdatedAt());
        return obj;
    }

    private StaffRequest requestFromJson(JsonObject obj) {
        try {
            StaffRequest request = new StaffRequest(
                    obj.get("id").getAsInt(),
                    StaffRequest.Type.valueOf(getString(obj, "type", "REPORT")),
                    getString(obj, "sender", "Unknown"),
                    getString(obj, "target", null),
                    getString(obj, "message", ""),
                    getLong(obj, "createdAt", System.currentTimeMillis())
            );
            request.setStatus(StaffRequest.Status.valueOf(getString(obj, "status", "OPEN")));
            request.setClaimedBy(getString(obj, "claimedBy", null));
            request.setClosedBy(getString(obj, "closedBy", null));
            request.setCloseReason(getString(obj, "closeReason", null));
            request.setUpdatedAt(getLong(obj, "updatedAt", request.getCreatedAt()));
            return request;
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonObject actionToJson(StaffAction action) {
        JsonObject obj = new JsonObject();
        obj.addProperty("actor", action.getActor());
        obj.addProperty("action", action.getAction());
        obj.addProperty("target", action.getTarget());
        obj.addProperty("detail", action.getDetail());
        obj.addProperty("createdAt", action.getCreatedAt());
        return obj;
    }

    private StaffAction actionFromJson(JsonObject obj) {
        try {
            return new StaffAction(
                    getString(obj, "actor", "Unknown"),
                    getString(obj, "action", "ACTION"),
                    getString(obj, "target", null),
                    getString(obj, "detail", ""),
                    getLong(obj, "createdAt", System.currentTimeMillis())
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonObject freezeToJson(FreezeInfo info) {
        JsonObject obj = new JsonObject();
        obj.addProperty("uuid", info.getUuid().toString());
        obj.addProperty("targetName", info.getTargetName());
        obj.addProperty("staffName", info.getStaffName());
        obj.addProperty("reason", info.getReason());
        obj.addProperty("createdAt", info.getCreatedAt());
        return obj;
    }

    private FreezeInfo freezeFromJson(JsonObject obj) {
        try {
            return new FreezeInfo(
                    UUID.fromString(getString(obj, "uuid", "")),
                    getString(obj, "targetName", "Unknown"),
                    getString(obj, "staffName", "Unknown"),
                    getString(obj, "reason", "No reason specified"),
                    getLong(obj, "createdAt", System.currentTimeMillis())
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonObject sessionToJson(StaffSession session) {
        JsonObject obj = new JsonObject();
        obj.addProperty("uuid", session.getUuid().toString());
        obj.addProperty("name", session.getName());
        obj.addProperty("startedAt", session.getStartedAt());
        obj.addProperty("endedAt", session.getEndedAt());
        return obj;
    }

    private StaffSession sessionFromJson(JsonObject obj) {
        try {
            StaffSession session = new StaffSession(
                    UUID.fromString(getString(obj, "uuid", "")),
                    getString(obj, "name", "Unknown"),
                    getLong(obj, "startedAt", System.currentTimeMillis())
            );
            session.setEndedAt(getLong(obj, "endedAt", session.getStartedAt()));
            return session;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getString(JsonObject obj, String key, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsString();
    }

    private long getLong(JsonObject obj, String key, long fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsLong();
    }

    private boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsBoolean();
    }

    public enum RequestResult { UPDATED, NOT_FOUND, CLOSED }

    public enum StaffModeItem {
        TELEPORT_COMPASS("teleport-compass", Material.COMPASS, 0, "&cTeleport Compass"),
        RANDOM_TELEPORT("random-teleport", Material.CLOCK, 1, "&cRandom Teleport"),
        INSPECT_PLAYER("inspect-player", Material.BLAZE_ROD, 3, "&cInspect Player"),
        TOGGLE_VANISH("toggle-vanish", Material.ENDER_EYE, 5, "&cToggle Vanish"),
        REPORTS("reports", Material.BOOK, 7, "&cReports");

        private final String key;
        private final Material defaultMaterial;
        private final int defaultSlot;
        private final String defaultName;

        StaffModeItem(String key, Material defaultMaterial, int defaultSlot, String defaultName) {
            this.key = key;
            this.defaultMaterial = defaultMaterial;
            this.defaultSlot = defaultSlot;
            this.defaultName = defaultName;
        }

        public String path() {
            return "staff-tools.staffmode-items.items." + key;
        }

        public Material getDefaultMaterial() {
            return defaultMaterial;
        }

        public int getDefaultSlot() {
            return defaultSlot;
        }

        public String getDefaultName() {
            return defaultName;
        }
    }

    public static class StaffRequest {
        public enum Type { REPORT, HELPOP }
        public enum Status { OPEN, CLAIMED, CLOSED }

        private final int id;
        private final Type type;
        private final String sender;
        private final String target;
        private final String message;
        private final long createdAt;
        private Status status = Status.OPEN;
        private String claimedBy;
        private String closedBy;
        private String closeReason;
        private long updatedAt;

        public StaffRequest(int id, Type type, String sender, String target, String message, long createdAt) {
            this.id = id;
            this.type = type;
            this.sender = sender;
            this.target = target;
            this.message = message;
            this.createdAt = createdAt;
            this.updatedAt = createdAt;
        }

        public int getId() { return id; }
        public Type getType() { return type; }
        public String getSender() { return sender; }
        public String getTarget() { return target; }
        public String getMessage() { return message; }
        public long getCreatedAt() { return createdAt; }
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
        public String getClaimedBy() { return claimedBy; }
        public void setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; }
        public String getClosedBy() { return closedBy; }
        public void setClosedBy(String closedBy) { this.closedBy = closedBy; }
        public String getCloseReason() { return closeReason; }
        public void setCloseReason(String closeReason) { this.closeReason = closeReason; }
        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

        public String getTargetOrSender() {
            return target != null ? target : sender;
        }

        public String getFormattedTime() {
            return new SimpleDateFormat("HH:mm").format(new Date(createdAt));
        }
    }

    public static class StaffAction {
        private final String actor;
        private final String action;
        private final String target;
        private final String detail;
        private final long createdAt;

        public StaffAction(String actor, String action, String target, String detail, long createdAt) {
            this.actor = actor;
            this.action = action;
            this.target = target;
            this.detail = detail == null ? "" : detail;
            this.createdAt = createdAt;
        }

        public String getActor() { return actor; }
        public String getAction() { return action; }
        public String getTarget() { return target; }
        public String getDetail() { return detail; }
        public long getCreatedAt() { return createdAt; }

        public String getFormattedTime() {
            return new SimpleDateFormat("MM/dd/yy HH:mm").format(new Date(createdAt));
        }

        public String toLogLine() {
            return "[" + getFormattedTime() + "] " + actor + " " + action
                    + (target == null ? "" : " " + target)
                    + (detail.isEmpty() ? "" : " - " + detail);
        }
    }

    public static class FreezeInfo {
        private final UUID uuid;
        private final String targetName;
        private final String staffName;
        private final String reason;
        private final long createdAt;

        public FreezeInfo(UUID uuid, String targetName, String staffName, String reason, long createdAt) {
            this.uuid = uuid;
            this.targetName = targetName;
            this.staffName = staffName;
            this.reason = reason;
            this.createdAt = createdAt;
        }

        public UUID getUuid() { return uuid; }
        public String getTargetName() { return targetName; }
        public String getStaffName() { return staffName; }
        public String getReason() { return reason; }
        public long getCreatedAt() { return createdAt; }
    }

    public static class StaffSession {
        private final UUID uuid;
        private final String name;
        private final long startedAt;
        private long endedAt;

        public StaffSession(UUID uuid, String name, long startedAt) {
            this.uuid = uuid;
            this.name = name;
            this.startedAt = startedAt;
            this.endedAt = 0L;
        }

        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public long getStartedAt() { return startedAt; }
        public long getEndedAt() { return endedAt; }
        public void setEndedAt(long endedAt) { this.endedAt = endedAt; }

        public long getDurationMillis() {
            long end = endedAt <= 0L ? System.currentTimeMillis() : endedAt;
            return Math.max(0L, end - startedAt);
        }

        public String getDurationString() {
            long seconds = getDurationMillis() / 1000L;
            long hours = seconds / 3600L;
            long minutes = (seconds % 3600L) / 60L;
            long secs = seconds % 60L;
            if (hours > 0L) return hours + "h " + minutes + "m";
            if (minutes > 0L) return minutes + "m " + secs + "s";
            return secs + "s";
        }

        public String getStartedFormatted() {
            return new SimpleDateFormat("MM/dd/yy HH:mm").format(new Date(startedAt));
        }
    }
}
