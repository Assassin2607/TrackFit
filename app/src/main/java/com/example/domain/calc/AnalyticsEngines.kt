package com.example.domain.calc

import com.example.domain.model.ActivityLevel
import com.example.domain.model.Gender
import com.example.domain.model.GoalType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class WeightPoint(
    val dateKey: String,
    val weightKg: Double,
    val epochMs: Long
)

data class WeightTrendSummary(
    val latestWeightKg: Double?,
    val rolling7DayAvgKg: Double?,
    val trend14DayKgPerWeek: Double?,
    val trend30DayKgPerWeek: Double?,
    val projectedWeeksToGoal: Int?,
    val rollingSeries: List<Pair<String, Double>>
)

enum class EstimatedWeightDirection {
    LOSING,
    GAINING,
    MAINTAINING,
    NEED_DATA
}

data class DailyNutritionActivityInput(
    val dateKey: String,
    val foodCalories: Int,
    val workoutBurnKcal: Int,
    val cardioBurnKcal: Int
)

data class DailyEstimatedWeightPoint(
    val dateKey: String,
    val shortLabel: String,
    val estimatedWeightKg: Double,
    val foodCalories: Int,
    val dailyBalanceKcal: Int,
    val deltaKg: Double,
    val hasNutritionData: Boolean,
    val actualScaleWeightKg: Double? = null
)

data class EstimatedWeightSummary(
    val anchorWeightKg: Double,
    val anchorDateKey: String,
    val daysSinceAnchor: Int,
    val estimatedCurrentWeightKg: Double,
    val deltaSinceAnchorKg: Double,
    val weeklyProjectedRateKg: Double,
    val loggedNutritionDays: Int,
    val direction: EstimatedWeightDirection,
    val dailySeries: List<DailyEstimatedWeightPoint>
)

data class DailyScoreComponent(
    val label: String,
    val earned: Int,
    val maxPoints: Int,
    val reason: String
)

data class DailyScoreBreakdown(
    val totalScore: Int,
    val nutritionPoints: Int,
    val proteinPoints: Int,
    val workoutPoints: Int,
    val cardioPoints: Int,
    val consistencyPoints: Int,
    val components: List<DailyScoreComponent>
)

enum class PrCategory(val label: String) {
    HEAVIEST_WEIGHT("Heaviest Weight"),
    BEST_1RM("Best Est. 1RM"),
    MOST_REPS_AT_WEIGHT("Most Reps at Weight"),
    HIGHEST_VOLUME("Highest Set Volume")
}

data class HistoricalSetRecord(
    val exerciseId: String,
    val weightKg: Double,
    val reps: Int,
    val warmup: Boolean
)

data class PrDetectionResult(
    val isNewPr: Boolean,
    val categories: List<PrCategory>,
    val estimated1RmKg: Double,
    val setVolumeKg: Double
)

enum class NutritionCoachingSeverity {
    POSITIVE,
    INFO,
    WARNING,
    SAFETY_ALERT
}

data class NutritionCoachingCard(
    val title: String,
    val message: String,
    val options: List<String>,
    val severity: NutritionCoachingSeverity
)

data class AdaptiveRecommendation(
    val title: String,
    val summary: String,
    val suggestion: String,
    val onTrack: Boolean
)

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val progressText: String
)

object AnalyticsEngines {

