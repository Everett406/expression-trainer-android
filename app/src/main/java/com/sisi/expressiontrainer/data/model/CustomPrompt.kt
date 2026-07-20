package com.sisi.expressiontrainer.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomPrompt(
    val goals: String = "",
    val customRules: String = "",
    val styleRef: String = "",
    val customWords: String = ""
)
