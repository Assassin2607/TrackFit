package com.example.data.seed

import com.example.data.local.ExerciseEntity
import com.example.data.local.FoodItemEntity
import com.example.data.local.WorkoutTemplateEntity
import com.example.domain.model.WorkoutSplit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TemplateExerciseSpec(
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeightKg: Double = 0.0,
    val restSeconds: Int = 90
)

data class SavedMealItemSpec(
    val foodItemId: String,
    val foodName: String,
    val quantity: Double,
    val unit: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double
)

/**
 * Swappable nutrition data source boundary so external food APIs
 * (e.g., Open Food Facts / USDA) can be added later without modifying ViewModels or UI.
 */
interface FoodCatalogProvider {
    fun getStarterFoods(): List<FoodItemEntity>
}

object SeedSerialization {
    fun encodeTemplateExercises(list: List<TemplateExerciseSpec>): String {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("exerciseId", item.exerciseId)
                    put("exerciseName", item.exerciseName)
                    put("muscleGroup", item.muscleGroup)
                    put("targetSets", item.targetSets)
                    put("targetReps", item.targetReps)
                    put("targetWeightKg", item.targetWeightKg)
                    put("restSeconds", item.restSeconds)
                }
            )
        }
        return arr.toString()
    }

    fun decodeTemplateExercises(json: String): List<TemplateExerciseSpec> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                TemplateExerciseSpec(
                    exerciseId = obj.optString("exerciseId"),
                    exerciseName = obj.optString("exerciseName"),
                    muscleGroup = obj.optString("muscleGroup", "CHEST"),
                    targetSets = obj.optInt("targetSets", 3),
                    targetReps = obj.optInt("targetReps", 10),
                    targetWeightKg = obj.optDouble("targetWeightKg", 0.0),
                    restSeconds = obj.optInt("restSeconds", 90)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun encodeTemplateExercisesToMaps(list: List<TemplateExerciseSpec>): List<Map<String, Any>> =
        list.take(30).map { item ->
            mapOf(
                "exerciseId" to item.exerciseId,
                "exerciseName" to item.exerciseName,
                "muscleGroup" to item.muscleGroup,
                "targetSets" to item.targetSets,
                "targetReps" to item.targetReps,
                "targetWeightKg" to item.targetWeightKg,
                "restSeconds" to item.restSeconds
            )
        }

    fun encodeSavedMealItems(list: List<SavedMealItemSpec>): String {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("foodItemId", item.foodItemId)
                    put("foodName", item.foodName)
                    put("quantity", item.quantity)
                    put("unit", item.unit)
                    put("calories", item.calories)
                    put("protein", item.protein)
                    put("carbs", item.carbs)
                    put("fat", item.fat)
                    put("fiber", item.fiber)
                }
            )
        }
        return arr.toString()
    }

    fun decodeSavedMealItems(json: String): List<SavedMealItemSpec> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                SavedMealItemSpec(
                    foodItemId = obj.optString("foodItemId"),
                    foodName = obj.optString("foodName"),
                    quantity = obj.optDouble("quantity", 100.0),
                    unit = obj.optString("unit", "g"),
                    calories = obj.optDouble("calories", 0.0),
                    protein = obj.optDouble("protein", 0.0),
                    carbs = obj.optDouble("carbs", 0.0),
                    fat = obj.optDouble("fat", 0.0),
                    fiber = obj.optDouble("fiber", 0.0)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun encodeSavedMealItemsToMaps(list: List<SavedMealItemSpec>): List<Map<String, Any>> =
        list.take(30).map { item ->
            mapOf(
                "foodItemId" to item.foodItemId,
                "foodName" to item.foodName,
                "quantity" to item.quantity,
                "unit" to item.unit,
                "calories" to item.calories,
                "protein" to item.protein,
                "carbs" to item.carbs,
                "fat" to item.fat,
                "fiber" to item.fiber
            )
        }
}

