package com.example.ui.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import com.example.data.local.CardioSessionEntity
import com.example.data.local.ExerciseEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.local.WorkoutSetEntity
import com.example.data.local.WorkoutTemplateEntity
import com.example.data.seed.SeedExerciseCatalog
import com.example.data.seed.SeedSerialization
import com.example.data.seed.TemplateExerciseSpec
import com.example.domain.calc.FitnessCalculations
import com.example.domain.model.CardioActivityType
import com.example.domain.model.MuscleGroup
import com.example.domain.model.UnitSystem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FitTrackCard
import com.example.ui.components.MetricBaselineResetDialog
import com.example.ui.components.SectionHeader
import com.example.ui.theme.FitTrackThemeExt
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.WorkoutCreamText
import com.example.ui.theme.WorkoutCrimson
import com.example.ui.theme.WorkoutCrimsonBright
import com.example.ui.theme.WorkoutOutline
import com.example.ui.theme.WorkoutRoyalBlue
import com.example.ui.theme.WorkoutRoyalBlueBright
import com.example.ui.theme.WorkoutRoyalBlueGlow
import com.example.viewmodel.FitTrackUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private enum class TrainingEnvironment(
    val title: String,
    val subtitle: String
) {
    GYM("Gym Training", "Weights & Machines"),
    HOME("Home Training", "Bodyweight & No-Gym")
}

