package com.sisi.expressiontrainer.asr

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class StepFunASRClient {

    companion object {
        private const val ASR_ENDPOINT = "wss://api.stepfun.com/v1/realtime/asr/stream"
        private const val MODEL = "stepaudio-2.5-asr-stream"
    }

    interface ASRResultListener {
        fun onResult(text: String, isFinal: Boolean, stash: String)
        fun onStatusChange(status: String, error: String?)
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var listener: ASRResultListener? = null
    private var apiKey: String = ""
    private val eventIdCounter = AtomicLong(0)
    @Volatile
    private var started = false

    fun setListener(listener: ASRResultListener) {
        this.listener = listener
    }

    fun start(apiKey: String, onConnected: (() -> Unit)? = null) {
        if (started) {
            throw IllegalStateException("ASR 会话已在运行")
        }
        this.apiKey = apiKey
        started = true

        val request = Request.Builder()
            .url(ASR_ENDPOINT)
            .header("Authorization", "Bearer $apiKey")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listener?.onStatusChange("connected", null)
                sendSessionUpdate()
                onConnected?.invoke()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                listener?.onStatusChange("closing", reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener?.onStatusChange("closed", reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener?.onStatusChange("error", t.message ?: t.toString())
            }
        })
    }

    private fun nextEventId(): String {
        return "evt_${System.currentTimeMillis()}_${eventIdCounter.incrementAndGet()}"
    }

    private fun sendSessionUpdate() {
        val message = SessionUpdateMessage(
            eventId = nextEventId(),
            type = "session.update",
            session = SessionConfig(
                audio = AudioConfig(
                    input = InputConfig(
                        format = AudioFormat(
                            type = "pcm",
                            codec = "pcm_s16le",
                            rate = 16000,
                            bits = 16,
                            channel = 1
                        ),
                        transcription = TranscriptionConfig(
                            model = MODEL,
                            language = "zh",
                            enableItn = true
                        ),
                        turnDetection = TurnDetectionConfig(
                            type = "server_vad",
                            silenceDurationMs = 800,
                            threshold = 0.5
                        )
                    )
                )
            )
        )
        send(json.encodeToString(message))
    }

    fun sendAudio(base64PcmData: String) {
        if (!started) return
        val message = AudioAppendMessage(
            eventId = nextEventId(),
            type = "input_audio_buffer.append",
            audio = base64PcmData
        )
        send(json.encodeToString(message))
    }

    private fun send(text: String) {
        webSocket?.send(text)
    }

    private fun handleMessage(data: String) {
        try {
            val event = json.decodeFromString<ASREvent>(data)
            when (event.type) {
                "conversation.item.input_audio_transcription.delta" -> {
                    val text = event.text ?: ""
                    val stash = event.stash ?: ""
                    listener?.onResult(text, false, stash)
                }
                "conversation.item.input_audio_transcription.completed" -> {
                    val transcript = event.transcript ?: ""
                    listener?.onResult(transcript, true, "")
                }
                "error" -> {
                    listener?.onStatusChange("error", event.error?.message)
                }
                else -> {}
            }
        } catch (e: Exception) {
            listener?.onStatusChange("error", "解析消息失败: ${e.message}")
        }
    }

    fun stop() {
        started = false
        webSocket?.close(1000, "User stopped")
        webSocket = null
    }

    // --- Data classes for JSON serialization ---

    @Serializable
    data class SessionUpdateMessage(
        val eventId: String,
        val type: String,
        val session: SessionConfig
    )

    @Serializable
    data class SessionConfig(val audio: AudioConfig)

    @Serializable
    data class AudioConfig(val input: InputConfig)

    @Serializable
    data class InputConfig(
        val format: AudioFormat,
        val transcription: TranscriptionConfig,
        val turnDetection: TurnDetectionConfig
    )

    @Serializable
    data class AudioFormat(
        val type: String,
        val codec: String,
        val rate: Int,
        val bits: Int,
        val channel: Int
    )

    @Serializable
    data class TranscriptionConfig(
        val model: String,
        val language: String,
        val enableItn: Boolean
    )

    @Serializable
    data class TurnDetectionConfig(
        val type: String,
        val silenceDurationMs: Int,
        val threshold: Double
    )

    @Serializable
    data class AudioAppendMessage(
        val eventId: String,
        val type: String,
        val audio: String
    )

    @Serializable
    data class ASREvent(
        val type: String,
        val text: String? = null,
        val stash: String? = null,
        val transcript: String? = null,
        val error: ErrorDetail? = null
    )

    @Serializable
    data class ErrorDetail(val message: String? = null)
}
