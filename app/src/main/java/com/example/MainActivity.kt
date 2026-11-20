package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.FitTrackDatabase
import com.example.data.prefs.PreferencesManager
import com.example.data.remote.FirestoreSyncManager
import com.example.data.repository.FitTrackRepository
import com.example.domain.model.CardioActivityType
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutFromGoogle
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.nutrition.NutritionScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.progress.ProgressScreen
import com.example.ui.theme.FitTrackTheme
import com.example.ui.workout.ActiveWorkoutScreen
import com.example.ui.workout.CardioLogDialog
import com.example.ui.workout.WorkoutScreen
import com.example.viewmodel.FitTrackViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

private enum class FitTrackTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    WORKOUT("workout", "Workout", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    NUTRITION("nutrition", "Nutrition", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    PROGRESS("progress", "Progress", Icons.Filled.ShowChart, Icons.Outlined.ShowChart),
    PROFILE("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            FitTrackTheme {
                FitTrackRoot()
            }
        }
    }
}

@Composable
fun FitTrackRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        val uid = user.uid
        // Keyed by UID so switching accounts tears down and recreates all ViewModels & listeners cleanly
        key(uid) {
            FitTrackAuthenticatedShell(
                currentUserId = uid,
                displayName = user.displayName ?: "",
                email = user.email ?: ""
            )
        }
    }
}

