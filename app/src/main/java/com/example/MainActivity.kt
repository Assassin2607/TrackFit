package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.ui.components.FitTrackLaunchOverlay
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.nutrition.NutritionScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.progress.ProgressScreen
import com.example.ui.theme.FitTrackSection
import com.example.ui.theme.FitTrackTheme
import com.example.ui.theme.rememberAnimatedShellPalette
import com.example.notifications.FitTrackNotificationManager
import com.example.ui.workout.ActiveWorkoutScreen
import com.example.ui.workout.CardioLogDialog
import com.example.ui.workout.WorkoutScreen
import com.example.viewmodel.FitTrackViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlin.math.roundToInt

private enum class FitTrackTab(
    val route: String,
    val label: String,
    val section: FitTrackSection,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", FitTrackSection.HOME, Icons.Filled.Home, Icons.Outlined.Home),
    WORKOUT("workout", "Workout", FitTrackSection.WORKOUT, Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    NUTRITION("nutrition", "Nutrition", FitTrackSection.NUTRITION, Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    PROGRESS("progress", "Progress", FitTrackSection.PROGRESS, Icons.Filled.ShowChart, Icons.Outlined.ShowChart),
    PROFILE("profile", "Profile", FitTrackSection.PROFILE, Icons.Filled.Person, Icons.Outlined.Person);

    companion object {
        fun fromRoute(route: String?): FitTrackTab =
            entries.firstOrNull { it.route == route } ?: HOME
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FitTrackNotificationManager.ensureChannels(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            FitTrackRoot()
        }
    }
}

@Composable
fun FitTrackRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var showLaunchAnimation by rememberSaveable { mutableStateOf(true) }
    var mountMainContent by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (mountMainContent || !showLaunchAnimation) {
            val user = currentUser
            if (user == null) {
                FitTrackTheme(section = FitTrackSection.HOME) {
                    AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
                }
            } else {
                val uid = user.uid
                // Keyed by UID so switching accounts tears down and recreates all ViewModels & listeners cleanly
                key(uid) {
                    FitTrackAuthenticatedShell(
                        currentUserId = uid,
                        displayName = user.displayName ?: "",
                        email = user.email ?: "",
                        isLaunchAnimationActive = showLaunchAnimation
                    )
                }
            }
        }

        if (showLaunchAnimation) {
            FitTrackTheme(section = FitTrackSection.HOME, applySystemBars = false) {
                FitTrackLaunchOverlay(
                    onReadyForContent = { mountMainContent = true },
                    onFinished = {
                        mountMainContent = true
                        showLaunchAnimation = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FitTrackAuthenticatedShell(
    currentUserId: String,
    displayName: String,
    email: String,
    isLaunchAnimationActive: Boolean = false
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
                    appContext = app.applicationContext,
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
    val apkDownloadState by viewModel.apkDownloadState.collectAsStateWithLifecycle()
    val updateBannerDismissed by viewModel.updateBannerDismissed.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-resume in-app APK installation if user just enabled "Install unknown apps" in Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.retryInstallIfPermittedOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // 1. Loading initial local + cloud session sync
    if (!state.isInitialized) {
        FitTrackTheme(section = FitTrackSection.HOME) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    // 2. First-time user setup wizard
    if (state.profile == null || state.profile?.onboardingCompleted != true) {
        FitTrackTheme(section = FitTrackSection.HOME) {
            OnboardingScreen(
                userId = currentUserId,
                defaultName = displayName,
                defaultEmail = email,
                unitSystem = state.preferences.unitSystem,
                onUnitSystemChanged = viewModel::setUnitSystem,
                onComplete = viewModel::completeOnboarding
            )
        }
        return
    }

    // 3. Full-screen Active Workout Session overlay (inherits Workout #701A1F + #1E3A8A theme)
    activeWorkout?.let { active ->
        FitTrackTheme(section = FitTrackSection.WORKOUT) {
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
        }
        return
    }

    // 4. Main 5-Tab Adaptive Navigation Shell with Per-Tab Animated Palettes & Smooth Transitions
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: FitTrackTab.HOME.route
    val activeTab = FitTrackTab.fromRoute(currentRoute)
    var showCardioDialog by remember { mutableStateOf(false) }

    // Track last-displayed Home score so Today's Score animates ONLY when score strictly increases
    var lastSeenHomeScore by remember { mutableStateOf<Int?>(null) }
    var pendingScoreIncreaseFrom by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(activeTab, state.dailyScore.totalScore, isLaunchAnimationActive) {
        if (isLaunchAnimationActive) {
            // While opening animation plays, keep syncing baseline silently without firing a score animation
            lastSeenHomeScore = state.dailyScore.totalScore
            pendingScoreIncreaseFrom = null
        } else if (activeTab == FitTrackTab.HOME) {
            val current = state.dailyScore.totalScore
            val prev = lastSeenHomeScore
            if (prev == null) {
                lastSeenHomeScore = current
                pendingScoreIncreaseFrom = null
            } else if (current > prev) {
                pendingScoreIncreaseFrom = prev
                lastSeenHomeScore = current
            } else if (current < prev) {
                lastSeenHomeScore = current
                pendingScoreIncreaseFrom = null
            }
        }
    }

    fun navigateToTab(tab: FitTrackTab) {
        if (currentRoute == tab.route) return
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val shellPalette = rememberAnimatedShellPalette(section = activeTab.section)

    FitTrackTheme(section = activeTab.section, applySystemBars = false) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(shellPalette.background)
        ) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = shellPalette.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useNavRail) {
                        Column {
                            // Subtle 3D top bevel highlight line separating nav bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(shellPalette.navTopHighlight.copy(alpha = 0.35f))
                            )
                            NavigationBar(
                                containerColor = shellPalette.navBarContainer,
                                contentColor = shellPalette.onSurface,
                                tonalElevation = 0.dp
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
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = shellPalette.navOnIndicator,
                                            selectedTextColor = shellPalette.onSurface,
                                            indicatorColor = shellPalette.navIndicator,
                                            unselectedIconColor = shellPalette.mutedText,
                                            unselectedTextColor = shellPalette.mutedText
                                        ),
                                        modifier = Modifier.testTag("nav_${tab.route}")
                                    )
                                }
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
                            containerColor = shellPalette.navBarContainer,
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
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = shellPalette.navOnIndicator,
                                        selectedTextColor = shellPalette.onSurface,
                                        indicatorColor = shellPalette.navIndicator,
                                        unselectedIconColor = shellPalette.mutedText,
                                        unselectedTextColor = shellPalette.mutedText
                                    ),
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
                            enterTransition = {
                                val fromIdx = FitTrackTab.fromRoute(initialState.destination.route).ordinal
                                val toIdx = FitTrackTab.fromRoute(targetState.destination.route).ordinal
                                val dir = if (toIdx >= fromIdx) 1 else -1
                                slideInHorizontally(
                                    animationSpec = tween(210, easing = FastOutSlowInEasing),
                                    initialOffsetX = { (it * 0.10f * dir).roundToInt() }
                                ) + fadeIn(
                                    animationSpec = tween(190, easing = FastOutSlowInEasing)
                                )
                            },
                            exitTransition = {
                                val fromIdx = FitTrackTab.fromRoute(initialState.destination.route).ordinal
                                val toIdx = FitTrackTab.fromRoute(targetState.destination.route).ordinal
                                val dir = if (toIdx >= fromIdx) 1 else -1
                                slideOutHorizontally(
                                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                                    targetOffsetX = { (-it * 0.08f * dir).roundToInt() }
                                ) + fadeOut(
                                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                                )
                            },
                            popEnterTransition = {
                                fadeIn(tween(190, easing = FastOutSlowInEasing))
                            },
                            popExitTransition = {
                                fadeOut(tween(150, easing = FastOutSlowInEasing))
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 820.dp)
                        ) {
                            composable(FitTrackTab.HOME.route) {
                                FitTrackTheme(section = FitTrackSection.HOME, applySystemBars = false) {
                                    DashboardScreen(
                                        state = state,
                                        appUpdateStatus = appUpdateStatus,
                                        apkDownloadState = apkDownloadState,
                                        updateBannerDismissed = updateBannerDismissed,
                                        scoreIncreaseFrom = pendingScoreIncreaseFrom,
                                        onScoreIncreaseAnimationFinished = { pendingScoreIncreaseFrom = null },
                                        onStartInAppUpdate = viewModel::startInAppUpdate,
                                        onCancelInAppUpdate = viewModel::cancelInAppUpdate,
                                        onInstallDownloadedApk = viewModel::installDownloadedApk,
                                        onDismissUpdateBanner = viewModel::dismissUpdateBanner,
                                        onSyncClick = viewModel::triggerManualSync,
                                        onLogWeight = viewModel::logWeight,
                                        onLogWater = viewModel::logWater,
                                        onResetWeightTrend = viewModel::resetWeightAndTrend,
                                        onClearWeightTrendReset = viewModel::clearWeightAndTrendReset,
                                        onNavigateToNutrition = { navigateToTab(FitTrackTab.NUTRITION) },
                                        onNavigateToProgress = { navigateToTab(FitTrackTab.PROGRESS) }
                                    )
                                }
                            }
                            composable(FitTrackTab.WORKOUT.route) {
                                FitTrackTheme(section = FitTrackSection.WORKOUT, applySystemBars = false) {
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
                                        onDeleteWorkoutSession = viewModel::deleteWorkoutSession,
                                        onResetSevenDayVolume = viewModel::resetSevenDayVolume,
                                        onClearSevenDayVolumeReset = viewModel::clearSevenDayVolumeReset
                                    )
                                }
                            }
                            composable(FitTrackTab.NUTRITION.route) {
                                FitTrackTheme(section = FitTrackSection.NUTRITION, applySystemBars = false) {
                                    NutritionScreen(
                                        state = state,
                                        onLogFood = viewModel::logFoodItem,
                                        onToggleFoodFavorite = viewModel::toggleFoodFavorite,
                                        onCreateCustomFood = viewModel::createCustomFood,
                                        onSaveMealPreset = viewModel::saveMealPreset,
                                        onLogSavedMeal = viewModel::logSavedMeal,
                                        onDeleteSavedMeal = viewModel::deleteSavedMeal,
                                        onDeleteFoodLog = viewModel::deleteFoodLog,
                                        onLogWater = viewModel::logWater,
                                        onDeleteWaterLog = viewModel::deleteWaterLog,
                                        onUpdateWaterTargetMl = viewModel::updateWaterTargetMl
                                    )
                                }
                            }
                            composable(FitTrackTab.PROGRESS.route) {
                                FitTrackTheme(section = FitTrackSection.PROGRESS, applySystemBars = false) {
                                    ProgressScreen(
                                        state = state,
                                        onLogWeight = viewModel::logWeight,
                                        onLogMeasurements = viewModel::logMeasurements,
                                        onAddProgressPhoto = viewModel::addProgressPhoto,
                                        onDeleteProgressPhoto = viewModel::deleteProgressPhoto
                                    )
                                }
                            }
                            composable(FitTrackTab.PROFILE.route) {
                                FitTrackTheme(section = FitTrackSection.PROFILE, applySystemBars = false) {
                                    ProfileScreen(
                                        state = state,
                                        appUpdateStatus = appUpdateStatus,
                                        apkDownloadState = apkDownloadState,
                                        bundledUpdateSourceConfigured = viewModel.defaultGitHubRepo.isNotBlank(),
                                        onSaveGitHubRepo = viewModel::saveGitHubRepoAndCheck,
                                        onCheckForUpdates = { viewModel.checkForAppUpdates(manual = true) },
                                        onStartInAppUpdate = viewModel::startInAppUpdate,
                                        onCancelInAppUpdate = viewModel::cancelInAppUpdate,
                                        onInstallDownloadedApk = viewModel::installDownloadedApk,
                                        onUpdateProfile = viewModel::updateProfile,
                                        onUnitSystemChange = viewModel::setUnitSystem,
                                        onUpdateReminders = viewModel::updateReminderPrefs,
                                        onUpdateReminderTime = viewModel::updateReminderTime,
                                        onSyncReminders = viewModel::syncRemindersNow,
                                        onSendTestNotification = viewModel::sendTestNotification,
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
    }
}
