package dev.evaulx.core.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TimeUtilTest {

    private static final long TOLERANCE_MS = 2_000L;

    private long offset(String input) {
        long expires = TimeUtil.parseDuration(input);
        assertTrue("expected a future timestamp for '" + input + "'", expires > 0);
        return expires - System.currentTimeMillis();
    }

    private void assertOffset(long expectedMs, String input) {
        long diff = Math.abs(offset(input) - expectedMs);
        assertTrue("'" + input + "' parsed to the wrong duration (off by " + diff + "ms)",
                diff <= TOLERANCE_MS);
    }

    @Test
    public void permanentReturnsMinusOne() {
        assertEquals(-1L, TimeUtil.parseDuration("perm"));
        assertEquals(-1L, TimeUtil.parseDuration("permanent"));
        assertEquals(-1L, TimeUtil.parseDuration("-1"));
        assertEquals(-1L, TimeUtil.parseDuration(""));
        assertEquals(-1L, TimeUtil.parseDuration(null));
    }

    @Test
    public void parsesBasicUnits() {
        assertOffset(30_000L, "30s");
        assertOffset(60_000L, "1m");
        assertOffset(3_600_000L, "1h");
        assertOffset(86_400_000L, "1d");
        assertOffset(604_800_000L, "1w");
        assertOffset(31_536_000_000L, "1y");
    }

    @Test
    public void lowercaseMisMinutesAndUppercaseMisMonths() {
        // Regression: 'M' (months) used to be unreachable because the input was lowercased,
        // so "1M" was silently parsed as "1m" (one minute). They must now differ.
        assertOffset(60_000L, "1m");              // minutes
        assertOffset(2_592_000_000L, "1M");       // months (30 days)
        assertTrue("months must be far longer than minutes",
                TimeUtil.parseDuration("1M") - TimeUtil.parseDuration("1m") > 2_000_000_000L);
    }

    @Test
    public void combinesMultipleUnits() {
        assertOffset(86_400_000L + 3_600_000L + 60_000L, "1d1h1m");
    }

    @Test
    public void unitsAreCaseInsensitiveExceptM() {
        assertOffset(3_600_000L, "1H");
        assertOffset(86_400_000L, "1D");
        assertOffset(30_000L, "30S");
    }
}
