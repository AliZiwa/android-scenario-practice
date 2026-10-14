# Assignment 01: Debugging — Score

**Result: 9 / 25. Below the pass line (20).** The crash fix is sound, the leak fix is half right, the race is not fixed and the change introduces a new crash path. None of the three tests fail on `main`. Details below, then what a pass would have looked like.

| Dimension | Score | Why |
|---|---|---|
| Correctness | 2 | Crash fixed. Leak: listener leak fixed, `GlobalScope` collector not. Race: UI guard only; repository still duplicates; `try/catch` moved outside `launch` so a failing sync now crashes the app and leaves `isSyncing = true` forever. |
| Architecture and boundaries | 2 | Fix for a data-layer race landed in the UI (`enabled = !isSyncing`). `container` made nullable and nulled in `onDestroy` for no reason. `applicationContext` removed from the Room builder. |
| Code quality and idiom | 2 | Explicit backing field is fine. Hardcoded `ui-test-junit4` version bypasses the Compose BOM and is added to `testImplementation` for a test that lives in `androidTest`. |
| Tests | 1 | Delete test asserts `note.value == null` on a `WhileSubscribed` StateFlow that is `null` before anyone subscribes: passes on `main`. Sync test runs four syncs on an unconfined dispatcher with no `syncGate`: passes on `main`. Activity test checks that your own nulling ran, not that the Activity is collectable. |
| Communication | 2 | Commit messages name the symptom, and the race commit names a wrong cause (`update` on a StateFlow is a synchronous compare-and-set, not a separate coroutine). `DEBUG_LOG.md` is untouched. PR description is one line. |

## Defect by defect

### 1. Crash on delete (`NotesRepository.observeNote`)

**Your fix:** `map { it!!.toDomain() }` to `mapNotNull { it?.toDomain() }`. The NPE is gone.

**What it misses:** `mapNotNull` hides the event instead of handling it. After delete, the detail screen's `StateFlow` keeps the last note forever and the screen stays open on a note that no longer exists; it only closes because `NoteDetailScreen` calls `onBack()` right after `delete()`, which is a coincidence of ordering, not a design. The honest signature is `Flow<Note?>`, with the detail screen treating `null` as "note is gone, navigate back". That also removes the `delete(); onBack()` race in the composable.

**Your test:** `NoteDetailViewModel.note` is `stateIn(..., WhileSubscribed(5_000), null)`. Nothing subscribes in the test, so `.value` is `null` before and after `delete()`. Run it against `main`: it passes. A real test subscribes (Turbine `note.test { ... }`), awaits the loaded note, deletes, and asserts the next emission.

### 2. Memory leak (`MainActivity`, `NotesListViewModel`)

**Your fix:** keep the listener in a field, remove it in `onDestroy`. Correct: the lambda captured `this@MainActivity` and `SyncManager` is a process-wide singleton, so every rotation leaked an Activity. This was the main retention path.

**What it misses:** `observeNotes()` collects on `GlobalScope.launch(Dispatchers.Main)`. That coroutine never ends, so it retains the `ViewModel` (and its repository) after `onCleared`, and it keeps collecting Room after the screen is gone. That is the "list screen keeps doing work after you leave it" lead, and it is the other half of the leak picture. Fix is `viewModelScope.launch` (and `stateIn` would be more idiomatic still).

**Changes that should not be there:**
- `container` is application-scoped; making it nullable and nulling it in `onDestroy` fixes nothing and adds `?.let` branches to `setContent`. Activities are collected when nothing references them; nulling their fields does not help.
- `AppContainer`: `context.applicationContext` became `context`. The caller passes the `Application`, so it works today, but you removed the guard that makes the container safe to construct from any context. Why?

**Your test:** `onDestroyClearsAllProperties` asserts `container == null`, which is your implementation, not the defect. A leak regression test is either (a) LeakCanary's `DetectLeaksAfterTestSuccess` rule in `androidTest`, or (b) a unit test on `SyncManager` asserting the listener count returns to zero after the Activity's remove call. (b) was reachable in ten lines.

### 3. Race on double sync (`DefaultNotesRepository.syncFromRemote`, `NotesListViewModel.sync`)

**Root cause (not identified in the PR):** two things.
- `syncFromRemote` reads `known`, suspends on the network, then inserts. Two overlapping calls both compute `known` before either inserts, so both insert the same five rows. The fix belongs in the data layer: a unique index on `remoteId` with `OnConflictStrategy.IGNORE`, or a `@Transaction` DAO method that does the read-and-insert together, or a `Mutex` around the sync. Any one of those makes the repository safe for every caller, including the auto-sync on launch.
- `sync()` captures `countBefore` before the suspension and writes `countBefore + 1` after. Two overlapping syncs both read N and both write N+1. `update { it.copy(syncCount = it.syncCount + 1) }` fixes it in one line, because `update` is a CAS loop on the current value.

**Your fix:** `enabled = !state.isSyncing` on the button. That stops the user tapping twice. It does not stop the launch auto-sync overlapping a tap on a slow network, and it leaves `syncFromRemote` unsafe for any future caller. The restructured `sync()` still has the `countBefore` lost update.

**The regression:** `try/catch` now wraps `viewModelScope.launch`, which returns immediately. An exception inside the coroutine is no longer caught: it propagates to `viewModelScope`'s handler and crashes the process, and `isSyncing` is never reset, so the button stays disabled. On `main`, a failed sync showed an error message. On your branch, it kills the app.

**Your test:** with `UnconfinedTestDispatcher` and `syncGate == null`, each `sync()` runs to completion before the next starts. Nothing overlaps, so `syncCount == 5` on `main` too. `FakeNotesRepository.syncGate` exists so you can hold the first sync open, start the second, release both, and assert `syncCount == 2` and `syncCalls == 2`.

### 4. The unreproduced lead

Not addressed. See `GlobalScope` above.

## What a pass looked like

- `Flow<Note?>` and a detail screen that navigates back on `null`.
- `viewModelScope` in `observeNotes` (or `stateIn`), listener removal in `onDestroy` as you did.
- Unique index on `remoteId` plus `OnConflictStrategy.IGNORE` (or `@Transaction`), and `syncCount = it.syncCount + 1` inside `update`.
- Three tests that fail on `main`: Turbine on the detail flow, a `SyncManager` listener-count test, and a gated-overlap sync test.
- `DEBUG_LOG.md` with the mechanism for each, in your words.

Roughly 40 lines of change. You wrote 113 and moved in the wrong direction on the race.

## Next step

Push fixes to this branch (do not open a new PR). Re-score on request. The `INTERVIEW.md` questions are the ones an interviewer would ask about this diff as it stands; be able to answer them before resubmitting.
