package com.example.ui.progress

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ProgressPhotoEntity
import com.example.domain.calc.FitnessCalculations
import com.example.domain.model.MuscleGroup
import com.example.domain.model.PhotoType
import com.example.domain.model.UnitSystem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FitTrackCard
import com.example.ui.components.MetricBarChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.WeightTrendLineChart
import com.example.ui.theme.FitTrackThemeExt
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.viewmodel.FitTrackUiState
import java.io.File

@Composable
fun ProgressScreen(
    state: FitTrackUiState,
    onLogWeight: (Double, String) -> Unit,
    onLogMeasurements: (Double?, Double?, Double?, Double?, Double?, Double?) -> Unit,
    onAddProgressPhoto: (Uri, PhotoType, String) -> Unit,
    onDeleteProgressPhoto: (ProgressPhotoEntity) -> Unit
) {
    var selectedSegment by remember { mutableIntStateOf(0) }
    val segments = listOf("Trends", "Body", "Photos", "PRs")
    val unit = state.preferences.unitSystem

    var showMeasurementDialog by remember { mutableStateOf(false) }
    var selectedPhotoType by remember { mutableStateOf(PhotoType.FRONT) }
    var showCompareDialog by remember { mutableStateOf(false) }

    // Play-Policy Compliant Zero-Permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onAddProgressPhoto(uri, selectedPhotoType, "${selectedPhotoType.label} check-in")
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("progress_screen")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("Progress & Analytics", style = MaterialTheme.typography.headlineMedium)
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
                0 -> { // Trends & Charts
                    item {
                        val dailyPoints = state.weightLogs.map {
                            it.dateKey to FitnessCalculations.kgToDisplay(it.weightKg, unit)
                        }
                        val rollingPoints = state.weightTrend.rollingSeries.map { (dk, kg) ->
                            dk to FitnessCalculations.kgToDisplay(kg, unit)
                        }
                        val goalDisplay = state.profile?.goalWeightKg?.let { FitnessCalculations.kgToDisplay(it, unit) }

                        FitTrackCard {
                            Text("Weight & 7-Day Rolling Average", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TrendMetric(
                                    "7-Day Avg",
                                    state.weightTrend.rolling7DayAvgKg?.let { FitnessCalculations.formatWeight(it, unit) } ?: "--"
                                )
                                TrendMetric(
                                    "14-Day Rate",
                                    state.weightTrend.trend14DayKgPerWeek?.let { FitnessCalculations.formatSignedWeightRate(it, unit) } ?: "--"
                                )
                                TrendMetric(
                                    "30-Day Rate",
                                    state.weightTrend.trend30DayKgPerWeek?.let { FitnessCalculations.formatSignedWeightRate(it, unit) } ?: "--"
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            WeightTrendLineChart(
                                dailyPoints = dailyPoints,
                                rollingAvgPoints = rollingPoints,
                                goalValue = goalDisplay,
                                unitLabel = unit.weightUnit
                            )
                        }
                    }

                    item {
                        FitTrackCard {
                            Text("7-Day Calorie Adherence", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Dashed amber line = ${state.profile?.calorieTarget ?: 2100} kcal target",
                                style = MaterialTheme.typography.bodySmall,
                                color = FitTrackThemeExt.colors.mutedText
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            MetricBarChart(
                                bars = state.weeklyCaloriesSeries.map { it.shortLabel to it.calories.toFloat() },
                                targetValue = (state.profile?.calorieTarget ?: 2100).toFloat(),
                                barColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    item {
                        FitTrackCard {
                            Text("7-Day Protein Adherence", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Target: ${state.profile?.proteinTargetG ?: 160} g/day",
                                style = MaterialTheme.typography.bodySmall,
                                color = FitTrackThemeExt.colors.mutedText
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            MetricBarChart(
                                bars = state.weeklyCaloriesSeries.map { it.shortLabel to it.proteinG.toFloat() },
                                targetValue = (state.profile?.proteinTargetG ?: 160).toFloat(),
                                barColor = FitTrackThemeExt.colors.success
                            )
                        }
                    }

                    item {
                        FitTrackCard {
                            Text("30-Day Volume by Muscle Group", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.volumeByMuscleKg.isEmpty()) {
                                Text(
                                    "Complete strength workouts to populate muscle volume analytics.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FitTrackThemeExt.colors.mutedText
                                )
                            } else {
                                state.volumeByMuscleKg.entries.sortedByDescending { it.value }.forEach { (mg, volKg) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(MuscleGroup.fromNameSafe(mg).label, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            FitnessCalculations.formatVolume(volKg, unit),
                                            style = MonoMetricSmall,
                                            color = FitTrackThemeExt.colors.volumeAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> { // Body Measurements
                    item {
                        SectionHeader(
                            title = "Body Measurements",
                            subtitle = "Track recomposition beyond the scale",
                            trailing = {
                                FilledTonalButton(
                                    onClick = { showMeasurementDialog = true },
                                    modifier = Modifier.testTag("log_measurement_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Log")
                                }
                            }
                        )
                    }
                    if (state.measurements.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.Straighten,
                                title = "No body measurements yet",
                                subtitle = "Record waist, chest, arms, thighs, hips, and neck to track true body composition shifts.",
                                actionLabel = "Log First Measurement",
                                onAction = { showMeasurementDialog = true }
                            )
                        }
                    } else {
                        val latest = state.measurements.first()
                        val previous = state.measurements.getOrNull(1)
                        item {
                            FitTrackCard {
                                Text("Latest Check-In (${latest.dateKey})", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(10.dp))
                                MeasurementDeltaRow("Waist", latest.waistCm, previous?.waistCm, unit)
                                MeasurementDeltaRow("Chest", latest.chestCm, previous?.chestCm, unit)
                                MeasurementDeltaRow("Arms", latest.armsCm, previous?.armsCm, unit)
                                MeasurementDeltaRow("Thighs", latest.thighsCm, previous?.thighsCm, unit)
                                MeasurementDeltaRow("Hips", latest.hipsCm, previous?.hipsCm, unit)
                                MeasurementDeltaRow("Neck", latest.neckCm, previous?.neckCm, unit)
                            }
                        }
                        items(state.measurements, key = { it.id }) { m ->
                            FitTrackCard {
                                Text(m.dateKey, style = MaterialTheme.typography.titleSmall)
                                val summary = buildList {
                                    m.waistCm?.let { add("Waist ${FitnessCalculations.formatLength(it, unit)}") }
                                    m.chestCm?.let { add("Chest ${FitnessCalculations.formatLength(it, unit)}") }
                                    m.armsCm?.let { add("Arms ${FitnessCalculations.formatLength(it, unit)}") }
                                    m.thighsCm?.let { add("Thighs ${FitnessCalculations.formatLength(it, unit)}") }
                                    m.hipsCm?.let { add("Hips ${FitnessCalculations.formatLength(it, unit)}") }
                                    m.neckCm?.let { add("Neck ${FitnessCalculations.formatLength(it, unit)}") }
                                }.joinToString(" • ")
                                Text(summary, style = MaterialTheme.typography.bodySmall, color = FitTrackThemeExt.colors.mutedText)
                            }
                        }
                    }
                }

                2 -> { // Private Progress Photos
                    item {
                        FitTrackCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = FitTrackThemeExt.colors.success)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Private Progress Photos", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Stored privately on-device with zero facial recognition. Delete anytime.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FitTrackThemeExt.colors.mutedText
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PhotoType.entries.forEach { type ->
                                    FilterChip(
                                        selected = selectedPhotoType == type,
                                        onClick = { selectedPhotoType = type },
                                        label = { Text(type.label) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("add_progress_photo_button")
                                ) {
                                    Icon(Icons.Rounded.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add ${selectedPhotoType.label}")
                                }
                                if (state.progressPhotos.size >= 2) {
                                    OutlinedButton(
                                        onClick = { showCompareDialog = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Compare, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Compare")
                                    }
                                }
                            }
                        }
                    }

                    val filteredPhotos = state.progressPhotos.filter {
                        it.photoType.equals(selectedPhotoType.name, ignoreCase = true)
                    }
                    if (filteredPhotos.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.AddAPhoto,
                                title = "No ${selectedPhotoType.label.lowercase()} photos yet",
                                subtitle = "Pick a photo from your device via Android Photo Picker (zero storage permission required)."
                            )
                        }
                    } else {
                        items(filteredPhotos, key = { it.id }) { photo ->
                            FitTrackCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${PhotoType.fromNameSafe(photo.photoType).label} • ${photo.dateKey}", style = MaterialTheme.typography.titleSmall)
                                    IconButton(onClick = { onDeleteProgressPhoto(photo) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete photo", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                AsyncImage(
                                    model = File(photo.localFilePath),
                                    contentDescription = "Progress photo from ${photo.dateKey}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                )
                            }
                        }
                    }
                }

                3 -> { // PRs & Data-Backed Achievements
                    item {
                        SectionHeader(title = "Verified Personal Records", subtitle = "Calculated strictly from completed working sets")
                    }
                    if (state.personalRecords.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.EmojiEvents,
                                title = "No Personal Records logged yet",
                                subtitle = "Complete strength workouts to automatically track heaviest weights, Epley 1RM, and best set volume."
                            )
                        }
                    } else {
                        items(state.personalRecords, key = { it.exerciseId }) { pr ->
                            FitTrackCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pr.exerciseName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "Heaviest: ${FitnessCalculations.formatWeight(pr.maxWeightKg, unit)} × ${pr.bestRepsAtMaxWeight}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FitTrackThemeExt.colors.mutedText
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Est. 1RM", style = MaterialTheme.typography.labelSmall, color = FitTrackThemeExt.colors.mutedText)
                                        Text(
                                            FitnessCalculations.formatWeight(pr.bestEstimated1RmKg, unit),
                                            style = MonoMetricMedium,
                                            color = FitTrackThemeExt.colors.success
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        SectionHeader(title = "Achievements", subtitle = "Earned strictly from your real recorded logs")
                    }
                    items(state.achievements, key = { it.id }) { badge ->
                        FitTrackCard(
                            borderColor = if (badge.unlocked) FitTrackThemeExt.colors.success.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Rounded.WorkspacePremium,
                                        contentDescription = null,
                                        tint = if (badge.unlocked) FitTrackThemeExt.colors.success else FitTrackThemeExt.colors.mutedText,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            badge.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (badge.unlocked) MaterialTheme.colorScheme.onSurface else FitTrackThemeExt.colors.mutedText
                                        )
                                        Text(badge.description, style = MaterialTheme.typography.bodySmall, color = FitTrackThemeExt.colors.mutedText)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (badge.unlocked) FitTrackThemeExt.colors.successContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (badge.unlocked) "Unlocked" else badge.progressText,
                                        style = MonoMetricSmall,
                                        color = if (badge.unlocked) FitTrackThemeExt.colors.success else FitTrackThemeExt.colors.mutedText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showMeasurementDialog) {
        LogMeasurementDialog(
            unitSystem = unit,
            onDismiss = { showMeasurementDialog = false },
            onSave = { w, c, a, t, n, h ->
                onLogMeasurements(w, c, a, t, n, h)
                showMeasurementDialog = false
            }
        )
    }

    if (showCompareDialog && state.progressPhotos.size >= 2) {
        val newest = state.progressPhotos.first()
        val oldest = state.progressPhotos.last()
        AlertDialog(
            onDismissRequest = { showCompareDialog = false },
            title = { Text("Progress Comparison") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(oldest.dateKey, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        AsyncImage(
                            model = File(oldest.localFilePath),
                            contentDescription = "Earlier progress photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                                .clip(MaterialTheme.shapes.small)
                        )
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(newest.dateKey, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        AsyncImage(
                            model = File(newest.localFilePath),
                            contentDescription = "Latest progress photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                                .clip(MaterialTheme.shapes.small)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCompareDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun TrendMetric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = FitTrackThemeExt.colors.mutedText)
        Text(value, style = MonoMetricSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MeasurementDeltaRow(label: String, currentCm: Double?, prevCm: Double?, unit: UnitSystem) {
    if (currentCm == null) return
    val deltaText = if (prevCm != null) {
        val diff = FitnessCalculations.cmToDisplay(currentCm - prevCm, unit)
        String.format(" (%+.1f %s)", diff, unit.lengthUnit)
    } else ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FitTrackThemeExt.colors.mutedText)
        Text(
            text = "${FitnessCalculations.formatLength(currentCm, unit)}$deltaText",
            style = MonoMetricSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LogMeasurementDialog(
    unitSystem: UnitSystem,
    onDismiss: () -> Unit,
    onSave: (Double?, Double?, Double?, Double?, Double?, Double?) -> Unit
) {
    var waist by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var arms by remember { mutableStateOf("") }
    var thighs by remember { mutableStateOf("") }
    var neck by remember { mutableStateOf("") }
    var hips by remember { mutableStateOf("") }

    fun parseCm(raw: String): Double? = raw.toDoubleOrNull()?.let { FitnessCalculations.displayToCm(it, unitSystem) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Measurements (${unitSystem.lengthUnit})") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MeasureField("Waist", waist, { waist = it }, Modifier.weight(1f))
                    MeasureField("Chest", chest, { chest = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MeasureField("Arms", arms, { arms = it }, Modifier.weight(1f))
                    MeasureField("Thighs", thighs, { thighs = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MeasureField("Hips", hips, { hips = it }, Modifier.weight(1f))
                    MeasureField("Neck", neck, { neck = it }, Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(parseCm(waist), parseCm(chest), parseCm(arms), parseCm(thighs), parseCm(neck), parseCm(hips))
                }
            ) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MeasureField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }.take(5)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}
