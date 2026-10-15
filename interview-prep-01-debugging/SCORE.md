# Assignment 01: Debugging — Score

## Round 4 (commit ff11bdd): 21 / 25. Pass.

| Dimension | R1 | R2 | R3 | R4 | Why |
|---|---|---|---|---|---|
| Correctness | 2 | 3 | 4 | 5 | All three defects fixed at the root. `added` now counts only real inserts. |
| Architecture and boundaries | 2 | 4 | 4 | 4 | Fixes in the right layers. `SyncManager.listeners` thread safety is noted but left; that is a fair call for this scope, and the right place to say so is where you said it. |
| Code quality and idiom | 2 | 3 | 3 | 4 | Round-2 cleanup landed: unused imports, the no-op `@Update` strategy, `advanceUntilIdle()`, the hardcoded test dependency. One leftover: the round-1 sync test (`on sync uiState updates after completion of sync`) is still there; it proves nothing and should go. |
| Tests | 1 | 2 | 2 | 4 | The gated test is correct: both syncs block on `syncGate`, `syncCalls == 3`, release, `syncCount == 3`. On `main` both would write `2`, so it fails there. The leak test now recreates the Activity under `DetectLeaksAfterTestSuccess`. Not a 5 because the dead test remains and the repository-level duplicate (unique index) has no test of its own; a DAO test with an in-memory Room database would cover it. |
| Communication | 2 | 2 | 3 | 4 | The log is now accurate, mechanism-level, and names the real tests; this is what the document is for. One slip: it mentions removing a `@Transaction` annotation that never existed. The commit message is still "Addressing PR comments v4"; a reviewer scanning history learns nothing from it. |

## What a 23 to 24 would have added

- Delete the round-1 sync test.
- A `NoteDaoTest` on an in-memory Room database proving two inserts with the same `remoteId` leave one row.
- Commit messages that name the change ("Add unique index on remoteId; count only real inserts").

## Interview notes

You can now answer every question in `INTERVIEW.md` from your own diff. Be ready in particular for 2 (why `Loading` is never observed under an unconfined dispatcher), 7 (the three layers a race can be fixed in and why you chose the index), and 9 (what happened to exceptions in the round-1 `sync()`). Those three are the questions that separate "fixed it" from "understood it".

---

## Round 3 (commit d7c4b40): 16 / 25. Still below the pass line (20), and close.

All three defects are now fixed in the code. What keeps this under 20 is verification and cleanup, not the fixes.

| Dimension | R1 | R2 | R3 | Why |
|---|---|---|---|---|
| Correctness | 2 | 3 | 4 | Unique index on `remoteId` plus `IGNORE` stops the duplicates; `it.syncCount + 1` fixes the counter. One leftover: `syncFromRemote` still does `added++` after every insert, but an ignored insert returns `-1`, so an overlapping sync reports "5 new" for rows it did not add. |
| Architecture and boundaries | 2 | 4 | 4 | Fix is now in the data layer where every caller benefits. Unchanged from round 2. |
| Code quality and idiom | 2 | 3 | 3 | None of the round-2 cleanup landed: unused imports, the no-op `@Update(onConflict = IGNORE)`, `advanceUntilIdle()` after the Turbine block, the misnamed delete test, the hardcoded `ui-test-junit4` in `testImplementation`. |
| Tests | 1 | 2 | 2 | The race has no test. The sync test is byte-for-byte the round-1 version and passes on `main`. The leak test still never recreates the Activity. |
| Communication | 2 | 2 | 3 | `DEBUG_LOG.md` is filled in, which is the point. But Defect 3 claims "transaction handling" that does not exist in the diff, and names a regression test ("No conflicts") that was never written. The log must describe what is actually in the branch. The commit message is "Addressing PR comments v2". |

## What is left for a pass

1. **The gated sync test.** `repository.syncGate = CompletableDeferred()`; `sync()` twice; `syncGate.complete(Unit)`; assert `repository.syncCalls == 2` and `syncCount` rose by exactly 2 beyond the init sync. On `main` (old counter) it reports one less. This is the single biggest missing item.
2. **Count only real inserts.** `if (dao.insert(...) != -1L) added++`.
3. **Cleanup list from round 2**, five minutes of work.
4. **Fix the log** so Defect 3 says index plus IGNORE plus CAS counter, and names the test you actually wrote.
5. Commit message that names the change, not the round.

With 1 and 2 this lands at about 20; with 3 and 4 as well, 22 or 23.

---

## Round 2 (commit b85b521): 14 / 25. Still below the pass line (20).

Two of three defects are now fixed properly. The race is not, and nothing in the PR proves it either way.

| Dimension | Round 1 | Round 2 | Why |
|---|---|---|---|
| Correctness | 2 | 3 | Crash: fixed at the root. Leak: both paths fixed (`viewModelScope`, listener removal). Race: `OnConflictStrategy.IGNORE` without a unique index on `remoteId` ignores nothing, because the only key is the auto-generated `id`; duplicates still insert. `countBefore + 1` still loses updates. |
| Architecture and boundaries | 2 | 4 | Sealed state and `LaunchedEffect` are the right shape. `container` nulling reverted, `applicationContext` restored. The button guard was correctly removed, but the repository is still not safe for overlapping callers. |
| Code quality and idiom | 2 | 3 | Unused imports (`StandardTestDispatcher`, `UnconfinedTestDispatcher`, `OnConflictStrategy` in the entity, `LeakCanaryReflectionHelper`). `@Update(onConflict = IGNORE)` is meaningless on an update by primary key. `advanceUntilIdle()` after a finished Turbine block does nothing. The hardcoded `ui-test-junit4` in `testImplementation` is still there. |
| Tests | 1 | 2 | The delete test is now real: subscribes, sees `Loaded`, then `Gone`. The leak test adds `DetectLeaksAfterTestSuccess` but never recreates the Activity, so it would not have caught the original bug; add `scenario.recreate()` before the assertion. The sync test is unchanged and still passes on `main`. |
| Communication | 2 | 2 | `DEBUG_LOG.md` is still empty. The round-2 commit is "Addressing PR comments" plus a merge commit; neither says what was wrong or what changed. |

## What is left

1. **Unique index.** `@Entity(tableName = "notes", indices = [Index(value = ["remoteId"], unique = true)])`. Only then does `IGNORE` on insert do anything. Check how `syncFromRemote` counts `added` once some inserts are ignored: `insert` returns `-1` for an ignored row.
2. **Counter.** `uiState.update { it.copy(isSyncing = false, syncCount = it.syncCount + 1) }`. Read the current value inside the CAS, not before the suspension.
3. **Sync test.** Set `repository.syncGate = CompletableDeferred()`, call `sync()` twice, then `syncGate.complete(Unit)`, then assert `syncCalls == 2` and `syncCount` went up by exactly 2 (plus the one from `init`). This fails on `main` because of the lost update, and passes once item 2 is in.
4. **`DEBUG_LOG.md`.** Three entries, mechanism not symptom. The round-1 crash entry should explain why `mapNotNull` was the wrong fix; that is the kind of reasoning the log is for.
5. Delete the unused imports and the `@Update` conflict strategy, rename the delete test for what it asserts, and drop `advanceUntilIdle()`.

Items 1 to 3 are about 15 lines. With those plus the log, this is a pass.

---

## Round 1 (commit c1d2c91)

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
