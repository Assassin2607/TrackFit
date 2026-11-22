package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BodyMeasurementEntity
import com.example.data.local.CardioSessionEntity
import com.example.data.local.ExerciseEntity
import com.example.data.local.FoodItemEntity
import com.example.data.local.FoodLogEntity
import com.example.data.local.ProgressPhotoEntity
import com.example.data.local.SavedMealEntity
import com.example.data.local.UserGoalEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WaterLogEntity
import com.example.data.local.WeightLogEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.local.WorkoutSetEntity
import com.example.data.local.WorkoutTemplateEntity
import com.example.data.prefs.PreferencesManager
import com.example.data.prefs.UserPreferences
import com.example.data.repository.FitTrackRepository
import com.example.data.seed.SavedMealItemSpec
import com.example.data.seed.SeedSerialization
import com.example.data.seed.TemplateExerciseSpec
import com.example.domain.calc.AchievementItem
import com.example.domain.calc.AdaptiveRecommendation
import com.example.domain.calc.AnalyticsEngines
import com.example.domain.calc.DailyScoreBreakdown
import com.example.domain.calc.EnergyBalanceEstimate
import com.example.domain.calc.FitnessCalculations
import com.example.domain.calc.HistoricalSetRecord
import com.example.domain.calc.NutritionCoachingCard
import com.example.domain.calc.WeightPoint
import com.example.domain.calc.WeightTrendSummary
import com.example.domain.model.ActivityLevel
import com.example.domain.model.CardioActivityType
import com.example.domain.model.Gender
import com.example.domain.model.GoalType
import com.example.domain.model.MealType
import com.example.domain.model.PhotoType
import com.example.domain.model.SyncState
import com.example.domain.model.UnitSystem
import com.example.BuildConfig
import com.example.data.remote.AppUpdateStatus
import com.example.data.remote.GitHubUpdateChecker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

data class ActiveSetDraft(
    val id: String = UUID.randomUUID().toString().take(8),
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rir: Int = 2,
    val warmup: Boolean = false,
    val completed: Boolean = false,
    val isPr: Boolean = false,
    val prLabel: String = ""
)

data class ActiveExerciseDraft(
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val restSeconds: Int,
    val previousSetsSummary: List<String>,
    val sets: List<ActiveSetDraft>
)

data class ActiveWorkoutState(
    val templateId: String?,
    val templateName: String,
    val startedAtEpochMs: Long,
    val exercises: List<ActiveExerciseDraft>,
    val notes: String = ""
)

data class RestTimerState(
    val isRunning: Boolean = false,
    val totalSeconds: Int = 90,
    val remainingSeconds: Int = 0
)

data class DailyCaloriePoint(
    val dateKey: String,
    val shortLabel: String,
    val calories: Int,
    val proteinG: Int
)

data class DailyWaterPoint(
    val dateKey: String,
    val shortLabel: String,
    val amountMl: Int
)

data class ExercisePrSummary(
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val maxWeightKg: Double,
    val bestEstimated1RmKg: Double,
    val bestSetVolumeKg: Double,
    val bestRepsAtMaxWeight: Int
)

data class FitTrackUiState(
    val isInitialized: Boolean = false,
    val userId: String = "",
    val profile: UserProfileEntity? = null,
    val goalHistory: List<UserGoalEntity> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val syncState: SyncState = SyncState.Synced,
    val pendingSyncCount: Int = 0,
    val selectedDateKey: String = FitnessCalculations.todayDateKey(),

    // Raw Collections
    val exercises: List<ExerciseEntity> = emptyList(),
    val templates: List<WorkoutTemplateEntity> = emptyList(),
    val workoutSessions: List<WorkoutSessionEntity> = emptyList(),
    val allWorkoutSets: List<WorkoutSetEntity> = emptyList(),
    val cardioSessions: List<CardioSessionEntity> = emptyList(),
    val foodItems: List<FoodItemEntity> = emptyList(),
    val savedMeals: List<SavedMealEntity> = emptyList(),
    val foodLogs: List<FoodLogEntity> = emptyList(),
    val waterLogs: List<WaterLogEntity> = emptyList(),
    val weightLogs: List<WeightLogEntity> = emptyList(),
    val measurements: List<BodyMeasurementEntity> = emptyList(),
    val progressPhotos: List<ProgressPhotoEntity> = emptyList(),

    // Computed Deterministic Metrics
    val todayCalories: Int = 0,
    val todayProteinG: Int = 0,
    val todayCarbsG: Int = 0,
    val todayFatG: Int = 0,
    val todayFiberG: Int = 0,
    val todayWaterMl: Int = 0,
    val waterTargetMl: Int = 2625,
    val isAutoWaterTarget: Boolean = true,
    val weeklyWaterSeries: List<DailyWaterPoint> = emptyList(),
    val targetCarbsG: Int = 220,
    val targetFatG: Int = 65,
    val targetFiberG: Int = 30,
    val todayCardioMinutes: Int = 0,
    val todayWorkoutCompleted: Boolean = false,
    val todayWorkoutName: String? = null,
    val workoutsThisWeek: Int = 0,
    val dailyVolumeKg: Double = 0.0,
    val weeklyVolumeKg: Double = 0.0,
    val monthlyVolumeKg: Double = 0.0,
    val volumeByMuscleKg: Map<String, Double> = emptyMap(),
    val dailyScore: DailyScoreBreakdown = AnalyticsEngines.calculateDailyScore(0, 2100, 0, 160, false, 0, 4, 0, 20, false, false, 0),
    val energyBalance: EnergyBalanceEstimate = EnergyBalanceEstimate(2400, 0, 0, 0, 2400, -2400, -0.31),
    val weightTrend: WeightTrendSummary = WeightTrendSummary(null, null, null, null, null, emptyList()),
    val goalProgressPercent: Int = 0,
    val weeklyCaloriesSeries: List<DailyCaloriePoint> = emptyList(),
    val weeklyCalorieTarget: Int = 14700,
    val weeklyCalorieConsumed: Int = 0,
    val nutritionCoaching: NutritionCoachingCard? = null,
    val adaptiveRecommendation: AdaptiveRecommendation = AdaptiveRecommendation(
        title = "Building Your Baseline Trend",
        summary = "Log weigh-ins across at least 4–7 days to unlock weekly trend adjustments.",
        suggestion = "Stay steady on your current nutrition and training schedule.",
        onTrack = true
    ),
    val personalRecords: List<ExercisePrSummary> = emptyList(),
    val achievements: List<AchievementItem> = emptyList(),
    val activeStreakDays: Int = 0
)

