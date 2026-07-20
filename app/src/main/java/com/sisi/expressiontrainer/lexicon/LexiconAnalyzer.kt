package com.sisi.expressiontrainer.lexicon

import com.sisi.expressiontrainer.data.local.EmotionLexicon
import com.sisi.expressiontrainer.data.model.AnalysisResult
import com.sisi.expressiontrainer.data.model.EmotionWordMatch
import com.sisi.expressiontrainer.data.model.LexiconMatch
import com.sisi.expressiontrainer.data.model.Suggestion
import com.sisi.expressiontrainer.data.model.VagueWordMatch
import kotlin.math.min

class LexiconAnalyzer(private val lexicon: EmotionLexicon) {

    // 合并硬编码精简版 + JSON 扩展版
    private val fillerWords: Set<String> = buildSet {
        addAll(lexicon.fillerWords)
        addAll(
            listOf(
                "嗯", "啊", "呃", "额", "那个", "就是", "然后",
                "这个", "对吧", "是吧", "你知道", "怎么说呢",
                "反正", "基本上", "总之", "所以说"
            )
        )
    }

    private val hedgeWords: Set<String> = buildSet {
        addAll(lexicon.hedgeWords)
        addAll(
            listOf(
                "可能", "也许", "大概", "应该", "我觉得", "好像",
                "似乎", "或许", "不一定", "差不多", "算是",
                "某种程度上", "一般来说", "感觉"
            )
        )
    }

    private val vagueToPrecise: Map<String, List<String>> = buildMap {
        putAll(lexicon.vagueToPrecise)
        putAll(
            mapOf(
                "开心" to listOf("欣喜", "雀跃", "兴奋", "欣慰", "畅快", "满足"),
                "难过" to listOf("心酸", "失落", "委屈", "心疼", "沮丧", "低落"),
                "害怕" to listOf("恐惧", "焦虑", "不安", "慌张", "胆怯", "忐忑"),
                "生气" to listOf("愤怒", "恼火", "窝火", "气愤", "不满", "暴躁"),
                "不舒服" to listOf("压抑", "烦躁", "憋屈", "窒息", "煎熬", "疲惫"),
                "很好" to listOf("出色", "精彩", "优秀", "惊艳", "完美", "理想"),
                "很多" to listOf("大量", "海量", "充裕", "丰富", "密集", "可观"),
                "很快" to listOf("迅速", "飞速", "立刻", "瞬间", "即刻", "火速"),
                "很大" to listOf("巨大", "庞大", "显著", "惊人", "可观", "壮观"),
                "很小" to listOf("微小", "细微", "轻微", "渺小", "微不足道", "些许"),
                "好看" to listOf("精致", "优雅", "绚丽", "惊艳", "别致", "夺目"),
                "不好" to listOf("糟糕", "恶劣", "拙劣", "不堪", "惨淡", "低劣"),
                "喜欢" to listOf("热爱", "痴迷", "着迷", "钟爱", "倾心", "沉醉"),
                "讨厌" to listOf("厌恶", "反感", "排斥", "憎恨", "鄙视", "嫌弃"),
                "觉得" to listOf("认为", "判断", "确信", "推断", "意识到", "发现"),
                "想" to listOf("渴望", "期待", "向往", "盼望", "企图", "打算"),
                "做" to listOf("执行", "落实", "推进", "完成", "实施", "操作"),
                "看" to listOf("审视", "观察", "注视", "打量", "端详", "凝视"),
                "说" to listOf("表达", "阐述", "强调", "指出", "坦言", "声明"),
                "想想" to listOf("反思", "回顾", "审视", "复盘", "琢磨", "斟酌")
            )
        )
    }

    private val emotions = lexicon.emotions

    private val dictionary: Set<String> by lazy {
        buildSet {
            addAll(fillerWords)
            addAll(hedgeWords)
            addAll(vagueToPrecise.keys)
            addAll(emotions.keys)
        }
    }

    private val maxWordLength = 6

    fun analyzeText(text: String): AnalysisResult? {
        if (text.isBlank()) return null

        val words = segmentText(text)
        val totalWords = words.size

        val fillers = mutableListOf<LexiconMatch>()
        val hedges = mutableListOf<LexiconMatch>()
        val vagueWords = mutableListOf<VagueWordMatch>()
        val emotionWords = mutableListOf<EmotionWordMatch>()

        words.forEachIndexed { index, word ->
            when {
                fillerWords.contains(word) -> fillers.add(LexiconMatch(word, index))
                hedgeWords.contains(word) -> hedges.add(LexiconMatch(word, index))
            }
            vagueToPrecise[word]?.let { alternatives ->
                vagueWords.add(VagueWordMatch(word, index, alternatives))
            }
            emotions[word]?.let { entry ->
                emotionWords.add(
                    EmotionWordMatch(
                        word = word,
                        position = index,
                        category = entry.category,
                        subcategory = entry.subcategory,
                        intensity = entry.intensity,
                        polarity = entry.polarity
                    )
                )
            }
        }

        val meaningfulWords = totalWords - fillers.size - hedges.size
        val density = if (totalWords > 0) (meaningfulWords * 100 / totalWords) else 100

        return AnalysisResult(
            totalWords = totalWords,
            fillers = fillers,
            hedges = hedges,
            vagueWords = vagueWords,
            emotionWords = emotionWords,
            density = density.coerceIn(0, 100),
            suggestions = generateSuggestions(vagueWords, fillers, hedges)
        )
    }

    private fun segmentText(text: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            var matched = false
            val maxLen = min(maxWordLength, text.length - i)
            for (len in maxLen downTo 2) {
                val word = text.substring(i, i + len)
                if (dictionary.contains(word)) {
                    result.add(word)
                    i += len
                    matched = true
                    break
                }
            }
            if (!matched) {
                result.add(text[i].toString())
                i++
            }
        }
        return result
    }

    private fun generateSuggestions(
        vagueWords: List<VagueWordMatch>,
        fillers: List<LexiconMatch>,
        hedges: List<LexiconMatch>
    ): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()

        vagueWords.forEach { item ->
            val topAlts = item.alternatives.take(3)
            suggestions.add(
                Suggestion(
                    type = "vague",
                    original = item.word,
                    alternatives = topAlts,
                    message = "「${item.word}」→ 试试更精准的：${topAlts.joinToString("、")}"
                )
            )
        }

        if (fillers.size >= 3) {
            val topFillers = fillers.map { it.word }.distinct().take(3)
            suggestions.add(
                Suggestion(
                    type = "filler",
                    message = "填充词偏多（${fillers.size}次）：${topFillers.joinToString("、")}。试试用停顿替代"
                )
            )
        }

        if (hedges.size >= 2) {
            suggestions.add(
                Suggestion(
                    type = "hedge",
                    message = "犹豫表达较多（${hedges.size}次）。试试把「我觉得」改成直接陈述"
                )
            )
        }

        return suggestions
    }
}
