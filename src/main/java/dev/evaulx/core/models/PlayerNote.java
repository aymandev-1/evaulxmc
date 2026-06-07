package dev.evaulx.core.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class PlayerNote {

    private String id;
    private UUID target;
    private String targetName;
    private String issuerName;
    private String note;
    private long createdAt;

    public PlayerNote(UUID target, String targetName, String issuerName, String note) {
        this.id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.target = target;
        this.targetName = targetName;
        this.issuerName = issuerName;
        this.note = note;
        this.createdAt = System.currentTimeMillis();
    }

    public String getFormattedTime() {
        return new SimpleDateFormat("MM/dd/yy HH:mm").format(new Date(createdAt));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public UUID getTarget() { return target; }
    public void setTarget(UUID target) { this.target = target; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getIssuerName() { return issuerName; }
    public void setIssuerName(String issuerName) { this.issuerName = issuerName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
