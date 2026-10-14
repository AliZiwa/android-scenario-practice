# Assignment 01: the questions this diff invites

Answer each out loud in under two minutes. These are the follow-ups a Staff interviewer would ask after reading your PR.

1. You replaced `map { it!! }` with `mapNotNull`. What does the detail screen show thirty seconds after a delete if nobody calls `onBack()`? What signature would make the "note is gone" case explicit?
2. `NoteDetailViewModel.note` uses `WhileSubscribed(5_000)`. What is `note.value` before the first subscriber? Why does that make your delete test pass on the unfixed code?
3. Explain, in terms of object references, why the `SyncManager` lambda leaked the Activity. What else in this codebase holds a reference past its owner's lifetime?
4. `GlobalScope.launch(Dispatchers.Main)` in a ViewModel: what happens to that coroutine after `onCleared()`? What does it retain?
5. You made `container` nullable and null it in `onDestroy`. Does nulling an Activity's fields help the garbage collector reclaim the Activity? Why or why not?
6. Why did you remove `.applicationContext` from the Room builder? What would break if `AppContainer` were ever constructed with an Activity context?
7. Walk me through two overlapping `syncFromRemote` calls line by line. At which line does the duplicate become inevitable? Name three different layers you could fix it in and the trade-off of each.
8. `MutableStateFlow.update {}`: is it synchronous? What does it guarantee under concurrent updates? Given that, why did `syncCount` still end up low?
9. In your restructured `sync()`, where does an exception thrown inside `repository.syncFromRemote()` go? What is the user-visible result?
10. Disabling the button while `isSyncing` is true: which overlapping-sync scenario does that leave open?
11. `FakeNotesRepository.syncGate` is a `CompletableDeferred`. Write, in words, a test that uses it to prove two syncs overlap and only one set of rows is inserted.
12. You added `ui-test-junit4` with a hardcoded version to `testImplementation`. The project already has the Compose BOM and `ui-test-junit4` under `androidTestImplementation`. What does the hardcoded version do to the rest of the Compose dependency graph?
13. Explicit backing fields (`val uiState: StateFlow<T>` with `field = MutableStateFlow(...)`): what does it buy over the `_uiState` convention, and what Kotlin language version does it require?
14. `SyncManager.listeners` is a plain `MutableList` and `notifyFinished` is called from the repository. From which thread? What could go wrong if two syncs finish at once?
15. If you had thirty more minutes on this PR, which one change would you make first, and why?
