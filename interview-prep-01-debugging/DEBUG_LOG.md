# FieldNotes Debugging Log

## Defect 1: Crash on Delete

- **Repro:** Open any note in `NoteDetailScreen` and tap **Delete**. The app crashes with `NullPointerException`.
- **Root cause:** In `DefaultNotesRepository.observeNote(id)`, Room's DAO query `observeById(id)` emits `null` when a note row is deleted. The repository previously used `map { it!! }` (or `mapNotNull`, which suppresses `null` emissions and leaves `NoteDetailViewModel.noteUiState` stuck in `NoteDetailUiState.Loaded` showing a deleted note).
- **Fix:** `NotesRepository.observeNote(id)` returns `Flow<Note?>`. `NoteDetailViewModel` maps `null` to `NoteDetailUiState.Gone`. `NoteDetailScreen` uses `LaunchedEffect(noteUiState)` to navigate back (`onBack()`) when state becomes `Gone`.
- **Regression test:** `on delete transitions noteUiState to Gone` in `NotesListViewModelTest.kt` (subscribes via Turbine, observes `Loaded`, calls `delete()`, and asserts `Gone`).
- **How I found it:** Stack trace inspection (`NullPointerException` at `DefaultNotesRepository.kt`) and Room flow emission analysis.

## Defect 2: Memory Leak on Device Rotation

- **Repro:** Rotate the device multiple times while app is running. Profiling with LeakCanary reports leaked `MainActivity` instances.
- **Root cause:** 
  1. `MainActivity.onCreate()` registered an anonymous `SyncManager.Listener` lambda capturing `this@MainActivity` to show a Toast. `SyncManager` is a process-wide singleton (`object`). `MainActivity` did not unregister the listener in `onDestroy()`, retaining destroyed activity instances.
  2. `NotesListViewModel.observeNotes()` launched a coroutine on `GlobalScope.launch(Dispatchers.Main)`, which persisted past `ViewModel.onCleared()` and kept collecting Room flows indefinitely (the "list screen keeps doing work after you leave it" lead).
- **Fix:** Unregister `toastListener` via `SyncManager.removeListener(it)` in `MainActivity.onDestroy()`. Replace `GlobalScope.launch` with `viewModelScope.launch` in `NotesListViewModel`.
- **Regression test:** `onCreateInitializesAllPropertiesAndRecreatesWithoutLeaks` in `MainActivityTest.kt` using `scenario.recreate()` with `DetectLeaksAfterTestSuccess`.
- **How I found it:** LeakCanary heap dump inspection and static code audit for `GlobalScope` usage.

## Defect 3: Race Condition on Concurrent Syncs

- **Repro:** Tap **Sync** twice quickly (or tap sync while auto-sync on launch is running). Duplicate notes appeared in the list, and the "Syncs: X" counter missed increments.
- **Root cause:** 
  1. `DefaultNotesRepository.syncFromRemote()` executed a check-then-act query (`known = dao.getAllOnce()`) before inserting remote notes. Overlapping sync calls both read `known` before either inserted, inserting identical remote notes without a unique index on `remoteId`.
  2. `NotesListViewModel.sync()` captured `countBefore = uiState.value.syncCount` before network suspension and assigned `countBefore + 1` post-suspension, overwriting concurrent increments.
- **Fix:**
  1. Added unique index `@Index(value = ["remoteId"], unique = true)` to `NoteEntity`, set `@Insert(onConflict = OnConflictStrategy.IGNORE)` on `NoteDao.insert()`, and incremented `added` count only when `dao.insert(...) != -1L`.
  2. Updated `syncCount` using `uiState.update { it.copy(isSyncing = false, syncCount = it.syncCount + 1) }` for atomic Compare-And-Set (CAS) state updates.
- **Regression test:** `overlapping syncs atomically increment syncCount without lost updates` in `NotesListViewModelTest.kt` using `FakeNotesRepository.syncGate` (`CompletableDeferred`).
- **How I found it:** Code analysis of concurrent `syncFromRemote` database queries and `StateFlow` state mutations.

## Anything else you noticed

- `SyncManager.listeners` is a `MutableList` accessed across threads; replacing with `CopyOnWriteArrayList` or enforcing `@MainThread` would prevent potential `ConcurrentModificationException`.
- Removed unused imports and redundant `@Transaction` / `@Update(onConflict = IGNORE)` annotations across DAO and test classes.
