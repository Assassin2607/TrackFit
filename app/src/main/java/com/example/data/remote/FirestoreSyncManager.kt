package com.example.data.remote

import android.content.Context
import com.example.R
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
import com.example.data.seed.SeedSerialization
import com.example.domain.model.SyncState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.io.File

class FirestoreSyncManager(
    private val db: FirebaseFirestore,
    private val localDb: FitTrackDatabase,
    private val appContext: Context,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context, localDb: FitTrackDatabase) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        localDb = localDb,
        appContext = context.applicationContext
    )

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Synced)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private fun requireAuthenticatedUid(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before syncing.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user sync." }
        return uid
    }

    /**
     * Upserts a document preserving immutable createdAt if it already exists in Firestore.
     */
    private suspend fun upsertPreservingCreatedAt(
        docRef: DocumentReference,
        mutableFields: Map<String, Any?>,
        createOnlyFields: Map<String, Any?> = emptyMap()
    ) {
        val cleanMutable = mutableFields.filterValues { it != null }.mapValues { it.value!! }
        val exists = docRef.get().await().exists()
        if (exists) {
            val updatePayload = cleanMutable + ("updatedAt" to FieldValue.serverTimestamp())
            docRef.update(updatePayload).await()
        } else {
            val cleanCreateOnly = createOnlyFields.filterValues { it != null }.mapValues { it.value!! }
            val createPayload = cleanCreateOnly + cleanMutable + mapOf(
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(createPayload).await()
        }
    }

    suspend fun flushQueue(userId: String): Result<Unit> {
        val pending = localDb.syncQueueDao().getPendingForUser(userId)
        if (pending.isEmpty()) {
            _syncState.value = SyncState.Synced
            return Result.success(Unit)
        }
        _syncState.value = SyncState.Syncing

        var lastFailure: Throwable? = null
        for (entry in pending) {
            val result = processQueueEntry(userId, entry)
            if (result.isSuccess) {
                localDb.syncQueueDao().remove(entry.queueKey)
            } else {
                val err = result.exceptionOrNull()
                lastFailure = err
                localDb.syncQueueDao().enqueue(
                    entry.copy(
                        retryCount = entry.retryCount + 1,
                        lastError = err?.localizedMessage ?: "Sync failed",
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
            }
        }

        val remaining = localDb.syncQueueDao().getPendingForUser(userId)
        return if (remaining.isEmpty()) {
            _syncState.value = SyncState.Synced
            Result.success(Unit)
        } else {
            _syncState.value = SyncState.Pending(
                count = remaining.size,
                lastError = lastFailure?.localizedMessage ?: "Waiting for connection"
            )
            Result.failure(lastFailure ?: IllegalStateException("Pending sync items remain"))
        }
    }

    private suspend fun processQueueEntry(userId: String, entry: SyncQueueEntity): Result<Unit> {
        val path = if (entry.collectionName == COLLECTION_PROFILE) {
            "users/$userId"
        } else {
            "users/$userId/${entry.collectionName}/${entry.documentId}"
        }
        return try {
            val uid = requireAuthenticatedUid(userId)
            val userDoc = db.collection("users").document(uid)

            if (entry.action == ACTION_DELETE) {
                val target = if (entry.collectionName == COLLECTION_PROFILE) {
                    userDoc
                } else {
                    userDoc.collection(entry.collectionName).document(entry.documentId)
                }
                target.delete().await()
                return Result.success(Unit)
            }

            when (entry.collectionName) {
                COLLECTION_PROFILE -> {
                    val profile = localDb.userProfileDao().getProfile(uid) ?: return Result.success(Unit)
                    upsertPreservingCreatedAt(
                        docRef = userDoc,
                        createOnlyFields = mapOf("userId" to uid),
                        mutableFields = mapOf(
                            "name" to profile.name.take(80).ifBlank { "Athlete" },
                            "email" to profile.email.take(160).ifBlank { null },
                            "age" to profile.age.coerceIn(13, 120),
                            "gender" to profile.gender,
                            "heightCm" to profile.heightCm.coerceIn(100.0, 250.0),
                            "currentWeightKg" to profile.currentWeightKg.coerceIn(25.0, 350.0),
                            "goalWeightKg" to profile.goalWeightKg.coerceIn(25.0, 350.0),
                            "goalType" to profile.goalType,
                            "activityLevel" to profile.activityLevel,
                            "trainingDaysPerWeek" to profile.trainingDaysPerWeek.coerceIn(1, 7),
                            "preferredSplit" to profile.preferredSplit,
                            "targetRateKgPerWeek" to profile.targetRateKgPerWeek.coerceIn(0.0, 1.5),
                            "calorieTarget" to profile.calorieTarget.coerceIn(1000, 8000),
                            "proteinTargetG" to profile.proteinTargetG.coerceIn(30, 450),
                            "cardioTargetMinPerDay" to profile.cardioTargetMinPerDay.coerceIn(0, 300),
                            "waterTargetMl" to if (profile.waterTargetMl in 500..10000) profile.waterTargetMl else 0,
                            "onboardingCompleted" to profile.onboardingCompleted
                        )
                    )
                }

                COLLECTION_GOALS -> {
                    val goal = localDb.userGoalDao().getById(uid, entry.documentId) ?: return Result.success(Unit)
                    upsertPreservingCreatedAt(
                        docRef = userDoc.collection(COLLECTION_GOALS).document(goal.id),
                        mutableFields = mapOf(
                            "goalType" to goal.goalType,
                            "startingWeightKg" to goal.startingWeightKg.coerceIn(25.0, 350.0),
                            "targetWeightKg" to goal.targetWeightKg.coerceIn(25.0, 350.0),
                            "targetRateKgPerWeek" to goal.targetRateKgPerWeek.coerceIn(0.0, 1.5),
                            "calorieTarget" to goal.calorieTarget.coerceIn(1000, 8000),
                            "proteinTargetG" to goal.proteinTargetG.coerceIn(30, 450),
                            "active" to goal.active
                        )
                    )
                }

                COLLECTION_EXERCISES -> {
                    val ex = localDb.exerciseDao().getById(entry.documentId) ?: return Result.success(Unit)
                    upsertPreservingCreatedAt(
                        docRef = userDoc.collection(COLLECTION_EXERCISES).document(ex.id),
                        mutableFields = mapOf(
                            "name" to ex.name.take(100),
                            "muscleGroup" to ex.muscleGroup.take(40),
                            "secondaryMuscles" to ex.secondaryMuscles.take(120),
                            "equipment" to ex.equipment.take(50).ifBlank { "Other" },
                            "difficulty" to ex.difficulty.take(30).ifBlank { "Intermediate" },
                            "instructions" to ex.instructions.take(500),
                            "custom" to ex.custom,
                            "favorite" to ex.favorite
                        )
                    )
                }

                COLLECTION_TEMPLATES -> {
                    val tpl = localDb.workoutTemplateDao().getById(uid, entry.documentId) ?: return Result.success(Unit)
                    val exercisesMaps = SeedSerialization.encodeTemplateExercisesToMaps(
                        SeedSerialization.decodeTemplateExercises(tpl.exercisesJson)
                    )
                    upsertPreservingCreatedAt(
                        docRef = userDoc.collection(COLLECTION_TEMPLATES).document(tpl.id),
                        mutableFields = mapOf(
                            "name" to tpl.name.take(80),
                            "description" to tpl.description.take(250),
                            "exercises" to exercisesMaps
                        )
                    )
                }

                COLLECTION_WORKOUTS -> {
                    val session = localDb.workoutSessionDao().getSessionById(uid, entry.documentId) ?: return Result.success(Unit)
                    val sets = localDb.workoutSessionDao().getSetsForSession(uid, session.id).take(200).map { s ->
                        mapOf(
                            "exerciseId" to s.exerciseId,
                            "exerciseName" to s.exerciseName,
                            "muscleGroup" to s.muscleGroup,
                            "setNumber" to s.setNumber,
                            "weightKg" to s.weightKg,
                            "reps" to s.reps,
                            "rir" to s.rir,
                            "warmup" to s.warmup,
                            "completed" to s.completed,
                            "isPr" to s.isPr,
                            "prLabel" to s.prLabel
                        )
                    }
                    upsertPreservingCreatedAt(
                        docRef = userDoc.collection(COLLECTION_WORKOUTS).document(session.id),
                        mutableFields = mapOf(
                            "templateId" to session.templateId?.take(128),
                            "templateName" to session.templateName.take(80),
                            "dateKey" to session.dateKey,
                            "startedAtEpochMs" to session.startedAtEpochMs,
                            "completedAtEpochMs" to session.completedAtEpochMs,
                            "durationMinutes" to session.durationMinutes.coerceIn(0, 600),
                            "totalVolumeKg" to session.totalVolumeKg.coerceIn(0.0, 500000.0),
                            "estimatedCalories" to session.estimatedCalories.coerceIn(0, 5000),
                            "notes" to session.notes.take(500),
                            "sets" to sets
                        )
                    )
                }

                COLLECTION_CARDIO -> {
                    val cardio = localDb.cardioSessionDao().getById(uid, entry.documentId) ?: return Result.success(Unit)
                    upsertPreservingCreatedAt(
                        docRef = userDoc.collection(COLLECTION_CARDIO).document(cardio.id),
                        mutableFields = mapOf(
                            "activityType" to cardio.activityType.take(40),
                            "dateKey" to cardio.dateKey,
                            "durationMinutes" to cardio.durationMinutes.coerceIn(1, 600),
                            "distanceKm" to cardio.distanceKm.coerceIn(0.0, 500.0),
                            "speedKmh" to cardio.speedKmh.coerceIn(0.0, 100.0),
                            "inclinePercent" to cardio.inclinePercent.coerceIn(0.0, 45.0),
                            "resistanceLevel" to cardio.resistanceLevel.coerceIn(0, 50),
                            "caloriesEstimated" to cardio.caloriesEstimated.coerceIn(0, 6000),
                            "startedAtEpochMs" to cardio.startedAtEpochMs,
                            "notes" to cardio.notes.take(300)
                        )
                    )
                }

                COLLECTION_CUSTOM_FOODS -> {
                    val food = localDb.nutritionDao().getFoodById(entry.documentId) ?: return Result.success(Unit)
                    upsertPreservingCreatedAt(
                        docRef = userDoc.collection(COLLECTION_CUSTOM_FOODS).document(food.id),
                        mutableFields = mapOf(
                            "name" to food.name.take(100),
                            "brand" to food.brand.take(80),
                            "servingSize" to food.servingSize.coerceIn(0.1, 5000.0),
                            "servingUnit" to food.servingUnit.take(20),
                            "calories" to food.calories.coerceIn(0.0, 5000.0),
                            "protein" to food.protein.coerceIn(0.0, 500.0),
                            "carbs" to food.carbs.coerceIn(0.0, 1000.0),
                            "fat" to food.fat.coerceIn(0.0, 500.0),
                            "fiber" to food.fiber.coerceIn(0.0, 200.0),
                            "favorite" to food.favorite
                        )
                    )
                }

                COLLECTION_MEALS -> {
                    val meal = localDb.nutritionDao().getSavedMealById(uid, entry.documentId) ?: return Result.success(Unit)
                    val itemsMaps = SeedSerialization.encodeSavedMealItemsToMaps(
                        SeedSerialization.decodeSavedMealItems(meal.itemsJson)
                    )
                    upsertPreservingCreatedAt(
                        docRef = userDoc.collection(COLLECTION_MEALS).document(meal.id),
                        mutableFields = mapOf(
                            "name" to meal.name.take(80),
                            "mealType" to meal.mealType,
                            "items" to itemsMaps,
                            "totalCalories" to meal.totalCalories.coerceIn(0.0, 10000.0),
                            "totalProtein" to meal.totalProtein.coerceIn(0.0, 1000.0),
                            "totalCarbs" to meal.totalCarbs.coerceIn(0.0, 2000.0),
                            "totalFat" to meal.totalFat.coerceIn(0.0, 1000.0),
                            "totalFiber" to meal.totalFiber.coerceIn(0.0, 500.0)
                        )
                    )
                }

                COLLECTION_FOOD_LOGS -> {
                    val log = localDb.nutritionDao().getFoodLogById(uid, entry.documentId) ?: return Result.success(Unit)
                    upsertPreservingCreatedAt(
                        docRef = userDoc.collection(COLLECTION_FOOD_LOGS).document(log.id),
                        mutableFields = mapOf(
                            "foodName" to log.foodName.take(120),
                            "foodItemId" to log.foodItemId?.take(128),
                            "mealId" to log.mealId?.take(128),
                            "quantity" to log.quantity.coerceIn(0.01, 10000.0),
                            "unit" to log.unit.take(20),
                            "calories" to log.calories.coerceIn(0.0, 10000.0),
                            "protein" to log.protein.coerceIn(0.0, 1000.0),
                            "carbs" to log.carbs.coerceIn(0.0, 2000.0),
                            "fat" to log.fat.coerceIn(0.0, 1000.0),
                            "fiber" to log.fiber.coerceIn(0.0, 500.0),
                            "mealType" to log.mealType,
                            "dateKey" to log.dateKey,
                            "loggedAtEpochMs" to log.loggedAtEpochMs
                        )
                    )
                }

                COLLECTION_WATER_LOGS -> {
                    val log = localDb.nutritionDao().getWaterLogById(uid, entry.documentId) ?: return Result.success(Unit)
                    upsertPreservingCreatedAt(
                        docRef = userDoc.collection(COLLECTION_WATER_LOGS).document(log.id),
                        mutableFields = mapOf(
                            "amountMl" to log.amountMl.coerceIn(1, 5000),
                            "dateKey" to log.dateKey,
                            "loggedAtEpochMs" to log.loggedAtEpochMs
                        )
                    )
                }

                COLLECTION_WEIGHT_LOGS -> {
                    val log = localDb.bodyMetricsDao().getWeightById(uid, entry.documentId) ?: return Result.success(Unit)
                    upsertPreservingCreatedAt(
                        docRef = userDoc.collection(COLLECTION_WEIGHT_LOGS).document(log.id),
                        mutableFields = mapOf(
                            "weightKg" to log.weightKg.coerceIn(25.0, 350.0),
                            "dateKey" to log.dateKey,
                            "recordedAtEpochMs" to log.recordedAtEpochMs,
                            "note" to log.note.take(200)
                        )
                    )
                }

                COLLECTION_MEASUREMENTS -> {
                    val m = localDb.bodyMetricsDao().getMeasurementById(uid, entry.documentId) ?: return Result.success(Unit)
                    upsertPreservingCreatedAt(
                        docRef = userDoc.collection(COLLECTION_MEASUREMENTS).document(m.id),
                        mutableFields = mapOf(
                            "dateKey" to m.dateKey,
                            "recordedAtEpochMs" to m.recordedAtEpochMs,
                            "waistCm" to m.waistCm?.coerceIn(30.0, 250.0),
                            "chestCm" to m.chestCm?.coerceIn(40.0, 250.0),
                            "armsCm" to m.armsCm?.coerceIn(15.0, 100.0),
                            "thighsCm" to m.thighsCm?.coerceIn(25.0, 150.0),
                            "neckCm" to m.neckCm?.coerceIn(20.0, 80.0),
                            "hipsCm" to m.hipsCm?.coerceIn(40.0, 250.0)
                        )
                    )
                }

                COLLECTION_PROGRESS_PHOTOS -> {
                    val p = localDb.bodyMetricsDao().getPhotoById(uid, entry.documentId) ?: return Result.success(Unit)
                    upsertPreservingCreatedAt(
                        docRef = userDoc.collection(COLLECTION_PROGRESS_PHOTOS).document(p.id),
                        mutableFields = mapOf(
                            "photoType" to p.photoType,
                            "dateKey" to p.dateKey,
                            "recordedAtEpochMs" to p.recordedAtEpochMs,
                            "note" to p.note.take(200),
                            "localFileName" to p.localFileName.take(160)
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (entry.action == ACTION_DELETE) OperationType.DELETE else OperationType.WRITE,
                path
            )
            Result.failure(e)
        }
    }

    /**
     * Pulls remote Firestore state into local Room on sign-in so users switching devices
     * restore their profile, templates, workouts, nutrition, and progress logs cleanly.
     */
    suspend fun pullCloudDataIntoLocal(userId: String): Result<Unit> {
        return try {
            val uid = requireAuthenticatedUid(userId)
            val userDoc = db.collection("users").document(uid)

            // 1. User profile
            val profileSnap = userDoc.get().await()
            if (profileSnap.exists()) {
                val d = profileSnap.data.orEmpty()
                val currentWeight = (d["currentWeightKg"] as? Number)?.toDouble() ?: 75.0
                val existingLocal = localDb.userProfileDao().getProfile(uid)
                localDb.userProfileDao().upsert(
                    UserProfileEntity(
                        userId = uid,
                        name = (d["name"] as? String) ?: "Athlete",
                        email = (d["email"] as? String) ?: "",
                        age = (d["age"] as? Number)?.toInt() ?: 25,
                        gender = (d["gender"] as? String) ?: "MALE",
                        heightCm = (d["heightCm"] as? Number)?.toDouble() ?: 175.0,
                        currentWeightKg = currentWeight,
                        startingWeightKg = existingLocal?.startingWeightKg ?: currentWeight,
                        goalWeightKg = (d["goalWeightKg"] as? Number)?.toDouble() ?: 70.0,
                        goalType = (d["goalType"] as? String) ?: "CUT",
                        activityLevel = (d["activityLevel"] as? String) ?: "MODERATE",
                        trainingDaysPerWeek = (d["trainingDaysPerWeek"] as? Number)?.toInt() ?: 4,
                        preferredSplit = (d["preferredSplit"] as? String) ?: "PPL",
                        targetRateKgPerWeek = (d["targetRateKgPerWeek"] as? Number)?.toDouble() ?: 0.45,
                        calorieTarget = (d["calorieTarget"] as? Number)?.toInt() ?: 2100,
                        proteinTargetG = (d["proteinTargetG"] as? Number)?.toInt() ?: 160,
                        cardioTargetMinPerDay = (d["cardioTargetMinPerDay"] as? Number)?.toInt() ?: 20,
                        waterTargetMl = (d["waterTargetMl"] as? Number)?.toInt() ?: 0,
                        onboardingCompleted = (d["onboardingCompleted"] as? Boolean) ?: false,
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
            }

            // 2. Templates
            val templatesSnap = userDoc.collection(COLLECTION_TEMPLATES).get().await()
            for (doc in templatesSnap.documents) {
                val d = doc.data.orEmpty()
                @Suppress("UNCHECKED_CAST")
                val rawList = (d["exercises"] as? List<Map<String, Any?>>).orEmpty()
                val specs = rawList.map { item ->
                    com.example.data.seed.TemplateExerciseSpec(
                        exerciseId = (item["exerciseId"] as? String) ?: "",
                        exerciseName = (item["exerciseName"] as? String) ?: "",
                        muscleGroup = (item["muscleGroup"] as? String) ?: "CHEST",
                        targetSets = (item["targetSets"] as? Number)?.toInt() ?: 3,
                        targetReps = (item["targetReps"] as? Number)?.toInt() ?: 10,
                        targetWeightKg = (item["targetWeightKg"] as? Number)?.toDouble() ?: 0.0,
                        restSeconds = (item["restSeconds"] as? Number)?.toInt() ?: 90
                    )
                }
                localDb.workoutTemplateDao().upsert(
                    WorkoutTemplateEntity(
                        id = doc.id,
                        userId = uid,
                        name = (d["name"] as? String) ?: "Workout",
                        description = (d["description"] as? String) ?: "",
                        exercisesJson = SeedSerialization.encodeTemplateExercises(specs)
                    )
                )
            }

            // 3. Workouts & Sets
            val workoutsSnap = userDoc.collection(COLLECTION_WORKOUTS).get().await()
            for (doc in workoutsSnap.documents) {
                val d = doc.data.orEmpty()
                @Suppress("UNCHECKED_CAST")
                val rawSets = (d["sets"] as? List<Map<String, Any?>>).orEmpty()
                val prCount = rawSets.count { (it["isPr"] as? Boolean) == true }
                localDb.workoutSessionDao().upsertSession(
                    WorkoutSessionEntity(
                        id = doc.id,
                        userId = uid,
                        templateId = d["templateId"] as? String,
                        templateName = (d["templateName"] as? String) ?: "Workout",
                        dateKey = (d["dateKey"] as? String) ?: "",
                        startedAtEpochMs = (d["startedAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        completedAtEpochMs = (d["completedAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        durationMinutes = (d["durationMinutes"] as? Number)?.toInt() ?: 0,
                        totalVolumeKg = (d["totalVolumeKg"] as? Number)?.toDouble() ?: 0.0,
                        estimatedCalories = (d["estimatedCalories"] as? Number)?.toInt() ?: 0,
                        prCount = prCount,
                        notes = (d["notes"] as? String) ?: ""
                    )
                )
                val setEntities = rawSets.mapIndexed { idx, s ->
                    WorkoutSetEntity(
                        id = "${doc.id}_set_$idx",
                        userId = uid,
                        workoutSessionId = doc.id,
                        exerciseId = (s["exerciseId"] as? String) ?: "",
                        exerciseName = (s["exerciseName"] as? String) ?: "",
                        muscleGroup = (s["muscleGroup"] as? String) ?: "CHEST",
                        setNumber = (s["setNumber"] as? Number)?.toInt() ?: (idx + 1),
                        weightKg = (s["weightKg"] as? Number)?.toDouble() ?: 0.0,
                        reps = (s["reps"] as? Number)?.toInt() ?: 0,
                        rir = (s["rir"] as? Number)?.toInt() ?: 2,
                        warmup = (s["warmup"] as? Boolean) ?: false,
                        completed = (s["completed"] as? Boolean) ?: true,
                        isPr = (s["isPr"] as? Boolean) ?: false,
                        prLabel = (s["prLabel"] as? String) ?: "",
                        recordedAtEpochMs = (d["completedAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                if (setEntities.isNotEmpty()) {
                    localDb.workoutSessionDao().upsertSets(setEntities)
                }
            }

            // 4. Cardio
            val cardioSnap = userDoc.collection(COLLECTION_CARDIO).get().await()
            for (doc in cardioSnap.documents) {
                val d = doc.data.orEmpty()
                localDb.cardioSessionDao().upsert(
                    CardioSessionEntity(
                        id = doc.id,
                        userId = uid,
                        activityType = (d["activityType"] as? String) ?: "WALKING",
                        dateKey = (d["dateKey"] as? String) ?: "",
                        durationMinutes = (d["durationMinutes"] as? Number)?.toInt() ?: 15,
                        distanceKm = (d["distanceKm"] as? Number)?.toDouble() ?: 0.0,
                        speedKmh = (d["speedKmh"] as? Number)?.toDouble() ?: 0.0,
                        inclinePercent = (d["inclinePercent"] as? Number)?.toDouble() ?: 0.0,
                        resistanceLevel = (d["resistanceLevel"] as? Number)?.toInt() ?: 0,
                        caloriesEstimated = (d["caloriesEstimated"] as? Number)?.toInt() ?: 0,
                        startedAtEpochMs = (d["startedAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        notes = (d["notes"] as? String) ?: ""
                    )
                )
            }

            // 5. Custom Foods, Meals, Food Logs
            val foodsSnap = userDoc.collection(COLLECTION_CUSTOM_FOODS).get().await()
            for (doc in foodsSnap.documents) {
                val d = doc.data.orEmpty()
                localDb.nutritionDao().upsertFood(
                    FoodItemEntity(
                        id = doc.id,
                        userId = uid,
                        name = (d["name"] as? String) ?: "Custom Food",
                        brand = (d["brand"] as? String) ?: "",
                        servingSize = (d["servingSize"] as? Number)?.toDouble() ?: 100.0,
                        servingUnit = (d["servingUnit"] as? String) ?: "g",
                        calories = (d["calories"] as? Number)?.toDouble() ?: 0.0,
                        protein = (d["protein"] as? Number)?.toDouble() ?: 0.0,
                        carbs = (d["carbs"] as? Number)?.toDouble() ?: 0.0,
                        fat = (d["fat"] as? Number)?.toDouble() ?: 0.0,
                        fiber = (d["fiber"] as? Number)?.toDouble() ?: 0.0,
                        custom = true,
                        favorite = (d["favorite"] as? Boolean) ?: false
                    )
                )
            }

            val mealsSnap = userDoc.collection(COLLECTION_MEALS).get().await()
            for (doc in mealsSnap.documents) {
                val d = doc.data.orEmpty()
                @Suppress("UNCHECKED_CAST")
                val rawItems = (d["items"] as? List<Map<String, Any?>>).orEmpty()
                val specs = rawItems.map { item ->
                    com.example.data.seed.SavedMealItemSpec(
                        foodItemId = (item["foodItemId"] as? String) ?: "",
                        foodName = (item["foodName"] as? String) ?: "",
                        quantity = (item["quantity"] as? Number)?.toDouble() ?: 100.0,
                        unit = (item["unit"] as? String) ?: "g",
                        calories = (item["calories"] as? Number)?.toDouble() ?: 0.0,
                        protein = (item["protein"] as? Number)?.toDouble() ?: 0.0,
                        carbs = (item["carbs"] as? Number)?.toDouble() ?: 0.0,
                        fat = (item["fat"] as? Number)?.toDouble() ?: 0.0,
                        fiber = (item["fiber"] as? Number)?.toDouble() ?: 0.0
                    )
                }
                localDb.nutritionDao().upsertSavedMeal(
                    SavedMealEntity(
                        id = doc.id,
                        userId = uid,
                        name = (d["name"] as? String) ?: "Saved Meal",
                        mealType = (d["mealType"] as? String) ?: "CUSTOM",
                        itemsJson = SeedSerialization.encodeSavedMealItems(specs),
                        totalCalories = (d["totalCalories"] as? Number)?.toDouble() ?: 0.0,
                        totalProtein = (d["totalProtein"] as? Number)?.toDouble() ?: 0.0,
                        totalCarbs = (d["totalCarbs"] as? Number)?.toDouble() ?: 0.0,
                        totalFat = (d["totalFat"] as? Number)?.toDouble() ?: 0.0,
                        totalFiber = (d["totalFiber"] as? Number)?.toDouble() ?: 0.0
                    )
                )
            }

            val logsSnap = userDoc.collection(COLLECTION_FOOD_LOGS).get().await()
            for (doc in logsSnap.documents) {
                val d = doc.data.orEmpty()
                localDb.nutritionDao().upsertFoodLog(
                    FoodLogEntity(
                        id = doc.id,
                        userId = uid,
                        foodName = (d["foodName"] as? String) ?: "Food",
                        foodItemId = d["foodItemId"] as? String,
                        mealId = d["mealId"] as? String,
                        quantity = (d["quantity"] as? Number)?.toDouble() ?: 100.0,
                        unit = (d["unit"] as? String) ?: "g",
                        calories = (d["calories"] as? Number)?.toDouble() ?: 0.0,
                        protein = (d["protein"] as? Number)?.toDouble() ?: 0.0,
                        carbs = (d["carbs"] as? Number)?.toDouble() ?: 0.0,
                        fat = (d["fat"] as? Number)?.toDouble() ?: 0.0,
                        fiber = (d["fiber"] as? Number)?.toDouble() ?: 0.0,
                        mealType = (d["mealType"] as? String) ?: "CUSTOM",
                        dateKey = (d["dateKey"] as? String) ?: "",
                        loggedAtEpochMs = (d["loggedAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                )
            }

            val waterSnap = userDoc.collection(COLLECTION_WATER_LOGS).get().await()
            for (doc in waterSnap.documents) {
                val d = doc.data.orEmpty()
                localDb.nutritionDao().upsertWaterLog(
                    WaterLogEntity(
                        id = doc.id,
                        userId = uid,
                        amountMl = (d["amountMl"] as? Number)?.toInt() ?: 250,
                        dateKey = (d["dateKey"] as? String) ?: "",
                        loggedAtEpochMs = (d["loggedAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                )
            }

            // 6. Weight & Measurements & Photo Metadata
            val weightsSnap = userDoc.collection(COLLECTION_WEIGHT_LOGS).get().await()
            for (doc in weightsSnap.documents) {
                val d = doc.data.orEmpty()
                localDb.bodyMetricsDao().upsertWeight(
                    WeightLogEntity(
                        id = doc.id,
                        userId = uid,
                        weightKg = (d["weightKg"] as? Number)?.toDouble() ?: 75.0,
                        dateKey = (d["dateKey"] as? String) ?: "",
                        recordedAtEpochMs = (d["recordedAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        note = (d["note"] as? String) ?: ""
                    )
                )
            }

            val measSnap = userDoc.collection(COLLECTION_MEASUREMENTS).get().await()
            for (doc in measSnap.documents) {
                val d = doc.data.orEmpty()
                localDb.bodyMetricsDao().upsertMeasurement(
                    BodyMeasurementEntity(
                        id = doc.id,
                        userId = uid,
                        dateKey = (d["dateKey"] as? String) ?: "",
                        recordedAtEpochMs = (d["recordedAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        waistCm = (d["waistCm"] as? Number)?.toDouble(),
                        chestCm = (d["chestCm"] as? Number)?.toDouble(),
                        armsCm = (d["armsCm"] as? Number)?.toDouble(),
                        thighsCm = (d["thighsCm"] as? Number)?.toDouble(),
                        neckCm = (d["neckCm"] as? Number)?.toDouble(),
                        hipsCm = (d["hipsCm"] as? Number)?.toDouble()
                    )
                )
            }

            val photosSnap = userDoc.collection(COLLECTION_PROGRESS_PHOTOS).get().await()
            val photoDir = File(appContext.filesDir, "progress_photos/$uid")
            for (doc in photosSnap.documents) {
                val d = doc.data.orEmpty()
                val fileName = (d["localFileName"] as? String) ?: continue
                val localFile = File(photoDir, fileName)
                if (localFile.exists()) {
                    localDb.bodyMetricsDao().upsertPhoto(
                        ProgressPhotoEntity(
                            id = doc.id,
                            userId = uid,
                            photoType = (d["photoType"] as? String) ?: "FRONT",
                            dateKey = (d["dateKey"] as? String) ?: "",
                            recordedAtEpochMs = (d["recordedAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            localFilePath = localFile.absolutePath,
                            localFileName = fileName,
                            note = (d["note"] as? String) ?: ""
                        )
                    )
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun deleteAllRemoteUserData(userId: String): Result<Unit> {
        return try {
            val uid = requireAuthenticatedUid(userId)
            val userDoc = db.collection("users").document(uid)
            val collections = listOf(
                COLLECTION_GOALS, COLLECTION_EXERCISES, COLLECTION_TEMPLATES,
                COLLECTION_WORKOUTS, COLLECTION_CARDIO, COLLECTION_CUSTOM_FOODS,
                COLLECTION_MEALS, COLLECTION_FOOD_LOGS, COLLECTION_WATER_LOGS,
                COLLECTION_WEIGHT_LOGS, COLLECTION_MEASUREMENTS, COLLECTION_PROGRESS_PHOTOS
            )
            for (col in collections) {
                val snap = userDoc.collection(col).get().await()
                for (doc in snap.documents) {
                    doc.reference.delete().await()
                }
            }
            userDoc.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId")
            Result.failure(e)
        }
    }

    companion object {
        const val ACTION_UPSERT = "UPSERT"
        const val ACTION_DELETE = "DELETE"

        const val COLLECTION_PROFILE = "profile"
        const val COLLECTION_GOALS = "goals"
        const val COLLECTION_EXERCISES = "exercises"
        const val COLLECTION_TEMPLATES = "templates"
        const val COLLECTION_WORKOUTS = "workouts"
        const val COLLECTION_CARDIO = "cardio"
        const val COLLECTION_CUSTOM_FOODS = "custom_foods"
        const val COLLECTION_MEALS = "meals"
        const val COLLECTION_FOOD_LOGS = "food_logs"
        const val COLLECTION_WATER_LOGS = "water_logs"
        const val COLLECTION_WEIGHT_LOGS = "weight_logs"
        const val COLLECTION_MEASUREMENTS = "measurements"
        const val COLLECTION_PROGRESS_PHOTOS = "progress_photos"
    }
}
