package com.example.ui.nutrition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.FoodItemEntity
import com.example.data.local.SavedMealEntity
import com.example.data.local.WaterLogEntity
import com.example.data.seed.SavedMealItemSpec
import com.example.domain.calc.FitnessCalculations
import com.example.domain.calc.NutritionCoachingSeverity
import com.example.domain.model.MealType
import com.example.domain.model.UnitSystem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FitTrackCard
import com.example.ui.components.Hydration3DRing
import com.example.ui.components.MacroProgressBar
import com.example.ui.components.MetricBarChart
import com.example.ui.components.SectionHeader
import com.example.ui.theme.FitTrackThemeExt
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.NutritionAqua
import com.example.ui.theme.NutritionForestDark
import com.example.ui.theme.NutritionLime
import com.example.ui.theme.NutritionSageBg
import com.example.viewmodel.FitTrackUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun NutritionScreen(
    state: FitTrackUiState,
    onLogFood: (FoodItemEntity, Double, MealType) -> Unit,
    onToggleFoodFavorite: (FoodItemEntity) -> Unit,
    onCreateCustomFood: (String, String, Double, String, Double, Double, Double, Double, Double, MealType?) -> Unit,
    onSaveMealPreset: (String, MealType, List<SavedMealItemSpec>) -> Unit,
    onLogSavedMeal: (SavedMealEntity, Double, MealType?) -> Unit,
    onDeleteSavedMeal: (String) -> Unit,
    onDeleteFoodLog: (String) -> Unit,
    onLogWater: (Int) -> Unit,
    onDeleteWaterLog: (String) -> Unit,
    onUpdateWaterTargetMl: (Int) -> Unit
) {
    var selectedSegment by remember { mutableIntStateOf(0) }
    val segments = listOf("Diary", "Favorites", "Meals", "Foods")
    var showWeeklyBudget by remember { mutableStateOf(false) }

    var foodToLog by remember { mutableStateOf<FoodItemEntity?>(null) }
    var mealToLog by remember { mutableStateOf<SavedMealEntity?>(null) }
    var showCreateCustomFood by remember { mutableStateOf(false) }
    var showCreateSavedMeal by remember { mutableStateOf(false) }
    var showCustomWaterDialog by remember { mutableStateOf(false) }
    var showEditWaterTargetDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val profile = state.profile
    val unit = state.preferences.unitSystem
    val ext = FitTrackThemeExt.colors
    val calTarget = profile?.calorieTarget ?: 2100
    val proTarget = profile?.proteinTargetG ?: 160
    val todayLogs = remember(state.foodLogs, state.selectedDateKey) {
        state.foodLogs.filter { it.dateKey == state.selectedDateKey }
    }
    val todayWaterLogs = remember(state.waterLogs, state.selectedDateKey) {
        state.waterLogs.filter { it.dateKey == state.selectedDateKey }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("nutrition_screen")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Nutrition",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Macros & Daily Hydration • ${state.selectedDateKey}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.mutedText
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Weekly", style = MaterialTheme.typography.labelMedium, color = ext.mutedText)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = showWeeklyBudget,
                        onCheckedChange = { showWeeklyBudget = it },
                        modifier = Modifier.testTag("weekly_budget_toggle")
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                segments.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedSegment == idx,
                        onClick = { selectedSegment = idx },
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = NutritionLime,
                            activeContentColor = NutritionForestDark,
                            inactiveContainerColor = Color(0xFFF5FAF0),
                            inactiveContentColor = ext.mutedText,
                            activeBorderColor = MaterialTheme.colorScheme.primary,
                            inactiveBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = segments.size)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Daily Macro Summary 3D Botanical Card
            item {
                FitTrackCard(depthElevation = 6.dp) {
                    MacroProgressBar(
                        label = "Calories",
                        consumed = state.todayCalories,
                        target = calTarget,
                        unit = "kcal",
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MacroProgressBar(
                        label = "Protein",
                        consumed = state.todayProteinG,
                        target = proTarget,
                        unit = "g",
                        accentColor = ext.success
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroPill("Carbs", "${state.todayCarbsG}/${state.targetCarbsG}g", Modifier.weight(1f))
                        MacroPill("Fat", "${state.todayFatG}/${state.targetFatG}g", Modifier.weight(1f))
                        MacroPill("Fiber", "${state.todayFiberG}/${state.targetFiberG}g", Modifier.weight(1f))
                    }
                }
            }

            // Dedicated 3D Water Log Card (Botanical Lime Accent + Aqua Hydration Ring)
            item {
                WaterLogCard(
                    todayWaterMl = state.todayWaterMl,
                    waterTargetMl = state.waterTargetMl,
                    isAutoTarget = state.isAutoWaterTarget,
                    unitSystem = unit,
                    todayEntries = todayWaterLogs,
                    onQuickAdd = onLogWater,
                    onOpenCustom = { showCustomWaterDialog = true },
                    onOpenEditTarget = { showEditWaterTargetDialog = true },
                    onDeleteEntry = onDeleteWaterLog
                )
            }

            // Optional Weekly Calorie Budget View
            if (showWeeklyBudget) {
                item {
                    val weeklyRemaining = state.weeklyCalorieTarget - state.weeklyCalorieConsumed
                    FitTrackCard(modifier = Modifier.testTag("weekly_budget_card")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("7-Day Calorie Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Flexible distribution across the week",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ext.mutedText
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${state.weeklyCalorieConsumed} / ${state.weeklyCalorieTarget}",
                                    style = MonoMetricSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (weeklyRemaining >= 0) "$weeklyRemaining kcal remaining" else "+${-weeklyRemaining} kcal over",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.mutedText
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        MetricBarChart(
                            bars = state.weeklyCaloriesSeries.map { it.shortLabel to it.calories.toFloat() },
                            targetValue = calTarget.toFloat(),
                            barColor = MaterialTheme.colorScheme.primary,
                            unitSuffix = " kcal"
                        )
                    }
                }
            }

            // Compassionate Non-Punitive Coaching Card
            state.nutritionCoaching?.let { coach ->
                item {
                    val accent = when (coach.severity) {
                        NutritionCoachingSeverity.POSITIVE -> ext.success
                        NutritionCoachingSeverity.INFO -> MaterialTheme.colorScheme.primary
                        NutritionCoachingSeverity.WARNING -> ext.warning
                        NutritionCoachingSeverity.SAFETY_ALERT -> MaterialTheme.colorScheme.error
                    }
                    FitTrackCard(borderColor = accent.copy(alpha = 0.65f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (coach.severity == NutritionCoachingSeverity.SAFETY_ALERT || coach.severity == NutritionCoachingSeverity.WARNING) {
                                    Icons.Rounded.Warning
                                } else Icons.Rounded.Info,
                                contentDescription = null,
                                tint = accent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(coach.title, style = MaterialTheme.typography.titleSmall, color = accent, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(coach.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        coach.options.forEachIndexed { idx, opt ->
                            Text(
                                text = "${idx + 1}. $opt",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.mutedText,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            when (selectedSegment) {
                0 -> { // Diary
                    item {
                        SectionHeader(
                            title = "Today's Food Log",
                            subtitle = "${todayLogs.size} entries logged",
                            trailing = {
                                Button(
                                    onClick = { selectedSegment = 3 },
                                    modifier = Modifier.testTag("diary_add_food_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add")
                                }
                            }
                        )
                    }
                    if (todayLogs.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.Restaurant,
                                title = "Nothing logged today",
                                subtitle = "Use Favorites, Saved Meals, or the Food Database to log in seconds.",
                                actionLabel = "Browse Favorites",
                                onAction = { selectedSegment = 1 }
                            )
                        }
                    } else {
                        items(todayLogs, key = { it.id }) { log ->
                            FitTrackCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(log.foodName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "${MealType.fromNameSafe(log.mealType).label} • ${String.format("%.0f", log.quantity)} ${log.unit}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.mutedText
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${log.calories.roundToInt()} kcal • P ${log.protein.roundToInt()}g • C ${log.carbs.roundToInt()}g • F ${log.fat.roundToInt()}g",
                                            style = MonoMetricSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    IconButton(onClick = { onDeleteFoodLog(log.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete log", tint = ext.mutedText)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> { // Favorites & Frequent
                    val favs = state.foodItems.filter { it.favorite || it.useCount > 0 }
                    item {
                        SectionHeader(
                            title = "Favorites & Frequent Foods",
                            subtitle = "One-tap logging for your daily staples"
                        )
                    }
                    items(favs, key = { it.id }) { food ->
                        FoodItemRowCard(
                            food = food,
                            onLogClick = { foodToLog = food },
                            onToggleFav = { onToggleFoodFavorite(food) }
                        )
                    }
                }

                2 -> { // Saved Meals
                    item {
                        SectionHeader(
                            title = "Saved Meals",
                            subtitle = "Log multi-ingredient meals in one tap",
                            trailing = {
                                Button(
                                    onClick = { showCreateSavedMeal = true },
                                    modifier = Modifier.testTag("create_saved_meal_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Meal")
                                }
                            }
                        )
                    }
                    if (state.savedMeals.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.Bookmark,
                                title = "No saved meals yet",
                                subtitle = "Bundle foods like '4 Eggs + Whey + Banana' into 'My Breakfast' for instant daily logging.",
                                actionLabel = "Create Saved Meal",
                                onAction = { showCreateSavedMeal = true }
                            )
                        }
                    } else {
                        items(state.savedMeals, key = { it.id }) { meal ->
                            FitTrackCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(meal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "${MealType.fromNameSafe(meal.mealType).label} • ${meal.totalCalories.roundToInt()} kcal • ${meal.totalProtein.roundToInt()}g P • ${meal.totalCarbs.roundToInt()}g C • ${meal.totalFat.roundToInt()}g F",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ext.mutedText
                                        )
                                    }
                                    IconButton(onClick = { onDeleteSavedMeal(meal.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete saved meal", tint = ext.mutedText)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onLogSavedMeal(meal, 1.0, null) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Log 1×")
                                    }
                                    OutlinedButton(
                                        onClick = { mealToLog = meal },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Adjust Qty")
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> { // Curated Food Search + Custom Foods
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                placeholder = { Text("Search chicken, eggs, rice, oats...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("food_search_input")
                            )
                            Button(
                                onClick = { showCreateCustomFood = true },
                                modifier = Modifier.testTag("create_custom_food_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Create custom food")
                            }
                        }
                    }

                    val matches = state.foodItems.filter { food ->
                        searchQuery.isBlank() ||
                            food.name.contains(searchQuery, ignoreCase = true) ||
                            food.brand.contains(searchQuery, ignoreCase = true)
                    }

                    if (matches.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.Fastfood,
                                title = "No matching food found",
                                subtitle = "Create a custom food (e.g. Homemade Chicken Curry) and reuse it anytime.",
                                actionLabel = "Create Custom Food",
                                onAction = { showCreateCustomFood = true }
                            )
                        }
                    } else {
                        items(matches, key = { it.id }) { food ->
                            FoodItemRowCard(
                                food = food,
                                onLogClick = { foodToLog = food },
                                onToggleFav = { onToggleFoodFavorite(food) }
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showCustomWaterDialog) {
        CustomWaterDialog(
            unitSystem = unit,
            onDismiss = { showCustomWaterDialog = false },
            onConfirm = { amountMl ->
                onLogWater(amountMl)
                showCustomWaterDialog = false
            }
        )
    }

    if (showEditWaterTargetDialog) {
        EditWaterTargetDialog(
            currentTargetMl = state.waterTargetMl,
            autoTargetMl = FitnessCalculations.calculateDefaultWaterTargetMl(profile?.currentWeightKg ?: 75.0),
            unitSystem = unit,
            onDismiss = { showEditWaterTargetDialog = false },
            onSaveTarget = { targetMl ->
                onUpdateWaterTargetMl(targetMl)
                showEditWaterTargetDialog = false
            }
        )
    }

    foodToLog?.let { food ->
        LogFoodPortionDialog(
            food = food,
            onDismiss = { foodToLog = null },
            onConfirm = { qty, mealType ->
                onLogFood(food, qty, mealType)
                foodToLog = null
            }
        )
    }

    mealToLog?.let { meal ->
        AdjustSavedMealDialog(
            meal = meal,
            onDismiss = { mealToLog = null },
            onConfirm = { mult, mealType ->
                onLogSavedMeal(meal, mult, mealType)
                mealToLog = null
            }
        )
    }

    if (showCreateCustomFood) {
        CreateCustomFoodDialog(
            onDismiss = { showCreateCustomFood = false },
            onSave = { name, brand, serving, u, kcal, p, c, f, fib, logMeal ->
                onCreateCustomFood(name, brand, serving, u, kcal, p, c, f, fib, logMeal)
                showCreateCustomFood = false
            }
        )
    }

    if (showCreateSavedMeal) {
        CreateSavedMealDialog(
            foods = state.foodItems,
            onDismiss = { showCreateSavedMeal = false },
            onSave = { name, mealType, items ->
                onSaveMealPreset(name, mealType, items)
                showCreateSavedMeal = false
            }
        )
    }
}

@Composable
private fun WaterLogCard(
    todayWaterMl: Int,
    waterTargetMl: Int,
    isAutoTarget: Boolean,
    unitSystem: UnitSystem,
    todayEntries: List<WaterLogEntity>,
    onQuickAdd: (Int) -> Unit,
    onOpenCustom: () -> Unit,
    onOpenEditTarget: () -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    val ext = FitTrackThemeExt.colors
    val remainingMl = (waterTargetMl - todayWaterMl).coerceAtLeast(0)
    val quick250Label = if (unitSystem == UnitSystem.IMPERIAL) "+8 oz" else "+250 ml"
    val quick500Label = if (unitSystem == UnitSystem.IMPERIAL) "+17 oz" else "+500 ml"
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.US) }

    FitTrackCard(
        containerColor = Color(0xFFF6FBF1),
        accentGradientEnd = NutritionSageBg,
        borderColor = NutritionLime,
        depthElevation = 6.dp,
        modifier = Modifier.testTag("water_log_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NutritionLime,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = NutritionAqua,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Water Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NutritionForestDark
                    )
                    Text(
                        text = if (isAutoTarget) "Auto target (35 ml/kg)" else "Custom daily target",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.mutedText
                    )
                }
            }
            IconButton(
                onClick = onOpenEditTarget,
                modifier = Modifier.testTag("edit_water_target_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit daily water target",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Hydration3DRing(
                consumedMl = todayWaterMl,
                targetMl = waterTargetMl,
                compactLabel = FitnessCalculations.formatWaterCompact(todayWaterMl, unitSystem),
                size = 104.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${FitnessCalculations.formatWater(todayWaterMl, unitSystem)} / ${FitnessCalculations.formatWater(waterTargetMl, unitSystem)}",
                    style = MonoMetricMedium,
                    color = NutritionForestDark,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (remainingMl == 0) {
                        "Daily hydration goal reached!"
                    } else {
                        "${FitnessCalculations.formatWater(remainingMl, unitSystem)} remaining today"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remainingMl == 0) ext.success else ext.mutedText,
                    fontWeight = if (remainingMl == 0) FontWeight.SemiBold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { onQuickAdd(250) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NutritionLime,
                            contentColor = NutritionForestDark
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nutrition_water_add_250")
                    ) {
                        Text(quick250Label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onQuickAdd(500) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NutritionAqua,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nutrition_water_add_500")
                    ) {
                        Text(quick500Label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onOpenCustom,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("nutrition_water_add_custom")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom Amount", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (todayEntries.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            Text(
                text = "Today's Water Entries (${todayEntries.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = ext.mutedText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                todayEntries.take(5).forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = null,
                                tint = NutritionAqua,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+${FitnessCalculations.formatWater(entry.amountMl, unitSystem)}",
                                style = MonoMetricSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = NutritionForestDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = timeFormatter.format(Date(entry.loggedAtEpochMs)),
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.mutedText
                            )
                        }
                        IconButton(
                            onClick = { onDeleteEntry(entry.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete water entry",
                                tint = ext.mutedText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomWaterDialog(
    unitSystem: UnitSystem,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val unitLabel = FitnessCalculations.waterUnitLabel(unitSystem)
    var inputText by remember { mutableStateOf(if (unitSystem == UnitSystem.IMPERIAL) "12" else "350") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Water ($unitLabel)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text("Amount ($unitLabel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = inputText.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        val ml = FitnessCalculations.displayWaterToMl(parsed, unitSystem).coerceIn(1, 5000)
                        onConfirm(ml)
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditWaterTargetDialog(
    currentTargetMl: Int,
    autoTargetMl: Int,
    unitSystem: UnitSystem,
    onDismiss: () -> Unit,
    onSaveTarget: (Int) -> Unit
) {
    val unitLabel = FitnessCalculations.waterUnitLabel(unitSystem)
    val initialVal = FitnessCalculations.mlToDisplayWater(currentTargetMl, unitSystem).roundToInt().toString()
    var targetText by remember { mutableStateOf(initialVal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily Water Target ($unitLabel)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Auto recommendation based on your weight (35 ml/kg): ${FitnessCalculations.formatWater(autoTargetMl, unitSystem)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FitTrackThemeExt.colors.mutedText
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter(Char::isDigit).take(5) },
                    label = { Text("Custom Target ($unitLabel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { onSaveTarget(0) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to Auto (${FitnessCalculations.formatWater(autoTargetMl, unitSystem)})")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = targetText.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        val ml = FitnessCalculations.displayWaterToMl(parsed, unitSystem).coerceIn(500, 10000)
                        onSaveTarget(ml)
                    }
                }
            ) { Text("Save Target") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MacroPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = NutritionLime.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = FitTrackThemeExt.colors.mutedText)
            Text(value, style = MonoMetricSmall, color = NutritionForestDark, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FoodItemRowCard(
    food: FoodItemEntity,
    onLogClick: () -> Unit,
    onToggleFav: () -> Unit
) {
    FitTrackCard(onClick = onLogClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Per ${String.format("%.0f", food.servingSize)} ${food.servingUnit} • ${food.calories.roundToInt()} kcal • ${food.protein.roundToInt()}g P • ${food.carbs.roundToInt()}g C • ${food.fat.roundToInt()}g F",
                    style = MaterialTheme.typography.bodySmall,
                    color = FitTrackThemeExt.colors.mutedText
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFav) {
                    Icon(
                        imageVector = if (food.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "Favorite food",
                        tint = if (food.favorite) FitTrackThemeExt.colors.warning else FitTrackThemeExt.colors.mutedText
                    )
                }
                FilledTonalButton(
                    onClick = onLogClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("Log") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogFoodPortionDialog(
    food: FoodItemEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, MealType) -> Unit
) {
    var qtyText by remember { mutableStateOf(String.format("%.0f", food.servingSize)) }
    var mealType by remember { mutableStateOf(MealType.LUNCH) }
    val qty = qtyText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val ratio = if (food.servingSize > 0) qty / food.servingSize else 1.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("Quantity (${food.servingUnit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${(food.calories * ratio).roundToInt()} kcal", style = MonoMetricMedium, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "P ${(food.protein * ratio).roundToInt()}g • C ${(food.carbs * ratio).roundToInt()}g • F ${(food.fat * ratio).roundToInt()}g • Fiber ${(food.fiber * ratio).roundToInt()}g",
                            style = MonoMetricSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text("Meal Slot", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealType.entries.forEach { mt ->
                        FilterChip(
                            selected = mealType == mt,
                            onClick = { mealType = mt },
                            label = { Text(mt.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (qty > 0) onConfirm(qty, mealType) },
                enabled = qty > 0
            ) { Text("Log Food") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdjustSavedMealDialog(
    meal: SavedMealEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, MealType) -> Unit
) {
    var mult by remember { mutableDoubleStateOf(1.0) }
    var mealType by remember { mutableStateOf(MealType.fromNameSafe(meal.mealType)) }
    val presets = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 2.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ${meal.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Portion Multiplier", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { p ->
                        FilterChip(
                            selected = mult == p,
                            onClick = { mult = p },
                            label = { Text("${p}×") }
                        )
                    }
                }
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${(meal.totalCalories * mult).roundToInt()} kcal", style = MonoMetricMedium, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "P ${(meal.totalProtein * mult).roundToInt()}g • C ${(meal.totalCarbs * mult).roundToInt()}g • F ${(meal.totalFat * mult).roundToInt()}g",
                            style = MonoMetricSmall
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealType.entries.forEach { mt ->
                        FilterChip(
                            selected = mealType == mt,
                            onClick = { mealType = mt },
                            label = { Text(mt.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(mult, mealType) }) { Text("Log Meal") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateCustomFoodDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, Double, Double, Double, Double, Double, MealType?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("Homemade") }
    var servingText by remember { mutableStateOf("100") }
    var unit by remember { mutableStateOf("g") }
    var kcalText by remember { mutableStateOf("") }
    var proText by remember { mutableStateOf("") }
    var carbText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }
    var fiberText by remember { mutableStateOf("0") }
    var logNow by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Food") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("Food Name (e.g., Homemade Chicken Curry)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = servingText,
                        onValueChange = { servingText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                        label = { Text("Serving") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it.take(12) },
                        label = { Text("Unit (g/ml/cup)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = kcalText,
                    onValueChange = { kcalText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = proText,
                        onValueChange = { proText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbText,
                        onValueChange = { carbText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fiberText,
                        onValueChange = { fiberText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                        label = { Text("Fiber (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = logNow, onCheckedChange = { logNow = it })
                    Text("Log immediately to today's diary", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = servingText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 100.0
                    val k = kcalText.toDoubleOrNull() ?: 0.0
                    val p = proText.toDoubleOrNull() ?: 0.0
                    val c = carbText.toDoubleOrNull() ?: 0.0
                    val f = fatText.toDoubleOrNull() ?: 0.0
                    val fib = fiberText.toDoubleOrNull() ?: 0.0
                    onSave(name, brand, s, unit, k, p, c, f, fib, if (logNow) MealType.CUSTOM else null)
                },
                enabled = name.isNotBlank() && (kcalText.toDoubleOrNull() ?: -1.0) >= 0.0
            ) { Text("Save Food") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateSavedMealDialog(
    foods: List<FoodItemEntity>,
    onDismiss: () -> Unit,
    onSave: (String, MealType, List<SavedMealItemSpec>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf(MealType.BREAKFAST) }
    val selectedIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Saved Meal") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Meal Name (e.g., My Breakfast)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealType.entries.forEach { mt ->
                        FilterChip(
                            selected = mealType == mt,
                            onClick = { mealType = mt },
                            label = { Text(mt.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Text("Select Foods (${selectedIds.size} selected)", style = MaterialTheme.typography.labelLarge)
                foods.take(20).forEach { food ->
                    val checked = food.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (checked) selectedIds.remove(food.id) else selectedIds.add(food.id)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { c ->
                                if (c) selectedIds.add(food.id) else selectedIds.remove(food.id)
                            }
                        )
                        Column {
                            Text(food.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${String.format("%.0f", food.servingSize)} ${food.servingUnit} • ${food.calories.roundToInt()} kcal • ${food.protein.roundToInt()}g P",
                                style = MaterialTheme.typography.labelSmall,
                                color = FitTrackThemeExt.colors.mutedText
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val items = foods.filter { it.id in selectedIds }.map { food ->
                        SavedMealItemSpec(
                            foodItemId = food.id,
                            foodName = food.name,
                            quantity = food.servingSize,
                            unit = food.servingUnit,
                            calories = food.calories,
                            protein = food.protein,
                            carbs = food.carbs,
                            fat = food.fat,
                            fiber = food.fiber
                        )
                    }
                    onSave(name, mealType, items)
                },
                enabled = name.isNotBlank() && selectedIds.isNotEmpty()
            ) { Text("Save Meal") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
