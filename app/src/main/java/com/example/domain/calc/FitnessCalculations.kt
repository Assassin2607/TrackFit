package com.example.domain.calc

import com.example.domain.model.ActivityLevel
import com.example.domain.model.CardioActivityType
import com.example.domain.model.Gender
import com.example.domain.model.GoalType
import com.example.domain.model.UnitSystem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class MacroTargets(
    val bmr: Int,
    val tdee: Int,
    val calorieTarget: Int,
    val proteinTargetG: Int,
    val carbsTargetG: Int,
    val fatTargetG: Int,
    val fiberTargetG: Int,
    val targetRateKgPerWeek: Double,
    val dailyDeltaKcal: Int
)

data class EnergyBalanceEstimate(
    val estimatedMaintenanceKcal: Int,
    val foodIntakeKcal: Int,
    val workoutBurnKcal: Int,
    val cardioBurnKcal: Int,
    val totalExpenditureKcal: Int,
    val estimatedBalanceKcal: Int,          // intake - totalExpenditure (negative = deficit)
    val estimatedWeightChangeKg: Double     // estimatedBalanceKcal / 7700.0
)

object FitnessCalculations {

    const val KCAL_PER_KG_TISSUE = 7700.0
    const val KG_TO_LB = 2.20462262
    const val CM_TO_IN = 0.39370079
    const val KM_TO_MI = 0.62137119

