package com.example

import com.example.util.BanglaStrings
import com.example.util.DateTimeFormatters
import com.example.util.EnglishStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ExampleUnitTest {

    @Test
    fun `age formatting calculates days, weeks, months, and years accurately`() {
        val now = 1_760_000_000_000L

        val twelveDaysAgo = now - TimeUnit.DAYS.toMillis(12)
        assertEquals("12 days old", DateTimeFormatters.formatAgeFromDob(twelveDaysAgo, now, isBangla = false))

        val twentyEightDaysAgo = now - TimeUnit.DAYS.toMillis(28)
        assertEquals("4 weeks old", DateTimeFormatters.formatAgeFromDob(twentyEightDaysAgo, now, isBangla = false))

        val oneHundredFourDaysAgo = now - TimeUnit.DAYS.toMillis(104) // 3 months (90d) + 2 weeks (14d)
        assertEquals("3 months and 2 weeks old", DateTimeFormatters.formatAgeFromDob(oneHundredFourDaysAgo, now, isBangla = false))

        val fourHundredTwentyFiveDaysAgo = now - TimeUnit.DAYS.toMillis(425) // 1 year (365d) + 2 months (60d)
        assertEquals("1 year and 2 months old", DateTimeFormatters.formatAgeFromDob(fourHundredTwentyFiveDaysAgo, now, isBangla = false))
    }

    @Test
    fun `bilingual Bangla and English localization strings are complete`() {
        assertEquals("CareSIR", EnglishStrings.appName)
        assertEquals("CareSIR", BanglaStrings.appName)
        assertTrue(BanglaStrings.isBangla)
        assertEquals("হোম", BanglaStrings.navHome)
        assertEquals("রিমাইন্ডার", BanglaStrings.navReminders)
    }

    @Test
    fun `duration formatter formats mm_ss and hh_mm_ss`() {
        assertEquals("02:05", DateTimeFormatters.formatDurationSeconds(125L))
        assertEquals("01:01:01", DateTimeFormatters.formatDurationSeconds(3661L))
    }
}
