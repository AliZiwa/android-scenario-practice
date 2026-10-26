# Assignment 03: Code smells, score

## Round 1 (commit a6799b2): 12 / 25. Below the pass line (20).

The seam is right: `ShiftRepository(dao, api, dispatcher)` and `TimesheetViewModel(repository)` are the two moves that make the ViewModel testable on the JVM, `SubmitResult` is the right return type, the Toast is out of the data layer, the week totals are out of the composable, and `UtilsTest` now pins most of `Utils`. That is a sound first hour.

What sinks the round is the brief's one hard rule. The assignment was "make it safe to change, without changing what it does", and the diff changes what it does in three places: the hourly rate preference is no longer read anywhere, "Submit all" now fires every draft at the server concurrently, and the database schema is bumped with a migration whose committed version 1 schema is not the version 1 schema on `main`. On top of that the process evidence is missing: one commit, 774 files including a 12.9 MB APK, `.idea/` and `app/build/`, so "characterization tests first, one smell per commit, green after every commit" cannot be verified and by the history did not happen.

| Dimension | Score | Why |
|---|---|---|
| Correctness | 2 | Behaviour contract line 3 broken: `Utils.rate(context: Context? = null)` returns 42.0 unless a `Context` is passed, and no caller passes one (`TimesheetViewModel.updateShifts` line 143, `ShiftRow` line 154). A technician whose `hourly_rate` preference is 55 now sees every pay figure at 42. `submitAllDrafts` went from sequential to `async`/`awaitAll`, which changes ordering, server load and failure interleaving; the count happens to match. `tmp` was removed and the schema bumped to 2 with a `@DeleteColumn` auto migration, which is the exact kind of latent-TODO "fix" the brief said to note and leave; and the committed `1.json` has no `tmp` column, so it does not describe the database that is on devices today. The refactors that are pure (UiState, constructor injection, Elvis, per-call `SimpleDateFormat`, `finally` for the loading flag) preserve behaviour as far as reading the diff can tell. No tests were proven on `main` first; the only tests that compile against `main` are the `UtilsTest` additions. |
| Architecture and boundaries | 3 | The dependency direction is now correct for the repository and the ViewModel, and `SubmitResult` keeps HTTP out of the ViewModel. Left: the ViewModel still defaults to `Injector.repository`, so the locator is hidden rather than removed; `Utils.rate()` is still a static that reaches for Android (now worse, since it silently falls back); `ShiftRow(shift, vm)` still hands the whole ViewModel to a row; `formatRow` is presentation logic in the ViewModel; the entity returned by Room is still mutated in place by `submit` and used directly as the UI model; `catch (e: Exception)` in `ShiftRepository.submit` still swallows `CancellationException`, the same point raised in assignment 02. Business rules are now in two places (`Utils` and `updateShifts`) rather than three; not one. |
| Code quality and idiom | 2 | The small moves are textbook: `?:` for the `!!`, named-argument construction of `ShiftEntity`, `_uiState.update { it.copy(...) }`. The big ones are not: `rate(context: Context? = null)` wrapped in a catch-all returning 42.0 is a nullable-parameter hack that hides a behaviour change, and the textbook move for "static that reads prefs" is to inject a `RateProvider` (or the rate itself) into the ViewModel. `async`/`awaitAll` is an optimisation, not a refactoring, and "N+1" is a query smell, not this. `Injector`'s new doc comment says nothing the old one did not. `gradle/gradle-daemon-jvm.properties` with `toolchainVersion=25` is back after being called out in assignment 02. 759 generated files, including KSP output and an APK, are in the diff. |
| Tests | 3 | `UtilsTest` is the strongest part of the submission: nine tests that pin rounding, pay, formatting, `weekStart` on Monday, Wednesday and Sunday, `isSubmitted` case-insensitivity, and `statusLabel`, all of which pass on `main` unchanged. The repository and ViewModel tests use the provided fakes correctly and cover create, validate, submit, failure, submit-all and delete. What they do not pin: pay as a number (`assertTrue(state.totalPay > 0.0)` would pass at any rate, which is how the rate bug got through), newest-first ordering, "Already submitted", `approved` for an unbillable shift, the failed submit leaving the Room row as `draft`, and the rate preference. `initial load fetches shifts into uiState` asserts nothing about shifts. No migration test, and none is possible against the committed `1.json`. The tests did not land before and after the seam, they landed with everything else. |
| Communication | 2 | `SMELLS.md` has fourteen rows and the top four are the right four, with correct names and a defensible order. After that it pads: "Unhandled Schema Integrity" is a problem created by row 14, not found in the code; "N+1" is the wrong name; "Redundant Serializer Instantiation" and "Unsafe Force Unwrap" are one-liners presented as smells. Every risk is "Low" or "Minimal", including shipping an untested on-device migration and a concurrency change. "Ensure 100% test coverage" is not true. The sentence "Deliberately left untouched per explicit user instructions ('Ignore anything in the Faketimessheetbackend file')" reads as a tool quoting its operator, not as an engineer writing an audit; see the note below. Commit history: one commit, "assignmnet 3 completed", 774 files. PR title does not follow the brief's format and the body is empty. |