    /**
     * Mifflin-St Jeor BMR formula:
     * BMR = 10 * weight(kg) + 6.25 * height(cm) - 5 * age + s
     * s = +5 (Male), -161 (Female), -78 (Other/Neutral midpoint)
     */
    fun calculateBmr(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        gender: Gender
    ): Int {
        val sexOffset = when (gender) {
            Gender.MALE -> 5.0
            Gender.FEMALE -> -161.0
            Gender.OTHER -> -78.0
        }
        val raw = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age) + sexOffset
        return raw.roundToInt().coerceIn(900, 4500)
    }

    /**
     * Estimated Total Daily Energy Expenditure (TDEE) = BMR * activityMultiplier
     */
    fun calculateTdee(bmr: Int, activityLevel: ActivityLevel): Int {
        return (bmr * activityLevel.multiplier).roundToInt().coerceIn(1100, 6500)
    }

    /**
     * Deterministic goal-calibrated daily calorie and macro targets.
     * Safeguards: never recommends extreme unsafe deficits below floor (1200 Female / 1500 Male / 1350 Other).
     */
    fun calculateInitialTargets(
        weightKg: Double,
        goalWeightKg: Double,
        heightCm: Double,
        age: Int,
        gender: Gender,
        activityLevel: ActivityLevel,
        goalType: GoalType,
        desiredRateKgPerWeek: Double? = null
    ): MacroTargets {
        val bmr = calculateBmr(weightKg, heightCm, age, gender)
        val tdee = calculateTdee(bmr, activityLevel)

        val defaultRate = when (goalType) {
            GoalType.CUT -> 0.45
            GoalType.BULK -> 0.25
            GoalType.MAINTAIN -> 0.0
        }
        val safeRate = (desiredRateKgPerWeek ?: defaultRate).coerceIn(0.0, 1.0)
        val dailyDelta = when (goalType) {
            GoalType.CUT -> -((safeRate * KCAL_PER_KG_TISSUE) / 7.0).roundToInt().coerceIn(200, 800)
            GoalType.BULK -> ((safeRate * KCAL_PER_KG_TISSUE) / 7.0).roundToInt().coerceIn(150, 500)
            GoalType.MAINTAIN -> 0
        }

        val minSafeCalories = when (gender) {
            Gender.MALE -> 1500
            Gender.FEMALE -> 1200
            Gender.OTHER -> 1350
        }
        val calorieTarget = (tdee + dailyDelta).coerceIn(minSafeCalories, 6000)

        // Protein target: 2.0 g/kg on Cut, 1.8 g/kg on Bulk/Maintain using reference weight
        // (capped to prevent unrealistically high targets at elevated BMI)
        val referenceWeight = when {
            goalType == GoalType.CUT && goalWeightKg < weightKg -> (weightKg * 0.4 + goalWeightKg * 0.6)
            else -> weightKg
        }.coerceIn(45.0, 130.0)

        val proteinFactor = when (goalType) {
            GoalType.CUT -> 2.1
            GoalType.BULK -> 1.85
            GoalType.MAINTAIN -> 1.9
        }
        val proteinTargetG = (referenceWeight * proteinFactor).roundToInt().coerceIn(60, 280)

        // Fat target: ~25-28% of calories (minimum 0.7 g/kg for hormonal health)
        val minFatG = (referenceWeight * 0.7).roundToInt().coerceAtLeast(35)
        val fatTargetG = max(minFatG, ((calorieTarget * 0.27) / 9.0).roundToInt())

        // Carbs: remainder of calorie budget
        val remainingKcalForCarbs = max(200, calorieTarget - (proteinTargetG * 4) - (fatTargetG * 9))
        val carbsTargetG = (remainingKcalForCarbs / 4.0).roundToInt()

        // Fiber: ~14g per 1000 kcal
        val fiberTargetG = ((calorieTarget / 1000.0) * 14.0).roundToInt().coerceIn(22, 50)

        return MacroTargets(
            bmr = bmr,
            tdee = tdee,
            calorieTarget = calorieTarget,
            proteinTargetG = proteinTargetG,
            carbsTargetG = carbsTargetG,
            fatTargetG = fatTargetG,
            fiberTargetG = fiberTargetG,
            targetRateKgPerWeek = safeRate,
            dailyDeltaKcal = calorieTarget - tdee
        )
    }

    /**
     * Calculates daily macro split when user manually overrides calorie or protein targets.
     */
    fun calculateMacrosForTargets(
        calorieTarget: Int,
        proteinTargetG: Int,
        weightKg: Double
    ): Triple<Int, Int, Int> { // carbs, fat, fiber
        val minFat = (weightKg.coerceIn(45.0, 120.0) * 0.7).roundToInt().coerceAtLeast(35)
        val fatG = max(minFat, ((calorieTarget * 0.27) / 9.0).roundToInt())
        val remainingKcal = max(120, calorieTarget - (proteinTargetG * 4) - (fatG * 9))
        val carbsG = (remainingKcal / 4.0).roundToInt()
        val fiberG = ((calorieTarget / 1000.0) * 14.0).roundToInt().coerceIn(20, 50)
        return Triple(carbsG, fatG, fiberG)
    }

    /**
     * Deterministic MET-based cardio expenditure estimate:
     * kcal = adjustedMET * weight(kg) * duration(hours)
     */
    fun estimateCardioCalories(
        activityType: CardioActivityType,
        durationMinutes: Int,
        weightKg: Double,
        speedKmh: Double = 0.0,
        inclinePercent: Double = 0.0,
        resistanceLevel: Int = 0
    ): Int {
        if (durationMinutes <= 0 || weightKg <= 0.0) return 0
        var met = activityType.baseMet
        when (activityType) {
            CardioActivityType.WALKING -> {
                if (speedKmh > 0) {
                    met = when {
                        speedKmh < 4.0 -> 2.8
                        speedKmh < 5.5 -> 3.8
                        speedKmh < 6.8 -> 4.8
                        else -> 6.0
                    }
                }
                if (inclinePercent > 0) {
                    met += inclinePercent.coerceIn(0.0, 20.0) * 0.42
                }
            }
            CardioActivityType.RUNNING -> {
                if (speedKmh > 0) {
                    met = (speedKmh * 0.98).coerceIn(6.5, 16.5)
                }
            }
            CardioActivityType.CYCLING -> {
                if (speedKmh > 0) {
                    met = when {
                        speedKmh < 16.0 -> 5.5
                        speedKmh < 20.0 -> 7.2
                        speedKmh < 24.0 -> 9.0
                        else -> 11.2
                    }
                }
                if (resistanceLevel > 0) {
                    met += (resistanceLevel.coerceIn(1, 25) * 0.15)
                }
            }
            CardioActivityType.STAIRMASTER, CardioActivityType.ELLIPTICAL, CardioActivityType.ROWING -> {
                if (resistanceLevel > 0) {
                    met += ((resistanceLevel - 5).coerceIn(-2, 15) * 0.22)
                }
                if (inclinePercent > 0) {
                    met += inclinePercent.coerceIn(0.0, 15.0) * 0.2
                }
            }
            else -> Unit
        }
        val hours = durationMinutes / 60.0
        return (met.coerceIn(2.0, 18.0) * weightKg * hours).roundToInt().coerceAtLeast(1)
    }

    /**
     * Deterministic strength workout expenditure estimate (~4.5 MET adjusted slightly by volume density).
     */
    fun estimateStrengthWorkoutCalories(
        durationMinutes: Int,
        weightKg: Double,
        workingSetCount: Int
    ): Int {
        if (durationMinutes <= 0 || weightKg <= 0.0) return 0
        val intensityMet = (3.8 + min(workingSetCount, 24) * 0.08).coerceIn(3.5, 6.2)
        return (intensityMet * weightKg * (durationMinutes / 60.0)).roundToInt()
    }

    /**
     * Computes transparent daily energy balance & estimated energy-equivalent weight change.
     * Note: Baseline TDEE reflects daily activity; extra logged workout/cardio above resting baseline
     * is surfaced transparently.
     */
    fun calculateEnergyBalance(
        bmr: Int,
        activityLevel: ActivityLevel,
        foodIntakeKcal: Int,
        workoutBurnKcal: Int,
        cardioBurnKcal: Int
    ): EnergyBalanceEstimate {
        // Use conservative non-exercise baseline (BMR * 1.25) + logged exercise when workouts/cardio are logged,
        // or full TDEE as reference maintenance if no exercise is logged, avoiding double-counting.
        val sedentaryBaseline = (bmr * 1.25).roundToInt()
        val fullTdee = calculateTdee(bmr, activityLevel)
        val totalExpenditure = max(fullTdee, sedentaryBaseline + workoutBurnKcal + cardioBurnKcal)
        val balance = foodIntakeKcal - totalExpenditure
        val weightEqKg = balance / KCAL_PER_KG_TISSUE
        return EnergyBalanceEstimate(
            estimatedMaintenanceKcal = fullTdee,
            foodIntakeKcal = foodIntakeKcal,
            workoutBurnKcal = workoutBurnKcal,
            cardioBurnKcal = cardioBurnKcal,
            totalExpenditureKcal = totalExpenditure,
            estimatedBalanceKcal = balance,
            estimatedWeightChangeKg = weightEqKg
        )
    }

    /**
     * Epley 1RM formula: weight * (1 + reps / 30.0)
     */
    fun estimateOneRepMax(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (1.0 + (reps / 30.0))
    }

    fun calculateSetVolume(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        return weightKg * reps
    }

    // --- Unit Conversion & Formatting ---

    fun kgToDisplay(kg: Double, unitSystem: UnitSystem): Double =
        if (unitSystem == UnitSystem.IMPERIAL) kg * KG_TO_LB else kg

    fun displayToKg(value: Double, unitSystem: UnitSystem): Double =
        if (unitSystem == UnitSystem.IMPERIAL) value / KG_TO_LB else value

    fun cmToDisplay(cm: Double, unitSystem: UnitSystem): Double =
        if (unitSystem == UnitSystem.IMPERIAL) cm * CM_TO_IN else cm

    fun displayToCm(value: Double, unitSystem: UnitSystem): Double =
        if (unitSystem == UnitSystem.IMPERIAL) value / CM_TO_IN else value

    fun kmToDisplay(km: Double, unitSystem: UnitSystem): Double =
        if (unitSystem == UnitSystem.IMPERIAL) km * KM_TO_MI else km

    fun displayToKm(value: Double, unitSystem: UnitSystem): Double =
        if (unitSystem == UnitSystem.IMPERIAL) value / KM_TO_MI else value

    fun formatWeight(kg: Double, unitSystem: UnitSystem, includeUnit: Boolean = true): String {
        val converted = kgToDisplay(kg, unitSystem)
        val formatted = String.format(Locale.US, "%.1f", converted)
        return if (includeUnit) "$formatted ${unitSystem.weightUnit}" else formatted
    }

    fun formatSignedWeightRate(kgPerWeek: Double, unitSystem: UnitSystem): String {
        val converted = kgToDisplay(kgPerWeek, unitSystem)
        val sign = if (converted > 0.005) "+" else ""
        return "$sign${String.format(Locale.US, "%.2f", converted)} ${unitSystem.weightUnit}/wk"
    }

    fun formatVolume(volumeKg: Double, unitSystem: UnitSystem): String {
        val converted = kgToDisplay(volumeKg, unitSystem)
        return if (converted >= 1000.0) {
            String.format(Locale.US, "%,.0f %s", converted, unitSystem.weightUnit)
        } else {
            String.format(Locale.US, "%.0f %s", converted, unitSystem.weightUnit)
        }
    }

    fun formatLength(cm: Double, unitSystem: UnitSystem): String {
        val converted = cmToDisplay(cm, unitSystem)
        return "${String.format(Locale.US, "%.1f", converted)} ${unitSystem.lengthUnit}"
    }

    fun formatDistance(km: Double, unitSystem: UnitSystem): String {
        val converted = kmToDisplay(km, unitSystem)
        return "${String.format(Locale.US, "%.2f", converted)} ${unitSystem.distanceUnit}"
    }

    fun todayDateKey(nowMs: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(nowMs))
    }

    fun dateKeyDaysAgo(daysAgo: Int, nowMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMs
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(cal.time)
    }

    fun calculateGoalProgressPercent(
        startWeightKg: Double,
        currentWeightKg: Double,
        goalWeightKg: Double
    ): Int {
        val totalDistance = abs(goalWeightKg - startWeightKg)
        if (totalDistance < 0.1) return 100
        val progressed = if (goalWeightKg < startWeightKg) {
            startWeightKg - currentWeightKg
        } else {
            currentWeightKg - startWeightKg
        }
        return ((progressed / totalDistance) * 100.0).roundToInt().coerceIn(0, 100)
    }
}
