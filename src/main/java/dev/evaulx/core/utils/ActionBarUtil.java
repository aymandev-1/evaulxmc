package dev.evaulx.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class ActionBarUtil {

    private ActionBarUtil() {
    }

    public static void send(Player player, String message) {
        if (player == null || message == null || message.trim().isEmpty()) return;

        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> chatSerializer = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Class<?> chatComponent = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");

            Object component = chatSerializer.getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + escapeJson(CC.color(message)) + "\"}");
            Object packet = packetClass.getConstructor(chatComponent, byte.class).newInstance(component, (byte) 2);

            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Method sendPacket = connection.getClass().getMethod("sendPacket",
                    Class.forName("net.minecraft.server." + version + ".Packet"));
            sendPacket.invoke(connection, packet);
        } catch (Exception ignored) {
            // Action bars are a visual extra; chat messages already carry the important feedback.
        }
    }

    private static String escapeJson(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                default:
                    builder.append(c);
                    break;
            }
        }
        return builder.toString();
    }
}
