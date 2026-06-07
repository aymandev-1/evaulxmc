package dev.evaulx.core.models;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PunishmentTest {

    @Test
    public void storesAppealEvidenceAndNotes() {
        Punishment punishment = new Punishment(
                UUID.randomUUID(),
                "Target",
                UUID.randomUUID(),
                "Staff",
                Punishment.Type.TEMPBAN,
                "Cheating",
                System.currentTimeMillis() + 60_000L,
                false
        );

        assertEquals("not-submitted", punishment.getAppealStatus());
        assertNull(punishment.getEvidenceUrl());

        punishment.setAppealStatus("pending");
        punishment.setEvidenceUrl("https://example.com/evidence");
        punishment.setStaffNote("Shared with staff");
        punishment.setInternalNote("Internal context");

        assertEquals("pending", punishment.getAppealStatus());
        assertEquals("https://example.com/evidence", punishment.getEvidenceUrl());
        assertEquals("Shared with staff", punishment.getStaffNote());
        assertEquals("Internal context", punishment.getInternalNote());
    }
}