object CuratedLocalFoodProvider : FoodCatalogProvider {
    override fun getStarterFoods(): List<FoodItemEntity> = listOf(
        // Proteins
        food("food_chicken_breast", "Chicken Breast (Raw/Cooked)", "Lean Poultry", 100.0, "g", 165.0, 31.0, 0.0, 3.6, 0.0, fav = true),
        food("food_whole_egg", "Whole Egg (Large)", "Whole Foods", 1.0, "egg", 72.0, 6.3, 0.4, 4.8, 0.0, fav = true),
        food("food_egg_whites", "Liquid Egg Whites", "Whole Foods", 100.0, "g", 52.0, 10.9, 0.7, 0.2, 0.0),
        food("food_whey_isolate", "Whey Protein Isolate", "Supplement", 30.0, "g", 115.0, 25.0, 1.5, 0.8, 0.0, fav = true),
        food("food_greek_yogurt_0", "Greek Yogurt (0% Fat)", "Dairy", 100.0, "g", 59.0, 10.3, 3.6, 0.4, 0.0),
        food("food_salmon", "Atlantic Salmon Fillet", "Seafood", 100.0, "g", 208.0, 20.4, 0.0, 13.4, 0.0),
        food("food_lean_beef", "Lean Ground Beef (93/7)", "Beef", 100.0, "g", 152.0, 21.0, 0.0, 7.0, 0.0),
        food("food_turkey_breast", "Turkey Breast", "Lean Poultry", 100.0, "g", 135.0, 29.9, 0.0, 1.0, 0.0),
        food("food_tuna_water", "Tuna Canned in Water", "Seafood", 100.0, "g", 116.0, 25.5, 0.0, 0.8, 0.0),
        food("food_shrimp", "Shrimp / Prawns (Cooked)", "Seafood", 100.0, "g", 99.0, 24.0, 0.2, 0.3, 0.0),
        food("food_paneer", "Paneer (Low-Fat Cottage Cheese)", "Dairy", 100.0, "g", 265.0, 18.3, 1.2, 20.8, 0.0),
        food("food_tofu_firm", "Firm Tofu", "Plant Protein", 100.0, "g", 144.0, 15.8, 3.5, 8.7, 2.3),
        food("food_cottage_cheese", "Cottage Cheese (Low-Fat 2%)", "Dairy", 100.0, "g", 81.0, 10.5, 4.3, 2.3, 0.0),
        food("food_soya_chunks", "Soya Chunks (Dry)", "Plant Protein", 100.0, "g", 345.0, 52.0, 33.0, 0.5, 13.0),

        // Complex Carbs & Grains
        food("food_white_rice", "White Rice (Cooked)", "Grains", 100.0, "g", 130.0, 2.7, 28.2, 0.3, 0.4, fav = true),
        food("food_basmati_rice", "Basmati Rice (Cooked)", "Grains", 100.0, "g", 121.0, 3.5, 25.2, 0.4, 0.4),
        food("food_oats", "Rolled Oats (Dry)", "Grains", 100.0, "g", 389.0, 16.9, 66.3, 6.9, 10.6, fav = true),
        food("food_sweet_potato", "Sweet Potato (Baked)", "Tubers", 100.0, "g", 90.0, 2.0, 20.7, 0.1, 3.3),
        food("food_white_potato", "Potato (Boiled)", "Tubers", 100.0, "g", 87.0, 1.9, 20.1, 0.1, 1.8),
        food("food_whole_wheat_bread", "Whole Wheat Bread", "Bakery", 1.0, "slice", 82.0, 4.0, 13.8, 1.1, 2.0),
        food("food_roti", "Whole Wheat Roti / Chapati", "Indian Staple", 1.0, "piece", 104.0, 3.4, 18.0, 2.4, 2.8),
        food("food_pasta_cooked", "Pasta (Cooked)", "Grains", 100.0, "g", 158.0, 5.8, 30.9, 0.9, 1.8),
        food("food_quinoa", "Quinoa (Cooked)", "Grains", 100.0, "g", 120.0, 4.4, 21.3, 1.9, 2.8),
        food("food_lentils", "Dal / Lentils (Cooked)", "Legumes", 100.0, "g", 116.0, 9.0, 20.1, 0.4, 7.9),
        food("food_chickpeas", "Chickpeas (Cooked)", "Legumes", 100.0, "g", 164.0, 8.9, 27.4, 2.6, 7.6),
        food("food_black_beans", "Black Beans (Cooked)", "Legumes", 100.0, "g", 132.0, 8.9, 23.7, 0.5, 8.7),
        food("food_rice_cakes", "Plain Rice Cakes", "Snack", 1.0, "cake", 35.0, 0.7, 7.3, 0.3, 0.4),

        // Fruits & Vegetables
        food("food_banana", "Banana (Medium)", "Fruit", 1.0, "piece", 105.0, 1.3, 27.0, 0.4, 3.1, fav = true),
        food("food_apple", "Apple (Medium)", "Fruit", 1.0, "piece", 95.0, 0.5, 25.1, 0.3, 4.4),
        food("food_blueberries", "Blueberries", "Fruit", 100.0, "g", 57.0, 0.7, 14.5, 0.3, 2.4),
        food("food_strawberries", "Strawberries", "Fruit", 100.0, "g", 32.0, 0.7, 7.7, 0.3, 2.0),
        food("food_orange", "Orange (Medium)", "Fruit", 1.0, "piece", 62.0, 1.2, 15.4, 0.2, 3.1),
        food("food_broccoli", "Broccoli (Steamed)", "Vegetable", 100.0, "g", 35.0, 2.4, 7.2, 0.4, 3.3),
        food("food_spinach", "Spinach (Raw)", "Vegetable", 100.0, "g", 23.0, 2.9, 3.6, 0.4, 2.2),
        food("food_mixed_salad", "Mixed Green Salad", "Vegetable", 100.0, "g", 20.0, 1.5, 3.5, 0.2, 2.0),
        food("food_cucumber", "Cucumber", "Vegetable", 100.0, "g", 15.0, 0.7, 3.6, 0.1, 0.5),
        food("food_carrots", "Carrots", "Vegetable", 100.0, "g", 41.0, 0.9, 9.6, 0.2, 2.8),

        // Healthy Fats & Dairy
        food("food_peanut_butter", "Natural Peanut Butter", "Healthy Fats", 16.0, "g", 95.0, 4.0, 3.2, 8.0, 1.5),
        food("food_almonds", "Almonds (Raw)", "Healthy Fats", 28.0, "g", 164.0, 6.0, 6.1, 14.2, 3.5),
        food("food_olive_oil", "Extra Virgin Olive Oil", "Healthy Fats", 14.0, "g", 119.0, 0.0, 0.0, 13.5, 0.0),
        food("food_avocado", "Avocado", "Healthy Fats", 100.0, "g", 160.0, 2.0, 8.5, 14.7, 6.7),
        food("food_chia_seeds", "Chia Seeds", "Healthy Fats", 28.0, "g", 138.0, 4.7, 11.9, 8.7, 9.8),
        food("food_milk_2pct", "Milk (2% Reduced Fat)", "Dairy", 240.0, "ml", 122.0, 8.1, 11.7, 4.8, 0.0),
        food("food_skim_milk", "Skim Milk (Fat-Free)", "Dairy", 240.0, "ml", 83.0, 8.3, 12.2, 0.2, 0.0),
        food("food_honey", "Raw Honey", "Sweetener", 21.0, "g", 64.0, 0.1, 17.3, 0.0, 0.0),
        food("food_dark_chocolate", "Dark Chocolate (85%)", "Treat", 30.0, "g", 170.0, 2.7, 11.0, 13.5, 3.6)
    )

