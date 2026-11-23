package com.karelisio.azur.ui.home

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.karelisio.azur.domain.Vigilance
import com.karelisio.azur.notif.vigilanceColor

/** Bannière de vigilance Météo-France (jaune, orange, rouge). */
@Composable
fun VigilanceBanner(v: Vigilance, placeName: String) {
    if (v.level < 2) return
    val context = LocalContext.current
    val bg = Color(vigilanceColor(v.level))
    val fg = if (v.level == 2) Color(0xFF2B2300) else Color.White
    Surface(
        onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, "https://vigilance.meteofrance.fr".toUri())) } },
        shape = MaterialTheme.shapes.extraLarge,
        color = bg,
        contentColor = fg,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Vigilance ${v.levelName} · $placeName", style = MaterialTheme.typography.titleMedium)
                Text(
                    v.active.joinToString(" · ") { "${it.name} (${Vigilance.levelName(it.level)})" },
                    style = MaterialTheme.typography.bodyMedium,
                )
                v.tomorrowLevel?.takeIf { it != v.level }?.let {
                    Text("Demain : vigilance ${Vigilance.levelName(it)}", style = MaterialTheme.typography.bodySmall)
                }
                Text("Source : Météo-France", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