    /**
     * Computes 7-day rolling average, 14-day & 30-day linear weight trends (kg/week),
     * and conservative goal projection. Never overreacts to single-day scale spikes.
     */
    fun calculateWeightTrend(
        points: List<WeightPoint>,
        goalWeightKg: Double?,
        goalType: GoalType
    ): WeightTrendSummary {
        if (points.isEmpty()) {
            return WeightTrendSummary(null, null, null, null, null, emptyList())
        }
        val sorted = points.sortedBy { it.epochMs }
        val latest = sorted.last().weightKg

        // Build rolling 7-entry (or 7-day window) average series
        val rollingSeries = sorted.mapIndexed { idx, pt ->
            val windowStartMs = pt.epochMs - (6L * 86_400_000L)
            val window = sorted.subList(0, idx + 1).filter { it.epochMs >= windowStartMs }
            val avg = window.map { it.weightKg }.average()
            pt.dateKey to avg
        }
        val rolling7 = rollingSeries.lastOrNull()?.second

        val latestMs = sorted.last().epochMs
        val trend14 = computeWeeklySlopeKg(sorted.filter { it.epochMs >= latestMs - 14L * 86_400_000L })
        val trend30 = computeWeeklySlopeKg(sorted.filter { it.epochMs >= latestMs - 30L * 86_400_000L })

        val primaryRate = trend14 ?: trend30
        val projectedWeeks = if (goalWeightKg != null && rolling7 != null && primaryRate != null && abs(primaryRate) >= 0.05) {
            val remaining = goalWeightKg - rolling7
            val movingTowardGoal = when (goalType) {
                GoalType.CUT -> remaining < 0 && primaryRate < 0
                GoalType.BULK -> remaining > 0 && primaryRate > 0
                GoalType.MAINTAIN -> false
            }
            if (movingTowardGoal) {
                (abs(remaining) / abs(primaryRate)).roundToInt().coerceIn(1, 260)
            } else {
                null
            }
        } else {
            null
        }

        return WeightTrendSummary(
            latestWeightKg = latest,
            rolling7DayAvgKg = rolling7,
            trend14DayKgPerWeek = trend14,
            trend30DayKgPerWeek = trend30,
            projectedWeeksToGoal = projectedWeeks,
            rollingSeries = rollingSeries
        )
    }

    private fun computeWeeklySlopeKg(window: List<WeightPoint>): Double? {
        if (window.size < 2) return null
        val firstMs = window.first().epochMs
        val spanDays = (window.last().epochMs - firstMs) / 86_400_000.0
        if (spanDays < 2.0) return null

        // Ordinary least squares slope in kg/day -> kg/week
        val xs = window.map { (it.epochMs - firstMs) / 86_400_000.0 }
        val ys = window.map { it.weightKg }
        val xMean = xs.average()
        val yMean = ys.average()
        var num = 0.0
        var den = 0.0
        for (i in xs.indices) {
            val dx = xs[i] - xMean
            num += dx * (ys[i] - yMean)
            den += dx * dx
        }
        if (den <= 1e-6) return null
        val kgPerDay = num / den
        return (kgPerDay * 7.0).coerceIn(-3.0, 3.0)
    }

