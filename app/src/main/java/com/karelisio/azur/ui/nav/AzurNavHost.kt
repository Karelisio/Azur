package com.karelisio.azur.ui.nav

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.karelisio.azur.ui.home.HomeScreen
import com.karelisio.azur.ui.settings.SettingsScreen
import com.karelisio.azur.ui.update.UpdateViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AzurNavHost(updateVm: UpdateViewModel, startOnboarding: Boolean) {
    val nav = rememberNavController()
    SharedTransitionLayout {
        NavHost(navController = nav, startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(onOpenSettings = { nav.navigate(SettingsRoute) })
            }
            composable<SettingsRoute> {
                SettingsScreen(updateVm = updateVm, onBack = { nav.popBackStack() })
            }
        }
    }
}
