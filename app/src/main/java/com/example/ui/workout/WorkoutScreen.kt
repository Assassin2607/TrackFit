package com.example.ui.workout

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CardioSessionEntity
import com.example.data.local.ExerciseEntity
import com.example.data.local.WorkoutTemplateEntity
import com.example.data.seed.SeedSerialization
import com.example.data.seed.TemplateExerciseSpec
import com.example.domain.calc.FitnessCalculations
import com.example.domain.model.CardioActivityType
import com.example.domain.model.MuscleGroup
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FitTrackCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.FitTrackThemeExt
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.viewmodel.FitTrackUiState

@Composable
fun WorkoutScreen(
    state: FitTrackUiState,
    onStartTemplate: (WorkoutTemplateEntity) -> Unit,
    onSaveTemplate: (String?, String, String, List<TemplateExerciseSpec>) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onAddCustomExercise: (String, String, String, String, String, String) -> Unit,
    onToggleExerciseFavorite: (ExerciseEntity) -> Unit,
    onOpenCardioDialog: () -> Unit,
    onRepeatCardio: (CardioSessionEntity) -> Unit,
    onDeleteCardio: (String) -> Unit,
    onDeleteWorkoutSession: (String) -> Unit
) {
    var selectedSegment by remember { mutableIntStateOf(0) }
    val segments = listOf("Templates", "Cardio", "Exercises", "History")
    val unit = state.preferences.unitSystem

    var showCreateTemplate by remember { mutableStateOf(false) }
    var showCreateExercise by remember { mutableStateOf(false) }
    var exerciseQuery by remember { mutableStateOf("") }
    var selectedMuscleFilter by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("workout_screen")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Training", style = MaterialTheme.typography.headlineMedium)
                FilledTonalButton(
                    onClick = onOpenCardioDialog,
                    modifier = Modifier.testTag("workout_quick_cardio_button")
                ) {
                    Icon(Icons.Rounded.DirectionsRun, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Cardio")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                segments.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedSegment == idx,
                        onClick = { selectedSegment = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = segments.size)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedSegment) {
                0 -> { // Templates
                    item {
                        SectionHeader(
                            title = "Workout Templates",
                            subtitle = "One-tap launch with previous weights & reps pre-loaded",
                            trailing = {
                                FilledTonalButton(
                                    onClick = { showCreateTemplate = true },
                                    modifier = Modifier.testTag("create_template_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New")
                                }
                            }
                        )
                    }

                    if (state.templates.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.FitnessCenter,
                                title = "No workout templates yet",
                                subtitle = "Create a reusable routine like Push Day or Full Body.",
                                actionLabel = "Create Template",
                                onAction = { showCreateTemplate = true }
                            )
                        }
                    } else {
                        items(state.templates, key = { it.id }) { template ->
                            val exercises = remember(template.exercisesJson) {
                                SeedSerialization.decodeTemplateExercises(template.exercisesJson)
                            }
                            FitTrackCard(modifier = Modifier.testTag("template_card_${template.id}")) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(template.name, style = MaterialTheme.typography.titleLarge)
                                        if (template.description.isNotBlank()) {
                                            Text(
                                                template.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = FitTrackThemeExt.colors.mutedText
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onDeleteTemplate(template.id) }) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "Delete template",
                                            tint = FitTrackThemeExt.colors.mutedText
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                exercises.take(5).forEach { ex ->
                                    Text(
                                        text = "• ${ex.exerciseName} (${ex.targetSets}×${ex.targetReps})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (exercises.size > 5) {
                                    Text(
                                        text = "+ ${exercises.size - 5} more exercises",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FitTrackThemeExt.colors.mutedText
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onStartTemplate(template) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("start_template_${template.id}")
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start ${template.name}")
                                }
                            }
                        }
                    }
                }

                1 -> { // Cardio
                    item {
                        FitTrackCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Today's Cardio", style = MaterialTheme.typography.labelMedium, color = FitTrackThemeExt.colors.mutedText)
                                    Text(
                                        text = "${state.todayCardioMinutes} / ${state.profile?.cardioTargetMinPerDay ?: 20} min",
                                        style = MonoMetricMedium,
                                        color = FitTrackThemeExt.colors.cardioAccent
                                    )
                                }
                                Button(onClick = onOpenCardioDialog) {
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
                                        Text("${act.label} • ${session.dateKey}", style = MaterialTheme.typography.titleMedium)
                                        val details = buildList {
                                            add("${session.durationMinutes} min")
                                            if (session.distanceKm > 0) add(FitnessCalculations.formatDistance(session.distanceKm, unit))
                                            if (session.inclinePercent > 0) add("${session.inclinePercent}% incline")
                                            if (session.resistanceLevel > 0) add("Lvl ${session.resistanceLevel}")
                                            add("~${session.caloriesEstimated} kcal est.")
                                        }.joinToString(" • ")
                                        Text(details, style = MaterialTheme.typography.bodySmall, color = FitTrackThemeExt.colors.mutedText)
                                    }
                                    Row {
                                        IconButton(onClick = { onRepeatCardio(session) }) {
                                            Icon(Icons.Rounded.Replay, contentDescription = "Repeat cardio", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { onDeleteCardio(session.id) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete cardio", tint = FitTrackThemeExt.colors.mutedText)
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
                            FilledTonalButton(onClick = { showCreateExercise = true }) {
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

                    val filtered = state.exercises.filter { ex ->
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
                                    Text(ex.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${MuscleGroup.fromNameSafe(ex.muscleGroup).label} • ${ex.equipment} • ${ex.difficulty}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (ex.instructions.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = ex.instructions,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FitTrackThemeExt.colors.mutedText
                                        )
                                    }
                                }
                                IconButton(onClick = { onToggleExerciseFavorite(ex) }) {
                                    Icon(
                                        imageVector = if (ex.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                        contentDescription = "Favorite exercise",
                                        tint = if (ex.favorite) FitTrackThemeExt.colors.warning else FitTrackThemeExt.colors.mutedText
                                    )
                                }
                            }
                        }
                    }
                }

                3 -> { // History & Volume
                    item {
                        FitTrackCard {
                            Text("Strength Volume Summary", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(10.dp))
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

                    if (state.workoutSessions.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.History,
                                title = "No completed workouts yet",
                                subtitle = "Start a template to track sets, progressive overload, and automatic PRs."
                            )
                        }
                    } else {
                        items(state.workoutSessions, key = { it.id }) { session ->
                            FitTrackCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(session.templateName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${session.dateKey} • ${session.durationMinutes} min • ${FitnessCalculations.formatVolume(session.totalVolumeKg, unit)} • ~${session.estimatedCalories} kcal est.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FitTrackThemeExt.colors.mutedText
                                        )
                                        if (session.prCount > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Rounded.EmojiEvents,
                                                    contentDescription = null,
                                                    tint = FitTrackThemeExt.colors.success,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "${session.prCount} Personal Record${if (session.prCount > 1) "s" else ""}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = FitTrackThemeExt.colors.success
                                                )
                                            }
                                        }
                                    }
                                    IconButton(onClick = { onDeleteWorkoutSession(session.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete workout", tint = FitTrackThemeExt.colors.mutedText)
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
            exercises = state.exercises,
            onDismiss = { showCreateTemplate = false },
            onSave = { name, desc, specs ->
                onSaveTemplate(null, name, desc, specs)
                showCreateTemplate = false
            }
        )
    }

    if (showCreateExercise) {
        CreateCustomExerciseDialog(
            onDismiss = { showCreateExercise = false },
            onSave = { name, muscle, secondary, equip, diff, instr ->
                onAddCustomExercise(name, muscle, secondary, equip, diff, instr)
                showCreateExercise = false
            }
        )
    }
}

@Composable
private fun VolumeStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = FitTrackThemeExt.colors.mutedText)
        Text(value, style = MonoMetricSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CreateTemplateDialog(
    exercises: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, List<TemplateExerciseSpec>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Workout Template") },
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
                    label = { Text("Template Name (e.g., Upper Power)") },
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
                exercises.take(25).forEach { ex ->
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
                                MuscleGroup.fromNameSafe(ex.muscleGroup).label,
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
                            targetReps = 10,
                            targetWeightKg = 20.0,
                            restSeconds = 90
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
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf(MuscleGroup.CHEST) }
    var equipment by remember { mutableStateOf("Dumbbell") }
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
