package com.sisi.expressiontrainer.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sisi.expressiontrainer.data.model.SubtitleLine

@Composable
fun SubtitlePane(subtitles: List<SubtitleLine>) {
    val listState = rememberLazyListState()

    LaunchedEffect(subtitles.size) {
        if (subtitles.isNotEmpty()) {
            listState.animateScrollToItem(subtitles.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(subtitles, key = { it.hashCode() }) { line ->
            val annotated = highlightText(line.text)
            Text(
                text = annotated,
                style = if (line.isFinal) {
                    MaterialTheme.typography.bodyLarge
                } else {
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.padding(vertical = 4.dp)
            )
            if (line.stash.isNotBlank()) {
                Text(
                    text = line.stash,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun highlightText(text: String): AnnotatedString {
    val vagueWords = listOf(
        "开心", "难过", "害怕", "生气", "不舒服", "很好", "很多", "很快", "很大", "很小",
        "好看", "不好", "喜欢", "讨厌", "觉得", "想", "做", "看", "说", "想想"
    )
    val fillerWords = listOf("嗯", "啊", "呃", "额", "那个", "就是", "然后", "这个", "对吧", "是吧", "反正", "基本上")
    val hedgeWords = listOf("可能", "也许", "大概", "应该", "我觉得", "好像", "似乎", "或许", "不一定", "差不多", "感觉")

    return buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            val match = (vagueWords + fillerWords + hedgeWords)
                .mapNotNull { word ->
                    val index = remaining.indexOf(word)
                    if (index >= 0) Triple(word, index, when (word) {
                        in vagueWords -> "vague"
                        in fillerWords -> "filler"
                        else -> "hedge"
                    }) else null
                }
                .minByOrNull { it.second }

            if (match == null) {
                append(remaining)
                break
            }

            val (word, index, type) = match
            if (index > 0) {
                append(remaining.substring(0, index))
            }

            val color = when (type) {
                "vague" -> MaterialTheme.colorScheme.tertiary
                "filler" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
            withStyle(SpanStyle(color = color)) {
                append(word)
            }

            remaining = remaining.substring(index + word.length)
        }
    }
}