@Composable
private fun FitTrackAuthenticatedShell(
    currentUserId: String,
    displayName: String,
    email: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: FitTrackViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
                val localDb = FitTrackDatabase.getInstance(app)
                val syncManager = FirestoreSyncManager(app, localDb)
                val repo = FitTrackRepository(localDb, syncManager, app)
                val prefs = PreferencesManager(app)
                val bundledRepo = app.getString(R.string.github_releases_repo).trim()
                FitTrackViewModel(
                    repository = repo,
                    preferencesManager = prefs,
                    currentUserId = currentUserId,
                    authDisplayName = displayName,
                    authEmail = email,
                    defaultGitHubRepo = bundledRepo
                )
            }
        }
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val restTimer by viewModel.restTimer.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val appUpdateStatus by viewModel.appUpdateStatus.collectAsStateWithLifecycle()
    val updateBannerDismissed by viewModel.updateBannerDismissed.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // 1. Loading initial local + cloud session sync
    if (!state.isInitialized) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // 2. First-time user setup wizard
    if (state.profile == null || state.profile?.onboardingCompleted != true) {
        OnboardingScreen(
            userId = currentUserId,
            defaultName = displayName,
            defaultEmail = email,
            unitSystem = state.preferences.unitSystem,
            onUnitSystemChanged = viewModel::setUnitSystem,
            onComplete = viewModel::completeOnboarding
        )
        return
    }

    // 3. Full-screen Active Workout Session overlay
    activeWorkout?.let { active ->
        ActiveWorkoutScreen(
            session = active,
            restTimer = restTimer,
            unitSystem = state.preferences.unitSystem,
            onUpdateSet = viewModel::updateActiveSet,
            onAddSet = viewModel::addSetToActiveExercise,
            onRemoveSet = viewModel::removeSetFromActiveExercise,
            onQuickProgression = viewModel::applyQuickProgression,
            onUpdateNotes = viewModel::updateActiveWorkoutNotes,
            onStartRestTimer = viewModel::startRestTimer,
            onAdjustRestTimer = viewModel::adjustRestTimer,
            onStopRestTimer = viewModel::stopRestTimer,
            onFinishWorkout = viewModel::finishActiveWorkout,
            onCancelWorkout = viewModel::cancelActiveWorkout
        )
        return
    }

    // 4. Main 5-Tab Adaptive Navigation Shell
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: FitTrackTab.HOME.route
    var showCardioDialog by remember { mutableStateOf(false) }

    fun navigateToTab(tab: FitTrackTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        FitTrackTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        FitTrackTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = FitTrackTab.HOME.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 820.dp)
                    ) {
                        composable(FitTrackTab.HOME.route) {
                            DashboardScreen(
                                state = state,
                                appUpdateStatus = appUpdateStatus,
                                updateBannerDismissed = updateBannerDismissed,
                                onDismissUpdateBanner = viewModel::dismissUpdateBanner,
                                onSyncClick = viewModel::triggerManualSync,
                                onLogWeight = viewModel::logWeight,
                                onStartWorkout = viewModel::startWorkoutFromTemplate,
                                onOpenCardioLog = { showCardioDialog = true },
                                onNavigateToNutrition = { navigateToTab(FitTrackTab.NUTRITION) },
                                onNavigateToWorkout = { navigateToTab(FitTrackTab.WORKOUT) },
                                onNavigateToProgress = { navigateToTab(FitTrackTab.PROGRESS) }
                            )
                        }
                        composable(FitTrackTab.WORKOUT.route) {
                            WorkoutScreen(
                                state = state,
                                onStartTemplate = viewModel::startWorkoutFromTemplate,
                                onSaveTemplate = viewModel::saveWorkoutTemplate,
                                onDeleteTemplate = viewModel::deleteWorkoutTemplate,
                                onAddCustomExercise = viewModel::addCustomExercise,
                                onToggleExerciseFavorite = viewModel::toggleExerciseFavorite,
                                onOpenCardioDialog = { showCardioDialog = true },
                                onRepeatCardio = { c ->
                                    viewModel.logCardio(
                                        activityType = CardioActivityType.fromNameSafe(c.activityType),
                                        durationMinutes = c.durationMinutes,
                                        distanceKm = c.distanceKm,
                                        speedKmh = c.speedKmh,
                                        inclinePercent = c.inclinePercent,
                                        resistanceLevel = c.resistanceLevel,
                                        notes = c.notes
                                    )
                                },
                                onDeleteCardio = viewModel::deleteCardioSession,
                                onDeleteWorkoutSession = viewModel::deleteWorkoutSession
                            )
                        }
                        composable(FitTrackTab.NUTRITION.route) {
                            NutritionScreen(
                                state = state,
                                onLogFood = viewModel::logFoodItem,
                                onToggleFoodFavorite = viewModel::toggleFoodFavorite,
                                onCreateCustomFood = viewModel::createCustomFood,
                                onSaveMealPreset = viewModel::saveMealPreset,
                                onLogSavedMeal = viewModel::logSavedMeal,
                                onDeleteSavedMeal = viewModel::deleteSavedMeal,
                                onDeleteFoodLog = viewModel::deleteFoodLog
                            )
                        }
                        composable(FitTrackTab.PROGRESS.route) {
                            ProgressScreen(
                                state = state,
                                onLogWeight = viewModel::logWeight,
                                onLogMeasurements = viewModel::logMeasurements,
                                onAddProgressPhoto = viewModel::addProgressPhoto,
                                onDeleteProgressPhoto = viewModel::deleteProgressPhoto
                            )
                        }
                        composable(FitTrackTab.PROFILE.route) {
                            ProfileScreen(
                                state = state,
                                appUpdateStatus = appUpdateStatus,
                                bundledUpdateSourceConfigured = viewModel.defaultGitHubRepo.isNotBlank(),
                                onSaveGitHubRepo = viewModel::saveGitHubRepoAndCheck,
                                onCheckForUpdates = { viewModel.checkForAppUpdates(manual = true) },
                                onUpdateProfile = viewModel::updateProfile,
                                onUnitSystemChange = viewModel::setUnitSystem,
                                onUpdateReminders = viewModel::updateReminderPrefs,
                                onManualSync = viewModel::triggerManualSync,
                                onExportData = viewModel::exportData,
                                onClearWorkouts = viewModel::clearWorkoutHistory,
                                onClearFoods = viewModel::clearFoodHistory,
                                onDeleteAccountData = {
                                    viewModel.deleteAccountData {
                                        signOutFromGoogle(credentialManager, scope)
                                    }
                                },
                                onSignOut = {
                                    signOutFromGoogle(credentialManager, scope)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCardioDialog) {
        CardioLogDialog(
            weightKg = state.profile?.currentWeightKg ?: 75.0,
            unitSystem = state.preferences.unitSystem,
            onDismiss = { showCardioDialog = false },
            onSave = { act, dur, dist, speed, inc, lvl, notes ->
                viewModel.logCardio(act, dur, dist, speed, inc, lvl, notes)
            }
        )
    }
}
