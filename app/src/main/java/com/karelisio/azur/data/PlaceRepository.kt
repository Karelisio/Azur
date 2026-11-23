package com.karelisio.azur.data

import com.karelisio.azur.data.db.AzurDatabase
import com.karelisio.azur.data.db.PlaceEntity
import com.karelisio.azur.data.net.OpenMeteoApi
import com.karelisio.azur.data.net.OpenMeteoParser
import kotlinx.coroutines.flow.Flow

class PlaceRepository(private val db: AzurDatabase, private val api: OpenMeteoApi) {
    val places: Flow<List<PlaceEntity>> = db.places().observeAll()

    suspend fun all(): List<PlaceEntity> = db.places().all()
    suspend fun byId(id: Long): PlaceEntity? = db.places().byId(id)
    suspend fun current(): PlaceEntity? = db.places().current()

    /** Lieu utilisé par les widgets, la tuile et les notifications : position actuelle sinon 1er favori. */
    suspend fun primary(): PlaceEntity? = db.places().current() ?: db.places().all().firstOrNull()

    suspend fun search(query: String): List<OpenMeteoParser.GeoResult> =
        if (query.trim().length < 2) emptyList()
        else api.search(query).sortedByDescending { if (it.country_code == "FR") 1 else 0 }

    suspend fun addFavorite(geo: OpenMeteoParser.GeoResult): Long = db.places().insert(
        PlaceEntity(
            name = geo.name,
            region = listOfNotNull(geo.admin2, geo.admin1).distinct().joinToString(", ").ifBlank { null },
            country = geo.country,
            countryCode = geo.country_code,
            latitude = geo.latitude,
            longitude = geo.longitude,
            elevation = geo.elevation,
            department = geo.department,
            sortOrder = db.places().maxOrder() + 1,
        ),
    )

    suspend fun remove(id: Long) {
        db.places().delete(id)
        db.cache().delete(id)
    }

    suspend fun reorder(ids: List<Long>) = db.places().reorder(ids)

    /** Met à jour (ou crée) la ligne « position actuelle » en conservant son identifiant. */
    suspend fun updateCurrent(lat: Double, lon: Double, name: String?, region: String?, department: String?): PlaceEntity {
        val existing = db.places().current()
        val entity = (existing ?: PlaceEntity(name = "", latitude = lat, longitude = lon, isCurrent = true)).copy(
            name = name ?: existing?.name?.ifBlank { null } ?: "Position actuelle",
            region = region ?: existing?.region,
            department = department ?: existing?.department,
            latitude = lat,
            longitude = lon,
            isCurrent = true,
            sortOrder = 0,
        )
        val id = db.places().insert(entity)
        return entity.copy(id = id)
    }
}
