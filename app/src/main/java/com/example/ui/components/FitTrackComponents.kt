package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.SyncState
import com.example.ui.theme.FitTrackThemeExt
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricSmall
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val CardShape20 = RoundedCornerShape(20.dp)

/**
 * Layered 3D depth card with recessed base plate shadow, diagonal sheen gradient,
 * top-edge specular bevel highlight, and subtle tactile press depth (120Hz-optimized).
 */
@Composable
fun FitTrackCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
    accentGradientEnd: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    depthElevation: Dp = 5.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = FitTrackThemeExt.colors
    val outlineColor = MaterialTheme.colorScheme.outline

    val topSurface = if (containerColor != Color.Unspecified) containerColor else ext.cardSurfaceTop
    val bottomSurface = when {
        accentGradientEnd != Color.Unspecified -> accentGradientEnd
        containerColor != Color.Unspecified -> lerp(containerColor, ext.cardBottomShadow, if (ext.isLightTheme) 0.08f else 0.28f)
        else -> ext.cardSurfaceBottom
    }
    val topBevel = if (borderColor != Color.Unspecified) {
        borderColor
    } else {
        ext.cardTopHighlight.copy(alpha = if (ext.isLightTheme) 0.95f else 0.52f)
    }
    val bottomBevel = if (borderColor != Color.Unspecified) {
        borderColor.copy(alpha = 0.45f)
    } else {
        ext.cardBottomShadow.copy(alpha = if (ext.isLightTheme) 0.55f else 0.85f)
    }
    val shadowPlateColor = ext.cardBottomShadow.copy(alpha = if (ext.isLightTheme) 0.45f else 0.78f)
    val ambientGlowColor = ext.cardBottomShadow.copy(alpha = if (ext.isLightTheme) 0.22f else 0.42f)

    val surfaceBrush = remember(topSurface, bottomSurface) {
        Brush.linearGradient(
            colors = listOf(topSurface, bottomSurface),
            start = Offset(0f, 0f),
            end = Offset(900f, 900f)
        )
    }
    val borderBrush = remember(topBevel, outlineColor, bottomBevel) {
        Brush.verticalGradient(
            colors = listOf(topBevel, outlineColor.copy(alpha = 0.45f), bottomBevel)
        )
    }

    val interactiveModifier = if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.985f else 1f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 650f),
            label = "card3dPressScale"
        )
        val pressOffsetPx by animateFloatAsState(
            targetValue = if (isPressed) 2f else 0f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 650f),
            label = "card3dPressOffset"
        )
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = pressOffsetPx
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = depthElevation)
            .drawBehind {
                val radiusPx = 20.dp.toPx()
                val lipPx = depthElevation.toPx()
                // Ambient soft base shadow
                drawRoundRect(
                    color = ambientGlowColor,
                    topLeft = Offset(2.dp.toPx(), lipPx + 2.dp.toPx()),
                    size = Size(size.width - 4.dp.toPx(), size.height),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
                // Crisp 3D extruded bottom slab lip
                drawRoundRect(
                    color = shadowPlateColor,
                    topLeft = Offset(0f, lipPx),
                    size = size,
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
            }
            .clip(CardShape20)
            .background(brush = surfaceBrush)
            .border(
                width = 1.15.dp,
                brush = borderBrush,
                shape = CardShape20
            )
            .then(interactiveModifier)
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
        border = BorderStroke(1.dp, fg.copy(alpha = 0.5f))
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

/**
 * 3D sculpted Daily Score Ring with recessed inner well, drop-shadow depth arc,
 * metallic gradient sweep, and specular highlight rim.
 */
@Composable
fun DailyScoreRing(
    score: Int,
    size: Dp = 118.dp,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f),
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 190f),
        label = "scoreRingProgress"
    )
    val ext = FitTrackThemeExt.colors
    val ringStart = when {
        score >= 80 -> ext.success
        score >= 55 -> MaterialTheme.colorScheme.primary
        score >= 30 -> ext.warning
        else -> MaterialTheme.colorScheme.error
    }
    val ringEnd = lerp(ringStart, Color.White, 0.34f)
    val trackColor = ext.cardBottomShadow.copy(alpha = if (ext.isLightTheme) 0.28f else 0.72f)
    val trackRimColor = ext.cardTopHighlight.copy(alpha = 0.28f)
    val wellCenterColor = ext.cardSurfaceTop
    val wellEdgeColor = ext.cardBottomShadow.copy(alpha = if (ext.isLightTheme) 0.18f else 0.55f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 12.dp.toPx()
            val inset = strokePx / 2f + 2.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2f, this.size.height - inset * 2f)
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val wellRadius = (min(this.size.width, this.size.height) / 2f) - strokePx - 1.dp.toPx()

            // Sculpted recessed inner center plate
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(wellCenterColor, wellEdgeColor),
                    center = Offset(center.x - wellRadius * 0.25f, center.y - wellRadius * 0.25f),
                    radius = wellRadius * 1.35f
                ),
                radius = wellRadius,
                center = center
            )

            // Recessed track channel
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            // Subtle outer bevel highlight on track
            drawArc(
                color = trackRimColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = 1.2.dp.toPx())
            )

            val sweep = 360f * progress
            if (sweep > 1f) {
                // 3D drop shadow under active arc
                drawArc(
                    color = ext.cardBottomShadow.copy(alpha = 0.65f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset + 3.dp.toPx()),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                // Main sculpted gradient arc
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(ringEnd, ringStart),
                        start = Offset(0f, 0f),
                        end = Offset(this.size.width, this.size.height)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                // Specular highlight ribbon along arc ridge
                drawArc(
                    color = Color.White.copy(alpha = 0.42f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset + 2.dp.toPx(), inset + 1.dp.toPx()),
                    size = Size(arcSize.width - 4.dp.toPx(), arcSize.height - 4.dp.toPx()),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
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
                color = ext.mutedText
            )
        }
    }
}

