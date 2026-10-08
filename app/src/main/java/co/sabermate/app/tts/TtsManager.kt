package co.sabermate.app.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) {
    private var textToSpeech: TextToSpeech? = null
    private var isReady = false

    private val _isSpeakingFlow = MutableStateFlow(false)
    val isSpeakingFlow: StateFlow<Boolean> = _isSpeakingFlow.asStateFlow()

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val tts = textToSpeech ?: return@TextToSpeech
                val langResult = tts.setLanguage(Locale("es", "CO"))
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale("es"))
                }
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeakingFlow.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeakingFlow.value = textToSpeech?.isSpeaking == true
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeakingFlow.value = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeakingFlow.value = false
                    }
                })
                isReady = true
            }
        }
    }

    fun speak(text: String?, rate: Float = 1.0f) {
        if (!isReady || text.isNullOrBlank()) return
        val tts = textToSpeech ?: return

        tts.setSpeechRate(if (rate <= 0f) 1.0f else rate)
        tts.stop()

        // Split text into readable sentences to avoid engine text limits
        val parts = text.split("(?<=[.!?:;])\\s+".toRegex())
        val block = StringBuilder()
        var utteranceIndex = 0

        for (part in parts) {
            if (block.length + part.length > 350) {
                val queueMode = if (utteranceIndex == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                tts.speak(block.toString(), queueMode, null, "sm_$utteranceIndex")
                block.clear()
                utteranceIndex++
            }
            block.append(part).append(' ')
        }

        if (block.isNotEmpty()) {
            val queueMode = if (utteranceIndex == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(block.toString(), queueMode, null, "sm_$utteranceIndex")
        }

        _isSpeakingFlow.value = true
    }

    fun stop() {
        textToSpeech?.stop()
        _isSpeakingFlow.value = false
    }

    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking == true
    }

    fun shutdown() {
        textToSpeech?.let {
            it.stop()
            it.shutdown()
        }
        textToSpeech = null
        isReady = false
        _isSpeakingFlow.value = false
    }
}
