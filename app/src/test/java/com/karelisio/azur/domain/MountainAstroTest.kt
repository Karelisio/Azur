package com.karelisio.azur.domain

import com.karelisio.azur.Fixtures
import com.karelisio.azur.data.net.OpenMeteoParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MountainAstroTest {
    @Test fun freezingLevelFromPressureLevels() {
        val f = OpenMeteoParser.parseForecast(Fixtures.read("forecast_grenoble_mf.json"))
        val h0 = f.hourly.first() // T850 1,1 °C à 1481 m, T700 -8,1 °C à 3017 m
        val fl = Mountain.freezingLevel(h0, f.elevation)
        assertEquals(1664.7, fl, 1.0)
        assertEquals(1364.7, Mountain.snowLimit(fl), 1.0)
        assertEquals(1650, Mountain.round50(fl))
    }

    @Test fun freezingLevelFallbacks() {
        val surfaceOnly = HourlyPoint(time = 0, temperature = 6.5)
        assertEquals(1214.0, Mountain.freezingLevel(surfaceOnly, 214.0), 0.5)
        val model = HourlyPoint(time = 0, temperature = 5.0, freezingLevel = 2100.0)
        assertEquals(2100.0, Mountain.freezingLevel(model, 214.0), 0.001)
        val coldAloft = HourlyPoint(time = 0, temperature = 4.0, temperature850 = -2.0, geopotential850 = 1500.0,
            temperature700 = -10.0, geopotential700 = 3000.0)
        assertEquals(214 + 4.0 / 6.0 * (1500 - 214), Mountain.freezingLevel(coldAloft, 214.0), 0.5)
        assertEquals(0.0, Mountain.snowLimit(200.0), 0.001)
    }

    @Test fun moonPhase() {
        // Pleine lune du 24/11/2026 14 h 53 UTC
        val p = Moon.phase(1795531980L)
        assertTrue("phase=$p", p in 0.47..0.53)
        assertEquals("Pleine lune", Moon.name(p))
        assertTrue(Moon.illumination(p) > 0.98)
        assertEquals("Nouvelle lune", Moon.name(Moon.phase(947182440L)))
    }

    @Test fun sunFraction() {
        assertEquals(0.5f, Sun.dayFraction(150, 100, 200)!!, 0.001f)
        assertNull(Sun.dayFraction(50, 100, 200))
    }
}