## A note on the rules

The rules say no AI-generated or AI-suggested code. The `SMELLS.md` sentence quoted above, the "100% test coverage" claim, and the wording of several rows ("resolve the sequential N+1 bottleneck", "triggering compiler performance warnings") read like assistant output. I am not asserting that they are, but a reviewer at a company would ask, and so would an interviewer. Say in the PR body how this was produced. If an assistant wrote the catalog or the code, the honest fix is to redo those parts by hand on this branch; the score would be re-done against that.

## Smell by smell

### 1. Service locator and leaked context. Fixed, mostly.

`Injector.context` and `currentUser` are gone, `ShiftRepository` takes `ShiftDao` and `TimesheetApi`, the ViewModel takes the repository. Right first move; it is what made the rest testable. Left: `Injector.repository` as a default parameter means the ViewModel still compiles against the locator and the real app still goes through it. The textbook step is a `ViewModelProvider.Factory` (or `viewModel { TimesheetViewModel(Injector.repository) }` at the call site) so the ViewModel class has no idea `Injector` exists.

### 2. Toast and `Dispatchers.Main` in the repository. Fixed.

`SubmitResult.Success | Failure` and the ViewModel maps it to a message. Exactly right. `Log.e` went too, which is fine. `lastError` and `submitCount` were dead state and are gone.

### 3. Three mutable flows plus `data2` plus `lastError`. Fixed.

Single `TimesheetUiState`, private `MutableStateFlow`, public `StateFlow`, `update { copy }`. The cache (`data2`) and the "force an emission" copies are gone with it. Good.

### 4. Week totals computed in the composable. Fixed.

Moved into `updateShifts`, which is the one place the list changes. Good. It still calls `Utils.rate()` from inside the ViewModel, which is where the rate bug lives.

### 5. "N+1" in `submitAllDrafts`. Not a smell; the change is a behaviour change.

Sequential submits were a design choice with consequences you can reason about: one failure, one retry, server ids in list order, one in-flight request on a phone. Concurrent submits change all of that. The brief said "not even ones you are sure are improvements". Put the loop back and move "could be parallel" to "What you deliberately left".

### 6. Schema migration. Self-inflicted, and the evidence is wrong.

`tmp` was a dead column with a TODO. Removing it needs a schema bump, a migration, a committed version 1 schema that matches what is on devices, and a migration test. The branch has the first two. The committed `1.json` was exported after `tmp` had already been removed (its `identityHash` is identical to `2.json`), so it does not describe version 1 as it exists on `main` or on any installed device; a migration test built from it would prove nothing. The generated migration in `app/build` happens to rebuild the table without `tmp`, which is why the APK works, but nothing in source proves it. The low-risk path for this assignment: leave `tmp`, list it under "What you deliberately left", and ship the column removal as its own PR with `1.json` from `main` and a `MigrationTestHelper` test.

### 7. `data2` and cryptic naming. Fixed with 3.

### 8. Flag arguments on `load(forceRefresh, silent)`. Fixed.

`load()` with no parameters, `submit` and `submitAll` fetch the list themselves. Behaviour preserved: the old `load(true, true)` was silent and the callers already toggled `loading`.

