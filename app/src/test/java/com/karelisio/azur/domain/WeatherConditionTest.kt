package com.karelisio.azur.domain

import com.karelisio.azur.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherConditionTest {
    @Test fun wmoMapping() {
        assertEquals(WeatherCondition.CLEAR, WeatherCondition.fromWmo(0))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherCondition.fromWmo(2))
        assertEquals(WeatherCondition.OVERCAST, WeatherCondition.fromWmo(3))
        assertEquals(WeatherCondition.FOG, WeatherCondition.fromWmo(45))
        assertEquals(WeatherCondition.DRIZZLE, WeatherCondition.fromWmo(53))
        assertEquals(WeatherCondition.FREEZING_RAIN, WeatherCondition.fromWmo(67))
        assertEquals(WeatherCondition.SNOW_HEAVY, WeatherCondition.fromWmo(75))
        assertEquals(WeatherCondition.SHOWERS_HEAVY, WeatherCondition.fromWmo(82))
        assertEquals(WeatherCondition.THUNDERSTORM_HAIL, WeatherCondition.fromWmo(99))
        assertEquals(WeatherCondition.UNKNOWN, WeatherCondition.fromWmo(42))
        assertEquals(WeatherCondition.UNKNOWN, WeatherCondition.fromWmo(null))
    }

    @Test fun iconsDayNight() {
        assertEquals(R.drawable.ic_wx_clear_day, WeatherCondition.fromWmo(0).icon.res(isDay = true))
        assertEquals(R.drawable.ic_wx_clear_night, WeatherCondition.fromWmo(0).icon.res(isDay = false))
        assertEquals(R.drawable.ic_wx_partly_night, WeatherCondition.fromWmo(2).icon.res(isDay = false))
        assertEquals(R.drawable.ic_wx_rain, WeatherCondition.fromWmo(63).icon.res(isDay = false))
        assertEquals(R.drawable.ic_wx_heavy_rain, WeatherCondition.fromWmo(65).icon.res(true))
        assertEquals(R.drawable.ic_wx_snow, WeatherCondition.fromWmo(71).icon.res(true))
        assertEquals(R.drawable.ic_wx_sleet, WeatherCondition.fromWmo(56).icon.res(true))
        assertEquals(R.drawable.ic_wx_storm, WeatherCondition.fromWmo(95).icon.res(true))
        assertEquals(R.drawable.ic_wx_fog, WeatherCondition.fromWmo(48).icon.res(true))
    }

    @Test fun everyKnownCodeHasLabelAndSky() {
        val codes = listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99)
        codes.forEach { code ->
            val c = WeatherCondition.fromWmo(code)
            assertFalse("code $code", c == WeatherCondition.UNKNOWN)
            assertTrue(c.label.isNotBlank())
        }
        assertTrue(WeatherCondition.fromWmo(61).isWet)
        assertFalse(WeatherCondition.fromWmo(3).isWet)
        assertEquals(SkyKind.STORM, WeatherCondition.fromWmo(95).sky)
    }
}