    /**
     * Estimates daily body weight progression from the user's scale anchor(s) and daily
     * nutrition + training calorie balance (7,700 kcal ≈ 1 kg tissue).
     * Days without any food logged hold the running estimate steady so missed logging days
     * never fabricate fake weight loss.
     */
    fun calculateCalorieEstimatedWeight(
        weightLogs: List<WeightPoint>,
        fallbackWeightKg: Double,
        heightCm: Double,
        age: Int,
        gender: Gender,
        activityLevel: ActivityLevel,
        dailyLogsByDateKey: Map<String, DailyNutritionActivityInput>,
        todayDateKey: String,
        orderedDateKeys: List<String> = (29 downTo 0).map { FitnessCalculations.dateKeyDaysAgo(it) }
    ): EstimatedWeightSummary {
        val keys = orderedDateKeys.ifEmpty { listOf(todayDateKey) }
        val sortedLogs = weightLogs.sortedBy { it.epochMs }
        val scaleByDate = sortedLogs
            .groupBy { it.dateKey }
            .mapValues { (_, list) -> list.maxBy { it.epochMs }.weightKg }

        val latestScalePoint = sortedLogs.lastOrNull { it.dateKey <= todayDateKey } ?: sortedLogs.lastOrNull()
        val anchorWeightKg = latestScalePoint?.weightKg ?: fallbackWeightKg.coerceIn(25.0, 350.0)
        val anchorDateKey = latestScalePoint?.dateKey ?: todayDateKey

        // Find starting weight at the beginning of the evaluation window
        val earliestPriorScale = sortedLogs.lastOrNull { it.dateKey < keys.first() }
        val firstInWindowIdx = keys.indexOfFirst { scaleByDate.containsKey(it) }
        val startIdx = when {
            earliestPriorScale != null -> 0
            firstInWindowIdx >= 0 -> firstInWindowIdx
            else -> max(0, keys.size - 7)
        }
        var runningWeightKg = when {
            earliestPriorScale != null -> earliestPriorScale.weightKg
            firstInWindowIdx >= 0 -> scaleByDate[keys[firstInWindowIdx]] ?: anchorWeightKg
            else -> anchorWeightKg
        }.coerceIn(25.0, 350.0)

        val evaluatedPoints = mutableListOf<DailyEstimatedWeightPoint>()
        for (idx in startIdx until keys.size) {
            val dk = keys[idx]
            val scaleKg = scaleByDate[dk]
            if (scaleKg != null) {
                runningWeightKg = scaleKg.coerceIn(25.0, 350.0)
            }

            val input = dailyLogsByDateKey[dk]
            val foodKcal = input?.foodCalories ?: 0
            val hasNutrition = foodKcal > 0
            var balanceKcal = 0
            var deltaKg = 0.0

            if (hasNutrition) {
                val bmr = FitnessCalculations.calculateBmr(runningWeightKg, heightCm, age, gender)
                val eb = FitnessCalculations.calculateEnergyBalance(
                    bmr = bmr,
                    activityLevel = activityLevel,
                    foodIntakeKcal = foodKcal,
                    workoutBurnKcal = input?.workoutBurnKcal ?: 0,
                    cardioBurnKcal = input?.cardioBurnKcal ?: 0
                )
                balanceKcal = eb.estimatedBalanceKcal
                // Cap single-day tissue shift at ±0.18 kg so partial-day morning logs never swing wildly
                deltaKg = (eb.estimatedWeightChangeKg).coerceIn(-0.18, 0.18)
                runningWeightKg = (runningWeightKg + deltaKg).coerceIn(25.0, 350.0)
            }

            evaluatedPoints.add(
                DailyEstimatedWeightPoint(
                    dateKey = dk,
                    shortLabel = dk.takeLast(2),
                    estimatedWeightKg = runningWeightKg,
                    foodCalories = foodKcal,
                    dailyBalanceKcal = balanceKcal,
                    deltaKg = deltaKg,
                    hasNutritionData = hasNutrition,
                    actualScaleWeightKg = scaleKg
                )
            )
        }

        val estimatedCurrentKg = evaluatedPoints.lastOrNull()?.estimatedWeightKg ?: anchorWeightKg
        val deltaSinceAnchor = estimatedCurrentKg - anchorWeightKg

        val anchorIdxInKeys = keys.indexOf(anchorDateKey)
        val daysSinceAnchor = if (anchorIdxInKeys >= 0) {
            (keys.lastIndex - anchorIdxInKeys).coerceAtLeast(0)
        } else {
            keys.size
        }

        val trailing7 = evaluatedPoints.takeLast(7)
        val loggedDaysInWindow = trailing7.filter { it.hasNutritionData }
        val loggedCount = loggedDaysInWindow.size
        val weeklyRateKg = if (loggedCount > 0) {
            val avgDailyDelta = loggedDaysInWindow.map { it.deltaKg }.average()
            (avgDailyDelta * 7.0).coerceIn(-1.5, 1.5)
        } else {
            0.0
        }

        val direction = when {
            loggedCount == 0 -> EstimatedWeightDirection.NEED_DATA
            weeklyRateKg <= -0.10 -> EstimatedWeightDirection.LOSING
            weeklyRateKg >= 0.10 -> EstimatedWeightDirection.GAINING
            else -> EstimatedWeightDirection.MAINTAINING
        }

        return EstimatedWeightSummary(
            anchorWeightKg = anchorWeightKg,
            anchorDateKey = anchorDateKey,
            daysSinceAnchor = daysSinceAnchor,
            estimatedCurrentWeightKg = estimatedCurrentKg,
            deltaSinceAnchorKg = deltaSinceAnchor,
            weeklyProjectedRateKg = weeklyRateKg,
            loggedNutritionDays = loggedCount,
            direction = direction,
            dailySeries = trailing7
        )
    }

