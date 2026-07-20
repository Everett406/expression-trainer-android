package com.sisi.expressiontrainer.data.model

enum class FeedbackType {
    GOOD, VAGUE, FILLER, HEDGE, AI
}

data class FeedbackItem(
    val text: String,
    val type: FeedbackType
)
