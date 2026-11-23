package com.karelisio.azur.ui.nav

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable data class DayRoute(val placeId: Long, val dayIndex: Int)
@Serializable object PlacesRoute
@Serializable object SearchRoute
@Serializable object RadarRoute
@Serializable object SettingsRoute
@Serializable object PermissionsRoute
@Serializable object OnboardingRoute
