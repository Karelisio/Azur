package com.karelisio.azur.ui.sky

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ShaderBrush
import com.karelisio.azur.domain.SkyKind
import com.karelisio.azur.ui.components.LocalReduceMotion
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Ciel vivant : dégradé selon l'heure, nuages qui dérivent, pluie, neige, étoiles et éclairs (AGSL).
 * Repli : dégradé statique (Android < 13 ou « réduire les animations »).
 */
@Composable
fun LiveSky(state: SkyState, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || reduce) {
        GradientSky(state, modifier)
        return
    }
    val shader = remember { runCatching { RuntimeShader(SKY_SHADER) }.getOrNull() }
    if (shader == null) GradientSky(state, modifier) else ShaderSky(shader, state, modifier)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderSky(shader: RuntimeShader, state: SkyState, modifier: Modifier) {
    val colors = skyColors(state.phase, state.kind)
    val top by animateColorAsState(colors.top, tween(1500), label = "top")
    val bottom by animateColorAsState(colors.bottom, tween(1500), label = "bottom")
    val clouds by animateFloatAsState(state.cloudiness, tween(1500), label = "clouds")
    val rain by animateFloatAsState(
        if (state.kind == SkyKind.RAIN || state.kind == SkyKind.STORM) state.precipitation.coerceAtLeast(0.3f) else 0f, tween(1500), label = "rain",
    )
    val snow by animateFloatAsState(if (state.kind == SkyKind.SNOW) state.precipitation.coerceAtLeast(0.4f) else 0f, tween(1500), label = "snow")
    val stars by animateFloatAsState(
        when (state.phase) {
            DayPhase.NIGHT -> 1f - clouds.coerceIn(0f, 0.9f)
            DayPhase.DAWN, DayPhase.DUSK -> 0.3f * (1f - clouds)
            DayPhase.DAY -> 0f
        },
        tween(1500), label = "stars",
    )
    val sun by animateFloatAsState(if (state.phase == DayPhase.DAY && state.kind == SkyKind.CLEAR) 1f else 0f, tween(1500), label = "sun")
    val fog by animateFloatAsState(if (state.kind == SkyKind.FOG) 1f else 0f, tween(1500), label = "fog")

    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { time.floatValue = ((it - start) / 1_000_000_000.0 % 3600.0).toFloat() }
    }
    // Éclairs : double flash aléatoire en cas d'orage.
    val flash = remember { Animatable(0f) }
    LaunchedEffect(state.kind) {
        if (state.kind != SkyKind.STORM) { flash.snapTo(0f); return@LaunchedEffect }
        while (true) {
            delay(Random.nextLong(3500, 9000))
            flash.animateTo(1f, tween(60)); flash.animateTo(0.2f, tween(90))
            flash.animateTo(0.8f, tween(50)); flash.animateTo(0f, tween(450))
        }
    }

    val brush = remember(shader) { ShaderBrush(shader) }
    Spacer(
        modifier.drawWithCache {
            onDrawBehind {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", time.floatValue)
                shader.setFloatUniform("topColor", top.red, top.green, top.blue, 1f)
                shader.setFloatUniform("bottomColor", bottom.red, bottom.green, bottom.blue, 1f)
                shader.setFloatUniform("cloudiness", clouds)
                shader.setFloatUniform("rain", rain)
                shader.setFloatUniform("snow", snow)
                shader.setFloatUniform("stars", stars)
                shader.setFloatUniform("sunGlow", sun)
                shader.setFloatUniform("fog", fog)
                shader.setFloatUniform("flash", flash.value)
                drawRect(brush)
            }
        },
    )
}


internal const val SKY_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float4 topColor;
uniform float4 bottomColor;
uniform float cloudiness;
uniform float rain;
uniform float snow;
uniform float stars;
uniform float sunGlow;
uniform float fog;
uniform float flash;

float hash(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.03 + float2(17.0, 9.0);
        a *= 0.5;
    }
    return v;
}

