# FitTrack — Pin-to-Pin Technical Reference

This document is the complete, exact inventory of every module, file, design token, font, formula, database table, Firestore collection, security rule, permission, and CI/CD step used to build **FitTrack**.

---

## 1. Architecture Overview

- **Pattern**: Unidirectional MVVM + Offline-First Repository
  - **UI Layer (Jetpack Compose + Material 3)**: Pure reactive screens observing `StateFlow` via `collectAsStateWithLifecycle()`.
  - **ViewModel (`FitTrackViewModel`)**: Combines reactive Room `Flow` streams, DataStore `UserPreferences`, sync queue state, active workout draft state, rest timer state, and GitHub Releases update state.
  - **Domain Layer (`domain/calc`, `domain/model`)**: 100% deterministic, side-effect-free Kotlin calculators and enums — zero generative AI, zero network coupling.
  - **Data Layer (`data/local`, `data/remote`, `data/prefs`, `data/seed`, `data/repository`)**:
    - All writes commit to **Room SQLite** first and enqueue a `SyncQueueEntity`.
    - `FirestoreSyncManager` flushes queued writes to the dedicated Cloud Firestore database and restores cloud state on Google Sign-In.
    - `PreferencesManager` stores device-local preferences (`UnitSystem`, reminder toggles, configured GitHub repo slug) in **Jetpack DataStore**.
    - `GitHubUpdateChecker` checks GitHub Releases API (`releases/latest`) over HTTPS using built-in `HttpURLConnection`.

---

## 2. Complete Source & Configuration File Map

### Root Project & CI/CD
| Path | Purpose |
| :--- | :--- |
| `settings.gradle.kts` | Gradle module configuration (`rootProject.name = "FitTrack"`, `:app`). |
| `build.gradle.kts` | Root plugin declarations. |
| `gradle/libs.versions.toml` | Centralized Gradle Version Catalog pinning every library & plugin version. |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle `9.3.1` distribution URL. |
| `app/build.gradle.kts` | Android application config (`applicationId = "com.aistudio.fittrack.hvxkqr"`, `namespace = "com.example"`, SDK 24–36, signing configs, active dependencies, KSP Room compiler). |
| `.github/workflows/release-apk.yml` | GitHub Actions workflow: runs unit tests, stamps `versionName`/`versionCode`, builds signed APK, computes SHA-256, and publishes a GitHub Release with attached `.apk`. |
| `firebase-blueprint.json` | Canonical JSON schema describing all 12 Firestore entities & paths. |
| `firestore.rules` | Zero-trust per-user Firestore Security Rules deployed to the FitTrack database. |
| `firebase.json` | Local Firebase Emulator configuration (Firestore on port `8085`). |
| `firestore.test.js` | Automated Node.js security rules test suite (4/4 passing against emulator). |
| `metadata.json` | AI Studio platform metadata (`name = "FitTrack"`). |
| `.gitignore` | Excludes build caches, `.env`, `local.properties`, `debug.keystore`, `*-debug.log`. |

