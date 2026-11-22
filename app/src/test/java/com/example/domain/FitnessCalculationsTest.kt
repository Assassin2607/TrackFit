package com.example.domain

import com.example.domain.calc.AnalyticsEngines
import com.example.domain.calc.FitnessCalculations
import com.example.domain.calc.HistoricalSetRecord
import com.example.domain.calc.NutritionCoachingSeverity
import com.example.domain.calc.PrCategory
import com.example.domain.calc.WeightPoint
import com.example.domain.model.ActivityLevel
import com.example.domain.model.CardioActivityType
import com.example.domain.model.Gender
import com.example.domain.model.GoalType
import com.example.domain.model.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FitnessCalculationsTest {

    @Test
    fun bmr_mifflinStJeor_matchesFormulaForMaleFemaleOther() {
        // 10*80 + 6.25*180 - 5*25 + 5 = 800 + 1125 - 125 + 5 = 1805
        assertEquals(1805, FitnessCalculations.calculateBmr(80.0, 180.0, 25, Gender.MALE))
        // 10*60 + 6.25*165 - 5*30 - 161 = 600 + 1031.25 - 150 - 161 = 1320.25 -> 1320
        assertEquals(1320, FitnessCalculations.calculateBmr(60.0, 165.0, 30, Gender.FEMALE))
        // 10*70 + 6.25*170 - 5*28 - 78 = 700 + 1062.5 - 140 - 78 = 1544.5 -> 1545
        assertEquals(1545, FitnessCalculations.calculateBmr(70.0, 170.0, 28, Gender.OTHER))
    }

    @Test
    fun tdee_and_initialTargets_cutBulkMaintainAndSafeFloors() {
        val cut = FitnessCalculations.calculateInitialTargets(
            weightKg = 77.0,
            goalWeightKg = 70.0,
            heightCm = 175.0,
            age = 26,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATE,
            goalType = GoalType.CUT,
            desiredRateKgPerWeek = 0.45
        )
        assertTrue(cut.tdee > cut.bmr)
        assertTrue(cut.calorieTarget < cut.tdee)
        assertTrue(cut.calorieTarget >= 1500)
        assertTrue(cut.proteinTargetG in 120..200)

        val bulk = FitnessCalculations.calculateInitialTargets(
            weightKg = 70.0,
            goalWeightKg = 76.0,
            heightCm = 175.0,
            age = 24,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATE,
            goalType = GoalType.BULK,
            desiredRateKgPerWeek = 0.25
        )
        assertTrue(bulk.calorieTarget > bulk.tdee)

        val maintain = FitnessCalculations.calculateInitialTargets(
            weightKg = 65.0,
            goalWeightKg = 65.0,
            heightCm = 165.0,
            age = 28,
            gender = Gender.FEMALE,
            activityLevel = ActivityLevel.LIGHT,
            goalType = GoalType.MAINTAIN
        )
        assertEquals(maintain.tdee, maintain.calorieTarget)
    }

    @Test
    fun energyBalance_uses7700KcalPerKgEquivalent() {
        val eb = FitnessCalculations.calculateEnergyBalance(
            bmr = 1800,
            activityLevel = ActivityLevel.MODERATE,
            foodIntakeKcal = 2020,
            workoutBurnKcal = 300,
            cardioBurnKcal = 200
        )
        assertTrue(eb.estimatedBalanceKcal < 0)
        assertEquals(
            eb.estimatedBalanceKcal / FitnessCalculations.KCAL_PER_KG_TISSUE,
            eb.estimatedWeightChangeKg,
            0.0001
        )
    }

    @Test
    fun cardioEstimation_scalesWithInclineAndDuration() {
        val flatWalk = FitnessCalculations.estimateCardioCalories(
            activityType = CardioActivityType.WALKING,
            durationMinutes = 30,
            weightKg = 80.0,
            speedKmh = 5.0,
            inclinePercent = 0.0
        )
        val inclineWalk = FitnessCalculations.estimateCardioCalories(
            activityType = CardioActivityType.WALKING,
            durationMinutes = 30,
            weightKg = 80.0,
            speedKmh = 5.0,
            inclinePercent = 10.0
        )
        assertTrue(inclineWalk > flatWalk)
        assertTrue(flatWalk in 120..220)
    }

    @Test
    fun workoutVolume_and_epley1Rm_areExact() {
        assertEquals(800.0, FitnessCalculations.calculateSetVolume(80.0, 10), 0.01)
        assertEquals(100.0, FitnessCalculations.estimateOneRepMax(100.0, 1), 0.01)
        // 80 * (1 + 8/30) = 101.33
        assertEquals(101.33, FitnessCalculations.estimateOneRepMax(80.0, 8), 0.05)
    }

    @Test
    fun personalRecordDetector_preventsFalsePrsAndDetectsRealPrs() {
        // Brand-new exercise with no prior history should NOT spam false PRs
        val firstEver = AnalyticsEngines.detectPersonalRecord(
            exerciseId = "ex_bench_press",
            weightKg = 60.0,
            reps = 8,
            isWarmup = false,
            historicalSets = emptyList()
        )
        assertFalse(firstEver.isNewPr)

        val history = listOf(
            HistoricalSetRecord("ex_bench_press", 80.0, 8, warmup = false),
            HistoricalSetRecord("ex_bench_press", 80.0, 7, warmup = false)
        )

        // Warmup set never counts as PR
        val warmupHit = AnalyticsEngines.detectPersonalRecord("ex_bench_press", 90.0, 10, true, history)
        assertFalse(warmupHit.isNewPr)

        // Beating heaviest weight & 1RM triggers verified PR
        val realPr = AnalyticsEngines.detectPersonalRecord("ex_bench_press", 85.0, 8, false, history)
        assertTrue(realPr.isNewPr)
        assertTrue(PrCategory.HEAVIEST_WEIGHT in realPr.categories)
        assertTrue(PrCategory.BEST_1RM in realPr.categories)
    }

    @Test
    fun weightTrendCalculator_computesRolling7DayAvgAndGoalProjection() {
        val dayMs = 86_400_000L
        val baseMs = 1_700_000_000_000L
        // Steady ~0.5 kg/week cut over 15 days (77.0 -> 76.0)
        val points = (0..14).map { day ->
            WeightPoint(
                dateKey = "2026-11-${String.format("%02d", day + 1)}",
                weightKg = 77.0 - (day * (1.0 / 14.0)),
                epochMs = baseMs + day * dayMs
            )
        }
        val summary = AnalyticsEngines.calculateWeightTrend(points, goalWeightKg = 70.0, goalType = GoalType.CUT)
        assertNotNull(summary.rolling7DayAvgKg)
        assertNotNull(summary.trend14DayKgPerWeek)
        assertEquals(-0.5, summary.trend14DayKgPerWeek!!, 0.08)
        assertNotNull(summary.projectedWeeksToGoal)
        assertTrue(summary.projectedWeeksToGoal!! in 10..16)
    }

    @Test
    fun dailyScore_sumsDeterministicallyTo100() {
        val perfect = AnalyticsEngines.calculateDailyScore(
            caloriesConsumed = 2050,
            calorieTarget = 2100,
            proteinConsumedG = 165,
            proteinTargetG = 160,
            workoutCompletedToday = true,
            workoutsThisWeek = 4,
            plannedTrainingDaysPerWeek = 4,
            cardioMinutesToday = 25,
            cardioTargetMinutes = 20,
            hasLoggedFoodToday = true,
            hasLoggedWeightRecently = true,
            activeStreakDays = 7
        )
        assertEquals(100, perfect.totalScore)
        assertEquals(5, perfect.components.size)
        assertEquals(100, perfect.components.sumOf { it.earned })
    }

    @Test
    fun nutritionCoaching_providesCompassionateOptionsOnExcessAndUnderEating() {
        val excess = AnalyticsEngines.evaluateNutritionGuidance(
            goalType = GoalType.CUT,
            caloriesConsumed = 2600,
            calorieTarget = 2100,
            proteinConsumedG = 150,
            proteinTargetG = 160,
            hasLoggedFood = true
        )
        assertNotNull(excess)
        assertEquals(NutritionCoachingSeverity.WARNING, excess!!.severity)
        assertEquals(4, excess.options.size)

        val under = AnalyticsEngines.evaluateNutritionGuidance(
            goalType = GoalType.CUT,
            caloriesConsumed = 950,
            calorieTarget = 2100,
            proteinConsumedG = 60,
            proteinTargetG = 160,
            hasLoggedFood = true
        )
        assertNotNull(under)
        assertEquals(NutritionCoachingSeverity.SAFETY_ALERT, under!!.severity)
    }

    @Test
    fun unitConversions_roundTripAccurately() {
        val lb = FitnessCalculations.kgToDisplay(80.0, UnitSystem.IMPERIAL)
        assertEquals(80.0, FitnessCalculations.displayToKg(lb, UnitSystem.IMPERIAL), 0.001)
        val inches = FitnessCalculations.cmToDisplay(180.0, UnitSystem.IMPERIAL)
        assertEquals(180.0, FitnessCalculations.displayToCm(inches, UnitSystem.IMPERIAL), 0.001)
        val miles = FitnessCalculations.kmToDisplay(10.0, UnitSystem.IMPERIAL)
        assertEquals(10.0, FitnessCalculations.displayToKm(miles, UnitSystem.IMPERIAL), 0.001)
        val flOz = FitnessCalculations.mlToDisplayWater(1000, UnitSystem.IMPERIAL)
        assertEquals(1000, FitnessCalculations.displayWaterToMl(flOz, UnitSystem.IMPERIAL))
    }

    @Test
    fun waterTargets_follow35MlPerKgAndRespectCustomOverride() {
        // 70 kg * 35 ml/kg = 2450 ml
        assertEquals(2450, FitnessCalculations.calculateDefaultWaterTargetMl(70.0))
        // 77 kg * 35 ml/kg = 2695 ml
        assertEquals(2695, FitnessCalculations.calculateDefaultWaterTargetMl(77.0))
        // Safety floor (1500 ml) and ceiling (6000 ml)
        assertEquals(1500, FitnessCalculations.calculateDefaultWaterTargetMl(30.0))
        assertEquals(6000, FitnessCalculations.calculateDefaultWaterTargetMl(200.0))

        // Auto mode (customTargetMl == 0) uses weight-based formula
        assertEquals(2695, FitnessCalculations.resolveWaterTargetMl(0, 77.0))
        // Explicit custom target in [500, 10000] overrides auto calculation
        assertEquals(3200, FitnessCalculations.resolveWaterTargetMl(3200, 77.0))
        // Out-of-range custom values fall back to weight-based default
        assertEquals(2695, FitnessCalculations.resolveWaterTargetMl(200, 77.0))
        assertEquals(2695, FitnessCalculations.resolveWaterTargetMl(15000, 77.0))
    }

    @Test
    fun waterFormatting_supportsMetricAndImperial() {
        assertEquals("2.50 L", FitnessCalculations.formatWater(2500, UnitSystem.METRIC))
        assertEquals("750 ml", FitnessCalculations.formatWater(750, UnitSystem.METRIC))
        assertEquals("2.5L", FitnessCalculations.formatWaterCompact(2500, UnitSystem.METRIC))
        assertEquals("750ml", FitnessCalculations.formatWaterCompact(750, UnitSystem.METRIC))
        assertTrue(FitnessCalculations.formatWater(1000, UnitSystem.IMPERIAL).endsWith("fl oz"))
        assertTrue(FitnessCalculations.formatWaterCompact(1000, UnitSystem.IMPERIAL).endsWith("oz"))
    }

    @Test
    fun calorieEstimatedWeight_tracksDailyDeficitFromDay1Anchor() {
        val keys = listOf(
            "2026-11-01", "2026-11-02", "2026-11-03", "2026-11-04",
            "2026-11-05", "2026-11-06", "2026-11-07"
        )
        // Day 1 scale weight = 75.0 kg. Male, 175 cm, 26y, Moderate (~2660 kcal TDEE).
        // Eating 2000 kcal + 100 kcal workout ≈ -760 kcal/day deficit (~-0.098 kg/day).
        val scaleLogs = listOf(
            WeightPoint(dateKey = "2026-11-01", weightKg = 75.0, epochMs = 1_700_000_000_000L)
        )
        val dailyInputs = keys.associateWith { dk ->
            com.example.domain.calc.DailyNutritionActivityInput(
                dateKey = dk,
                foodCalories = 2000,
                workoutBurnKcal = 100,
                cardioBurnKcal = 0
            )
        }

        val summary = AnalyticsEngines.calculateCalorieEstimatedWeight(
            weightLogs = scaleLogs,
            fallbackWeightKg = 75.0,
            heightCm = 175.0,
            age = 26,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATE,
            dailyLogsByDateKey = dailyInputs,
            todayDateKey = "2026-11-07",
            orderedDateKeys = keys
        )

        assertEquals(75.0, summary.anchorWeightKg, 0.001)
        assertEquals(7, summary.dailySeries.size)
        assertEquals(7, summary.loggedNutritionDays)
        assertEquals(com.example.domain.calc.EstimatedWeightDirection.LOSING, summary.direction)
        assertTrue("Expected estimated weight below 75.0 kg, got ${summary.estimatedCurrentWeightKg}", summary.estimatedCurrentWeightKg < 74.6)
        assertTrue("Expected realistic 1-week drop near ~74.2-74.5 kg, got ${summary.estimatedCurrentWeightKg}", summary.estimatedCurrentWeightKg > 73.8)
        assertTrue(summary.deltaSinceAnchorKg < -0.4)
        assertTrue(summary.weeklyProjectedRateKg < -0.4)
    }

    @Test
    fun calorieEstimatedWeight_tracksSurplusAndUnloggedDaysHoldSteady() {
        val keys = listOf(
            "2026-11-01", "2026-11-02", "2026-11-03", "2026-11-04",
            "2026-11-05", "2026-11-06", "2026-11-07"
        )
        val scaleLogs = listOf(
            WeightPoint(dateKey = "2026-11-01", weightKg = 70.0, epochMs = 1_700_000_000_000L)
        )
        // Log surplus on 4 days, leave 3 days unlogged (should hold steady with deltaKg == 0.0)
        val dailyInputs = mapOf(
            "2026-11-01" to com.example.domain.calc.DailyNutritionActivityInput("2026-11-01", 3100, 0, 0),
            "2026-11-02" to com.example.domain.calc.DailyNutritionActivityInput("2026-11-02", 3100, 0, 0),
            "2026-11-04" to com.example.domain.calc.DailyNutritionActivityInput("2026-11-04", 3100, 0, 0),
            "2026-11-06" to com.example.domain.calc.DailyNutritionActivityInput("2026-11-06", 3100, 0, 0)
        )

        val summary = AnalyticsEngines.calculateCalorieEstimatedWeight(
            weightLogs = scaleLogs,
            fallbackWeightKg = 70.0,
            heightCm = 175.0,
            age = 26,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATE,
            dailyLogsByDateKey = dailyInputs,
            todayDateKey = "2026-11-07",
            orderedDateKeys = keys
        )

        assertEquals(com.example.domain.calc.EstimatedWeightDirection.GAINING, summary.direction)
        assertEquals(4, summary.loggedNutritionDays)
        assertTrue(summary.estimatedCurrentWeightKg > 70.15)
        // Day 3 (index 2) has no food logged -> deltaKg must be 0.0 and holds Day 2 weight
        val day3 = summary.dailySeries[2]
        assertFalse(day3.hasNutritionData)
        assertEquals(0.0, day3.deltaKg, 0.0001)
        assertEquals(summary.dailySeries[1].estimatedWeightKg, day3.estimatedWeightKg, 0.0001)
    }

    @Test
    fun calorieEstimatedWeight_reAnchorsOnMidWeekScaleCheckIn() {
        val keys = listOf("2026-11-01", "2026-11-02", "2026-11-03", "2026-11-04")
        val scaleLogs = listOf(
            WeightPoint(dateKey = "2026-11-01", weightKg = 75.0, epochMs = 1_700_000_000_000L),
            WeightPoint(dateKey = "2026-11-03", weightKg = 74.2, epochMs = 1_700_172_800_000L)
        )
        val summary = AnalyticsEngines.calculateCalorieEstimatedWeight(
            weightLogs = scaleLogs,
            fallbackWeightKg = 75.0,
            heightCm = 175.0,
            age = 26,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATE,
            dailyLogsByDateKey = emptyMap(),
            todayDateKey = "2026-11-04",
            orderedDateKeys = keys
        )
        assertEquals(74.2, summary.anchorWeightKg, 0.001)
        assertEquals(74.2, summary.estimatedCurrentWeightKg, 0.001)
        assertEquals(com.example.domain.calc.EstimatedWeightDirection.NEED_DATA, summary.direction)
    }

    @Test
    fun personalRecordDetector_supportsBodyweightRepPrs() {
        val firstEver = AnalyticsEngines.detectPersonalRecord(
            exerciseId = "ex_pushups",
            weightKg = 0.0,
            reps = 15,
            isWarmup = false,
            historicalSets = emptyList()
        )
        assertFalse(firstEver.isNewPr)

        val history = listOf(
            HistoricalSetRecord("ex_pushups", 0.0, 15, warmup = false),
            HistoricalSetRecord("ex_pushups", 0.0, 12, warmup = false)
        )
        val sameReps = AnalyticsEngines.detectPersonalRecord("ex_pushups", 0.0, 15, false, history)
        assertFalse(sameReps.isNewPr)

        val newRepPr = AnalyticsEngines.detectPersonalRecord("ex_pushups", 0.0, 20, false, history)
        assertTrue(newRepPr.isNewPr)
        assertTrue(PrCategory.MOST_REPS_AT_WEIGHT in newRepPr.categories)
    }

    @Test
    fun homeWorkoutCatalog_providesComprehensiveHomeExercisesAndCleanClassification() {
        val allBuiltIn = com.example.data.seed.SeedExerciseCatalog.getBuiltInExercises()
        val homeExercises = allBuiltIn.filter { com.example.data.seed.SeedExerciseCatalog.isHomeExercise(it) }
        assertTrue("Expected at least 35 built-in home exercises, got ${homeExercises.size}", homeExercises.size >= 35)
        val homeMuscles = homeExercises.map { it.muscleGroup }.toSet()
        assertTrue("CHEST" in homeMuscles)
        assertTrue("BACK" in homeMuscles)
        assertTrue("SHOULDERS" in homeMuscles)
        assertTrue("QUADS" in homeMuscles)
        assertTrue("GLUTES" in homeMuscles)
        assertTrue("CORE" in homeMuscles)
        assertTrue("FULL_BODY" in homeMuscles)

        val homeTpl = com.example.data.local.WorkoutTemplateEntity(
            id = "tpl_home_fullbody_1234",
            userId = "u1",
            name = "Home Full Body Foundation",
            description = "No-equipment routine",
            exercisesJson = "[]"
        )
        val gymTpl = com.example.data.local.WorkoutTemplateEntity(
            id = "tpl_ppl_push_1234",
            userId = "u1",
            name = "Push Day (Chest / Shoulders / Triceps)",
            description = "Barbell & dumbbell hypertrophy",
            exercisesJson = "[]"
        )
        assertTrue(com.example.data.seed.SeedExerciseCatalog.isHomeTemplate(homeTpl))
        assertFalse(com.example.data.seed.SeedExerciseCatalog.isHomeTemplate(gymTpl))
        assertTrue(com.example.data.seed.SeedExerciseCatalog.isHomeSession(homeTpl.id, homeTpl.name, setOf(homeTpl.id)))
        assertFalse(com.example.data.seed.SeedExerciseCatalog.isHomeSession(gymTpl.id, gymTpl.name, setOf(homeTpl.id)))
    }
}