/**
 * 3D sculpted Hydration Ring used on Home (Nutrition Today) and Nutrition (Water Log).
 */
@Composable
fun Hydration3DRing(
    consumedMl: Int,
    targetMl: Int,
    compactLabel: String,
    size: Dp = 88.dp,
    modifier: Modifier = Modifier
) {
    val rawRatio = if (targetMl > 0) consumedMl.toFloat() / targetMl.toFloat() else 0f
    val progress by animateFloatAsState(
        targetValue = rawRatio.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 200f),
        label = "hydration3dRing"
    )
    val percent = (rawRatio * 100f).roundToInt().coerceIn(0, 999)
    val ext = FitTrackThemeExt.colors
    val waterStart = ext.hydrationAccent
    val waterEnd = lerp(waterStart, Color.White, 0.35f)
    val trackColor = ext.cardBottomShadow.copy(alpha = if (ext.isLightTheme) 0.25f else 0.68f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = (size.toPx() * 0.115f).coerceIn(7.dp.toPx(), 12.dp.toPx())
            val inset = strokePx / 2f + 1.5.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2f, this.size.height - inset * 2f)
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val innerR = (min(this.size.width, this.size.height) / 2f) - strokePx - 1.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ext.cardSurfaceTop, waterStart.copy(alpha = if (ext.isLightTheme) 0.12f else 0.16f)),
                    center = Offset(center.x - innerR * 0.2f, center.y - innerR * 0.2f),
                    radius = innerR * 1.4f
                ),
                radius = innerR,
                center = center
            )

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            val sweep = 360f * progress
            if (sweep > 1f) {
                drawArc(
                    color = ext.cardBottomShadow.copy(alpha = 0.55f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset + 2.5.dp.toPx()),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(waterEnd, waterStart),
                        start = Offset(0f, 0f),
                        end = Offset(this.size.width, this.size.height)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.45f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset + 1.5.dp.toPx(), inset + 1.dp.toPx()),
                    size = Size(arcSize.width - 3.dp.toPx(), arcSize.height - 3.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.WaterDrop,
                contentDescription = null,
                tint = waterStart,
                modifier = Modifier.size(if (size >= 96.dp) 18.dp else 14.dp)
            )
            Text(
                text = compactLabel,
                style = MonoMetricSmall.copy(fontSize = if (size >= 96.dp) 14.sp else 12.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = ext.mutedText
            )
        }
    }
}

/**
 * 3D beveled progress channel with animated metallic sheen fill.
 */
