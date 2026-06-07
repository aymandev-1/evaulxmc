package dev.evaulx.core.models;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RankTest {

    @Test
    public void normalizesAllowedCategories() {
        Rank rank = new Rank("Youtuber");

        rank.setCategory("media");

        assertEquals("Media", rank.getCategory());
        assertTrue(Rank.isAllowedCategory("Store"));
    }

    @Test
    public void infersCategoriesFromRankFlagsAndNames() {
        Rank staff = new Rank("Admin");
        staff.setStaff(true);

        Rank media = new Rank("Twitch");
        Rank store = new Rank("VIP");
        Rank fallback = new Rank("Member");

        assertEquals("Staff", Rank.inferCategory(staff));
        assertEquals("Media", Rank.inferCategory(media));
        assertEquals("Store", Rank.inferCategory(store));
        assertEquals("Default", Rank.inferCategory(fallback));
    }
}