### Kotlin Source Files (`app/src/main/java/com/example/`)
| Package / File | Responsibility |
| :--- | :--- |
| `MainActivity.kt` | Edge-to-edge host activity, Firebase `AuthStateListener` gate, UID-keyed `FitTrackAuthenticatedShell`, adaptive `NavigationBar` (`< 600dp`) / `NavigationRail` (`>= 600dp`), `NavHost` across 5 tabs, onboarding & active workout overlays. |
| `ui/theme/Color.kt` | Exact 19-token dark-first Slate + Cobalt + Emerald + Amber + Crimson + Cyan + Violet palette. |
| `ui/theme/Type.kt` | Bundled `Space Grotesk`, `Inter`, and `JetBrains Mono` font families & M3 Typography scale + custom `MonoMetricLarge/Medium/Small` styles. |
| `ui/theme/Theme.kt` | `FitTrackTheme` dark-first M3 `ColorScheme` + `FitTrackThemeExt` custom semantic tokens (`mutedText`, `success`, `warning`, `cardioAccent`, `volumeAccent`). |
| `domain/model/FitTrackEnums.kt` | Type-safe enums: `GoalType`, `Gender`, `ActivityLevel`, `WorkoutSplit`, `MuscleGroup`, `CardioActivityType` (with base MET values), `MealType`, `PhotoType`, `UnitSystem`, `SyncState`. |
| `domain/calc/FitnessCalculations.kt` | Deterministic calculators: Mifflin-St Jeor BMR, TDEE, initial macro/calorie targets with safety floors, manual macro split, MET cardio calories, strength workout calories, energy balance & 7,700 kcal/kg weight change, Epley 1RM, set volume, goal progress %, unit conversions, date keys. |
| `domain/calc/AnalyticsEngines.kt` | Deterministic analytics: 100-pt Daily Score breakdown, rolling 7/14/30-day weight trends, **Calorie-Deficit Daily Estimated Weight** (`calculateCalorieEstimatedWeight`), PR detection (Max Weight, Est. 1RM, Set Volume, Bodyweight Rep PRs), compassionate nutrition coaching, weekly adaptive targets, achievements engine. |
| `data/local/FitTrackEntities.kt` | All 15 Room `@Entity` definitions and database indices (including `water_logs`). |
| `data/local/FitTrackDaos.kt` | Room DAOs (`UserProfileDao`, `UserGoalDao`, `ExerciseDao`, `WorkoutTemplateDao`, `WorkoutSessionDao`, `CardioSessionDao`, `NutritionDao`, `WaterDao`, `BodyMetricsDao`, `SyncQueueDao`). |
| `data/local/FitTrackDatabase.kt` | Singleton Room database (`fittrack_db`, version `2` with non-destructive `MIGRATION_1_2`). |
| `data/seed/SeedCatalogs.kt` | `FoodCatalogProvider` swappable boundary, JSON serializers (`SeedSerialization`), pre-seeded Gym + 38+ Home/Bodyweight exercise library, starter Gym & 6 starter Home templates (`tpl_home_*`), Home/Gym classification helpers, and curated staple food catalog. |
| `data/prefs/PreferencesManager.kt` | Jetpack DataStore (`fittrack_prefs`) persisting `unitSystem`, reminder toggles, custom personal reminder times (hour/minute + weekly weekday), non-destructive 7-Day Volume & Weight/Trend reset baselines, and `githubRepo`. |
| `notifications/FitTrackNotificationManager.kt` | Notification channels (`fittrack_reminders`, `fittrack_pr_alerts`), Android 13+ permission checks, Play-compliant `AlarmManager.setInexactRepeating` daily/weekly reminder scheduling using user-customized personal times, 12-hour/weekday formatters, instant PR alert notifications, and test notification trigger. |
| `notifications/FitTrackReminderReceiver.kt` | Non-exported `BroadcastReceiver` delivering scheduled reminder notifications and restoring enabled reminders on `ACTION_BOOT_COMPLETED`. |
| `data/remote/FirestoreErrorHandler.kt` | Structured Firestore error classification & logging (`OperationType`). |
| `data/remote/FirestoreSyncManager.kt` | Connectivity-aware (`ConnectivityManager`) authenticated Firestore push (`flushQueue` with 8s per-op timeout & fast offline bail-out), immutable-`createdAt` safe upserts (`upsertPreservingCreatedAt`), cloud-to-local restore (`pullCloudDataIntoLocal` with 12s timeout), and full account deletion (`deleteAllRemoteUserData`). |
| `data/remote/GitHubUpdateChecker.kt` | GitHub Releases API checker (`normalizeRepoInput`, `isNewerVersion`, `checkLatestRelease`, `AppUpdateInfo`, `AppUpdateStatus`). |
| `data/remote/InAppApkUpdater.kt` | Direct in-app GitHub Release `.apk` streaming downloader (with redirect support & live progress `ApkDownloadState`), `canRequestPackageInstalls()` check, and `FileProvider` system Package Installer launcher (`ACTION_INSTALL_PACKAGE` / `ACTION_VIEW` with `FLAG_GRANT_READ_URI_PERMISSION`). |
| `data/repository/FitTrackRepository.kt` | Instant offline-first repository splitting `<50ms` local Room init (`initializeLocalSession`) from non-blocking background `syncCloudSession` & dedicated `backgroundSyncScope` queue flushes, plus Gym & Home seed hydration, template sync from workout history, private progress photo copying, and JSON export. |
| `viewmodel/FitTrackViewModel.kt` | Reactive state aggregator with non-blocking offline startup, non-destructive 7-Day Volume & Weight/Trend baseline scoping, direct in-app APK updater state machine, custom reminder time updates, active workout state, rest timer coroutine, PR notifications, and cloud sync triggers. |
| `ui/auth/GoogleAuthHelper.kt` | Jetpack `CredentialManager` Google Sign-In (`GetGoogleIdOption` / `GetSignInWithGoogleOption`), Firebase credential exchange, and sign-out cleanup. |
| `ui/auth/AuthScreen.kt` | Branded FitTrack welcome & Google Sign-In gate. |
| `ui/components/FitTrackComponents.kt` | Reusable 3D M3 components (`FitTrackCard`, `DailyScoreRing`, `MacroProgressBar`, `SectionHeader`, `SyncStatusBadge`, `EstimationDisclaimerBanner`). |
| `ui/components/FitTrackLaunchAnimation.kt` | 120Hz cold-start app opening animation (`FitTrackLaunchOverlay`) with sculpted shield crest, 360° orbital energy ring, shockwave rings, and kinetic wordmark reveal. |
| `ui/components/BaselineResetDialog.kt` | Shared non-destructive `MetricBaselineResetDialog` (Restore Full History, Start Fresh Right Now, Start from First of Week Mon, custom 7-day picker) and `InAppUpdateActionPanel` (live download progress bar, Cancel, Allow & Install, Install, Retry). |
| `ui/onboarding/OnboardingScreen.kt` | 3-step onboarding flow with live deterministic BMR/TDEE/macro preview. |
| `ui/dashboard/GameScoreHeroCard.kt` | Game-HUD Today's Score hero card: Rank Shield Crest (`S+`→`D`), smooth 3D glowing orbital energy ring (`GameOrbitalPowerRing`), animated score meter transition (`animateFromScore` → `score` with floating `+X PTS` gain badge when returning to Home after a log), XP tier progress bar, Streak Flame pill, and 5-pillar RPG quest stat slots. |
| `ui/dashboard/DashboardScreen.kt` | Home tab: `GameScoreHeroCard` & rank breakdown modal, Weight & Trend card with Calorie-Deficit Daily Estimated Weight + non-destructive Reset dialog + `WeightEstimateBreakdownDialog`, Nutrition Today with 3D Hydration Ring & quick Water Log, Energy Balance, coaching insight, direct in-app GitHub Release updater banner. |
| `ui/workout/WorkoutScreen.kt` | Training Studio tab: Two-button **Gym Training** / **Home Training** mode switcher, mode-scoped KPI card (`7-Day Volume` / `7-Day Home Reps`) with clickable daily workout breakdown dialog & non-destructive Reset control (preserves 100% of Workout History), Cream-titled templates, Cardio logger, Gym/Home exercise library, and workout history. |
| `ui/workout/ActiveWorkoutScreen.kt` | Full-screen active workout logger with Rest Timer pinned at top, Finish Workout docked at bottom, bodyweight (`BW`) input support, previous-set hints, set/RIR/warmup editing, quick progression buttons, and live PR badges. |
| `ui/workout/CardioLogSheet.kt` | MET-calibrated cardio logging dialog with real-time calorie estimate preview. |
| `ui/nutrition/NutritionScreen.kt` | Nutrition tab: Light botanical palette, high-contrast black Weekly toggle, 3D Water Log card, daily macro summary, meal-grouped logs, staple/custom food picker, favorites, custom food creator, Saved Meals builder & multiplier logger. |
| `ui/progress/ProgressScreen.kt` | Progress tab: Weight logger, interactive Canvas Bezier trend chart + `Est. Today` calorie-estimated weight KPI, 3D 7-day Calorie/Protein/Hydration charts, body measurements, Android Photo Picker progress gallery & comparison, PR leaderboard, achievements. |
| `ui/profile/ProfileScreen.kt` | Profile tab: Athlete summary, Edit Goals dialog, Metric/Imperial toggle, Transparent Formulas dialog, live Reminder Preferences with Android 13+ `POST_NOTIFICATIONS` permission launcher + tappable personal time chips (`TimePicker` + weekday picker) + Test Notification trigger, direct in-app App Updates above Cloud Sync & Privacy, JSON export, Clear/Delete data controls, Sign Out. |

