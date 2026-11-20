package com.example.domain.model

enum class GoalType(val label: String, val subtitle: String) {
    CUT("Cut", "Gradual fat loss while preserving lean muscle"),
    BULK("Bulk", "Controlled calorie surplus for muscle gain"),
    MAINTAIN("Maintain", "Body recomposition & weight stability");

    companion object {
        fun fromNameSafe(raw: String?): GoalType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MAINTAIN
    }
}

enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other / Prefer not to say");

    companion object {
        fun fromNameSafe(raw: String?): Gender =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OTHER
    }
}

enum class ActivityLevel(val label: String, val subtitle: String, val multiplier: Double) {
    SEDENTARY("Sedentary", "Desk job, minimal daily walking", 1.20),
    LIGHT("Lightly Active", "Light daily movement + 1-2 workouts/wk", 1.375),
    MODERATE("Moderately Active", "Regular gym training 3-5 days/wk", 1.55),
    ACTIVE("Very Active", "Hard training 5-6 days/wk + active lifestyle", 1.725),
    VERY_ACTIVE("Athlete", "Physical job or 2x daily training", 1.90);

    companion object {
        fun fromNameSafe(raw: String?): ActivityLevel =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MODERATE
    }
}

enum class WorkoutSplit(val label: String, val subtitle: String) {
    PPL("Push Pull Legs", "3-6 days/wk classic hypertrophy split"),
    UPPER_LOWER("Upper / Lower", "4 days/wk balanced strength & size"),
    BRO_SPLIT("Body-Part Split", "5 days/wk dedicated muscle focus"),
    FULL_BODY("Full Body", "3 days/wk high-frequency compounds"),
    CUSTOM("Custom", "Build your own workout templates");

    companion object {
        fun fromNameSafe(raw: String?): WorkoutSplit =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PPL
    }
}

enum class CardioActivityType(
    val label: String,
    val baseMet: Double,
    val hasDistance: Boolean,
    val hasSpeed: Boolean,
    val hasIncline: Boolean,
    val hasResistance: Boolean
) {
    WALKING("Walking", 3.8, true, true, true, false),
    RUNNING("Running", 9.2, true, true, false, false),
    CYCLING("Cycling", 7.5, true, true, false, true),
    STAIRMASTER("Stairmaster", 8.8, false, false, false, true),
    ELLIPTICAL("Elliptical", 6.2, true, false, true, true),
    SWIMMING("Swimming", 8.0, true, false, false, false),
    ROWING("Rowing", 7.8, true, false, false, true),
    SKIPPING("Jump Rope", 10.5, false, false, false, false),
    CUSTOM("Custom Cardio", 6.0, true, false, false, false);

    companion object {
        fun fromNameSafe(raw: String?): CardioActivityType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: WALKING
    }
}

enum class MealType(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    POST_WORKOUT("Post Workout"),
    SNACK("Snack"),
    CUSTOM("Custom");

    companion object {
        fun fromNameSafe(raw: String?): MealType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: CUSTOM
    }
}

enum class PhotoType(val label: String) {
    FRONT("Front"),
    SIDE("Side"),
    BACK("Back");

    companion object {
        fun fromNameSafe(raw: String?): PhotoType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: FRONT
    }
}

enum class UnitSystem(val label: String, val weightUnit: String, val lengthUnit: String, val distanceUnit: String) {
    METRIC("Metric (kg, cm, km)", "kg", "cm", "km"),
    IMPERIAL("Imperial (lb, in, mi)", "lb", "in", "mi");

    companion object {
        fun fromNameSafe(raw: String?): UnitSystem =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: METRIC
    }
}

enum class MuscleGroup(val label: String) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    QUADS("Quads"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    CORE("Core"),
    CALVES("Calves"),
    FULL_BODY("Full Body");

    companion object {
        fun fromNameSafe(raw: String?): MuscleGroup =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) || it.label.equals(raw, ignoreCase = true) }
                ?: FULL_BODY
    }
}

sealed interface SyncState {
    data object Synced : SyncState
    data object Syncing : SyncState
    data class Pending(val count: Int, val lastError: String? = null) : SyncState
}
