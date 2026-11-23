package com.karelisio.azur.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import com.karelisio.azur.BuildConfig
import com.karelisio.azur.data.SettingsRepository
import com.karelisio.azur.data.net.AzurJson
import com.karelisio.azur.data.net.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.Request
import java.io.File

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long = 0,
)

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tag: String,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GithubAsset> = emptyList(),
) {
    val apk: GithubAsset? get() = assets.firstOrNull { it.name.endsWith(".apk") }
    val version: String get() = tag.removePrefix("v")
}

sealed interface DownloadState {
    data class Progress(val fraction: Float) : DownloadState
    data class Done(val file: File) : DownloadState
    data class Failed(val message: String) : DownloadState
}

class UpdateRepository(
    private val context: Context,
    private val http: Http,
    private val settings: SettingsRepository,
) {
    /** Interroge releases/latest au plus toutes les 6 h (sauf [force]). Renvoie la release si plus récente. */
    suspend fun check(force: Boolean = false): GithubRelease? {
        val now = System.currentTimeMillis()
        if (!force && now - settings.current().lastUpdateCheck < CHECK_INTERVAL_MS) return null
        val text = http.get(
            "https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest",
            mapOf("Accept" to "application/vnd.github+json"),
        )
        settings.setLastUpdateCheck(now)
        val release = AzurJson.decodeFromString(GithubRelease.serializer(), text)
        return release.takeIf { it.apk != null && isNewer(it.tag, BuildConfig.VERSION_NAME) }
    }

    fun download(release: GithubRelease): Flow<DownloadState> = flow {
        val asset = release.apk ?: run { emit(DownloadState.Failed("Aucun APK dans la release")); return@flow }
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "azur-${release.version}.apk")
        try {
            http.client.newCall(Request.Builder().url(asset.url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
                val body = response.body
                val total = body.contentLength().takeIf { it > 0 } ?: asset.size
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        var done = 0L
                        var lastEmit = -1
                        while (input.read(buffer).also { read = it } >= 0) {
                            output.write(buffer, 0, read)
                            done += read
                            val pct = if (total > 0) (done * 100 / total).toInt() else 0
                            if (pct != lastEmit) {
                                lastEmit = pct
                                emit(DownloadState.Progress(pct / 100f))
                            }
                        }
                    }
                }
            }
            emit(DownloadState.Done(target))
        } catch (e: Exception) {
            target.delete()
            emit(DownloadState.Failed(e.message ?: "Erreur de téléchargement"))
        }
    }.flowOn(Dispatchers.IO)

    fun isVitrineInstalled(): Boolean = try {
        context.packageManager.getApplicationInfo(VITRINE_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun vitrineIntent(): Intent =
        Intent(Intent.ACTION_VIEW, "vitrine://app/${BuildConfig.APPLICATION_ID.removeSuffix(".debug")}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun canInstallPackages(): Boolean = context.packageManager.canRequestPackageInstalls()

    companion object {
        const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
        const val VITRINE_PACKAGE = "com.karelisio.vitrine"
    }
}
