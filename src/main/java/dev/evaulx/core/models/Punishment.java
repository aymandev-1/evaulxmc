package dev.evaulx.core.models;

import java.util.UUID;

public class Punishment {

    public enum Type {
        BAN, TEMPBAN, IPBAN, MUTE, TEMPMUTE, KICK, WARN, BLACKLIST;

        public boolean isBan() { return this == BAN || this == TEMPBAN || this == IPBAN || this == BLACKLIST; }
        public boolean isMute() { return this == MUTE || this == TEMPMUTE; }
        public boolean isPermanent() { return this == BAN || this == MUTE || this == BLACKLIST; }
    }

    private String id;
    private UUID target;
    private String targetName;
    private String targetIp;
    private UUID punisher;
    private String punisherName;
    private Type type;
    private String reason;
    private long issued;
    private long expires;
    private boolean active;
    private boolean silent;
    private UUID removedBy;
    private String removedReason;
    private long removedAt;
    private String evidenceUrl;
    private String appealStatus;
    private String staffNote;
    private String internalNote;

    public Punishment(UUID target, String targetName, UUID punisher, String punisherName,
                      Type type, String reason, long expires, boolean silent) {
        this.id = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.target = target;
        this.targetName = targetName;
        this.punisher = punisher;
        this.punisherName = punisherName;
        this.type = type;
        this.reason = reason;
        this.issued = System.currentTimeMillis();
        this.expires = expires;
        this.active = true;
        this.silent = silent;
        this.appealStatus = "not-submitted";
    }

    public boolean isExpired() {
        return dev.evaulx.core.utils.TimeUtil.isExpired(expires);
    }

    public boolean isActive() {
        return active && !isExpired();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public UUID getTarget() { return target; }
    public String getTargetName() { return targetName; }

    public String getTargetIp() { return targetIp; }
    public void setTargetIp(String ip) { this.targetIp = ip; }

    public UUID getPunisher() { return punisher; }
    public String getPunisherName() { return punisherName; }

    public Type getType() { return type; }
    public String getReason() { return reason; }

    public long getIssued() { return issued; }
    public void setIssued(long issued) { this.issued = issued; }

    public long getExpires() { return expires; }
    public void setExpires(long expires) { this.expires = expires; }

    public void setActive(boolean active) { this.active = active; }
    public boolean isSilent() { return silent; }

    public UUID getRemovedBy() { return removedBy; }
    public void setRemovedBy(UUID removedBy) { this.removedBy = removedBy; }

    public String getRemovedReason() { return removedReason; }
    public void setRemovedReason(String reason) { this.removedReason = reason; }

    public long getRemovedAt() { return removedAt; }
    public void setRemovedAt(long removedAt) { this.removedAt = removedAt; }

    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }

    public String getAppealStatus() { return appealStatus == null ? "not-submitted" : appealStatus; }
    public void setAppealStatus(String appealStatus) { this.appealStatus = appealStatus == null ? "not-submitted" : appealStatus; }

    public String getStaffNote() { return staffNote; }
    public void setStaffNote(String staffNote) { this.staffNote = staffNote; }

    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }

    public String getDurationString() {
        return dev.evaulx.core.utils.TimeUtil.formatDuration(expires);
    }
}
