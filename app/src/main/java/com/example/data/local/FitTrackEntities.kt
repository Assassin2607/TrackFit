package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String = "",
    val age: Int = 25,
    val gender: String = "MALE",
    val heightCm: Double = 175.0,
    val currentWeightKg: Double = 75.0,
    val startingWeightKg: Double = 75.0,
    val goalWeightKg: Double = 70.0,
    val goalType: String = "CUT",
    val activityLevel: String = "MODERATE",
    val trainingDaysPerWeek: Int = 4,
    val preferredSplit: String = "PPL",
    val targetRateKgPerWeek: Double = 0.45,
    val calorieTarget: Int = 2100,
    val proteinTargetG: Int = 160,
    val cardioTargetMinPerDay: Int = 20,
    val onboardingCompleted: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_goals", indices = [Index("userId")])
data class UserGoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val goalType: String,
    val startingWeightKg: Double,
    val targetWeightKg: Double,
    val targetRateKgPerWeek: Double,
    val calorieTarget: Int,
    val proteinTargetG: Int,
    val active: Boolean,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercises", indices = [Index("userId")])
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val userId: String, // "GLOBAL" for built-in catalog, or actual uid for custom
    val name: String,
    val muscleGroup: String,
    val secondaryMuscles: String = "",
    val equipment: String = "Barbell",
    val difficulty: String = "Intermediate",
    val instructions: String = "",
    val custom: Boolean = false,
    val favorite: Boolean = false
)

@Entity(tableName = "workout_templates", indices = [Index("userId")])
data class WorkoutTemplateEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String = "",
    val exercisesJson: String = "[]", // Serialized list of TemplateExerciseSpec
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_sessions", indices = [Index("userId"), Index("dateKey")])
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val templateId: String? = null,
    val templateName: String,
    val dateKey: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val durationMinutes: Int,
    val totalVolumeKg: Double,
    val estimatedCalories: Int,
    val prCount: Int = 0,
    val notes: String = ""
)

@Entity(tableName = "workout_sets", indices = [Index("userId"), Index("workoutSessionId"), Index("exerciseId")])
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val workoutSessionId: String,
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rir: Int = 2,
    val warmup: Boolean = false,
    val completed: Boolean = true,
    val isPr: Boolean = false,
    val prLabel: String = "",
    val recordedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "cardio_sessions", indices = [Index("userId"), Index("dateKey")])
data class CardioSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val activityType: String,
    val dateKey: String,
    val durationMinutes: Int,
    val distanceKm: Double = 0.0,
    val speedKmh: Double = 0.0,
    val inclinePercent: Double = 0.0,
    val resistanceLevel: Int = 0,
    val caloriesEstimated: Int,
    val startedAtEpochMs: Long,
    val notes: String = ""
)

@Entity(tableName = "food_items", indices = [Index("userId")])
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val userId: String, // "GLOBAL" for curated staple catalog, or actual uid for custom
    val name: String,
    val brand: String = "",
    val servingSize: Double = 100.0,
    val servingUnit: String = "g",
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,
    val custom: Boolean = false,
    val favorite: Boolean = false,
    val useCount: Int = 0
)

@Entity(tableName = "saved_meals", indices = [Index("userId")])
data class SavedMealEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val mealType: String,
    val itemsJson: String = "[]",
    val totalCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val totalFiber: Double,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_logs", indices = [Index("userId"), Index("dateKey")])
data class FoodLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val foodName: String,
    val foodItemId: String? = null,
    val mealId: String? = null,
    val quantity: Double,
    val unit: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val mealType: String,
    val dateKey: String,
    val loggedAtEpochMs: Long
)

@Entity(tableName = "weight_logs", indices = [Index("userId"), Index("dateKey")])
data class WeightLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val weightKg: Double,
    val dateKey: String,
    val recordedAtEpochMs: Long,
    val note: String = ""
)

@Entity(tableName = "body_measurements", indices = [Index("userId"), Index("dateKey")])
data class BodyMeasurementEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val dateKey: String,
    val recordedAtEpochMs: Long,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val armsCm: Double? = null,
    val thighsCm: Double? = null,
    val neckCm: Double? = null,
    val hipsCm: Double? = null
)

@Entity(tableName = "progress_photos", indices = [Index("userId")])
data class ProgressPhotoEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val photoType: String,
    val dateKey: String,
    val recordedAtEpochMs: Long,
    val localFilePath: String,
    val localFileName: String,
    val note: String = ""
)

@Entity(tableName = "sync_queue", indices = [Index("userId")])
data class SyncQueueEntity(
    @PrimaryKey val queueKey: String, // e.g., "$collection:$documentId"
    val userId: String,
    val collectionName: String,
    val documentId: String,
    val action: String, // "UPSERT" or "DELETE"
    val retryCount: Int = 0,
    val lastError: String? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
