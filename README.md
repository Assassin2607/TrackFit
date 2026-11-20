# FitTrack — Personal Fitness Operating System (Android)

**FitTrack** is a native, dark-first, offline-first Android fitness application built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Room**, and **Firebase Cloud Firestore**.

It answers one core daily question using **100% deterministic, transparent formulas** (zero generative AI, zero chatbots):

> *"Based on what I ate, how I trained, my cardio, and my recent weight trend, am I on track toward my goal?"*

---

## Table of Contents

1. [Core Features](#core-features)
2. [Tech Stack & Exact Pinned Versions](#tech-stack--exact-pinned-versions)
3. [Push to GitHub & Automated APK Releases](#push-to-github--automated-apk-releases)
4. [Automatic Update Alerts on Your Phone](#automatic-update-alerts-on-your-phone)
5. [Local Android Studio Build & Firebase Setup](#local-android-studio-build--firebase-setup)
6. [Testing & Verification](#testing--verification)
7. [Privacy, Security & Google Play Policy Compliance](#privacy-security--google-play-policy-compliance)
8. [Pin-to-Pin Technical Reference](#pin-to-pin-technical-reference)

---

## Core Features

| Area | Capabilities |
| :--- | :--- |
| **Authentication & Onboarding** | Google Sign-In via Jetpack Credential Manager (`GetSignInWithGoogleOption`), automatic cloud restore on sign-in, 3-step onboarding wizard (biometrics, Cut/Bulk/Maintain goal, activity level, training split, transparent target preview). |
| **Home Dashboard** | 100-point daily adherence ring with tappable 5-pillar breakdown, daily energy-balance & energy-equivalent weight change (`7,700 kcal/kg`), macro bars, compassionate non-punitive coaching insights, weekly adaptive recommendations, one-tap quick actions, live cloud sync badge, and GitHub Release update banner. |
| **Strength & Cardio** | Pre-seeded Push/Pull/Legs/Upper/Lower templates, custom template builder, searchable exercise library with favorites, live Active Workout screen with previous-session weight/rep prefill, built-in rest timer (`+15s` / `-15s` / Skip), Epley 1RM & automatic PR detection, and MET-based cardio logger. |
| **Nutrition** | Curated local staple food catalog (behind swappable `FoodCatalogProvider`), custom food creator, favorites, one-tap Saved Meals with serving multiplier scaling, meal-type grouping (`Breakfast`, `Lunch`, `Dinner`, `Post-Workout`, `Snack`, `Custom`), and daily macro tracking. |
| **Progress Tracking** | Body weight logger with interactive Canvas trend chart (raw weigh-ins + rolling 7-day average), 7/14/30-day rate-of-change analytics, circumference measurements (`waist`, `chest`, `arms`, `thighs`, `neck`, `hips`), zero-permission Android Photo Picker progress photos with side-by-side comparison, strength PR history, and achievements. |
| **Profile & Settings** | Historical goal milestones & live target recalculation, Metric (`kg / cm / km`) ↔ Imperial (`lb / in / mi`) toggle persisted in DataStore, transparent formulas modal, JSON data export, manual cloud sync, in-app GitHub Releases updater, and account/data deletion controls. |
| **Offline-First Cloud Sync** | Instant local writes to Room SQLite (`14 tables`) backed by a persistent sync queue (`sync_queue`) that uploads to per-user Firebase Firestore collections (`12 collections`) automatically whenever online. |

---

## Tech Stack & Exact Pinned Versions

Every tool, SDK, plugin, and library used in this repository is pinned below (source of truth: `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `app/build.gradle.kts`):

### Build & Platform Toolchain
| Component | Exact Value / Version |
| :--- | :--- |
| **Application ID** | `com.aistudio.fittrack.hvxkqr` |
| **Namespace** | `com.example` |
| **Compile SDK** | API `36` (minor API level `1`) |
| **Target SDK** | API `36` |
| **Minimum SDK** | API `24` (Android 7.0+) |
| **Java / JVM Bytecode Target** | Java `11` (CI runs JDK `21` Temurin) |
| **Gradle** | `9.3.1` |
| **Android Gradle Plugin (AGP)** | `9.1.1` |
| **Kotlin & Compose Compiler Plugin** | `2.4.21` |
| **KSP (Kotlin Symbol Processing)** | `2.3.5` |
| **Google Services Gradle Plugin** | `4.5.0` |
| **Secrets Gradle Plugin** | `2.0.1` |

### Active Android & Cloud Dependencies (`app/build.gradle.kts`)
| Category | Artifact | Exact Version |
| :--- | :--- | :--- |
| **UI Framework** | `androidx.compose:compose-bom` (`ui`, `ui-graphics`, `ui-tooling-preview`, `material3`, `material-icons-core`, `material-icons-extended`) | `2024.09.00` |
| **Core & Activity** | `androidx.core:core-ktx` / `androidx.activity:activity-compose` | `1.18.0` / `1.10.1` |
| **Lifecycle & ViewModel** | `androidx.lifecycle:lifecycle-runtime-ktx`, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` | `2.8.7` |
| **Navigation** | `androidx.navigation:navigation-compose` | `2.8.9` |
| **Local Database** | `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (KSP) | `2.7.0` |
| **Preferences Storage** | `androidx.datastore:datastore-preferences` | `1.1.7` |
| **Image Loading** | `io.coil-kt:coil-compose` | `2.7.0` |
| **Concurrency** | `org.jetbrains.kotlinx:kotlinx-coroutines-core`, `kotlinx-coroutines-android` | `1.10.2` |
| **Cloud Backend** | `com.google.firebase:firebase-bom` (`firebase-firestore`, `firebase-auth`) | `35.1.0` |
| **Google Sign-In** | `androidx.credentials:credentials`, `credentials-play-services-auth` / `com.google.android.libraries.identity.googleid:googleid` | `1.5.0` / `1.1.1` |
| **GitHub Release Checker** | Zero extra libraries — built-in `java.net.HttpURLConnection` + `org.json.JSONObject` | Android SDK |
| **Unit & JVM Testing** | `junit:junit` (`4.13.2`), `org.robolectric:robolectric` (`4.16.1`), `kotlinx-coroutines-test` (`1.10.2`), `androidx.test:core` (`1.6.1`), `androidx.test.ext:junit` (`1.3.0`) | Pinned in `libs.versions.toml` |

### Bundled Typography & Visual Assets
| Asset | Source / File |
| :--- | :--- |
| **Display & Headings Font** | `Space Grotesk` (`app/src/main/res/font/space_grotesk.ttf`) |
| **Body & UI Font** | `Inter` (`app/src/main/res/font/inter.ttf`) |
| **Numeric & Data Font** | `JetBrains Mono` (`app/src/main/res/font/jetbrains_mono.ttf`) |
| **Adaptive Launcher Icon** | Custom FitTrack emblem (`ic_launcher_foreground.xml` 66dp safe zone + `#0F172A` `ic_launcher_background.xml` + PNG & round PNG fallbacks across `mdpi`–`xxxhdpi`) |

---

## Push to GitHub & Automated APK Releases

This project includes a ready-to-run GitHub Actions workflow at **`.github/workflows/release-apk.yml`** that automatically runs unit tests, compiles the APK, stamps the release version, computes a SHA-256 checksum, and publishes a downloadable `.apk` under **GitHub Releases**.

### Step 1: Push Your Project from AI Studio to GitHub
1. In **AI Studio**, open the **GitHub / Export** menu in the top toolbar and click **Push to GitHub**.
2. Create a **Public** repository (recommended so your phone can check GitHub Releases without an API token), e.g., `your-username/fittrack`.

### Step 2: Configure Consistent APK Signing (One-Time Setup)
Android only allows **in-place updates** (updating the app without uninstalling and losing local files) when every new APK is signed with the **same keystore**. Google Sign-In also requires that keystore's **SHA-1** to be registered in Firebase Console.

1. Generate a permanent upload keystore once on your computer (keep this file safe and **never commit it to Git**):
   ```bash
   keytool -genkeypair -v \
     -keystore my-upload-key.jks \
     -alias upload \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Print its **SHA-1** and **SHA-256** fingerprints and add them to **Firebase Console → Project Settings → Your apps (`com.aistudio.fittrack.hvxkqr`) → Add fingerprint**, then download the updated `google-services.json` into `app/google-services.json`:
   ```bash
   keytool -list -v -keystore my-upload-key.jks -alias upload | grep -E "SHA1|SHA256"
   ```
3. Encode the keystore to Base64:
   ```bash
   base64 -w 0 my-upload-key.jks
   ```
4. In your GitHub repo, go to **Settings → Secrets and variables → Actions → New repository secret** and add:
   - `RELEASE_KEYSTORE_BASE64` — the Base64 string from step 3
   - `STORE_PASSWORD` — your keystore password
   - `KEY_PASSWORD` — your `upload` key password

*(Alternatively, if you prefer debug builds for personal testing, you can store your local `debug.keystore` Base64 as `DEBUG_KEYSTORE_BASE64` instead.)*

### Step 3: Publish an Update Anytime
Whenever you push new changes from AI Studio to GitHub:
- **Via GitHub Web UI (No terminal needed):**
  1. Open your repo on GitHub → **Actions** tab → **Build & Publish FitTrack APK Release**.
  2. Click **Run workflow**, enter a higher version tag (e.g., `v1.1.0`), optionally write release notes, and click **Run workflow**.
- **Or via Git Tag:**
  ```bash
  git tag v1.1.0
  git push origin v1.1.0
  ```

Within ~2–3 minutes, GitHub Actions will:
- Stamp `versionName = "1.1.0"` and an auto-incrementing `versionCode` for the build,
- Run all unit tests (`:app:testDebugUnitTest`),
- Build the signed APK,
- Publish **GitHub Release `v1.1.0`** with **`FitTrack-v1.1.0.apk`** and **`FitTrack-v1.1.0.apk.sha256`** attached for direct download.

---

## Automatic Update Alerts on Your Phone

FitTrack includes a built-in, zero-permission **GitHub Releases Update Checker**:

1. Install your first APK from GitHub Releases onto your phone.
2. Open FitTrack → go to the **Profile** tab → scroll to **App Updates (GitHub)**.
3. Enter your repository slug (e.g., `your-username/fittrack` or paste the full GitHub URL) and tap **Save & Check**.
4. From then on:
   - Every time you open FitTrack, it checks `https://api.github.com/repos/<owner>/<repo>/releases/latest` in the background.
   - As soon as you publish a newer release (e.g., `v1.1.0` > `1.0`), an **Update Available** banner appears automatically at the top of your **Home** screen (and in **Profile → App Updates**).
   - Tap **Download APK** → your phone downloads `FitTrack-vX.Y.Z.apk` directly from GitHub → tap the downloaded file to update in place while keeping all your workouts, nutrition logs, and cloud sync intact.

---

## Local Android Studio Build & Firebase Setup

If you clone or export the repository locally:
1. Ensure a `debug.keystore` exists in the project root (not committed to Git):
   ```bash
   if [ -f ~/.android/debug.keystore ]; then
     cp ~/.android/debug.keystore ./debug.keystore
   else
     keytool -genkeypair -v -keystore debug.keystore -storepass android \
       -alias androiddebugkey -keypass android \
       -keyalg RSA -keysize 2048 -validity 10000 \
       -dname "CN=Android Debug,O=Android,C=US"
   fi
   ```
2. Register your local keystore's SHA-1 in Firebase Console (get it via `./gradlew signingReport` or `keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android`) and place the refreshed `google-services.json` in `app/`.
3. Build from Android Studio (**Build → Build Bundle(s) / APK(s) → Build APK(s)**) or via CLI:
   ```bash
   ./gradlew :app:assembleDebug
   ```

---

## Testing & Verification

- **Android JVM & Robolectric Unit Tests** (deterministic formulas, SemVer update checker, and app context checks):
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **Firestore Security Rules Tests** (runs against local Firestore Emulator using `firestore.rules` and `firestore.test.js`):
  ```bash
  FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
  ```

---

## Privacy, Security & Google Play Policy Compliance

- **Minimal Permissions**: Only `INTERNET` and `ACCESS_NETWORK_STATE` are declared in `AndroidManifest.xml`.
- **Zero Broad Storage Permissions**: Progress photos use the zero-permission Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) and are copied into private app-internal storage (`filesDir/progress_photos/<uid>/`). Image files never leave the device; only metadata syncs to Firestore.
- **No Dynamic Code Loading / No `REQUEST_INSTALL_PACKAGES`**: App updates open the official GitHub Release APK link via standard `Intent.ACTION_VIEW` in the system browser.
- **Zero-Trust Cloud Security**: All Firestore paths are strictly scoped to `/users/{userId}/...`, require Google Sign-In (`FirebaseAuth`), enforce field schemas, numeric bounds, immutable `createdAt`, and server timestamps (`firestore.rules`).

---

## Pin-to-Pin Technical Reference

For the exhaustive file-by-file, formula-by-formula, table-by-table, color-token-by-color-token, and Firestore-collection-by-collection specification, read:

➡️ **[`docs/TECHNICAL_REFERENCE.md`](docs/TECHNICAL_REFERENCE.md)**
