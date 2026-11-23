package com.karelisio.azur.work

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class WorkSchedulerTest {
    @Test fun morningDelay() {
        val now = LocalDateTime.of(2026, 11, 23, 6, 30)
        assertEquals(Duration.ofMinutes(30), WorkScheduler.delayUntil(7, 0, now))
        // Heure déjà passée : le lendemain.
        assertEquals(Duration.ofHours(23).plusMinutes(30), WorkScheduler.delayUntil(6, 0, now))
        assertEquals(Duration.ofDays(1), WorkScheduler.delayUntil(6, 30, now))
    }
}
