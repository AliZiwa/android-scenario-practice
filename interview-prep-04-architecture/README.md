# Assignment 04: Architecture

**Area:** Architecture · **Timebox:** 3 hours · **Due:** Thursday, October 29 · **Branch:** `04-architecture` · **Submit:** pull request into `main`

## The app

PartsBin is the parts inventory for a field technician's van: a searchable list of parts with quantities and bin locations, a detail screen for one part, and two buttons, Use and Restock, that adjust the quantity with a reason. Kotlin, Compose, ViewModel + StateFlow, Room, Retrofit + OkHttp + kotlinx.serialization. Same toolchain as assignments 01 to 03 (Gradle 9.6.0, AGP 9.4.1, Kotlin 2.4.20, Compose BOM 2026.09.00, Room 2.8.5, KSP 2.3.12, OkHttp 5.5.0, Retrofit 3.0.0). Open `interview-prep-04-architecture` in Android Studio as the project root.

There is no real server. `FakePartsBackend` is an OkHttp interceptor that answers in process from a seed of twelve parts: `GET /parts`, `GET /parts/{id}`, and `POST /parts/{id}/adjust` with `{"delta": n, "reason": "..."}` (200 with the updated part, 404 unknown id, 422 `reason_required` or `insufficient_stock`). The Offline switch in the top bar makes every request throw an `IOException`. Nothing in that file needs to change.

**The app works on a good connection.** It was built as a prototype in a week and the prototype shipped. You have been asked to make it the base for the next year: a sync queue, barcode scanning, a second technician role, and a tablet layout are all on the roadmap. None of that is this assignment. This assignment is the architecture those features will land on, and the proof that it holds.

## What you are looking at

About 600 lines in `app/src/main`: `Globals`, `MainActivity`, `net/`, `db/`, `screens/`. Read all of it before you write anything. The structural problems are not hidden; the list below is complete enough that finding them is not the exercise. Deciding what to do about them, in what order, and proving it, is.

1. Two ViewModels each build their own `Retrofit` and `OkHttpClient`. There are two backends in one process.
2. `Globals` holds the database, the selected part id, the offline flag and a "list is dirty" flag. Every layer reads and writes it.
3. `MainActivity` creates the Room database with `allowMainThreadQueries()` and navigates with a string.
4. The list writes to Room after every successful fetch and nothing ever reads Room. Offline, the list is empty and the error is the exception message.
5. The detail screen fetches the part again from the network and shows network DTOs directly. After an adjustment the list and the detail disagree until someone presses Refresh.
6. "Low stock" is computed in two places with two different rules (`<` in the list, `<=` in the detail).
7. DTO to row mapping is written out in each ViewModel. `updatedAt` is shown raw.
8. There is no repository, no domain model, and no place for a business rule to live. The adjustment validation (reason required, no negative stock) exists only on the server.
9. Errors are strings built from exceptions. `HttpException` code 422 is shown as "Server said 422" and the body is never read.
10. Nothing is unit-testable: every ViewModel reaches `Globals` and builds a network stack in its constructor.

## Behaviour contract

What the app must do when you are finished. Every line is testable without a device, and the review checks each one.

- The list shows what is in Room immediately, then refreshes from the network. A refresh that fails with an `IOException` leaves the list as it was and shows "Offline, showing cached parts". A refresh that fails with a server error shows "Could not refresh" and leaves the list as it was.
- Search matches sku or name, case-insensitive, trimmed. Empty query shows everything. Order is by sku.
- A part is low stock when `quantity <= minQuantity`. The same rule, from the same code, on both screens.
- The detail screen reads the same part the list shows. An adjustment that succeeds is visible on both screens with no refresh.
- Adjust: a blank reason is rejected before any network call with "Reason required". A result below zero is rejected before any network call with "Not enough stock". The server's 422 (either code) rolls back any local change and shows the server's reason. An `IOException` shows "Offline, adjustment not saved" and the quantity is unchanged.
- `updatedAt` is shown as a local date and time, not an ISO string.
- Navigation: single activity, the selected part id travels as an argument to the detail destination, never through a global. Back from detail returns to the list with its search text intact.
- The Offline switch keeps working and still flips the fake backend. Where that flag lives is your call; it is not in `Globals` when you are done, because `Globals` is gone.

## Deliverables

