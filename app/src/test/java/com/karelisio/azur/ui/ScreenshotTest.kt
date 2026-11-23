package com.karelisio.azur.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karelisio.azur.Fixtures
import com.karelisio.azur.data.AppSettings
import com.karelisio.azur.data.PlaceWeather
import com.karelisio.azur.data.db.PlaceEntity
import com.karelisio.azur.data.net.OpenMeteoParser
import com.karelisio.azur.ui.components.LocalReduceMotion
import com.karelisio.azur.ui.home.WeatherPage
import com.karelisio.azur.ui.home.skyStateOf
import com.karelisio.azur.ui.sky.GradientSky
import com.karelisio.azur.ui.theme.AzurTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Captures d'écran (vérification visuelle) : ./gradlew testDebugUnitTest -Pscreenshots --tests '*ScreenshotTest*' */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h2800dp-xhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Before fun enabled() = assumeTrue(System.getProperty("azur.screenshots") == "true")

    private val data by lazy {
        PlaceWeather(
            placeId = 1,
            forecast = OpenMeteoParser.parseForecast(Fixtures.read("forecast_grenoble_mf.json")),
            air = OpenMeteoParser.parseAirQuality(Fixtures.read("air_quality_grenoble.json")),
            altitude = null, altitudeMeters = null, vigilance = null,
            fetchedAt = System.currentTimeMillis(),
        )
    }

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            AzurTheme(darkTheme = dark, dynamicColor = false) {
                CompositionLocalProvider(LocalReduceMotion provides true) { content() }
            }
        }
        compose.waitForIdle()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(System.getProperty("azur.screenshotDir")!!).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun home() = shot("home") { Page() }

    @Test fun homeDark() = shot("home_dark", dark = true) { Page() }

    @Composable private fun Page() {
        Box(Modifier.fillMaxSize()) {
            GradientSky(skyStateOf(data), Modifier.fillMaxSize())
            WeatherPage(
                place = PlaceEntity(id = 1, name = "Grenoble", latitude = 45.19, longitude = 5.72),
                data = data, settings = AppSettings(), refreshing = false,
                topPadding = 72.dp, bottomPadding = 0.dp, onRefresh = {}, onDayClick = {},
            )
        }
    }
}
