package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        UserGoalEntity::class,
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        CardioSessionEntity::class,
        FoodItemEntity::class,
        SavedMealEntity::class,
        FoodLogEntity::class,
        WeightLogEntity::class,
        BodyMeasurementEntity::class,
        ProgressPhotoEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FitTrackDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userGoalDao(): UserGoalDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun cardioSessionDao(): CardioSessionDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun bodyMetricsDao(): BodyMetricsDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: FitTrackDatabase? = null

        fun getInstance(context: Context): FitTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitTrackDatabase::class.java,
                    "fittrack_offline.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
