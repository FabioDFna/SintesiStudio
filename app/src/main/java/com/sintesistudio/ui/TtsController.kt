package com.sintesistudio.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class TtsController(context: Context) {
    private var tts: TextToSpeech? = null
    private val ready = AtomicBoolean(false)
    var isSpeaking = false
        private set

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ITALIAN
                ready.set(true)
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }
            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }
            @Deprecated("Deprecated")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }
        })
    }

    fun speak(text: String) {
        if (!ready.get()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sintesi_podcast")
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
