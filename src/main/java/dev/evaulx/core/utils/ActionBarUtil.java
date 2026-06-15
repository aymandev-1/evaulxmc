package dev.evaulx.core.utils;

import org.bukkit.entity.Player;

public final class ActionBarUtil {

    private ActionBarUtil() {
    }

    public static void send(Player player, String message) {
        if (player == null || message == null || message.trim().isEmpty()) return;
        try {
            String pkg = player.getServer().getClass().getPackage().getName();
            String ver = pkg.substring(pkg.lastIndexOf('.') + 1);
            String nms = "net.minecraft.server." + ver + ".";
            Class<?> chatSerializer = Class.forName(nms + "ChatSerializer");
            Class<?> iChatBase = Class.forName(nms + "IChatBaseComponent");
            String escaped = CC.color(message).replace("\\", "\\\\").replace("\"", "\\\"");
            Object comp = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + escaped + "\"}");
            Class<?> packetClass = Class.forName(nms + "PacketPlayOutChat");
            Object packet = packetClass.getConstructor(iChatBase, byte.class).newInstance(comp, (byte) 2);
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Class<?> packetIF = Class.forName(nms + "Packet");
            connection.getClass().getMethod("sendPacket", packetIF).invoke(connection, packet);
        } catch (Throwable ignored) {}
    }
}
