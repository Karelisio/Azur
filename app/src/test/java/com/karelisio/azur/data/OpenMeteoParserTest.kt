package com.karelisio.azur.data

import com.karelisio.azur.Fixtures
import com.karelisio.azur.data.net.OpenMeteoApi
import com.karelisio.azur.data.net.OpenMeteoParser
import com.karelisio.azur.data.net.departmentFromPostcode
import com.karelisio.azur.domain.AqiLevel
import com.karelisio.azur.domain.Pollen
import com.karelisio.azur.domain.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoParserTest {

    private val forecast by lazy { OpenMeteoParser.parseForecast(Fixtures.read("forecast_grenoble_mf.json")) }

    @Test fun current() {
        val c = forecast.current
        assertEquals(1795437900L, c.time)
        assertEquals(11.6, c.temperature, 0.001)
        assertEquals(10.2, c.apparentTemperature!!, 0.001)
        assertEquals(WeatherCondition.OVERCAST, c.condition)
        assertEquals(1022.3, c.pressure!!, 0.001)
        assertEquals(291.0, c.windDirection!!, 0.001)
        assertTrue(c.isDay)
        assertNull(c.visibility)
        assertNull(c.uvIndex)
        assertEquals("Europe/Paris", forecast.timezone)
        assertEquals(214.0, forecast.elevation, 0.001)
    }

    @Test fun hourlyDropsTrailingNulls() {
        // Météo-France ne couvre que 110 heures : les pas sans température sont ignorés.
        assertEquals(110, forecast.hourly.size)
        val first = forecast.hourly.first()
        assertEquals(1795388400L, first.time)
        assertEquals(10.6, first.temperature, 0.001)
        assertEquals(1.2, first.precipitation, 0.001)
        assertEquals(63, first.weatherCode)
        assertNull(first.precipitationProbability)
        assertEquals(1.1, first.temperature850!!, 0.001)
        assertEquals(3017.0, first.geopotential700!!, 0.001)
        assertNull(first.freezingLevel)
        assertTrue(forecast.hourly.zipWithNext().all { (a, b) -> b.time - a.time == 3600L })
    }

    @Test fun dailyAndMinutely() {
        assertEquals(4, forecast.daily.size)
        val d0 = forecast.daily[0]
        assertEquals(12.3, d0.temperatureMax, 0.001)
        assertEquals(5.1, d0.temperatureMin, 0.001)
        assertEquals(1795416341L, d0.sunrise)
        assertEquals(1795449657L, d0.sunset)
        assertEquals(45.4, forecast.daily[2].precipitationSum!!, 0.001)
        assertEquals(8, forecast.minutely.size)
        assertTrue(forecast.minutely.all { it.precipitation == 0.0 })
        assertEquals(24, forecast.hoursOfDay(d0).size)
    }

    @Test fun multiModelMergePrefersMeteoFrance() {
        val json = """
        {"latitude":45.19,"longitude":5.72,"elevation":214,"timezone":"Europe/Paris","utc_offset_seconds":3600,
         "current":{"time":100,"temperature_2m_meteofrance_seamless":5.0,"temperature_2m_best_match":6.0,
                    "uv_index_meteofrance_seamless":null,"uv_index_best_match":2.5,"weather_code_meteofrance_seamless":61},
         "hourly":{"time":[0,3600,7200],
           "temperature_2m_meteofrance_seamless":[1.0,2.0,null],
           "temperature_2m_best_match":[9.0,9.0,3.5],
           "precipitation_probability_meteofrance_seamless":[null,null,null],
           "precipitation_probability_best_match":[10,40,80]},
         "daily":{"time":[0,86400],
           "temperature_2m_max_meteofrance_seamless":[8.0,null],"temperature_2m_max_best_match":[9.0,7.0],
           "temperature_2m_min_meteofrance_seamless":[1.0,null],"temperature_2m_min_best_match":[0.0,-1.0]}}
        """.trimIndent()
        val f = OpenMeteoParser.parseForecast(json)
        assertEquals(5.0, f.current.temperature, 0.001)
        assertEquals(2.5, f.current.uvIndex!!, 0.001)
        assertEquals(WeatherCondition.RAIN_LIGHT, f.current.condition)
        assertEquals(listOf(1.0, 2.0, 3.5), f.hourly.map { it.temperature })
        assertEquals(listOf(10.0, 40.0, 80.0), f.hourly.map { it.precipitationProbability })
        assertEquals(listOf(8.0, 7.0), f.daily.map { it.temperatureMax })
    }

    @Test(expected = OpenMeteoParser.OpenMeteoError::class)
    fun apiErrorIsRaised() {
        OpenMeteoParser.parseForecast("""{"error":true,"reason":"Daily API request limit exceeded."}""")
    }

    @Test fun airQuality() {
        val aq = OpenMeteoParser.parseAirQuality(Fixtures.read("air_quality_grenoble.json"))
        assertEquals(21.0, aq.europeanAqi!!, 0.001)
        assertEquals(AqiLevel.FAIR, aq.aqiLevel)
        assertEquals(4.1, aq.pm25!!, 0.001)
        assertEquals(0, Pollen.GRASS.level(aq.pollen[Pollen.GRASS]))
        assertNull(aq.pollen[Pollen.OLIVE])
        assertEquals(3, Pollen.GRASS.level(120.0))
        assertEquals(4, Pollen.RAGWEED.level(150.0))
    }

    @Test fun geocoding() {
        val results = OpenMeteoParser.parseGeocoding(Fixtures.read("geocoding_grenoble.json"))
        assertEquals(3, results.size)
        assertEquals("Grenoble", results[0].name)
        assertEquals("Isère", results[0].admin2)
        assertEquals("38", results[0].department)
        assertNull(results[1].department)
        assertEquals("2A", departmentFromPostcode("20000"))
        assertEquals("2B", departmentFromPostcode("20200"))
        assertEquals("974", departmentFromPostcode("97411"))
        assertEquals(emptyList<OpenMeteoParser.GeoResult>(), OpenMeteoParser.parseGeocoding("""{"generationtime_ms":0.1}"""))
    }

    @Test fun forecastUrlRequestsBothModels() {
        val url = OpenMeteoApi.forecastUrl(45.19, 5.72)
        assertEquals("meteofrance_seamless,best_match", url.queryParameter("models"))
        assertEquals("unixtime", url.queryParameter("timeformat"))
        assertEquals("45.1900", url.queryParameter("latitude"))
        assertTrue(url.queryParameter("hourly")!!.contains("temperature_850hPa"))
    }
}

class RainViewerTest {
    @org.junit.Test fun tileUrl() {
        val json = """{"version":"2.0","generated":1795437000,"host":"https://tilecache.rainviewer.com",
            "radar":{"past":[{"time":1795436400,"path":"/v2/radar/abc"},{"time":1795437000,"path":"/v2/radar/def"}],"nowcast":[]},
            "satellite":{"infrared":[]}}"""
        val maps = com.karelisio.azur.data.net.AzurJson.decodeFromString(com.karelisio.azur.data.net.RainViewerMaps.serializer(), json)
        org.junit.Assert.assertEquals(2, maps.frames.size)
        org.junit.Assert.assertEquals("https://tilecache.rainviewer.com/v2/radar/def/256/{z}/{x}/{y}/2/1_1.png", maps.tileUrl(maps.frames[1]))
    }
}