### 9. Redundant `withContext(Dispatchers.IO)` in the ViewModel. Fixed.

Dispatcher injected into the repository, ViewModel no longer dispatches. Right.

### 10. `Json { }` built inline. Trivial, fixed.

### 11. `Utils` as a procedural dumping ground. Claimed, not done.

`Utils` has the same fifteen functions it had. The only structural change is `rate(context)`, which made it worse. A real step would be a `PayCalculator` or `ShiftRules` object owning `workedMinutes`, `calcPay` and the week filter, with the formatters left where they are.

### 12. `SimpleDateFormat` statics. Fixed.

Per-call instances. Correct and safe. `java.time.DateTimeFormatter` would be the modern move; this is fine.

### 13. `!!` on `rateOverride`. Fixed, one line.

### 14. Dead code and TODOs. Fixed, but see 6.

### Not in the catalog

- Duplicate `Injector.init` in `TimesheetApp` and `MainActivity`. You removed the activity call, which is right, but it is not a row.
- `ShiftRow(shift, vm)`: a row composable that takes the whole ViewModel. Pass `onSubmit`/`onDelete` lambdas and the formatted strings.
- `ShiftRow` builds two `SimpleDateFormat`s per row per recomposition and uses `!!` on `parse`.
- `formatRow` in the ViewModel: presentation strings belong in the UI state or a mapper.
- Mutable `var` entity as the UI model; `submit` mutates the object the list is showing.
- `catch (e: Exception)` around a suspend call in `submit` swallows `CancellationException`.
- Magic strings `"draft"`, `"submitted"`, `"approved"` compared in three places with inconsistent case handling.
- `ShiftDao.observeAll()` exists and nothing uses it; the ViewModel polls with manual reloads instead.
- `Utils.rate()` reads SharedPreferences on every recomposition and every row (that was the real smell at that site).
- Work in the constructor: `init { load() }`.

## What a pass looked like

- Commit 1: `UtilsTest` additions, passing on `main`. Commit 2: constructor injection into the repository and ViewModel, nothing else. Commit 3: `ShiftRepositoryTest` and `TimesheetViewModelTest` pinning create, order, submit success and failure (row still `draft`), `approved` for unbillable, "Already submitted", submit-all count, delete, and pay as a number. Then one commit per smell with its name in the message, each leaving the tests green.
- `Utils.rate()` replaced by a rate the ViewModel receives (a `RateProvider` interface backed by SharedPreferences in the app, a constant in tests), with a ViewModel test that proves a rate of 55 changes `totalPay`.
- `submitAllDrafts` left sequential. `tmp` left in place and listed under "What you deliberately left".
- `SMELLS.md` with ten honest rows, at least one risk that is not "Low", and the duplicated week loop and `ShiftRow(shift, vm)` in it.
- No `app/build`, `.idea` or `gradle-daemon-jvm.properties` in the diff. The base `.gitignore` only covers the root `build` directory, so add `build/` and `.idea/` to it and `git rm -r --cached` the rest.

## Next step

Push fixes to this branch, do not open a new PR. In order:

1. `git rm -r --cached interview-prep-03-code-smells/app/build interview-prep-03-code-smells/.idea interview-prep-03-code-smells/gradle/gradle-daemon-jvm.properties`, fix `.gitignore`, commit. The diff should be about fifteen files.
2. Restore the rate preference: inject the rate (or a provider) into the ViewModel, read SharedPreferences in the app wiring only, and add the ViewModel test that pins a non-default rate.
3. Put `submitAllDrafts` back to a sequential loop. Put `tmp` back, or keep the removal and replace `1.json` with the schema exported from `main` plus a migration test; the first is the safer call inside this assignment.
4. Pin pay as a number, newest-first order, "Already submitted", `approved` for unbillable, and the failed-submit row still `draft`.
5. Rewrite the `SMELLS.md` risk column honestly, drop rows 5 and 6, add the duplicated week loop and `ShiftRow(shift, vm)`, and say in the PR body how the work was produced.

Items 1 to 3 get this to about 18; all five is a pass. Even with one commit behind you, the fix commits can show the sequence the brief asked for: make each one a single smell with the smell's name in the message.