    /**
     * Transparent, deterministic Daily Score out of 100:
     * - Nutrition Adherence: 25 pts
     * - Protein Target: 20 pts
     * - Strength Workout: 25 pts
     * - Cardio Activity: 15 pts
     * - Daily Consistency: 15 pts
     */
    fun calculateDailyScore(
        caloriesConsumed: Int,
        calorieTarget: Int,
        proteinConsumedG: Int,
        proteinTargetG: Int,
        workoutCompletedToday: Boolean,
        workoutsThisWeek: Int,
        plannedTrainingDaysPerWeek: Int,
        cardioMinutesToday: Int,
        cardioTargetMinutes: Int,
        hasLoggedFoodToday: Boolean,
        hasLoggedWeightRecently: Boolean,
        activeStreakDays: Int
    ): DailyScoreBreakdown {
        // 1. Nutrition (25 pts): Full points within ±10% of calorie target; smooth taper beyond
        val nutritionPts = if (!hasLoggedFoodToday || calorieTarget <= 0) {
            0
        } else {
            val deviationRatio = abs(caloriesConsumed - calorieTarget).toDouble() / calorieTarget.toDouble()
            when {
                deviationRatio <= 0.10 -> 25
                deviationRatio >= 0.45 -> 5
                else -> {
                    val normalized = 1.0 - ((deviationRatio - 0.10) / 0.35)
                    (5 + (20.0 * normalized)).roundToInt().coerceIn(5, 25)
                }
            }
        }
        val nutritionReason = when {
            !hasLoggedFoodToday -> "Log meals to earn calorie adherence points"
            nutritionPts >= 23 -> "Within ±10% of your $calorieTarget kcal target"
            caloriesConsumed > calorieTarget -> "${caloriesConsumed - calorieTarget} kcal above target"
            else -> "${calorieTarget - caloriesConsumed} kcal below target"
        }

        // 2. Protein (20 pts): Linear up to 100% of protein target
        val proteinPts = if (proteinTargetG <= 0) 0 else {
            ((proteinConsumedG.toDouble() / proteinTargetG.toDouble()).coerceIn(0.0, 1.0) * 20.0).roundToInt()
        }
        val proteinReason = "${proteinConsumedG}g / ${proteinTargetG}g protein target"

        // 3. Workout (25 pts): Full points if trained today or weekly training frequency already met
        val weeklyGoalMet = plannedTrainingDaysPerWeek > 0 && workoutsThisWeek >= plannedTrainingDaysPerWeek
        val workoutPts = when {
            workoutCompletedToday -> 25
            weeklyGoalMet -> 25
            workoutsThisWeek > 0 -> ((workoutsThisWeek.toDouble() / max(1, plannedTrainingDaysPerWeek)) * 18.0)
                .roundToInt().coerceIn(0, 20)
            else -> 0
        }
        val workoutReason = when {
            workoutCompletedToday -> "Strength session completed today"
            weeklyGoalMet -> "Weekly target ($workoutsThisWeek/$plannedTrainingDaysPerWeek) achieved — recovery day"
            else -> "$workoutsThisWeek of $plannedTrainingDaysPerWeek weekly sessions completed"
        }

        // 4. Cardio (15 pts)
        val cardioPts = if (cardioTargetMinutes <= 0) {
            15
        } else {
            ((cardioMinutesToday.toDouble() / cardioTargetMinutes.toDouble()).coerceIn(0.0, 1.0) * 15.0).roundToInt()
        }
        val cardioReason = if (cardioTargetMinutes <= 0) {
            "No mandatory daily cardio target set"
        } else {
            "$cardioMinutesToday / $cardioTargetMinutes min logged today"
        }

        // 5. Consistency (15 pts): 6 pts food logged + 5 pts recent weigh-in + 4 pts streak >= 2 days
        val foodCheckPts = if (hasLoggedFoodToday) 6 else 0
        val weightCheckPts = if (hasLoggedWeightRecently) 5 else 0
        val streakPts = min(4, activeStreakDays)
        val consistencyPts = (foodCheckPts + weightCheckPts + streakPts).coerceIn(0, 15)
        val consistencyReason = buildList {
            if (hasLoggedFoodToday) add("food logged")
            if (hasLoggedWeightRecently) add("weight checked")
            if (activeStreakDays > 0) add("${activeStreakDays}d streak")
        }.ifEmpty { listOf("Log food or weight to build consistency") }.joinToString(" • ")

        val total = (nutritionPts + proteinPts + workoutPts + cardioPts + consistencyPts).coerceIn(0, 100)
        return DailyScoreBreakdown(
            totalScore = total,
            nutritionPoints = nutritionPts,
            proteinPoints = proteinPts,
            workoutPoints = workoutPts,
            cardioPoints = cardioPts,
            consistencyPoints = consistencyPts,
            components = listOf(
                DailyScoreComponent("Nutrition", nutritionPts, 25, nutritionReason),
                DailyScoreComponent("Protein", proteinPts, 20, proteinReason),
                DailyScoreComponent("Workout", workoutPts, 25, workoutReason),
                DailyScoreComponent("Cardio", cardioPts, 15, cardioReason),
                DailyScoreComponent("Consistency", consistencyPts, 15, consistencyReason)
            )
        )
    }

