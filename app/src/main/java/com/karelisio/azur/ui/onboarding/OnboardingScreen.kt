package com.karelisio.azur.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karelisio.azur.R
import com.karelisio.azur.domain.PrecipUnit
import com.karelisio.azur.domain.PressureUnit
import com.karelisio.azur.domain.TempUnit
import com.karelisio.azur.domain.WindUnit
import com.karelisio.azur.permissions.AzurPermission
import com.karelisio.azur.permissions.rememberAzurPermission
import com.karelisio.azur.ui.components.LocalReduceMotion
import com.karelisio.azur.ui.components.MorphingShape
import com.karelisio.azur.ui.permissions.PermissionVisual
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PERMISSIONS = AzurPermission.entries

@Composable
fun OnboardingScreen(vm: OnboardingViewModel, onFinished: () -> Unit) {
    val pageCount = 2 + PERMISSIONS.size + 1
    val pager = rememberPagerState { pageCount }
    val scope = rememberCoroutineScope()
    val next: () -> Unit = { scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(pageCount - 1)) } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding().navigationBarsPadding()) {
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            Box(Modifier.fillMaxSize().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
                when (page) {
                    0 -> WelcomePage(next)
                    1 -> UnitsPage(vm, next)
                    pageCount - 1 -> ReadyPage { vm.finish(onFinished) }
                    else -> {
                        val permission = PERMISSIONS[page - 2]
                        PermissionPage(permission, isCurrent = pager.currentPage == page, onNext = next, onGranted = {
                            if (permission == AzurPermission.LOCATION) vm.onLocationGranted()
                        })
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
            repeat(pageCount) { i ->
                val selected = i == pager.currentPage
                val w by animateDpAsState(if (selected) 22.dp else 8.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "w")
                val c by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "c",
                )
                Box(Modifier.padding(horizontal = 3.dp).height(8.dp).width(w).clip(CircleShape).background(c))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomePage(onNext: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val infinite = rememberInfiniteTransition(label = "welcome")
    val spin by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(20_000)), label = "spin")
    val morph by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "morph")
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) enter.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(220.dp).graphicsLayer { scaleX = enter.value; scaleY = enter.value }, contentAlignment = Alignment.Center) {
            MorphingShape(
                MaterialShapes.Cookie12Sided, MaterialShapes.SoftBurst,
                progress = if (reduce) 0f else morph,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxSize(),
                rotation = if (reduce) 0f else spin,
            )
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(200.dp))
        }
        Text("Azur", style = MaterialTheme.typography.displayMedium)
        Text(
            "La météo précise pour la France, avec les modèles de Météo-France. Locale, sans compte.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Commencer") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitsPage(vm: OnboardingViewModel, onNext: () -> Unit) {
    val units by vm.units.collectAsStateWithLifecycle()
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Unités", style = MaterialTheme.typography.headlineLarge)
        Text("Modifiables à tout moment dans les réglages.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        UnitChooser("Température", TempUnit.entries, units.temp, { it.label }) { vm.setUnits(units.copy(temp = it)) }
        UnitChooser("Vent", WindUnit.entries, units.wind, { it.label }) { vm.setUnits(units.copy(wind = it)) }
        UnitChooser("Précipitations", PrecipUnit.entries, units.precip, { it.label }) { vm.setUnits(units.copy(precip = it)) }
        UnitChooser("Pression", PressureUnit.entries, units.pressure, { it.label }) { vm.setUnits(units.copy(pressure = it)) }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Suivant") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> UnitChooser(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.ToggleOn); onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                ) { Text(label(option), maxLines = 1) }
            }
        }
    }
}

@Composable
private fun PermissionPage(permission: AzurPermission, isCurrent: Boolean, onNext: () -> Unit, onGranted: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val state = rememberAzurPermission(permission) { granted ->
        if (granted) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onGranted()
        }
    }
    val needsLocation = permission == AzurPermission.BACKGROUND_LOCATION && !AzurPermission.LOCATION.isGranted(context)
    // Avance automatiquement peu après l'accord.
    LaunchedEffect(state.granted, isCurrent) {
        if (state.granted && isCurrent) { delay(900); onNext() }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PermissionVisual(permission.icon, state.granted)
        Text(permission.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(permission.description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (needsLocation) {
            Text("Autorisez d'abord la localisation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(8.dp))
        if (state.granted) {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Suivant") }
        } else {
            Button(onClick = state.request, enabled = !needsLocation, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Autoriser") }
            TextButton(onClick = onNext) { Text(if (permission.optional) "Plus tard" else "Passer") }
        }
    }
}

@Composable
private fun ReadyPage(onFinish: () -> Unit) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("C'est prêt !", style = MaterialTheme.typography.headlineLarge)
        Text("Vous pourrez revoir les autorisations dans Réglages → Autorisations.",
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        PERMISSIONS.forEach { p ->
            val granted = p.isGranted(context)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
                    tint = if (granted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.width(12.dp))
                Text(p.title, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("C'est parti") }
    }
}

