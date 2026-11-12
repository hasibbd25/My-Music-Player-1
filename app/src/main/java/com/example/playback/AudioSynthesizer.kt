package com.example.playback

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * High-performance additive synthesizer that generates real 44.1kHz stereo PCM WAV tracks
 * for instant offline playback without external network downloads or large assets.
 */
object AudioSynthesizer {

    private const val SAMPLE_RATE = 44100
    private const val CHANNELS = 2
    private const val BITS_PER_SAMPLE = 16

    data class DemoTrackMeta(
        val filename: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationSeconds: Int,
        val trackType: Int
    )

    val DEMO_TRACKS = listOf(
        DemoTrackMeta("neon_horizon.wav", "Neon Horizon", "SonicFlow Studio", "Synthwave Vol. 1", 16, 0),
        DemoTrackMeta("midnight_lofi.wav", "Midnight Lo-Fi", "Velvet Beat", "Chilled Sessions", 16, 1),
        DemoTrackMeta("cyber_chill.wav", "Cyber Chill", "Neural Pulse", "Ambient Mind", 18, 2),
        DemoTrackMeta("cosmic_starlight.wav", "Cosmic Starlight", "Astral Sound", "Deep Space Orbit", 15, 3),
        DemoTrackMeta("acoustic_sunset.wav", "Acoustic Sunset", "Horizon Echo", "Golden Hour", 16, 4)
    )

