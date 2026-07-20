package com.sisi.expressiontrainer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sisi.expressiontrainer.data.model.AnalysisResult
import com.sisi.expressiontrainer.data.model.ReportStats

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsPane(
    stats: ReportStats,
    analysisResults: List<AnalysisResult>
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "填充词",
                value = stats.fillers.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "犹豫词",
                value = stats.hedges.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "笼统词",
                value = stats.vagueWords.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "表达密度",
                value = calculateDensity(stats),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "详情",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (analysisResults.isEmpty()) {
            Text(
                text = "录制中实时统计…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val allFillers = analysisResults.flatMap { it.fillers }.map { it.word }
            val allHedges = analysisResults.flatMap { it.hedges }.map { it.word }
            val allVague = analysisResults.flatMap { it.vagueWords }.map { it.word }.distinct()

            if (allFillers.isNotEmpty()) {
                DetailSection(title = "填充词", items = countWords(allFillers))
            }
            if (allHedges.isNotEmpty()) {
                DetailSection(title = "犹豫词", items = countWords(allHedges))
            }
            if (allVague.isNotEmpty()) {
                DetailSection(title = "笼统词", items = allVague.map { it to 1 })
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun DetailSection(title: String, items: List<Pair<String, Int>>) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = items.joinToString("、") { "${it.first} (${it.second}次)" },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun calculateDensity(stats: ReportStats): String {
    val total = stats.totalWords
    return if (total > 0) {
        val meaningful = (total - stats.fillers - stats.hedges).coerceAtLeast(0)
        "${(meaningful * 100 / total)}%"
    } else {
        "100%"
    }
}

private fun countWords(words: List<String>): List<Pair<String, Int>> {
    return words.groupingBy { it }.eachCount()
        .toList()
        .sortedByDescending { it.second }
}
