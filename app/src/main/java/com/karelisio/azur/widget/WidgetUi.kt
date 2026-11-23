package com.karelisio.azur.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.karelisio.azur.MainActivity
import com.karelisio.azur.data.WidgetBackground
import com.karelisio.azur.domain.WeatherCondition

/** Couleurs d'un widget selon le fond choisi (ciel = texte blanc, dynamique = thème Glance). */
class WidgetColors(val primary: ColorProvider, val secondary: ColorProvider, val accent: ColorProvider)

@Composable
fun widgetColors(snapshot: WidgetSnapshot?): WidgetColors =
    if (snapshot == null || snapshot.background == WidgetBackground.SKY) {
        WidgetColors(ColorProvider(Color.White), ColorProvider(Color(0xE6FFFFFF)), ColorProvider(Color(0xFF9CC8FF)))
    } else {
        WidgetColors(GlanceTheme.colors.onSurface, GlanceTheme.colors.onSurfaceVariant, GlanceTheme.colors.primary)
    }

/** Conteneur commun : fond ciel ou dynamique, coins arrondis système, ouvre l'appli au toucher. */
@Composable
fun WidgetContainer(
    snapshot: WidgetSnapshot?,
    padding: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    val base = GlanceModifier.fillMaxSize()
        .cornerRadius(android.R.dimen.system_app_widget_background_radius)
        .clickable(actionStartActivity<MainActivity>())
    val withBg = if (snapshot == null || snapshot.background == WidgetBackground.SKY) {
        base.background(ImageProvider(snapshot?.skyBackground ?: com.karelisio.azur.R.drawable.widget_sky_day))
    } else {
        base.background(GlanceTheme.colors.widgetBackground)
    }
    Box(withBg.padding(padding), contentAlignment = Alignment.TopStart) {
        if (snapshot == null) {
            Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Azur", style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 16.sp))
                Text("Ouvrez l'appli pour charger la météo", style = TextStyle(color = ColorProvider(Color(0xCCFFFFFF)), fontSize = 12.sp))
            }
        } else {
            content()
        }
    }
}

@Composable
fun WxIcon(condition: WeatherCondition, isDay: Boolean, size: Dp) {
    Image(ImageProvider(condition.icon.res(isDay)), condition.label, GlanceModifier.size(size))
}

fun textStyle(color: ColorProvider, size: TextUnit, bold: Boolean = false) =
    TextStyle(color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
