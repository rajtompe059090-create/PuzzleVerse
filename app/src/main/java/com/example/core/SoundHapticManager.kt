package com.example.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * High-performance, self-contained Audio & Haptic Manager for PuzzleVerse.
 * Generates crisp 16-bit PCM audio waveforms dynamically using Android AudioTrack
 * requiring zero external asset files while providing rich sound effects and ambient music.
 */
class SoundHapticManager(private val context: Context) {

    private val tag = "SoundHapticManager"

    // Haptics
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isVibrationEnabled: Boolean = true
    var isSoundEnabled: Boolean = true
    var isMusicEnabled: Boolean = true

    // Audio Synthesis Sample Rate
    private val sampleRate = 22050

    // Pre-computed Sound Effect Buffers
    private val soundBuffers = mutableMapOf<String, ShortArray>()

    // Music Coroutine Job
    private val musicScope = CoroutineScope(Dispatchers.Default)
    private var musicJob: Job? = null
    private var musicTrack: AudioTrack? = null

    init {
        try {
            preloadSoundEffects()
        } catch (e: Exception) {
            Log.e(tag, "Failed to preload sound effects", e)
        }
    }

    private fun preloadSoundEffects() {
        soundBuffers["click"] = generateTone(frequency = 1200.0, durationMs = 15, attackMs = 2, decayMs = 13)
        soundBuffers["move"] = generateSweep(startFreq = 480.0, endFreq = 720.0, durationMs = 35)
        soundBuffers["wrong"] = generateBuzzer(freq = 150.0, durationMs = 80)
        soundBuffers["arrow"] = generateSweep(startFreq = 300.0, endFreq = 950.0, durationMs = 55)
        soundBuffers["slide"] = generateTone(frequency = 260.0, durationMs = 35, attackMs = 5, decayMs = 30)
        soundBuffers["merge"] = generateChord(freqs = doubleArrayOf(440.0, 659.25, 880.0), durationMs = 120)
        soundBuffers["select"] = generateTone(frequency = 880.0, durationMs = 25, attackMs = 2, decayMs = 23)
        soundBuffers["match"] = generateArpeggio(freqs = doubleArrayOf(523.25, 659.25, 783.99), noteDurationMs = 40)
        soundBuffers["hint"] = generateSweep(startFreq = 880.0, endFreq = 1400.0, durationMs = 90)
        soundBuffers["win"] = generateFanfare()
        soundBuffers["reward"] = generateChord(freqs = doubleArrayOf(783.99, 987.77, 1174.66, 1567.98), durationMs = 200)
        soundBuffers["achievement"] = generateArpeggio(freqs = doubleArrayOf(440.0, 554.37, 659.25, 880.0), noteDurationMs = 60)
    }

