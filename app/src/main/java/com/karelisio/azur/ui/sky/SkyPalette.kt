package com.karelisio.azur.ui.sky

import androidx.compose.ui.graphics.Color
import com.karelisio.azur.domain.SkyKind

enum class DayPhase { NIGHT, DAWN, DAY, DUSK }

/** Phase du jour : aube / crépuscule = ± 45 min autour du lever / coucher. */
fun dayPhase(now: Long, sunrise: Long?, sunset: Long?, isDay: Boolean): DayPhase {
    if (sunrise == null || sunset == null) return if (isDay) DayPhase.DAY else DayPhase.NIGHT
    val margin = 45 * 60
    return when {
        now in (sunrise - margin)..(sunrise + margin) -> DayPhase.DAWN
        now in (sunset - margin)..(sunset + margin) -> DayPhase.DUSK
        now in sunrise..sunset -> DayPhase.DAY
        else -> DayPhase.NIGHT
    }
}

data class SkyColors(val top: Color, val bottom: Color)

fun skyColors(phase: DayPhase, sky: SkyKind): SkyColors {
    val wet = sky == SkyKind.RAIN || sky == SkyKind.STORM
    val grey = sky != SkyKind.CLEAR
    return when (phase) {
        DayPhase.DAY -> when (sky) {
            SkyKind.CLEAR -> SkyColors(Color(0xFF2F7DE1), Color(0xFF86BEF5))
            SkyKind.CLOUDY -> SkyColors(Color(0xFF52708F), Color(0xFF9DB2C8))
            SkyKind.FOG -> SkyColors(Color(0xFF6E7F8E), Color(0xFFADB9C4))
            SkyKind.RAIN -> SkyColors(Color(0xFF43566C), Color(0xFF7C90A5))
            SkyKind.STORM -> SkyColors(Color(0xFF2A3546), Color(0xFF566476))
            SkyKind.SNOW -> SkyColors(Color(0xFF6A84A2), Color(0xFFB9CBDD))
        }
        DayPhase.DAWN -> if (grey) SkyColors(Color(0xFF4F587A), Color(0xFFB8978A)) else SkyColors(Color(0xFF39497A), Color(0xFFF3A27A))
        DayPhase.DUSK -> if (grey) SkyColors(Color(0xFF41466A), Color(0xFFA67A72)) else SkyColors(Color(0xFF28306A), Color(0xFFEE875C))
        DayPhase.NIGHT -> when {
            wet -> SkyColors(Color(0xFF111824), Color(0xFF2A3445))
            grey -> SkyColors(Color(0xFF161D2C), Color(0xFF323C50))
            else -> SkyColors(Color(0xFF070C22), Color(0xFF1C2858))
        }
    }
}