class FitTrackViewModel(
    private val repository: FitTrackRepository,
    private val preferencesManager: PreferencesManager,
    private val currentUserId: String,
    val authDisplayName: String,
    val authEmail: String,
    val defaultGitHubRepo: String = ""
) : ViewModel() {

    private val _selectedDateKey = MutableStateFlow(FitnessCalculations.todayDateKey())
    private val _isInitialized = MutableStateFlow(false)
    private val _activeWorkout = MutableStateFlow<ActiveWorkoutState?>(null)
    val activeWorkout: StateFlow<ActiveWorkoutState?> = _activeWorkout.asStateFlow()

    private val _restTimer = MutableStateFlow(RestTimerState())
    val restTimer: StateFlow<RestTimerState> = _restTimer.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _appUpdateStatus = MutableStateFlow<AppUpdateStatus>(AppUpdateStatus.Idle)
    val appUpdateStatus: StateFlow<AppUpdateStatus> = _appUpdateStatus.asStateFlow()

    private val _updateBannerDismissed = MutableStateFlow(false)
    val updateBannerDismissed: StateFlow<Boolean> = _updateBannerDismissed.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeUserSession(currentUserId)
            _isInitialized.value = true
            val effectiveRepo = resolveEffectiveGitHubRepo()
            if (effectiveRepo.isNotBlank()) {
                checkForAppUpdates(manual = false, repoOverride = effectiveRepo)
            } else {
                _appUpdateStatus.value = AppUpdateStatus.Unconfigured
            }
        }
    }

    private suspend fun resolveEffectiveGitHubRepo(): String {
        val userOverride = preferencesManager.preferencesFlow.first().githubRepo.trim()
        return userOverride.ifBlank { defaultGitHubRepo.trim() }
    }

    private data class CoreBundle(
        val initialized: Boolean,
        val profile: UserProfileEntity?,
        val goals: List<UserGoalEntity>,
        val prefs: UserPreferences,
        val syncState: SyncState,
        val pendingCount: Int,
        val selectedDate: String
    )

    private data class TrainingBundle(
        val exercises: List<ExerciseEntity>,
        val templates: List<WorkoutTemplateEntity>,
        val sessions: List<WorkoutSessionEntity>,
        val sets: List<WorkoutSetEntity>,
        val cardio: List<CardioSessionEntity>
    )

    private data class NutritionBundle(
        val foods: List<FoodItemEntity>,
        val meals: List<SavedMealEntity>,
        val logs: List<FoodLogEntity>,
        val waterLogs: List<WaterLogEntity>
    )

    private data class MetricsBundle(
        val weights: List<WeightLogEntity>,
        val measurements: List<BodyMeasurementEntity>,
        val photos: List<ProgressPhotoEntity>
    )

    private val coreFlow = combine(
        _isInitialized,
        repository.observeProfile(currentUserId),
        repository.observeGoals(currentUserId),
        preferencesManager.preferencesFlow,
        repository.syncState,
        repository.observePendingSyncCount(currentUserId),
        _selectedDateKey
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        CoreBundle(
            initialized = arr[0] as Boolean,
            profile = arr[1] as UserProfileEntity?,
            goals = arr[2] as List<UserGoalEntity>,
            prefs = arr[3] as UserPreferences,
            syncState = arr[4] as SyncState,
            pendingCount = arr[5] as Int,
            selectedDate = arr[6] as String
        )
    }

    private val trainingFlow = combine(
        repository.observeExercises(currentUserId),
        repository.observeTemplates(currentUserId),
        repository.observeWorkoutSessions(currentUserId),
        repository.observeAllWorkoutSets(currentUserId),
        repository.observeCardioSessions(currentUserId)
    ) { exercises, templates, sessions, sets, cardio ->
        TrainingBundle(exercises, templates, sessions, sets, cardio)
    }

    private val nutritionFlow = combine(
        repository.observeFoodItems(currentUserId),
        repository.observeSavedMeals(currentUserId),
        repository.observeFoodLogs(currentUserId),
        repository.observeWaterLogs(currentUserId)
    ) { foods, meals, logs, waterLogs ->
        NutritionBundle(foods, meals, logs, waterLogs)
    }

    private val metricsFlow = combine(
        repository.observeWeightLogs(currentUserId),
        repository.observeMeasurements(currentUserId),
        repository.observePhotos(currentUserId)
    ) { weights, measurements, photos ->
        MetricsBundle(weights, measurements, photos)
    }

    val uiState: StateFlow<FitTrackUiState> = combine(
        coreFlow,
        trainingFlow,
        nutritionFlow,
        metricsFlow
    ) { core, training, nutrition, metrics ->
        buildUiState(core, training, nutrition, metrics)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = FitTrackUiState(userId = currentUserId)
    )

    private fun buildUiState(
        core: CoreBundle,
        training: TrainingBundle,
        nutrition: NutritionBundle,
        metrics: MetricsBundle
    ): FitTrackUiState {
        val profile = core.profile
        val dateKey = core.selectedDate
        val todayKey = FitnessCalculations.todayDateKey()

        val dayFoodLogs = nutrition.logs.filter { it.dateKey == dateKey }
        val todayCal = dayFoodLogs.sumOf { it.calories }.roundToInt()
        val todayPro = dayFoodLogs.sumOf { it.protein }.roundToInt()
        val todayCarb = dayFoodLogs.sumOf { it.carbs }.roundToInt()
        val todayFat = dayFoodLogs.sumOf { it.fat }.roundToInt()
        val todayFiber = dayFoodLogs.sumOf { it.fiber }.roundToInt()

        val dayWaterLogs = nutrition.waterLogs.filter { it.dateKey == dateKey }
        val todayWaterMl = dayWaterLogs.sumOf { it.amountMl }

        val calorieTarget = profile?.calorieTarget ?: 2100
        val proteinTarget = profile?.proteinTargetG ?: 160
        val weightKg = profile?.currentWeightKg ?: 75.0
        val customWaterTarget = profile?.waterTargetMl ?: 0
        val isAutoWaterTarget = customWaterTarget !in 500..10000
        val waterTargetMl = FitnessCalculations.resolveWaterTargetMl(customWaterTarget, weightKg)
        val (targetCarbs, targetFat, targetFiber) = FitnessCalculations.calculateMacrosForTargets(
            calorieTarget = calorieTarget,
            proteinTargetG = proteinTarget,
            weightKg = weightKg
        )

        val todayWorkouts = training.sessions.filter { it.dateKey == dateKey }
        val todayCardio = training.cardio.filter { it.dateKey == dateKey }
        val todayCardioMin = todayCardio.sumOf { it.durationMinutes }
        val todayWorkoutKcal = todayWorkouts.sumOf { it.estimatedCalories }
        val todayCardioKcal = todayCardio.sumOf { it.caloriesEstimated }

        // Rolling 7-day and 30-day date keys
        val last7Keys = (0..6).map { FitnessCalculations.dateKeyDaysAgo(it) }.toSet()
        val last30Keys = (0..29).map { FitnessCalculations.dateKeyDaysAgo(it) }.toSet()

        val workoutsThisWeek = training.sessions.count { it.dateKey in last7Keys }
        val dailyVol = todayWorkouts.sumOf { it.totalVolumeKg }
        val weeklyVol = training.sessions.filter { it.dateKey in last7Keys }.sumOf { it.totalVolumeKg }
        val monthlyVol = training.sessions.filter { it.dateKey in last30Keys }.sumOf { it.totalVolumeKg }

        val sessionIdDateMap = training.sessions.associate { it.id to it.dateKey }
        val volumeByMuscle = training.sets
            .filter { !it.warmup && it.completed && (sessionIdDateMap[it.workoutSessionId] in last30Keys) }
            .groupBy { it.muscleGroup }
            .mapValues { (_, sets) -> sets.sumOf { FitnessCalculations.calculateSetVolume(it.weightKg, it.reps) } }

        // Compute logging streak (consecutive days with food, workout, cardio, or weight)
        val activeDates = buildSet {
            nutrition.logs.forEach { add(it.dateKey) }
            training.sessions.forEach { add(it.dateKey) }
            training.cardio.forEach { add(it.dateKey) }
            metrics.weights.forEach { add(it.dateKey) }
        }
        var streak = 0
        for (daysAgo in 0..90) {
            if (FitnessCalculations.dateKeyDaysAgo(daysAgo) in activeDates) streak++ else break
        }

        val recentWeightExists = metrics.weights.any { it.dateKey in last7Keys }

        val dailyScore = AnalyticsEngines.calculateDailyScore(
            caloriesConsumed = todayCal,
            calorieTarget = calorieTarget,
            proteinConsumedG = todayPro,
            proteinTargetG = proteinTarget,
            workoutCompletedToday = todayWorkouts.isNotEmpty(),
            workoutsThisWeek = workoutsThisWeek,
            plannedTrainingDaysPerWeek = profile?.trainingDaysPerWeek ?: 4,
            cardioMinutesToday = todayCardioMin,
            cardioTargetMinutes = profile?.cardioTargetMinPerDay ?: 20,
            hasLoggedFoodToday = dayFoodLogs.isNotEmpty(),
            hasLoggedWeightRecently = recentWeightExists,
            activeStreakDays = streak
        )

        val bmr = if (profile != null) {
            FitnessCalculations.calculateBmr(
                weightKg = profile.currentWeightKg,
                heightCm = profile.heightCm,
                age = profile.age,
                gender = Gender.fromNameSafe(profile.gender)
            )
        } else 1750

        val energyBalance = FitnessCalculations.calculateEnergyBalance(
            bmr = bmr,
            activityLevel = ActivityLevel.fromNameSafe(profile?.activityLevel),
            foodIntakeKcal = todayCal,
            workoutBurnKcal = todayWorkoutKcal,
            cardioBurnKcal = todayCardioKcal
        )

        val goalType = GoalType.fromNameSafe(profile?.goalType)
        val weightTrend = AnalyticsEngines.calculateWeightTrend(
            points = metrics.weights.map { WeightPoint(it.dateKey, it.weightKg, it.recordedAtEpochMs) },
            goalWeightKg = profile?.goalWeightKg,
            goalType = goalType
        )

        val startWeight = profile?.startingWeightKg ?: metrics.weights.firstOrNull()?.weightKg ?: weightKg
        val latestWeight = weightTrend.latestWeightKg ?: weightKg
        val goalProgress = if (profile != null) {
            FitnessCalculations.calculateGoalProgressPercent(startWeight, latestWeight, profile.goalWeightKg)
        } else 0

        // Weekly calorie budget & hydration series (oldest -> today)
        val weeklySeries = (6 downTo 0).map { daysAgo ->
            val dk = FitnessCalculations.dateKeyDaysAgo(daysAgo)
            val logsForDay = nutrition.logs.filter { it.dateKey == dk }
            DailyCaloriePoint(
                dateKey = dk,
                shortLabel = dk.takeLast(2),
                calories = logsForDay.sumOf { it.calories }.roundToInt(),
                proteinG = logsForDay.sumOf { it.protein }.roundToInt()
            )
        }
        val weeklyWaterSeries = (6 downTo 0).map { daysAgo ->
            val dk = FitnessCalculations.dateKeyDaysAgo(daysAgo)
            val waterForDay = nutrition.waterLogs.filter { it.dateKey == dk }.sumOf { it.amountMl }
            DailyWaterPoint(
                dateKey = dk,
                shortLabel = dk.takeLast(2),
                amountMl = waterForDay
            )
        }
        val weeklyConsumed = weeklySeries.sumOf { it.calories }
        val loggedDaysCount = weeklySeries.count { it.calories > 0 }
        val avgWeeklyCal = if (loggedDaysCount > 0) weeklyConsumed / loggedDaysCount else null

        val coaching = AnalyticsEngines.evaluateNutritionGuidance(
            goalType = goalType,
            caloriesConsumed = todayCal,
            calorieTarget = calorieTarget,
            proteinConsumedG = todayPro,
            proteinTargetG = proteinTarget,
            hasLoggedFood = dayFoodLogs.isNotEmpty()
        )

        val adaptive = AnalyticsEngines.generateAdaptiveRecommendation(
            goalType = goalType,
            targetRateKgPerWeek = profile?.targetRateKgPerWeek ?: 0.45,
            actualTrendKgPerWeek = weightTrend.trend14DayKgPerWeek ?: weightTrend.trend30DayKgPerWeek,
            averageWeeklyCalories = avgWeeklyCal,
            calorieTarget = calorieTarget,
            workoutsLast7Days = workoutsThisWeek,
            plannedWorkoutsPerWeek = profile?.trainingDaysPerWeek ?: 4
        )

        // Compute PR Summaries per exercise
        val prs = training.sets
            .filter { !it.warmup && it.completed && it.weightKg > 0 && it.reps > 0 }
            .groupBy { it.exerciseId }
            .map { (exId, exSets) ->
                val maxWt = exSets.maxOf { it.weightKg }
                val bestRepsAtMax = exSets.filter { it.weightKg >= maxWt - 0.1 }.maxOf { it.reps }
                val best1Rm = exSets.maxOf { FitnessCalculations.estimateOneRepMax(it.weightKg, it.reps) }
                val bestVol = exSets.maxOf { FitnessCalculations.calculateSetVolume(it.weightKg, it.reps) }
                val sample = exSets.first()
                ExercisePrSummary(
                    exerciseId = exId,
                    exerciseName = sample.exerciseName,
                    muscleGroup = sample.muscleGroup,
                    maxWeightKg = maxWt,
                    bestEstimated1RmKg = best1Rm,
                    bestSetVolumeKg = bestVol,
                    bestRepsAtMaxWeight = bestRepsAtMax
                )
            }
            .sortedByDescending { it.bestEstimated1RmKg }

        val heaviestBench = training.sets
            .filter { !it.warmup && it.completed && it.exerciseName.contains("Bench", ignoreCase = true) }
            .maxOfOrNull { it.weightKg } ?: 0.0

        val totalPrCount = training.sets.count { it.isPr }
        val achievements = AnalyticsEngines.evaluateAchievements(
            totalWorkouts = training.sessions.size,
            totalPrCount = totalPrCount,
            longestLoggingStreakDays = streak,
            heaviestBenchKg = heaviestBench,
            totalCardioSessions = training.cardio.size,
            totalFoodLogs = nutrition.logs.size
        )

        val effectiveSyncState = when {
            core.syncState is SyncState.Syncing -> SyncState.Syncing
            core.pendingCount > 0 -> SyncState.Pending(
                count = core.pendingCount,
                lastError = (core.syncState as? SyncState.Pending)?.lastError
            )
            else -> SyncState.Synced
        }

        return FitTrackUiState(
            isInitialized = core.initialized,
            userId = currentUserId,
            profile = profile,
            goalHistory = core.goals,
            preferences = core.prefs,
            syncState = effectiveSyncState,
            pendingSyncCount = core.pendingCount,
            selectedDateKey = dateKey.ifBlank { todayKey },
            exercises = training.exercises,
            templates = training.templates,
            workoutSessions = training.sessions,
            allWorkoutSets = training.sets,
            cardioSessions = training.cardio,
            foodItems = nutrition.foods,
            savedMeals = nutrition.meals,
            foodLogs = nutrition.logs,
            waterLogs = nutrition.waterLogs,
            weightLogs = metrics.weights,
            measurements = metrics.measurements,
            progressPhotos = metrics.photos,
            todayCalories = todayCal,
            todayProteinG = todayPro,
            todayCarbsG = todayCarb,
            todayFatG = todayFat,
            todayFiberG = todayFiber,
            todayWaterMl = todayWaterMl,
            waterTargetMl = waterTargetMl,
            isAutoWaterTarget = isAutoWaterTarget,
            weeklyWaterSeries = weeklyWaterSeries,
            targetCarbsG = targetCarbs,
            targetFatG = targetFat,
            targetFiberG = targetFiber,
            todayCardioMinutes = todayCardioMin,
            todayWorkoutCompleted = todayWorkouts.isNotEmpty(),
            todayWorkoutName = todayWorkouts.firstOrNull()?.templateName,
            workoutsThisWeek = workoutsThisWeek,
            dailyVolumeKg = dailyVol,
            weeklyVolumeKg = weeklyVol,
            monthlyVolumeKg = monthlyVol,
            volumeByMuscleKg = volumeByMuscle,
            dailyScore = dailyScore,
            energyBalance = energyBalance,
            weightTrend = weightTrend,
            goalProgressPercent = goalProgress,
            weeklyCaloriesSeries = weeklySeries,
            weeklyCalorieTarget = calorieTarget * 7,
            weeklyCalorieConsumed = weeklyConsumed,
            nutritionCoaching = coaching,
            adaptiveRecommendation = adaptive,
            personalRecords = prs,
            achievements = achievements,
            activeStreakDays = streak
        )
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun postMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            val res = repository.syncNow(currentUserId)
            if (res.isSuccess) {
                postMessage("All data synced to cloud.")
            } else {
                postMessage("Couldn't sync right now. Your data is safely stored locally and will retry automatically.")
            }
        }
    }

    // --- Onboarding & Profile ---

    fun completeOnboarding(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.completeOnboarding(profile.copy(userId = currentUserId, email = authEmail))
            postMessage("Welcome to FitTrack! Starter templates are ready.")
        }
    }

    fun updateProfile(profile: UserProfileEntity, recordGoalHistory: Boolean) {
        viewModelScope.launch {
            repository.updateProfileAndGoal(profile.copy(userId = currentUserId), recordGoalHistory)
            postMessage("Profile & targets updated.")
        }
    }

    fun setUnitSystem(unitSystem: UnitSystem) {
        viewModelScope.launch {
            preferencesManager.setUnitSystem(unitSystem)
        }
    }

    fun saveGitHubRepoAndCheck(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch {
                preferencesManager.setGitHubRepo("")
                _appUpdateStatus.value = AppUpdateStatus.Unconfigured
                postMessage("Cleared GitHub updates repository.")
            }
            return
        }
        val normalized = GitHubUpdateChecker.normalizeRepoInput(trimmed)
        if (normalized == null) {
            postMessage("Enter a valid GitHub repo (e.g. username/fittrack).")
            return
        }
        viewModelScope.launch {
            preferencesManager.setGitHubRepo(normalized)
            _updateBannerDismissed.value = false
            checkForAppUpdates(manual = true, repoOverride = normalized)
        }
    }

    fun checkForAppUpdates(manual: Boolean = true, repoOverride: String? = null) {
        viewModelScope.launch {
            val repo = repoOverride ?: resolveEffectiveGitHubRepo()
            if (repo.isBlank()) {
                _appUpdateStatus.value = AppUpdateStatus.Unconfigured
                if (manual) postMessage("Updates are enabled automatically in GitHub Release builds.")
                return@launch
            }
            _appUpdateStatus.value = AppUpdateStatus.Checking
            val result = GitHubUpdateChecker.checkLatestRelease(
                repoSlug = repo,
                currentVersionName = BuildConfig.VERSION_NAME
            )
            _appUpdateStatus.value = result
            if (manual) {
                when (result) {
                    is AppUpdateStatus.UpdateAvailable -> {
                        _updateBannerDismissed.value = false
                        postMessage("Update ${result.info.tagName} is available!")
                    }
                    is AppUpdateStatus.UpToDate -> {
                        postMessage("FitTrack v${result.currentVersion} is up to date.")
                    }
                    is AppUpdateStatus.Error -> {
                        postMessage(result.message)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun dismissUpdateBanner() {
        _updateBannerDismissed.value = true
    }

    fun updateReminderPrefs(
        workout: Boolean? = null,
        meal: Boolean? = null,
        weight: Boolean? = null,
        weeklyReport: Boolean? = null,
        prAlerts: Boolean? = null
    ) {
        viewModelScope.launch {
            preferencesManager.setReminderPreference(workout, meal, weight, weeklyReport, prAlerts)
        }
    }

    // --- Templates & Exercises ---

    fun addCustomExercise(
        name: String,
        muscleGroup: String,
        secondary: String,
        equipment: String,
        difficulty: String,
        instructions: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCustomExercise(currentUserId, name, muscleGroup, secondary, equipment, difficulty, instructions)
            postMessage("Added custom exercise: $name")
        }
    }

    fun toggleExerciseFavorite(exercise: ExerciseEntity) {
        viewModelScope.launch {
            repository.toggleExerciseFavorite(currentUserId, exercise)
        }
    }

    fun saveWorkoutTemplate(
        templateId: String?,
        name: String,
        description: String,
        exercises: List<TemplateExerciseSpec>
    ) {
        if (name.isBlank() || exercises.isEmpty()) {
            postMessage("Add a name and at least one exercise.")
            return
        }
        viewModelScope.launch {
            repository.saveWorkoutTemplate(currentUserId, templateId, name, description, exercises)
            postMessage("Saved template: $name")
        }
    }

    fun deleteWorkoutTemplate(templateId: String) {
        viewModelScope.launch {
            repository.deleteWorkoutTemplate(currentUserId, templateId)
            postMessage("Template deleted.")
        }
    }

    // --- Active Workout Session & Previous Performance ---

    fun startWorkoutFromTemplate(template: WorkoutTemplateEntity) {
        val specs = SeedSerialization.decodeTemplateExercises(template.exercisesJson)
        val state = uiState.value
        val unitSystem = state.preferences.unitSystem

        val validSessionIds = state.workoutSessions.map { it.id }.toSet()
        val exercises = specs.map { spec ->
            val prevSets = findLastSessionSetsForExercise(spec.exerciseId, state.allWorkoutSets, validSessionIds)
            val prevSummary = prevSets.map { s ->
                "${FitnessCalculations.formatWeight(s.weightKg, unitSystem)} × ${s.reps}"
            }
            val initialSets = if (prevSets.isNotEmpty()) {
                prevSets.mapIndexed { idx, s ->
                    ActiveSetDraft(
                        setNumber = idx + 1,
                        weightKg = s.weightKg,
                        reps = s.reps,
                        rir = s.rir,
                        warmup = s.warmup,
                        completed = false
                    )
                }
            } else {
                (1..spec.targetSets.coerceIn(1, 8)).map { setNum ->
                    ActiveSetDraft(
                        setNumber = setNum,
                        weightKg = spec.targetWeightKg,
                        reps = spec.targetReps,
                        rir = 2,
                        warmup = false,
                        completed = false
                    )
                }
            }

            ActiveExerciseDraft(
                exerciseId = spec.exerciseId,
                exerciseName = spec.exerciseName,
                muscleGroup = spec.muscleGroup,
                restSeconds = spec.restSeconds,
                previousSetsSummary = prevSummary,
                sets = initialSets
            )
        }

        _activeWorkout.value = ActiveWorkoutState(
            templateId = template.id,
            templateName = template.name,
            startedAtEpochMs = System.currentTimeMillis(),
            exercises = exercises
        )
    }

    private fun findLastSessionSetsForExercise(
        exerciseId: String,
        allSets: List<WorkoutSetEntity>,
        validSessionIds: Set<String>
    ): List<WorkoutSetEntity> {
        val forExercise = allSets.filter {
            it.exerciseId == exerciseId && it.completed && it.workoutSessionId in validSessionIds
        }
        val latestSessionId = forExercise.maxByOrNull { it.recordedAtEpochMs }?.workoutSessionId ?: return emptyList()
        return forExercise
            .filter { it.workoutSessionId == latestSessionId }
            .sortedBy { it.setNumber }
    }

    fun updateActiveSet(
        exerciseIndex: Int,
        setIndex: Int,
        weightKg: Double? = null,
        reps: Int? = null,
        rir: Int? = null,
        warmup: Boolean? = null,
        completed: Boolean? = null
    ) {
        val current = _activeWorkout.value ?: return
        val historical = uiState.value.allWorkoutSets.map {
            HistoricalSetRecord(it.exerciseId, it.weightKg, it.reps, it.warmup)
        }
        val updatedExercises = current.exercises.mapIndexed { eIdx, ex ->
            if (eIdx != exerciseIndex) ex
            else {
                val updatedSets = ex.sets.mapIndexed { sIdx, set ->
                    if (sIdx != setIndex) set
                    else {
                        val newWeight = weightKg ?: set.weightKg
                        val newReps = reps ?: set.reps
                        val newWarmup = warmup ?: set.warmup
                        val newCompleted = completed ?: set.completed
                        val prResult = if (newCompleted) {
                            AnalyticsEngines.detectPersonalRecord(
                                exerciseId = ex.exerciseId,
                                weightKg = newWeight,
                                reps = newReps,
                                isWarmup = newWarmup,
                                historicalSets = historical
                            )
                        } else null
                        set.copy(
                            weightKg = newWeight.coerceAtLeast(0.0),
                            reps = newReps.coerceIn(0, 200),
                            rir = (rir ?: set.rir).coerceIn(0, 6),
                            warmup = newWarmup,
                            completed = newCompleted,
                            isPr = prResult?.isNewPr == true,
                            prLabel = prResult?.categories?.firstOrNull()?.label ?: ""
                        )
                    }
                }
                ex.copy(sets = updatedSets)
            }
        }
        _activeWorkout.value = current.copy(exercises = updatedExercises)

        // Automatically kick off exercise rest timer when completing a set
        if (completed == true) {
            val restSecs = current.exercises.getOrNull(exerciseIndex)?.restSeconds ?: 90
            startRestTimer(restSecs)
        }
    }

    fun addSetToActiveExercise(exerciseIndex: Int) {
        val current = _activeWorkout.value ?: return
        val updatedExercises = current.exercises.mapIndexed { eIdx, ex ->
            if (eIdx != exerciseIndex) ex
            else {
                val last = ex.sets.lastOrNull()
                val nextSet = ActiveSetDraft(
                    setNumber = ex.sets.size + 1,
                    weightKg = last?.weightKg ?: 20.0,
                    reps = last?.reps ?: 10,
                    rir = last?.rir ?: 2
                )
                ex.copy(sets = ex.sets + nextSet)
            }
        }
        _activeWorkout.value = current.copy(exercises = updatedExercises)
    }

    fun removeSetFromActiveExercise(exerciseIndex: Int, setIndex: Int) {
        val current = _activeWorkout.value ?: return
        val updatedExercises = current.exercises.mapIndexed { eIdx, ex ->
            if (eIdx != exerciseIndex || ex.sets.size <= 1) ex
            else {
                val filtered = ex.sets.filterIndexed { idx, _ -> idx != setIndex }
                    .mapIndexed { idx, s -> s.copy(setNumber = idx + 1) }
                ex.copy(sets = filtered)
            }
        }
        _activeWorkout.value = current.copy(exercises = updatedExercises)
    }

    fun applyQuickProgression(exerciseIndex: Int, deltaWeightKg: Double, deltaReps: Int) {
        val current = _activeWorkout.value ?: return
        val updatedExercises = current.exercises.mapIndexed { eIdx, ex ->
            if (eIdx != exerciseIndex) ex
            else ex.copy(
                sets = ex.sets.map { s ->
                    if (s.completed || s.warmup) s
                    else s.copy(
                        weightKg = (s.weightKg + deltaWeightKg).coerceAtLeast(0.0),
                        reps = (s.reps + deltaReps).coerceIn(1, 200)
                    )
                }
            )
        }
        _activeWorkout.value = current.copy(exercises = updatedExercises)
    }

    fun updateActiveWorkoutNotes(notes: String) {
        _activeWorkout.update { it?.copy(notes = notes) }
    }

    fun cancelActiveWorkout() {
        stopRestTimer()
        _activeWorkout.value = null
    }

    fun finishActiveWorkout() {
        val active = _activeWorkout.value ?: return
        val now = System.currentTimeMillis()
        val sessionId = "wk_${UUID.randomUUID().toString().take(8)}"
        val completedSets = active.exercises.flatMap { ex ->
            ex.sets.filter { it.completed && it.reps > 0 }.map { s ->
                WorkoutSetEntity(
                    id = "${sessionId}_${ex.exerciseId}_${s.setNumber}_${s.id}",
                    userId = currentUserId,
                    workoutSessionId = sessionId,
                    exerciseId = ex.exerciseId,
                    exerciseName = ex.exerciseName,
                    muscleGroup = ex.muscleGroup,
                    setNumber = s.setNumber,
                    weightKg = s.weightKg,
                    reps = s.reps,
                    rir = s.rir,
                    warmup = s.warmup,
                    completed = true,
                    isPr = s.isPr,
                    prLabel = s.prLabel,
                    recordedAtEpochMs = now
                )
            }
        }
        if (completedSets.isEmpty()) {
            postMessage("Check off at least one set before finishing.")
            return
        }

        val durationMin = max(1, ((now - active.startedAtEpochMs) / 60_000L).toInt())
        val workingSets = completedSets.filter { !it.warmup }
        val totalVol = workingSets.sumOf { FitnessCalculations.calculateSetVolume(it.weightKg, it.reps) }
        val bodyWeight = uiState.value.profile?.currentWeightKg ?: 75.0
        val estCal = FitnessCalculations.estimateStrengthWorkoutCalories(durationMin, bodyWeight, workingSets.size)
        val prCount = completedSets.count { it.isPr }

        val session = WorkoutSessionEntity(
            id = sessionId,
            userId = currentUserId,
            templateId = active.templateId,
            templateName = active.templateName,
            dateKey = FitnessCalculations.todayDateKey(now),
            startedAtEpochMs = active.startedAtEpochMs,
            completedAtEpochMs = now,
            durationMinutes = durationMin,
            totalVolumeKg = totalVol,
            estimatedCalories = estCal,
            prCount = prCount,
            notes = active.notes.trim()
        )

        stopRestTimer()
        _activeWorkout.value = null

        viewModelScope.launch {
            repository.saveCompletedWorkout(session, completedSets)
            val prSuffix = if (prCount > 0) " • $prCount NEW PR!" else ""
            postMessage("Workout saved (${completedSets.size} sets)$prSuffix")
        }
    }

    fun deleteWorkoutSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteWorkoutSession(currentUserId, sessionId)
            postMessage("Workout deleted.")
        }
    }

    // --- Rest Timer ---

    fun startRestTimer(seconds: Int) {
        val safeSec = seconds.coerceIn(10, 600)
        timerJob?.cancel()
        _restTimer.value = RestTimerState(isRunning = true, totalSeconds = safeSec, remainingSeconds = safeSec)
        timerJob = viewModelScope.launch {
            var remaining = safeSec
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _restTimer.update { it.copy(remainingSeconds = remaining, isRunning = remaining > 0) }
            }
        }
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        val current = _restTimer.value
        val next = (current.remainingSeconds + deltaSeconds).coerceIn(5, 600)
        startRestTimer(next)
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        _restTimer.value = RestTimerState(isRunning = false, totalSeconds = 90, remainingSeconds = 0)
    }

    // --- Cardio ---

    fun logCardio(
        activityType: CardioActivityType,
        durationMinutes: Int,
        distanceKm: Double,
        speedKmh: Double,
        inclinePercent: Double,
        resistanceLevel: Int,
        notes: String
    ) {
        if (durationMinutes <= 0) return
        val now = System.currentTimeMillis()
        val weightKg = uiState.value.profile?.currentWeightKg ?: 75.0
        val estCal = FitnessCalculations.estimateCardioCalories(
            activityType = activityType,
            durationMinutes = durationMinutes,
            weightKg = weightKg,
            speedKmh = speedKmh,
            inclinePercent = inclinePercent,
            resistanceLevel = resistanceLevel
        )
        val session = CardioSessionEntity(
            id = "car_${UUID.randomUUID().toString().take(8)}",
            userId = currentUserId,
            activityType = activityType.name,
            dateKey = uiState.value.selectedDateKey,
            durationMinutes = durationMinutes,
            distanceKm = distanceKm.coerceAtLeast(0.0),
            speedKmh = speedKmh.coerceAtLeast(0.0),
            inclinePercent = inclinePercent.coerceAtLeast(0.0),
            resistanceLevel = resistanceLevel.coerceAtLeast(0),
            caloriesEstimated = estCal,
            startedAtEpochMs = now,
            notes = notes.trim()
        )
        viewModelScope.launch {
            repository.logCardioSession(session)
            postMessage("Logged ${activityType.label} ($durationMinutes min, ~$estCal kcal est.)")
        }
    }

    fun deleteCardioSession(cardioId: String) {
        viewModelScope.launch {
            repository.deleteCardioSession(currentUserId, cardioId)
            postMessage("Cardio log removed.")
        }
    }

    // --- Nutrition ---

    fun logFoodItem(food: FoodItemEntity, quantity: Double, mealType: MealType) {
        if (quantity <= 0.0) return
        viewModelScope.launch {
            repository.logFoodItem(currentUserId, food, quantity, mealType, uiState.value.selectedDateKey)
            postMessage("Logged ${food.name}")
        }
    }

    fun toggleFoodFavorite(food: FoodItemEntity) {
        viewModelScope.launch {
            repository.toggleFoodFavorite(currentUserId, food)
        }
    }

    fun createCustomFood(
        name: String,
        brand: String,
        servingSize: Double,
        servingUnit: String,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double,
        logImmediatelyMealType: MealType? = null
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val food = repository.createCustomFood(
                currentUserId, name, brand, servingSize, servingUnit, calories, protein, carbs, fat, fiber
            )
            if (logImmediatelyMealType != null) {
                repository.logFoodItem(currentUserId, food, servingSize, logImmediatelyMealType, uiState.value.selectedDateKey)
            }
            postMessage("Saved custom food: ${food.name}")
        }
    }

    fun saveMealPreset(name: String, mealType: MealType, items: List<SavedMealItemSpec>) {
        if (name.isBlank() || items.isEmpty()) return
        viewModelScope.launch {
            repository.saveMealPreset(currentUserId, name, mealType, items)
            postMessage("Saved meal: $name")
        }
    }

    fun logSavedMeal(meal: SavedMealEntity, multiplier: Double, mealTypeOverride: MealType? = null) {
        viewModelScope.launch {
            repository.logSavedMeal(currentUserId, meal, multiplier, mealTypeOverride, uiState.value.selectedDateKey)
            postMessage("Logged ${meal.name}")
        }
    }

    fun deleteSavedMeal(mealId: String) {
        viewModelScope.launch {
            repository.deleteSavedMeal(currentUserId, mealId)
            postMessage("Saved meal removed.")
        }
    }

    fun deleteFoodLog(logId: String) {
        viewModelScope.launch {
            repository.deleteFoodLog(currentUserId, logId)
        }
    }

    // --- Water Log ---

    fun logWater(amountMl: Int) {
        if (amountMl !in 1..5000) {
            postMessage("Enter a valid water amount.")
            return
        }
        val unit = uiState.value.preferences.unitSystem
        viewModelScope.launch {
            repository.logWater(currentUserId, amountMl, uiState.value.selectedDateKey)
            postMessage("Logged +${FitnessCalculations.formatWater(amountMl, unit)} water")
        }
    }

    fun deleteWaterLog(logId: String) {
        viewModelScope.launch {
            repository.deleteWaterLog(currentUserId, logId)
            postMessage("Water entry removed.")
        }
    }

    fun updateWaterTargetMl(targetMl: Int) {
        viewModelScope.launch {
            repository.updateWaterTargetMl(currentUserId, targetMl)
            if (targetMl == 0) {
                postMessage("Water target reset to Auto (35 ml/kg).")
            } else {
                val unit = uiState.value.preferences.unitSystem
                postMessage("Daily water target set to ${FitnessCalculations.formatWater(targetMl, unit)}.")
            }
        }
    }

    // --- Body Metrics & Photos ---

    fun logWeight(weightKg: Double, note: String = "") {
        if (weightKg !in 25.0..350.0) {
            postMessage("Enter a valid weight.")
            return
        }
        viewModelScope.launch {
            repository.logWeight(currentUserId, weightKg, note, uiState.value.selectedDateKey)
            postMessage("Weight logged.")
        }
    }

    fun logMeasurements(
        waistCm: Double?,
        chestCm: Double?,
        armsCm: Double?,
        thighsCm: Double?,
        neckCm: Double?,
        hipsCm: Double?
    ) {
        if (listOf(waistCm, chestCm, armsCm, thighsCm, neckCm, hipsCm).all { it == null }) {
            postMessage("Enter at least one measurement.")
            return
        }
        viewModelScope.launch {
            repository.logMeasurement(currentUserId, waistCm, chestCm, armsCm, thighsCm, neckCm, hipsCm)
            postMessage("Measurements saved.")
        }
    }

    fun addProgressPhoto(uri: Uri, photoType: PhotoType, note: String) {
        viewModelScope.launch {
            val res = repository.saveProgressPhoto(currentUserId, uri, photoType, note)
            if (res.isSuccess) {
                postMessage("Progress photo saved privately on device.")
            } else {
                postMessage("Could not save photo: ${res.exceptionOrNull()?.localizedMessage ?: "unknown error"}")
            }
        }
    }

    fun deleteProgressPhoto(photo: ProgressPhotoEntity) {
        viewModelScope.launch {
            repository.deleteProgressPhoto(currentUserId, photo)
            postMessage("Progress photo deleted.")
        }
    }

    // --- Privacy & Account ---

    fun exportData(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportUserDataJson(currentUserId)
            onExportReady(json)
        }
    }

    fun clearWorkoutHistory() {
        viewModelScope.launch {
            repository.clearWorkoutHistory(currentUserId)
            postMessage("Workout history cleared.")
        }
    }

    fun clearFoodHistory() {
        viewModelScope.launch {
            repository.clearFoodHistory(currentUserId)
            postMessage("Food history cleared.")
        }
    }

    fun deleteAccountData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllUserData(currentUserId)
            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