    /**
     * Strictly verifies whether a completed working set beats genuine historical records.
     * Warm-up sets or invalid sets never claim PRs.
     */
    fun detectPersonalRecord(
        exerciseId: String,
        weightKg: Double,
        reps: Int,
        isWarmup: Boolean,
        historicalSets: List<HistoricalSetRecord>
    ): PrDetectionResult {
        val est1Rm = FitnessCalculations.estimateOneRepMax(weightKg, reps)
        val volume = FitnessCalculations.calculateSetVolume(weightKg, reps)
        if (isWarmup || reps <= 0 || weightKg < 0.0) {
            return PrDetectionResult(false, emptyList(), est1Rm, volume)
        }

        // Bodyweight PR support: beating previous max reps on bodyweight (0 kg) sets
        if (weightKg == 0.0) {
            val priorBw = historicalSets.filter { it.exerciseId == exerciseId && !it.warmup && it.weightKg <= 0.01 && it.reps > 0 }
            if (priorBw.isEmpty()) {
                return PrDetectionResult(false, emptyList(), 0.0, 0.0)
            }
            val maxPriorBwReps = priorBw.maxOf { it.reps }
            val hits = if (reps > maxPriorBwReps) listOf(PrCategory.MOST_REPS_AT_WEIGHT) else emptyList()
            return PrDetectionResult(
                isNewPr = hits.isNotEmpty(),
                categories = hits,
                estimated1RmKg = 0.0,
                setVolumeKg = 0.0
            )
        }

        val priorWorking = historicalSets.filter { it.exerciseId == exerciseId && !it.warmup && it.weightKg > 0 && it.reps > 0 }
        // Require at least one prior logged set to declare a beaten PR (prevents false PR spam on brand-new exercises)
        if (priorWorking.isEmpty()) {
            return PrDetectionResult(false, emptyList(), est1Rm, volume)
        }

        val maxPriorWeight = priorWorking.maxOf { it.weightKg }
        val maxPrior1Rm = priorWorking.maxOf { FitnessCalculations.estimateOneRepMax(it.weightKg, it.reps) }
        val maxPriorVolume = priorWorking.maxOf { FitnessCalculations.calculateSetVolume(it.weightKg, it.reps) }
        val priorSameWeightMaxReps = priorWorking
            .filter { abs(it.weightKg - weightKg) <= 0.25 }
            .maxOfOrNull { it.reps }

        val hits = mutableListOf<PrCategory>()
        if (weightKg > maxPriorWeight + 0.01) hits.add(PrCategory.HEAVIEST_WEIGHT)
        if (est1Rm > maxPrior1Rm + 0.25) hits.add(PrCategory.BEST_1RM)
        if (priorSameWeightMaxReps != null && reps > priorSameWeightMaxReps) hits.add(PrCategory.MOST_REPS_AT_WEIGHT)
        if (volume > maxPriorVolume + 0.5) hits.add(PrCategory.HIGHEST_VOLUME)

        return PrDetectionResult(
            isNewPr = hits.isNotEmpty(),
            categories = hits,
            estimated1RmKg = est1Rm,
            setVolumeKg = volume
        )
    }

