package com.karelisio.azur.domain

import com.karelisio.azur.Fixtures
import com.karelisio.azur.data.net.OpenMeteoParser
import com.karelisio.azur.domain.TestForecasts.forecast
import com.karelisio.azur.domain.TestForecasts.hour
import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryGeneratorTest {

    @Test fun realFixtureOvercastThenClearingTonight() {
        val f = OpenMeteoParser.parseForecast(Fixtures.read("forecast_grenoble_mf.json"))
        assertEquals("Ciel couvert, éclaircies ce soir", SummaryGenerator.generate(f))
    }

    @Test fun rainLaterTodayThenClearing() {
        // 13 h → 15 h couvert, pluie 16 h-18 h, couvert 19 h, éclaircies 20 h.
        val hours = (0..17).map { i ->
            when (i) {
                in 3..5 -> hour(i, code = 63, mm = 1.5)
                6 -> hour(i, code = 3)
                in 7..17 -> hour(i, code = 1)
                else -> hour(i, code = 3)
            }
        }
        assertEquals("Pluie vers 16 h, éclaircies ce soir", SummaryGenerator.generate(forecast(hours)))
    }

    @Test fun rainInTheNextHourFromMinutely() {
        val hours = (0..17).map { i -> if (i in 1..2) hour(i, code = 61, mm = 0.8) else hour(i, code = 3) }
        val f = forecast(hours, minutely = listOf(0.0, 0.0, 0.3, 0.6, 0.6, 0.6, 0.6, 0.6))
        assertEquals("Pluie vers 14 h 15", SummaryGenerator.generate(f))
    }

    @Test fun ongoingRainUntil() {
        val hours = (0..17).map { i -> if (i <= 2) hour(i, code = 63, mm = 2.0) else if (i == 3) hour(i, code = 3) else hour(i, code = 0) }
        val f = forecast(hours, minutely = List(8) { 0.6 })
        assertEquals("Pluie jusqu'à 16 h, éclaircies cet après-midi", SummaryGenerator.generate(f))
    }

    @Test fun snowTomorrowMorning() {
        val hours = (0..23).map { i -> if (i in 19..22) hour(i, code = 73, mm = 1.0, temp = -1.0) else hour(i, code = 3) }
        // 13 h + 19 h = 8 h le lendemain
        assertEquals("Neige demain matin", SummaryGenerator.generate(forecast(hours)))
    }

    @Test fun drySunnyAndWindy() {
        val hours = (0..17).map { i -> hour(i, code = 0, gust = if (i == 4) 72.0 else 20.0) }
        assertEquals("Temps sec et ensoleillé, rafales jusqu'à 72 km/h", SummaryGenerator.generate(forecast(hours, currentCode = 0)))
    }

    @Test fun clearNightAndMixed() {
        val clear = (0..17).map { i -> hour(i, code = 1) }
        assertEquals("Nuit claire, temps sec", SummaryGenerator.generate(forecast(clear, currentCode = 1, isDay = false)))
        val mixed = (0..17).map { i -> hour(i, code = if (i % 2 == 0) 2 else 3) }
        assertEquals("Alternance de nuages et d'éclaircies", SummaryGenerator.generate(forecast(mixed, currentCode = 2)))
    }

    @Test fun stormsWithProbability() {
        val hours = (0..17).map { i -> if (i == 4) hour(i, code = 95, mm = 0.0, prob = 80.0) else hour(i, code = 2) }
        assertEquals("Orages vers 17 h, éclaircies ce soir", SummaryGenerator.generate(forecast(hours, currentCode = 2)))
    }
}