float rainLayer(float2 p, float scale, float speed, float t) {
    float2 g = float2(p.x * scale, p.y * scale * 0.18 - t * speed);
    float col = floor(g.x);
    g.y += hash(float2(col, 3.1)) * 7.0;
    float2 f = fract(g);
    float cell = floor(g.y);
    float on = step(0.55, hash(float2(col, cell)));
    float streak = smoothstep(0.0, 0.08, f.y) * smoothstep(0.55, 0.15, f.y);
    float thin = smoothstep(0.09, 0.0, abs(f.x - 0.5));
    return on * streak * thin;
}

float snowLayer(float2 p, float scale, float speed, float t) {
    float2 g = p * scale;
    g.y -= t * speed;
    g.x += sin(t * 0.6 + g.y * 0.7) * 0.25;
    float2 id = floor(g);
    float h = hash(id);
    float2 off = float2(hash(id + 1.7), hash(id + 4.3)) - 0.5;
    float2 f = fract(g) - 0.5 - off * 0.6;
    float r = 0.05 + 0.07 * h;
    return step(0.5, h) * smoothstep(r, r * 0.2, length(f));
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float aspect = iResolution.x / iResolution.y;
    float2 p = float2(uv.x * aspect, uv.y);
    float3 col = mix(topColor.rgb, bottomColor.rgb, uv.y);

    // Halo du soleil (ciel dégagé de jour)
    if (sunGlow > 0.0) {
        float d = length(p - float2(aspect * 0.82, 0.08));
        col += float3(1.0, 0.92, 0.7) * sunGlow * (0.35 * exp(-d * 5.0) + 0.12 * exp(-d * 1.5));
    }

    // Étoiles scintillantes
    if (stars > 0.0) {
        float2 g = p * 70.0;
        float2 id = floor(g);
        float h = hash(id);
        float2 f = fract(g) - 0.5;
        float tw = 0.55 + 0.45 * sin(iTime * (1.0 + h * 3.0) + h * 50.0);
        float s = step(0.972, h) * smoothstep(0.2, 0.0, length(f)) * tw;
        col += float3(1.4 * s * stars * (1.0 - uv.y * 0.7));
    }

    // Nuages : deux couches qui dérivent à des vitesses différentes
    if (cloudiness > 0.0) {
        float c1 = fbm(p * 2.2 + float2(iTime * 0.010, 0.0));
        float c2 = fbm(p * 4.5 + float2(iTime * 0.022, 5.0));
        float n = c1 * 0.65 + c2 * 0.5;
        float density = smoothstep(0.75 - cloudiness * 0.45, 1.0 - cloudiness * 0.3, n);
        density *= 1.0 - 0.45 * uv.y;
        float3 light = mix(float3(1.0), float3(0.62, 0.67, 0.74), rain * 0.85);
        float3 cloudCol = mix(light, col * 1.35 + 0.06, 0.4);
        col = mix(col, cloudCol, density * 0.8);
    }

    // Brouillard
    if (fog > 0.0) {
        float bands = fbm(float2(p.x * 1.5 + iTime * 0.03, p.y * 6.0));
        col = mix(col, float3(0.82, 0.85, 0.88), fog * (0.25 + 0.35 * bands) * (0.4 + uv.y * 0.6));
    }

    // Pluie oblique (trois profondeurs)
    if (rain > 0.0) {
        float2 q = float2(p.x + uv.y * 0.12, uv.y);
        float r = rainLayer(q, 70.0, 9.0, iTime) * 0.55
                + rainLayer(q + 0.37, 110.0, 12.0, iTime) * 0.35
                + rainLayer(q + 0.71, 45.0, 7.0, iTime) * 0.7;
        col = mix(col, float3(0.85, 0.9, 1.0), clamp(r * rain, 0.0, 0.6));
    }

    // Neige (trois profondeurs)
    if (snow > 0.0) {
        float s = snowLayer(p, 9.0, 0.35, iTime) * 0.9
                + snowLayer(p + 0.5, 16.0, 0.55, iTime) * 0.6
                + snowLayer(p + 0.25, 26.0, 0.8, iTime) * 0.4;
        col = mix(col, float3(1.0), clamp(s * snow, 0.0, 0.95));
    }

    // Éclair
    col += float3(0.75, 0.8, 1.0) * flash * 0.55;

    return half4(half3(col), 1.0);
}
"""
