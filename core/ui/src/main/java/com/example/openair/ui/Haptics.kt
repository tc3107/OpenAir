package com.example.openair.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class Haptics(private val context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun soft() {
        vibratePredefined(
            effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VibrationEffect.EFFECT_TICK
            } else {
                -1
            },
            durationMs = 10,
            amplitude = 60
        )
    }

    fun strong() {
        vibratePredefined(
            effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VibrationEffect.EFFECT_HEAVY_CLICK
            } else {
                -1
            },
            durationMs = 28,
            amplitude = 180
        )
    }

    private fun vibratePredefined(effectId: Int, durationMs: Long, amplitude: Int) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && effectId != -1) {
                vib.vibrate(VibrationEffect.createPredefined(effectId))
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (vib.hasAmplitudeControl()) {
                    VibrationEffect.createOneShot(durationMs, amplitude)
                } else {
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        } catch (_: SecurityException) {
            // VIBRATE permission missing or restricted; ignore.
        }
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember(context) { Haptics(context) }
}
