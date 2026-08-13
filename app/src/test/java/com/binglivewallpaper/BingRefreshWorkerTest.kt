package com.binglivewallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BingRefreshWorkerTest {

    @Test
    fun testInitialDelayToNextUtc() {
        val delay = BingRefreshWorker.initialDelayToNextUtc(9)
        assertTrue("Initial delay must be positive", delay > 0)
        assertTrue("Initial delay must be <= 24 hours", delay <= 86_400_000L)
    }

    @Test
    fun testGetTodayUtcDateStringFormat() {
        val dateStr = BingImageFetcher.getTodayUtcDateString()
        assertEquals("Date string must be 8 digits (YYYYMMDD)", 8, dateStr.length)
        assertTrue("Date string must contain only digits", dateStr.all { it.isDigit() })
    }
}