    /**
     * Compassionate, non-punitive nutrition guidance for Excess, Under-Eating, Bulking, and Maintenance.
     */
    fun evaluateNutritionGuidance(
        goalType: GoalType,
        caloriesConsumed: Int,
        calorieTarget: Int,
        proteinConsumedG: Int,
        proteinTargetG: Int,
        hasLoggedFood: Boolean
    ): NutritionCoachingCard? {
        if (!hasLoggedFood || calorieTarget <= 0) return null
        val diff = caloriesConsumed - calorieTarget

        // Excess calories (> +120 kcal above target)
        if (diff > 120) {
            return NutritionCoachingCard(
                title = "You are $diff kcal above today's target",
                message = "One higher-calorie day won't derail your progress. Avoid extreme restriction or punitive workouts.",
                options = listOf(
                    "Keep today unchanged and return to plan tomorrow",
                    "Maintain your normal scheduled training",
                    "Gently smooth ~${(diff / 3).coerceAtLeast(50)} kcal/day over the next 3 days if desired",
                    "Optional: take a light, enjoyable walk if it fits your day"
                ),
                severity = NutritionCoachingSeverity.WARNING
            )
        }

        // Under-eating safety check
        val minReasonableFloor = max(1200, (calorieTarget * 0.65).roundToInt())
        if (caloriesConsumed < minReasonableFloor && caloriesConsumed > 0) {
            val proteinGap = max(0, proteinTargetG - proteinConsumedG)
            return NutritionCoachingCard(
                title = "Intake is currently low ($caloriesConsumed / $calorieTarget kcal)",
                message = if (proteinGap > 25) {
                    "You still have ${calorieTarget - caloriesConsumed} kcal and ${proteinGap}g protein remaining. Sustained steep deficits can impair recovery and muscle retention."
                } else {
                    "You are well below your planned energy target. Consider adding a balanced meal so energy and recovery stay strong."
                },
                options = listOf(
                    "Prioritize hitting your protein target (${proteinTargetG}g)",
                    "Add a nutrient-dense meal with complex carbs and healthy fats",
                    "Never drop intake to extreme restriction levels"
                ),
                severity = NutritionCoachingSeverity.SAFETY_ALERT
            )
        }

        // Bulking below target
        if (goalType == GoalType.BULK && diff < -200) {
            val remaining = abs(diff)
            return NutritionCoachingCard(
                title = "You are $remaining kcal below today's bulk target",
                message = "Consistent modest surpluses support lean strength gains without excessive force-feeding.",
                options = listOf(
                    "Add an easy calorie bridge: oats + banana + whey or Greek yogurt + peanut butter",
                    "Pair an extra serving of rice or whole-grain toast with dinner",
                    "Focus on steady weekly averages rather than stuffing one meal"
                ),
                severity = NutritionCoachingSeverity.INFO
            )
        }

        // Maintenance stability
        if (goalType == GoalType.MAINTAIN && abs(diff) <= 150) {
            return NutritionCoachingCard(
                title = "Right in your maintenance zone",
                message = "Balanced energy intake paired with steady protein (${proteinConsumedG}g / ${proteinTargetG}g) supports body recomposition and training quality.",
                options = listOf("Keep protein steady and stay hydrated"),
                severity = NutritionCoachingSeverity.POSITIVE
            )
        }

        return null
    }