### Automated Test Files
| Path | Coverage |
| :--- | :--- |
| `app/src/test/java/com/example/domain/FitnessCalculationsTest.kt` | Unit tests for BMR, TDEE, initial targets, 7,700 kcal/kg energy balance, rolling weight trends, Epley 1RM, PR detection, MET cardio calories, daily score rubric. |
| `app/src/test/java/com/example/domain/GitHubUpdateCheckerTest.kt` | Unit tests for GitHub repo slug/URL normalization and SemVer tag comparisons. |
| `app/src/test/java/com/example/ExampleRobolectricTest.kt` | Robolectric Android context test verifying `R.string.app_name == "FitTrack"`. |
| `app/src/test/java/com/example/ExampleUnitTest.kt` | Baseline JVM unit test. |
| `firestore.test.js` | 4 Firestore Security Rules tests (unauthenticated denial, valid owner write/read, cross-user denial, invalid schema rejection). |

---

## 3. Design System Pin-to-Pin

### Per-Tab Dynamic 3D Color Palettes — 120Hz Optimized (`Color.kt` & `Theme.kt`)
FitTrack achieves locked 120Hz tab switching by scoping cached immutable `ColorScheme` and `FitTrackExtendedColors` palettes directly inside each tab screen while animating only the outer shell & navigation bar chrome (`rememberAnimatedShellPalette`, `220ms FastOutSlowInEasing`) alongside lightweight GPU-accelerated horizontal slide + fade transitions (`200ms`). Non-clickable `FitTrackCard` surfaces skip press-animation allocations and cache gradient brushes:

