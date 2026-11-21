package com.example.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.remote.AppUpdateStatus
import com.example.domain.calc.FitnessCalculations
import com.example.domain.model.GoalType
import com.example.domain.model.UnitSystem
import com.example.ui.components.DailyScoreRing
import com.example.ui.components.FitTrackCard
import com.example.ui.components.Hydration3DRing
import com.example.ui.components.MacroProgressBar
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.FitTrackThemeExt
import com.example.ui.theme.HomeIndigoTop
import com.example.ui.theme.HomeTerracotta
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.viewmodel.FitTrackUiState
import kotlin.math.max

@Composable
fun DashboardScreen(
    state: FitTrackUiState,
    appUpdateStatus: AppUpdateStatus = AppUpdateStatus.Idle,
    updateBannerDismissed: Boolean = false,
    onDismissUpdateBanner: () -> Unit = {},
    onSyncClick: () -> Unit,
    onLogWeight: (Double, String) -> Unit,
    onLogWater: (Int) -> Unit,
    onNavigateToNutrition: () -> Unit,
    onNavigateToProgress: () -> Unit
) {
    val context = LocalContext.current
    val profile = state.profile
    val unit = state.preferences.unitSystem
    val goalType = GoalType.fromNameSafe(profile?.goalType)
    val ext = FitTrackThemeExt.colors

    var showScoreBreakdown by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = "${goalType.label} Mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = state.selectedDateKey,
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.mutedText
                        )
                    }
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
                FitTrackCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                    modifier = Modifier.testTag("app_update_banner")
                ) {
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

        // 1. HERO: Today's Score — Layered 3D Depth Slab + Sculpted 3D Ring
        item {
            FitTrackCard(
                onClick = { showScoreBreakdown = true },
                containerColor = lerp(HomeIndigoTop, HomeTerracotta, 0.14f),
                accentGradientEnd = ext.cardSurfaceBottom,
                borderColor = HomeTerracotta.copy(alpha = 0.55f),
                depthElevation = 7.dp,
                modifier = Modifier.testTag("daily_score_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DailyScoreRing(score = state.dailyScore.totalScore, size = 118.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Score",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "${state.activeStreakDays}d streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.dailyScore.totalScore >= 75) {
                                "Strong alignment with your ${goalType.label.lowercase()} targets."
                            } else {
                                "Tap to inspect your 5-pillar daily score."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.mutedText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Mini3DScoreChip("Food", "${state.dailyScore.nutritionPoints}/25", Modifier.weight(1f))
                            Mini3DScoreChip("Pro", "${state.dailyScore.proteinPoints}/20", Modifier.weight(1f))
                            Mini3DScoreChip("Lift", "${state.dailyScore.workoutPoints}/25", Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap card for full breakdown →",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 2. WEIGHT & TREND — Layered 3D Depth KPI + Mini Sparkline + Progress Well
        item {
            val latestKg = state.weightTrend.latestWeightKg ?: profile?.currentWeightKg ?: 75.0
            val goalKg = profile?.goalWeightKg ?: 70.0
            val rolling7Kg = state.weightTrend.rolling7DayAvgKg
            val trendRate = state.weightTrend.trend14DayKgPerWeek ?: state.weightTrend.trend30DayKgPerWeek
            val sparkValues = remember(state.weightLogs) {
                state.weightLogs.takeLast(10).map { it.weightKg }
            }

            FitTrackCard(
                onClick = onNavigateToProgress,
                depthElevation = 6.dp,
                modifier = Modifier.testTag("weight_trend_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MonitorWeight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Weight & Trend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = trendRate?.let { "Trend: ${FitnessCalculations.formatSignedWeightRate(it, unit)}" }
                                    ?: "Log weigh-ins to plot trend",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Button(
                        onClick = { showWeightDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("quick_weigh_in_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Weigh In", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dual 3D Recessed Stat Wells
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RecessedStatWell(
                        label = "7-Day Rolling Avg",
                        value = FitnessCalculations.formatWeight(rolling7Kg ?: latestKg, unit),
                        subLabel = "Latest: ${FitnessCalculations.formatWeight(latestKg, unit)}",
                        valueColor = ext.success,
                        modifier = Modifier.weight(1f)
                    )
                    RecessedStatWell(
                        label = "Goal Weight",
                        value = FitnessCalculations.formatWeight(goalKg, unit),
                        subLabel = state.weightTrend.projectedWeeksToGoal?.let { "~${it} wks est." } ?: "${state.goalProgressPercent}% reached",
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (sparkValues.size >= 2) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MiniWeightSparkline(
                        values = sparkValues,
                        lineColor = ext.success,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Goal Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.mutedText
                    )
                    Text(
                        text = "${state.goalProgressPercent}%",
                        style = MonoMetricSmall,
                        color = ext.success,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                BeveledGoalBar(
                    progress = state.goalProgressPercent / 100f,
                    accentColor = ext.success
                )
            }
        }

        // 3. NUTRITION TODAY + WATER LOG — Layered 3D Cockpit with 3D Hydration Ring & Quick Add
        item {
            val quick250Label = if (unit == UnitSystem.IMPERIAL) "+8 oz" else "+250 ml"
            val quick500Label = if (unit == UnitSystem.IMPERIAL) "+17 oz" else "+500 ml"

            FitTrackCard(
                onClick = onNavigateToNutrition,
                depthElevation = 6.dp,
                modifier = Modifier.testTag("dashboard_nutrition_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Nutrition Today",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Open Diary →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                    accentColor = ext.success
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardMacroPill("Carbs", "${state.todayCarbsG}/${state.targetCarbsG}g", Modifier.weight(1f))
                    DashboardMacroPill("Fat", "${state.todayFatG}/${state.targetFatG}g", Modifier.weight(1f))
                    DashboardMacroPill("Fiber", "${state.todayFiberG}/${state.targetFiberG}g", Modifier.weight(1f))
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                )

                // Embedded 3D Water Log Row inside Nutrition Today
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_water_section"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Hydration3DRing(
                        consumedMl = state.todayWaterMl,
                        targetMl = state.waterTargetMl,
                        compactLabel = FitnessCalculations.formatWaterCompact(state.todayWaterMl, unit),
                        size = 86.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = null,
                                tint = ext.hydrationAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Water Log",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${FitnessCalculations.formatWater(state.todayWaterMl, unit)} / ${FitnessCalculations.formatWater(state.waterTargetMl, unit)}",
                            style = MonoMetricSmall,
                            color = ext.hydrationAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { onLogWater(250) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("home_water_add_250")
                            ) {
                                Text(quick250Label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onLogWater(500) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ext.hydrationAccent,
                                    contentColor = Color(0xFF0A1929)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("home_water_add_500")
                            ) {
                                Text(quick500Label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Compact 3D Energy Balance Summary
        item {
            val eb = state.energyBalance
            val sign = if (eb.estimatedBalanceKcal > 0) "+" else ""
            val eqFormatted = String.format("%+.2f %s", FitnessCalculations.kgToDisplay(eb.estimatedWeightChangeKg, unit), unit.weightUnit)

            FitTrackCard(
                depthElevation = 5.dp,
                modifier = Modifier.testTag("energy_balance_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Energy Balance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$sign${eb.estimatedBalanceKcal} kcal",
                        style = MonoMetricMedium,
                        color = if (eb.estimatedBalanceKcal <= 0) ext.success else ext.warning
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                EnergyRow("Maintenance (Est. TDEE)", "${eb.estimatedMaintenanceKcal} kcal")
                EnergyRow("Food Intake Logged", "${eb.foodIntakeKcal} kcal")
                EnergyRow("Activity Burn (Est.)", "${eb.workoutBurnKcal + eb.cardioBurnKcal} kcal")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
                Text(
                    text = "Energy-equivalent delta: ~$eqFormatted (7,700 kcal/kg)",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.mutedText
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
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
                        color = ext.mutedText
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
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Text(
                                text = comp.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.mutedText
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
private fun Mini3DScoreChip(label: String, score: String, modifier: Modifier = Modifier) {
    val ext = FitTrackThemeExt.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = ext.cardBottomShadow.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, ext.cardTopHighlight.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ext.mutedText
            )
            Text(
                text = score,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecessedStatWell(
    label: String,
    value: String,
    subLabel: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val ext = FitTrackThemeExt.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = ext.cardBottomShadow.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, ext.cardTopHighlight.copy(alpha = 0.28f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = ext.mutedText)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MonoMetricMedium, color = valueColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subLabel, style = MaterialTheme.typography.labelSmall, color = ext.mutedText)
        }
    }
}

@Composable
private fun DashboardMacroPill(label: String, value: String, modifier: Modifier = Modifier) {
    val ext = FitTrackThemeExt.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ext.cardBottomShadow.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, ext.cardTopHighlight.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = ext.mutedText)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MonoMetricSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MiniWeightSparkline(
    values: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val minV = (values.minOrNull() ?: 0.0) - 0.3
    val maxV = (values.maxOrNull() ?: 1.0) + 0.3
    val range = max(0.5, maxV - minV)

    Canvas(modifier = modifier) {
        val pad = 6.dp.toPx()
        val usableW = max(1f, size.width - pad * 2f)
        val usableH = max(1f, size.height - pad * 2f)
        val pts = values.mapIndexed { idx, v ->
            val x = pad + usableW * idx / (values.size - 1).toFloat()
            val y = pad + (((maxV - v) / range).toFloat().coerceIn(0f, 1f) * usableH)
            Offset(x, y)
        }
        val linePath = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 0 until pts.lastIndex) {
                val cx = (pts[i].x + pts[i + 1].x) / 2f
                cubicTo(cx, pts[i].y, cx, pts[i + 1].y, pts[i + 1].x, pts[i + 1].y)
            }
        }
        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(pts.last().x, size.height - pad)
            lineTo(pts.first().x, size.height - pad)
            close()
        }
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
            )
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(color = lineColor.copy(alpha = 0.3f), radius = 6.dp.toPx(), center = pts.last())
        drawCircle(color = lineColor, radius = 3.2.dp.toPx(), center = pts.last())
    }
}

@Composable
private fun BeveledGoalBar(progress: Float, accentColor: Color) {
    val animProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 220f),
        label = "goalBar3d"
    )
    val ext = FitTrackThemeExt.colors
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(RoundedCornerShape(999.dp))
    ) {
        val r = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = ext.cardBottomShadow.copy(alpha = 0.7f), cornerRadius = r)
        val w = size.width * animProgress
        if (w > 2f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(accentColor, lerp(accentColor, Color.White, 0.3f))),
                size = Size(w, size.height),
                cornerRadius = r
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(2.dp.toPx(), 1.dp.toPx()),
                size = Size((w - 4.dp.toPx()).coerceAtLeast(0f), size.height * 0.34f),
                cornerRadius = r
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
