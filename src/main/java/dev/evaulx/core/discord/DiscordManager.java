package dev.evaulx.core.discord;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Grant;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TaskUtil;
import dev.evaulx.core.utils.TimeUtil;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordManager {

    private final EvaulxCore plugin;

    public DiscordManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void sendPunishment(Punishment pun) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        String webhook = plugin.getConfig().getString("discord.webhooks.punishments", "");
        if (webhook == null || webhook.isEmpty()) return;

        int color = getColor(pun.getType());
        String title = pun.getType().name() + " | " + pun.getTargetName();
        String description = "**Player:** " + pun.getTargetName() + "\n" +
                "**Staff:** " + pun.getPunisherName() + "\n" +
                "**Reason:** " + pun.getReason() + "\n" +
                "**Appeal:** " + pun.getAppealStatus() + "\n" +
                (pun.getEvidenceUrl() == null || pun.getEvidenceUrl().isEmpty() ? "" : "**Evidence:** " + pun.getEvidenceUrl() + "\n") +
                "**Duration:** " + TimeUtil.formatDuration(pun.getExpires()) + "\n" +
                "**ID:** " + pun.getId();

        sendEmbed(webhook, title, description, color, headUrl(pun.getTarget(), pun.getTargetName()));
    }

    public void sendRankChange(String playerName, String oldRank, String newRank, String issuer) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        String webhook = plugin.getConfig().getString("discord.webhooks.rank-changes", "");
        if (webhook == null || webhook.isEmpty()) return;

        String title = "Rank Change | " + playerName;
        String description = "**Player:** " + playerName + "\n" +
                "**Old Rank:** " + oldRank + "\n" +
                "**New Rank:** " + newRank + "\n" +
                "**Changed By:** " + issuer;
        sendEmbed(webhook, title, description, 0x5B8AF5, headUrl(null, playerName));
    }

    public void sendGrant(Grant grant) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        String webhook = plugin.getConfig().getString("discord.webhooks.rank-changes", "");
        if (webhook == null || webhook.isEmpty()) return;

        String title = "Rank Grant | " + grant.getTargetName();
        String description = "**Player:** " + grant.getTargetName() + "\n" +
                "**Rank:** " + grant.getRankName() + "\n" +
                "**Duration:** " + grant.getDurationString() + "\n" +
                "**Granted By:** " + grant.getIssuerName() + "\n" +
                "**Reason:** " + grant.getReason() + "\n" +
                "**ID:** " + grant.getId();
        sendEmbed(webhook, title, description, 0x22C55E, headUrl(grant.getTarget(), grant.getTargetName()));
    }

    public void sendHelpOp(String playerName, String message) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        String webhook = plugin.getConfig().getString("discord.webhooks.helpop", "");
        if (webhook == null || webhook.isEmpty()) return;
        sendEmbed(webhook, "HelpOP | " + playerName, "**Player:** " + playerName + "\n**Message:** " + message, 0xF59E0B, headUrl(null, playerName));
    }

    public void sendReport(String reporter, String reported, String reason) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        String webhook = plugin.getConfig().getString("discord.webhooks.reports", "");
        if (webhook == null || webhook.isEmpty()) return;
        String description = "**Reporter:** " + reporter + "\n**Reported:** " + reported + "\n**Reason:** " + reason;
        sendEmbed(webhook, "Player Report | " + reported, description, 0xEF4444, headUrl(null, reported));
    }

    public void sendStaffChat(String playerName, String message) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        String webhook = plugin.getConfig().getString("discord.webhooks.staff-actions",
                plugin.getConfig().getString("discord.webhooks.staff-chat", ""));
        if (webhook == null || webhook.isEmpty()) return;
        sendEmbed(webhook, "Staff Chat | " + playerName, "**Message:** " + message, 0x38BDF8, headUrl(null, playerName));
    }

    public void sendStaffLog(String actor, String action, String target, String detail) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        String webhook = plugin.getConfig().getString("discord.webhooks.staff-actions",
                plugin.getConfig().getString("discord.webhooks.staff-chat", ""));
        if (webhook == null || webhook.isEmpty()) return;

        String description = "**Actor:** " + actor + "\n" +
                "**Action:** " + action + "\n" +
                "**Server:** " + plugin.getConfig().getString("server.server-id", "hub") + "\n" +
                (target != null ? "**Target:** " + target + "\n" : "") +
                "**Detail:** " + (detail == null || detail.isEmpty() ? "None" : detail);
        sendEmbed(webhook, "Staff Action", description, 0x94A3B8, headUrl(null, actor));
    }

    public void sendStaffServerEvent(String playerName, boolean joined, String server) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        String webhook = plugin.getConfig().getString("discord.webhooks.staff-actions",
                plugin.getConfig().getString("discord.webhooks.system", ""));
        if (webhook == null || webhook.isEmpty()) return;

        String description = "**Staff:** " + playerName + "\n" +
                "**Server:** " + server + "\n" +
                "**Event:** " + (joined ? "Joined" : "Left");
        sendEmbed(webhook, joined ? "Staff Joined Server" : "Staff Left Server", description, joined ? 0x22C55E : 0xEF4444, headUrl(null, playerName));
    }

    public void sendDisguise(String playerName, String disguiseName, String skinName, String rankName, boolean enabled) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        String webhook = plugin.getConfig().getString("discord.webhooks.staff-actions", "");
        if (webhook == null || webhook.isEmpty()) return;

        String description = "**Player:** " + playerName + "\n" +
                "**State:** " + (enabled ? "Disguised" : "Removed") + "\n" +
                "**Disguise:** " + (disguiseName == null || disguiseName.isEmpty() ? "None" : disguiseName) + "\n" +
                "**Skin:** " + (skinName == null || skinName.isEmpty() ? "None" : skinName) + "\n" +
                "**Rank:** " + (rankName == null || rankName.isEmpty() ? "None" : rankName) + "\n" +
                "**Server:** " + plugin.getConfig().getString("server.server-id", "hub");
        sendEmbed(webhook, enabled ? "Disguise Enabled" : "Disguise Removed", description, enabled ? 0xA855F7 : 0x64748B, headUrl(null, playerName));
    }

    public void sendFrozenLogout(String playerName) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        String webhook = plugin.getConfig().getString("discord.webhooks.security",
                plugin.getConfig().getString("discord.webhooks.staff-actions", ""));
        if (webhook == null || webhook.isEmpty()) return;

        String description = "**Player:** " + playerName + "\n" +
                "**Server:** " + plugin.getConfig().getString("server.server-id", "hub") + "\n" +
                "**Alert:** Player quit while frozen.";
        sendEmbed(webhook, "Frozen Logout", description, 0xDC2626, headUrl(null, playerName));
    }

    public void sendMaintenance(boolean enabled, String actor, String reason) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        String webhook = plugin.getConfig().getString("discord.webhooks.system",
                plugin.getConfig().getString("discord.webhooks.staff-actions", ""));
        if (webhook == null || webhook.isEmpty()) return;

        String description = "**Actor:** " + actor + "\n" +
                "**State:** " + (enabled ? "Enabled" : "Disabled") + "\n" +
                "**Server:** " + plugin.getConfig().getString("server.server-id", "hub") + "\n" +
                "**Reason:** " + (reason == null || reason.isEmpty() ? "None" : reason);
        sendEmbed(webhook, "Maintenance Mode", description, enabled ? 0xF59E0B : 0x22C55E, headUrl(null, actor));
    }

    public void sendSystemLog(String title, String detail, int color) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        String webhook = plugin.getConfig().getString("discord.webhooks.system",
                plugin.getConfig().getString("discord.webhooks.staff-actions", ""));
        if (webhook == null || webhook.isEmpty()) return;
        sendEmbed(webhook, title, "**Server:** " + plugin.getConfig().getString("server.server-id", "hub") + "\n" + detail, color);
    }

    private void sendEmbed(String webhookUrl, String title, String description, int color) {
        sendEmbed(webhookUrl, title, description, color, null);
    }

    /**
     * Builds a player-head image URL for Discord embeds from {@code discord.player-head.url}.
     * The template understands {@code {id}} (UUID when known, otherwise the name), {@code {uuid}}
     * and {@code {name}}. Returns {@code null} when the feature is disabled or no player is known,
     * so the embed simply renders without a thumbnail.
     */
    private String headUrl(java.util.UUID uuid, String name) {
        if (!plugin.getConfig().getBoolean("discord.player-head.enabled", true)) return null;
        boolean hasName = name != null && !name.trim().isEmpty();
        if (uuid == null && !hasName) return null;

        String template = plugin.getConfig().getString("discord.player-head.url", "https://mc-heads.net/avatar/{id}/100");
        if (template == null || template.trim().isEmpty()) return null;

        String id = uuid != null ? uuid.toString() : name;
        return template
                .replace("{id}", id)
                .replace("{uuid}", uuid != null ? uuid.toString() : (hasName ? name : ""))
                .replace("{name}", hasName ? name : (uuid != null ? uuid.toString() : ""));
    }

    private void sendEmbed(String webhookUrl, String title, String description, int color, String headUrl) {
        TaskUtil.async(() -> {
            try {
                String username = plugin.getConfig().getString("discord.username", "EvaulxMC");
                String thumbnail = (headUrl == null || headUrl.isEmpty())
                        ? ""
                        : ",\"thumbnail\":{\"url\":\"" + escape(headUrl) + "\"}";
                String json = "{\"username\":\"" + escape(username) + "\",\"embeds\":[{\"title\":\"" + escape(title) + "\",\"description\":\"" +
                        escape(description) + "\",\"color\":" + color + thumbnail +
                        ",\"footer\":{\"text\":\"" + escape(plugin.getConfig().getString("server.name", "EvaulxMC")) + "\"}}]}";
                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("User-Agent", "EvaulxMC/1.0");
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                try (OutputStream os = con.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                con.getResponseCode();
                con.disconnect();
            } catch (Exception ignored) {}
        });
    }

    private int getColor(Punishment.Type type) {
        switch (type) {
            case BAN: case IPBAN: case BLACKLIST: return 0xEF4444;
            case TEMPBAN: return 0xF59E0B;
            case MUTE: case TEMPMUTE: return 0xF97316;
            case KICK: return 0xEAB308;
            case WARN: return 0x3B82F6;
            default: return 0x6B7280;
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