| Section | Primary + Secondary Palette | Mode & Key Accents |
| :--- | :--- | :--- |
| **Home** (`FitTrackSection.HOME`) | Deep Indigo `#252A5A` + Terracotta `#C66A4A` | Dark 3D theme, terracotta highlight & beveled indigo decks |
| **Workout** (`FitTrackSection.WORKOUT`) | Crimson `#701A1F` + Royal Blue `#1E3A8A` + Cream `#FFF5E1` titles | Exclusive to Workout & Active Workout; dual-tone crimson/blue 3D cards, Rest Timer pinned at top, Finish Workout docked at bottom, clickable 7-Day Volume daily workout breakdown |
| **Nutrition** (`FitTrackSection.NUTRITION`) | Botanical Lime `#C1E899` + Soft Sage `#E6F0DC` | Light botanical 3D mode with dark forest ink `#152913`, high-contrast black Weekly toggle `#0E1410`, & dark system bar icons |
| **Progress** (`FitTrackSection.PROGRESS`) | Warm Obsidian `#282623` + Stone Pewter `#898681` | Dark warm-stone 3D theme with interactive Bezier & 3D beveled charts |
| **Profile** (`FitTrackSection.PROFILE`) | Deep Teal `#134E4A` + Warm Gold `#D4A373` | Dark teal & warm-gold 3D settings cards with App Updates positioned above Cloud Sync & Privacy |

### Typography (`res/font/` & `Type.kt`)
- **Headlines & Titles**: `Space Grotesk` (`res/font/space_grotesk.ttf`)
- **Body & Controls**: `Inter` (`res/font/inter.ttf`)
- **Numeric KPIs, Timers, Weights, Macros**: `JetBrains Mono` (`res/font/jetbrains_mono.ttf`) — prevents layout jitter during live timer ticks and numeric updates.

### Adaptive Layout & Accessibility
- Edge-to-edge enabled via `enableEdgeToEdge()` with dark system bar styles and `WindowInsets.safeDrawing`.
- Responsive navigation switches automatically at `maxWidth >= 600.dp` (`NavigationBar` on phones → `NavigationRail` + `820.dp` centered max content width on tablets/foldables).
- All interactive elements expose `Modifier.testTag(...)` identifiers and `contentDescription` semantics.

---

## 4. Deterministic Fitness Formulas Pin-to-Pin

