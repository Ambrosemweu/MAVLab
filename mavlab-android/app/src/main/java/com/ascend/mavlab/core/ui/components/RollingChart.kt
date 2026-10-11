package com.ascend.mavlab.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RollingChart(
    title: String,
    series: List<ChartSeries>,
    leftAxis: ChartAxisSpec,
    rightAxis: ChartAxisSpec,
    modifier: Modifier = Modifier,
    sourceLabel: String = "Simulated vehicle",
) {
    val leftScale = rollingChartScaleForAxis(series, ChartAxis.LEFT, leftAxis.symmetricAroundZero)
    val rightScale = rollingChartScaleForAxis(series, ChartAxis.RIGHT, rightAxis.symmetricAroundZero)
    val plotHeight = 180.dp
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val traceDescription = "$title, $sourceLabel, last 10 seconds. " +
        "Left axis, ${leftAxis.label}, ${chartValue(leftScale.minimum, leftAxis.unit)} to ${chartValue(leftScale.maximum, leftAxis.unit)}. " +
        "Right axis, ${rightAxis.label}, ${chartValue(rightScale.minimum, rightAxis.unit)} to ${chartValue(rightScale.maximum, rightAxis.unit)}."
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "$sourceLabel · last 10 seconds",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            series.forEach { chartSeries ->
                val unit = if (chartSeries.axis == ChartAxis.LEFT) leftAxis.unit else rightAxis.unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Canvas(modifier = Modifier.size(width = 28.dp, height = 12.dp)) {
                        drawLine(
                            color = chartSeries.color,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = chartSeries.lineStyle.pathEffect(density),
                        )
                    }
                    Text(chartSeries.label, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = chartValue(chartSeries.values.lastOrNull(), unit),
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = chartSeries.color,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${leftAxis.label} (${leftAxis.unit})",
                    style = MaterialTheme.typography.labelSmall,
                    color = leftAxis.color,
                )
                Text(
                    text = "${rightAxis.label} (${rightAxis.unit})",
                    style = MaterialTheme.typography.labelSmall,
                    color = rightAxis.color,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(
                    modifier = Modifier.height(plotHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    ChartAxisLabel(leftScale.maximum, leftAxis)
                    ChartAxisLabel((leftScale.maximum + leftScale.minimum) / 2f, leftAxis)
                    ChartAxisLabel(leftScale.minimum, leftAxis)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(plotHeight)
                            .semantics { contentDescription = traceDescription },
                    ) {
                        listOf(0f, 0.5f, 1f).forEach { fraction ->
                            drawLine(
                                color = axisColor,
                                start = Offset(0f, fraction * size.height),
                                end = Offset(size.width, fraction * size.height),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                        series.forEach { chartSeries ->
                            val scale = if (chartSeries.axis == ChartAxis.LEFT) leftScale else rightScale
                            val values = chartSeries.values
                            val path = Path()
                            var penDown = false
                            values.forEachIndexed { index, value ->
                                if (value.isFinite()) {
                                    val x = rollingChartTimeFraction(index, values.size) * size.width
                                    val y = scale.heightFraction(value) * size.height
                                    if (penDown) path.lineTo(x, y) else path.moveTo(x, y)
                                    penDown = true
                                } else {
                                    penDown = false
                                }
                            }
                            drawPath(
                                path = path,
                                color = chartSeries.color,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = chartSeries.lineStyle.pathEffect(density),
                                ),
                            )
                            values.lastOrNull()?.takeIf { it.isFinite() }?.let { latest ->
                                drawCircle(
                                    color = chartSeries.color,
                                    radius = 3.dp.toPx(),
                                    center = Offset(size.width, scale.heightFraction(latest) * size.height),
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("10 s ago", style = MaterialTheme.typography.labelSmall)
                        Text("5 s", style = MaterialTheme.typography.labelSmall)
                        Text("Now", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Column(
                    modifier = Modifier.height(plotHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start,
                ) {
                    ChartAxisLabel(rightScale.maximum, rightAxis)
                    ChartAxisLabel((rightScale.maximum + rightScale.minimum) / 2f, rightAxis)
                    ChartAxisLabel(rightScale.minimum, rightAxis)
                }
            }
            if (series.none { chartSeries -> chartSeries.values.any { it.isFinite() } }) {
                Text(
                    text = "Waiting for telemetry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChartAxisLabel(value: Float, axis: ChartAxisSpec) {
    Text(
        text = chartValue(value, axis.unit),
        style = MaterialTheme.typography.labelSmall,
        color = axis.color,
    )
}

private fun chartValue(value: Float?, unit: String): String {
    if (value == null || !value.isFinite()) return "—"
    return "%.1f%s".format(value, if (unit == "°") unit else " $unit")
}

enum class ChartLineStyle {
    SOLID,
    DASHED,
    DOTTED,
}

enum class ChartAxis {
    LEFT,
    RIGHT,
}

data class ChartAxisSpec(
    val label: String,
    val unit: String,
    val symmetricAroundZero: Boolean,
    val color: Color,
)

private fun ChartLineStyle.pathEffect(density: Float): PathEffect? = when (this) {
    ChartLineStyle.SOLID -> null
    ChartLineStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(8f * density, 5f * density))
    ChartLineStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(2f * density, 5f * density))
}

data class ChartSeries(
    val label: String,
    val values: List<Float>,
    val color: Color,
    val axis: ChartAxis,
    val lineStyle: ChartLineStyle = ChartLineStyle.SOLID,
)
