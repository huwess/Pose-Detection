package com.google.mediapipe.examples.poselandmarker

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechHelper(context: Context) {
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US // Change locale as needed
                tts?.setSpeechRate(1.0f)
                Log.d("TTS", "Text-to-Speech initialized successfully")
            } else {
                Log.e("TTS", "Failed to initialize Text-to-Speech")
            }
        }
    }

    fun speakText(text: String) {
        Log.d("TTS", "Speaking: $text")
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun release() {
        tts?.shutdown()
    }
}
