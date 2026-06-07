package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TaskUtil;
import dev.evaulx.core.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class PunishmentManager {

    private final EvaulxCore plugin;

    public PunishmentManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public Punishment punish(CommandSender issuer, UUID target, String targetName, String targetIp,
                              Punishment.Type type, String reason, long expires, boolean silent) {
        UUID issuerUUID = (issuer instanceof Player) ? ((Player) issuer).getUniqueId() : null;
        String issuerName = issuer.getName();

        Punishment pun = new Punishment(target, targetName, issuerUUID, issuerName, type, reason, expires, silent);
        pun.setTargetIp(targetIp);
        pun.setAppealStatus(plugin.getConfig().getString("punishments.default-appeal-status", "not-submitted"));

        TaskUtil.async(() -> plugin.getDatabaseManager().savePunishment(pun));

        // Announce
        if (!silent) {
            String msg = buildPunishMessage(pun);
            Bukkit.broadcastMessage(msg);
        } else {
            String msg = buildPunishMessage(pun);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("evaulx.staff")) p.sendMessage(CC.color("&8[Silent] ") + msg);
            }
        }

        // Discord webhook
        plugin.getDiscordManager().sendPunishment(pun);
        if (plugin.getRedisSyncManager() != null) plugin.getRedisSyncManager().publishPunishment(pun);

        // Kick/disconnect if needed
        if (type.isBan() || type == Punishment.Type.KICK) {
            Player p = Bukkit.getPlayer(target);
            if (p != null) {
                String kickMsg = buildKickScreen(pun);
                TaskUtil.sync(() -> p.kickPlayer(kickMsg));
            }
        } else if (type.isMute()) {
            Player p = Bukkit.getPlayer(target);
            if (p != null) p.sendMessage(CC.color(buildMuteMessage(pun)));
        }

        return pun;
    }

    public boolean removePunishment(UUID target, Punishment.Type type, CommandSender remover, String reason) {
        Punishment pun = plugin.getDatabaseManager().getActivePunishment(target, type);
        if (pun == null) return false;
        pun.setActive(false);
        pun.setRemovedBy(remover instanceof Player ? ((Player) remover).getUniqueId() : null);
        pun.setRemovedReason(reason);
        pun.setRemovedAt(System.currentTimeMillis());
        TaskUtil.async(() -> plugin.getDatabaseManager().updatePunishment(pun));
        return true;
    }

    public boolean isBanned(UUID uuid) {
        Punishment ban = plugin.getDatabaseManager().getActivePunishment(uuid, Punishment.Type.BAN);
        if (ban != null && !ban.isExpired()) return true;
        Punishment tempban = plugin.getDatabaseManager().getActivePunishment(uuid, Punishment.Type.TEMPBAN);
        if (tempban != null && !tempban.isExpired()) return true;
        Punishment blacklist = plugin.getDatabaseManager().getActivePunishment(uuid, Punishment.Type.BLACKLIST);
        return blacklist != null && !blacklist.isExpired();
    }

    public boolean isMuted(UUID uuid) {
        Punishment mute = plugin.getDatabaseManager().getActivePunishment(uuid, Punishment.Type.MUTE);
        if (mute != null && !mute.isExpired()) return true;
        Punishment tempmute = plugin.getDatabaseManager().getActivePunishment(uuid, Punishment.Type.TEMPMUTE);
        return tempmute != null && !tempmute.isExpired();
    }

    public Punishment getActiveBan(UUID uuid) {
        for (Punishment.Type t : new Punishment.Type[]{Punishment.Type.BLACKLIST, Punishment.Type.BAN, Punishment.Type.IPBAN, Punishment.Type.TEMPBAN}) {
            Punishment p = plugin.getDatabaseManager().getActivePunishment(uuid, t);
            if (p != null && !p.isExpired()) return p;
        }
        return null;
    }

    public Punishment getActiveMute(UUID uuid) {
        for (Punishment.Type t : new Punishment.Type[]{Punishment.Type.MUTE, Punishment.Type.TEMPMUTE}) {
            Punishment p = plugin.getDatabaseManager().getActivePunishment(uuid, t);
            if (p != null && !p.isExpired()) return p;
        }
        return null;
    }

    public List<Punishment> getHistory(UUID uuid) {
        return plugin.getDatabaseManager().getPunishments(uuid);
    }

    private String buildPunishMessage(Punishment pun) {
        String type = pun.getType().name().toLowerCase().replace("temp", "temp-").replace("ip", "IP ");
        return CC.color("&8[&cEvaulxMC&8] &f" + pun.getTargetName() + " &7has been &c" + type +
                "d &7by &f" + pun.getPunisherName() +
                " &7| Reason: &f" + pun.getReason() +
                (pun.getExpires() != -1 ? " &7| Duration: &f" + TimeUtil.formatDuration(pun.getExpires()) : "") +
                " &7| ID: &f" + pun.getId());
    }

    private String buildKickScreen(Punishment pun) {
        String template;
        if (pun.getType() == Punishment.Type.BLACKLIST) {
            template = plugin.getConfig().getString("punishments.blacklist-message", "&4Blacklisted.\n&7Reason: &f{reason}");
        } else if (pun.getType() == Punishment.Type.KICK) {
            template = plugin.getConfig().getString("punishments.kick-message", "&cKicked.\n&7Reason: &f{reason}");
        } else {
            template = plugin.getConfig().getString("punishments.ban-message",
                    "&cBanned.\n&7Reason: &f{reason}\n&7Duration: &f{duration}");
        }
        return CC.color(template
                .replace("{reason}", pun.getReason())
                .replace("{duration}", TimeUtil.formatDuration(pun.getExpires()))
                .replace("{appeal_status}", pun.getAppealStatus())
                .replace("{evidence}", pun.getEvidenceUrl() == null ? "" : pun.getEvidenceUrl())
                .replace("{punisher}", pun.getPunisherName())
                .replace("{id}", pun.getId()));
    }

    private String buildMuteMessage(Punishment pun) {
        String template = plugin.getConfig().getString("punishments.mute-message",
                "&cYou have been muted. Reason: &f{reason} &cExpires: &f{duration}");
        return template
                .replace("{reason}", pun.getReason())
                .replace("{duration}", TimeUtil.formatDuration(pun.getExpires()))
                .replace("{appeal_status}", pun.getAppealStatus())
                .replace("{evidence}", pun.getEvidenceUrl() == null ? "" : pun.getEvidenceUrl())
                .replace("{id}", pun.getId());
    }
}
