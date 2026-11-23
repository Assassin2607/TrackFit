package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.calc.DailyScoreBreakdown
import com.example.domain.model.GoalType
import com.example.ui.theme.HomeBgDeep
import com.example.ui.theme.HomeIndigoTop
import com.example.ui.theme.HomeTerracotta
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricSmall
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class ScoreRankTier(
    val grade: String,
    val title: String,
    val primaryColor: Color,
    val glowColor: Color,
    val minScore: Int,
    val nextRankGrade: String?,
    val nextRankScore: Int?
)

fun resolveDailyScoreRank(score: Int): ScoreRankTier = when {
    score >= 95 -> ScoreRankTier(
        grade = "S+",
        title = "LEGENDARY",
        primaryColor = Color(0xFFFACC15),
        glowColor = Color(0xFFF59E0B),
        minScore = 95,
        nextRankGrade = null,
        nextRankScore = null
    )
    score >= 85 -> ScoreRankTier(
        grade = "S",
        title = "ELITE",
        primaryColor = Color(0xFFFB923C),
        glowColor = Color(0xFFEA580C),
        minScore = 85,
        nextRankGrade = "S+",
        nextRankScore = 95
    )
    score >= 75 -> ScoreRankTier(
        grade = "A",
        title = "VETERAN",
        primaryColor = Color(0xFF34D399),
        glowColor = Color(0xFF10B981),
        minScore = 75,
        nextRankGrade = "S",
        nextRankScore = 85
    )
    score >= 60 -> ScoreRankTier(
        grade = "B",
        title = "CONTENDER",
        primaryColor = Color(0xFF38BDF8),
        glowColor = Color(0xFF0284C7),
        minScore = 60,
        nextRankGrade = "A",
        nextRankScore = 75
    )
    score >= 40 -> ScoreRankTier(
        grade = "C",
        title = "CHALLENGER",
        primaryColor = Color(0xFFE07A5F),
        glowColor = HomeTerracotta,
        minScore = 40,
        nextRankGrade = "B",
        nextRankScore = 60
    )
    else -> ScoreRankTier(
        grade = "D",
        title = "WARM-UP",
        primaryColor = Color(0xFFA5B4FC),
        glowColor = Color(0xFF6366F1),
        minScore = 0,
        nextRankGrade = "C",
        nextRankScore = 40
    )
}

/**
 * Game-HUD inspired Daily Score hero card harmonized with Home's Deep Indigo (#252A5A)
 * and Terracotta (#C66A4A) palette. Features a geometric Rank Shield crest, smooth 3D
 * luminous orbital energy arc (replacing radial sticks), XP rank progress bar, streak
 * flame badge, 5 RPG quest stat bars, and score-delta animation whenever logs update.
 */
