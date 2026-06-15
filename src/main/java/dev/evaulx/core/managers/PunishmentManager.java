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
            if (p != null) {
                String muteMsg = CC.color(buildMuteMessage(pun));
                for (String line : muteMsg.split("\n")) {
                    p.sendMessage(line);
                }
            }
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

    public int getActiveWarnCount(UUID uuid) {
        int count = 0;
        for (Punishment p : plugin.getDatabaseManager().getPunishments(uuid)) {
            if (p.getType() == Punishment.Type.WARN && p.isActive()) count++;
        }
        return count;
    }

    public int getTotalPunishmentCount(UUID uuid) {
        return plugin.getDatabaseManager().getPunishments(uuid).size();
    }

    public String getRiskLevel(UUID uuid) {
        int score = 0;
        for (Punishment p : plugin.getDatabaseManager().getPunishments(uuid)) {
            if (!p.isActive()) continue;
            switch (p.getType()) {
                case WARN:      score += 1;  break;
                case KICK:      score += 1;  break;
                case TEMPMUTE:  score += 2;  break;
                case MUTE:      score += 3;  break;
                case TEMPBAN:   score += 4;  break;
                case BAN:       score += 8;  break;
                case IPBAN:     score += 10; break;
                case BLACKLIST: score += 20; break;
                default: break;
            }
        }
        if (score >= 15) return "&4CRITICAL";
        if (score >= 8)  return "&cHIGH";
        if (score >= 3)  return "&eMEDIUM";
        if (score >= 1)  return "&aLOW";
        return "&7NONE";
    }

    private String buildPunishMessage(Punishment pun) {
        String server = plugin.getConfig().getString("server.name", "Server");
        String pastTense = pastTense(pun.getType());
        return CC.color("&8[&c" + server + "&8] &f" + pun.getTargetName() + " &7has been &c" + pastTense +
                " &7by &f" + pun.getPunisherName() +
                " &7| Reason: &f" + pun.getReason() +
                (pun.getExpires() != -1 ? " &7| Duration: &f" + TimeUtil.formatDuration(pun.getExpires()) : "") +
                " &7| ID: &f" + pun.getId());
    }

    private static String pastTense(Punishment.Type type) {
        switch (type) {
            case BAN:       return "banned";
            case TEMPBAN:   return "temp-banned";
            case IPBAN:     return "IP banned";
            case MUTE:      return "muted";
            case TEMPMUTE:  return "temp-muted";
            case BLACKLIST: return "blacklisted";
            case KICK:      return "kicked";
            case WARN:      return "warned";
            default:        return type.name().toLowerCase();
        }
    }

    private String buildKickScreen(Punishment pun) {
        String serverName = plugin.getConfig().getString("server.name", "EvaulxMC");
        String appeal = plugin.getConfig().getString("punishments.appeal-url", "evaulxmc.net/appeal");
        String template;
        if (pun.getType() == Punishment.Type.BLACKLIST) {
            template = plugin.getConfig().getString("punishments.blacklist-message",
                    "&4&m----------------------------------------\n" +
                    " \n" +
                    "&4&l   ✖ BLACKLISTED FROM " + serverName.toUpperCase() + "   \n" +
                    " \n" +
                    "&7Reason: &f{reason}\n" +
                    "&7Punisher: &f{punisher}\n" +
                    "&7Punishment ID: &8#&f{id}\n" +
                    " \n" +
                    "&4This is a permanent action with no appeal.\n" +
                    " \n" +
                    "&4&m----------------------------------------");
        } else if (pun.getType() == Punishment.Type.KICK) {
            template = plugin.getConfig().getString("punishments.kick-message",
                    "&8&m----------------------------------------\n" +
                    " \n" +
                    "&e&l   ⚠ You have been kicked from " + serverName + "   \n" +
                    " \n" +
                    "&7Reason: &f{reason}\n" +
                    "&7Kicked by: &f{punisher}\n" +
                    " \n" +
                    "&7Reconnect at &bmc.evaulxmc.net\n" +
                    " \n" +
                    "&8&m----------------------------------------");
        } else {
            template = plugin.getConfig().getString("punishments.ban-message",
                    "&8&m----------------------------------------\n" +
                    " \n" +
                    "&c&l   ✖ Banned from " + serverName + "   \n" +
                    " \n" +
                    "&7Reason: &f{reason}\n" +
                    "&7Duration: &f{duration}\n" +
                    "&7Punished by: &f{punisher}\n" +
                    "&7Punishment ID: &8#&f{id}\n" +
                    " \n" +
                    "&7Appeal status: &f{appeal_status}\n" +
                    "&7To appeal visit: &b{appeal}\n" +
                    " \n" +
                    "&8&m----------------------------------------");
        }
        return CC.color(template
                .replace("{reason}", pun.getReason())
                .replace("{duration}", TimeUtil.formatDuration(pun.getExpires()))
                .replace("{appeal}", appeal)
                .replace("{appeal_status}", pun.getAppealStatus() == null ? "not-submitted" : pun.getAppealStatus())
                .replace("{evidence}", pun.getEvidenceUrl() == null ? "" : pun.getEvidenceUrl())
                .replace("{punisher}", pun.getPunisherName())
                .replace("{id}", pun.getId()));
    }

    private String buildMuteMessage(Punishment pun) {
        String template = plugin.getConfig().getString("punishments.mute-message",
                " \n" +
                "&c&l   ✖ You have been muted   \n" +
                " \n" +
                "&7Reason: &f{reason}\n" +
                "&7Duration: &f{duration}\n" +
                "&7Mute ID: &8#&f{id}\n" +
                "&7Appeal status: &f{appeal_status}\n" +
                " ");
        return template
                .replace("{reason}", pun.getReason())
                .replace("{duration}", TimeUtil.formatDuration(pun.getExpires()))
                .replace("{appeal_status}", pun.getAppealStatus() == null ? "not-submitted" : pun.getAppealStatus())
                .replace("{evidence}", pun.getEvidenceUrl() == null ? "" : pun.getEvidenceUrl())
                .replace("{id}", pun.getId());
    }
}
