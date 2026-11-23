package com.karelisio.azur.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karelisio.azur.AppContainer
import com.karelisio.azur.container

/** Crée un ViewModel à partir du conteneur d'application (DI manuelle). */
@Composable
inline fun <reified VM : ViewModel> azurViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = LocalContext.current.container
    return viewModel(key = key) { create(container) }
}
