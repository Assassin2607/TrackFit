package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * High-refresh-rate (120Hz) FitTrack cold-start opening animation overlay.
 * Renders over the live app shell so Room SQLite initializes instantaneously underneath
 * while the branded emblem, orbital arc, shockwave rings, and wordmark play smoothly.
 */
@Composable
fun FitTrackLaunchOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emblemScale = remember { Animatable(0.55f) }
    val emblemAlpha = remember { Animatable(0f) }
    val arcSweep = remember { Animatable(0f) }
    val shockwaveProgress = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(24f) }
    val accentBarProgress = remember { Animatable(0f) }
    val overlayAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Phase 1: Emblem spring reveal + 360° orbital ring + expanding aura pulse
        launch {
            emblemAlpha.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
        }
        launch {
            emblemScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            arcSweep.animateTo(360f, tween(680, easing = FastOutSlowInEasing))
        }
        launch {
            shockwaveProgress.animateTo(1f, tween(850, easing = LinearOutSlowInEasing))
        }

        // Phase 2: Kinetic typography & glowing accent beam
        delay(220L)
        launch {
            textAlpha.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        }
        launch {
            textOffsetY.animateTo(0f, tween(380, easing = FastOutSlowInEasing))
        }
        launch {
            delay(110L)
            accentBarProgress.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        }

        // Hold briefly so user appreciates the crisp lockup
        delay(640L)

        // Phase 3: Smooth scale-up + fade-out reveal into the active screen
        launch {
            exitScale.animateTo(1.10f, tween(300, easing = FastOutSlowInEasing))
        }
        overlayAlpha.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = overlayAlpha.value
                scaleX = exitScale.value
                scaleY = exitScale.value
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(HomeIndigoTop, Color(0xFF15193B), HomeBgDeep)
                )
            )
            .testTag("fittrack_launch_overlay"),
        contentAlignment = Alignment.Center
    ) {
        // Full-screen atmospheric radial glow & expanding shockwave rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.44f)
            val maxDim = size.minDimension

            // Soft indigo + terracotta ambient core aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        HomeTerracotta.copy(alpha = 0.24f * emblemAlpha.value),
                        Color(0xFF6366F1).copy(alpha = 0.14f * emblemAlpha.value),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxDim * 0.52f
                ),
                radius = maxDim * 0.52f,
                center = center
            )

            // Primary expanding orbital shockwave ring
            val wave = shockwaveProgress.value
            if (wave > 0f && wave < 1f) {
                val ringRadius = maxDim * (0.14f + 0.36f * wave)
                val ringAlpha = (1f - wave) * 0.55f
                drawCircle(
                    color = HomeTerracottaLight.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = (3.5f * (1f - wave * 0.6f)).dp.toPx())
                )
            }

            // Secondary delayed shockwave echo
            val wave2 = ((wave - 0.18f) / 0.82f).coerceIn(0f, 1f)
            if (wave2 > 0f && wave2 < 1f) {
                val ringRadius2 = maxDim * (0.14f + 0.30f * wave2)
                val ringAlpha2 = (1f - wave2) * 0.35f
                drawCircle(
                    color = Color(0xFF818CF8).copy(alpha = ringAlpha2),
                    radius = ringRadius2,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 3D Sculpted Shield Emblem + 360° Orbital Energy Sweep
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .graphicsLayer {
                        scaleX = emblemScale.value
                        scaleY = emblemScale.value
                        alpha = emblemAlpha.value
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val ringStroke = 5.dp.toPx()
                    val ringRadius = (size.minDimension - ringStroke) / 2f - 4.dp.toPx()
                    val topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
                    val arcSize = Size(ringRadius * 2f, ringRadius * 2f)

                    // Subtle orbital track
                    drawCircle(
                        color = Color.White.copy(alpha = 0.10f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = ringStroke)
                    )

                    // Sweeping luminous orbital energy arc
                    val sweep = arcSweep.value
                    if (sweep > 0.5f) {
                        drawArc(
                            color = HomeTerracotta.copy(alpha = 0.30f),
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = ringStroke * 2.1f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    HomeTerracotta,
                                    Color(0xFFF59E0B),
                                    Color(0xFF818CF8),
                                    HomeTerracottaLight
                                ),
                                center = center
                            ),
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = ringStroke, cap = StrokeCap.Round)
                        )
                    }

                    // Inner sculpted metallic shield crest
                    val w = size.width
                    val h = size.height
                    val shield = Path().apply {
                        moveTo(w * 0.50f, h * 0.17f)
                        lineTo(w * 0.78f, h * 0.28f)
                        lineTo(w * 0.73f, h * 0.65f)
                        lineTo(w * 0.50f, h * 0.84f)
                        lineTo(w * 0.27f, h * 0.65f)
                        lineTo(w * 0.22f, h * 0.28f)
                        close()
                    }
                    drawPath(
                        path = shield,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2B3270),
                                Color(0xFF171B42),
                                Color(0xFF0C0F26)
                            )
                        )
                    )
                    drawPath(
                        path = shield,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.72f),
                                HomeTerracottaLight.copy(alpha = 0.85f),
                                Color(0xFF6366F1).copy(alpha = 0.65f)
                            )
                        ),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.FitnessCenter,
                    contentDescription = "FitTrack emblem",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Brand Title + Animated Accent Beam + Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = textOffsetY.value
                }
            ) {
                Text(
                    text = "FITTRACK",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width((96f * accentBarProgress.value).dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    HomeTerracotta,
                                    Color(0xFFF59E0B),
                                    HomeTerracottaLight
                                )
                            )
                        )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "TRAIN  •  FUEL  •  EVOLVE",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFB5BCE0),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.4.sp
                )
            }
        }
    }
}