@Composable
fun GameScoreHeroCard(
    dailyScore: DailyScoreBreakdown,
    activeStreakDays: Int,
    goalType: GoalType,
    increaseFromScore: Int? = null,
    onIncreaseAnimationFinished: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetScore = dailyScore.totalScore.coerceIn(0, 100)
    val shouldAnimateIncrease = increaseFromScore != null && increaseFromScore < targetScore
    val initialScoreValue = if (shouldAnimateIncrease) {
        increaseFromScore!!.coerceIn(0, 100).toFloat()
    } else {
        targetScore.toFloat()
    }
    val animatedScoreState = remember { Animatable(initialScoreValue) }
    var scoreDeltaBadge by remember { mutableIntStateOf(0) }
    var showDeltaBadge by remember { mutableStateOf(false) }

    // Ensure leaving Home mid-animation still marks the increase as consumed so it never replays
    DisposableEffect(increaseFromScore) {
        onDispose {
            if (increaseFromScore != null) {
                onIncreaseAnimationFinished()
            }
        }
    }

    LaunchedEffect(showDeltaBadge) {
        if (showDeltaBadge) {
            delay(2200L)
            showDeltaBadge = false
        }
    }

    LaunchedEffect(increaseFromScore, targetScore) {
        if (increaseFromScore != null && increaseFromScore < targetScore) {
            val startVal = increaseFromScore.coerceIn(0, 100)
            val delta = targetScore - startVal
            scoreDeltaBadge = delta
            showDeltaBadge = true
            animatedScoreState.snapTo(startVal.toFloat())
            animatedScoreState.animateTo(
                targetValue = targetScore.toFloat(),
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
            )
            onIncreaseAnimationFinished()
        } else {
            // Static render when score has not increased (or on normal tab switches)
            animatedScoreState.snapTo(targetScore.toFloat())
        }
    }

    val displayedScore = animatedScoreState.value.roundToInt().coerceIn(0, 100)
    val rank = resolveDailyScoreRank(displayedScore)
    val maxedPillars = dailyScore.components.count { it.earned >= it.maxPoints }

    val animatedXp = if (rank.nextRankScore != null) {
        val span = (rank.nextRankScore - rank.minScore).coerceAtLeast(1)
        ((animatedScoreState.value - rank.minScore) / span.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    val outerShape = RoundedCornerShape(22.dp)
    val surfaceTop = lerp(HomeIndigoTop, rank.glowColor, 0.13f)
    val surfaceMid = Color(0xFF1A1E43)
    val surfaceBottom = HomeBgDeep

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_score_card")
    ) {
        // 3D Bottom extruded slab shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 6.dp)
                .clip(outerShape)
                .background(Color(0xFF0B0D20))
        )

        // Main Game HUD Metallic Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp)
                .clip(outerShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(surfaceTop, surfaceMid, surfaceBottom)
                    )
                )
                .clickable(onClick = onClick)
        ) {
            // Custom Canvas HUD background: radial rank aura, bevel border & corner brackets
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            rank.primaryColor.copy(alpha = 0.20f),
                            HomeTerracotta.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.24f, size.height * 0.44f),
                        radius = size.width * 0.56f
                    ),
                    radius = size.width * 0.56f,
                    center = Offset(size.width * 0.24f, size.height * 0.44f)
                )

                val borderStroke = 1.5.dp.toPx()
                val cornerPx = 22.dp.toPx()
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            rank.primaryColor.copy(alpha = 0.78f),
                            HomeTerracotta.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.14f),
                            rank.glowColor.copy(alpha = 0.65f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = borderStroke)
                )

                val bracketLen = 14.dp.toPx()
                val inset = 9.dp.toPx()
                val bracketColor = rank.primaryColor.copy(alpha = 0.72f)
                val strokeW = 2.dp.toPx()

                drawLine(bracketColor, Offset(inset, inset + bracketLen), Offset(inset, inset), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(inset, inset), Offset(inset + bracketLen, inset), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(size.width - inset - bracketLen, inset), Offset(size.width - inset, inset), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(size.width - inset, inset), Offset(size.width - inset, inset + bracketLen), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(inset, size.height - inset - bracketLen), Offset(inset, size.height - inset), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(inset, size.height - inset), Offset(inset + bracketLen, size.height - inset), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(size.width - inset - bracketLen, size.height - inset), Offset(size.width - inset, size.height - inset), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(size.width - inset, size.height - inset - bracketLen), Offset(size.width - inset, size.height - inset), strokeW, StrokeCap.Round)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Top HUD Strip: Rank Shield Crest + Title + Glowing Streak Flame Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RankShieldCrest(rank = rank, size = 38.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TODAY'S SCORE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB5BCE0),
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.4.sp
                            )
                            Text(
                                text = "RANK ${rank.grade} • ${rank.title}",
                                style = MaterialTheme.typography.titleSmall,
                                color = rank.primaryColor,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }

                    // Streak Flame Pill
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = HomeTerracotta.copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, HomeTerracotta.copy(alpha = 0.85f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${activeStreakDays}D STREAK",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFF3E0),
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Center HUD: Smooth 3D Luminous Orbital Power Ring + XP Rank Progression Panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GameOrbitalPowerRing(
                        animatedScore = animatedScoreState.value,
                        displayedScore = displayedScore,
                        rank = rank,
                        deltaPoints = scoreDeltaBadge,
                        showDeltaBadge = showDeltaBadge,
                        size = 126.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // XP Progress Panel
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF10132C).copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, rank.primaryColor.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(11.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (rank.nextRankGrade != null) "NEXT TIER: ${rank.nextRankGrade}" else "MAX TIER",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFC5CBE8),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp
                                    )
                                    Text(
                                        text = if (rank.nextRankScore != null) {
                                            "+${(rank.nextRankScore - displayedScore).coerceAtLeast(0)} pts"
                                        } else {
                                            "MAXED"
                                        },
                                        style = MonoMetricSmall,
                                        color = rank.primaryColor,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(7.dp))
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                ) {
                                    val r = CornerRadius(size.height / 2f, size.height / 2f)
                                    drawRoundRect(
                                        color = Color(0xFF1E244D),
                                        cornerRadius = r
                                    )
                                    val fillW = size.width * animatedXp
                                    if (fillW > 2f) {
                                        drawRoundRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(rank.glowColor, rank.primaryColor, Color.White.copy(alpha = 0.9f))
                                            ),
                                            size = Size(fillW, size.height),
                                            cornerRadius = r
                                        )
                                    }
                                    val notches = 5
                                    for (i in 1 until notches) {
                                        val nx = size.width * (i.toFloat() / notches.toFloat())
                                        drawLine(
                                            color = Color(0xFF0E1126).copy(alpha = 0.65f),
                                            start = Offset(nx, 0f),
                                            end = Offset(nx, size.height),
                                            strokeWidth = 1.5.dp.toPx()
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$maxedPillars/5 quests maxed • ${goalType.label} mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF9EA7D1)
                                )
                            }
                        }

                        // Compact status callout
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (displayedScore >= 75) Icons.Rounded.Verified else Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = rank.primaryColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when {
                                    displayedScore >= 95 -> "Flawless discipline today!"
                                    displayedScore >= 85 -> "Elite momentum — push for S+!"
                                    displayedScore >= 75 -> "Strong ${goalType.label.lowercase()} alignment."
                                    displayedScore >= 40 -> "Complete quests below to rank up."
                                    else -> "Log meals or train to power up."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE3E7FA),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom 5-Pillar Game Quest Stat Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GameQuestStatSlot(
                        label = "Fuel",
                        earned = dailyScore.nutritionPoints,
                        maxPts = 25,
                        icon = Icons.Rounded.Restaurant,
                        accentColor = Color(0xFFE07A5F),
                        modifier = Modifier.weight(1f)
                    )
                    GameQuestStatSlot(
                        label = "Pro",
                        earned = dailyScore.proteinPoints,
                        maxPts = 20,
                        icon = Icons.Rounded.Bolt,
                        accentColor = Color(0xFF34D399),
                        modifier = Modifier.weight(1f)
                    )
                    GameQuestStatSlot(
                        label = "Train",
                        earned = dailyScore.workoutPoints,
                        maxPts = 25,
                        icon = Icons.Rounded.FitnessCenter,
                        accentColor = Color(0xFF60A5FA),
                        modifier = Modifier.weight(1f)
                    )
                    GameQuestStatSlot(
                        label = "Cardio",
                        earned = dailyScore.cardioPoints,
                        maxPts = 15,
                        icon = Icons.Rounded.DirectionsRun,
                        accentColor = Color(0xFFFBBF24),
                        modifier = Modifier.weight(1f)
                    )
                    GameQuestStatSlot(
                        label = "Habit",
                        earned = dailyScore.consistencyPoints,
                        maxPts = 15,
                        icon = Icons.Rounded.CheckCircle,
                        accentColor = Color(0xFFC084FC),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INSPECT QUEST BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall,
                        color = rank.primaryColor.copy(alpha = 0.92f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = rank.primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RankShieldCrest(rank: ScoreRankTier, size: Dp = 38.dp) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val shield = Path().apply {
                moveTo(w * 0.50f, h * 0.04f)
                lineTo(w * 0.92f, h * 0.22f)
                lineTo(w * 0.84f, h * 0.70f)
                lineTo(w * 0.50f, h * 0.96f)
                lineTo(w * 0.16f, h * 0.70f)
                lineTo(w * 0.08f, h * 0.22f)
                close()
            }
            drawPath(
                path = shield,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        rank.primaryColor.copy(alpha = 0.35f),
                        rank.glowColor.copy(alpha = 0.18f),
                        Color(0xFF0E1228)
                    )
                )
            )
            drawPath(
                path = shield,
                brush = Brush.linearGradient(
                    colors = listOf(rank.primaryColor, Color.White.copy(alpha = 0.8f), rank.glowColor)
                ),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        Text(
            text = rank.grade,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = if (rank.grade.length > 1) 12.sp else 14.sp
        )
    }
}

