# Assignment 03: Code smells

**Area:** Code smells · **Timebox:** 3 hours · **Due:** Sunday, October 18 · **Branch:** `03-code-smells` · **Submit:** pull request into `main`

## The app

Timesheet is a small Jetpack Compose app for field technicians: log a shift against a work order, see the week's hours, overtime and pay, submit shifts to the office. Kotlin, Compose, ViewModel + StateFlow, Room, Retrofit + OkHttp + kotlinx.serialization. Same toolchain as assignments 01 and 02 (Gradle 9.6.0, AGP 9.4.1, Kotlin 2.4.20, Compose BOM 2026.09.00, Room 2.8.5, KSP 2.3.12, OkHttp 5.5.0, Retrofit 3.0.0, kotlinx.serialization 1.11.0). Open `interview-prep-03-code-smells` in Android Studio as the project root.

There is no real server. `FakeTimesheetBackend` is an OkHttp interceptor that answers `POST /shifts` in process: 201 with a server id, 422 for a shift whose end is not after its start or whose break is longer than the shift, 503 when the note contains the word "outage". Nothing in that file needs to change.

**The app works.** Every screen does what it says, the numbers are right, and nobody has filed a bug. It was written in a hurry by someone who has since left, and you have inherited it. Your job this time is not to fix defects. It is to make the code safe to change, without changing what it does.

## What you are looking at

Read everything in `app/src/main` before you touch anything: `Injector`, `Utils`, `ShiftRepository`, `TimesheetViewModel`, `TimesheetScreen`, the entity and the DAO. About 550 lines. There are more than a dozen distinct smells in there. Some are one-liners; a few are structural and make the ViewModel impossible to unit test on the JVM as it stands.

## Behaviour contract

This is what the app does today and must still do when you are finished. Your characterization tests should pin all of it.

- Worked time is end minus start minus break, never negative, rounded to the nearest 15 minutes (a remainder of 8 or more rounds up).
- Pay for a shift: the first 8 hours at the rate, anything past that at 1.5x; an unbillable shift pays 0; a per-shift rate override replaces the technician's rate; the result is rounded to cents.
- The technician's hourly rate is read from SharedPreferences, key `hourly_rate`, default 42.
- The week starts on Monday. The header shows worked minutes, overtime minutes (minutes past 8 hours per shift, summed) and pay for the week containing today.
- Shifts are listed newest first (date, then start time).
- A shift is created as `draft`. Submitting sends it to the server; on success its `status` and `serverId` come from the server's response (`submitted`, or `approved` for an unbillable shift). A shift that is `submitted` or `approved` cannot be submitted again. "Submit all" submits every draft and reports how many succeeded.
- A failed submit shows "Submit failed" and leaves the shift as a draft.
- Validation on create: work order required, date must parse as `yyyy-MM-dd`, end must be after start.

## Deliverables

1. **Characterization tests first.** Before any refactor, write tests in `app/src/test` that pin the behaviour contract where the current code allows it (`Utils` is reachable; `UtilsTest` has two examples). Commit them on their own, passing on `main` unchanged. Where the current code does not allow a unit test (the ViewModel, the repository), say so in `SMELLS.md`; the refactor that creates the seam comes first, and its characterization tests come immediately after it, before anything else moves.
2. **`SMELLS.md`** filled in: every smell you found, with its standard name, where it is, why it matters in this codebase specifically, the fix, and the risk of the fix. Ranked in the order you would fix them. Expect at least ten.
3. **Refactor the top five or six**, one smell per commit, tests green after every commit, behaviour unchanged. The characterization tests are the proof; if one has to change, the commit message says why. `InMemoryShiftDao` and `FakeTimesheetApi` are in `app/src/test` for the seams you will need.
4. **No new features, no behaviour changes.** Not even ones you are sure are improvements. If you find something you believe is a latent bug, note it in `SMELLS.md` under "What you deliberately left" and leave it.

Scope guidance for three hours: the catalog and the characterization tests are an hour; the refactors are the rest. Five well-chosen refactors beat ten shallow ones. The reviewer will look at which five you chose and in what order.

## Rules

- No AI-generated or AI-suggested code. Documentation is open; the references below are enough.
- Commit as you go. The commit history is part of what gets reviewed: it should read as a sequence of safe steps.
- If you run out of time, submit what you have with `SMELLS.md` filled in honestly. A clean partial beats a rushed complete.

## Rubric (1 to 5 each, 20 to pass)

| Dimension | What 5 looks like |
|---|---|
| Correctness | Behaviour identical before and after, proven by tests that passed on `main` first; no latent bug "fixed" on the way |
| Architecture and boundaries | The refactors you chose are the ones that unlock the rest: dependencies flow inward, the ViewModel is testable, business rules live in one place |
| Code quality and idiom | Each refactor is the textbook move for that smell, no more; names say what things are; nothing clever |
| Tests | Characterization tests that pin the contract and would fail on a behaviour change; the first refactor makes the ViewModel testable and its tests land right after it |
| Communication | `SMELLS.md` reads like a senior engineer's audit: correct names, honest risk, defensible order; commits are small and each message names the smell |

After grading you also get `INTERVIEW.md`: the questions a Staff interviewer would ask you about this exact code.

## References

- Smells and refactorings: [Code Smell (Fowler)](https://martinfowler.com/bliki/CodeSmell.html), [Refactoring catalog](https://refactoring.com/catalog)
- Architecture: [Guide to app architecture](https://developer.android.com/topic/architecture), [UI events](https://developer.android.com/topic/architecture/ui-layer/events), [Compose UI architecture](https://developer.android.com/develop/ui/compose/architecture)
- Compose: [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state), [Where to hoist state](https://developer.android.com/develop/ui/compose/state-hoisting), [Performance](https://developer.android.com/develop/ui/compose/performance), [Stability](https://developer.android.com/develop/ui/compose/performance/stability)
- Coroutines: [Best practices for coroutines in Android](https://developer.android.com/kotlin/coroutines/coroutines-best-practices), [Testing Kotlin coroutines on Android](https://developer.android.com/kotlin/coroutines/test)
- Tests: [Turbine](https://github.com/cashapp/turbine)

## Submitting

```
git checkout -b 03-code-smells
# work, commit as you go
git push -u origin 03-code-smells
```

Open a pull request into `main` titled `03 Code smells: <your one-line summary>`. I review the PR line by line and push `SCORE.md` and `INTERVIEW.md` to your branch within 24 hours.
