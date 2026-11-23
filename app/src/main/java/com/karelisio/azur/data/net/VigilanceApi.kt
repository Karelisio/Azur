package com.karelisio.azur.data.net

import com.karelisio.azur.domain.Vigilance
import com.karelisio.azur.domain.VigilanceParser

/** API Météo-France « Données Publiques Vigilance » (clé gratuite saisie par l'utilisateur). */
class VigilanceApi(private val http: Http) {
    suspend fun fetch(apiKey: String, department: String): Vigilance? {
        val json = http.get(URL, mapOf("apikey" to apiKey, "accept" to "application/json"))
        return VigilanceParser.parse(json, department)
    }

    companion object {
        const val URL = "https://public-api.meteofrance.fr/public/DPVigilance/v1/cartevigilance/encours"
    }
}
