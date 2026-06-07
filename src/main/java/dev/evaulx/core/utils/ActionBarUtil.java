package dev.evaulx.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Sends action-bar messages using Paper's bundled Adventure API.
 *
 * <p>This replaces the legacy 1.8 NMS reflection that the 1.8.8 build used. {@code CC.color}
 * produces a section-sign ({@code §}) coloured string, which is deserialized into an Adventure
 * {@link Component} before being sent.
 */
public final class ActionBarUtil {

    private ActionBarUtil() {
    }

    public static void send(Player player, String message) {
        if (player == null || message == null || message.trim().isEmpty()) return;
        try {
            Component component = LegacyComponentSerializer.legacySection().deserialize(CC.color(message));
            player.sendActionBar(component);
        } catch (Exception ignored) {
            // Action bars are a visual extra; chat messages already carry the important feedback.
        }
    }
}