    suspend fun generateDemoTracks(context: Context): List<File> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "demo_tracks")
        if (!dir.exists()) dir.mkdirs()

        DEMO_TRACKS.map { meta ->
            val file = File(dir, meta.filename)
            if (!file.exists() || file.length() < 1000) {
                generateWavFile(file, meta.durationSeconds, meta.trackType)
            }
            file
        }
    }

    private fun generateWavFile(targetFile: File, durationSec: Int, style: Int) {
        val totalSamples = SAMPLE_RATE * durationSec
        val bytesPerSample = (BITS_PER_SAMPLE / 8) * CHANNELS
        val dataSize = totalSamples * bytesPerSample

        val fos = FileOutputStream(targetFile)
        writeWavHeader(fos, dataSize)

        val bufferSize = 4096
        val byteBuffer = ByteBuffer.allocate(bufferSize * bytesPerSample).order(ByteOrder.LITTLE_ENDIAN)

        var sampleIndex = 0
        while (sampleIndex < totalSamples) {
            byteBuffer.clear()
            val chunkSamples = minOf(bufferSize, totalSamples - sampleIndex)

            for (i in 0 until chunkSamples) {
                val t = (sampleIndex + i).toDouble() / SAMPLE_RATE
                val (left, right) = computeStereoSample(t, style, durationSec.toDouble())

                val sLeft = (left.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
                val sRight = (right.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()

                byteBuffer.putShort(sLeft)
                byteBuffer.putShort(sRight)
            }

            fos.write(byteBuffer.array(), 0, chunkSamples * bytesPerSample)
            sampleIndex += chunkSamples
        }

        fos.flush()
        fos.close()
    }

    private fun computeStereoSample(t: Double, style: Int, totalDuration: Double): Pair<Double, Double> {
        // Smooth fade-in and fade-out
        val fadeTime = 0.5
        val envelope = when {
            t < fadeTime -> t / fadeTime
            t > totalDuration - fadeTime -> (totalDuration - t) / fadeTime
            else -> 1.0
        }

        return when (style) {
            0 -> {
                // Neon Horizon: Synthwave chords + punchy bass + arpeggio
                // Chords: Am (220, 261.63, 329.63), F (174.61, 220, 261.63), C (261.63, 329.63, 392), G (196, 246.94, 293.66)
                val measure = (t % 4.0)
                val chordIndex = ((t / 2.0).toInt()) % 4
                val chordFreqs = when (chordIndex) {
                    0 -> doubleArrayOf(220.0, 261.63, 329.63) // Am
                    1 -> doubleArrayOf(174.61, 220.0, 261.63) // F
                    2 -> doubleArrayOf(261.63, 329.63, 392.0) // C
                    else -> doubleArrayOf(196.0, 246.94, 293.66) // G
                }
                val bassFreq = chordFreqs[0] / 2.0

                // Synth pad
                var pad = 0.0
                for (f in chordFreqs) {
                    pad += 0.18 * (sin(2 * PI * f * t) + 0.3 * sin(4 * PI * f * t))
                }

                // 8th-note arp
                val arpStep = ((t * 4.0).toInt()) % 4
                val arpFreq = chordFreqs[arpStep % chordFreqs.size] * 2.0
                val arpDecay = exp(-4.0 * (t % 0.25))
                val arp = 0.22 * sin(2 * PI * arpFreq * t) * arpDecay

                // Sub bass
                val bassDecay = exp(-2.0 * (t % 0.5))
                val bass = 0.3 * (sin(2 * PI * bassFreq * t) + 0.5 * sin(4 * PI * bassFreq * t)) * bassDecay

                // Soft kick on beat
                val kickDecay = exp(-12.0 * (t % 1.0))
                val kick = 0.35 * sin(2 * PI * (60.0 + 80.0 * kickDecay) * t) * kickDecay

                val mixLeft = (pad * 0.8 + arp * 0.6 + bass + kick) * envelope
                val mixRight = (pad * 0.8 + arp * 1.0 + bass + kick) * envelope
                Pair(mixLeft, mixRight)
            }
            1 -> {
                // Midnight Lo-Fi: Mellow electric piano jazz chords + vinyl noise simulation
                val chordIndex = ((t / 2.5).toInt()) % 4
                val chord = when (chordIndex) {
                    0 -> doubleArrayOf(293.66, 349.23, 440.0, 523.25) // Dm7
                    1 -> doubleArrayOf(196.0, 246.94, 293.66, 349.23) // G7
                    2 -> doubleArrayOf(261.63, 329.63, 392.0, 493.88) // Cmaj7
                    else -> doubleArrayOf(220.0, 261.63, 329.63, 392.0) // Am7
                }
                var rhodes = 0.0
                val chordT = t % 2.5
                val chordEnvelope = exp(-1.2 * chordT)
                for (f in chord) {
                    rhodes += (0.16 * sin(2 * PI * f * t) + 0.08 * sin(4 * PI * f * t) + 0.03 * sin(6 * PI * f * t))
                }
                rhodes *= chordEnvelope

                // Lo-fi bass line
                val bassF = chord[0] / 2.0
                val bass = 0.25 * sin(2 * PI * bassF * t) * exp(-0.8 * (t % 1.25))

                // Subtle rimshot / click
                val beatT = t % 1.25
                val rim = if (beatT > 0.6 && beatT < 0.65) 0.15 * sin(2 * PI * 1200.0 * t) * exp(-30.0 * (beatT - 0.6)) else 0.0

                Pair((rhodes + bass + rim) * envelope, (rhodes * 0.9 + bass + rim * 0.8) * envelope)
            }
            2 -> {
                // Cyber Chill: Deep atmospheric drone & shimmering crystalline bell
                val baseF = 164.81 // E3
                val drone = 0.25 * (sin(2 * PI * baseF * t) + 0.4 * sin(2 * PI * (baseF * 1.5) * t + 0.5 * sin(2 * PI * 0.2 * t)))

                val bellF = when (((t / 1.5).toInt()) % 6) {
                    0 -> 659.25 // E5
                    1 -> 783.99 // G5
                    2 -> 987.77 // B5
                    3 -> 880.00 // A5
                    4 -> 783.99 // G5
                    else -> 659.25
                }
                val bellDecay = exp(-2.5 * (t % 1.5))
                val bell = 0.25 * (sin(2 * PI * bellF * t) + 0.3 * sin(2 * PI * bellF * 2.01 * t)) * bellDecay

                Pair((drone * 0.7 + bell * 0.9) * envelope, (drone * 0.7 + bell * 0.5) * envelope)
            }
            3 -> {
                // Cosmic Starlight: Fast shimmering arpeggios
                val noteIdx = ((t * 6.0).toInt()) % 8
                val scale = doubleArrayOf(370.0, 440.0, 554.37, 659.25, 740.0, 880.0, 987.77, 1108.73)
                val f = scale[noteIdx]
                val noteDecay = exp(-7.0 * (t % (1.0 / 6.0)))
                val lead = 0.35 * (sin(2 * PI * f * t) + 0.2 * sin(6 * PI * f * t)) * noteDecay

                val pad = 0.15 * (sin(2 * PI * 185.0 * t) + sin(2 * PI * 220.0 * t))
                Pair((pad + lead * 0.8) * envelope, (pad + lead * 1.0) * envelope)
            }
            else -> {
                // Acoustic Sunset: Warm acoustic plucked chord simulation
                val chordIdx = ((t / 2.0).toInt()) % 4
                val roots = doubleArrayOf(196.0, 146.83, 164.81, 130.81) // G, D, Em, C
                val root = roots[chordIdx]

                var pluck = 0.0
                for (harmonic in 1..4) {
                    val hf = root * harmonic
                    pluck += (0.28 / harmonic) * sin(2 * PI * hf * t)
                }
                pluck *= exp(-2.0 * (t % 0.5))
                Pair(pluck * envelope, pluck * envelope)
            }
        }
    }

    private fun writeWavHeader(fos: FileOutputStream, dataSize: Int) {
        val totalSize = dataSize + 36
        val byteRate = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)
        val blockAlign = (CHANNELS * (BITS_PER_SAMPLE / 8)).toShort()

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        // RIFF chunk descriptor
        header.put("RIFF".toByteArray())
        header.putInt(totalSize)
        header.put("WAVE".toByteArray())

        // "fmt " sub-chunk
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1.toShort()) // AudioFormat 1 = PCM
        header.putShort(CHANNELS.toShort())
        header.putInt(SAMPLE_RATE)
        header.putInt(byteRate)
        header.putShort(blockAlign)
        header.putShort(BITS_PER_SAMPLE.toShort())

        // "data" sub-chunk
        header.put("data".toByteArray())
        header.putInt(dataSize)

        fos.write(header.array())
    }
}