1. **`ARCHITECTURE.md`** filled in, before the code. It is an architecture decision record: context (the ten problems in your words, which ones block the roadmap), the decision (layers, the dependency rule, where each kind of logic lives, how state reaches the screen, how errors are modelled, how dependencies are wired, how navigation carries arguments), the alternatives you considered and why not, and the consequences, including the ones you do not like. One page, a diagram if it helps. The reviewer reads this first and then checks the code against it. Where the code and the document disagree, the document loses.
2. **The restructure.** Package the app the way the ADR says. The dependency rule has to be real: `ArchitectureRulesTest` in `app/src/test` encodes one reasonable version of it and is `@Ignore`d on `main`; enable it (rename packages in it if your ADR chose different names) and keep it green. Room is the source of truth; the network is a sync. One composition root. Business rules in one place. Typed outcomes instead of strings between layers. `Globals.kt` deleted.
3. **Tests at each boundary**, with the right double for each: the repository against MockWebServer and `InMemoryPartDao` (cached list survives a failed refresh, successful refresh upserts, adjust success writes through, server 422 rolls back, `IOException` leaves Room unchanged); the business rules on their own; the two ViewModels with a fake repository and Turbine or `StateFlow.value`. `MainDispatcherRule` and `InMemoryPartDao` are provided.
4. **Scope discipline.** No new features. No sync queue, no WorkManager, no Hilt (a hand-rolled container is right for this size; say so in the ADR). Navigation Compose is allowed if you want it; the hand-rolled switch is also fine once the id is an argument. The one behaviour change the contract asks for is offline reads from Room; everything else the app does today, it still does.

Scope guidance for three hours: forty-five minutes on the ADR, ninety on the restructure, forty-five on tests. If you are running out of time, a complete data layer with tests and a half-migrated UI beats a complete UI on top of a half-built data layer; say which half you left in the ADR.

## Rules

- No AI-generated or AI-suggested code, and no AI-written ADR. Documentation is open; the references below are enough. Say in the PR body how the work was produced.
- Commit as you go. The history should read as the ADR's decisions landing one at a time: ADR; domain model and rules with tests; repository with tests; composition root; list screen; detail screen; architecture test enabled.
- If you run out of time, submit what you have with the ADR honest about what is missing. A clean partial beats a rushed complete.

## Rubric (1 to 5 each, 20 to pass)

| Dimension | What 5 looks like |
|---|---|
| Correctness | Every line of the behaviour contract holds and has a test that fails without it; the offline paths are driven by MockWebServer disconnects, not by a flag |
| Architecture and boundaries | The dependency rule holds and `ArchitectureRulesTest` proves it; Room is the single source of truth and both screens observe it; each business rule exists once; the ADR describes the code that exists, and would still be the right document when the sync queue lands |
| Code quality and idiom | Packages say what they contain; mapping functions live at the boundary they cross; `StateFlow` built with `stateIn` or `update`, not manual copies; no `Globals`, no `allowMainThreadQueries`, no string-typed navigation or errors |
| Tests | Right double at each boundary; the repository tests would catch a regression in any contract line about caching or adjustment; the ViewModel tests prove state, not that a fake was called |
| Communication | The ADR reads like one a team would adopt: honest context, a decision with reasons, alternatives named and declined for a reason, consequences including costs; commits land one decision at a time; PR body says what to read first |

After grading you also get `INTERVIEW.md`: the questions a Staff interviewer would ask you about this exact code.

## References

- Architecture: [Guide to app architecture](https://developer.android.com/topic/architecture), [Recommendations for Android architecture](https://developer.android.com/topic/architecture/recommendations), [Common modularization patterns](https://developer.android.com/topic/modularization/patterns)
- Data: [Data layer](https://developer.android.com/topic/architecture/data-layer), [Build an offline-first app](https://developer.android.com/topic/architecture/data-layer/offline-first), [Domain layer](https://developer.android.com/topic/architecture/domain-layer)
- UI: [UI state production](https://developer.android.com/topic/architecture/ui-layer/state-production), [State holders and UI state](https://developer.android.com/topic/architecture/ui-layer/stateholders)
- Wiring: [Manual dependency injection](https://developer.android.com/training/dependency-injection/manual)
- ADRs: [ADR templates](https://adr.github.io/adr-templates/), [Architecture decision record examples and templates](https://github.com/joelparkerhenderson/architecture-decision-record)
- Tests: [MockWebServer](https://github.com/square/okhttp/blob/master/mockwebserver/README.md), [Testing Kotlin coroutines on Android](https://developer.android.com/kotlin/coroutines/test), [Turbine](https://github.com/cashapp/turbine)

## Submitting

```
git checkout -b 04-architecture
# ADR first, then work, commit as you go
git push -u origin 04-architecture
```

Open a pull request into `main` titled `04 Architecture: <your one-line summary>`. I review the PR line by line and push `SCORE.md` and `INTERVIEW.md` to your branch within 24 hours.