FitTrack uses **zero AI models** — every calculation is deterministic and auditable in `FitnessCalculations.kt` and `AnalyticsEngines.kt`:

1. **BMR (Mifflin-St Jeor)**:
   - $\text{BMR} = 10 \times \text{weight}_{\text{kg}} + 6.25 \times \text{height}_{\text{cm}} - 5 \times \text{age} + s$
   - Sex offset $s$: `+5` (Male), `-161` (Female), `-78` (Other/Neutral). Clamped `[900, 4500]` kcal.
2. **Estimated TDEE**:
   - $\text{TDEE} = \text{BMR} \times \text{ActivityMultiplier}$
   - Multipliers: `SEDENTARY = 1.20`, `LIGHT = 1.375`, `MODERATE = 1.55`, `ACTIVE = 1.725`, `VERY_ACTIVE = 1.90`. Clamped `[1100, 6500]` kcal.
3. **Goal Calorie & Macro Targets**:
   - Tissue energy constant: `KCAL_PER_KG_TISSUE = 7700.0 kcal/kg`.
   - Default weekly rate: `CUT = 0.45 kg/wk` (delta clamped `[-800, -200]` kcal/day), `BULK = 0.25 kg/wk` (delta clamped `[+150, +500]` kcal/day), `MAINTAIN = 0`.
   - **Safety Floors**: Minimum daily target never drops below `1500 kcal` (Male), `1200 kcal` (Female), or `1350 kcal` (Other).
   - **Protein**: Reference weight × (`2.1 g/kg` Cut, `1.85 g/kg` Bulk, `1.9 g/kg` Maintain), clamped `[60, 280] g`.
   - **Fat**: $\max(0.7 \times \text{refWeight}, 27\% \text{ of kcal} \div 9)$, minimum `35 g`.
   - **Carbs**: Remaining calorie budget $\div 4$.
   - **Fiber**: `14 g` per `1000 kcal`, clamped `[22, 50] g`.
4. **Daily Energy Balance & Calorie-Deficit Daily Estimated Weight**:
   - $\text{TotalExpenditure} = \text{TDEE}(\text{currentEstimatedWeight}) + \text{WorkoutBurn} + \text{CardioBurn}$
   - $\text{NetBalance} = \text{FoodIntake} - \text{TotalExpenditure}$
   - $\text{DailyDelta}_{\text{kg}} = \text{clamp}(\text{NetBalance} \div 7700.0,\ -0.18,\ +0.18)$
   - Anchors on the user's real scale weigh-in(s) and walks forward day-by-day across logged nutrition days; days without logged meals hold steady (`delta = 0`) so missed logs never fabricate fake weight loss.
5. **100-Point Daily Adherence Score & Game Rank Tiers**:
   - Nutrition Calorie Adherence: `25 pts`
   - Protein Target: `20 pts`
   - Strength Workout / Weekly Cadence: `25 pts`
   - Cardio Target: `15 pts`
   - Logging Consistency & Streak: `15 pts`
   - Rank Tiers (`GameScoreHeroCard.kt`): `S+` (`95–100` Legendary), `S` (`85–94` Elite), `A` (`70–84` Veteran), `B` (`55–69` Contender), `C` (`35–54` Challenger), `D` (`0–34` Warm-Up).
6. **Strength Training & PR Math**:
   - **Set Volume**: $\text{weight}_{\text{kg}} \times \text{reps}$ (excludes warmup sets).
   - **Estimated 1RM (Epley)**: $\text{weight}_{\text{kg}} \times (1 + \text{reps} \div 30.0)$ (equals weight when `reps == 1`).
   - **PR Categories**: Automatically flags new all-time Max Weight, Estimated 1RM, Single-Set Volume on weighted sets, and Max Reps (`MOST_REPS_AT_WEIGHT`) on bodyweight (`0 kg`) sets.
7. **MET-Based Cardio Expenditure**:
   - $\text{kcal} = \text{AdjustedMET} \times \text{weight}_{\text{kg}} \times (\text{duration}_{\text{min}} \div 60.0)$
   - Supports Walking (with speed + incline `+0.42 MET/%`), Running (`speedKmh * 0.98`), Cycling, Elliptical, Rowing, Stairmaster, Swimming, Jump Rope, HIIT, and Sports.
