package com.sisi.expressiontrainer.data.model

data class SubtitleLine(
    val text: String,
    val isFinal: Boolean,
    val stash: String = ""
)
