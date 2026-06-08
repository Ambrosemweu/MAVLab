package com.ascend.mavlab.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun RollingChart(
    title: String,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(top = 12.dp),
            ) {
                val axisColor = Color.White.copy(alpha = 0.16f)
                val maxMagnitude = series
                    .flatMap { it.values }
                    .fold(1f) { current, value -> maxOf(current, abs(value)) }
                val centerY = size.height / 2f
                val xStep = size.width / maxOf(1, (series.maxOfOrNull { it.values.size } ?: 1) - 1)

                drawLine(
                    color = axisColor,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 1f,
                )

                series.forEach { chartSeries ->
                    val values = chartSeries.values
                    if (values.size < 2) return@forEach
                    values.zipWithNext().forEachIndexed { index, pair ->
                        val start = Offset(
                            x = index * xStep,
                            y = centerY - (pair.first / maxMagnitude) * centerY,
                        )
                        val end = Offset(
                            x = (index + 1) * xStep,
                            y = centerY - (pair.second / maxMagnitude) * centerY,
                        )
                        drawLine(
                            color = chartSeries.color,
                            start = start,
                            end = end,
                            strokeWidth = 3f,
                        )
                    }
                }
            }
        }
    }
}

data class ChartSeries(
    val label: String,
    val values: List<Float>,
    val color: Color,
)
