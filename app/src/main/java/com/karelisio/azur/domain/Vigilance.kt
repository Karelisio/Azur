package com.karelisio.azur.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

@Serializable
data class VigilancePhenomenon(val id: Int, val level: Int) {
    val name: String get() = PHENOMENA[id] ?: "Phénomène $id"

    companion object {
        val PHENOMENA = mapOf(
            1 to "Vent violent", 2 to "Pluie-inondation", 3 to "Orages", 4 to "Crues", 5 to "Neige-verglas",
            6 to "Canicule", 7 to "Grand froid", 8 to "Avalanches", 9 to "Vagues-submersion",
        )
    }
}

/** Vigilance Météo-France d'un département (niveau 1 vert, 2 jaune, 3 orange, 4 rouge). */
@Serializable
data class Vigilance(
    val department: String,
    val level: Int,
    val phenomena: List<VigilancePhenomenon>,
    val tomorrowLevel: Int? = null,
    val updated: String? = null,
) {
    val levelName: String get() = levelName(level)

    /** Phénomènes au moins jaunes, du plus fort au plus faible. */
    val active: List<VigilancePhenomenon> get() = phenomena.filter { it.level >= 2 }.sortedByDescending { it.level }

    companion object {
        fun levelName(level: Int) = when (level) {
            4 -> "rouge"
            3 -> "orange"
            2 -> "jaune"
            else -> "verte"
        }
    }
}

/** Analyse de la carte de vigilance en cours (API « Données Publiques Vigilance » de Météo-France). */
object VigilanceParser {
    private fun JsonElement?.obj() = this as? JsonObject
    private fun JsonElement?.arr() = this as? JsonArray
    private fun JsonElement?.int() = (this as? JsonPrimitive)?.let { it.intOrNull ?: it.content.toIntOrNull() }
    private fun JsonElement?.str() = (this as? JsonPrimitive)?.content

    private fun domainIn(period: JsonObject?, department: String): JsonObject? =
        period?.get("timelaps").obj()?.get("domain_ids").arr()
            ?.mapNotNull { it.obj() }
            ?.firstOrNull { it["domain_id"].str().equals(department, ignoreCase = true) }

    fun parse(json: String, department: String): Vigilance? {
        val root = com.karelisio.azur.data.net.AzurJson.parseToJsonElement(json).jsonObject
        val product = root["product"].obj() ?: return null
        val periods = product["periods"].arr()?.mapNotNull { it.obj() } ?: return null
        val today = periods.firstOrNull { it["echeance"].str() == "J" } ?: periods.firstOrNull()
        val tomorrow = periods.firstOrNull { it["echeance"].str() == "J1" }
        val domain = domainIn(today, department) ?: return null
        val phenomena = domain["phenomenon_items"].arr()?.mapNotNull { item ->
            val o = item.obj() ?: return@mapNotNull null
            val id = o["phenomenon_id"].int() ?: return@mapNotNull null
            VigilancePhenomenon(id, o["phenomenon_max_color_id"].int() ?: 1)
        }.orEmpty()
        return Vigilance(
            department = department,
            level = domain["max_color_id"].int() ?: phenomena.maxOfOrNull { it.level } ?: 1,
            phenomena = phenomena,
            tomorrowLevel = domainIn(tomorrow, department)?.get("max_color_id").int(),
            updated = product["update_time"].str(),
        )
    }
}
