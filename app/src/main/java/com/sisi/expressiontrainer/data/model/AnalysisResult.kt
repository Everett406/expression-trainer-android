package com.sisi.expressiontrainer.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LexiconMatch(
    val word: String,
    val position: Int
)

@Serializable
data class VagueWordMatch(
    val word: String,
    val position: Int,
    val alternatives: List<String>
)

@Serializable
data class EmotionWordMatch(
    val word: String,
    val position: Int,
    val category: String,
    val subcategory: String,
    val intensity: Int,
    val polarity: String
)

@Serializable
data class Suggestion(
    val type: String,
    val original: String = "",
    val alternatives: List<String> = emptyList(),
    val message: String
)

@Serializable
data class AnalysisResult(
    val totalWords: Int,
    val fillers: List<LexiconMatch>,
    val hedges: List<LexiconMatch>,
    val vagueWords: List<VagueWordMatch>,
    val emotionWords: List<EmotionWordMatch>,
    val density: Int,
    val suggestions: List<Suggestion>
)