    private fun food(
        id: String,
        name: String,
        brand: String,
        serving: Double,
        unit: String,
        kcal: Double,
        p: Double,
        c: Double,
        f: Double,
        fiber: Double,
        fav: Boolean = false
    ) = FoodItemEntity(
        id = id,
        userId = "GLOBAL",
        name = name,
        brand = brand,
        servingSize = serving,
        servingUnit = unit,
        calories = kcal,
        protein = p,
        carbs = c,
        fat = f,
        fiber = fiber,
        custom = false,
        favorite = fav,
        useCount = if (fav) 3 else 0
    )
}

object SeedExerciseCatalog {
    fun getBuiltInExercises(): List<ExerciseEntity> = listOf(
        // Chest
        ex("ex_bench_press", "Barbell Bench Press", "CHEST", "Triceps, Front Delts", "Barbell", "Intermediate", "Retract shoulder blades, lower bar to mid-chest with control, press powerfully."),
        ex("ex_incline_db_press", "Incline Dumbbell Press", "CHEST", "Front Delts, Triceps", "Dumbbell", "Beginner", "Set bench to 30°, press dumbbells over upper chest without locking elbows sharply."),
        ex("ex_cable_fly", "Cable Chest Fly", "CHEST", "Front Delts", "Cable", "Beginner", "Keep slight elbow bend and squeeze palms together across midline."),
        ex("ex_dips", "Weighted Chest Dips", "CHEST", "Triceps, Front Delts", "Bodyweight", "Intermediate", "Lean slightly forward, descend until upper arms are parallel to floor."),
        ex("ex_pushups", "Push-Ups", "CHEST", "Triceps, Core", "Bodyweight", "Beginner", "Maintain rigid plank line from head to heels."),

        // Back
        ex("ex_pullups", "Pull-Ups", "BACK", "Biceps, Rear Delts", "Bodyweight", "Intermediate", "Drive elbows down toward hips until chin clears the bar."),
        ex("ex_lat_pulldown", "Lat Pulldown", "BACK", "Biceps, Rhomboids", "Cable", "Beginner", "Pull bar to upper chest with controlled eccentric stretch."),
        ex("ex_barbell_row", "Barbell Bent-Over Row", "BACK", "Biceps, Core, Hamstrings", "Barbell", "Intermediate", "Hinge at hips ~45°, brace core, row bar cleanly to lower ribs."),
        ex("ex_seated_cable_row", "Seated Cable Row", "BACK", "Biceps, Mid-Traps", "Cable", "Beginner", "Keep chest tall and squeeze shoulder blades together at peak contraction."),
        ex("ex_db_row", "Single-Arm Dumbbell Row", "BACK", "Biceps, Rear Delts", "Dumbbell", "Beginner", "Pull dumbbell toward hip pocket while keeping torso square."),
        ex("ex_deadlift", "Conventional Deadlift", "BACK", "Hamstrings, Glutes, Core", "Barbell", "Advanced", "Wedge hips, pull slack out of bar, drive floor away keeping bar close to shins."),

        // Shoulders
        ex("ex_ohp", "Overhead Barbell Press", "SHOULDERS", "Triceps, Upper Chest", "Barbell", "Intermediate", "Brace glutes and abs, press bar overhead in a vertical path."),
        ex("ex_db_shoulder_press", "Seated Dumbbell Shoulder Press", "SHOULDERS", "Triceps", "Dumbbell", "Beginner", "Press overhead smoothly with elbows slightly in front of torso."),
        ex("ex_lateral_raise", "Dumbbell Lateral Raise", "SHOULDERS", "Upper Traps", "Dumbbell", "Beginner", "Raise dumbbells to shoulder height leading gently with elbows."),
        ex("ex_face_pull", "Cable Face Pull", "SHOULDERS", "Rear Delts, External Rotators", "Cable", "Beginner", "Pull rope toward forehead while externally rotating forearms."),
        ex("ex_rear_delt_fly", "Rear Delt Machine Fly", "SHOULDERS", "Mid-Back", "Machine", "Beginner", "Sweep arms back horizontally without shrugging traps."),

        // Quads / Legs
        ex("ex_squat", "Barbell Back Squat", "QUADS", "Glutes, Core, Adductors", "Barbell", "Intermediate", "Brace deeply, sit between knees to parallel or below, drive up balanced."),
        ex("ex_leg_press", "45° Leg Press", "QUADS", "Glutes", "Machine", "Beginner", "Keep lower back pinned to pad, lower sled deep without rounding pelvis."),
        ex("ex_bulgarian_split", "Bulgarian Split Squat", "QUADS", "Glutes, Hamstrings", "Dumbbell", "Intermediate", "Rear foot elevated, drop back knee straight down under control."),
        ex("ex_leg_extension", "Leg Extension", "QUADS", "", "Machine", "Beginner", "Pause 1 second at full knee extension for maximal quad tension."),

        // Hamstrings & Glutes
        ex("ex_rdl", "Romanian Deadlift (RDL)", "HAMSTRINGS", "Glutes, Lower Back", "Barbell", "Intermediate", "Soft knees, push hips straight back until deep hamstring stretch, squeeze glutes to stand."),
        ex("ex_leg_curl", "Lying / Seated Leg Curl", "HAMSTRINGS", "Calves", "Machine", "Beginner", "Keep hips planted firmly against pad throughout each rep."),
        ex("ex_hip_thrust", "Barbell Hip Thrust", "GLUTES", "Hamstrings", "Barbell", "Intermediate", "Tuck chin, drive through heels, lock out hips horizontally with 1s squeeze."),
        ex("ex_calf_raise", "Standing Calf Raise", "CALVES", "", "Machine", "Beginner", "Full bottom stretch pause, rise high onto balls of feet."),

        // Arms
        ex("ex_barbell_curl", "Barbell / EZ-Bar Curl", "BICEPS", "Forearms", "Barbell", "Beginner", "Keep elbows pinned beside ribs, curl smoothly without body swing."),
        ex("ex_incline_curl", "Incline Dumbbell Curl", "BICEPS", "", "Dumbbell", "Beginner", "Arms hang behind torso on 45° incline for loaded long-head stretch."),
        ex("ex_hammer_curl", "Dumbbell Hammer Curl", "BICEPS", "Brachialis, Forearms", "Dumbbell", "Beginner", "Neutral grip, controlled curl to shoulder height."),
        ex("ex_triceps_pushdown", "Cable Triceps Pushdown", "TRICEPS", "", "Cable", "Beginner", "Elbows locked at sides, extend forearms fully and control return."),
        ex("ex_overhead_triceps", "Overhead Rope Triceps Extension", "TRICEPS", "", "Cable", "Beginner", "Stretch triceps fully behind head before extending."),
        ex("ex_skullcrusher", "EZ-Bar Skullcrusher", "TRICEPS", "", "Barbell", "Intermediate", "Lower bar just behind forehead, extend smoothly."),

        // Core
        ex("ex_hanging_leg_raise", "Hanging Leg Raise", "CORE", "Hip Flexors", "Bodyweight", "Intermediate", "Curl pelvis upward toward ribs rather than simply swinging legs."),
        ex("ex_cable_crunch", "Kneeling Cable Crunch", "CORE", "", "Cable", "Beginner", "Flex thoracic spine downward while keeping hips stationary."),
        ex("ex_plank", "Weighted Plank (Reps = Secs)", "CORE", "Shoulders", "Bodyweight", "Beginner", "Squeeze glutes and brace abs maintaining neutral spine.")
    )

