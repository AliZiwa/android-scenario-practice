# Assignment 02: Enhancement

**Area:** Enhancement · **Timebox:** 3 hours · **Due:** Saturday, October 17 · **Branch:** `02-enhancement` · **Submit:** pull request into `main`

## The app

Dispatch is a small Jetpack Compose app for field technicians: a list of work orders pulled from the dispatch server into Room, and a detail screen. Kotlin, Compose, ViewModel + StateFlow, Room with Flow, coroutines, Retrofit + OkHttp + kotlinx.serialization, hand-rolled DI in `AppContainer`. Two screens switched by state in `DispatchNavHost`, same as assignment 01.

Open `interview-prep-02-enhancement` in Android Studio as the project root. It builds with the Gradle wrapper in this folder (Gradle 9.6.0, AGP 9.4.1, Kotlin 2.4.20, Compose BOM 2026.09.00, Room 2.8.5, KSP 2.3.12, OkHttp 5.5.0, Retrofit 3.0.0, kotlinx.serialization 1.11.0), the same toolchain as assignment 01.

There is no real server. `FakeDispatchBackend` is an OkHttp application interceptor that answers every request in process, so Retrofit, the converters, HTTP status codes and `IOException` are all real; only the socket is missing. The **Offline** switch in the top bar makes it throw `IOException` on every call. The backend also simulates a colleague: after the first list fetch, a dispatcher at the office edits `wo-104`, so the first write this device sends for `wo-104` will be stale. Read `FakeDispatchBackend.kt` before you start; nothing in it needs to change.

The app works today. Version 1 shipped in September and technicians have cached work orders on their phones in the version 1 schema (`app/schemas/.../1.json`; build once before you touch the schema so Room regenerates that file from the real code).

## The feature

Product wants technicians to move a work order along from the phone: **OPEN → IN PROGRESS → DONE**, from the detail screen, with two buttons, **Start** and **Complete**. The acceptance criteria, as written by the PM and the backend lead:

1. **Immediate.** Tapping Start or Complete updates the screen at once, connected or not. No spinner. Room stays the only source the UI reads.
2. **Sent to the server** as `PATCH /work-orders/{id}` with the new status and the `version` the device last saw for that work order (see the contract below).
3. **200:** the server's representation replaces the local row; `version` moves on.
4. **No connection (`IOException`):** the change is kept locally as *pending*. The list row and the detail screen say "Pending sync". The next refresh, manual or on launch, retries pending changes first and only then pulls the list. **A refresh must never discard a pending change.**
5. **409 Conflict:** the server wins. The body is the server's current representation; store it, drop the pending change, and show one line on the detail screen: "Updated by dispatcher, review and try again". The buttons must reflect the server's status, not the one the technician tapped.
6. **400:** roll the change back and show the server's `error` field. (The UI should make an invalid transition impossible; the server stays the authority.)
7. **Nobody loses their cache.** The database goes to version 2 with a `Migration`. `fallbackToDestructiveMigration` and friends are not acceptable in this product. Commit the exported `2.json`.
8. **Nothing else changes behavior.** The existing tests keep passing. The list order, the Offline switch and the refresh semantics for rows without pending changes stay as they are.

Transitions are exactly OPEN → IN_PROGRESS and IN_PROGRESS → DONE. Enforce them in one place.

## API contract

`GET /work-orders` → 200, the technician's full list:

```json
[
  {"id":"wo-101","title":"Replace hydraulic seal, lift 3","site":"Bay 3","priority":"HIGH","status":"OPEN","version":1,"updatedAt":1760400000000}
]
```

`PATCH /work-orders/{id}` with body `{"status":"IN_PROGRESS","version":1}`:

| Response | Body | Meaning |
|---|---|---|
| 200 | the updated work order, `version` incremented | applied |
| 409 | the server's current work order | your `version` is stale; someone else changed it |
| 400 | `{"error":"invalid_transition","from":"OPEN","to":"DONE"}` or `{"error":"missing_version"}` | rejected, nothing changed |
| 404 | `{"error":"not_found"}` | unknown id |

`GET /work-orders/{id}` → 200 with one work order, in case you want it.

## What is given

