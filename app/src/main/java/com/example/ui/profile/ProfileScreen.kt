package com.example.ui.profile

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.UserProfileEntity
import com.example.data.remote.AppUpdateStatus
import com.example.domain.calc.FitnessCalculations
import com.example.domain.model.ActivityLevel
import com.example.domain.model.Gender
import com.example.domain.model.GoalType
import com.example.domain.model.UnitSystem
import com.example.ui.components.EstimationDisclaimerBanner
import com.example.ui.components.FitTrackCard
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.FitTrackThemeExt
import com.example.ui.theme.MonoMetricSmall
import com.example.viewmodel.FitTrackUiState

@Composable
fun ProfileScreen(
    state: FitTrackUiState,
    appUpdateStatus: AppUpdateStatus = AppUpdateStatus.Idle,
    bundledUpdateSourceConfigured: Boolean = false,
    onSaveGitHubRepo: (String) -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onUpdateProfile: (UserProfileEntity, Boolean) -> Unit,
    onUnitSystemChange: (UnitSystem) -> Unit,
    onUpdateReminders: (Boolean?, Boolean?, Boolean?, Boolean?, Boolean?) -> Unit,
    onManualSync: () -> Unit,
    onExportData: ((String) -> Unit) -> Unit,
    onClearWorkouts: () -> Unit,
    onClearFoods: () -> Unit,
    onDeleteAccountData: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val profile = state.profile
    val unit = state.preferences.unitSystem

    var showEditGoalDialog by remember { mutableStateOf(false) }
    var showFormulasDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var githubRepoInput by remember { mutableStateOf(state.preferences.githubRepo) }

    LaunchedEffect(state.preferences.githubRepo) {
        if (state.preferences.githubRepo.isNotBlank() && githubRepoInput.isBlank()) {
            githubRepoInput = state.preferences.githubRepo
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Profile & Settings", style = MaterialTheme.typography.headlineMedium)
                SyncStatusBadge(syncState = state.syncState, onClick = onManualSync)
            }
        }

        // Athlete Summary & Active Targets
        if (profile != null) {
            item {
                val bmr = FitnessCalculations.calculateBmr(
                    profile.currentWeightKg, profile.heightCm, profile.age, Gender.fromNameSafe(profile.gender)
                )
                val tdee = FitnessCalculations.calculateTdee(bmr, ActivityLevel.fromNameSafe(profile.activityLevel))

                FitTrackCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.name, style = MaterialTheme.typography.titleLarge)
                            if (profile.email.isNotBlank()) {
                                Text(profile.email, style = MaterialTheme.typography.bodySmall, color = FitTrackThemeExt.colors.mutedText)
                            }
                        }
                        OutlinedButton(
                            onClick = { showEditGoalDialog = true },
                            modifier = Modifier.testTag("edit_goals_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Goals")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileInfoRow("Goal", "${GoalType.fromNameSafe(profile.goalType).label} (${FitnessCalculations.formatWeight(profile.goalWeightKg, unit)})")
                    ProfileInfoRow("Daily Target", "${profile.calorieTarget} kcal • ${profile.proteinTargetG}g protein")
                    ProfileInfoRow("BMR / Est. TDEE", "$bmr kcal / $tdee kcal")
                    ProfileInfoRow("Training / Cardio", "${profile.trainingDaysPerWeek} days/wk • ${profile.cardioTargetMinPerDay} min/day")
                    ProfileInfoRow("Goal History", "${state.goalHistory.size} recorded goal milestone(s)")
                }
            }
        }

        // Units & Formula Transparency
        item {
            FitTrackCard {
                Text("Units & Display", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UnitSystem.entries.forEach { sys ->
                        FilterChip(
                            selected = unit == sys,
                            onClick = { onUnitSystemChange(sys) },
                            label = { Text(sys.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showFormulasDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("formulas_reference_button")
                ) {
                    Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Transparent Formulas & Calculations")
                }
            }
        }

        // Notification Preferences
        item {
            val prefs = state.preferences
            FitTrackCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reminder Preferences", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                PreferenceToggleRow("Workout Reminders", prefs.workoutReminderEnabled) {
                    onUpdateReminders(it, null, null, null, null)
                }
                PreferenceToggleRow("Meal Logging Reminders", prefs.mealReminderEnabled) {
                    onUpdateReminders(null, it, null, null, null)
                }
                PreferenceToggleRow("Morning Weigh-In Reminder", prefs.weightReminderEnabled) {
                    onUpdateReminders(null, null, it, null, null)
                }
                PreferenceToggleRow("Weekly Progress Summary", prefs.weeklyReportEnabled) {
                    onUpdateReminders(null, null, null, it, null)
                }
                PreferenceToggleRow("Personal Record Alerts", prefs.prAlertsEnabled) {
                    onUpdateReminders(null, null, null, null, it)
                }
            }
        }

        // Offline Sync & Data Privacy Controls
        item {
            FitTrackCard {
                Text("Cloud Sync & Privacy", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onManualSync,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Now (${state.pendingSyncCount} pending)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        onExportData { json ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "FitTrack Data Export")
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export FitTrack JSON"))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_data_button")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export My Data (JSON)")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onClearWorkouts,
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear Workouts", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = onClearFoods,
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear Food Logs", style = MaterialTheme.typography.labelSmall) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteAccountDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_account_button")
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete All Data & Account")
                }
            }
        }

        // App Updates
        item {
            val canCheckUpdates = bundledUpdateSourceConfigured || state.preferences.githubRepo.isNotBlank()
            FitTrackCard(modifier = Modifier.testTag("app_updates_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("App Updates", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MonoMetricSmall,
                        color = FitTrackThemeExt.colors.mutedText
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (bundledUpdateSourceConfigured) {
                    Text(
                        text = "FitTrack checks automatically for new releases whenever you open the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FitTrackThemeExt.colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onCheckForUpdates,
                        enabled = appUpdateStatus !is AppUpdateStatus.Checking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("check_updates_button")
                    ) {
                        if (appUpdateStatus is AppUpdateStatus.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Check for Updates")
                        }
                    }
                } else {
                    Text(
                        text = "Local dev build detected. Releases built via GitHub Actions configure updates automatically—or link a repo below to test.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FitTrackThemeExt.colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = githubRepoInput,
                        onValueChange = { githubRepoInput = it },
                        label = { Text("GitHub Repository (owner/repo)") },
                        placeholder = { Text("e.g. username/fittrack") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_repo_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSaveGitHubRepo(githubRepoInput) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_github_repo_button")
                        ) {
                            Text("Save & Check")
                        }
                        OutlinedButton(
                            onClick = onCheckForUpdates,
                            enabled = canCheckUpdates && appUpdateStatus !is AppUpdateStatus.Checking,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("check_updates_button")
                        ) {
                            if (appUpdateStatus is AppUpdateStatus.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Check Now")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                when (appUpdateStatus) {
                    is AppUpdateStatus.UpdateAvailable -> {
                        val info = appUpdateStatus.info
                        Text(
                            text = "New version available: ${info.tagName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = FitTrackThemeExt.colors.success
                        )
                        if (info.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = info.releaseNotes.take(240),
                                style = MaterialTheme.typography.bodySmall,
                                color = FitTrackThemeExt.colors.mutedText
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.preferredDownloadUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_download_apk_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Update (${info.tagName})")
                        }
                    }
                    is AppUpdateStatus.UpToDate -> {
                        Text(
                            text = "You're on the latest version (v${appUpdateStatus.currentVersion}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = FitTrackThemeExt.colors.success
                        )
                    }
                    is AppUpdateStatus.Error -> {
                        Text(
                            text = appUpdateStatus.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = FitTrackThemeExt.colors.warning
                        )
                    }
                    AppUpdateStatus.Unconfigured -> {
                        Text(
                            text = "Build via GitHub Actions to embed automatic updates for all users.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FitTrackThemeExt.colors.mutedText
                        )
                    }
                    AppUpdateStatus.Checking -> {
                        Text(
                            text = "Checking for updates…",
                            style = MaterialTheme.typography.bodySmall,
                            color = FitTrackThemeExt.colors.mutedText
                        )
                    }
                    AppUpdateStatus.Idle -> Unit
                }
            }
        }

        // Sign Out
        item {
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }

        item {
            EstimationDisclaimerBanner(
                text = "FitTrack is a deterministic fitness tracking tool and never diagnoses medical conditions or replaces professional healthcare advice."
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showEditGoalDialog && profile != null) {
        EditGoalDialog(
            profile = profile,
            onDismiss = { showEditGoalDialog = false },
            onSave = { updated ->
                onUpdateProfile(updated, true)
                showEditGoalDialog = false
            }
        )
    }

    if (showFormulasDialog) {
        AlertDialog(
            onDismissRequest = { showFormulasDialog = false },
            title = { Text("Transparent Formulas") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FormulaBlock("BMR (Mifflin-St Jeor)", "10×weight(kg) + 6.25×height(cm) − 5×age + offset (+5 M, −161 F, −78 Neutral)")
                    FormulaBlock("Estimated TDEE", "BMR × Activity Multiplier (1.20 Sedentary → 1.90 Athlete)")
                    FormulaBlock("Energy-Equivalent Weight", "Estimated Energy Balance (kcal) ÷ 7,700 kcal/kg. Always treated as an estimate alongside 7-day rolling weight trends.")
                    FormulaBlock("Daily Score (0–100)", "Nutrition Adherence (25) + Protein Target (20) + Strength Workout (25) + Cardio Target (15) + Logging Consistency (15)")
                    FormulaBlock("Workout Volume & 1RM", "Set Volume = Weight × Reps. Estimated 1RM (Epley) = Weight × (1 + Reps ÷ 30).")
                    FormulaBlock("Cardio Estimation", "Adjusted MET(activity, speed, incline, level) × Body Weight(kg) × Duration(hours).")
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormulasDialog = false }) { Text("Close") }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete All FitTrack Data?") },
            text = {
                Text("This permanently deletes your local database, private progress photos, and cloud Firestore records, then signs you out.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccountData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Permanently") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
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

@Composable
private fun PreferenceToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FormulaBlock(title: String, formula: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(formula, style = MaterialTheme.typography.bodySmall, color = FitTrackThemeExt.colors.mutedText)
    }
}

@Composable
private fun EditGoalDialog(
    profile: UserProfileEntity,
    onDismiss: () -> Unit,
    onSave: (UserProfileEntity) -> Unit
) {
    var goalType by remember { mutableStateOf(GoalType.fromNameSafe(profile.goalType)) }
    var goalWeightText by remember { mutableStateOf(String.format("%.1f", profile.goalWeightKg)) }
    var calorieText by remember { mutableStateOf(profile.calorieTarget.toString()) }
    var proteinText by remember { mutableStateOf(profile.proteinTargetG.toString()) }
    var cardioText by remember { mutableStateOf(profile.cardioTargetMinPerDay.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Goal & Targets") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalType.entries.forEach { g ->
                        FilterChip(
                            selected = goalType == g,
                            onClick = {
                                goalType = g
                                val recalc = FitnessCalculations.calculateInitialTargets(
                                    weightKg = profile.currentWeightKg,
                                    goalWeightKg = goalWeightText.toDoubleOrNull() ?: profile.goalWeightKg,
                                    heightCm = profile.heightCm,
                                    age = profile.age,
                                    gender = Gender.fromNameSafe(profile.gender),
                                    activityLevel = ActivityLevel.fromNameSafe(profile.activityLevel),
                                    goalType = g
                                )
                                calorieText = recalc.calorieTarget.toString()
                                proteinText = recalc.proteinTargetG.toString()
                            },
                            label = { Text(g.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = goalWeightText,
                    onValueChange = { goalWeightText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("Goal Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calorieText,
                    onValueChange = { calorieText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Daily Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = proteinText,
                    onValueChange = { proteinText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Daily Protein (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cardioText,
                    onValueChange = { cardioText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Daily Cardio Target (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val gw = goalWeightText.toDoubleOrNull()?.coerceIn(25.0, 350.0) ?: profile.goalWeightKg
                    val cal = calorieText.toIntOrNull()?.coerceIn(1000, 8000) ?: profile.calorieTarget
                    val pro = proteinText.toIntOrNull()?.coerceIn(30, 450) ?: profile.proteinTargetG
                    val car = cardioText.toIntOrNull()?.coerceIn(0, 300) ?: profile.cardioTargetMinPerDay
                    onSave(
                        profile.copy(
                            goalType = goalType.name,
                            goalWeightKg = gw,
                            calorieTarget = cal,
                            proteinTargetG = pro,
                            cardioTargetMinPerDay = car
                        )
                    )
                }
            ) { Text("Save Goal") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
