package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    fun observeProfile(userId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfile(userId: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface UserGoalDao {
    @Query("SELECT * FROM user_goals WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
    fun observeGoals(userId: String): Flow<List<UserGoalEntity>>

    @Query("SELECT * FROM user_goals WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: String, id: String): UserGoalEntity?

    @Query("UPDATE user_goals SET active = 0 WHERE userId = :userId")
    suspend fun deactivateAll(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: UserGoalEntity)

    @Query("DELETE FROM user_goals WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE userId = 'GLOBAL' OR userId = :userId ORDER BY favorite DESC, name ASC")
    fun observeExercises(userId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT COUNT(*) FROM exercises WHERE userId = 'GLOBAL'")
    suspend fun countGlobal(): Int

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface WorkoutTemplateDao {
    @Query("SELECT * FROM workout_templates WHERE userId = :userId ORDER BY createdAtEpochMs ASC")
    fun observeTemplates(userId: String): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: String, id: String): WorkoutTemplateEntity?

    @Query("SELECT COUNT(*) FROM workout_templates WHERE userId = :userId")
    suspend fun countForUser(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: WorkoutTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(templates: List<WorkoutTemplateEntity>)

    @Query("DELETE FROM workout_templates WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: String, id: String)

    @Query("DELETE FROM workout_templates WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY startedAtEpochMs DESC")
    fun observeSessions(userId: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getSessionById(userId: String, id: String): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE userId = :userId AND id = :id")
    suspend fun deleteSession(userId: String, id: String)

    @Query("DELETE FROM workout_sessions WHERE userId = :userId")
    suspend fun deleteSessionsForUser(userId: String)

    @Query("SELECT * FROM workout_sets WHERE userId = :userId ORDER BY recordedAtEpochMs DESC")
    fun observeAllSets(userId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE userId = :userId AND workoutSessionId = :sessionId ORDER BY setNumber ASC")
    suspend fun getSetsForSession(userId: String, sessionId: String): List<WorkoutSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSets(sets: List<WorkoutSetEntity>)

    @Query("DELETE FROM workout_sets WHERE userId = :userId AND workoutSessionId = :sessionId")
    suspend fun deleteSetsForSession(userId: String, sessionId: String)

    @Query("DELETE FROM workout_sets WHERE userId = :userId")
    suspend fun deleteSetsForUser(userId: String)
}

@Dao
interface CardioSessionDao {
    @Query("SELECT * FROM cardio_sessions WHERE userId = :userId ORDER BY startedAtEpochMs DESC")
    fun observeCardio(userId: String): Flow<List<CardioSessionEntity>>

    @Query("SELECT * FROM cardio_sessions WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: String, id: String): CardioSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: CardioSessionEntity)

    @Query("DELETE FROM cardio_sessions WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: String, id: String)

    @Query("DELETE FROM cardio_sessions WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface NutritionDao {
    @Query("SELECT * FROM food_items WHERE userId = 'GLOBAL' OR userId = :userId ORDER BY favorite DESC, useCount DESC, name ASC")
    fun observeFoodItems(userId: String): Flow<List<FoodItemEntity>>

    @Query("SELECT COUNT(*) FROM food_items WHERE userId = 'GLOBAL'")
    suspend fun countGlobalFoods(): Int

    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    suspend fun getFoodById(id: String): FoodItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoods(items: List<FoodItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFood(item: FoodItemEntity)

    @Query("UPDATE food_items SET useCount = useCount + 1 WHERE id = :id")
    suspend fun incrementFoodUseCount(id: String)

    @Query("UPDATE food_items SET favorite = :favorite WHERE id = :id")
    suspend fun setFoodFavorite(id: String, favorite: Boolean)

    @Query("DELETE FROM food_items WHERE userId = :userId")
    suspend fun deleteCustomFoodsForUser(userId: String)

    @Query("SELECT * FROM saved_meals WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
    fun observeSavedMeals(userId: String): Flow<List<SavedMealEntity>>

    @Query("SELECT * FROM saved_meals WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getSavedMealById(userId: String, id: String): SavedMealEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedMeal(meal: SavedMealEntity)

    @Query("DELETE FROM saved_meals WHERE userId = :userId AND id = :id")
    suspend fun deleteSavedMeal(userId: String, id: String)

    @Query("DELETE FROM saved_meals WHERE userId = :userId")
    suspend fun deleteSavedMealsForUser(userId: String)

    @Query("SELECT * FROM food_logs WHERE userId = :userId ORDER BY loggedAtEpochMs DESC")
    fun observeFoodLogs(userId: String): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getFoodLogById(userId: String, id: String): FoodLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoodLog(log: FoodLogEntity)

    @Query("DELETE FROM food_logs WHERE userId = :userId AND id = :id")
    suspend fun deleteFoodLog(userId: String, id: String)

    @Query("DELETE FROM food_logs WHERE userId = :userId")
    suspend fun deleteFoodLogsForUser(userId: String)
}

@Dao
interface BodyMetricsDao {
    @Query("SELECT * FROM weight_logs WHERE userId = :userId ORDER BY recordedAtEpochMs ASC")
    fun observeWeightLogs(userId: String): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getWeightById(userId: String, id: String): WeightLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeight(log: WeightLogEntity)

    @Query("DELETE FROM weight_logs WHERE userId = :userId AND id = :id")
    suspend fun deleteWeight(userId: String, id: String)

    @Query("DELETE FROM weight_logs WHERE userId = :userId")
    suspend fun deleteWeightsForUser(userId: String)

    @Query("SELECT * FROM body_measurements WHERE userId = :userId ORDER BY recordedAtEpochMs DESC")
    fun observeMeasurements(userId: String): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getMeasurementById(userId: String, id: String): BodyMeasurementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeasurement(item: BodyMeasurementEntity)

    @Query("DELETE FROM body_measurements WHERE userId = :userId AND id = :id")
    suspend fun deleteMeasurement(userId: String, id: String)

    @Query("DELETE FROM body_measurements WHERE userId = :userId")
    suspend fun deleteMeasurementsForUser(userId: String)

    @Query("SELECT * FROM progress_photos WHERE userId = :userId ORDER BY recordedAtEpochMs DESC")
    fun observePhotos(userId: String): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT * FROM progress_photos WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getPhotoById(userId: String, id: String): ProgressPhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPhoto(photo: ProgressPhotoEntity)

    @Query("DELETE FROM progress_photos WHERE userId = :userId AND id = :id")
    suspend fun deletePhoto(userId: String, id: String)

    @Query("DELETE FROM progress_photos WHERE userId = :userId")
    suspend fun deletePhotosForUser(userId: String)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE userId = :userId ORDER BY updatedAtEpochMs ASC")
    suspend fun getPendingForUser(userId: String): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE userId = :userId")
    fun observePendingCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entry: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE queueKey = :queueKey")
    suspend fun remove(queueKey: String)

    @Query("DELETE FROM sync_queue WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}
