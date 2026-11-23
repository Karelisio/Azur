package com.karelisio.azur.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.karelisio.azur.MainActivity
import com.karelisio.azur.widget.WidgetData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Tuile Réglages rapides : la température actuelle sert d'icône. */
class TemperatureTileService : TileService() {
    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main).also { scope = it }
        s.launch {
            val tile = qsTile ?: return@launch
            val snap = runCatching { WidgetData.load(applicationContext) }.getOrNull()
            if (snap == null) {
                tile.label = "Azur"
                tile.subtitle = "Ouvrir l'appli"
                tile.state = Tile.STATE_INACTIVE
            } else {
                val cur = snap.weather.forecast.current
                val value = snap.units.tempValue(cur.temperature).roundToInt()
                tile.icon = Icon.createWithBitmap(temperatureBitmap("$value°"))
                tile.label = "$value° · ${cur.condition.label}"
                tile.subtitle = snap.place.name
                tile.contentDescription = "${snap.place.name} : $value degrés, ${cur.condition.label}"
                tile.state = Tile.STATE_ACTIVE
            }
            tile.updateTile()
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated") // seule API disponible avant Android 14
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    /** Dessine le texte en blanc (le système teinte l'icône). */
    private fun temperatureBitmap(text: String): Bitmap {
        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = if (text.length > 3) 46f else 60f
        }
        val canvas = Canvas(bmp)
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(text, size / 2f, y, paint)
        return bmp
    }
}
