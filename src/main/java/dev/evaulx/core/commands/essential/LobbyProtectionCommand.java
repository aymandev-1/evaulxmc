package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyProtectionCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ENFORCED_FLAGS = Arrays.asList(
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

    private final EvaulxCore plugin;
    private final Map<UUID, Location> pos1 = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos2 = new ConcurrentHashMap<>();

    public LobbyProtectionCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.lobbyprotection")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player) {
                plugin.getGuiManager().openLobbyProtection((Player) sender);
                return true;
            }
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ENGLISH);
        if (sub.equals("gui") || sub.equals("menu")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(CC.color("&cOnly players can open the lobby protection GUI."));
                return true;
            }
            plugin.getGuiManager().openLobbyProtection((Player) sender);
            return true;
        }

        if (sub.equals("status") || sub.equals("check")) {
            sendStatus(sender, args.length > 1 ? args[1] : null);
            return true;
        }

        if (sub.equals("test")) {
            testProtection(sender);
            return true;
        }

        if (sub.equals("pos1") || sub.equals("pos2")) {
            setPosition(sender, sub.equals("pos1"));
            return true;
        }

        if (sub.equals("region")) {
            handleRegion(sender, args);
            return true;
        }

        if (sub.equals("on") || sub.equals("enable")) {
            plugin.getConfig().set("lobby-protection.enabled", true);
            save(sender, "&aLobby protection enabled.");
            return true;
        }

        if (sub.equals("off") || sub.equals("disable")) {
            plugin.getConfig().set("lobby-protection.enabled", false);
            save(sender, "&cLobby protection disabled.");
            return true;
        }

        if (sub.equals("protect")) {
            String world = resolveWorldName(sender, args.length > 1 ? args[1] : null);
            if (world == null) return true;
            if (world.equalsIgnoreCase("all")) {
                plugin.getConfig().set("lobby-protection.worlds", new ArrayList<String>());
                save(sender, "&aLobby protection now applies to every loaded world.");
            } else {
                addWorld(world);
                save(sender, "&aProtected lobby world: &f" + world);
            }
            return true;
        }

        if (sub.equals("unprotect")) {
            String world = resolveWorldName(sender, args.length > 1 ? args[1] : null);
            if (world == null || world.equalsIgnoreCase("all")) {
                sender.sendMessage(CC.color("&cUsage: /lobbyprotect unprotect <world>"));
                return true;
            }
            removeWorld(world);
            save(sender, "&cUnprotected lobby world: &f" + world);
            return true;
        }

        if (sub.equals("worlds")) {
            List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
            sender.sendMessage(CC.color(worlds.isEmpty()
                    ? "&7Protected worlds: &fALL loaded worlds"
                    : "&7Protected worlds: &f" + join(worlds)));
            return true;
        }

        if (sub.equals("enforce") || sub.equals("lockdown")) {
            String world = resolveWorldName(sender, args.length > 1 ? args[1] : null);
            if (world == null) return true;
            enforce(world);
            save(sender, world.equalsIgnoreCase("all")
                    ? "&aLobby protection enforced for every loaded world."
                    : "&aLobby protection enforced for &f" + world + "&a.");
            return true;
        }

        if (sub.equals("toggle")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /lobbyprotect toggle <setting>"));
                return true;
            }
            String setting = normalizeSetting(args[1]);
            if (setting == null) {
                sender.sendMessage(CC.color("&cUnknown setting. Try tab-complete after /lobbyprotect toggle."));
                return true;
            }
            String path = "lobby-protection." + setting;
            boolean value = !plugin.getConfig().getBoolean(path, true);
            plugin.getConfig().set(path, value);
            save(sender, "&7" + setting + ": " + (value ? "&aenabled" : "&cdisabled"));
            return true;
        }

        if (sub.equals("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(CC.color("&aConfiguration reloaded."));
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendStatus(CommandSender sender, String worldName) {
        String world = worldName != null ? worldName : sender instanceof Player ? ((Player) sender).getWorld().getName() : null;
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cLobby Protection"));
        sender.sendMessage(CC.color("&7Enabled: " + enabledText(plugin.getConfig().getBoolean("lobby-protection.enabled", true))));
        sender.sendMessage(CC.color("&7Mode: &f" + protectionMode()));
        if (world != null) sender.sendMessage(CC.color("&7World &f" + world + "&7: " + enabledText(isProtectedWorld(world))));
        sender.sendMessage(CC.color("&7Denied action messages: " + enabledText(plugin.getConfig().getBoolean("lobby-protection.notify-on-deny", true))));
        sender.sendMessage(CC.color("&7Void rescue: " + enabledText(plugin.getConfig().getBoolean("lobby-protection.void-rescue.enabled", true))));
        sender.sendMessage(CC.color("&7Use &f/lobbyprotect enforce " + (world == null ? "<world>" : world) + " &7to hard-enable protection."));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cLobby Protection Commands"));
        sender.sendMessage(CC.color("&f/lobbyprotect &7- Open the protection GUI"));
        sender.sendMessage(CC.color("&f/lobbyprotect status [world] &7- Check protection state"));
        sender.sendMessage(CC.color("&f/lobbyprotect enforce [world|all] &7- Enable and hard-lock protection"));
        sender.sendMessage(CC.color("&f/lobbyprotect protect [world|all] &7- Add a protected world"));
        sender.sendMessage(CC.color("&f/lobbyprotect unprotect <world> &7- Remove a protected world"));
        sender.sendMessage(CC.color("&f/lobbyprotect toggle <setting> &7- Toggle a protection setting"));
        sender.sendMessage(CC.color("&f/lobbyprotect test &7- Test protection at your position"));
        sender.sendMessage(CC.color("&f/lobbyprotect pos1/pos2 &7- Select region corners"));
        sender.sendMessage(CC.color("&f/lobbyprotect region create <name> &7- Save selected region"));
        sender.sendMessage(CC.color("&f/lobbyprotect region delete <name> &7- Delete a region"));
        sender.sendMessage(CC.color("&f/buildmode &7- Staff build bypass while protection remains active"));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void testProtection(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cPlayers only."));
            return;
        }
        Player player = (Player) sender;
        boolean protectedWorld = isProtectedWorld(player.getWorld().getName());
        boolean protectedRegion = isInsideRegion(player.getLocation());
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cLobby Protection Test"));
        sender.sendMessage(CC.color("&7World: &f" + player.getWorld().getName()));
        sender.sendMessage(CC.color("&7World protected: " + enabledText(protectedWorld)));
        sender.sendMessage(CC.color("&7Region protected: " + enabledText(protectedRegion)));
        sender.sendMessage(CC.color("&7You can build: " + enabledText(player.hasPermission("evaulx.protection.bypass"))));
        sender.sendMessage(CC.color("&7Result: " + enabledText(protectedWorld || protectedRegion)));
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void setPosition(CommandSender sender, boolean first) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cPlayers only."));
            return;
        }
        Player player = (Player) sender;
        if (first) pos1.put(player.getUniqueId(), player.getLocation());
        else pos2.put(player.getUniqueId(), player.getLocation());
        sender.sendMessage(CC.color("&aSet lobby protection position " + (first ? "1" : "2") + "."));
    }

    private void handleRegion(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /lobbyprotect region <create|delete|list|show> [name]"));
            return;
        }
        String action = args[1].toLowerCase(Locale.ENGLISH);
        if (action.equals("list")) {
            org.bukkit.configuration.ConfigurationSection regions = plugin.getConfig().getConfigurationSection("lobby-protection.regions.entries");
            sender.sendMessage(CC.color("&7Regions: &f" + (regions == null || regions.getKeys(false).isEmpty() ? "none" : join(new ArrayList<>(regions.getKeys(false))))));
            return;
        }
        if (action.equals("show") || action.equals("visualize")) {
            showRegion(sender, args);
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(CC.color("&cUsage: /lobbyprotect region " + action + " <name>"));
            return;
        }
        String name = args[2].toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9_-]", "");
        if (name.isEmpty()) {
            sender.sendMessage(CC.color("&cRegion name can only use letters, numbers, dashes, and underscores."));
            return;
        }

        if (action.equals("delete")) {
            plugin.getConfig().set("lobby-protection.regions.entries." + name, null);
            save(sender, "&cDeleted lobby protection region &f" + name + "&c.");
            return;
        }

        if (!action.equals("create")) {
            sender.sendMessage(CC.color("&cUse create, delete, or list."));
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cPlayers only."));
            return;
        }
        Player player = (Player) sender;
        Location first = pos1.get(player.getUniqueId());
        Location second = pos2.get(player.getUniqueId());
        if (first == null || second == null || !first.getWorld().equals(second.getWorld())) {
            sender.sendMessage(CC.color("&cSet both positions in the same world first."));
            return;
        }

        String path = "lobby-protection.regions.entries." + name + ".";
        plugin.getConfig().set(path + "world", first.getWorld().getName());
        plugin.getConfig().set(path + "min-x", Math.min(first.getBlockX(), second.getBlockX()));
        plugin.getConfig().set(path + "min-y", Math.min(first.getBlockY(), second.getBlockY()));
        plugin.getConfig().set(path + "min-z", Math.min(first.getBlockZ(), second.getBlockZ()));
        plugin.getConfig().set(path + "max-x", Math.max(first.getBlockX(), second.getBlockX()) + 1);
        plugin.getConfig().set(path + "max-y", Math.max(first.getBlockY(), second.getBlockY()) + 1);
        plugin.getConfig().set(path + "max-z", Math.max(first.getBlockZ(), second.getBlockZ()) + 1);
        save(sender, "&aCreated lobby protection region &f" + name + "&a.");
    }

    private void showRegion(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.color("&cPlayers only."));
            return;
        }
        Player player = (Player) sender;
        String name = args.length > 2 ? args[2].toLowerCase(Locale.ENGLISH) : "selection";
        int seconds = args.length > 3 ? parseInt(args[3], 8) : 8;

        Location first;
        Location second;
        if (name.equals("selection")) {
            first = pos1.get(player.getUniqueId());
            second = pos2.get(player.getUniqueId());
            if (first == null || second == null || !first.getWorld().equals(second.getWorld())) {
                sender.sendMessage(CC.color("&cSet /lobbyprotect pos1 and pos2 in the same world first."));
                return;
            }
        } else {
            String path = "lobby-protection.regions.entries." + name + ".";
            String worldName = plugin.getConfig().getString(path + "world", "");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                sender.sendMessage(CC.color("&cRegion not found or world is not loaded."));
                return;
            }
            first = new Location(world,
                    plugin.getConfig().getDouble(path + "min-x"),
                    plugin.getConfig().getDouble(path + "min-y"),
                    plugin.getConfig().getDouble(path + "min-z"));
            second = new Location(world,
                    plugin.getConfig().getDouble(path + "max-x"),
                    plugin.getConfig().getDouble(path + "max-y"),
                    plugin.getConfig().getDouble(path + "max-z"));
        }

        playRegion(player, first, second, Math.max(2, Math.min(30, seconds)));
        sender.sendMessage(CC.color("&aShowing lobby protection region &f" + name + " &afor &f" + seconds + "s&a."));
    }

    private void playRegion(Player player, Location first, Location second, int seconds) {
        final World world = first.getWorld();
        final int minX = Math.min(first.getBlockX(), second.getBlockX());
        final int minY = Math.min(first.getBlockY(), second.getBlockY());
        final int minZ = Math.min(first.getBlockZ(), second.getBlockZ());
        final int maxX = Math.max(first.getBlockX(), second.getBlockX());
        final int maxY = Math.max(first.getBlockY(), second.getBlockY());
        final int maxZ = Math.max(first.getBlockZ(), second.getBlockZ());
        final int step = Math.max(1, Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ) / 24);
        final int maxRuns = seconds * 2;

        new BukkitRunnable() {
            private int runs;

            @Override
            public void run() {
                if (!player.isOnline() || runs++ >= maxRuns) {
                    cancel();
                    return;
                }
                drawCuboid(player, world, minX, minY, minZ, maxX, maxY, maxZ, step);
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void drawCuboid(Player player, World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int step) {
        for (int x = minX; x <= maxX; x += step) {
            play(player, world, x, minY, minZ);
            play(player, world, x, minY, maxZ);
            play(player, world, x, maxY, minZ);
            play(player, world, x, maxY, maxZ);
        }
        for (int y = minY; y <= maxY; y += step) {
            play(player, world, minX, y, minZ);
            play(player, world, minX, y, maxZ);
            play(player, world, maxX, y, minZ);
            play(player, world, maxX, y, maxZ);
        }
        for (int z = minZ; z <= maxZ; z += step) {
            play(player, world, minX, minY, z);
            play(player, world, minX, maxY, z);
            play(player, world, maxX, minY, z);
            play(player, world, maxX, maxY, z);
        }
    }

    private void play(Player player, World world, int x, int y, int z) {
        player.playEffect(new Location(world, x + 0.5D, y + 0.5D, z + 0.5D), Effect.MOBSPAWNER_FLAMES, 0);
    }

    private void enforce(String world) {
        plugin.getConfig().set("lobby-protection.enabled", true);
        for (String flag : ENFORCED_FLAGS) {
            plugin.getConfig().set("lobby-protection." + flag, true);
        }
        if (world.equalsIgnoreCase("all")) {
            plugin.getConfig().set("lobby-protection.worlds", new ArrayList<String>());
        } else {
            addWorld(world);
        }
    }

    private void addWorld(String world) {
        List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
        for (String configured : worlds) {
            if (configured.equalsIgnoreCase(world)) return;
        }
        worlds.add(world);
        plugin.getConfig().set("lobby-protection.worlds", worlds);
    }

    private void removeWorld(String world) {
        List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
        List<String> updated = new ArrayList<>();
        for (String configured : worlds) {
            if (!configured.equalsIgnoreCase(world)) updated.add(configured);
        }
        plugin.getConfig().set("lobby-protection.worlds", updated);
    }

    private String resolveWorldName(CommandSender sender, String input) {
        if (input != null && !input.trim().isEmpty()) return input.trim();
        if (sender instanceof Player) return ((Player) sender).getWorld().getName();
        sender.sendMessage(CC.color("&cUsage requires a world from console."));
        return null;
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

    private boolean isInsideRegion(Location location) {
        org.bukkit.configuration.ConfigurationSection regions = plugin.getConfig().getConfigurationSection("lobby-protection.regions.entries");
        if (regions == null) return false;
        for (String key : regions.getKeys(false)) {
            String path = "lobby-protection.regions.entries." + key + ".";
            if (!location.getWorld().getName().equalsIgnoreCase(plugin.getConfig().getString(path + "world", ""))) continue;
            if (location.getX() >= plugin.getConfig().getDouble(path + "min-x")
                    && location.getX() <= plugin.getConfig().getDouble(path + "max-x")
                    && location.getY() >= plugin.getConfig().getDouble(path + "min-y")
                    && location.getY() <= plugin.getConfig().getDouble(path + "max-y")
                    && location.getZ() >= plugin.getConfig().getDouble(path + "min-z")
                    && location.getZ() <= plugin.getConfig().getDouble(path + "max-z")) {
                return true;
            }
        }
        return false;
    }

    private String protectionMode() {
        List<String> worlds = plugin.getConfig().getStringList("lobby-protection.worlds");
        return worlds.isEmpty() ? "All loaded worlds" : join(worlds);
    }

    private String enabledText(boolean value) {
        return value ? "&aenabled" : "&cdisabled";
    }

    private String normalizeSetting(String input) {
        String normalized = input.toLowerCase(Locale.ENGLISH).replace("_", "-");
        for (String flag : ENFORCED_FLAGS) {
            if (flag.equalsIgnoreCase(normalized)) return flag;
        }
        if (normalized.equals("enabled")) return "enabled";
        if (normalized.equals("deny-message") || normalized.equals("notify")) return "notify-on-deny";
        return null;
    }

    private void save(CommandSender sender, String message) {
        plugin.saveConfig();
        sender.sendMessage(CC.color(message));
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(value);
        }
        return builder.toString();
    }

    private int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.lobbyprotection")) return Collections.emptyList();
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            Collections.addAll(suggestions, "gui", "status", "test", "enforce", "protect", "unprotect", "worlds", "toggle", "pos1", "pos2", "region", "enable", "disable", "reload");
            return filter(suggestions, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ENGLISH);
        if (args.length == 2 && (sub.equals("protect") || sub.equals("enforce"))) {
            suggestions.add("all");
            addWorldNames(suggestions);
            return filter(suggestions, args[1]);
        }
        if (args.length == 2 && (sub.equals("unprotect") || sub.equals("status") || sub.equals("check"))) {
            addWorldNames(suggestions);
            return filter(suggestions, args[1]);
        }
        if (args.length == 2 && sub.equals("toggle")) {
            suggestions.add("enabled");
            suggestions.addAll(ENFORCED_FLAGS);
            suggestions.add("notify");
            return filter(suggestions, args[1]);
        }
        if (args.length == 2 && sub.equals("region")) {
            Collections.addAll(suggestions, "create", "delete", "list", "show");
            return filter(suggestions, args[1]);
        }
        if (args.length == 3 && sub.equals("region") && (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("show"))) {
            suggestions.add("selection");
            org.bukkit.configuration.ConfigurationSection regions = plugin.getConfig().getConfigurationSection("lobby-protection.regions.entries");
            if (regions != null) suggestions.addAll(regions.getKeys(false));
            return filter(suggestions, args[2]);
        }
        return Collections.emptyList();
    }

    private void addWorldNames(List<String> suggestions) {
        for (World world : Bukkit.getWorlds()) {
            suggestions.add(world.getName());
        }
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ENGLISH);
        List<String> filtered = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ENGLISH).startsWith(lower)) filtered.add(value);
        }
        Collections.sort(filtered);
        return filtered;
    }
}
