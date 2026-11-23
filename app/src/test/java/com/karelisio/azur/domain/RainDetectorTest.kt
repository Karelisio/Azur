package com.karelisio.azur.domain

import com.karelisio.azur.domain.TestForecasts.NOW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RainDetectorTest {
    private fun slots(vararg mm: Double) = mm.mapIndexed { i, v -> MinutelyPoint(NOW + i * 900L, v) }

    @Test fun dry() {
        val r = RainDetector.detect(slots(0.0, 0.0, 0.05, 0.0, 3.0), NOW)
        assertEquals(RainState.DRY, r.state) // le 5e quart d'heure est hors de l'heure
        assertEquals(4, r.slots.size)
        assertFalse(r.rainExpected)
    }

    @Test fun starting() {
        val r = RainDetector.detect(slots(0.0, 0.0, 0.4, 0.8), NOW)
        assertEquals(RainState.STARTING, r.state)
        assertEquals(NOW + 1800, r.startsAt)
        assertEquals(null, r.endsAt)
        assertEquals(0.8, r.maxMm, 0.001)
        assertEquals("modérée", r.intensityLabel)
        assertTrue(r.rainExpected)
    }

    @Test fun startingThenStopping() {
        val r = RainDetector.detect(slots(0.0, 0.2, 0.0, 0.0), NOW)
        assertEquals(RainState.STARTING, r.state)
        assertEquals(NOW + 900, r.startsAt)
        assertEquals(NOW + 1800, r.endsAt)
        assertEquals("faible", r.intensityLabel)
    }

    @Test fun ongoingAndStopping() {
        assertEquals(RainState.ONGOING, RainDetector.detect(slots(0.5, 0.5, 2.5, 0.3), NOW).state)
        val stop = RainDetector.detect(slots(0.5, 0.2, 0.0, 0.0), NOW)
        assertEquals(RainState.STOPPING, stop.state)
        assertEquals(NOW + 1800, stop.endsAt)
        assertEquals("forte", RainDetector.detect(slots(0.5, 2.5, 0.0, 0.0), NOW).intensityLabel)
    }

    @Test fun pastSlotsAreIgnored() {
        // Le créneau de 13 h 30 (fini à 13 h 45) ne compte plus.
        val minutely = listOf(MinutelyPoint(NOW - 900, 5.0)) + slots(0.0, 0.0, 0.0, 0.0)
        assertEquals(RainState.DRY, RainDetector.detect(minutely, NOW).state)
        assertEquals(RainState.DRY, RainDetector.detect(emptyList(), NOW).state)
    }
}
