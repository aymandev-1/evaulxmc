package dev.evaulx.core.hooks;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.Collection;

public class EvaulxMCHubHook {

    private final EvaulxCore plugin;
    private Plugin hubPlugin;
    private Object hubApi;
    private String hookMode = "none";

    public EvaulxMCHubHook(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (!plugin.getConfig().getBoolean("hub-hook.enabled", true)) return;

        String pluginName = plugin.getConfig().getString("hub-hook.plugin-name", "EvaulxMCHub");
        hubPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        hubApi = findApiProvider();

        if (hubApi != null) {
            hookMode = "api";
            plugin.getLogger().info("Hooked into " + pluginName + " API service.");
            return;
        }

        if (hubPlugin != null && hubPlugin.isEnabled()) {
            hookMode = "plugin";
            plugin.getLogger().info("Hooked into " + hubPlugin.getName() + " plugin instance.");
        }
    }

    public boolean isHooked() {
        return hubApi != null || (hubPlugin != null && hubPlugin.isEnabled());
    }

    public String getHookMode() {
        return hookMode;
    }

    public boolean handlesNameTags() {
        return isHooked() && plugin.getConfig().getBoolean("hub-hook.handles-nametags", true);
    }

    public void refreshPlayer(Player player) {
        if (!isHooked() || player == null) return;

        if (invokeConfiguredRefresh(player)) return;

        if (hubApi != null && invokePlayerMethod(hubApi, player,
                "refreshPlayer", "updatePlayer", "refreshDisplay", "updateDisplay",
                "applyNametag", "updateNametag")) {
            return;
        }

        if (hubPlugin != null && invokePlayerMethod(hubPlugin, player,
                "refreshPlayer", "updatePlayer", "refreshDisplay", "updateDisplay",
                "applyNametag", "updateNametag")) {
            return;
        }

        for (Object source : new Object[]{hubApi, hubPlugin}) {
            if (source == null) continue;
            for (String accessor : new String[]{
                    "getDisplayManager", "getNameTagManager", "getNametagManager",
                    "getPlayerManager", "getProfileManager", "getHubManager"}) {
                Object manager = invokeNoArgs(source, accessor);
                if (manager == null) continue;
                if (invokePlayerMethod(manager, player,
                        "refreshPlayer", "updatePlayer", "refresh", "update", "apply", "setup", "load")) {
                    return;
                }
            }
        }

        dispatchRefreshCommand(player);
    }

    private Object findApiProvider() {
        String configuredService = plugin.getConfig().getString("hub-hook.api-service-class", "");
        Object provider = findServiceProvider(configuredService);
        if (provider != null) return provider;

        if (hubPlugin == null || !hubPlugin.isEnabled()) return null;
        for (String accessor : new String[]{"getApi", "getAPI", "getHubApi", "getHubAPI", "getDisplayApi", "getDisplayAPI"}) {
            Object api = invokeNoArgs(hubPlugin, accessor);
            if (api != null) return api;
        }
        return null;
    }

    private Object findServiceProvider(String configuredService) {
        Collection<Class<?>> services = Bukkit.getServicesManager().getKnownServices();
        for (Class<?> service : services) {
            if (!matchesService(service, configuredService)) continue;
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(service);
            if (registration != null && registration.getProvider() != null) {
                return registration.getProvider();
            }
        }
        return null;
    }

    private boolean matchesService(Class<?> service, String configuredService) {
        if (configuredService != null && !configuredService.trim().isEmpty()) {
            return service.getName().equals(configuredService)
                    || service.getSimpleName().equals(configuredService)
                    || service.getName().endsWith("." + configuredService);
        }

        String name = service.getName().toLowerCase();
        return name.contains("evaulxmchub") && (name.contains("api") || name.contains("service"));
    }

    private boolean invokeConfiguredRefresh(Player player) {
        for (String method : plugin.getConfig().getStringList("hub-hook.refresh-methods")) {
            if (method == null || method.trim().isEmpty()) continue;
            for (Object target : new Object[]{hubApi, hubPlugin}) {
                if (target != null && invokePlayerMethod(target, player, method.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean invokePlayerMethod(Object target, Player player, String... methodNames) {
        for (String methodName : methodNames) {
            if (invoke(target, methodName, new Class[]{Player.class}, new Object[]{player})) return true;
            if (invoke(target, methodName, new Class[]{String.class}, new Object[]{player.getName()})) return true;
            if (invoke(target, methodName, new Class[]{java.util.UUID.class}, new Object[]{player.getUniqueId()})) return true;
            if (invoke(target, methodName, new Class[]{Player.class, boolean.class}, new Object[]{player, true})) return true;
            if (invoke(target, methodName, new Class[]{String.class, boolean.class}, new Object[]{player.getName(), true})) return true;
        }
        return false;
    }

    public void refreshAll() {
        if (!isHooked()) return;
        if (hubApi != null && invokeNoArgsBoolean(hubApi, "refreshAll", "updateAll", "reloadDisplays")) return;
        if (hubPlugin != null && invokeNoArgsBoolean(hubPlugin, "refreshAll", "updateAll", "reloadDisplays")) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayer(player);
        }
    }

    private boolean invokeNoArgsBoolean(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                method.setAccessible(true);
                method.invoke(target);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private Object invokeNoArgs(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean invoke(Object target, String methodName, Class<?>[] types, Object[] args) {
        try {
            Method method = target.getClass().getMethod(methodName, types);
            method.setAccessible(true);
            method.invoke(target, args);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void dispatchRefreshCommand(Player player) {
        String command = plugin.getConfig().getString("hub-hook.refresh-command", "");
        if (command == null || command.trim().isEmpty()) return;

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString()));
    }
}