- `data/local`: `WorkOrderEntity`, `WorkOrderDao` (abstract class, with a transactional `replaceAll`), `DispatchDatabase` at version 1, `exportSchema = true`, schema location `app/schemas`.
- `data/remote`: `WorkOrderDto`, `WorkOrdersApi` (only `GET /work-orders` so far), `FakeDispatchBackend`.
- `data/WorkOrdersRepository.kt`: `observeWorkOrders` (sorted), `observeWorkOrder`, `refresh`.
- `ui`: list and detail screens, both ViewModels, the Offline switch wired to the backend.
- `app/src/test`: `MainDispatcherRule`, `InMemoryWorkOrderDao` (so repository tests need no Room), `FakeWorkOrdersRepository` (for ViewModel tests), `DefaultWorkOrdersRepositoryTest` (a working MockWebServer setup to extend), `WorkOrdersListViewModelTest`.
- `DESIGN.md`: the four questions you must answer, empty.

## Deliverables

1. **The feature**, meeting all eight criteria, in commits that each leave the project building and the tests green. Commit messages name the change, not the ticket.
2. **Tests** in `app/src/test`, deterministic, each one failing without its corresponding change:
   - repository tests against MockWebServer for the PATCH outcomes: 200, `IOException`, 409, 400;
   - a test proving a refresh does not discard a pending change;
   - a ViewModel test for the detail actions (buttons offered per status, optimistic update visible before the server answers).
   An instrumented migration test with `MigrationTestHelper` in `app/src/androidTest` is the difference between a 4 and a 5 on Tests.
3. **`DESIGN.md`** filled in, under a page, answering the four questions in it.
4. Nothing else changes behavior. Refactors that make the feature cleaner are welcome; rewrites are not.

Scope guidance for three hours: criteria 1 to 7 with the repository tests are the must. If you run out of time, submit what you have, say so in `DESIGN.md`, and leave the list in a state you would be willing to ship.

## Rules

- No AI-generated or AI-suggested code. Documentation is open; the references below are enough.
- Commit as you go. The commit history is part of what gets reviewed.
- If you run out of time, submit what you have with `DESIGN.md` filled in honestly. A clean partial beats a rushed complete.

## Rubric (1 to 5 each, 20 to pass)

| Dimension | What 5 looks like |
|---|---|
| Correctness | All eight criteria, including the two that bite (refresh never drops a pending change; 409 means the server wins), with no regression in existing behavior |
| Architecture and boundaries | Pending state is a data-layer concept; the UI never sees an HTTP code; transitions are enforced once; the migration is the schema change and nothing more |
| Code quality and idiom | Typed outcomes rather than stringly errors; idiomatic Retrofit, Room, Flow and Compose; no clever abstraction the feature does not need |
| Tests | MockWebServer tests on every PATCH outcome, deterministic, each failing without its change; the pending-survives-refresh test; a migration test |
| Communication | `DESIGN.md` answers its four questions with real trade-offs; commits tell the story in order; the PR description says what to look at first |

After grading you also get `INTERVIEW.md`: the questions a Staff interviewer would ask you about this exact code.

## References

- Room: [Migrate your Room database](https://developer.android.com/training/data-storage/room/migrating-db-versions), [Room 2.x guide](https://developer.android.com/training/data-storage/room/v2), [`@Upsert`](https://developer.android.com/reference/androidx/room/Upsert), [`Migration`](https://developer.android.com/reference/androidx/room/migration/Migration), [`androidx.room.testing`](https://developer.android.com/reference/androidx/room/testing/package-summary)
- Architecture: [Build an offline-first app](https://developer.android.com/topic/architecture/data-layer/offline-first)
- Network: [Retrofit declarations](https://square.github.io/retrofit/declarations/), [OkHttp interceptors](https://github.com/square/okhttp/blob/master/docs/features/interceptors.md), [MockWebServer](https://github.com/square/okhttp/blob/master/mockwebserver/README.md), [kotlinx.serialization JSON guide](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md)
- Tests: [Testing Kotlin coroutines on Android](https://developer.android.com/kotlin/coroutines/test), [Testing Kotlin flows on Android](https://developer.android.com/kotlin/flow/test), [Turbine](https://github.com/cashapp/turbine)

## Submitting

```
git checkout -b 02-enhancement
# work, commit as you go
git push -u origin 02-enhancement
```

Open a pull request into `main` titled `02 Enhancement: <your one-line summary>`. I review the PR line by line and push `SCORE.md` and `INTERVIEW.md` to your branch within 24 hours.
