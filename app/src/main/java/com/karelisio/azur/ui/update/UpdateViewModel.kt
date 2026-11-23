package com.karelisio.azur.ui.update

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karelisio.azur.update.ApkInstaller
import com.karelisio.azur.update.DownloadState
import com.karelisio.azur.update.GithubRelease
import com.karelisio.azur.update.UpdateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateUiState(
    val release: GithubRelease? = null,
    val visible: Boolean = false,
    val download: DownloadState? = null,
    val checking: Boolean = false,
    val message: String? = null,
    val vitrineInstalled: Boolean = false,
)

class UpdateViewModel(private val repo: UpdateRepository) : ViewModel() {
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state
    private var job: Job? = null

    fun check(force: Boolean = false) {
        if (_state.value.checking) return
        viewModelScope.launch {
            _state.update { it.copy(checking = true, message = null) }
            val release = runCatching { repo.check(force) }.getOrNull()
            _state.update {
                it.copy(
                    checking = false,
                    release = release,
                    visible = release != null,
                    vitrineInstalled = repo.isVitrineInstalled(),
                    message = if (force && release == null) "Azur est à jour" else null,
                )
            }
        }
    }

    fun dismiss() {
        job?.cancel()
        _state.update { it.copy(visible = false, download = null) }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun openVitrine(context: Context) {
        context.startActivity(repo.vitrineIntent())
        dismiss()
    }

    fun downloadAndInstall(context: Context) {
        val release = _state.value.release ?: return
        if (!repo.canInstallPackages()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        job?.cancel()
        job = viewModelScope.launch {
            repo.download(release).collect { s ->
                _state.update { it.copy(download = s) }
                if (s is DownloadState.Done) {
                    runCatching { ApkInstaller.install(context.applicationContext, s.file) }
                        .onFailure { e -> _state.update { it.copy(download = DownloadState.Failed(e.message ?: "Échec")) } }
                }
            }
        }
    }
}
