package com.sisi.expressiontrainer.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sisi.expressiontrainer.ai.AIFeedbackClient
import com.sisi.expressiontrainer.ai.Prompts
import com.sisi.expressiontrainer.asr.AudioRecorder
import com.sisi.expressiontrainer.asr.StepFunASRClient
import com.sisi.expressiontrainer.data.local.SettingsDataStore
import com.sisi.expressiontrainer.data.model.AnalysisResult
import com.sisi.expressiontrainer.data.model.CustomPrompt
import com.sisi.expressiontrainer.data.model.FeedbackItem
import com.sisi.expressiontrainer.data.model.FeedbackType
import com.sisi.expressiontrainer.data.model.ReportStats
import com.sisi.expressiontrainer.data.model.Settings
import com.sisi.expressiontrainer.data.model.SubtitleLine
import com.sisi.expressiontrainer.lexicon.LexiconAnalyzer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrainerViewModel(
    application: Application,
    private val settingsDataStore: SettingsDataStore,
    private val lexiconAnalyzer: LexiconAnalyzer
) : AndroidViewModel(application) {

    companion object {
        private const val FEEDBACK_INTERVAL_CHARS = 30
        private const val MAX_FEEDBACK_ITEMS = 20

        fun Factory(
            application: Application,
            settingsDataStore: SettingsDataStore,
            lexiconAnalyzer: LexiconAnalyzer
        ) = viewModelFactory {
            initializer {
                TrainerViewModel(application, settingsDataStore, lexiconAnalyzer)
            }
        }
    }

    // Settings
    private val _settings = mutableStateOf(Settings())
    val settings: State<Settings> = _settings

    private val _customPrompt = mutableStateOf(CustomPrompt())
    val customPrompt: State<CustomPrompt> = _customPrompt

    // Recording state
    private val _isRecording = mutableStateOf(false)
    val isRecording: State<Boolean> = _isRecording

    private val _isPaused = mutableStateOf(false)
    val isPaused: State<Boolean> = _isPaused

    private val _elapsedSeconds = mutableIntStateOf(0)
    val elapsedSeconds: State<Int> = _elapsedSeconds

    // Subtitles
    private val _subtitles = mutableStateListOf<SubtitleLine>()
    val subtitles: List<SubtitleLine> = _subtitles

    // Stats
    private val _reportStats = mutableStateOf(ReportStats())
    val reportStats: State<ReportStats> = _reportStats

    private val _analysisResults = mutableStateListOf<AnalysisResult>()
    val analysisResults: List<AnalysisResult> = _analysisResults

    // Feedback
    private val _feedbackItems = mutableStateListOf<FeedbackItem>()
    val feedbackItems: List<FeedbackItem> = _feedbackItems

    // Report
    private val _reportMarkdown = mutableStateOf<String?>(null)
    val reportMarkdown: State<String?> = _reportMarkdown

    private val _isGeneratingReport = mutableStateOf(false)
    val isGeneratingReport: State<Boolean> = _isGeneratingReport

    // Error / status
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _asrStatus = mutableStateOf<String>("idle")
    val asrStatus: State<String> = _asrStatus

    // Internals
    private var audioRecorder: AudioRecorder? = null
    private var asrClient: StepFunASRClient? = null
    private val aiClient = AIFeedbackClient()

    private var recordingStartTime = 0L
    private var pausedDuration = 0L
    private var pauseStartTime = 0L
    private var timerJob: Job? = null
    private var lastFeedbackLength = 0
    private val fullTextBuilder = StringBuilder()

    init {
        viewModelScope.launch {
            settingsDataStore.settings.collect { _settings.value = it }
        }
        viewModelScope.launch {
            settingsDataStore.customPrompt.collect { _customPrompt.value = it }
        }
    }

    //region Settings

    fun updateSettings(settings: Settings) {
        _settings.value = settings
        viewModelScope.launch {
            settingsDataStore.saveSettings(settings)
        }
    }

    fun updateCustomPrompt(customPrompt: CustomPrompt) {
        _customPrompt.value = customPrompt
        viewModelScope.launch {
            settingsDataStore.saveCustomPrompt(customPrompt)
        }
    }

    //endregion

    //region Recording

    fun startRecording() {
        if (_settings.value.asrApiKey.isBlank()) {
            _errorMessage.value = "请先在设置中配置阶跃 ASR API Key"
            return
        }

        try {
            resetSession()
            _isRecording.value = true
            _isPaused.value = false
            recordingStartTime = System.currentTimeMillis()
            pausedDuration = 0L
            startTimer()

            asrClient = StepFunASRClient().apply {
                setListener(object : StepFunASRClient.ASRResultListener {
                    override fun onResult(text: String, isFinal: Boolean, stash: String) {
                        handleASRResult(text, isFinal, stash)
                    }

                    override fun onStatusChange(status: String, error: String?) {
                        _asrStatus.value = status
                        if (error != null) {
                            _errorMessage.value = "ASR: $error"
                        }
                    }
                })
                start(_settings.value.asrApiKey) {
                    startAudioRecorder()
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = "开始录制失败: ${e.message}"
            stopRecording()
        }
    }

    private fun startAudioRecorder() {
        audioRecorder = AudioRecorder(getApplication()).apply {
            try {
                start { base64Pcm ->
                    if (_isRecording.value && !_isPaused.value) {
                        asrClient?.sendAudio(base64Pcm)
                    }
                }
            } catch (e: SecurityException) {
                _errorMessage.value = "麦克风权限被拒绝"
            }
        }
    }

    fun pauseRecording() {
        _isPaused.value = true
        pauseStartTime = System.currentTimeMillis()
    }

    fun resumeRecording() {
        pausedDuration += System.currentTimeMillis() - pauseStartTime
        _isPaused.value = false
    }

    fun stopRecording() {
        _isRecording.value = false
        _isPaused.value = false
        stopTimer()
        audioRecorder?.stop()
        audioRecorder = null
        asrClient?.stop()
        asrClient = null

        val totalPaused = if (pauseStartTime > 0L) {
            pausedDuration + (System.currentTimeMillis() - pauseStartTime)
        } else pausedDuration
        _reportStats.value = _reportStats.value.copy(
            duration = ((System.currentTimeMillis() - recordingStartTime - totalPaused) / 1000).toInt()
        )
    }

    //endregion

    //region ASR Result Handling

    private fun handleASRResult(text: String, isFinal: Boolean, stash: String) {
        if (text.isBlank()) return

        // Update subtitle
        if (isFinal) {
            _subtitles.removeAll { !it.isFinal }
            _subtitles.add(SubtitleLine(text, true))
            fullTextBuilder.append(text)

            val analysis = lexiconAnalyzer.analyzeText(text)
            if (analysis != null) {
                _analysisResults.add(analysis)
                updateStats(analysis)
                addLexiconSuggestions(analysis)
            }

            if (fullTextBuilder.length - lastFeedbackLength >= FEEDBACK_INTERVAL_CHARS) {
                requestRealtimeFeedback()
            }
        } else {
            _subtitles.removeAll { !it.isFinal }
            _subtitles.add(SubtitleLine(text, false, stash))
        }
    }

    private fun updateStats(analysis: AnalysisResult) {
        _reportStats.value = _reportStats.value.copy(
            totalWords = _reportStats.value.totalWords + analysis.totalWords,
            fillers = _reportStats.value.fillers + analysis.fillers.size,
            hedges = _reportStats.value.hedges + analysis.hedges.size,
            vagueWords = _reportStats.value.vagueWords + analysis.vagueWords.size
        )
    }

    private fun addLexiconSuggestions(analysis: AnalysisResult) {
        analysis.vagueWords.forEach { vague ->
            val alts = vague.alternatives.take(3).joinToString(" / ")
            addFeedbackItem("「${vague.word}」→ $alts", FeedbackType.VAGUE)
        }
        if (analysis.fillers.size >= 2) {
            val unique = analysis.fillers.map { it.word }.distinct().take(3).joinToString("、")
            addFeedbackItem("填充词：$unique —— 试试停顿", FeedbackType.FILLER)
        }
        if (analysis.hedges.isNotEmpty()) {
            val unique = analysis.hedges.map { it.word }.distinct().take(2).joinToString("」「", "「", "」")
            addFeedbackItem("$unique → 直接说", FeedbackType.HEDGE)
        }
    }

    //endregion

    //region Timer

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (_isRecording.value && !_isPaused.value) {
                    val totalPaused = if (pauseStartTime > 0L) {
                        pausedDuration + (System.currentTimeMillis() - pauseStartTime)
                    } else pausedDuration
                    _elapsedSeconds.intValue =
                        ((System.currentTimeMillis() - recordingStartTime - totalPaused) / 1000).toInt()
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    //endregion

    //region AI Feedback

    private fun requestRealtimeFeedback() {
        val currentText = fullTextBuilder.toString()
        lastFeedbackLength = currentText.length
        val currentSettings = _settings.value
        val currentPrompt = _customPrompt.value

        if (currentSettings.aiApiKey.isBlank() && currentSettings.aiProvider != "ollama") return

        viewModelScope.launch {
            try {
                val feedback = aiClient.sendFeedback(currentText, currentSettings, currentPrompt)
                feedback.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { line ->
                        addFeedbackItem(line, classifyFeedback(line))
                    }
            } catch (e: Exception) {
                // 实时反馈失败不打扰用户
            }
        }
    }

    private fun classifyFeedback(text: String): FeedbackType {
        return when {
            text.contains("✓") || text.contains("⭐") -> FeedbackType.GOOD
            text.contains("→") -> FeedbackType.VAGUE
            listOf("嗯", "啊", "呃", "那个", "就是", "然后", "这个", "对吧", "是吧", "反正")
                .any { text.contains(it) } -> FeedbackType.FILLER
            listOf("可能", "也许", "大概", "应该", "我觉得", "好像", "似乎", "感觉")
                .any { text.contains(it) } -> FeedbackType.HEDGE
            else -> FeedbackType.AI
        }
    }

    private fun addFeedbackItem(text: String, type: FeedbackType) {
        if (_feedbackItems.any { it.text == text }) return
        _feedbackItems.add(0, FeedbackItem(text, type))
        while (_feedbackItems.size > MAX_FEEDBACK_ITEMS) {
            _feedbackItems.removeLast()
        }
    }

    //endregion

    //region Report

    fun generateReport() {
        val currentSettings = _settings.value
        val currentPrompt = _customPrompt.value
        val fullText = fullTextBuilder.toString()

        if (currentSettings.aiApiKey.isBlank() && currentSettings.aiProvider != "ollama") {
            _errorMessage.value = "请先在设置中配置 AI API Key"
            return
        }

        _isGeneratingReport.value = true
        viewModelScope.launch {
            try {
                val report = aiClient.sendReport(
                    fullText = fullText,
                    stats = Prompts.ReportPromptStats(
                        duration = _reportStats.value.duration,
                        totalWords = _reportStats.value.totalWords,
                        fillers = _reportStats.value.fillers,
                        hedges = _reportStats.value.hedges,
                        vagueWords = _reportStats.value.vagueWords
                    ),
                    settings = currentSettings,
                    customPrompt = currentPrompt
                )
                _reportMarkdown.value = report
            } catch (e: Exception) {
                _errorMessage.value = "生成报告失败: ${e.message}"
            } finally {
                _isGeneratingReport.value = false
            }
        }
    }

    fun clearReport() {
        _reportMarkdown.value = null
    }

    fun copyReportToClipboard(report: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("表达训练报告", report))
    }

    fun saveReportToFile(report: String): String? {
        return try {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val timeStr = SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())
            val filename = "expression-$dateStr-$timeStr.md"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)
            val content = buildString {
                appendLine("# 表达训练报告")
                appendLine()
                appendLine("**日期**: $dateStr")
                appendLine("**时长**: ${_reportStats.value.duration}秒")
                appendLine("**总字数**: ${_reportStats.value.totalWords}")
                appendLine()
                appendLine("---")
                appendLine()
                appendLine("## 完整原文")
                appendLine()
                appendLine(fullTextBuilder.toString())
                appendLine()
                appendLine("---")
                appendLine()
                append(report)
            }
            file.writeText(content)
            file.absolutePath
        } catch (e: Exception) {
            _errorMessage.value = "保存失败: ${e.message}"
            null
        }
    }

    //endregion

    //region Paste Text

    fun analyzePastedText(text: String) {
        resetSession()
        fullTextBuilder.append(text)

        val sentences = text.split(Regex("(?<=[。！？\n])")).filter { it.isNotBlank() }
        sentences.forEach { sentence ->
            val trimmed = sentence.trim()
            _subtitles.add(SubtitleLine(trimmed, true))
            val analysis = lexiconAnalyzer.analyzeText(trimmed)
            if (analysis != null) {
                _analysisResults.add(analysis)
                updateStats(analysis)
            }
        }

        _reportStats.value = _reportStats.value.copy(duration = 0)
        requestRealtimeFeedback()
    }

    //endregion

    //region Utility

    fun clearError() {
        _errorMessage.value = null
    }

    private fun resetSession() {
        stopRecording()
        _subtitles.clear()
        _feedbackItems.clear()
        _analysisResults.clear()
        _reportStats.value = ReportStats()
        _elapsedSeconds.intValue = 0
        _reportMarkdown.value = null
        lastFeedbackLength = 0
        fullTextBuilder.clear()
    }

    fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    //endregion
}