8. **Daily Hydration Target (Dedicated Metric — Does Not Alter 100-Pt Score Weights)**:
   - Default auto target (`waterTargetMl == 0`): $\text{weight}_{\text{kg}} \times 35\text{ ml/kg}$, rounded to nearest integer and clamped `[1500, 6000] ml`.
   - Editable custom target: `[500, 10000] ml` (or reset to `0` for auto mode).
9. **Unit Conversion Constants**:
   - `1 kg = 2.20462262 lb`
   - `1 cm = 0.39370079 in`
   - `1 km = 0.62137119 mi`
   - `1 ml = 0.03381402 fl oz`

---

## 5. Local Room Database Schema (`fittrack_db`, v2 — Non-Destructive `MIGRATION_1_2`)

| # | Table Name | Entity Class | Primary Key | Indices |
| :-: | :--- | :--- | :--- | :--- |
| 1 | `user_profiles` | `UserProfileEntity` (incl. `waterTargetMl`) | `userId` | — |
| 2 | `user_goals` | `UserGoalEntity` | `id` | `userId` |
| 3 | `exercises` | `ExerciseEntity` | `id` | `userId` |
| 4 | `workout_templates` | `WorkoutTemplateEntity` | `id` | `userId` |
| 5 | `workout_sessions` | `WorkoutSessionEntity` | `id` | `userId`, `dateKey` |
| 6 | `workout_sets` | `WorkoutSetEntity` | `id` | `userId`, `workoutSessionId`, `exerciseId` |
| 7 | `cardio_sessions` | `CardioSessionEntity` | `id` | `userId`, `dateKey` |
| 8 | `food_items` | `FoodItemEntity` | `id` | `userId` |
| 9 | `saved_meals` | `SavedMealEntity` | `id` | `userId` |
| 10 | `food_logs` | `FoodLogEntity` | `id` | `userId`, `dateKey` |
| 11 | `water_logs` | `WaterLogEntity` | `id` | `userId`, `dateKey` |
| 12 | `weight_logs` | `WeightLogEntity` | `id` | `userId`, `dateKey` |
| 13 | `body_measurements` | `BodyMeasurementEntity` | `id` | `userId`, `dateKey` |
| 14 | `progress_photos` | `ProgressPhotoEntity` | `id` | `userId` |
| 15 | `sync_queue` | `SyncQueueEntity` | `queueKey` | `userId` |

---

## 6. Cloud Firestore Schema & Security Rules

- **Firebase Project ID**: `gen-lang-client-0446342500`
- **Dedicated Firestore Database ID** (`res/values/firebase_applet_config.xml`): `ai-studio-android-fittrack-40c9d2d9-b219-4a92-a279-6e6b04ad5fee`
- **Authentication**: Google Sign-In only via Jetpack `CredentialManager` → `FirebaseAuth` (no anonymous auth, no email/password auth).
- **Zero-Trust Security Invariants (`firestore.rules`)**:
  - Default deny on all unmatched paths (`allow read, write: if false;`).
  - Strict per-user ownership: `request.auth != null && request.auth.uid == userId`.
  - Schema & range validation helpers for all 13 collections (`isValidUserProfile`, `isValidGoal`, `isValidExercise`, `isValidTemplate`, `isValidWorkout`, `isValidCardio`, `isValidCustomFood`, `isValidSavedMeal`, `isValidFoodLog`, `isValidWaterLog`, `isValidWeightLog`, `isValidMeasurement`, `isValidProgressPhoto`).
  - Immutable `createdAt` preserved on updates; `updatedAt == request.time` enforced via `FieldValue.serverTimestamp()`.

