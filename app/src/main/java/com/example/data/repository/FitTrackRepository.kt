package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.BodyMeasurementEntity
import com.example.data.local.CardioSessionEntity
import com.example.data.local.ExerciseEntity
import com.example.data.local.FitTrackDatabase
import com.example.data.local.FoodItemEntity
import com.example.data.local.FoodLogEntity
import com.example.data.local.ProgressPhotoEntity
import com.example.data.local.SavedMealEntity
import com.example.data.local.SyncQueueEntity
import com.example.data.local.UserGoalEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WaterLogEntity
import com.example.data.local.WeightLogEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.local.WorkoutSetEntity
import com.example.data.local.WorkoutTemplateEntity
import com.example.data.remote.FirestoreSyncManager
import com.example.data.seed.CuratedLocalFoodProvider
import com.example.data.seed.FoodCatalogProvider
import com.example.data.seed.SavedMealItemSpec
import com.example.data.seed.SeedExerciseCatalog
import com.example.data.seed.SeedSerialization
import com.example.data.seed.TemplateExerciseSpec
import com.example.domain.calc.FitnessCalculations
import com.example.domain.model.MealType
import com.example.domain.model.PhotoType
import com.example.domain.model.SyncState
import com.example.domain.model.WorkoutSplit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class FitTrackRepository(
    private val localDb: FitTrackDatabase,
    private val syncManager: FirestoreSyncManager,
    private val appContext: Context,
    private val foodCatalogProvider: FoodCatalogProvider = CuratedLocalFoodProvider
) {
    val syncState: StateFlow<SyncState> = syncManager.syncState

    fun observeProfile(userId: String): Flow<UserProfileEntity?> =
        localDb.userProfileDao().observeProfile(userId)

    fun observeGoals(userId: String): Flow<List<UserGoalEntity>> =
        localDb.userGoalDao().observeGoals(userId)

    fun observeExercises(userId: String): Flow<List<ExerciseEntity>> =
        localDb.exerciseDao().observeExercises(userId)

    fun observeTemplates(userId: String): Flow<List<WorkoutTemplateEntity>> =
        localDb.workoutTemplateDao().observeTemplates(userId)

    fun observeWorkoutSessions(userId: String): Flow<List<WorkoutSessionEntity>> =
        localDb.workoutSessionDao().observeSessions(userId)

    fun observeAllWorkoutSets(userId: String): Flow<List<WorkoutSetEntity>> =
        localDb.workoutSessionDao().observeAllSets(userId)

    fun observeCardioSessions(userId: String): Flow<List<CardioSessionEntity>> =
        localDb.cardioSessionDao().observeCardio(userId)

    fun observeFoodItems(userId: String): Flow<List<FoodItemEntity>> =
        localDb.nutritionDao().observeFoodItems(userId)

    fun observeSavedMeals(userId: String): Flow<List<SavedMealEntity>> =
        localDb.nutritionDao().observeSavedMeals(userId)

    fun observeFoodLogs(userId: String): Flow<List<FoodLogEntity>> =
        localDb.nutritionDao().observeFoodLogs(userId)

    fun observeWaterLogs(userId: String): Flow<List<WaterLogEntity>> =
        localDb.nutritionDao().observeWaterLogs(userId)

    fun observeWeightLogs(userId: String): Flow<List<WeightLogEntity>> =
        localDb.bodyMetricsDao().observeWeightLogs(userId)

    fun observeMeasurements(userId: String): Flow<List<BodyMeasurementEntity>> =
        localDb.bodyMetricsDao().observeMeasurements(userId)

    fun observePhotos(userId: String): Flow<List<ProgressPhotoEntity>> =
        localDb.bodyMetricsDao().observePhotos(userId)

    fun observePendingSyncCount(userId: String): Flow<Int> =
        localDb.syncQueueDao().observePendingCount(userId)

    suspend fun initializeUserSession(userId: String) {
        withContext(Dispatchers.IO) {
            if (localDb.exerciseDao().countGlobal() == 0) {
                localDb.exerciseDao().upsertAll(SeedExerciseCatalog.getBuiltInExercises())
            }
            if (localDb.nutritionDao().countGlobalFoods() == 0) {
                localDb.nutritionDao().upsertFoods(foodCatalogProvider.getStarterFoods())
            }
        }
        // Pull any existing cloud data for this authenticated user, then flush pending local writes
        syncManager.pullCloudDataIntoLocal(userId)
        syncManager.flushQueue(userId)
    }

    suspend fun syncNow(userId: String): Result<Unit> = syncManager.flushQueue(userId)

    private suspend fun enqueueAndSync(
        userId: String,
        collection: String,
        documentId: String,
        action: String = FirestoreSyncManager.ACTION_UPSERT
    ) {
        localDb.syncQueueDao().enqueue(
            SyncQueueEntity(
                queueKey = "$userId:$collection:$documentId",
                userId = userId,
                collectionName = collection,
                documentId = documentId,
                action = action,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
        syncManager.flushQueue(userId)
    }

    // --- Profile & Onboarding ---

    suspend fun completeOnboarding(profile: UserProfileEntity) {
        val now = System.currentTimeMillis()
        val finalProfile = profile.copy(onboardingCompleted = true, updatedAtEpochMs = now)
        localDb.userProfileDao().upsert(finalProfile)

        // Create initial goal history entry
        localDb.userGoalDao().deactivateAll(profile.userId)
        val goalId = "goal_${UUID.randomUUID().toString().take(8)}"
        val goal = UserGoalEntity(
            id = goalId,
            userId = profile.userId,
            goalType = profile.goalType,
            startingWeightKg = profile.currentWeightKg,
            targetWeightKg = profile.goalWeightKg,
            targetRateKgPerWeek = profile.targetRateKgPerWeek,
            calorieTarget = profile.calorieTarget,
            proteinTargetG = profile.proteinTargetG,
            active = true,
            createdAtEpochMs = now
        )
        localDb.userGoalDao().upsert(goal)

        // Log starting weight
        val weightId = "wt_${UUID.randomUUID().toString().take(8)}"
        localDb.bodyMetricsDao().upsertWeight(
            WeightLogEntity(
                id = weightId,
                userId = profile.userId,
                weightKg = profile.currentWeightKg,
                dateKey = FitnessCalculations.todayDateKey(now),
                recordedAtEpochMs = now,
                note = "Starting weight"
            )
        )

        // Seed workout templates for preferred split if user has none yet
        val split = WorkoutSplit.fromNameSafe(profile.preferredSplit)
        val seededTemplates = if (localDb.workoutTemplateDao().countForUser(profile.userId) == 0) {
            SeedExerciseCatalog.createStarterTemplates(profile.userId, split).also {
                localDb.workoutTemplateDao().upsertAll(it)
            }
        } else {
            emptyList()
        }

        // Enqueue all records for cloud sync
        localDb.syncQueueDao().enqueue(
            SyncQueueEntity("${profile.userId}:${FirestoreSyncManager.COLLECTION_PROFILE}:${profile.userId}", profile.userId, FirestoreSyncManager.COLLECTION_PROFILE, profile.userId, FirestoreSyncManager.ACTION_UPSERT)
        )
        localDb.syncQueueDao().enqueue(
            SyncQueueEntity("${profile.userId}:${FirestoreSyncManager.COLLECTION_GOALS}:$goalId", profile.userId, FirestoreSyncManager.COLLECTION_GOALS, goalId, FirestoreSyncManager.ACTION_UPSERT)
        )
        localDb.syncQueueDao().enqueue(
            SyncQueueEntity("${profile.userId}:${FirestoreSyncManager.COLLECTION_WEIGHT_LOGS}:$weightId", profile.userId, FirestoreSyncManager.COLLECTION_WEIGHT_LOGS, weightId, FirestoreSyncManager.ACTION_UPSERT)
        )
        seededTemplates.forEach { tpl ->
            localDb.syncQueueDao().enqueue(
                SyncQueueEntity("${profile.userId}:${FirestoreSyncManager.COLLECTION_TEMPLATES}:${tpl.id}", profile.userId, FirestoreSyncManager.COLLECTION_TEMPLATES, tpl.id, FirestoreSyncManager.ACTION_UPSERT)
            )
        }
        syncManager.flushQueue(profile.userId)
    }

    suspend fun updateProfileAndGoal(profile: UserProfileEntity, recordGoalHistory: Boolean = false) {
        val now = System.currentTimeMillis()
        localDb.userProfileDao().upsert(profile.copy(updatedAtEpochMs = now))
        localDb.syncQueueDao().enqueue(
            SyncQueueEntity("${profile.userId}:${FirestoreSyncManager.COLLECTION_PROFILE}:${profile.userId}", profile.userId, FirestoreSyncManager.COLLECTION_PROFILE, profile.userId, FirestoreSyncManager.ACTION_UPSERT)
        )
        if (recordGoalHistory) {
            localDb.userGoalDao().deactivateAll(profile.userId)
            val goalId = "goal_${UUID.randomUUID().toString().take(8)}"
            localDb.userGoalDao().upsert(
                UserGoalEntity(
                    id = goalId,
                    userId = profile.userId,
                    goalType = profile.goalType,
                    startingWeightKg = profile.currentWeightKg,
                    targetWeightKg = profile.goalWeightKg,
                    targetRateKgPerWeek = profile.targetRateKgPerWeek,
                    calorieTarget = profile.calorieTarget,
                    proteinTargetG = profile.proteinTargetG,
                    active = true,
                    createdAtEpochMs = now
                )
            )
            localDb.syncQueueDao().enqueue(
                SyncQueueEntity("${profile.userId}:${FirestoreSyncManager.COLLECTION_GOALS}:$goalId", profile.userId, FirestoreSyncManager.COLLECTION_GOALS, goalId, FirestoreSyncManager.ACTION_UPSERT)
            )
        }
        syncManager.flushQueue(profile.userId)
    }

    // --- Exercises & Workout Templates ---

    suspend fun addCustomExercise(
        userId: String,
        name: String,
        muscleGroup: String,
        secondaryMuscles: String,
        equipment: String,
        difficulty: String,
        instructions: String
    ) {
        val id = "ex_${UUID.randomUUID().toString().take(8)}"
        val entity = ExerciseEntity(
            id = id,
            userId = userId,
            name = name.trim(),
            muscleGroup = muscleGroup,
            secondaryMuscles = secondaryMuscles.trim(),
            equipment = equipment.trim().ifBlank { "Other" },
            difficulty = difficulty,
            instructions = instructions.trim(),
            custom = true,
            favorite = true
        )
        localDb.exerciseDao().upsert(entity)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_EXERCISES, id)
    }

    suspend fun toggleExerciseFavorite(userId: String, exercise: ExerciseEntity) {
        val updated = exercise.copy(favorite = !exercise.favorite)
        localDb.exerciseDao().upsert(updated)
        if (exercise.userId == userId) {
            enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_EXERCISES, exercise.id)
        }
    }

    suspend fun saveWorkoutTemplate(
        userId: String,
        templateId: String?,
        name: String,
        description: String,
        exercises: List<TemplateExerciseSpec>
    ) {
        val now = System.currentTimeMillis()
        val id = templateId ?: "tpl_${UUID.randomUUID().toString().take(8)}"
        val existing = templateId?.let { localDb.workoutTemplateDao().getById(userId, it) }
        val entity = WorkoutTemplateEntity(
            id = id,
            userId = userId,
            name = name.trim(),
            description = description.trim(),
            exercisesJson = SeedSerialization.encodeTemplateExercises(exercises),
            createdAtEpochMs = existing?.createdAtEpochMs ?: now,
            updatedAtEpochMs = now
        )
        localDb.workoutTemplateDao().upsert(entity)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_TEMPLATES, id)
    }

    suspend fun deleteWorkoutTemplate(userId: String, templateId: String) {
        localDb.workoutTemplateDao().deleteById(userId, templateId)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_TEMPLATES, templateId, FirestoreSyncManager.ACTION_DELETE)
    }

    // --- Workout Sessions & Cardio ---

    suspend fun saveCompletedWorkout(
        session: WorkoutSessionEntity,
        sets: List<WorkoutSetEntity>
    ) {
        localDb.workoutSessionDao().upsertSession(session)
        localDb.workoutSessionDao().upsertSets(sets)
        enqueueAndSync(session.userId, FirestoreSyncManager.COLLECTION_WORKOUTS, session.id)
    }

    suspend fun deleteWorkoutSession(userId: String, sessionId: String) {
        localDb.workoutSessionDao().deleteSetsForSession(userId, sessionId)
        localDb.workoutSessionDao().deleteSession(userId, sessionId)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_WORKOUTS, sessionId, FirestoreSyncManager.ACTION_DELETE)
    }

    suspend fun logCardioSession(session: CardioSessionEntity) {
        localDb.cardioSessionDao().upsert(session)
        enqueueAndSync(session.userId, FirestoreSyncManager.COLLECTION_CARDIO, session.id)
    }

    suspend fun deleteCardioSession(userId: String, cardioId: String) {
        localDb.cardioSessionDao().deleteById(userId, cardioId)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_CARDIO, cardioId, FirestoreSyncManager.ACTION_DELETE)
    }

    // --- Nutrition ---

    suspend fun createCustomFood(
        userId: String,
        name: String,
        brand: String,
        servingSize: Double,
        servingUnit: String,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double
    ): FoodItemEntity {
        val id = "cf_${UUID.randomUUID().toString().take(8)}"
        val food = FoodItemEntity(
            id = id,
            userId = userId,
            name = name.trim(),
            brand = brand.trim(),
            servingSize = servingSize,
            servingUnit = servingUnit.trim().ifBlank { "g" },
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            fiber = fiber,
            custom = true,
            favorite = true,
            useCount = 1
        )
        localDb.nutritionDao().upsertFood(food)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_CUSTOM_FOODS, id)
        return food
    }

    suspend fun toggleFoodFavorite(userId: String, food: FoodItemEntity) {
        val newFav = !food.favorite
        localDb.nutritionDao().setFoodFavorite(food.id, newFav)
        if (food.userId == userId) {
            enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_CUSTOM_FOODS, food.id)
        }
    }

    suspend fun logFoodItem(
        userId: String,
        food: FoodItemEntity,
        quantity: Double,
        mealType: MealType,
        dateKey: String = FitnessCalculations.todayDateKey()
    ) {
        val ratio = if (food.servingSize > 0.0) quantity / food.servingSize else 1.0
        val id = "fl_${UUID.randomUUID().toString().take(8)}"
        val log = FoodLogEntity(
            id = id,
            userId = userId,
            foodName = food.name,
            foodItemId = food.id,
            mealId = null,
            quantity = quantity,
            unit = food.servingUnit,
            calories = food.calories * ratio,
            protein = food.protein * ratio,
            carbs = food.carbs * ratio,
            fat = food.fat * ratio,
            fiber = food.fiber * ratio,
            mealType = mealType.name,
            dateKey = dateKey,
            loggedAtEpochMs = System.currentTimeMillis()
        )
        localDb.nutritionDao().upsertFoodLog(log)
        localDb.nutritionDao().incrementFoodUseCount(food.id)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_FOOD_LOGS, id)
    }

    suspend fun saveMealPreset(
        userId: String,
        name: String,
        mealType: MealType,
        items: List<SavedMealItemSpec>
    ) {
        val id = "meal_${UUID.randomUUID().toString().take(8)}"
        val meal = SavedMealEntity(
            id = id,
            userId = userId,
            name = name.trim(),
            mealType = mealType.name,
            itemsJson = SeedSerialization.encodeSavedMealItems(items),
            totalCalories = items.sumOf { it.calories },
            totalProtein = items.sumOf { it.protein },
            totalCarbs = items.sumOf { it.carbs },
            totalFat = items.sumOf { it.fat },
            totalFiber = items.sumOf { it.fiber },
            createdAtEpochMs = System.currentTimeMillis()
        )
        localDb.nutritionDao().upsertSavedMeal(meal)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_MEALS, id)
    }

    suspend fun deleteSavedMeal(userId: String, mealId: String) {
        localDb.nutritionDao().deleteSavedMeal(userId, mealId)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_MEALS, mealId, FirestoreSyncManager.ACTION_DELETE)
    }

    suspend fun logSavedMeal(
        userId: String,
        meal: SavedMealEntity,
        multiplier: Double = 1.0,
        mealTypeOverride: MealType? = null,
        dateKey: String = FitnessCalculations.todayDateKey()
    ) {
        val safeMult = multiplier.coerceIn(0.25, 5.0)
        val id = "fl_${UUID.randomUUID().toString().take(8)}"
        val label = if (safeMult == 1.0) meal.name else "${meal.name} (${String.format("%.1f", safeMult)}x)"
        val log = FoodLogEntity(
            id = id,
            userId = userId,
            foodName = label,
            foodItemId = null,
            mealId = meal.id,
            quantity = safeMult,
            unit = "meal",
            calories = meal.totalCalories * safeMult,
            protein = meal.totalProtein * safeMult,
            carbs = meal.totalCarbs * safeMult,
            fat = meal.totalFat * safeMult,
            fiber = meal.totalFiber * safeMult,
            mealType = (mealTypeOverride?.name ?: meal.mealType),
            dateKey = dateKey,
            loggedAtEpochMs = System.currentTimeMillis()
        )
        localDb.nutritionDao().upsertFoodLog(log)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_FOOD_LOGS, id)
    }

    suspend fun deleteFoodLog(userId: String, logId: String) {
        localDb.nutritionDao().deleteFoodLog(userId, logId)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_FOOD_LOGS, logId, FirestoreSyncManager.ACTION_DELETE)
    }

    // --- Water Log ---

    suspend fun logWater(
        userId: String,
        amountMl: Int,
        dateKey: String = FitnessCalculations.todayDateKey()
    ) {
        val safeMl = amountMl.coerceIn(1, 5000)
        val id = "wl_${UUID.randomUUID().toString().take(8)}"
        val log = WaterLogEntity(
            id = id,
            userId = userId,
            amountMl = safeMl,
            dateKey = dateKey,
            loggedAtEpochMs = System.currentTimeMillis()
        )
        localDb.nutritionDao().upsertWaterLog(log)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_WATER_LOGS, id)
    }

    suspend fun deleteWaterLog(userId: String, logId: String) {
        localDb.nutritionDao().deleteWaterLog(userId, logId)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_WATER_LOGS, logId, FirestoreSyncManager.ACTION_DELETE)
    }

    suspend fun updateWaterTargetMl(userId: String, targetMl: Int) {
        val existing = localDb.userProfileDao().getProfile(userId) ?: return
        val sanitized = if (targetMl == 0) 0 else targetMl.coerceIn(500, 10000)
        localDb.userProfileDao().upsert(
            existing.copy(waterTargetMl = sanitized, updatedAtEpochMs = System.currentTimeMillis())
        )
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_PROFILE, userId)
    }

    // --- Weight, Body Measurements & Progress Photos ---

    suspend fun logWeight(
        userId: String,
        weightKg: Double,
        note: String = "",
        dateKey: String = FitnessCalculations.todayDateKey()
    ) {
        val now = System.currentTimeMillis()
        val id = "wt_${UUID.randomUUID().toString().take(8)}"
        localDb.bodyMetricsDao().upsertWeight(
            WeightLogEntity(
                id = id,
                userId = userId,
                weightKg = weightKg,
                dateKey = dateKey,
                recordedAtEpochMs = now,
                note = note.trim()
            )
        )
        localDb.userProfileDao().getProfile(userId)?.let { profile ->
            localDb.userProfileDao().upsert(profile.copy(currentWeightKg = weightKg, updatedAtEpochMs = now))
            localDb.syncQueueDao().enqueue(
                SyncQueueEntity("$userId:${FirestoreSyncManager.COLLECTION_PROFILE}:$userId", userId, FirestoreSyncManager.COLLECTION_PROFILE, userId, FirestoreSyncManager.ACTION_UPSERT)
            )
        }
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_WEIGHT_LOGS, id)
    }

    suspend fun deleteWeightLog(userId: String, logId: String) {
        localDb.bodyMetricsDao().deleteWeight(userId, logId)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_WEIGHT_LOGS, logId, FirestoreSyncManager.ACTION_DELETE)
    }

    suspend fun logMeasurement(
        userId: String,
        waistCm: Double?,
        chestCm: Double?,
        armsCm: Double?,
        thighsCm: Double?,
        neckCm: Double?,
        hipsCm: Double?,
        dateKey: String = FitnessCalculations.todayDateKey()
    ) {
        val id = "bm_${UUID.randomUUID().toString().take(8)}"
        localDb.bodyMetricsDao().upsertMeasurement(
            BodyMeasurementEntity(
                id = id,
                userId = userId,
                dateKey = dateKey,
                recordedAtEpochMs = System.currentTimeMillis(),
                waistCm = waistCm,
                chestCm = chestCm,
                armsCm = armsCm,
                thighsCm = thighsCm,
                neckCm = neckCm,
                hipsCm = hipsCm
            )
        )
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_MEASUREMENTS, id)
    }

    suspend fun saveProgressPhoto(
        userId: String,
        sourceUri: Uri,
        photoType: PhotoType,
        note: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val id = "ph_${UUID.randomUUID().toString().take(8)}"
            val photoDir = File(appContext.filesDir, "progress_photos/$userId").apply { mkdirs() }
            val fileName = "${id}_${photoType.name.lowercase()}.jpg"
            val destFile = File(photoDir, fileName)
            appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Could not read selected image.")

            val now = System.currentTimeMillis()
            localDb.bodyMetricsDao().upsertPhoto(
                ProgressPhotoEntity(
                    id = id,
                    userId = userId,
                    photoType = photoType.name,
                    dateKey = FitnessCalculations.todayDateKey(now),
                    recordedAtEpochMs = now,
                    localFilePath = destFile.absolutePath,
                    localFileName = fileName,
                    note = note.trim()
                )
            )
            enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_PROGRESS_PHOTOS, id)
        }
    }

    suspend fun deleteProgressPhoto(userId: String, photo: ProgressPhotoEntity) {
        withContext(Dispatchers.IO) {
            runCatching { File(photo.localFilePath).delete() }
        }
        localDb.bodyMetricsDao().deletePhoto(userId, photo.id)
        enqueueAndSync(userId, FirestoreSyncManager.COLLECTION_PROGRESS_PHOTOS, photo.id, FirestoreSyncManager.ACTION_DELETE)
    }

    // --- Privacy: Export & Deletion ---

    suspend fun exportUserDataJson(userId: String): String = withContext(Dispatchers.IO) {
        val profile = localDb.userProfileDao().getProfile(userId)
        val workouts = localDb.workoutSessionDao().observeSessions(userId).first()
        val cardio = localDb.cardioSessionDao().observeCardio(userId).first()
        val foodLogs = localDb.nutritionDao().observeFoodLogs(userId).first()
        val waterLogs = localDb.nutritionDao().observeWaterLogs(userId).first()
        val weights = localDb.bodyMetricsDao().observeWeightLogs(userId).first()
        val measurements = localDb.bodyMetricsDao().observeMeasurements(userId).first()

        JSONObject().apply {
            put("app", "FitTrack")
            put("exportedAt", FitnessCalculations.todayDateKey())
            put("profile", profile?.let {
                JSONObject().apply {
                    put("name", it.name)
                    put("goalType", it.goalType)
                    put("currentWeightKg", it.currentWeightKg)
                    put("goalWeightKg", it.goalWeightKg)
                    put("calorieTarget", it.calorieTarget)
                    put("proteinTargetG", it.proteinTargetG)
                    put("waterTargetMl", FitnessCalculations.resolveWaterTargetMl(it.waterTargetMl, it.currentWeightKg))
                }
            })
            put("workoutsCount", workouts.size)
            put("cardioCount", cardio.size)
            put("foodLogsCount", foodLogs.size)
            put("waterLogsCount", waterLogs.size)
            put("weightLogs", JSONArray().apply {
                weights.forEach { w ->
                    put(JSONObject().apply {
                        put("date", w.dateKey)
                        put("weightKg", w.weightKg)
                    })
                }
            })
            put("measurementsCount", measurements.size)
        }.toString(2)
    }

    suspend fun clearWorkoutHistory(userId: String) {
        val sessions = localDb.workoutSessionDao().observeSessions(userId).first()
        sessions.forEach { s ->
            localDb.syncQueueDao().enqueue(
                SyncQueueEntity("$userId:${FirestoreSyncManager.COLLECTION_WORKOUTS}:${s.id}", userId, FirestoreSyncManager.COLLECTION_WORKOUTS, s.id, FirestoreSyncManager.ACTION_DELETE)
            )
        }
        localDb.workoutSessionDao().deleteSetsForUser(userId)
        localDb.workoutSessionDao().deleteSessionsForUser(userId)
        syncManager.flushQueue(userId)
    }

    suspend fun clearFoodHistory(userId: String) {
        val logs = localDb.nutritionDao().observeFoodLogs(userId).first()
        logs.forEach { l ->
            localDb.syncQueueDao().enqueue(
                SyncQueueEntity("$userId:${FirestoreSyncManager.COLLECTION_FOOD_LOGS}:${l.id}", userId, FirestoreSyncManager.COLLECTION_FOOD_LOGS, l.id, FirestoreSyncManager.ACTION_DELETE)
            )
        }
        localDb.nutritionDao().deleteFoodLogsForUser(userId)
        syncManager.flushQueue(userId)
    }

    suspend fun deleteAllUserData(userId: String): Result<Unit> {
        withContext(Dispatchers.IO) {
            runCatching { File(appContext.filesDir, "progress_photos/$userId").deleteRecursively() }
            localDb.syncQueueDao().clearForUser(userId)
            localDb.userProfileDao().deleteForUser(userId)
            localDb.userGoalDao().deleteForUser(userId)
            localDb.exerciseDao().deleteForUser(userId)
            localDb.workoutTemplateDao().deleteForUser(userId)
            localDb.workoutSessionDao().deleteSetsForUser(userId)
            localDb.workoutSessionDao().deleteSessionsForUser(userId)
            localDb.cardioSessionDao().deleteForUser(userId)
            localDb.nutritionDao().deleteCustomFoodsForUser(userId)
            localDb.nutritionDao().deleteSavedMealsForUser(userId)
            localDb.nutritionDao().deleteFoodLogsForUser(userId)
            localDb.nutritionDao().deleteWaterLogsForUser(userId)
            localDb.bodyMetricsDao().deleteWeightsForUser(userId)
            localDb.bodyMetricsDao().deleteMeasurementsForUser(userId)
            localDb.bodyMetricsDao().deletePhotosForUser(userId)
        }
        return syncManager.deleteAllRemoteUserData(userId)
    }
}
