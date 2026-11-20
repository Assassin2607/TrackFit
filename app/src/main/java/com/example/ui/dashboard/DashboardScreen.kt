package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.WorkoutTemplateEntity
import com.example.data.remote.AppUpdateStatus
import com.example.domain.calc.FitnessCalculations
import com.example.domain.model.GoalType
import com.example.ui.components.DailyScoreRing
import com.example.ui.components.EstimationDisclaimerBanner
import com.example.ui.components.FitTrackCard
import com.example.ui.components.MacroProgressBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.FitTrackThemeExt
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.viewmodel.FitTrackUiState
import kotlin.math.abs

@Composable
fun DashboardScreen(
    state: FitTrackUiState,
    appUpdateStatus: AppUpdateStatus = AppUpdateStatus.Idle,
    updateBannerDismissed: Boolean = false,
    onDismissUpdateBanner: () -> Unit = {},
    onSyncClick: () -> Unit,
    onLogWeight: (Double, String) -> Unit,
    onStartWorkout: (WorkoutTemplateEntity) -> Unit,
    onOpenCardioLog: () -> Unit,
    onNavigateToNutrition: () -> Unit,
    onNavigateToWorkout: () -> Unit,
    onNavigateToProgress: () -> Unit
) {
    val context = LocalContext.current
    val profile = state.profile
    val unit = state.preferences.unitSystem
    val goalType = GoalType.fromNameSafe(profile?.goalType)

    var showScoreBreakdown by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }

    val nextTemplate = state.templates.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Sync Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hi, ${profile?.name ?: "Athlete"}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${goalType.label} Mode • ${state.selectedDateKey}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FitTrackThemeExt.colors.mutedText
                    )
                }
                SyncStatusBadge(
                    syncState = state.syncState,
                    onClick = onSyncClick
                )
            }
        }

        // GitHub Release Update Banner (shown only when a newer release exists)
        if (appUpdateStatus is AppUpdateStatus.UpdateAvailable && !updateBannerDismissed) {
            item {
                val info = appUpdateStatus.info
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_update_banner")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = "App update available",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Update Available (${info.tagName})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "A new FitTrack APK is ready on GitHub Releases.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.preferredDownloadUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("download_update_button")
                            ) {
                                Text("Download APK")
                            }
                            TextButton(onClick = onDismissUpdateBanner) {
                                Text("Later")
                            }
                        }
                    }
                }
            }
        }

        // Hero: Today's Deterministic Score (Tappable Breakdown)
        item {
            FitTrackCard(
                onClick = { showScoreBreakdown = true },
                modifier = Modifier.testTag("daily_score_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DailyScoreRing(score = state.dailyScore.totalScore, size = 108.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Today's Score",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.adaptiveRecommendation.onTrack) {
                                "Actions today are aligned with your ${goalType.label.lowercase()} plan."
                            } else {
                                "Tap to inspect your 5-pillar score breakdown."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = FitTrackThemeExt.colors.mutedText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MiniScoreChip("Food", "${state.dailyScore.nutritionPoints}/25")
                            MiniScoreChip("Pro", "${state.dailyScore.proteinPoints}/20")
                            MiniScoreChip("Lift", "${state.dailyScore.workoutPoints}/25")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap card for full breakdown →",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Weight & Rolling 7-Day Trend Card
        item {
            val latestKg = state.weightTrend.latestWeightKg ?: profile?.currentWeightKg ?: 75.0
            val goalKg = profile?.goalWeightKg ?: 70.0
            val rolling7Kg = state.weightTrend.rolling7DayAvgKg
            val trendRate = state.weightTrend.trend14DayKgPerWeek ?: state.weightTrend.trend30DayKgPerWeek

            FitTrackCard(modifier = Modifier.testTag("weight_trend_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.MonitorWeight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Weight & Trend", style = MaterialTheme.typography.titleMedium)
                    }
                    FilledTonalButton(
                        onClick = { showWeightDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("quick_weigh_in_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Weigh In", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("7-Day Rolling Avg", style = MaterialTheme.typography.labelSmall, color = FitTrackThemeExt.colors.mutedText)
                        Text(
                            text = FitnessCalculations.formatWeight(rolling7Kg ?: latestKg, unit),
                            style = MonoMetricMedium,
                            color = FitTrackThemeExt.colors.success
                        )
                        Text(
                            text = "Today: ${FitnessCalculations.formatWeight(latestKg, unit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FitTrackThemeExt.colors.mutedText
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Goal Weight", style = MaterialTheme.typography.labelSmall, color = FitTrackThemeExt.colors.mutedText)
                        Text(
                            text = FitnessCalculations.formatWeight(goalKg, unit),
                            style = MonoMetricMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Trend: ${trendRate?.let { FitnessCalculations.formatSignedWeightRate(it, unit) } ?: "Collecting data"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Goal Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = FitTrackThemeExt.colors.mutedText
                    )
                    Text(
                        text = "${state.goalProgressPercent}%",
                        style = MonoMetricSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { state.goalProgressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = FitTrackThemeExt.colors.success,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )

                val projWeeks = state.weightTrend.projectedWeeksToGoal
                if (projWeeks != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Estimated projection at current trend: ~$projWeeks weeks to ${FitnessCalculations.formatWeight(goalKg, unit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FitTrackThemeExt.colors.mutedText
                    )
                }
            }
        }

        // Calories & Protein Cockpit
        item {
            FitTrackCard(onClick = onNavigateToNutrition, modifier = Modifier.testTag("dashboard_nutrition_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = FitTrackThemeExt.colors.warning)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nutrition Today", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Log Food →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                MacroProgressBar(
                    label = "Calories",
                    consumed = state.todayCalories,
                    target = profile?.calorieTarget ?: 2100,
                    unit = "kcal",
                    accentColor = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                MacroProgressBar(
                    label = "Protein",
                    consumed = state.todayProteinG,
                    target = profile?.proteinTargetG ?: 160,
                    unit = "g",
                    accentColor = FitTrackThemeExt.colors.success
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Carbs: ${state.todayCarbsG}/${state.targetCarbsG}g", style = MaterialTheme.typography.bodySmall, color = FitTrackThemeExt.colors.mutedText)
                    Text("Fat: ${state.todayFatG}/${state.targetFatG}g", style = MaterialTheme.typography.bodySmall, color = FitTrackThemeExt.colors.mutedText)
                    Text("Fiber: ${state.todayFiberG}/${state.targetFiberG}g", style = MaterialTheme.typography.bodySmall, color = FitTrackThemeExt.colors.mutedText)
                }
            }
        }

        // Workout & Cardio Quick Action Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FitTrackCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_workout_card"),
                    onClick = {
                        if (nextTemplate != null && !state.todayWorkoutCompleted) {
                            onStartWorkout(nextTemplate)
                        } else {
                            onNavigateToWorkout()
                        }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (state.todayWorkoutCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            tint = if (state.todayWorkoutCompleted) FitTrackThemeExt.colors.success else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Workout", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.todayWorkoutCompleted) {
                            state.todayWorkoutName ?: "Completed"
                        } else {
                            nextTemplate?.name ?: "Open Templates"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (state.todayWorkoutCompleted) {
                            "Completed • ${FitnessCalculations.formatVolume(state.dailyVolumeKg, unit)}"
                        } else {
                            "Tap to start session"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.todayWorkoutCompleted) FitTrackThemeExt.colors.success else MaterialTheme.colorScheme.primary
                    )
                }

                FitTrackCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_cardio_card"),
                    onClick = onOpenCardioLog
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsRun,
                            contentDescription = null,
                            tint = FitTrackThemeExt.colors.cardioAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cardio", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val targetMin = profile?.cardioTargetMinPerDay ?: 20
                    Text(
                        text = "${state.todayCardioMinutes} / $targetMin min",
                        style = MonoMetricMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+ Log Cardio",
                        style = MaterialTheme.typography.bodySmall,
                        color = FitTrackThemeExt.colors.cardioAccent
                    )
                }
            }
        }

        // Transparent Estimated Energy Balance
        item {
            val eb = state.energyBalance
            val sign = if (eb.estimatedBalanceKcal > 0) "+" else ""
            val eqKg = eb.estimatedWeightChangeKg
            val eqFormatted = String.format("%+.2f %s", FitnessCalculations.kgToDisplay(eqKg, unit), unit.weightUnit)

            FitTrackCard(modifier = Modifier.testTag("energy_balance_card")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = FitTrackThemeExt.colors.warning
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Estimated Energy Balance", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(10.dp))
                EnergyRow("Estimated Maintenance (TDEE)", "${eb.estimatedMaintenanceKcal} kcal")
                EnergyRow("Food Intake Logged", "${eb.foodIntakeKcal} kcal")
                EnergyRow("Exercise Expenditure (Est.)", "${eb.workoutBurnKcal + eb.cardioBurnKcal} kcal")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (eb.estimatedBalanceKcal <= 0) "Estimated Deficit" else "Estimated Surplus",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "$sign${eb.estimatedBalanceKcal} kcal",
                        style = MonoMetricMedium,
                        color = if (eb.estimatedBalanceKcal <= 0) FitTrackThemeExt.colors.success else FitTrackThemeExt.colors.warning
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Estimated energy-equivalent change: ~$eqFormatted (using 7,700 kcal/kg)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Actual daily scale weight varies significantly from water, glycogen, sodium, and food mass—always rely on your 7-day rolling average.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FitTrackThemeExt.colors.mutedText
                )
            }
        }

        // Weekly Adaptive Goal Recommendation
        item {
            val rec = state.adaptiveRecommendation
            val accent = if (rec.onTrack) FitTrackThemeExt.colors.success else FitTrackThemeExt.colors.warning
            FitTrackCard(
                onClick = onNavigateToProgress,
                borderColor = accent.copy(alpha = 0.55f),
                modifier = Modifier.testTag("adaptive_recommendation_card")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (goalType == GoalType.CUT) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = accent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(rec.title, style = MaterialTheme.typography.titleMedium, color = accent)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(rec.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(rec.suggestion, style = MaterialTheme.typography.bodySmall, color = FitTrackThemeExt.colors.mutedText)
            }
        }

        // Quick Start Template CTA
        if (nextTemplate != null && !state.todayWorkoutCompleted) {
            item {
                Button(
                    onClick = { onStartWorkout(nextTemplate) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_primary_workout_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start ${nextTemplate.name}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        item {
            EstimationDisclaimerBanner(
                text = "All calorie expenditures, BMR/TDEE targets, and energy-equivalent estimates are mathematical approximations for personal fitness tracking, not medical advice."
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Score Breakdown Modal
    if (showScoreBreakdown) {
        AlertDialog(
            onDismissRequest = { showScoreBreakdown = false },
            title = {
                Text("Daily Score Breakdown (${state.dailyScore.totalScore}/100)")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Deterministic formula across 5 daily pillars:",
                        style = MaterialTheme.typography.bodySmall,
                        color = FitTrackThemeExt.colors.mutedText
                    )
                    state.dailyScore.components.forEach { comp ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(comp.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${comp.earned} / ${comp.maxPoints}",
                                    style = MonoMetricSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = comp.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = FitTrackThemeExt.colors.mutedText
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScoreBreakdown = false }) {
                    Text("Got it")
                }
            }
        )
    }

    // Quick Weight Entry Modal
    if (showWeightDialog) {
        val currentDisplay = FitnessCalculations.kgToDisplay(
            state.weightTrend.latestWeightKg ?: profile?.currentWeightKg ?: 75.0,
            unit
        )
        var weightInput by remember { mutableStateOf(String.format("%.1f", currentDisplay)) }
        var noteInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("Log Body Weight (${unit.weightUnit})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                        label = { Text("Weight (${unit.weightUnit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it.take(120) },
                        label = { Text("Optional Note (e.g., Morning fasted)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = weightInput.toDoubleOrNull()
                        if (parsed != null && parsed > 0) {
                            val kg = FitnessCalculations.displayToKg(parsed, unit)
                            onLogWeight(kg, noteInput)
                            showWeightDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showWeightDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MiniScoreChip(label: String, score: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = "$label ",
                style = MaterialTheme.typography.labelSmall,
                color = FitTrackThemeExt.colors.mutedText
            )
            Text(
                text = score,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EnergyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = FitTrackThemeExt.colors.mutedText)
        Text(value, style = MonoMetricSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
