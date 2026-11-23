package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HomeBgDeep
import com.example.ui.theme.HomeIndigoTop
import com.example.ui.theme.HomeTerracotta
import com.example.ui.theme.HomeTerracottaLight
import com.example.ui.theme.WorkoutCreamText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SilkyEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * Zero-recomposition, zero-allocation 120Hz FitTrack cold-start launch overlay.
 * - Waits 2 frames via withFrameNanos so Android window inflation finishes before motion starts.
 * - Pre-caches all Path, Brush, and Stroke instances via drawWithCache (0 allocations per frame).
 * - Animates exclusively in GPU graphicsLayer & draw phases (0 layout passes per frame).
 * - Signals [onReadyForContent] once the emblem settles so the app shell composes behind the opaque
 *   overlay without causing frame drops during the opening motion.
 */
@Composable
fun FitTrackLaunchOverlay(
    onReadyForContent: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val introProgress = remember { Animatable(0f) }
    val ringSweep = remember { Animatable(0f) }
    val exitProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Wait 2 vsync frames so cold-start surface setup & font loading complete cleanly before frame 1
        withFrameNanos { }
        withFrameNanos { }

        // Phase 1: Silky GPU intro (emblem + ring sweep + wordmark lockup)
        val sweepJob = launch {
            ringSweep.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 560, easing = SilkyEaseOut)
            )
        }
        introProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 480, easing = SilkyEaseOut)
        )
        sweepJob.join()

        // Let the main app shell compose invisibly behind this opaque overlay while emblem holds
        onReadyForContent()
        delay(280L)

        // Phase 2: Pure GPU fade + subtle scale reveal into the ready app
        exitProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        )
        onFinished()
    }

    val bgBrush = remember {
        Brush.verticalGradient(
            colors = listOf(HomeIndigoTop, Color(0xFF15193B), HomeBgDeep)
        )
    }
    val accentBarBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(HomeTerracotta, Color(0xFFF3DEC0), HomeTerracottaLight)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val e = exitProgress.value
                alpha = 1f - e
                val s = 1f + 0.06f * e
                scaleX = s
                scaleY = s
            }
            .background(bgBrush)
            .drawWithCache {
                val center = Offset(size.width / 2f, size.height * 0.44f)
                val maxDim = size.minDimension
                val auraRadius = maxDim * 0.50f
                val auraBrush = Brush.radialGradient(
                    colors = listOf(
                        HomeTerracotta.copy(alpha = 0.22f),
                        Color(0xFF6366F1).copy(alpha = 0.13f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = auraRadius
                )
                val waveStroke = Stroke(width = 2.dp.toPx())
                onDrawBehind {
                    val p = introProgress.value
                    if (p > 0.01f) {
                        drawCircle(
                            brush = auraBrush,
                            radius = auraRadius,
                            center = center,
                            alpha = p
                        )
                        if (p < 0.99f) {
                            val ringRadius = maxDim * (0.14f + 0.28f * p)
                            drawCircle(
                                color = HomeTerracottaLight.copy(alpha = (1f - p) * 0.42f),
                                radius = ringRadius,
                                center = center,
                                style = waveStroke
                            )
                        }
                    }
                }
            }
            .testTag("fittrack_launch_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 3D Sculpted Shield Emblem + 360° Orbital Ring (all brushes & path cached once!)
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .graphicsLayer {
                        val p = introProgress.value
                        val s = 0.72f + 0.28f * p
                        scaleX = s
                        scaleY = s
                        alpha = (p * 1.6f).coerceIn(0f, 1f)
                    }
                    .drawWithCache {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val ringWidthPx = 4.8.dp.toPx()
                        val ringRadius = (size.minDimension - ringWidthPx) / 2f - 4.dp.toPx()
                        val topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
                        val arcSize = Size(ringRadius * 2f, ringRadius * 2f)

                        val trackStroke = Stroke(width = ringWidthPx)
                        val glowStroke = Stroke(width = ringWidthPx * 1.9f, cap = StrokeCap.Round)
                        val arcStroke = Stroke(width = ringWidthPx, cap = StrokeCap.Round)
                        val shieldStroke = Stroke(width = 2.dp.toPx())

                        val sweepBrush = Brush.sweepGradient(
                            colors = listOf(
                                HomeTerracotta,
                                Color(0xFFF3DEC0),
                                Color(0xFF818CF8),
                                HomeTerracottaLight
                            ),
                            center = center
                        )
                        val w = size.width
                        val h = size.height
                        val shieldPath = Path().apply {
                            moveTo(w * 0.50f, h * 0.17f)
                            lineTo(w * 0.78f, h * 0.28f)
                            lineTo(w * 0.73f, h * 0.65f)
                            lineTo(w * 0.50f, h * 0.84f)
                            lineTo(w * 0.27f, h * 0.65f)
                            lineTo(w * 0.22f, h * 0.28f)
                            close()
                        }
                        val shieldFill = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2B3270),
                                Color(0xFF171B42),
                                Color(0xFF0C0F26)
                            )
                        )
                        val shieldBorder = Brush.linearGradient(
                            colors = listOf(
                                WorkoutCreamText.copy(alpha = 0.80f),
                                HomeTerracottaLight.copy(alpha = 0.85f),
                                Color(0xFF6366F1).copy(alpha = 0.65f)
                            )
                        )

                        onDrawBehind {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.10f),
                                radius = ringRadius,
                                center = center,
                                style = trackStroke
                            )
                            val sweep = ringSweep.value
                            if (sweep > 0.5f) {
                                drawArc(
                                    color = HomeTerracotta.copy(alpha = 0.26f),
                                    startAngle = -90f,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = glowStroke
                                )
                                drawArc(
                                    brush = sweepBrush,
                                    startAngle = -90f,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = arcStroke
                                )
                            }
                            drawPath(path = shieldPath, brush = shieldFill)
                            drawPath(path = shieldPath, brush = shieldBorder, style = shieldStroke)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FitnessCenter,
                    contentDescription = "FitTrack emblem",
                    tint = WorkoutCreamText,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Brand Title + GPU-Scaled Accent Beam + Tagline (0 layout passes per frame!)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    val textP = ((introProgress.value - 0.22f) / 0.78f).coerceIn(0f, 1f)
                    alpha = textP
                    translationY = (1f - textP) * 18.dp.toPx()
                }
            ) {
                Text(
                    text = "FITTRACK",
                    style = MaterialTheme.typography.headlineLarge,
                    color = WorkoutCreamText,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(3.dp)
                        .graphicsLayer {
                            val barP = ((introProgress.value - 0.35f) / 0.65f).coerceIn(0f, 1f)
                            scaleX = barP
                            transformOrigin = TransformOrigin.Center
                        }
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentBarBrush)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "TRAIN  •  FUEL  •  EVOLVE",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE2D3BE),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.4.sp
                )
            }
        }
    }
}
