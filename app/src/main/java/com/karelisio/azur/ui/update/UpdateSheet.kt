package com.karelisio.azur.ui.update

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.karelisio.azur.BuildConfig
import com.karelisio.azur.update.DownloadState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateSheet(state: UpdateUiState, vm: UpdateViewModel) {
    val release = state.release ?: return
    if (!state.visible) return
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = vm::dismiss) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Nouvelle version", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "Azur ${release.version}",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "Version installée : ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                Modifier
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
            ) { Changelog(release.body.orEmpty()) }

            AnimatedContent(targetState = state.download, contentKey = { it?.javaClass }, label = "download") { d ->
                when (d) {
                    is DownloadState.Progress -> {
                        val p by animateFloatAsState(d.fraction, label = "progress")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearWavyProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                            Text("Téléchargement… ${(d.fraction * 100).toInt()} %", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is DownloadState.Done -> Row(verticalAlignment = Alignment.CenterVertically) {
                        LoadingIndicator(Modifier.size(40.dp))
                        Spacer(Modifier.size(12.dp))
                        Text("Installation…")
                    }
                    is DownloadState.Failed -> Column {
                        Text("Échec : ${d.message}", color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { vm.downloadAndInstall(context) }) { Text("Réessayer") }
                    }
                    null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.vitrineInstalled) {
                            Button(onClick = { vm.openVitrine(context) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.Storefront, null)
                                Spacer(Modifier.size(8.dp))
                                Text("Ouvrir dans Vitrine")
                            }
                            FilledTonalButton(onClick = { vm.downloadAndInstall(context) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Installer directement")
                            }
                        } else {
                            Button(onClick = { vm.downloadAndInstall(context) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.Download, null)
                                Spacer(Modifier.size(8.dp))
                                Text("Télécharger et installer")
                            }
                        }
                        TextButton(onClick = vm::dismiss, modifier = Modifier.fillMaxWidth()) { Text("Plus tard") }
                    }
                }
            }
        }
    }
}

/** Rendu Markdown minimal : titres, puces, gras. */
@Composable
private fun Changelog(markdown: String) {
    val lines = markdown.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) {
        Text("Améliorations et corrections.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    lines.forEach { line ->
        when {
            line.startsWith("#") -> Text(
                line.trimStart('#', ' '),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> Row(Modifier.padding(vertical = 2.dp)) {
                Text("•  ", color = MaterialTheme.colorScheme.primary)
                Text(bold(line.trimStart().drop(2)), style = MaterialTheme.typography.bodyMedium)
            }
            else -> Text(bold(line), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun bold(text: String) = buildAnnotatedString {
    val parts = text.split("**")
    parts.forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) } else append(part)
    }
}