    /**
     * Weekly Adaptive Goal Engine: compares actual rolling weight trend against target rate.
     */
    fun generateAdaptiveRecommendation(
        goalType: GoalType,
        targetRateKgPerWeek: Double,
        actualTrendKgPerWeek: Double?,
        averageWeeklyCalories: Int?,
        calorieTarget: Int,
        workoutsLast7Days: Int,
        plannedWorkoutsPerWeek: Int
    ): AdaptiveRecommendation {
        if (actualTrendKgPerWeek == null) {
            return AdaptiveRecommendation(
                title = "Building Your Baseline Trend",
                summary = "Log weigh-ins across at least 4–7 days to unlock weekly trend adjustments.",
                suggestion = "Weigh in under consistent conditions (morning, after waking) and focus on hitting your current $calorieTarget kcal target.",
                onTrack = true
            )
        }

        return when (goalType) {
            GoalType.CUT -> {
                val actualLossRate = -actualTrendKgPerWeek // positive means losing weight
                when {
                    actualLossRate < targetRateKgPerWeek - 0.15 -> AdaptiveRecommendation(
                        title = "Cut Trend Slower Than Target",
                        summary = "Actual trend is ${String.format("%.2f", actualTrendKgPerWeek)} kg/wk vs target -${String.format("%.2f", targetRateKgPerWeek)} kg/wk.",
                        suggestion = "Consider a modest 100–150 kcal daily reduction or adding 10–15 minutes of low-impact walking—avoid drastic cuts.",
                        onTrack = false
                    )
                    actualLossRate > targetRateKgPerWeek + 0.25 -> AdaptiveRecommendation(
                        title = "Losing Faster Than Planned",
                        summary = "Actual trend is ${String.format("%.2f", actualTrendKgPerWeek)} kg/wk (faster than -${String.format("%.2f", targetRateKgPerWeek)} kg/wk target).",
                        suggestion = "Consider adding +100–150 kcal (mostly carbs/protein) to protect muscle mass and gym performance.",
                        onTrack = false
                    )
                    else -> AdaptiveRecommendation(
                        title = "On Track Toward Your Cut Goal",
                        summary = "Your rolling trend (${String.format("%.2f", actualTrendKgPerWeek)} kg/wk) aligns closely with your target rate.",
                        suggestion = "Keep calories around $calorieTarget kcal and maintain your $workoutsLast7Days/$plannedWorkoutsPerWeek weekly training cadence.",
                        onTrack = true
                    )
                }
            }
            GoalType.BULK -> {
                when {
                    actualTrendKgPerWeek < targetRateKgPerWeek - 0.12 -> AdaptiveRecommendation(
                        title = "Gain Trend Below Bulk Target",
                        summary = "Actual trend is ${String.format("%+.2f", actualTrendKgPerWeek)} kg/wk vs target +${String.format("%.2f", targetRateKgPerWeek)} kg/wk.",
                        suggestion = "Consider adding +100–150 kcal/day from quality carbs and protein while progressing compound lifts.",
                        onTrack = false
                    )
                    actualTrendKgPerWeek > targetRateKgPerWeek + 0.20 -> AdaptiveRecommendation(
                        title = "Gaining Faster Than Planned",
                        summary = "Actual trend is ${String.format("%+.2f", actualTrendKgPerWeek)} kg/wk vs target +${String.format("%.2f", targetRateKgPerWeek)} kg/wk.",
                        suggestion = "Trim surplus slightly by 100–150 kcal/day to keep lean mass gains efficient.",
                        onTrack = false
                    )
                    else -> AdaptiveRecommendation(
                        title = "Steady Lean Bulk Progress",
                        summary = "Rolling trend (${String.format("%+.2f", actualTrendKgPerWeek)} kg/wk) is right inside your controlled surplus zone.",
                        suggestion = "Stay consistent with $calorieTarget kcal/day and progressive overload.",
                        onTrack = true
                    )
                }
            }
            GoalType.MAINTAIN -> {
                if (abs(actualTrendKgPerWeek) <= 0.15) {
                    AdaptiveRecommendation(
                        title = "Weight Holding Steady",
                        summary = "Your 14-day rolling trend (${String.format("%+.2f", actualTrendKgPerWeek)} kg/wk) is stable.",
                        suggestion = "Keep protein high and focus on workout performance & recovery.",
                        onTrack = true
                    )
                } else {
                    val direction = if (actualTrendKgPerWeek > 0) "drifting upward" else "drifting downward"
                    val tweak = if (actualTrendKgPerWeek > 0) "-100 kcal/day" else "+100 kcal/day"
                    AdaptiveRecommendation(
                        title = "Maintenance Drift Detected",
                        summary = "Rolling trend is $direction (${String.format("%+.2f", actualTrendKgPerWeek)} kg/wk).",
                        suggestion = "A small adjustment of $tweak can gently recenter maintenance.",
                        onTrack = false
                    )
                }
            }
        }
    }

