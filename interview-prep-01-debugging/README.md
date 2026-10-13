# Assignment 01: Debugging

**Area:** Debugging · **Timebox:** 3 hours · **Due:** Thursday, October 15 · **Branch:** `01-debugging` · **Submit:** pull request into `main`

## The app

FieldNotes is a small Jetpack Compose app for field technicians: a list of notes, a detail screen, and a "Sync" that pulls notes from a (fake, in-process) server into Room. Kotlin, Compose, ViewModel + StateFlow, Room with Flow, coroutines, hand-rolled DI in `AppContainer`. No navigation library; the two screens are switched by state in `FieldNotesNavHost`.

Open `interview-prep-01-debugging` in Android Studio as the project root. It builds with the Gradle wrapper in this folder (Gradle 9.6.0, AGP 9.4.1, Kotlin 2.4.20, Compose BOM 2026.09.00, Room 2.8.5, KSP 2.3.12), the same toolchain as your current Studio template.

## What's wrong

Three defects were reported by QA. They are real, reproducible, and each has a single root cause.

1. **Crash.** Open any note, tap **Delete**. The app dies.
2. **Memory leak.** Rotate the device a few times, then run the **LeakCanary** task in the Android Studio Profiler (Quail 2 or later). `MainActivity` instances are retained. There may be more than one retention path; find every one.
3. **Race.** Tap **Sync** twice quickly (or tap it once while the auto-sync on launch is still running). Duplicate notes appear, and the "Syncs" counter in the top bar is lower than the number of syncs that actually completed.

QA also wrote, without a repro: "The list screen keeps doing work after you leave it." Treat that as a fourth lead.

## Deliverables

1. Fixes for all defects, each in its own commit with a message that names the cause, not just the symptom.
2. A regression test per defect in `app/src/test`. Each test must fail on `main` and pass on your branch. `FakeNotesRepository` and `MainDispatcherRule` are there to build on; Turbine is on the classpath. The race test should be deterministic (no `delay`-and-hope): `FakeNotesRepository.syncGate` exists so a test can hold one sync open while another starts.
3. `DEBUG_LOG.md` filled in: repro, root cause with file and mechanism, fix, how you found it.
4. Nothing else changes behavior. Refactors that make a fix cleaner are welcome; rewrites are not.

## Rules

- No AI-generated or AI-suggested code. Documentation is open; the references below are enough.
- Commit as you go. The commit history is part of what gets reviewed.
- If you run out of time, submit what you have with the log filled in honestly. A clean partial beats a rushed complete.

## Rubric (1 to 5 each, 20 to pass)

| Dimension | What 5 looks like |
|---|---|
| Correctness | Every defect fixed at the root, nothing papered over, no new defects |
| Architecture and boundaries | Fixes land in the right layer; lifecycle owned by the right component |
| Code quality and idiom | Idiomatic coroutines, Flow, Room, Compose; nothing clever for its own sake |
| Tests | Deterministic, fail-before pass-after, test the mechanism not the symptom |
| Communication | Log and commit messages let a reviewer verify the diagnosis without re-deriving it |

After grading you also get `INTERVIEW.md`: the questions a Staff interviewer would ask you about this exact code.

## References

- Room: [Write asynchronous DAO queries](https://developer.android.com/training/data-storage/room/async-queries), [`@Transaction`](https://developer.android.com/reference/androidx/room/Transaction), [`@Index`](https://developer.android.com/reference/androidx/room/Index), [`OnConflictStrategy`](https://developer.android.com/reference/androidx/room/OnConflictStrategy)
- Coroutines: [Shared mutable state and concurrency](https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html), [`MutableStateFlow.update`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/update.html), [`GlobalScope`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-global-scope/), [Testing Kotlin coroutines on Android](https://developer.android.com/kotlin/coroutines/test)
- Lifecycle: [Use Kotlin coroutines with lifecycle-aware components](https://developer.android.com/topic/libraries/architecture/coroutines), [Consume flows safely in Compose](https://developer.android.com/develop/ui/compose/state#use-other-types-of-state-in-jetpack-compose)
- Leaks: [LeakCanary](https://square.github.io/leakcanary/), [Android Studio Quail 2 release notes (LeakCanary profiler task)](https://developer.android.com/studio/releases/past-releases/as-quail-2-release-notes)
- Tests: [Turbine](https://github.com/cashapp/turbine)

## Submitting

```
git checkout -b 01-debugging
# work, commit as you go
git push -u origin 01-debugging
```

Open a pull request into `main` titled `01 Debugging: <your one-line summary>`. I review the PR line by line and push `SCORE.md` and `INTERVIEW.md` to your branch within 24 hours.
