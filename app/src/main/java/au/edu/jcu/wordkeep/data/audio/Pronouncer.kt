package au.edu.jcu.wordkeep.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import au.edu.jcu.wordkeep.domain.model.Accent
import au.edu.jcu.wordkeep.domain.model.Word
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class PlaybackState { IDLE, LOADING, PLAYING }

/** Says a word out loud. */
interface Pronouncer {
    val state: StateFlow<PlaybackState>

    /**
     * Plays a human recording when one exists and the network may be used; otherwise falls back
     * to the device's text-to-speech voice. Many words have no recording. When [allowNetwork] is
     * false only an on-device voice is used, because some TTS voices synthesise in the cloud.
     */
    fun pronounce(word: Word, accent: Accent, allowNetwork: Boolean)

    fun stop()
}

@Singleton
class AndroidPronouncer @Inject constructor(
    @ApplicationContext private val context: Context,
) : Pronouncer, TextToSpeech.OnInitListener {

    private val _state = MutableStateFlow(PlaybackState.IDLE)
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: PendingSpeech? = null

    private data class PendingSpeech(val text: String, val locale: Locale, val allowNetwork: Boolean)

    override fun pronounce(word: Word, accent: Accent, allowNetwork: Boolean) {
        stop()
        val recording = word.audioFor(accent)
        if (recording != null && allowNetwork) {
            playRecording(recording, word.text, accent.locale)
        } else {
            speak(word.text, accent.locale, allowNetwork)
        }
    }

    override fun stop() {
        // Forget speech queued while the engine was starting, so it cannot play on a later screen.
        pendingSpeech = null
        releasePlayer()
        tts?.stop()
        _state.value = PlaybackState.IDLE
    }

    private fun playRecording(url: String, text: String, locale: Locale) {
        _state.value = PlaybackState.LOADING
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        try {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mediaPlayer.setDataSource(url)
            mediaPlayer.setOnPreparedListener {
                if (player === it) {
                    it.start()
                    _state.value = PlaybackState.PLAYING
                }
            }
            mediaPlayer.setOnCompletionListener {
                if (player === it) {
                    releasePlayer()
                    _state.value = PlaybackState.IDLE
                }
            }
            mediaPlayer.setOnErrorListener { failed, _, _ ->
                if (player === failed) {
                    releasePlayer()
                    speak(text, locale, allowNetwork = true)
                }
                true
            }
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            releasePlayer()
            speak(text, locale, allowNetwork = true)
        }
    }

    private fun speak(text: String, locale: Locale, allowNetwork: Boolean) {
        val engine = tts ?: TextToSpeech(context, this).also { tts = it }
        if (!ttsReady) {
            pendingSpeech = PendingSpeech(text, locale, allowNetwork)
            _state.value = PlaybackState.LOADING
            return
        }
        if (allowNetwork) {
            val supported = engine.setLanguage(locale)
            if (supported == TextToSpeech.LANG_MISSING_DATA || supported == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.ENGLISH)
            }
        } else {
            val offlineVoice = runCatching { engine.voices }.getOrNull().orEmpty()
                .filter { !it.isNetworkConnectionRequired && it.locale.language == Locale.ENGLISH.language }
                .sortedByDescending { it.locale.country == locale.country }
                .firstOrNull()
            if (offlineVoice == null) {
                _state.value = PlaybackState.IDLE
                return
            }
            engine.voice = offlineVoice
        }
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        _state.value = if (result == TextToSpeech.SUCCESS) PlaybackState.PLAYING else PlaybackState.IDLE
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = PlaybackState.PLAYING
            }

            override fun onDone(utteranceId: String?) {
                _state.value = PlaybackState.IDLE
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = PlaybackState.IDLE
            }
        })
        val waiting = pendingSpeech
        pendingSpeech = null
        when {
            waiting != null && ttsReady -> speak(waiting.text, waiting.locale, waiting.allowNetwork)
            else -> _state.value = PlaybackState.IDLE
        }
    }

    private fun releasePlayer() {
        player?.run {
            setOnPreparedListener(null)
            setOnCompletionListener(null)
            setOnErrorListener(null)
            release()
        }
        player = null
    }

    private companion object {
        const val UTTERANCE_ID = "wordkeep-word"
    }
}
