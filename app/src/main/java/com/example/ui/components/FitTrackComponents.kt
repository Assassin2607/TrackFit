package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.SyncState
import com.example.ui.theme.FitTrackThemeExt
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricSmall
import kotlin.math.max
import kotlin.math.min

@Composable
fun FitTrackCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = FitTrackThemeExt.colors.mutedText
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun SyncStatusBadge(
    syncState: SyncState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon, text) = when (syncState) {
        is SyncState.Synced -> Quad(
            FitTrackThemeExt.colors.successContainer,
            FitTrackThemeExt.colors.success,
            Icons.Rounded.CloudDone,
            "Synced"
        )
        is SyncState.Syncing -> Quad(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Rounded.Sync,
            "Syncing..."
        )
        is SyncState.Pending -> Quad(
            FitTrackThemeExt.colors.warningContainer,
            FitTrackThemeExt.colors.warning,
            Icons.Rounded.CloudOff,
            "Waiting to sync (${syncState.count})"
        )
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .testTag("sync_status_badge"),
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Sync status",
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun DailyScoreRing(
    score: Int,
    size: Dp = 112.dp,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 220f),
        label = "scoreRingProgress"
    )
    val ringColor = when {
        score >= 80 -> FitTrackThemeExt.colors.success
        score >= 55 -> MaterialTheme.colorScheme.primary
        score >= 30 -> FitTrackThemeExt.colors.warning
        else -> MaterialTheme.colorScheme.error
    }
    val trackColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 11.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MonoMetricLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "/100",
                style = MaterialTheme.typography.labelSmall,
                color = FitTrackThemeExt.colors.mutedText
            )
        }
    }
}

@Composable
fun MacroProgressBar(
    label: String,
    consumed: Int,
    target: Int,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val ratio = if (target > 0) (consumed.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val remaining = target - consumed
    val remainingText = if (remaining >= 0) "$remaining$unit left" else "+${-remaining}$unit over"

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$consumed / $target $unit",
                    style = MonoMetricSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "($remainingText)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (remaining < 0) FitTrackThemeExt.colors.warning else FitTrackThemeExt.colors.mutedText
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = accentColor,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun WeightTrendLineChart(
    dailyPoints: List<Pair<String, Double>>,
    rollingAvgPoints: List<Pair<String, Double>>,
    goalValue: Double?,
    unitLabel: String,
    modifier: Modifier = Modifier
) {
    if (dailyPoints.isEmpty()) {
        EmptyStateCard(
            icon = Icons.Rounded.Info,
            title = "No weight entries yet",
            subtitle = "Log your first weigh-in to plot daily weight vs. 7-day rolling average."
        )
        return
    }

    val allValues = buildList {
        dailyPoints.forEach { add(it.second) }
        rollingAvgPoints.forEach { add(it.second) }
        if (goalValue != null) add(goalValue)
    }
    val minVal = (allValues.minOrNull() ?: 0.0) - 0.8
    val maxVal = (allValues.maxOrNull() ?: 1.0) + 0.8
    val range = max(1.0, maxVal - minVal)

    val dotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val lineColor = FitTrackThemeExt.colors.success
    val goalColor = FitTrackThemeExt.colors.warning
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val w = size.width
            val h = size.height
            val padX = 12.dp.toPx()
            val padY = 14.dp.toPx()
            val usableW = max(1f, w - padX * 2)
            val usableH = max(1f, h - padY * 2)

            // Horizontal guide lines
            for (i in 0..3) {
                val y = padY + (usableH * i / 3f)
                drawLine(gridColor, Offset(padX, y), Offset(w - padX, y), strokeWidth = 1.dp.toPx())
            }

            // Goal dashed reference line
            if (goalValue != null && goalValue in minVal..maxVal) {
                val normY = ((maxVal - goalValue) / range).toFloat()
                val gy = padY + normY * usableH
                drawLine(
                    color = goalColor.copy(alpha = 0.7f),
                    start = Offset(padX, gy),
                    end = Offset(w - padX, gy),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }

            val count = dailyPoints.size
            fun xFor(idx: Int): Float = if (count <= 1) padX + usableW / 2f else padX + (usableW * idx / (count - 1).toFloat())
            fun yFor(v: Double): Float = padY + (((maxVal - v) / range).toFloat().coerceIn(0f, 1f) * usableH)

            // Daily scale dots
            dailyPoints.forEachIndexed { idx, (_, v) ->
                drawCircle(
                    color = dotColor,
                    radius = 4.dp.toPx(),
                    center = Offset(xFor(idx), yFor(v))
                )
            }

            // 7-day rolling average smooth path
            if (rollingAvgPoints.size >= 2) {
                val path = Path()
                rollingAvgPoints.forEachIndexed { idx, (_, v) ->
                    val pt = Offset(xFor(idx), yFor(v))
                    if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            } else if (rollingAvgPoints.size == 1) {
                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = Offset(xFor(0), yFor(rollingAvgPoints[0].second))
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min: ${String.format("%.1f", minVal + 0.8)} $unitLabel",
                style = MaterialTheme.typography.labelSmall,
                color = FitTrackThemeExt.colors.mutedText
            )
            Text(
                text = "Green: 7-day avg • Blue dots: daily",
                style = MaterialTheme.typography.labelSmall,
                color = FitTrackThemeExt.colors.mutedText
            )
            Text(
                text = "Max: ${String.format("%.1f", maxVal - 0.8)} $unitLabel",
                style = MaterialTheme.typography.labelSmall,
                color = FitTrackThemeExt.colors.mutedText
            )
        }
    }
}

@Composable
fun MetricBarChart(
    bars: List<Pair<String, Float>>,
    targetValue: Float?,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty()) return
    val maxVal = max(1f, max(bars.maxOf { it.second }, targetValue ?: 0f) * 1.15f)
    val targetLineColor = FitTrackThemeExt.colors.warning
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val w = size.width
            val h = size.height
            val slotW = w / bars.size.toFloat()
            val barW = min(28.dp.toPx(), slotW * 0.55f)

            bars.forEachIndexed { idx, (_, value) ->
                val left = idx * slotW + (slotW - barW) / 2f
                // Background track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, 6.dp.toPx()),
                    size = Size(barW, h - 12.dp.toPx()),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                val barH = ((value / maxVal).coerceIn(0f, 1f)) * (h - 12.dp.toPx())
                if (barH > 2f) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, h - 6.dp.toPx() - barH),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            if (targetValue != null && targetValue > 0f) {
                val ty = h - 6.dp.toPx() - ((targetValue / maxVal).coerceIn(0f, 1f) * (h - 12.dp.toPx()))
                drawLine(
                    color = targetLineColor,
                    start = Offset(0f, ty),
                    end = Offset(w, ty),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            bars.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = FitTrackThemeExt.colors.mutedText
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    FitTrackCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = FitTrackThemeExt.colors.mutedText
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun EstimationDisclaimerBanner(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "Estimation notice",
                tint = FitTrackThemeExt.colors.mutedText,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = FitTrackThemeExt.colors.mutedText
            )
        }
    }
}
