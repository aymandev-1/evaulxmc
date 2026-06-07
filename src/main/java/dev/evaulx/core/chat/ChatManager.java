package dev.evaulx.core.chat;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerProfile;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class ChatManager {

    private final EvaulxCore plugin;
    private boolean chatMuted = false;
    private final Map<UUID, UUID> lastMessaged = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastChat = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessage = new ConcurrentHashMap<>();

    private static final Pattern LINK_PATTERN = Pattern.compile("(?i)(https?://|www\\.|[a-z0-9-]+\\.(com|net|org|gg|io|dev|xyz|me|co|us|uk))");

    public ChatManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public String formatChat(Player player, String message) {
        String format = plugin.getConfig().getString("chat.format", "{prefix}{player}{suffix}&f: {message}");
        PlayerProfile profile = plugin.getPlayerManager().getProfile(player);
        Rank rank = null;
        if (profile != null && profile.isDisguised() && profile.getDisguiseRank() != null) {
            rank = plugin.getRankManager().getRank(profile.getDisguiseRank());
        }
        if (rank == null && profile != null) rank = plugin.getRankManager().getRank(profile.getRankName());

        String prefix = rank != null ? rank.getPrefix() : "";
        String suffix = rank != null ? rank.getSuffix() : "";
        String color = rank != null ? rank.getColor() : "&f";
        String tag = profile != null && profile.getTag() != null && !profile.getTag().isEmpty() ? profile.getTag() + " " : "";
        String nameColor = profile != null && !profile.getNameColor().isEmpty() ? profile.getNameColor() : color;
        String chatColor = profile != null && !profile.getChatColor().isEmpty() ? profile.getChatColor() : "&f";
        String visibleName = plugin.getDisguiseManager().getVisibleName(player);

        return CC.color(format
                .replace("{tag}", tag)
                .replace("{prefix}", prefix)
                .replace("{suffix}", suffix)
                .replace("{color}", color)
                .replace("{namecolor}", nameColor)
                .replace("{chatcolor}", chatColor)
                .replace("{player}", visibleName)
                .replace("{rank}", rank != null ? rank.getName() : "default")
                .replace("{message}", chatColor + message));
    }

    public ModerationResult moderate(Player player, String rawMessage) {
        if (player.hasPermission("evaulx.chat.bypass")) {
            return ModerationResult.allowed(rawMessage);
        }

        String message = CC.strip(rawMessage).replaceAll("\\s+", " ").trim();
        if (message.isEmpty()) return ModerationResult.blocked("&cYou cannot send an empty message.");

        String spamBlock = checkSpam(player, message);
        if (spamBlock != null) return ModerationResult.blocked(spamBlock);

        String linkBlock = checkLinks(player, message);
        if (linkBlock != null) return ModerationResult.blocked(linkBlock);

        message = filterWords(message);
        message = normalizeCaps(message);
        message = highlightMentions(message);

        lastChat.put(player.getUniqueId(), System.currentTimeMillis());
        lastMessage.put(player.getUniqueId(), message.toLowerCase(Locale.ENGLISH));
        return ModerationResult.allowed(message);
    }

    public boolean isChatMuted() { return chatMuted; }
    public void setChatMuted(boolean muted) { this.chatMuted = muted; }

    public void setLastMessaged(UUID sender, UUID receiver) {
        lastMessaged.put(sender, receiver);
        lastMessaged.put(receiver, sender);
    }

    public UUID getLastMessaged(UUID uuid) { return lastMessaged.get(uuid); }

    private String checkSpam(Player player, String message) {
        if (!plugin.getConfig().getBoolean("chat.anti-spam.enabled", true)) return null;

        long cooldownMillis = (long) (plugin.getConfig().getDouble("chat.anti-spam.cooldown-seconds", 1.5D) * 1000D);
        Long last = lastChat.get(player.getUniqueId());
        if (last != null && cooldownMillis > 0L && System.currentTimeMillis() - last < cooldownMillis) {
            return plugin.getConfig().getString("chat.anti-spam.cooldown-message", "&cSlow down before chatting again.");
        }

        if (plugin.getConfig().getBoolean("chat.anti-spam.block-repeat", true)) {
            String previous = lastMessage.get(player.getUniqueId());
            if (previous != null && previous.equalsIgnoreCase(message)) {
                return plugin.getConfig().getString("chat.anti-spam.repeat-message", "&cDo not repeat the same message.");
            }
        }

        return null;
    }

    private String checkLinks(Player player, String message) {
        if (!plugin.getConfig().getBoolean("chat.links.block", false)) return null;
        if (player.hasPermission("evaulx.chat.links")) return null;
        if (!LINK_PATTERN.matcher(message).find()) return null;

        String lower = message.toLowerCase(Locale.ENGLISH);
        List<String> allowed = plugin.getConfig().getStringList("chat.links.allowed-domains");
        for (String domain : allowed) {
            if (domain != null && !domain.isEmpty() && lower.contains(domain.toLowerCase(Locale.ENGLISH))) {
                return null;
            }
        }

        return plugin.getConfig().getString("chat.links.block-message", "&cLinks are not allowed in chat.");
    }

    private String filterWords(String message) {
        if (!plugin.getConfig().getBoolean("chat.filter-enabled", false)) return message;
        for (String word : plugin.getConfig().getStringList("chat.filtered-words")) {
            if (word == null || word.trim().isEmpty()) continue;
            message = message.replaceAll("(?i)\\b" + Pattern.quote(word.trim()) + "\\b", repeat("*", word.trim().length()));
        }
        return message;
    }

    private String normalizeCaps(String message) {
        if (!plugin.getConfig().getBoolean("chat.caps.enabled", true)) return message;
        int minLength = plugin.getConfig().getInt("chat.caps.min-length", 8);
        if (message.length() < minLength) return message;

        int letters = 0;
        int caps = 0;
        for (char c : message.toCharArray()) {
            if (!Character.isLetter(c)) continue;
            letters++;
            if (Character.isUpperCase(c)) caps++;
        }

        if (letters == 0) return message;
        int maxPercent = plugin.getConfig().getInt("chat.caps.max-percent", 70);
        if ((caps * 100 / letters) <= maxPercent) return message;
        return message.substring(0, 1).toUpperCase(Locale.ENGLISH) + message.substring(1).toLowerCase(Locale.ENGLISH);
    }

    private String highlightMentions(String message) {
        if (!plugin.getConfig().getBoolean("chat.mentions.enabled", true)) return message;
        String color = plugin.getConfig().getString("chat.mentions.color", "&e");
        for (Player online : Bukkit.getOnlinePlayers()) {
            String token = "@" + online.getName();
            message = message.replace(token, color + token + "&f");
        }
        return message;
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) builder.append(value);
        return builder.toString();
    }

    public static class ModerationResult {
        private final boolean allowed;
        private final String message;

        private ModerationResult(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }

        public static ModerationResult allowed(String message) {
            return new ModerationResult(true, message);
        }

        public static ModerationResult blocked(String message) {
            return new ModerationResult(false, message);
        }

        public boolean isAllowed() { return allowed; }
        public String getMessage() { return message; }
    }
}