@Composable
fun WorkoutScreen(
    state: FitTrackUiState,
    onStartTemplate: (WorkoutTemplateEntity) -> Unit,
    onSaveTemplate: (String?, String, String, List<TemplateExerciseSpec>, Boolean) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onAddCustomExercise: (String, String, String, String, String, String) -> Unit,
    onToggleExerciseFavorite: (ExerciseEntity) -> Unit,
    onOpenCardioDialog: () -> Unit,
    onRepeatCardio: (CardioSessionEntity) -> Unit,
    onDeleteCardio: (String) -> Unit,
    onDeleteWorkoutSession: (String) -> Unit,
    onResetSevenDayVolume: (String, Long) -> Unit = { _, _ -> },
    onClearSevenDayVolumeReset: () -> Unit = {}
) {
    var selectedEnvironment by remember { mutableStateOf(TrainingEnvironment.GYM) }
    var selectedSegment by remember { mutableIntStateOf(0) }
    val segments = listOf("Templates", "Cardio", "Exercises", "History")
    val unit = state.preferences.unitSystem
    val ext = FitTrackThemeExt.colors
    val isHomeMode = selectedEnvironment == TrainingEnvironment.HOME

    var showCreateTemplate by remember { mutableStateOf(false) }
    var showCreateExercise by remember { mutableStateOf(false) }
    var showSevenDayVolumeDialog by remember { mutableStateOf(false) }
    var showVolumeResetDialog by remember { mutableStateOf(false) }
    var exerciseQuery by remember { mutableStateOf("") }
    var selectedMuscleFilter by remember { mutableStateOf<String?>(null) }

    val homeTemplateIds = remember(state.templates) {
        state.templates.filter { SeedExerciseCatalog.isHomeTemplate(it) }.map { it.id }.toSet()
    }
    val visibleTemplates = remember(state.templates, isHomeMode) {
        if (isHomeMode) state.templates.filter { SeedExerciseCatalog.isHomeTemplate(it) }
        else state.templates.filter { !SeedExerciseCatalog.isHomeTemplate(it) }
    }
    val visibleExercises = remember(state.exercises, isHomeMode) {
        if (isHomeMode) state.exercises.filter { SeedExerciseCatalog.isHomeExercise(it) }
        else state.exercises.filter { !it.id.startsWith("ex_home_") }
    }
    val visibleSessions = remember(state.workoutSessions, homeTemplateIds, isHomeMode) {
        if (isHomeMode) {
            state.workoutSessions.filter {
                SeedExerciseCatalog.isHomeSession(it.templateId, it.templateName, homeTemplateIds)
            }
        } else {
            state.workoutSessions.filter {
                !SeedExerciseCatalog.isHomeSession(it.templateId, it.templateName, homeTemplateIds)
            }
        }
    }
    val last7Keys = remember(state.selectedDateKey) {
        (0..6).map { FitnessCalculations.dateKeyDaysAgo(it) }.toSet()
    }
    val volResetDate = state.preferences.volumeResetDateKey
    val volResetEpoch = state.preferences.volumeResetEpochMs
    val effectiveSessionsFor7Day = remember(state.workoutSessions, volResetDate, volResetEpoch) {
        state.workoutSessions.filter { s ->
            (volResetDate.isBlank() || s.dateKey >= volResetDate) &&
                (volResetEpoch <= 0L || s.completedAtEpochMs >= volResetEpoch)
        }
    }
    val effectiveCardioFor7Day = remember(state.cardioSessions, volResetDate, volResetEpoch) {
        state.cardioSessions.filter { c ->
            (volResetDate.isBlank() || c.dateKey >= volResetDate) &&
                (volResetEpoch <= 0L || c.startedAtEpochMs >= volResetEpoch)
        }
    }
    val homeSessionsLast7 = remember(visibleSessions, last7Keys, volResetDate, volResetEpoch) {
        visibleSessions.filter { s ->
            s.dateKey in last7Keys &&
                (volResetDate.isBlank() || s.dateKey >= volResetDate) &&
                (volResetEpoch <= 0L || s.completedAtEpochMs >= volResetEpoch)
        }
    }
    val homeRepsLast7 = remember(homeSessionsLast7, state.allWorkoutSets) {
        val ids = homeSessionsLast7.map { it.id }.toSet()
        state.allWorkoutSets.filter { it.workoutSessionId in ids && it.completed && !it.warmup }.sumOf { it.reps }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("workout_screen")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Training Studio",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${state.workoutsThisWeek} / ${state.profile?.trainingDaysPerWeek ?: 4} sessions this week",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.mutedText
                    )
                }
                Button(
                    onClick = onOpenCardioDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WorkoutRoyalBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("workout_quick_cardio_button")
                ) {
                    Icon(Icons.Rounded.DirectionsRun, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cardio")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Clean Two-Button Studio Mode Selector: Gym Training vs Home Training
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrainingModeSelectorButton(
                    title = TrainingEnvironment.GYM.title,
                    subtitle = TrainingEnvironment.GYM.subtitle,
                    icon = Icons.Rounded.FitnessCenter,
                    selected = !isHomeMode,
                    activeColor = WorkoutCrimson,
                    activeBorderColor = WorkoutCrimsonBright,
                    onClick = {
                        selectedEnvironment = TrainingEnvironment.GYM
                        selectedMuscleFilter = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_gym_training_button")
                )
                TrainingModeSelectorButton(
                    title = TrainingEnvironment.HOME.title,
                    subtitle = TrainingEnvironment.HOME.subtitle,
                    icon = Icons.Rounded.Home,
                    selected = isHomeMode,
                    activeColor = WorkoutRoyalBlue,
                    activeBorderColor = WorkoutRoyalBlueGlow,
                    onClick = {
                        selectedEnvironment = TrainingEnvironment.HOME
                        selectedMuscleFilter = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_home_training_button")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual-Tone Athletic 3D Split Command Bar (Crimson #701A1F + Royal Blue #1E3A8A)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FitTrackCard(
                    containerColor = WorkoutCrimson,
                    accentGradientEnd = Color(0xFF471015),
                    borderColor = WorkoutCrimsonBright,
                    depthElevation = 5.dp,
                    onClick = { showSevenDayVolumeDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("seven_day_volume_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isHomeMode) Icons.Rounded.Home else Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHomeMode) "7-DAY HOME REPS" else "7-DAY VOLUME",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            onClick = { showVolumeResetDialog = true },
                            shape = RoundedCornerShape(999.dp),
                            color = Color.Black.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
                            modifier = Modifier.testTag("reset_seven_day_volume_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = "Reset 7-day volume baseline",
                                    tint = WorkoutCreamText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WorkoutCreamText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHomeMode) {
                            "$homeRepsLast7 reps • ${homeSessionsLast7.size}d"
                        } else {
                            FitnessCalculations.formatVolume(state.weeklyVolumeKg, unit)
                        },
                        style = MonoMetricMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (volResetDate.isNotBlank()) {
                            "Since ${volResetDate.takeLast(5)} • Daily log ▸"
                        } else {
                            "Tap for daily log ▸"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = WorkoutCreamText.copy(alpha = 0.85f)
                    )
                }

                FitTrackCard(
                    containerColor = WorkoutRoyalBlue,
                    accentGradientEnd = Color(0xFF122354),
                    borderColor = WorkoutRoyalBlueBright,
                    depthElevation = 5.dp,
                    onClick = onOpenCardioDialog,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsRun,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TODAY CARDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.82f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${state.todayCardioMinutes} / ${state.profile?.cardioTargetMinPerDay ?: 20}m",
                        style = MonoMetricMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                segments.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedSegment == idx,
                        onClick = { selectedSegment = idx },
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = if (idx % 2 == 0) WorkoutCrimson else WorkoutRoyalBlue,
                            activeContentColor = Color.White,
                            inactiveContainerColor = Color(0xFF1D1017),
                            inactiveContentColor = ext.mutedText,
                            activeBorderColor = if (idx % 2 == 0) WorkoutCrimsonBright else WorkoutRoyalBlueBright,
                            inactiveBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = segments.size)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedSegment) {
                0 -> { // Templates (Gym or Home)
                    item {
                        SectionHeader(
                            title = if (isHomeMode) "Home Workout Templates" else "Gym Workout Templates",
                            subtitle = if (isHomeMode) {
                                "No-equipment bodyweight routines ready to start anywhere"
                            } else {
                                "One-tap launch with previous weights & reps pre-loaded"
                            },
                            trailing = {
                                Button(
                                    onClick = { showCreateTemplate = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WorkoutRoyalBlue,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("create_template_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New")
                                }
                            }
                        )
                    }

                    if (visibleTemplates.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = if (isHomeMode) Icons.Rounded.Home else Icons.Rounded.FitnessCenter,
                                title = if (isHomeMode) "No home templates yet" else "No gym templates yet",
                                subtitle = if (isHomeMode) {
                                    "Create a bodyweight routine for home like Full Body or Core Finisher."
                                } else {
                                    "Create a reusable routine like Push Day or Full Body."
                                },
                                actionLabel = "Create Template",
                                onAction = { showCreateTemplate = true }
                            )
                        }
                    } else {
                        items(visibleTemplates, key = { it.id }) { template ->
                            val exercises = remember(template.exercisesJson) {
                                SeedSerialization.decodeTemplateExercises(template.exercisesJson)
                            }
                            val lastSession = remember(state.workoutSessions, template.id, template.name) {
                                state.workoutSessions.firstOrNull {
                                    it.templateId == template.id || it.templateName.equals(template.name, ignoreCase = true)
                                }
                            }
                            FitTrackCard(
                                depthElevation = 6.dp,
                                modifier = Modifier.testTag("template_card_${template.id}")
                            ) {
                                // Crimson-to-Royal-Blue sculpted 3D energy accent bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                if (isHomeMode) listOf(WorkoutRoyalBlueBright, WorkoutCrimsonBright)
                                                else listOf(WorkoutCrimsonBright, WorkoutRoyalBlueBright)
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = template.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = WorkoutCreamText,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = WorkoutRoyalBlue.copy(alpha = 0.85f),
                                                border = BorderStroke(1.dp, WorkoutRoyalBlueGlow.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "${exercises.size} ex",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (template.description.isNotBlank()) {
                                            Text(
                                                template.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ext.mutedText
                                            )
                                        }
                                        Text(
                                            text = if (lastSession != null) {
                                                val metricSuffix = if (lastSession.totalVolumeKg > 0.0) {
                                                    FitnessCalculations.formatVolume(lastSession.totalVolumeKg, unit)
                                                } else {
                                                    "${lastSession.durationMinutes} min • Bodyweight"
                                                }
                                                "Last done: ${lastSession.dateKey} • $metricSuffix"
                                            } else {
                                                "No completed sessions yet"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (lastSession != null) WorkoutRoyalBlueGlow else ext.mutedText
                                        )
                                    }
                                    IconButton(onClick = { onDeleteTemplate(template.id) }) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "Delete template",
                                            tint = ext.mutedText
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                exercises.take(5).forEach { ex ->
                                    val wtSuffix = if (ex.targetWeightKg > 0.0) {
                                        " • ${FitnessCalculations.formatWeight(ex.targetWeightKg, unit)}"
                                    } else if (isHomeMode) {
                                        " • Bodyweight"
                                    } else ""
                                    Text(
                                        text = "• ${ex.exerciseName} (${ex.targetSets}×${ex.targetReps}$wtSuffix)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (exercises.size > 5) {
                                    Text(
                                        text = "+ ${exercises.size - 5} more exercises",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WorkoutRoyalBlueGlow
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onStartTemplate(template) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isHomeMode) WorkoutRoyalBlue else WorkoutCrimson,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("start_template_${template.id}")
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start ${template.name}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                1 -> { // Cardio
                    item {
                        FitTrackCard(
                            containerColor = WorkoutRoyalBlue,
                            accentGradientEnd = Color(0xFF10204F),
                            borderColor = WorkoutRoyalBlueGlow.copy(alpha = 0.65f),
                            depthElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Today's Cardio", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                                    Text(
                                        text = "${state.todayCardioMinutes} / ${state.profile?.cardioTargetMinPerDay ?: 20} min",
                                        style = MonoMetricMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = onOpenCardioDialog,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WorkoutCrimson,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Log Session")
                                }
                            }
                        }
                    }
                    if (state.cardioSessions.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.DirectionsRun,
                                title = "No cardio sessions logged yet",
                                subtitle = "Track walking, running, cycling, stairmaster, rowing, and more with MET calorie estimates.",
                                actionLabel = "Log First Cardio",
                                onAction = onOpenCardioDialog
                            )
                        }
                    } else {
                        items(state.cardioSessions, key = { it.id }) { session ->
                            val act = CardioActivityType.fromNameSafe(session.activityType)
                            FitTrackCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${act.label} • ${session.dateKey}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        val details = buildList {
                                            add("${session.durationMinutes} min")
                                            if (session.distanceKm > 0) add(FitnessCalculations.formatDistance(session.distanceKm, unit))
                                            if (session.inclinePercent > 0) add("${session.inclinePercent}% incline")
                                            if (session.resistanceLevel > 0) add("Lvl ${session.resistanceLevel}")
                                            add("~${session.caloriesEstimated} kcal est.")
                                        }.joinToString(" • ")
                                        Text(details, style = MaterialTheme.typography.bodySmall, color = ext.mutedText)
                                    }
                                    Row {
                                        IconButton(onClick = { onRepeatCardio(session) }) {
                                            Icon(Icons.Rounded.Replay, contentDescription = "Repeat cardio", tint = WorkoutRoyalBlueGlow)
                                        }
                                        IconButton(onClick = { onDeleteCardio(session.id) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete cardio", tint = ext.mutedText)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> { // Exercise Library
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = exerciseQuery,
                                onValueChange = { exerciseQuery = it },
                                placeholder = { Text("Search exercises...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { showCreateExercise = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WorkoutCrimson,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add custom exercise")
                            }
                        }
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedMuscleFilter == null,
                                    onClick = { selectedMuscleFilter = null },
                                    label = { Text("All") }
                                )
                            }
                            items(MuscleGroup.entries) { mg ->
                                FilterChip(
                                    selected = selectedMuscleFilter == mg.name,
                                    onClick = { selectedMuscleFilter = if (selectedMuscleFilter == mg.name) null else mg.name },
                                    label = { Text(mg.label) }
                                )
                            }
                        }
                    }

                    val filtered = visibleExercises.filter { ex ->
                        (selectedMuscleFilter == null || ex.muscleGroup.equals(selectedMuscleFilter, ignoreCase = true)) &&
                            (exerciseQuery.isBlank() || ex.name.contains(exerciseQuery, ignoreCase = true))
                    }
                    items(filtered, key = { it.id }) { ex ->
                        FitTrackCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ex.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = WorkoutCreamText)
                                    Text(
                                        text = "${MuscleGroup.fromNameSafe(ex.muscleGroup).label} • ${ex.equipment} • ${ex.difficulty}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WorkoutRoyalBlueGlow
                                    )
                                    if (ex.instructions.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = ex.instructions,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ext.mutedText
                                        )
                                    }
                                }
                                IconButton(onClick = { onToggleExerciseFavorite(ex) }) {
                                    Icon(
                                        imageVector = if (ex.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                        contentDescription = "Favorite exercise",
                                        tint = if (ex.favorite) ext.warning else ext.mutedText
                                    )
                                }
                            }
                        }
                    }
                }

                3 -> { // History & Volume (scoped to Gym or Home)
                    item {
                        FitTrackCard(
                            containerColor = if (isHomeMode) WorkoutRoyalBlue else WorkoutCrimson,
                            accentGradientEnd = if (isHomeMode) WorkoutCrimson else WorkoutRoyalBlue,
                            borderColor = WorkoutRoyalBlueGlow.copy(alpha = 0.55f),
                            depthElevation = 6.dp,
                            onClick = { showSevenDayVolumeDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHomeMode) "Home Training Summary" else "Strength Volume Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WorkoutCreamText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("7-Day Log ▸", style = MaterialTheme.typography.labelSmall, color = WorkoutCreamText.copy(alpha = 0.85f))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            if (isHomeMode) {
                                val totalHomeSessions = visibleSessions.size
                                val totalHomeMinutes = visibleSessions.sumOf { it.durationMinutes }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    VolumeStat("7-Day Reps", "$homeRepsLast7")
                                    VolumeStat("Sessions", "$totalHomeSessions")
                                    VolumeStat("Total Time", "${totalHomeMinutes}m")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    VolumeStat("Today", FitnessCalculations.formatVolume(state.dailyVolumeKg, unit))
                                    VolumeStat("7-Day", FitnessCalculations.formatVolume(state.weeklyVolumeKg, unit))
                                    VolumeStat("30-Day", FitnessCalculations.formatVolume(state.monthlyVolumeKg, unit))
                                }
                            }
                        }
                    }

                    if (visibleSessions.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.History,
                                title = if (isHomeMode) "No home workouts logged yet" else "No completed gym workouts yet",
                                subtitle = if (isHomeMode) {
                                    "Start a Home Training template to log reps, sets, calories, and bodyweight PRs."
                                } else {
                                    "Start a template to track sets, progressive overload, and automatic PRs."
                                }
                            )
                        }
                    } else {
                        items(visibleSessions, key = { it.id }) { session ->
                            val sessionReps = remember(state.allWorkoutSets, session.id) {
                                state.allWorkoutSets
                                    .filter { it.workoutSessionId == session.id && it.completed && !it.warmup }
                                    .sumOf { it.reps }
                            }
                            FitTrackCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.templateName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WorkoutCreamText
                                        )
                                        val loadMetric = if (session.totalVolumeKg > 0.0) {
                                            FitnessCalculations.formatVolume(session.totalVolumeKg, unit)
                                        } else {
                                            "$sessionReps reps (BW)"
                                        }
                                        Text(
                                            text = "${session.dateKey} • ${session.durationMinutes} min • $loadMetric • ~${session.estimatedCalories} kcal est.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ext.mutedText
                                        )
                                        if (session.prCount > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Rounded.EmojiEvents,
                                                    contentDescription = null,
                                                    tint = ext.success,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "${session.prCount} Personal Record${if (session.prCount > 1) "s" else ""}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ext.success
                                                )
                                            }
                                        }
                                    }
                                    IconButton(onClick = { onDeleteWorkoutSession(session.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete workout", tint = ext.mutedText)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showCreateTemplate) {
        CreateTemplateDialog(
            exercises = visibleExercises.ifEmpty { state.exercises },
            isHomeMode = isHomeMode,
            onDismiss = { showCreateTemplate = false },
            onSave = { name, desc, specs ->
                onSaveTemplate(null, name, desc, specs, isHomeMode)
                showCreateTemplate = false
            }
        )
    }

    if (showCreateExercise) {
        CreateCustomExerciseDialog(
            defaultEquipment = if (isHomeMode) "Bodyweight" else "Dumbbell",
            onDismiss = { showCreateExercise = false },
            onSave = { name, muscle, secondary, equip, diff, instr ->
                onAddCustomExercise(name, muscle, secondary, equip, diff, instr)
                showCreateExercise = false
            }
        )
    }

    if (showSevenDayVolumeDialog) {
        SevenDayVolumeProgressDialog(
            workoutSessions = effectiveSessionsFor7Day,
            allWorkoutSets = state.allWorkoutSets,
            cardioSessions = effectiveCardioFor7Day,
            unitSystem = unit,
            activeResetDateKey = volResetDate,
            onOpenResetDialog = {
                showSevenDayVolumeDialog = false
                showVolumeResetDialog = true
            },
            onDismiss = { showSevenDayVolumeDialog = false }
        )
    }

    if (showVolumeResetDialog) {
        MetricBaselineResetDialog(
            title = "Reset 7-Day Volume",
            preservedNote = "Reset your 7-Day Volume / Home Reps counter to start fresh right now, from Monday (first of week), or any specific day. All past sessions remain safely saved in Workout History & Progress.",
            activeResetDateKey = volResetDate,
            activeResetEpochMs = volResetEpoch,
            onApplyReset = onResetSevenDayVolume,
            onClearReset = onClearSevenDayVolumeReset,
            onDismiss = { showVolumeResetDialog = false }
        )
    }
}

@Composable
private fun TrainingModeSelectorButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    activeColor: Color,
    activeBorderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) activeColor else Color(0xFF1A1019),
        border = BorderStroke(
            width = if (selected) 1.6.dp else 1.dp,
            color = if (selected) activeBorderColor else WorkoutOutline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) WorkoutCreamText else FitTrackThemeExt.colors.mutedText,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) WorkoutCreamText else Color.White.copy(alpha = 0.78f),
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White.copy(alpha = 0.85f) else FitTrackThemeExt.colors.mutedText,
                    maxLines = 1
                )
            }
        }
    }
}