### Synced Firestore Paths
1. `/users/{userId}` — Profile & active targets (including optional `waterTargetMl`)
2. `/users/{userId}/goals/{goalId}` — Historical goal snapshots
3. `/users/{userId}/exercises/{exerciseId}` — Custom & favorite exercises
4. `/users/{userId}/templates/{templateId}` — Workout templates
5. `/users/{userId}/workouts/{workoutId}` — Completed strength sessions (with embedded sets array)
6. `/users/{userId}/cardio/{cardioId}` — Cardio sessions
7. `/users/{userId}/custom_foods/{foodId}` — User-defined custom foods
8. `/users/{userId}/meals/{mealId}` — Saved multi-item meals
9. `/users/{userId}/food_logs/{logId}` — Daily food intake logs
10. `/users/{userId}/water_logs/{logId}` — Daily hydration logs (`amountMl`, `dateKey`, `loggedAtEpochMs`)
11. `/users/{userId}/weight_logs/{logId}` — Body weight history
12. `/users/{userId}/measurements/{measurementId}` — Body measurements
13. `/users/{userId}/progress_photos/{photoId}` — Progress photo metadata only (image bytes remain private on device)

---

## 7. Permissions & Platform Security Inventory

| Permission / API | Usage |
| :--- | :--- |
| `android.permission.INTERNET` | Firebase Auth, Cloud Firestore sync, GitHub Releases update check & direct APK download. |
| `android.permission.ACCESS_NETWORK_STATE` | Fast connectivity detection (`ConnectivityManager`) so offline startup & writes never hang waiting on network timeouts. |
| `android.permission.POST_NOTIFICATIONS` | Runtime permission (Android 13+) for daily/weekly fitness reminders & instant PR celebration alerts. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Restores enabled inexact reminders via `FitTrackReminderReceiver` after device reboot (no exact-alarm permissions). |
| `android.permission.REQUEST_INSTALL_PACKAGES` | Launches Android's native Package Installer overlay directly via `FileProvider` (`res/xml/file_paths.xml`) for self-hosted GitHub Releases APK updates without Chrome redirection. |
| `ActivityResultContracts.PickVisualMedia` | Zero-permission Android Photo Picker for progress photos (no `READ_MEDIA_IMAGES` or `READ_EXTERNAL_STORAGE`). |

---

## 8. GitHub Releases CI/CD & In-App Updater Flow

1. **Workflow File**: `.github/workflows/release-apk.yml`
2. **Triggers**:
   - Pushing a Git tag matching `v*.*.*`
   - Clicking **Actions → Build & Publish FitTrack APK Release → Run workflow**
3. **Version & Repository Stamping**:
   - Extracts clean semantic version from the tag (`v1.2.0` → `versionName = "1.2.0"`).
   - Computes strictly increasing `versionCode = 100 + GITHUB_RUN_NUMBER` so Android accepts in-place upgrades over `versionCode = 1`.
   - Stamps `GITHUB_REPOSITORY` into `app/src/main/res/values/strings.xml` (`github_releases_repo`) so every published APK knows its release repository automatically with zero end-user setup.
4. **Signing & Firebase Secret Priority**:
   - Restores `app/google-services.json` from `GOOGLE_SERVICES_JSON_BASE64` or `GOOGLE_SERVICES_JSON` if not committed.
   - `RELEASE_KEYSTORE_BASE64` + `STORE_PASSWORD` + `KEY_PASSWORD` → builds signed **Release APK** (`assembleRelease`).
   - Otherwise `DEBUG_KEYSTORE_BASE64` (or `debug.keystore.base64` / fallback debug keystore) → builds **Debug APK** (`assembleDebug`).
5. **Release Artifacts**:
   - `FitTrack-v<version>.apk`
   - `FitTrack-v<version>.apk.sha256`
6. **Direct In-App Update Detection & Installation (`GitHubUpdateChecker.kt` + `InAppApkUpdater.kt`)**:
   - Reads the CI-stamped `R.string.github_releases_repo` automatically (hiding the developer repo input in Profile for end users).
   - On launch (non-blocking background check) and via **Check for Updates**, FitTrack fetches `https://api.github.com/repos/{owner}/{repo}/releases/latest`.
   - Parses `tag_name` vs `BuildConfig.VERSION_NAME` via semantic version comparison and locates the `.apk` asset's `browser_download_url`.
   - Tapping **Update Now** downloads the `.apk` directly into app-private `cacheDir/updates/` with live progress (`MB` & `%`) and launches Android's system Package Installer overlay via `FileProvider` (prompting once for Android's "Install unknown apps" permission if needed and auto-resuming installation on return via `ON_RESUME`).
