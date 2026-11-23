package com.karelisio.azur.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {
    @Test fun parse() {
        assertEquals(SemVer(1, 2, 3), SemVer.parse("v1.2.3"))
        assertEquals(SemVer(1, 2, 0), SemVer.parse("1.2"))
        assertNull(SemVer.parse("abc"))
    }

    @Test fun newer() {
        assertTrue(isNewer("v0.2.0", "0.1.9"))
        assertTrue(isNewer("v1.0.0", "0.9.9"))
        assertFalse(isNewer("v0.1.0", "0.1.0"))
        assertFalse(isNewer("v0.0.9", "0.1.0"))
        assertFalse(isNewer("garbage", "0.1.0"))
    }
}
