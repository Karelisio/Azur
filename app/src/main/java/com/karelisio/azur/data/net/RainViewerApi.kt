package com.karelisio.azur.data.net

import kotlinx.serialization.Serializable

@Serializable
data class RainViewerMaps(val host: String, val radar: Radar = Radar()) {
    @Serializable data class Radar(val past: List<Frame> = emptyList(), val nowcast: List<Frame> = emptyList())
    @Serializable data class Frame(val time: Long, val path: String)

    /** Images passées puis prévision immédiate (si fournie). */
    val frames: List<Frame> get() = radar.past + radar.nowcast

    /** Tuiles 256 px, palette 2 (bleu universel), lissage et neige activés. */
    fun tileUrl(frame: Frame) = "$host${frame.path}/256/{z}/{x}/{y}/2/1_1.png"
}

class RainViewerApi(private val http: Http) {
    suspend fun maps(): RainViewerMaps =
        AzurJson.decodeFromString(RainViewerMaps.serializer(), http.get("https://api.rainviewer.com/public/weather-maps.json"))
}
