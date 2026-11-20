const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_athlete_123";
const BOB_UID = "bob_lifter_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTime(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfilePayload(uid, ts = pastTime()) {
  return {
    userId: uid,
    name: "Alice Strong",
    email: "alice@example.com",
    age: 28,
    gender: "FEMALE",
    heightCm: 168.0,
    currentWeightKg: 64.5,
    goalWeightKg: 60.0,
    goalType: "CUT",
    activityLevel: "MODERATE",
    trainingDaysPerWeek: 4,
    preferredSplit: "PPL",
    targetRateKgPerWeek: 0.45,
    calorieTarget: 1850,
    proteinTargetG: 135,
    cardioTargetMinPerDay: 20,
    onboardingCompleted: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated users are denied reads and writes across user profile and subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("workouts").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("food_logs").get()
  );
});

test("User profile: owner create, partial update preserving createdAt, and rejection of shadow/immutable/future fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastTime(15000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Partial update matching FirestoreSyncManager.upsertPreservingCreatedAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      currentWeightKg: 64.1,
      calorieTarget: 1820,
      updatedAt: pastTime(1000),
    })
  );

  // Reject mutating immutable createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastTime(500),
      updatedAt: pastTime(500),
    })
  );

  // Reject shadow/ghost field injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastTime(500),
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      currentWeightKg: 64.0,
      updatedAt: new Date(Date.now() + 120000),
    })
  );

  // Reject root users collection list scraping
  await assertFails(aliceDb.collection("users").get());
});

test("Cross-user isolation: Bob cannot read, list, or mutate Alice's profile or fitness subcollections", async () => {
  const ts = pastTime(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, ts));
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("weight_logs")
      .doc("wt_1")
      .set({
        weightKg: 64.5,
        dateKey: "2026-11-20",
        recordedAtEpochMs: Date.now(),
        note: "Morning",
        createdAt: ts,
        updatedAt: ts,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ name: "Hacked", updatedAt: pastTime() }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("weight_logs").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("weight_logs").doc("wt_1").delete());
});

test("Kotlin-mirrored subcollections: templates, workouts, cardio, foods, meals, food_logs, weight, measurements, progress_photos", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);
  const ts = pastTime(8000);
  const nowMs = Date.now();

  // Goals
  await assertSucceeds(
    userDoc.collection("goals").doc("goal_1").set({
      goalType: "CUT",
      startingWeightKg: 65.0,
      targetWeightKg: 60.0,
      targetRateKgPerWeek: 0.45,
      calorieTarget: 1850,
      proteinTargetG: 135,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Workout Template
  await assertSucceeds(
    userDoc.collection("templates").doc("tpl_push").set({
      name: "Push Day",
      description: "Chest & Triceps",
      exercises: [
        {
          exerciseId: "ex_bench_press",
          exerciseName: "Barbell Bench Press",
          muscleGroup: "CHEST",
          targetSets: 4,
          targetReps: 8,
          targetWeightKg: 60.0,
          restSeconds: 120,
        },
      ],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Workout Session (optional templateId omitted when null)
  await assertSucceeds(
    userDoc.collection("workouts").doc("wk_1").set({
      templateId: "tpl_push",
      templateName: "Push Day",
      dateKey: "2026-11-20",
      startedAtEpochMs: nowMs - 3600000,
      completedAtEpochMs: nowMs,
      durationMinutes: 60,
      totalVolumeKg: 4800.0,
      estimatedCalories: 310,
      notes: "Strong bench",
      sets: [
        {
          exerciseId: "ex_bench_press",
          exerciseName: "Barbell Bench Press",
          muscleGroup: "CHEST",
          setNumber: 1,
          weightKg: 80.0,
          reps: 8,
          rir: 2,
          warmup: false,
          completed: true,
          isPr: true,
          prLabel: "Heaviest Weight",
        },
      ],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cardio
  await assertSucceeds(
    userDoc.collection("cardio").doc("cardio_1").set({
      activityType: "WALKING",
      dateKey: "2026-11-20",
      durationMinutes: 25,
      distanceKm: 2.2,
      speedKmh: 5.3,
      inclinePercent: 6.0,
      resistanceLevel: 0,
      caloriesEstimated: 185,
      startedAtEpochMs: nowMs,
      notes: "Post-workout incline walk",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Custom Food
  await assertSucceeds(
    userDoc.collection("custom_foods").doc("cf_curry").set({
      name: "Homemade Chicken Curry",
      brand: "Home",
      servingSize: 100.0,
      servingUnit: "g",
      calories: 175.0,
      protein: 21.0,
      carbs: 6.0,
      fat: 7.5,
      fiber: 1.5,
      favorite: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Saved Meal
  await assertSucceeds(
    userDoc.collection("meals").doc("meal_breakfast").set({
      name: "My Breakfast",
      mealType: "BREAKFAST",
      items: [
        {
          foodItemId: "food_whole_egg",
          foodName: "Whole Egg (Large)",
          quantity: 4.0,
          unit: "egg",
          calories: 288.0,
          protein: 25.2,
          carbs: 1.6,
          fat: 19.2,
          fiber: 0.0,
        },
      ],
      totalCalories: 288.0,
      totalProtein: 25.2,
      totalCarbs: 1.6,
      totalFat: 19.2,
      totalFiber: 0.0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Food Log (omits null mealId)
  await assertSucceeds(
    userDoc.collection("food_logs").doc("fl_1").set({
      foodName: "Chicken Breast (Raw/Cooked)",
      foodItemId: "food_chicken_breast",
      quantity: 250.0,
      unit: "g",
      calories: 412.5,
      protein: 77.5,
      carbs: 0.0,
      fat: 9.0,
      fiber: 0.0,
      mealType: "LUNCH",
      dateKey: "2026-11-20",
      loggedAtEpochMs: nowMs,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Measurements (optional fields omitted when null)
  await assertSucceeds(
    userDoc.collection("measurements").doc("bm_1").set({
      dateKey: "2026-11-20",
      recordedAtEpochMs: nowMs,
      waistCm: 76.5,
      chestCm: 98.0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Progress Photo Metadata
  await assertSucceeds(
    userDoc.collection("progress_photos").doc("ph_1").set({
      photoType: "FRONT",
      dateKey: "2026-11-20",
      recordedAtEpochMs: nowMs,
      note: "Week 1 check-in",
      localFileName: "ph_1_front.jpg",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // List queries succeed on owner subcollections (even empty ones like exercises)
  await assertSucceeds(userDoc.collection("exercises").get());
  await assertSucceeds(userDoc.collection("templates").get());
  await assertSucceeds(userDoc.collection("workouts").get());
  await assertSucceeds(userDoc.collection("cardio").get());
  await assertSucceeds(userDoc.collection("food_logs").get());
  await assertSucceeds(userDoc.collection("measurements").get());
  await assertSucceeds(userDoc.collection("progress_photos").get());

  // Reject invalid @DocumentId "id" key inside body
  await assertFails(
    userDoc.collection("weight_logs").doc("wt_bad").set({
      id: "wt_bad",
      weightKg: 70.0,
      dateKey: "2026-11-20",
      recordedAtEpochMs: nowMs,
      note: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
