package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        WaterLogEntity::class,
        WeightLogEntity::class,
        BodyMeasurementEntity::class,
        ProgressPhotoEntity::class,
        SyncQueueEntity::class
    ],
    version = 2,
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_profiles ADD COLUMN waterTargetMl INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `water_logs` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `amountMl` INTEGER NOT NULL,
                        `dateKey` TEXT NOT NULL,
                        `loggedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_water_logs_userId` ON `water_logs` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_water_logs_dateKey` ON `water_logs` (`dateKey`)")
            }
        }

        fun getInstance(context: Context): FitTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitTrackDatabase::class.java,
                    "fittrack_offline.db"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
