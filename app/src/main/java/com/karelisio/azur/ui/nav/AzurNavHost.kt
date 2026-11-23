package com.karelisio.azur.ui.nav

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.karelisio.azur.permissions.AzurPermission
import com.karelisio.azur.permissions.rememberAzurPermission
import com.karelisio.azur.ui.components.azurViewModel
import com.karelisio.azur.ui.onboarding.OnboardingScreen
import com.karelisio.azur.ui.onboarding.OnboardingViewModel
import com.karelisio.azur.ui.permissions.PermissionsScreen
import com.karelisio.azur.ui.day.DayDetailScreen
import com.karelisio.azur.ui.home.HomeScreen
import com.karelisio.azur.ui.home.HomeViewModel
import com.karelisio.azur.ui.home.WeatherPage
import com.karelisio.azur.ui.places.PlacesScreen
import com.karelisio.azur.ui.places.PlacesViewModel
import com.karelisio.azur.ui.places.SearchScreen
import com.karelisio.azur.data.net.RainViewerApi
import com.karelisio.azur.ui.radar.RadarScreen
import com.karelisio.azur.ui.radar.RadarViewModel
import com.karelisio.azur.ui.settings.SettingsScreen
import com.karelisio.azur.ui.settings.SettingsViewModel
import com.karelisio.azur.ui.update.UpdateViewModel

private const val SELECT_PLACE = "selectPlace"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AzurNavHost(updateVm: UpdateViewModel, startOnboarding: Boolean) {
    val nav = rememberNavController()
    val homeVm = azurViewModel { HomeViewModel(it.locationUpdater, it.places, it.weather, it.settings) }
    val placesVm = azurViewModel { PlacesViewModel(it.places) }
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedScope provides this) {
            NavHost(navController = nav, startDestination = if (startOnboarding) OnboardingRoute else HomeRoute) {
                composable<OnboardingRoute> {
                    val vm = azurViewModel { OnboardingViewModel(it.settings, it.locationUpdater, it.weather) }
                    OnboardingScreen(vm, onFinished = {
                        nav.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                    })
                }
                composable<PermissionsRoute> {
                    PermissionsScreen(onBack = { nav.popBackStack() }, onLocationGranted = { homeVm.updateLocation(force = true) })
                }
                composable<HomeRoute> { entry ->
                    val location = rememberAzurPermission(AzurPermission.LOCATION) { if (it) homeVm.updateLocation(force = true) }
                    val selected by entry.savedStateHandle.getStateFlow<Long?>(SELECT_PLACE, null).collectAsStateWithLifecycle()
                    val settings by homeVm.settings.collectAsStateWithLifecycle()
                    CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                        HomeScreen(
                            vm = homeVm,
                            selectPlaceId = selected,
                            onPlaceSelected = { entry.savedStateHandle[SELECT_PLACE] = null },
                            onOpenPlaces = { nav.navigate(PlacesRoute) },
                            onOpenSearch = { nav.navigate(SearchRoute) },
                            onOpenSettings = { nav.navigate(SettingsRoute) },
                            onOpenRadar = { nav.navigate(RadarRoute) },
                            onOpenDay = { id, i -> nav.navigate(DayRoute(id, i)) },
                            onUseLocation = { if (location.granted) homeVm.updateLocation(force = true) else location.request() },
                        ) { place, data, refreshing, top, bottom ->
                            WeatherPage(
                                place = place, data = data, settings = settings, refreshing = refreshing,
                                topPadding = top, bottomPadding = bottom,
                                onRefresh = { homeVm.refresh(place, force = true) },
                                onDayClick = { i -> nav.navigate(DayRoute(place.id, i)) },
                            )
                        }
                    }
                }
                composable<DayRoute> { entry ->
                    val route = entry.toRoute<DayRoute>()
                    CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                        DayDetailScreen(homeVm, route.placeId, route.dayIndex, onBack = { nav.popBackStack() })
                    }
                }
                composable<PlacesRoute> {
                    val weather by homeVm.weather.collectAsStateWithLifecycle()
                    val settings by homeVm.settings.collectAsStateWithLifecycle()
                    PlacesScreen(
                        vm = placesVm, weather = weather, settings = settings,
                        onBack = { nav.popBackStack() },
                        onAdd = { nav.navigate(SearchRoute) },
                        onSelect = { id ->
                            nav.getBackStackEntry<HomeRoute>().savedStateHandle[SELECT_PLACE] = id
                            nav.popBackStack(HomeRoute, inclusive = false)
                        },
                    )
                }
                composable<SearchRoute> {
                    SearchScreen(placesVm, onBack = { nav.popBackStack() }, onAdded = { id ->
                        nav.getBackStackEntry<HomeRoute>().savedStateHandle[SELECT_PLACE] = id
                        nav.popBackStack(HomeRoute, inclusive = false)
                    })
                }
                composable<RadarRoute> {
                    val vm = azurViewModel { RadarViewModel(RainViewerApi(it.http), it.places) }
                    RadarScreen(vm, onBack = { nav.popBackStack() })
                }
                composable<SettingsRoute> {
                    val vm = azurViewModel { SettingsViewModel(it) }
                    SettingsScreen(vm, updateVm, onBack = { nav.popBackStack() }, onOpenPermissions = { nav.navigate(PermissionsRoute) })
                }
            }
        }
    }
}
