package dev.evaulx.core.hooks;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collections;

public class ProtocolLibHook {

    private final EvaulxCore plugin;
    private Object protocolManager;
    private Method updateEntityMethod;
    private boolean hooked;

    public ProtocolLibHook(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        hooked = false;
        if (!plugin.getConfig().getBoolean("disguise.protocollib.enabled", true)) return;
        if (!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) return;

        try {
            Class<?> protocolLibrary = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            protocolManager = protocolLibrary.getMethod("getProtocolManager").invoke(null);
            updateEntityMethod = protocolManager.getClass().getMethod("updateEntity", Entity.class, java.util.List.class);
            hooked = true;
            plugin.getLogger().info("Hooked into ProtocolLib for disguise refresh support.");
        } catch (Exception e) {
            plugin.getLogger().warning("ProtocolLib detected, but disguise refresh hook could not load: " + e.getMessage());
        }
    }

    public boolean refreshEntity(Player viewer, Player target) {
        if (!hooked || viewer == null || target == null) return false;
        try {
            updateEntityMethod.invoke(protocolManager, target, Collections.singletonList(viewer));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean isHooked() {
        return hooked;
    }
}
