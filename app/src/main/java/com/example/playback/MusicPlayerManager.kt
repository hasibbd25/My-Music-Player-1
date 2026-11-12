package com.example.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import com.example.data.model.EqualizerPreset
import com.example.data.model.RepeatMode
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections
import kotlin.math.sin

class MusicPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    // Playback state flows
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackQueue = MutableStateFlow<List<Track>>(emptyList())
    val playbackQueue: StateFlow<List<Track>> = _playbackQueue.asStateFlow()

    private var originalQueue: List<Track> = emptyList()
    private var currentIndex = -1

    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _equalizerPreset = MutableStateFlow(EqualizerPreset.NORMAL)
    val equalizerPreset: StateFlow<EqualizerPreset> = _equalizerPreset.asStateFlow()

    private val _sleepTimerSecondsLeft = MutableStateFlow<Int?>(null)
    val sleepTimerSecondsLeft: StateFlow<Int?> = _sleepTimerSecondsLeft.asStateFlow()

    // Visualizer amplitudes (16 bars normalized 0f..1f)
    private val _visualizerAmplitudes = MutableStateFlow(List(16) { 0.1f })
    val visualizerAmplitudes: StateFlow<List<Float>> = _visualizerAmplitudes.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }
    private var isNoisyReceiverRegistered = false

    init {
        registerBecomingNoisy()
    }

    private fun registerBecomingNoisy() {
        if (!isNoisyReceiverRegistered) {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(becomingNoisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(becomingNoisyReceiver, filter)
            }
            isNoisyReceiverRegistered = true
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        if (newQueue != null && newQueue != originalQueue) {
            originalQueue = newQueue
            _playbackQueue.value = if (_isShuffled.value) {
                val shuffled = newQueue.toMutableList()
                Collections.shuffle(shuffled)
                shuffled.remove(track)
                shuffled.add(0, track)
                shuffled
            } else {
                newQueue
            }
        } else if (_playbackQueue.value.isEmpty()) {
            originalQueue = listOf(track)
            _playbackQueue.value = listOf(track)
        }

        currentIndex = _playbackQueue.value.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _currentTrack.value = track

        startPlaybackForCurrentTrack()
    }

    private fun startPlaybackForCurrentTrack() {
        val track = _currentTrack.value ?: return

        stopAndReleasePlayer()

        if (!requestAudioFocus()) return

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(track.uriString))
                prepare()
            }

            mediaPlayer = player
            _duration.value = player.duration.toLong().coerceAtLeast(track.durationMs)
            applyPlaybackSpeed(_playbackSpeed.value)

            player.setOnCompletionListener {
                handleTrackCompletion()
            }

            player.setOnErrorListener { _, what, extra ->
                _isPlaying.value = false
                false
            }

            player.start()
            _isPlaying.value = true
            startProgressTracker()

            // Start foreground service if needed
            MusicPlaybackService.start(context, track.title, track.artist, isPlaying = true)
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    fun playOrPause() {
        val player = mediaPlayer
        if (player != null) {
            if (player.isPlaying) {
                pause()
            } else {
                resume()
            }
        } else {
            val track = _currentTrack.value ?: _playbackQueue.value.firstOrNull()
            if (track != null) {
                playTrack(track)
            }
        }
    }

    fun resume() {
        mediaPlayer?.let { player ->
            if (requestAudioFocus()) {
                player.start()
                _isPlaying.value = true
                startProgressTracker()
                _currentTrack.value?.let {
                    MusicPlaybackService.start(context, it.title, it.artist, isPlaying = true)
                }
            }
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
        }
        _isPlaying.value = false
        _currentTrack.value?.let {
            MusicPlaybackService.start(context, it.title, it.artist, isPlaying = false)
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        val safePos = positionMs.coerceIn(0L, _duration.value.coerceAtLeast(1L))
        player.seekTo(safePos.toInt())
        _currentPosition.value = safePos
    }

    fun seekRelative(offsetMs: Long) {
        val current = _currentPosition.value
        seekTo(current + offsetMs)
    }

    fun skipToNext() {
        val queue = _playbackQueue.value
        if (queue.isEmpty()) return

        if (currentIndex < queue.size - 1) {
            currentIndex++
            _currentTrack.value = queue[currentIndex]
            startPlaybackForCurrentTrack()
        } else if (_repeatMode.value == RepeatMode.ALL) {
            currentIndex = 0
            _currentTrack.value = queue[currentIndex]
            startPlaybackForCurrentTrack()
        } else {
            // End of queue
            pause()
            seekTo(0)
        }
    }

    fun skipToPrevious() {
        val queue = _playbackQueue.value
        if (queue.isEmpty()) return

        // If played more than 3 seconds, replay from start of track
        if (_currentPosition.value > 3000) {
            seekTo(0)
            return
        }

        if (currentIndex > 0) {
            currentIndex--
            _currentTrack.value = queue[currentIndex]
            startPlaybackForCurrentTrack()
        } else if (_repeatMode.value == RepeatMode.ALL) {
            currentIndex = queue.size - 1
            _currentTrack.value = queue[currentIndex]
            startPlaybackForCurrentTrack()
        } else {
            seekTo(0)
        }
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                mediaPlayer?.start()
                _isPlaying.value = true
            }
            RepeatMode.ALL -> {
                skipToNext()
            }
            RepeatMode.OFF -> {
                if (currentIndex < _playbackQueue.value.size - 1) {
                    skipToNext()
                } else {
                    pause()
                    seekTo(0)
                }
            }
        }
    }

    fun toggleShuffle() {
        val current = _isShuffled.value
        val newShuffled = !current
        _isShuffled.value = newShuffled

        val active = _currentTrack.value
        if (newShuffled) {
            val shuffled = originalQueue.toMutableList()
            Collections.shuffle(shuffled)
            if (active != null) {
                shuffled.remove(active)
                shuffled.add(0, active)
            }
            _playbackQueue.value = shuffled
            currentIndex = 0
        } else {
            _playbackQueue.value = originalQueue
            currentIndex = originalQueue.indexOfFirst { it.id == active?.id }.coerceAtLeast(0)
        }
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applyPlaybackSpeed(speed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.playbackParams = PlaybackParams().apply { this.speed = speed }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _equalizerPreset.value = preset
        // Note: In custom DSP / UI visualization, preset adjustments modify playback visualization dynamics
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerSecondsLeft.value = null
            return
        }

        val totalSeconds = minutes * 60
        _sleepTimerSecondsLeft.value = totalSeconds

        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (isActive && remaining > 0) {
                delay(1000)
                remaining--
                _sleepTimerSecondsLeft.value = remaining
            }
            // Timer expired: gently pause music
            pause()
            _sleepTimerSecondsLeft.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerSecondsLeft.value = null
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var phase = 0.0
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPosition.value = player.currentPosition.toLong()
                            val dur = player.duration.toLong()
                            if (dur > 0) _duration.value = dur
                        }
                    } catch (ignored: Exception) {}
                }

                // Compute dynamic reactive visualizer bars based on playback & equalizer preset
                phase += 0.25
                val eq = _equalizerPreset.value
                val updatedBars = (0 until 16).map { i ->
                    val baseWave = (sin(phase + i * 0.45) * 0.5 + 0.5).toFloat()
                    val harmonic = (sin(phase * 1.8 + i * 0.7) * 0.3).toFloat()
                    val boost = if (i < 5) eq.bassMultiplier * 0.2f else if (i > 10) eq.trebleMultiplier * 0.2f else 0f
                    (baseWave * 0.65f + harmonic + boost + 0.15f).coerceIn(0.08f, 1.0f)
                }
                _visualizerAmplitudes.value = updatedBars

                delay(100)
            }
            if (!_isPlaying.value) {
                // Dim down visualizer
                _visualizerAmplitudes.value = List(16) { 0.08f }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_GAIN -> resume()
                    }
                }
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) pause()
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun stopAndReleasePlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (ignored: Exception) {}
        mediaPlayer = null
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        stopAndReleasePlayer()
        abandonAudioFocus()
        if (isNoisyReceiverRegistered) {
            try { context.unregisterReceiver(becomingNoisyReceiver) } catch (ignored: Exception) {}
            isNoisyReceiverRegistered = false
        }
    }
}