@Composable
fun MacroProgressBar(
    label: String,
    consumed: Int,
    target: Int,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val targetRatio = if (target > 0) (consumed.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val ratio by animateFloatAsState(
        targetValue = targetRatio,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 240f),
        label = "macroBar3d"
    )
    val remaining = target - consumed
    val remainingText = if (remaining >= 0) "$remaining$unit left" else "+${-remaining}$unit over"
    val ext = FitTrackThemeExt.colors

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
                    color = if (remaining < 0) ext.warning else ext.mutedText
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
        ) {
            val r = CornerRadius(size.height / 2f, size.height / 2f)
            // Recessed 3D track well
            drawRoundRect(
                color = ext.cardBottomShadow.copy(alpha = if (ext.isLightTheme) 0.25f else 0.72f),
                cornerRadius = r
            )
            val fillW = size.width * ratio
            if (fillW > 2f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(accentColor, lerp(accentColor, Color.White, 0.28f))
                    ),
                    size = Size(fillW, size.height),
                    cornerRadius = r
                )
                // Top specular bevel gleam on fill
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.36f),
                    topLeft = Offset(2.dp.toPx(), 1.2.dp.toPx()),
                    size = Size((fillW - 4.dp.toPx()).coerceAtLeast(0f), size.height * 0.34f),
                    cornerRadius = r
                )
            }
        }
    }
}

