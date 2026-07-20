package com.sisi.expressiontrainer.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ReportStats(
    val duration: Int = 0,
    val totalWords: Int = 0,
    val fillers: Int = 0,
    val hedges: Int = 0,
    val vagueWords: Int = 0
)
