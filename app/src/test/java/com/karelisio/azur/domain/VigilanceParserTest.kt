package com.karelisio.azur.domain

import com.karelisio.azur.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VigilanceParserTest {
    private val json = Fixtures.read("vigilance_encours.json")

    @Test fun isere() {
        val v = VigilanceParser.parse(json, "38")!!
        assertEquals(3, v.level)
        assertEquals("orange", v.levelName)
        assertEquals(2, v.tomorrowLevel)
        assertEquals(listOf("Orages", "Pluie-inondation", "Avalanches"), v.active.map { it.name })
        assertEquals("2026-11-23T14:00:00Z", v.updated)
    }

    @Test fun corsicaAndUnknown() {
        val v = VigilanceParser.parse(json, "2a")!!
        assertEquals(1, v.level)
        assertEquals(emptyList<VigilancePhenomenon>(), v.active)
        assertNull(v.tomorrowLevel)
        assertNull(VigilanceParser.parse(json, "75"))
        assertNull(VigilanceParser.parse("""{"foo":1}""", "38"))
    }
}