    /**
     * Evaluates authentic achievements derived exclusively from recorded data.
     */
    fun evaluateAchievements(
        totalWorkouts: Int,
        totalPrCount: Int,
        longestLoggingStreakDays: Int,
        heaviestBenchKg: Double,
        totalCardioSessions: Int,
        totalFoodLogs: Int
    ): List<AchievementItem> = listOf(
        AchievementItem(
            id = "first_workout",
            title = "First Workout",
            description = "Complete your first strength session",
            unlocked = totalWorkouts >= 1,
            progressText = "${min(totalWorkouts, 1)}/1"
        ),
        AchievementItem(
            id = "ten_workouts",
            title = "10 Workouts",
            description = "Log 10 completed gym sessions",
            unlocked = totalWorkouts >= 10,
            progressText = "${min(totalWorkouts, 10)}/10"
        ),
        AchievementItem(
            id = "fifty_workouts",
            title = "50 Workouts",
            description = "Log 50 completed gym sessions",
            unlocked = totalWorkouts >= 50,
            progressText = "${min(totalWorkouts, 50)}/50"
        ),
        AchievementItem(
            id = "first_pr",
            title = "First PR",
            description = "Beat a previous performance personal record",
            unlocked = totalPrCount >= 1,
            progressText = "${min(totalPrCount, 1)}/1"
        ),
        AchievementItem(
            id = "ten_prs",
            title = "10 Personal Records",
            description = "Achieve 10 verified strength PRs",
            unlocked = totalPrCount >= 10,
            progressText = "${min(totalPrCount, 10)}/10"
        ),
        AchievementItem(
            id = "consistency_7",
            title = "7-Day Consistency",
            description = "Track nutrition or training for 7 days straight",
            unlocked = longestLoggingStreakDays >= 7,
            progressText = "${min(longestLoggingStreakDays, 7)}/7d"
        ),
        AchievementItem(
            id = "consistency_30",
            title = "30-Day Consistency",
            description = "Maintain a 30-day logging streak",
            unlocked = longestLoggingStreakDays >= 30,
            progressText = "${min(longestLoggingStreakDays, 30)}/30d"
        ),
        AchievementItem(
            id = "bench_100kg",
            title = "100 kg Bench Club",
            description = "Log a working Bench Press set at 100 kg+",
            unlocked = heaviestBenchKg >= 100.0,
            progressText = "${heaviestBenchKg.roundToInt()}/100 kg"
        ),
        AchievementItem(
            id = "cardio_10",
            title = "Engine Builder",
            description = "Complete 10 cardio sessions",
            unlocked = totalCardioSessions >= 10,
            progressText = "${min(totalCardioSessions, 10)}/10"
        ),
        AchievementItem(
            id = "macro_tracker",
            title = "Macro Master",
            description = "Log 25 food or meal entries",
            unlocked = totalFoodLogs >= 25,
            progressText = "${min(totalFoodLogs, 25)}/25"
        )
    )
}