private data class SevenDayEntry(
    val dateKey: String,
    val dayShort: String,
    val dayNumber: String,
    val fullLabel: String,
    val workouts: List<WorkoutSessionEntity>,
    val cardio: List<CardioSessionEntity>,
    val volumeKg: Double
)

@Composable
private fun SevenDayVolumeProgressDialog(
    workoutSessions: List<WorkoutSessionEntity>,
    allWorkoutSets: List<WorkoutSetEntity>,
    cardioSessions: List<CardioSessionEntity>,
    unitSystem: UnitSystem,
    activeResetDateKey: String = "",
    onOpenResetDialog: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val ext = FitTrackThemeExt.colors
    val days = remember(workoutSessions, cardioSessions) {
        val parseFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val weekdayFmt = SimpleDateFormat("EEE", Locale.US)
        val fullFmt = SimpleDateFormat("EEE, MMM d", Locale.US)
        (6 downTo 0).map { daysAgo ->
            val dk = FitnessCalculations.dateKeyDaysAgo(daysAgo)
            val parsed: Date? = runCatching { parseFmt.parse(dk) }.getOrNull()
            val daySessions = workoutSessions.filter { it.dateKey == dk }
            val dayCardio = cardioSessions.filter { it.dateKey == dk }
            SevenDayEntry(
                dateKey = dk,
                dayShort = if (daysAgo == 0) "Today" else parsed?.let { weekdayFmt.format(it) } ?: dk.takeLast(2),
                dayNumber = dk.takeLast(2),
                fullLabel = if (daysAgo == 0) "Today ($dk)" else parsed?.let { fullFmt.format(it) } ?: dk,
                workouts = daySessions,
                cardio = dayCardio,
                volumeKg = daySessions.sumOf { it.totalVolumeKg }
            )
        }
    }

    val defaultSelectedIndex = remember(days) {
        val lastActive = days.indexOfLast { it.workouts.isNotEmpty() || it.cardio.isNotEmpty() }
        if (lastActive >= 0) lastActive else days.lastIndex
    }
    var selectedDayIndex by remember(defaultSelectedIndex) { mutableIntStateOf(defaultSelectedIndex) }
    val selectedDay = days[selectedDayIndex.coerceIn(0, days.lastIndex)]
    val maxDayVol = max(1.0, days.maxOfOrNull { it.volumeKg } ?: 1.0)
    val total7DayVol = days.sumOf { it.volumeKg }
    val activeDaysCount = days.count { it.workouts.isNotEmpty() || it.cardio.isNotEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A0F18),
        titleContentColor = WorkoutCreamText,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "7-Day Workout Progress",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutCreamText
                    )
                    Text(
                        text = "${FitnessCalculations.formatVolume(total7DayVol, unitSystem)} • $activeDaysCount/7 active days" +
                            if (activeResetDateKey.isNotBlank()) " (from $activeResetDateKey)" else "",
                        style = MonoMetricSmall,
                        color = WorkoutRoyalBlueGlow
                    )
                }
                TextButton(onClick = onOpenResetDialog) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        tint = WorkoutCreamText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Reset", color = WorkoutCreamText)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("seven_day_volume_dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive 7-day selector bar chart strip
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF120B12),
                    border = BorderStroke(1.dp, WorkoutOutline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        days.forEachIndexed { idx, entry ->
                            val isSelected = idx == selectedDayIndex
                            val hasWorkout = entry.workouts.isNotEmpty()
                            val hasCardio = entry.cardio.isNotEmpty()
                            val barRatio = if (entry.volumeKg > 0.0) {
                                (entry.volumeKg / maxDayVol).toFloat().coerceIn(0.18f, 1f)
                            } else if (hasCardio) 0.14f else 0.06f

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) WorkoutRoyalBlue.copy(alpha = 0.55f)
                                        else Color.Transparent
                                    )
                                    .clickable { selectedDayIndex = idx }
                                    .padding(vertical = 6.dp, horizontal = 2.dp)
                                    .testTag("seven_day_cell_$idx")
                            ) {
                                // Mini volume bar
                                Box(
                                    modifier = Modifier
                                        .height(46.dp)
                                        .width(12.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((46f * barRatio).dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when {
                                                    hasWorkout && isSelected -> WorkoutCreamText
                                                    hasWorkout -> WorkoutCrimsonBright
                                                    hasCardio -> WorkoutRoyalBlueGlow
                                                    else -> Color.White.copy(alpha = 0.14f)
                                                }
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = entry.dayShort.take(3),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) WorkoutCreamText else ext.mutedText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                                Text(
                                    text = entry.dayNumber,
                                    style = MonoMetricSmall,
                                    color = if (isSelected) Color.White else ext.mutedText
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                hasWorkout -> WorkoutCrimsonBright
                                                hasCardio -> WorkoutRoyalBlueGlow
                                                else -> Color.Transparent
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                // Selected Day Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDay.fullLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutCreamText
                    )
                    Text(
                        text = FitnessCalculations.formatVolume(selectedDay.volumeKg, unitSystem),
                        style = MonoMetricSmall,
                        color = WorkoutRoyalBlueGlow,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Selected Day Strength Workouts + Exercise Breakdown
                if (selectedDay.workouts.isEmpty() && selectedDay.cardio.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF22141E),
                        border = BorderStroke(1.dp, WorkoutOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Rest / Recovery Day",
                                style = MaterialTheme.typography.titleSmall,
                                color = WorkoutCreamText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No strength or cardio sessions logged on this day. Tap another day above to inspect.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.mutedText
                            )
                        }
                    }
                } else {
                    selectedDay.workouts.forEach { session ->
                        val sessionSets = remember(allWorkoutSets, session.id) {
                            allWorkoutSets
                                .filter { it.workoutSessionId == session.id && it.completed }
                                .sortedBy { it.setNumber }
                        }
                        val groupedByExercise = remember(sessionSets) {
                            sessionSets.groupBy { it.exerciseName }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = WorkoutCrimson.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, WorkoutCrimsonBright.copy(alpha = 0.7f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = session.templateName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = WorkoutCreamText,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${session.durationMinutes}m",
                                        style = MonoMetricSmall,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "${FitnessCalculations.formatVolume(session.totalVolumeKg, unitSystem)} • ${sessionSets.size} sets • ~${session.estimatedCalories} kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                if (session.prCount > 0) {
                                    Text(
                                        text = "★ ${session.prCount} Personal Record${if (session.prCount > 1) "s" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.success,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (groupedByExercise.isNotEmpty()) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
                                    groupedByExercise.forEach { (exerciseName, exSets) ->
                                        val workingSets = exSets.filter { !it.warmup }.ifEmpty { exSets }
                                        val allBodyweight = workingSets.all { it.weightKg <= 0.01 }
                                        val setsLine = workingSets.joinToString(", ") { s ->
                                            if (s.weightKg <= 0.01) {
                                                "BW×${s.reps}"
                                            } else {
                                                "${FitnessCalculations.formatWeight(s.weightKg, unitSystem, includeUnit = false)}×${s.reps}"
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = exerciseName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = WorkoutCreamText
                                            )
                                            Text(
                                                text = if (allBodyweight) {
                                                    "${workingSets.size} sets (Bodyweight): $setsLine"
                                                } else {
                                                    "${workingSets.size} sets (${unitSystem.weightUnit}): $setsLine"
                                                },
                                                style = MonoMetricSmall,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    selectedDay.cardio.forEach { cardio ->
                        val act = CardioActivityType.fromNameSafe(cardio.activityType)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = WorkoutRoyalBlue.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, WorkoutRoyalBlueGlow.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cardio: ${act.label}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = WorkoutCreamText
                                    )
                                    val distPart = if (cardio.distanceKm > 0) {
                                        " • ${FitnessCalculations.formatDistance(cardio.distanceKm, unitSystem)}"
                                    } else ""
                                    Text(
                                        text = "${cardio.durationMinutes} min$distPart • ~${cardio.caloriesEstimated} kcal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.DirectionsRun,
                                    contentDescription = null,
                                    tint = WorkoutRoyalBlueGlow
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = WorkoutCreamText, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun VolumeStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
        Text(value, style = MonoMetricSmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CreateTemplateDialog(
    exercises: List<ExerciseEntity>,
    isHomeMode: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String, String, List<TemplateExerciseSpec>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isHomeMode) "New Home Workout Template" else "New Gym Workout Template") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text(if (isHomeMode) "Template Name (e.g., Home Morning Burn)" else "Template Name (e.g., Upper Power)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it.take(120) },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Select Exercises (${selectedIds.size} chosen)", style = MaterialTheme.typography.labelLarge)
                exercises.take(40).forEach { ex ->
                    val checked = ex.id in selectedIds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                if (isChecked) selectedIds.add(ex.id) else selectedIds.remove(ex.id)
                            }
                        )
                        Column {
                            Text(ex.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${MuscleGroup.fromNameSafe(ex.muscleGroup).label} • ${ex.equipment}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FitTrackThemeExt.colors.mutedText
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val specs = exercises.filter { it.id in selectedIds }.map { ex ->
                        TemplateExerciseSpec(
                            exerciseId = ex.id,
                            exerciseName = ex.name,
                            muscleGroup = ex.muscleGroup,
                            targetSets = 3,
                            targetReps = if (isHomeMode) 12 else 10,
                            targetWeightKg = if (isHomeMode) 0.0 else 20.0,
                            restSeconds = if (isHomeMode) 60 else 90
                        )
                    }
                    onSave(name, desc, specs)
                },
                enabled = name.isNotBlank() && selectedIds.isNotEmpty()
            ) { Text("Save Template") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateCustomExerciseDialog(
    defaultEquipment: String = "Dumbbell",
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf(MuscleGroup.CHEST) }
    var equipment by remember { mutableStateOf(defaultEquipment) }
    var instructions by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Exercise") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("Exercise Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Primary Muscle Group", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MuscleGroup.entries) { mg ->
                        FilterChip(
                            selected = muscle == mg,
                            onClick = { muscle = mg },
                            label = { Text(mg.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it.take(40) },
                    label = { Text("Equipment") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it.take(300) },
                    label = { Text("Form Cues (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, muscle.name, "", equipment, "Intermediate", instructions) },
                enabled = name.isNotBlank()
            ) { Text("Add Exercise") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
