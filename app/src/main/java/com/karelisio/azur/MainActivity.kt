package com.karelisio.azur

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karelisio.azur.ui.components.LocalReduceMotion
import com.karelisio.azur.ui.components.azurViewModel
import com.karelisio.azur.ui.components.isReduceMotion
import com.karelisio.azur.ui.nav.AzurNavHost
import com.karelisio.azur.ui.theme.AzurTheme
import com.karelisio.azur.ui.update.UpdateSheet
import com.karelisio.azur.ui.update.UpdateViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setOnExitAnimationListener { provider ->
            val view = provider.view
            val icon = provider.iconView
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 1.4f),
                    ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 1.4f),
                    ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f),
                )
                duration = if (isReduceMotion()) 0 else 350
                doOnEnd { provider.remove() }
                start()
            }
        }
        val reduceMotion = isReduceMotion()
        setContent {
            AzurTheme {
                CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
                    val updateVm = azurViewModel { UpdateViewModel(it.updates) }
                    val updateState by updateVm.state.collectAsStateWithLifecycle()
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        AzurNavHost(updateVm = updateVm, startOnboarding = false)
                    }
                    UpdateSheet(updateState, updateVm)
                    androidx.compose.runtime.LaunchedEffect(Unit) { updateVm.check() }
                }
            }
        }
    }
}
