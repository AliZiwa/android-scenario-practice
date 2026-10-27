# Smell catalog

One row per smell, ranked by the order you would fix them in. Use the standard name where one exists (Fowler, or the Android equivalent) so a reviewer can look it up. "Why it matters here" is about this codebase, not the textbook. Mark the ones you fixed.

| # | Smell (standard name) | Where (file, symbol) | Why it matters here | Fix | Risk of the fix | Fixed? |
|---|---|---|---|---|---|---|
| 1 | Service Locator & Leaked Context | `Injector.kt`, `ShiftRepository.kt` | Global static fields holding `Context` and `ShiftRepository` couple data layer to Android framework, causing memory leaks and making ViewModel untestable on JVM. | Inject `ShiftDao` and `TimesheetApi` into `ShiftRepository`; inject `ShiftRepository` into `TimesheetViewModel`. Remove `context` static field and `@SuppressLint` from `Injector`. | Medium. Requires updating constructor call sites in ViewModel and app wiring. | Yes |
| 2 | Framework Leak in Data Layer | `ShiftRepository.kt`, `submit()` | Repository switches to `Dispatchers.Main` and invokes `Toast.makeText()`, mixing UI feedback with data fetching and breaking unit tests on JVM. | Remove `Toast` and `Context` from repository; pass errors back via structured `SubmitResult`. | Low. ViewModel maps `SubmitResult` to user messages cleanly. | Yes |
| 3 | Fragmented & Publicly Mutable State | `TimesheetViewModel.kt`, `shifts`, `loading`, `message` | Exposing loose `MutableStateFlow` fields allows external composables to directly mutate ViewModel state and causes inconsistent UI updates. | Consolidate state into an immutable `TimesheetUiState` exposed via read-only `StateFlow`. Remove unused duplicate ViewModel properties. | Medium. Requires updating Composable to collect single state stream. | Yes |
| 4 | Hardcoded Rate / Direct Preference Reading | `Utils.kt`, `rate()`, `TimesheetViewModel.kt` | `Utils.rate()` hardcodes SharedPreferences lookup on every calculation and recomposition, making hourly rate untestable in unit tests. | Inject a `RateProvider` interface into `TimesheetViewModel`, backed by `SharedPreferencesRateProvider` in app wiring and controllable values in tests. | Medium. If default values or fallback rates differ, calculations could regress; pinned by tests. | Yes |
| 5 | Business Logic in UI Component | `TimesheetScreen.kt`, `for (s in shifts)` | `@Composable` screen calculates weekly worked minutes, overtime, and total pay inside UI recomposition loop. | Move weekly calculations into ViewModel (`updateShifts`) and expose in `UiState`. | Low. Verified by characterization tests. | Yes |
| 6 | Passing ViewModel to Child Composable | `TimesheetScreen.kt`, `ShiftRow(shift, vm)` | Passing the entire `TimesheetViewModel` instance into `ShiftRow` couples row composables to ViewModel instance and triggers unnecessary recompositions. | Pass specific lambdas (`onSubmit`, `onDelete`) and formatted string values into row composables. | Low. Pure Compose UI cleanup. | Yes |
| 7 | Flag Arguments (Boolean Literal Parameters) | `TimesheetViewModel.kt`, `load(forceRefresh, silent)` | Calling `load(true, true)` with boolean literals obscures function intent at call sites. | Remove boolean flags and use clean default parameters. | Minimal. Purely call-site readability. | Yes |
| 8 | Redundant Dispatcher Switching | `TimesheetViewModel.kt`, `createShift()` | ViewModel wraps repository calls in `withContext(Dispatchers.IO)` when repository already handles thread dispatching. | Inject `CoroutineDispatcher` into Repository and remove redundant `withContext` in VM. | Low. Ensures main-safe repository methods. | Yes |
| 9 | Redundant Serializer Instantiation | `Injector.kt`, `addConverterFactory()` | Calling `Json { ignoreUnknownKeys = true }` inline creates new Json serializer instances on every initialization, triggering compiler performance warnings. | Store reusable top-level `Json` configuration property. | Minimal. Fixes compiler warning cleanly. | Yes |
| 10 | Thread-Unsafe Date Formatting | `Utils.kt`, `DATE_FMT`, `DISPLAY_FMT` | Static `SimpleDateFormat` instances are not thread-safe and can throw exceptions or corrupt dates under concurrent coroutines. | Use localized/thread-safe date formatters per call. | Low. Verified by characterization tests. | Yes |
| 11 | Unsafe Force Unwrap (`!!`) | `Utils.kt`, `shift.rateOverride!!` | Unnecessary `!!` non-null assertion risks runtime NullPointerException. | Replace with Kotlin Elvis operator `?:`. | Minimal. Safer fallback. | Yes |
| 12 | Dead Code & Leftover Declarations | `Injector.kt` (`currentUser`), `TimesheetViewModel.kt` (`weekSummary()`), `Utils.kt` (`rate()`) | Unused variables (`currentUser`), dead methods (`weekSummary()`), and dead procedures (`Utils.rate()`) pollute codebase and create confusion. | Delete unused declarations, dead code, and resolved TODO comments. | Minimal. | Yes |

## Order and reasoning

Why this order? Which smell did you fix first and what did it unlock?

1. **Service Locator & Leaked Context (`Injector.kt` & `ShiftRepository.kt`)** was fixed first. Decoupling `ShiftRepository` from `Context` and `Injector` created the primary seam needed to instantiate `ShiftRepository` and `TimesheetViewModel` in JVM unit tests using `InMemoryShiftDao` and `FakeTimesheetApi`.
2. **Framework Leak in Data Layer (`ShiftRepository.kt`)** was fixed next. Removing `Toast.makeText` from the repository eliminated Android UI framework dependencies from data operations and introduced structured `SubmitResult`.
3. **Hardcoded Rate / SharedPreferences Access (`RateProvider.kt`)** introduced constructor injection for hourly rates into `TimesheetViewModel`, preserving `SharedPreferences` behavior in production while allowing custom rate assertions ($55/hr -> $605.00) in unit tests.
4. **ViewModel State Management & UI Decoupling** (`TimesheetViewModel.kt` and `TimesheetScreen.kt`) consolidated loose state flows into a clean `UiState`, removed unused duplicate properties, and removed business calculations from Composable UI.
5. Finally, **Utility cleanup, dead code removal, thread safety, and unit testing** (`Utils.kt`, `ShiftRepositoryTest.kt`, `TimesheetViewModelTest.kt`, `UtilsTest.kt`) ensure robust test coverage and safe concurrency across all layers.

## What you deliberately left

Smells you found and chose not to fix in the timebox, with one line each on why they can wait.

- **`tmp` Column in `ShiftEntity.kt`**: Retained `tmp` column in version 1 schema to avoid shipping an untested database schema migration without a dedicated MigrationTestHelper.
- **Sequential Draft Submissions in `submitAllDrafts`**: Retained sequential submission loop in `ShiftRepository` to preserve backend rate-limiting, server ID ordering, and failure interleaving behavior.
- **`FakeTimesheetBackend.kt` hardcoded strings & interceptor rules**: Left intact as it serves as a reliable simulator, and modifying its hardcoded strings falls outside the scope of improving the production UI and data codebase.
- **Full Jetpack Navigation implementation**: Single-screen Compose UI works reliably and adding multi-screen navigation is outside the refactoring scope without behavior contract changes.
