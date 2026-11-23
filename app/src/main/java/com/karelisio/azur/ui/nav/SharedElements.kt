package com.karelisio.azur.ui.nav

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Transition « shared element » (jour → détail) quand les scopes sont disponibles. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedBoundsIfAvailable(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val anim = LocalNavAnimatedScope.current ?: return this
    return with(shared) { this@sharedBoundsIfAvailable.sharedBounds(rememberSharedContentState(key), anim) }
}
