package au.edu.jcu.wordkeep.data.audio

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

/** Short sounds after an answer. Always paired with on-screen text, never the only feedback. */
interface FeedbackPlayer {
    fun correct()
    fun incorrect()
}

@Singleton
class AndroidFeedbackPlayer @Inject constructor() : FeedbackPlayer {

    // Created lazily: some emulators and devices refuse a ToneGenerator, and sound is optional.
    private val tones: ToneGenerator? by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME) }.getOrNull()
    }

    override fun correct() {
        tones?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    }

    override fun incorrect() {
        tones?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
    }

    private companion object {
        const val VOLUME = 60
    }
}
