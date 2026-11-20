package com.example.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.UserProfileEntity
import com.example.domain.calc.FitnessCalculations
import com.example.domain.model.ActivityLevel
import com.example.domain.model.Gender
import com.example.domain.model.GoalType
import com.example.domain.model.UnitSystem
import com.example.domain.model.WorkoutSplit
import com.example.ui.components.EstimationDisclaimerBanner
import com.example.ui.components.FitTrackCard
import com.example.ui.theme.FitTrackThemeExt
import com.example.ui.theme.MonoMetricMedium
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    userId: String,
    defaultName: String,
    defaultEmail: String,
    unitSystem: UnitSystem,
    onUnitSystemChanged: (UnitSystem) -> Unit,
    onComplete: (UserProfileEntity) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    var name by remember { mutableStateOf(defaultName.ifBlank { "Athlete" }) }
    var ageText by remember { mutableStateOf("26") }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var heightText by remember { mutableStateOf("175") } // cm
    var currentWeightText by remember { mutableStateOf("77.0") } // kg
    var goalWeightText by remember { mutableStateOf("70.0") } // kg
    var goalType by remember { mutableStateOf(GoalType.CUT) }
    var activityLevel by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var trainingDays by remember { mutableIntStateOf(4) }
    var split by remember { mutableStateOf(WorkoutSplit.PPL) }
    var cardioTargetMin by remember { mutableIntStateOf(20) }
    var rateSlider by remember { mutableFloatStateOf(0.45f) }

    val parsedAge = ageText.toIntOrNull()?.coerceIn(14, 100) ?: 26
    val parsedHeightCm = heightText.toDoubleOrNull()?.coerceIn(120.0, 230.0) ?: 175.0
    val parsedWeightKg = currentWeightText.toDoubleOrNull()?.coerceIn(35.0, 250.0) ?: 77.0
    val parsedGoalWeightKg = goalWeightText.toDoubleOrNull()?.coerceIn(35.0, 250.0) ?: 70.0

    val computed = remember(
        parsedWeightKg, parsedGoalWeightKg, parsedHeightCm, parsedAge, gender, activityLevel, goalType, rateSlider
    ) {
        FitnessCalculations.calculateInitialTargets(
            weightKg = parsedWeightKg,
            goalWeightKg = parsedGoalWeightKg,
            heightCm = parsedHeightCm,
            age = parsedAge,
            gender = gender,
            activityLevel = activityLevel,
            goalType = goalType,
            desiredRateKgPerWeek = if (goalType == GoalType.MAINTAIN) 0.0 else rateSlider.toDouble()
        )
    }

    var customCaloriesText by remember(computed.calorieTarget) { mutableStateOf(computed.calorieTarget.toString()) }
    var customProteinText by remember(computed.proteinTargetG) { mutableStateOf(computed.proteinTargetG.toString()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Progress header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FitTrack Setup",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Step ${step + 1} of $totalSteps",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (step + 1) / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (step) {
                    0 -> {
                        Text(
                            text = "Your Baseline Stats",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "Used only for transparent Mifflin-St Jeor BMR & TDEE formulas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FitTrackThemeExt.colors.mutedText
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnitSystem.entries.forEach { sys ->
                                FilterChip(
                                    selected = unitSystem == sys,
                                    onClick = { onUnitSystemChanged(sys) },
                                    label = { Text(sys.label) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_name_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = ageText,
                                onValueChange = { ageText = it.filter(Char::isDigit).take(3) },
                                label = { Text("Age (yrs)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = heightText,
                                onValueChange = { heightText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                                label = { Text("Height (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text("Sex (for BMR constant)", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Gender.entries.forEach { g ->
                                FilterChip(
                                    selected = gender == g,
                                    onClick = { gender = g },
                                    label = { Text(g.label) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = currentWeightText,
                                onValueChange = { currentWeightText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                                label = { Text("Current Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = goalWeightText,
                                onValueChange = { goalWeightText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                                label = { Text("Goal Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    1 -> {
                        Text("Primary Fitness Goal", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            text = "Determines your daily energy balance target and weekly coaching logic.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FitTrackThemeExt.colors.mutedText
                        )

                        GoalType.entries.forEach { g ->
                            val selected = goalType == g
                            SelectableOptionCard(
                                title = g.label,
                                subtitle = g.subtitle,
                                selected = selected,
                                onClick = {
                                    goalType = g
                                    rateSlider = when (g) {
                                        GoalType.CUT -> 0.45f
                                        GoalType.BULK -> 0.25f
                                        GoalType.MAINTAIN -> 0f
                                    }
                                }
                            )
                        }

                        if (goalType != GoalType.MAINTAIN) {
                            FitTrackCard {
                                Text(
                                    text = "Target Weekly Pace: ${String.format("%.2f", rateSlider)} kg/week",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (goalType == GoalType.CUT) "Recommended sustainable fat-loss pace: 0.25 – 0.60 kg/wk"
                                    else "Recommended lean gain pace: 0.15 – 0.35 kg/wk",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FitTrackThemeExt.colors.mutedText
                                )
                                Slider(
                                    value = rateSlider,
                                    onValueChange = { rateSlider = (it * 20).roundToInt() / 20f },
                                    valueRange = if (goalType == GoalType.CUT) 0.20f..0.80f else 0.10f..0.50f
                                )
                            }
                        }
                    }

                    2 -> {
                        Text("Activity & Training Split", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            text = "We pre-load starter workout templates matching your split so you can train immediately.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FitTrackThemeExt.colors.mutedText
                        )

                        Text("Daily Activity Level", style = MaterialTheme.typography.titleSmall)
                        ActivityLevel.entries.forEach { lvl ->
                            SelectableOptionCard(
                                title = "${lvl.label} (×${lvl.multiplier})",
                                subtitle = lvl.subtitle,
                                selected = activityLevel == lvl,
                                onClick = { activityLevel = lvl }
                            )
                        }

                        FitTrackCard {
                            Text("Strength Training Days / Week: $trainingDays", style = MaterialTheme.typography.titleSmall)
                            Slider(
                                value = trainingDays.toFloat(),
                                onValueChange = { trainingDays = it.roundToInt() },
                                valueRange = 1f..7f,
                                steps = 5
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Daily Cardio Target: $cardioTargetMin min", style = MaterialTheme.typography.titleSmall)
                            Slider(
                                value = cardioTargetMin.toFloat(),
                                onValueChange = { cardioTargetMin = (it / 5f).roundToInt() * 5 },
                                valueRange = 0f..60f,
                                steps = 11
                            )
                        }

                        Text("Preferred Workout Split", style = MaterialTheme.typography.titleSmall)
                        WorkoutSplit.entries.forEach { sp ->
                            SelectableOptionCard(
                                title = sp.label,
                                subtitle = sp.subtitle,
                                selected = split == sp,
                                onClick = { split = sp }
                            )
                        }
                    }

                    3 -> {
                        Text("Your Transparent Targets", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            text = "Every number is calculated transparently using Mifflin-St Jeor BMR and adjustable anytime.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FitTrackThemeExt.colors.mutedText
                        )

                        FitTrackCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Estimated BMR", style = MaterialTheme.typography.labelMedium, color = FitTrackThemeExt.colors.mutedText)
                                    Text("${computed.bmr} kcal", style = MonoMetricMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Estimated TDEE", style = MaterialTheme.typography.labelMedium, color = FitTrackThemeExt.colors.mutedText)
                                    Text("${computed.tdee} kcal", style = MonoMetricMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Formula: BMR = 10×wt + 6.25×ht − 5×age + offset; TDEE = BMR × ${activityLevel.multiplier}. " +
                                    "Daily energy delta: ${if (computed.dailyDeltaKcal >= 0) "+" else ""}${computed.dailyDeltaKcal} kcal/day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FitTrackThemeExt.colors.mutedText
                            )
                        }

                        FitTrackCard {
                            Text("Customize Initial Daily Targets", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = customCaloriesText,
                                    onValueChange = { customCaloriesText = it.filter(Char::isDigit).take(4) },
                                    label = { Text("Calories (kcal)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customProteinText,
                                    onValueChange = { customProteinText = it.filter(Char::isDigit).take(3) },
                                    label = { Text("Protein (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Suggested macros: ${computed.proteinTargetG}g P • ${computed.carbsTargetG}g C • ${computed.fatTargetG}g F • ${computed.fiberTargetG}g Fiber",
                                style = MaterialTheme.typography.bodySmall,
                                color = FitTrackThemeExt.colors.success
                            )
                        }

                        EstimationDisclaimerBanner(
                            text = "FitTrack calculations are mathematical estimates intended for fitness tracking—not medical certainty or clinical advice. Real-world scale weight fluctuates daily due to water, glycogen, and sodium; FitTrack tracks rolling weekly trends."
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("onboarding_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back")
                    }
                }

                Button(
                    onClick = {
                        if (step < totalSteps - 1) {
                            step++
                        } else {
                            val finalCal = customCaloriesText.toIntOrNull()?.coerceIn(1200, 6000) ?: computed.calorieTarget
                            val finalPro = customProteinText.toIntOrNull()?.coerceIn(40, 350) ?: computed.proteinTargetG
                            onComplete(
                                UserProfileEntity(
                                    userId = userId,
                                    name = name.trim().ifBlank { "Athlete" },
                                    email = defaultEmail,
                                    age = parsedAge,
                                    gender = gender.name,
                                    heightCm = parsedHeightCm,
                                    currentWeightKg = parsedWeightKg,
                                    startingWeightKg = parsedWeightKg,
                                    goalWeightKg = parsedGoalWeightKg,
                                    goalType = goalType.name,
                                    activityLevel = activityLevel.name,
                                    trainingDaysPerWeek = trainingDays,
                                    preferredSplit = split.name,
                                    targetRateKgPerWeek = if (goalType == GoalType.MAINTAIN) 0.0 else rateSlider.toDouble(),
                                    calorieTarget = finalCal,
                                    proteinTargetG = finalPro,
                                    cardioTargetMinPerDay = cardioTargetMin,
                                    onboardingCompleted = true
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(if (step > 0) 1.4f else 1f)
                        .height(52.dp)
                        .testTag("onboarding_next_button")
                ) {
                    Text(if (step < totalSteps - 1) "Continue" else "Launch FitTrack")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (step < totalSteps - 1) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Check,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = FitTrackThemeExt.colors.mutedText
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
