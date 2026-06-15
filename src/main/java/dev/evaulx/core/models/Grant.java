package dev.evaulx.core.models;

import dev.evaulx.core.utils.TimeUtil;

import java.util.UUID;

public class Grant {

    private String id;
    private UUID target;
    private String targetName;
    private String rankName;
    private String issuerName;
    private String reason;
    private long issuedAt;
    private long expiresAt;
    private boolean active;

    public Grant(UUID target, String targetName, String rankName, String issuerName, String reason, long expiresAt) {
        this.id = "GRT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        this.target = target;
        this.targetName = targetName;
        this.rankName = rankName;
        this.issuerName = issuerName;
        this.reason = reason;
        this.issuedAt = System.currentTimeMillis();
        this.expiresAt = expiresAt;
        this.active = true;
    }

    public boolean isExpired() {
        return TimeUtil.isExpired(expiresAt);
    }

    public boolean isActive() {
        return active && !isExpired();
    }

    public String getDurationString() {
        return TimeUtil.formatDuration(expiresAt);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public UUID getTarget() { return target; }
    public void setTarget(UUID target) { this.target = target; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getRankName() { return rankName; }
    public void setRankName(String rankName) { this.rankName = rankName; }

    public String getIssuerName() { return issuerName; }
    public void setIssuerName(String issuerName) { this.issuerName = issuerName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getIssuedAt() { return issuedAt; }
    public void setIssuedAt(long issuedAt) { this.issuedAt = issuedAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public boolean isStoredActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
