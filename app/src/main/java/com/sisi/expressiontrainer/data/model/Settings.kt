package com.sisi.expressiontrainer.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val asrApiKey: String = "",
    val aiProvider: String = "deepseek",
    val aiApiKey: String = "",
    val aiModel: String = "deepseek-chat",
    val customEndpoint: String = ""
)