    private fun ex(
        id: String,
        name: String,
        muscle: String,
        secondary: String,
        equipment: String,
        difficulty: String,
        instructions: String
    ) = ExerciseEntity(
        id = id,
        userId = "GLOBAL",
        name = name,
        muscleGroup = muscle,
        secondaryMuscles = secondary,
        equipment = equipment,
        difficulty = difficulty,
        instructions = instructions,
        custom = false,
        favorite = false
    )

    fun createStarterTemplates(userId: String, split: WorkoutSplit): List<WorkoutTemplateEntity> {
        val now = System.currentTimeMillis()
        fun tpl(name: String, desc: String, specs: List<TemplateExerciseSpec>) = WorkoutTemplateEntity(
            id = "tpl_${UUID.randomUUID().toString().take(8)}",
            userId = userId,
            name = name,
            description = desc,
            exercisesJson = SeedSerialization.encodeTemplateExercises(specs),
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        return when (split) {
            WorkoutSplit.PPL -> listOf(
                tpl("Push Day", "Chest, Front/Side Delts & Triceps", listOf(
                    TemplateExerciseSpec("ex_bench_press", "Barbell Bench Press", "CHEST", 4, 8, 60.0, 120),
                    TemplateExerciseSpec("ex_incline_db_press", "Incline Dumbbell Press", "CHEST", 3, 10, 22.5, 90),
                    TemplateExerciseSpec("ex_db_shoulder_press", "Seated Dumbbell Shoulder Press", "SHOULDERS", 3, 10, 17.5, 90),
                    TemplateExerciseSpec("ex_lateral_raise", "Dumbbell Lateral Raise", "SHOULDERS", 4, 15, 10.0, 60),
                    TemplateExerciseSpec("ex_triceps_pushdown", "Cable Triceps Pushdown", "TRICEPS", 3, 12, 25.0, 60)
                )),
                tpl("Pull Day", "Lats, Upper Back, Rear Delts & Biceps", listOf(
                    TemplateExerciseSpec("ex_lat_pulldown", "Lat Pulldown", "BACK", 4, 10, 55.0, 90),
                    TemplateExerciseSpec("ex_barbell_row", "Barbell Bent-Over Row", "BACK", 4, 8, 60.0, 120),
                    TemplateExerciseSpec("ex_seated_cable_row", "Seated Cable Row", "BACK", 3, 12, 50.0, 90),
                    TemplateExerciseSpec("ex_face_pull", "Cable Face Pull", "SHOULDERS", 3, 15, 20.0, 60),
                    TemplateExerciseSpec("ex_barbell_curl", "Barbell / EZ-Bar Curl", "BICEPS", 3, 10, 25.0, 60)
                )),
                tpl("Leg Day", "Quads, Hamstrings, Glutes & Calves", listOf(
                    TemplateExerciseSpec("ex_squat", "Barbell Back Squat", "QUADS", 4, 8, 80.0, 150),
                    TemplateExerciseSpec("ex_rdl", "Romanian Deadlift (RDL)", "HAMSTRINGS", 3, 10, 70.0, 120),
                    TemplateExerciseSpec("ex_leg_press", "45° Leg Press", "QUADS", 3, 12, 120.0, 90),
                    TemplateExerciseSpec("ex_leg_curl", "Lying / Seated Leg Curl", "HAMSTRINGS", 3, 12, 40.0, 60),
                    TemplateExerciseSpec("ex_calf_raise", "Standing Calf Raise", "CALVES", 4, 15, 50.0, 60)
                ))
            )
            WorkoutSplit.UPPER_LOWER -> listOf(
                tpl("Upper Body A", "Horizontal Push/Pull + Arms", listOf(
                    TemplateExerciseSpec("ex_bench_press", "Barbell Bench Press", "CHEST", 4, 8, 60.0, 120),
                    TemplateExerciseSpec("ex_barbell_row", "Barbell Bent-Over Row", "BACK", 4, 8, 60.0, 120),
                    TemplateExerciseSpec("ex_db_shoulder_press", "Seated Dumbbell Shoulder Press", "SHOULDERS", 3, 10, 17.5, 90),
                    TemplateExerciseSpec("ex_lat_pulldown", "Lat Pulldown", "BACK", 3, 10, 55.0, 90),
                    TemplateExerciseSpec("ex_barbell_curl", "Barbell / EZ-Bar Curl", "BICEPS", 3, 12, 25.0, 60),
                    TemplateExerciseSpec("ex_triceps_pushdown", "Cable Triceps Pushdown", "TRICEPS", 3, 12, 25.0, 60)
                )),
                tpl("Lower Body A", "Quad & Posterior Chain Focus", listOf(
                    TemplateExerciseSpec("ex_squat", "Barbell Back Squat", "QUADS", 4, 6, 80.0, 150),
                    TemplateExerciseSpec("ex_rdl", "Romanian Deadlift (RDL)", "HAMSTRINGS", 4, 10, 70.0, 120),
                    TemplateExerciseSpec("ex_bulgarian_split", "Bulgarian Split Squat", "QUADS", 3, 10, 15.0, 90),
                    TemplateExerciseSpec("ex_leg_curl", "Lying / Seated Leg Curl", "HAMSTRINGS", 3, 12, 40.0, 60),
                    TemplateExerciseSpec("ex_calf_raise", "Standing Calf Raise", "CALVES", 4, 15, 50.0, 60)
                ))
            )
            WorkoutSplit.BRO_SPLIT -> listOf(
                tpl("Chest Day", "Complete Chest Hypertrophy", listOf(
                    TemplateExerciseSpec("ex_bench_press", "Barbell Bench Press", "CHEST", 4, 8, 60.0, 120),
                    TemplateExerciseSpec("ex_incline_db_press", "Incline Dumbbell Press", "CHEST", 4, 10, 22.5, 90),
                    TemplateExerciseSpec("ex_cable_fly", "Cable Chest Fly", "CHEST", 3, 15, 15.0, 60),
                    TemplateExerciseSpec("ex_dips", "Weighted Chest Dips", "CHEST", 3, 10, 0.0, 90)
                )),
                tpl("Back Day", "Width & Thickness", listOf(
                    TemplateExerciseSpec("ex_deadlift", "Conventional Deadlift", "BACK", 3, 5, 100.0, 180),
                    TemplateExerciseSpec("ex_lat_pulldown", "Lat Pulldown", "BACK", 4, 10, 55.0, 90),
                    TemplateExerciseSpec("ex_seated_cable_row", "Seated Cable Row", "BACK", 4, 10, 50.0, 90),
                    TemplateExerciseSpec("ex_db_row", "Single-Arm Dumbbell Row", "BACK", 3, 12, 25.0, 60)
                )),
                tpl("Shoulders & Arms", "Delts, Biceps & Triceps", listOf(
                    TemplateExerciseSpec("ex_ohp", "Overhead Barbell Press", "SHOULDERS", 4, 8, 40.0, 120),
                    TemplateExerciseSpec("ex_lateral_raise", "Dumbbell Lateral Raise", "SHOULDERS", 4, 15, 10.0, 60),
                    TemplateExerciseSpec("ex_barbell_curl", "Barbell / EZ-Bar Curl", "BICEPS", 3, 10, 25.0, 60),
                    TemplateExerciseSpec("ex_overhead_triceps", "Overhead Rope Triceps Extension", "TRICEPS", 3, 12, 20.0, 60)
                )),
                tpl("Leg Day", "Full Lower Body", listOf(
                    TemplateExerciseSpec("ex_squat", "Barbell Back Squat", "QUADS", 4, 8, 80.0, 150),
                    TemplateExerciseSpec("ex_rdl", "Romanian Deadlift (RDL)", "HAMSTRINGS", 3, 10, 70.0, 120),
                    TemplateExerciseSpec("ex_leg_extension", "Leg Extension", "QUADS", 3, 15, 45.0, 60),
                    TemplateExerciseSpec("ex_leg_curl", "Lying / Seated Leg Curl", "HAMSTRINGS", 3, 12, 40.0, 60)
                ))
            )
            WorkoutSplit.FULL_BODY, WorkoutSplit.CUSTOM -> listOf(
                tpl("Full Body A", "Compound Strength Foundation", listOf(
                    TemplateExerciseSpec("ex_squat", "Barbell Back Squat", "QUADS", 3, 8, 70.0, 120),
                    TemplateExerciseSpec("ex_bench_press", "Barbell Bench Press", "CHEST", 3, 8, 60.0, 120),
                    TemplateExerciseSpec("ex_barbell_row", "Barbell Bent-Over Row", "BACK", 3, 10, 55.0, 90),
                    TemplateExerciseSpec("ex_lateral_raise", "Dumbbell Lateral Raise", "SHOULDERS", 3, 15, 10.0, 60),
                    TemplateExerciseSpec("ex_cable_crunch", "Kneeling Cable Crunch", "CORE", 3, 15, 25.0, 60)
                )),
                tpl("Full Body B", "Hinge, Overhead & Pull Focus", listOf(
                    TemplateExerciseSpec("ex_rdl", "Romanian Deadlift (RDL)", "HAMSTRINGS", 3, 8, 70.0, 120),
                    TemplateExerciseSpec("ex_ohp", "Overhead Barbell Press", "SHOULDERS", 3, 8, 40.0, 120),
                    TemplateExerciseSpec("ex_lat_pulldown", "Lat Pulldown", "BACK", 3, 10, 55.0, 90),
                    TemplateExerciseSpec("ex_incline_db_press", "Incline Dumbbell Press", "CHEST", 3, 10, 20.0, 90),
                    TemplateExerciseSpec("ex_hammer_curl", "Dumbbell Hammer Curl", "BICEPS", 3, 12, 12.5, 60)
                ))
            )
        }
    }
}
