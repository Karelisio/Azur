package com.karelisio.azur.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karelisio.azur.Fixtures
import com.karelisio.azur.container
import com.karelisio.azur.data.WidgetBackground
import com.karelisio.azur.data.db.ForecastCacheEntity
import com.karelisio.azur.data.db.PlaceEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Chaque widget se compose, s'applique en vue et se dessine avec les données du cache. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "xhdpi")
class WidgetRenderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before fun seed() = runBlocking {
        val c = context.container
        val id = c.database.places().insert(PlaceEntity(name = "Grenoble", latitude = 45.19, longitude = 5.72, isCurrent = true))
        c.database.cache().upsert(
            ForecastCacheEntity(id, Fixtures.read("forecast_grenoble_mf.json"), System.currentTimeMillis(),
                airJson = Fixtures.read("air_quality_grenoble.json")),
        )
    }

    private fun render(widget: GlanceAppWidget, name: String, size: DpSize) = runBlocking {
        val rv = widget.compose(context, size = size)
        val parent = FrameLayout(context)
        val view = rv.apply(context, parent)
        val density = context.resources.displayMetrics.density
        val w = (size.width.value * density).toInt()
        val h = (size.height.value * density).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
        assertTrue("$name vide", (view as? ViewGroup)?.childCount ?: 1 > 0)
        if (System.getProperty("azur.screenshots") == "true") {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(bmp).apply { drawColor(0xFF6B7A8F.toInt()) }.also { view.draw(it) }
            val dir = File(System.getProperty("azur.screenshotDir")!!).apply { mkdirs() }
            File(dir, "widget_$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        assertNotNull(view)
    }

    @Test fun allWidgetsRender() {
        runBlocking { context.container.settings.setWidgetBackground(WidgetBackground.SKY) }
        render(CompactWidget(), "compact_small", DpSize(70.dp, 70.dp))
        render(CompactWidget(), "compact_wide", DpSize(180.dp, 110.dp))
        render(HourlyWidget(), "hourly", DpSize(320.dp, 150.dp))
        render(WeekWidget(), "week", DpSize(320.dp, 260.dp))
        render(ClockWidget(), "clock", DpSize(320.dp, 130.dp))
        render(RainWidget(), "rain", DpSize(260.dp, 150.dp))
    }

    @Test fun dynamicBackground() {
        runBlocking { context.container.settings.setWidgetBackground(WidgetBackground.DYNAMIC) }
        render(HourlyWidget(), "hourly_dynamic", DpSize(320.dp, 150.dp))
        render(ClockWidget(), "clock_dynamic", DpSize(320.dp, 130.dp))
    }
}