/**
 * Smooth 3D luminous orbital power ring (no radial sticks):
 * - Recessed 3D metallic dial core with inner bevel rim
 * - Continuous glowing energy arc with multi-stop gradient and outer bloom
 * - Luminous orbital plasma orb at the active arc endpoint
 * - Animated +PTS gain badge when logs increase today's score
 */
@Composable
private fun GameOrbitalPowerRing(
    animatedScore: Float,
    displayedScore: Int,
    rank: ScoreRankTier,
    deltaPoints: Int,
    showDeltaBadge: Boolean,
    size: Dp
) {
    val startAngleDeg = 135f
    val totalSweepDeg = 270f
    val progress = (animatedScore / 100f).coerceIn(0f, 1f)
    val sweepDeg = totalSweepDeg * progress

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val strokePx = 10.5.dp.toPx()
            val arcRadius = (this.size.minDimension - strokePx) / 2f - 4.dp.toPx()
            val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

            // Outer subtle halo frame ring
            drawCircle(
                color = rank.primaryColor.copy(alpha = 0.16f),
                radius = arcRadius + strokePx * 0.62f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Deep 3D recessed dial core
            val coreRadius = arcRadius - strokePx * 0.72f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF252D63), Color(0xFF141838), Color(0xFF0B0E24)),
                    center = Offset(center.x, center.y - coreRadius * 0.25f),
                    radius = coreRadius * 1.15f
                ),
                radius = coreRadius,
                center = center
            )
            // Inner metallic rim highlight
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.34f),
                        rank.primaryColor.copy(alpha = 0.22f),
                        Color.Black.copy(alpha = 0.45f)
                    ),
                    start = Offset(center.x - coreRadius, center.y - coreRadius),
                    end = Offset(center.x + coreRadius, center.y + coreRadius)
                ),
                radius = coreRadius,
                center = center,
                style = Stroke(width = 1.4.dp.toPx())
            )

            // Dark beveled track channel (270° open-bottom arc)
            drawArc(
                color = Color(0xFF0A0D20),
                startAngle = startAngleDeg,
                sweepAngle = totalSweepDeg,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokePx + 2.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = startAngleDeg,
                sweepAngle = totalSweepDeg,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokePx * 0.78f, cap = StrokeCap.Round)
            )

            if (sweepDeg > 0.5f) {
                // Soft outer neon bloom arc
                drawArc(
                    color = rank.primaryColor.copy(alpha = 0.26f),
                    startAngle = startAngleDeg,
                    sweepAngle = sweepDeg,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx * 1.85f, cap = StrokeCap.Round)
                )

                // Main luminous gradient energy arc
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            rank.glowColor,
                            rank.primaryColor,
                            lerp(rank.primaryColor, Color.White, 0.35f)
                        ),
                        start = Offset(arcTopLeft.x, arcTopLeft.y + arcSize.height),
                        end = Offset(arcTopLeft.x + arcSize.width, arcTopLeft.y)
                    ),
                    startAngle = startAngleDeg,
                    sweepAngle = sweepDeg,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Thin inner specular gloss ridge along the arc
                drawArc(
                    color = Color.White.copy(alpha = 0.42f),
                    startAngle = startAngleDeg,
                    sweepAngle = sweepDeg,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx * 0.26f, cap = StrokeCap.Round)
                )

                // Glowing orbital energy orb at the leading edge
                val endRad = ((startAngleDeg + sweepDeg) * PI / 180.0).toFloat()
                val tipOffset = Offset(
                    x = center.x + arcRadius * cos(endRad),
                    y = center.y + arcRadius * sin(endRad)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            rank.primaryColor.copy(alpha = 0.70f),
                            Color.Transparent
                        ),
                        center = tipOffset,
                        radius = strokePx * 1.05f
                    ),
                    radius = strokePx * 1.05f,
                    center = tipOffset
                )
                drawCircle(
                    color = Color.White,
                    radius = strokePx * 0.34f,
                    center = tipOffset
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$displayedScore",
                style = MonoMetricLarge,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 31.sp
            )
            Text(
                text = "/ 100 PTS",
                style = MaterialTheme.typography.labelSmall,
                color = rank.primaryColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        // Animated floating +PTS gain pill at bottom opening of the 270° gauge
        AnimatedVisibility(
            visible = showDeltaBadge && deltaPoints > 0,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.7f),
            exit = fadeOut(tween(350)) + scaleOut(targetScale = 0.85f),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFF10B981),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f))
            ) {
                Text(
                    text = "+$deltaPoints PTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun GameQuestStatSlot(
    label: String,
    earned: Int,
    maxPts: Int,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isMaxed = earned >= maxPts && maxPts > 0
    val ratio = if (maxPts > 0) (earned.toFloat() / maxPts.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "questSlotBar_$label"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isMaxed) accentColor.copy(alpha = 0.16f) else Color(0xFF10132C).copy(alpha = 0.72f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isMaxed) accentColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (earned > 0) accentColor else Color(0xFF7780AA),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFC5CBE8),
                fontSize = 10.sp,
                maxLines = 1
            )
            Text(
                text = "$earned/$maxPts",
                style = MonoMetricSmall,
                color = if (isMaxed) accentColor else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedRatio)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor)
                )
            }
        }
    }
}