/**
 * Interactive 3D-styled Weight Trend Graph with smooth cubic Bezier curve,
 * gradient area fill, dashed Y-axis scale ticks, glowing nodes, and tap-to-inspect callout.
 */
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

    var selectedIdx by remember(dailyPoints.size) { mutableIntStateOf(dailyPoints.lastIndex) }
    val ext = FitTrackThemeExt.colors
    val dotColor = MaterialTheme.colorScheme.tertiary
    val lineColor = ext.success
    val goalColor = ext.warning
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)

    val safeIdx = selectedIdx.coerceIn(0, dailyPoints.lastIndex)
    val inspectedDaily = dailyPoints[safeIdx]
    val inspectedRolling = rollingAvgPoints.getOrNull(safeIdx)?.second

    Column(modifier = modifier.fillMaxWidth()) {
        // Interactive inspection pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, ext.cardTopHighlight.copy(alpha = 0.38f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = inspectedDaily.first,
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.mutedText
                    )
                    Text(
                        text = "Scale: ${String.format("%.1f", inspectedDaily.second)} $unitLabel",
                        style = MonoMetricSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (inspectedRolling != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "7-Day Trend",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.mutedText
                        )
                        Text(
                            text = "${String.format("%.1f", inspectedRolling)} $unitLabel",
                            style = MonoMetricSmall,
                            color = lineColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Y-Axis Scale Ticks
            Column(
                modifier = Modifier
                    .height(210.dp)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                for (i in 0..3) {
                    val tick = maxVal - (range * i / 3.0)
                    Text(
                        text = String.format("%.1f", tick),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = ext.mutedText
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(210.dp)
                    .pointerInput(dailyPoints.size) {
                        detectTapGestures { offset ->
                            val padX = 12.dp.toPx()
                            val usableW = max(1f, size.width - padX * 2f)
                            val count = dailyPoints.size
                            if (count <= 1) {
                                selectedIdx = 0
                            } else {
                                val normalized = ((offset.x - padX) / usableW).coerceIn(0f, 1f)
                                selectedIdx = (normalized * (count - 1)).roundToInt().coerceIn(0, count - 1)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val padX = 12.dp.toPx()
                val padY = 14.dp.toPx()
                val usableW = max(1f, w - padX * 2f)
                val usableH = max(1f, h - padY * 2f)

                val count = dailyPoints.size
                fun xFor(idx: Int): Float = if (count <= 1) padX + usableW / 2f else padX + (usableW * idx / (count - 1).toFloat())
                fun yFor(v: Double): Float = padY + (((maxVal - v) / range).toFloat().coerceIn(0f, 1f) * usableH)

                // Dashed horizontal gridlines
                for (i in 0..3) {
                    val y = padY + (usableH * i / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(padX, y),
                        end = Offset(w - padX, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
                    )
                }

                // Goal weight dashed line
                if (goalValue != null && goalValue in minVal..maxVal) {
                    val gy = yFor(goalValue)
                    drawLine(
                        color = goalColor.copy(alpha = 0.85f),
                        start = Offset(padX, gy),
                        end = Offset(w - padX, gy),
                        strokeWidth = 1.8.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                }

                // Selected vertical inspection crosshair
                val selX = xFor(safeIdx)
                drawLine(
                    color = ext.cardTopHighlight.copy(alpha = 0.55f),
                    start = Offset(selX, padY),
                    end = Offset(selX, h - padY),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )

                // Smooth Cubic Bezier Path + Area Gradient Fill for 7-day rolling average
                if (rollingAvgPoints.size >= 2) {
                    val linePath = Path()
                    val areaPath = Path()
                    val pts = rollingAvgPoints.mapIndexed { idx, (_, v) -> Offset(xFor(idx), yFor(v)) }

                    linePath.moveTo(pts.first().x, pts.first().y)
                    areaPath.moveTo(pts.first().x, h - padY)
                    areaPath.lineTo(pts.first().x, pts.first().y)

                    for (i in 0 until pts.lastIndex) {
                        val p0 = pts[i]
                        val p1 = pts[i + 1]
                        val ctrlX = (p0.x + p1.x) / 2f
                        linePath.cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                        areaPath.cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                    }
                    areaPath.lineTo(pts.last().x, h - padY)
                    areaPath.close()

                    // Soft gradient area fill beneath curve
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.34f), lineColor.copy(alpha = 0.02f)),
                            startY = padY,
                            endY = h - padY
                        )
                    )

                    // 3D drop shadow under curve
                    drawPath(
                        path = linePath,
                        color = ext.cardBottomShadow.copy(alpha = 0.55f),
                        style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Luminous rolling average Bezier curve
                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(lineColor, lerp(lineColor, Color.White, 0.25f))
                        ),
                        style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                } else if (rollingAvgPoints.size == 1) {
                    val center = Offset(xFor(0), yFor(rollingAvgPoints[0].second))
                    drawCircle(color = lineColor.copy(alpha = 0.28f), radius = 11.dp.toPx(), center = center)
                    drawCircle(color = lineColor, radius = 5.5.dp.toPx(), center = center)
                }

                // Glowing Daily Weigh-In Nodes
                dailyPoints.forEachIndexed { idx, (_, v) ->
                    val pt = Offset(xFor(idx), yFor(v))
                    val isSelected = idx == safeIdx
                    if (isSelected) {
                        drawCircle(color = dotColor.copy(alpha = 0.26f), radius = 11.dp.toPx(), center = pt)
                        drawCircle(color = dotColor, radius = 5.5.dp.toPx(), center = pt)
                        drawCircle(color = Color.White, radius = 2.4.dp.toPx(), center = pt)
                    } else {
                        drawCircle(color = dotColor.copy(alpha = 0.22f), radius = 6.dp.toPx(), center = pt)
                        drawCircle(color = dotColor, radius = 3.4.dp.toPx(), center = pt)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tap graph to inspect • Green: 7-day trend",
                style = MaterialTheme.typography.labelSmall,
                color = ext.mutedText
            )
            if (goalValue != null) {
                Text(
                    text = "Goal: ${String.format("%.1f", goalValue)} $unitLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = goalColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Interactive 3D-beveled vertical bar chart with animated entry growth,
 * metallic top bevels, glowing target threshold line, and tap-to-inspect callout.
 */
@Composable
fun MetricBarChart(
    bars: List<Pair<String, Float>>,
    targetValue: Float?,
    barColor: Color,
    unitSuffix: String = "",
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty()) return

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(bars) { animationTriggered = true }
    val growth by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "metricBarGrowth"
    )

    var selectedIdx by remember(bars.size) { mutableIntStateOf(bars.lastIndex) }
    val safeIdx = selectedIdx.coerceIn(0, bars.lastIndex)

    val maxVal = max(1f, max(bars.maxOf { it.second }, targetValue ?: 0f) * 1.18f)
    val ext = FitTrackThemeExt.colors
    val targetLineColor = ext.warning
    val trackColor = ext.cardBottomShadow.copy(alpha = if (ext.isLightTheme) 0.20f else 0.62f)
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val barHighlight = lerp(barColor, Color.White, 0.34f)
    val barDeep = lerp(barColor, ext.cardBottomShadow, 0.28f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Selected bar readout badge
        val (selLabel, selVal) = bars[safeIdx]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Day $selLabel • Tap any bar",
                style = MaterialTheme.typography.labelSmall,
                color = ext.mutedText
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = barColor.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, barColor.copy(alpha = 0.55f))
            ) {
                val formatted = if (abs(selVal - selVal.roundToInt()) < 0.05f) {
                    "${selVal.roundToInt()}$unitSuffix"
                } else {
                    String.format("%.1f%s", selVal, unitSuffix)
                }
                Text(
                    text = formatted,
                    style = MonoMetricSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .pointerInput(bars.size) {
                    detectTapGestures { offset ->
                        val slotW = size.width / bars.size.toFloat()
                        selectedIdx = (offset.x / slotW).toInt().coerceIn(0, bars.lastIndex)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val slotW = w / bars.size.toFloat()
            val barW = min(30.dp.toPx(), slotW * 0.56f)
            val padY = 8.dp.toPx()
            val usableH = h - padY * 2f

            // Subtle horizontal reference grid
            for (i in 0..2) {
                val y = padY + (usableH * i / 2f)
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
                )
            }

            bars.forEachIndexed { idx, (_, value) ->
                val left = idx * slotW + (slotW - barW) / 2f
                val isSelected = idx == safeIdx

                // Recessed 3D channel
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, padY),
                    size = Size(barW, usableH),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )

                val barH = ((value / maxVal).coerceIn(0f, 1f)) * usableH * growth
                if (barH > 2f) {
                    val topY = h - padY - barH
                    // 3D drop shadow offset
                    drawRoundRect(
                        color = ext.cardBottomShadow.copy(alpha = 0.45f),
                        topLeft = Offset(left + 2.dp.toPx(), topY + 2.dp.toPx()),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                    // Main vertical sculpted gradient bar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = if (isSelected) listOf(barHighlight, barColor) else listOf(barColor, barDeep),
                            startY = topY,
                            endY = h - padY
                        ),
                        topLeft = Offset(left, topY),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                    // Left-edge 3D specular bevel highlight
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (isSelected) 0.42f else 0.24f),
                        topLeft = Offset(left + 2.dp.toPx(), topY + 2.dp.toPx()),
                        size = Size(3.dp.toPx(), (barH - 4.dp.toPx()).coerceAtLeast(0f)),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
            }

            // Glowing dashed target line
            if (targetValue != null && targetValue > 0f) {
                val ty = h - padY - ((targetValue / maxVal).coerceIn(0f, 1f) * usableH)
                drawLine(
                    color = targetLineColor.copy(alpha = 0.25f),
                    start = Offset(0f, ty),
                    end = Offset(w, ty),
                    strokeWidth = 5.dp.toPx()
                )
                drawLine(
                    color = targetLineColor,
                    start = Offset(0f, ty),
                    end = Offset(w, ty),
                    strokeWidth = 1.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            bars.forEachIndexed { idx, (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (idx == safeIdx) MaterialTheme.colorScheme.onSurface else ext.mutedText,
                    fontWeight = if (idx == safeIdx) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * Horizontal 3D beveled bar chart for 30-Day Muscle Group Volume analytics.
 */
@Composable
fun MuscleVolumeBarChart(
    items: List<Pair<String, Double>>,
    valueFormatter: (Double) -> String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val maxVol = max(1.0, items.maxOf { it.second })
    val ext = FitTrackThemeExt.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { (label, vol) ->
            val targetRatio = (vol / maxVol).toFloat().coerceIn(0.04f, 1f)
            val ratio by animateFloatAsState(
                targetValue = targetRatio,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 210f),
                label = "muscleBar_$label"
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = valueFormatter(vol),
                        style = MonoMetricSmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                ) {
                    val r = CornerRadius(size.height / 2f, size.height / 2f)
                    drawRoundRect(
                        color = ext.cardBottomShadow.copy(alpha = if (ext.isLightTheme) 0.22f else 0.65f),
                        cornerRadius = r
                    )
                    val barW = size.width * ratio
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(accentColor, lerp(accentColor, Color.White, 0.32f))
                        ),
                        size = Size(barW, size.height),
                        cornerRadius = r
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.32f),
                        topLeft = Offset(2.dp.toPx(), 1.2.dp.toPx()),
                        size = Size((barW - 4.dp.toPx()).coerceAtLeast(0f), size.height * 0.34f),
                        cornerRadius = r
                    )
                }
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
