package com.karelisio.azur.ui

import android.graphics.RuntimeShader
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karelisio.azur.ui.sky.SKY_SHADER
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Compile le shader AGSL du ciel vivant (le constructeur lève une exception si le code est invalide)
 * et vérifie chaque uniform (nom et taille) utilisé par LiveSky.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SkyShaderTest {
    @Test fun compilesWithAllUniforms() {
        val shader = RuntimeShader(SKY_SHADER)
        shader.setFloatUniform("iResolution", 400f, 800f)
        shader.setFloatUniform("iTime", 12f)
        shader.setFloatUniform("topColor", 0.2f, 0.5f, 0.9f, 1f)
        shader.setFloatUniform("bottomColor", 0.5f, 0.7f, 0.95f, 1f)
        listOf("cloudiness", "rain", "snow", "stars", "sunGlow", "fog", "flash").forEach { shader.setFloatUniform(it, 0.5f) }
    }
}
