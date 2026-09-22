package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object AlertSoundManager {
    private var isPlaying = false

    /**
     * Plays a high-quality three-tone emergency chime (880Hz, 1174Hz, 1568Hz)
     * synthesized dynamically via AudioTrack.
     */
    fun playEmergencyAlert(context: Context, vibrate: Boolean = true) {
        if (isPlaying) return
        isPlaying = true

        if (vibrate) {
            triggerVibration(context)
        }

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val sampleRate = 44100
                // Three progressive ascending alert tones
                val frequencies = doubleArrayOf(880.0, 1174.66, 1567.98)
                val durationsMs = intArrayOf(140, 140, 260)

                var totalSamples = 0
                for (dur in durationsMs) {
                    totalSamples += (sampleRate * (dur / 1000.0)).toInt()
                }

                val pcmBuffer = ShortArray(totalSamples)
                var currentSample = 0

                for (i in frequencies.indices) {
                    val freq = frequencies[i]
                    val durationMs = durationsMs[i]
                    val toneSamples = (sampleRate * (durationMs / 1000.0)).toInt()

                    for (s in 0 until toneSamples) {
                        val t = s.toDouble() / sampleRate
                        val rawSine = sin(2.0 * Math.PI * freq * t)

                        // Apply smooth attack and decay envelope to eliminate clipping clicks
                        val fadeLength = (sampleRate * 0.015).toInt()
                        val envelope = when {
                            s < fadeLength -> s.toDouble() / fadeLength
                            s > toneSamples - fadeLength -> (toneSamples - s).toDouble() / fadeLength
                            else -> 1.0
                        }

                        val sampleValue = (rawSine * envelope * Short.MAX_VALUE * 0.85).toInt()
                        pcmBuffer[currentSample++] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcmBuffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(pcmBuffer, 0, pcmBuffer.size)
                audioTrack.play()

                // Wait for playback duration
                Thread.sleep(600)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isPlaying = false
            }
        }
    }

    private fun triggerVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                val timings = longArrayOf(0, 350, 120, 350, 120, 500)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(timings, -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
