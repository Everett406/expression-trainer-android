package com.sisi.expressiontrainer.data.local

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EmotionEntry(
    val category: String,
    val subcategory: String,
    val intensity: Int,
    val polarity: String
)

@Serializable
data class EmotionLexicon(
    @SerialName("_meta")
    val meta: LexiconMeta? = null,
    val emotions: Map<String, EmotionEntry> = emptyMap(),
    @SerialName("vagueToPresice")
    val vagueToPrecise: Map<String, List<String>> = emptyMap(),
    val fillerWords: List<String> = emptyList(),
    val hedgeWords: List<String> = emptyList(),
    val intensityScale: Map<String, List<String>> = emptyMap(),
    val vividDescriptions: Map<String, List<String>> = emptyMap(),
    @SerialName("hedgeToDirectMap")
    val hedgeToDirectMap: Map<String, String> = emptyMap()
)

@Serializable
data class LexiconMeta(
    val description: String,
    val categories: List<String>,
    val version: String,
    val totalWords: Int
)

class EmotionLexiconLoader(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): EmotionLexicon {
        val text = context.assets.open("emotion-lexicon.json").bufferedReader().use { it.readText() }
        return json.decodeFromString(text)
    }
}
