# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common CLI commands

```bash
# Build a Debug variant (debugging in Android Studio or an emulator)
./gradlew assembleDebug

# Build a Release variant (for production signing)
./gradlew assembleRelease

# Run Android Lint on the whole project
./gradlew lint

# Run all JVM unit tests
./gradlew test

# Run all Android instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a *single* JVM unit test class
./gradlew testDebugUnitTest --tests <fully.qualified.TestClassName>

# Run a single Android instrumented test method
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<fully.qualified.TestClassName>::<testMethod>
```

## High‑level architecture

- **`GameState`** – Be `ViewModel` that owns the game board, dictionary reference, solver, and live data for computer and player results. It provides methods to shuffle the board, process user guesses, and coordinate the background `SolveTask`.

- **Dictionary layer** – `Dictionary` interface defines look‑ups. Implementations include:
  - `EnglishWordDefinitionLookupServiceFreeApi` (fetches definitions from freedictionaryapi.com)
  - `GermanWordDefinitionLookupService` (queries a local German dictionary)
  - `WordInfoCache` / `WordLookupTask` manage caching of lookup results.

- **Solver** – `SolveTask` performs an exhaustive search on the 5×5 grid. It first builds all prefixes, then for each prefix fetches candidate words from the chosen dictionary, and finally validates them against the board.

- **UI layer** – Android fragments (`WordFinderSettingsFragment`, `ComputerResultListAdapter`, `InfoDialogFragment`, etc.) observe `GameState` via `LiveData` and update the UI. The app follows an MVVM pattern.

- **Misc utilities** – `letters` package provides deterministic and random letter generation; `fireworks` provides visual feedback on success.

## Build configuration

- Gradle settings in `build.gradle` (project and app) use Kotlin 2.2.0, Gradle 8.13.2, Android SDK 36. The `outputFile` for the APK is `app/build/outputs/apk/<variant>/app-<variant>.apk`.
- `gradle-wrapper.properties` ensures the correct Gradle distribution.

## Notable files

- `app/src/main/kotlin/org/carstenf/wordfinder/GameState.kt` – the core model.
- `app/src/main/kotlin/org/carstenf/wordfinder/dictionary/EnglishWordDefinitionLookupServiceFreeApi.kt` – HTTP client for definitions.
- `app/src/main/kotlin/org/carstenf/wordfinder/util/SolveTask.kt` – background solver.
- `app/src/main/kotlin/org/carstenf/wordfinder/gui/ComputerResultListAdapter.kt` – UI adapter for results.
- `app/src/main/AndroidManifest.xml` – declares activities and permissions.

---

Thanks for contributing!"