    private fun generateTone(frequency: Double, durationMs: Int, attackMs: Int, decayMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs / 1000.0).toInt()
        val attackSamples = (sampleRate * attackMs / 1000.0).toInt().coerceAtLeast(1)
        val decaySamples = (sampleRate * decayMs / 1000.0).toInt().coerceAtLeast(1)
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val env = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i > totalSamples - decaySamples -> (totalSamples - i).toDouble() / decaySamples
                else -> 1.0
            }
            val sample = sin(2.0 * PI * frequency * i / sampleRate) * env * 0.7
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateSweep(startFreq: Double, endFreq: Double, durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs / 1000.0).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val freq = startFreq + (endFreq - startFreq) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = 1.0 - t
            val sample = sin(phase) * env * 0.7
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateBuzzer(freq: Double, durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs / 1000.0).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val env = 1.0 - (i.toDouble() / totalSamples)
            val fund = sin(2.0 * PI * freq * i / sampleRate)
            val harm = sin(2.0 * PI * (freq * 2.5) * i / sampleRate) * 0.5
            val sample = (fund + harm) / 1.5 * env * 0.75
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateChord(freqs: DoubleArray, durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs / 1000.0).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val env = 1.0 - (i.toDouble() / totalSamples)
            var sum = 0.0
            for (f in freqs) {
                sum += sin(2.0 * PI * f * i / sampleRate)
            }
            val sample = (sum / freqs.size) * env * 0.8
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateArpeggio(freqs: DoubleArray, noteDurationMs: Int): ShortArray {
        val noteSamples = (sampleRate * noteDurationMs / 1000.0).toInt()
        val totalSamples = noteSamples * freqs.size
        val buffer = ShortArray(totalSamples)

        for (n in freqs.indices) {
            val freq = freqs[n]
            for (i in 0 until noteSamples) {
                val env = 1.0 - (i.toDouble() / noteSamples)
                val sample = sin(2.0 * PI * freq * i / sampleRate) * env * 0.7
                buffer[n * noteSamples + i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return buffer
    }

    private fun generateFanfare(): ShortArray {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
        val durations = intArrayOf(60, 60, 60, 180)
        val totalSamples = durations.sumOf { (sampleRate * it / 1000.0).toInt() }
        val buffer = ShortArray(totalSamples)
        var offset = 0

        for (n in notes.indices) {
            val noteSamples = (sampleRate * durations[n] / 1000.0).toInt()
            val freq = notes[n]
            for (i in 0 until noteSamples) {
                val env = 1.0 - (i.toDouble() / noteSamples) * 0.8
                val sample = sin(2.0 * PI * freq * i / sampleRate) * env * 0.8
                buffer[offset + i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
            offset += noteSamples
        }
        return buffer
    }

    private fun playPcm(buffer: ShortArray?, volume: Float = 0.85f) {
        if (!isSoundEnabled || buffer == null || buffer.isEmpty()) return
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
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
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.setVolume(volume)
            track.write(buffer, 0, buffer.size)
            track.play()
            // Auto release after sound completes
            CoroutineScope(Dispatchers.Default).launch {
                delay((buffer.size * 1000L / sampleRate) + 50)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to play sound: ${e.message}")
        }
    }

    // Sound FX Methods
    fun playClick() {
        playPcm(soundBuffers["click"], 0.7f)
        vibrate(18)
    }

    fun playMoveSuccess() {
        playPcm(soundBuffers["move"], 0.75f)
        vibrate(28)
    }

    fun playWrongMove() {
        playPcm(soundBuffers["wrong"], 0.8f)
        vibratePattern(longArrayOf(0, 45, 30, 45), intArrayOf(0, 180, 0, 180))
    }

    fun playArrowEscape() {
        playPcm(soundBuffers["arrow"], 0.8f)
        vibrate(35)
    }

    fun playBlockSlide() {
        playPcm(soundBuffers["slide"], 0.65f)
        vibrate(20)
    }

    fun playBlockMerge() {
        playPcm(soundBuffers["merge"], 0.85f)
        vibrate(45)
    }

    fun playTileSelect() {
        playPcm(soundBuffers["select"], 0.7f)
        vibrate(22)
    }

    fun playTileMatch() {
        playPcm(soundBuffers["match"], 0.85f)
        vibrate(50)
    }

    fun playHint() {
        playPcm(soundBuffers["hint"], 0.8f)
        vibrate(30)
    }

    fun playWin() {
        playPcm(soundBuffers["win"], 0.95f)
        vibratePattern(longArrayOf(0, 70, 50, 110, 50, 180), intArrayOf(0, 150, 0, 200, 0, 255))
    }

    fun playReward() {
        playPcm(soundBuffers["reward"], 0.9f)
        vibrate(60)
    }

    fun playAchievement() {
        playPcm(soundBuffers["achievement"], 0.9f)
        vibrate(70)
    }

    // Background Music (Ambient Looping Neon Synth)
    fun startBackgroundMusic() {
        if (!isMusicEnabled || musicJob?.isActive == true) return

        musicJob = musicScope.launch {
            try {
                val loopNotes = doubleArrayOf(
                    220.0, 261.63, 293.66, 329.63, 392.0, 440.0, // A minor pentatonic ambient loop
                    329.63, 293.66, 261.63, 220.0, 196.0, 220.0
                )
                val noteDurationMs = 280
                val noteSamples = (sampleRate * noteDurationMs / 1000.0).toInt()
                val loopBuffer = ShortArray(noteSamples * loopNotes.size)

                for (n in loopNotes.indices) {
                    val freq = loopNotes[n]
                    for (i in 0 until noteSamples) {
                        val env = sin(PI * i / noteSamples) * 0.28 // gentle ambient volume
                        val sub = sin(2.0 * PI * (freq / 2.0) * i / sampleRate) * 0.15
                        val sample = (sin(2.0 * PI * freq * i / sampleRate) + sub) * env
                        loopBuffer[n * noteSamples + i] = (sample * Short.MAX_VALUE).toInt().toShort()
                    }
                }

                musicTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(loopBuffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                musicTrack?.setVolume(0.25f)
                musicTrack?.write(loopBuffer, 0, loopBuffer.size)
                musicTrack?.setLoopPoints(0, loopBuffer.size, -1)
                musicTrack?.play()
            } catch (e: Exception) {
                Log.w(tag, "Background music error: ${e.message}")
            }
        }
    }

    fun stopBackgroundMusic() {
        try {
            musicJob?.cancel()
            musicJob = null
            musicTrack?.stop()
            musicTrack?.release()
            musicTrack = null
        } catch (_: Exception) {}
    }

    // Haptics Helpers
    private fun vibrate(durationMs: Long) {
        if (!isVibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        if (!isVibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }
}
