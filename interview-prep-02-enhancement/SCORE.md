# Assignment 02: Enhancement, score

## Round 2 (commit cc2b23b): 21 / 25. Pass.

| Dimension | R1 | R2 | Why |
|---|---|---|---|
| Correctness | 2 | 4 | All eight criteria now met: `AutoMigration(from = 1, to = 2)` with `defaultValue`, no fallback, `3.json` gone; `IOException` returns `Pending` instead of escaping; `StatusPatchDto` matches the contract; conflict and rejection messages reach the screen; backend restored; toolchain file deleted; the merge keeps pending rows the server dropped. Not a 5 because of one realistic path: offline, tap Start then Complete. The second write overwrites the first in Room (`DONE`, still version 1). On refresh the retry gets 400 for OPEN to DONE, the rollback clears pending, the merge takes the server's `OPEN`, and both taps vanish with no message, because `refresh()` discards the `Rejected` outcome. Disabling the buttons while `isPending` is true, or refusing a second transition on a pending row in the repository, closes it. |
| Architecture and boundaries | 3 | 4 | `UpdateWorkOrderResult` is the right seam: the ViewModel maps outcomes to a message and never sees an HTTP code. `Status.next()` in the domain is the single transition table. `CancellationException` rethrown. Left: the ViewModel still sets `isPending = true` on the copy it hands down (the repository overwrites it, so it is dead code with a message), and `refresh()` swallows every retry outcome, so a rejected retry is invisible to the user. |
| Code quality and idiom | 2 | 4 | Dead imports, `ResponseCodes`, `Color.Red` and the no-op rethrow are gone; Material colours and typography used; `runCatching` around the error-body parse with a sane default. Left: the round-1 reformatting of `SEED` and the encode calls in `FakeDispatchBackend` is still in the diff (77 lines of noise in a file that should show no behaviour change), and the `.gitignore` rewrite. |
| Tests | 3 | 5 | Every PATCH outcome asserted on both the typed result and the Room row; `IOException` produced with `SocketPolicy.DISCONNECT_AT_START` rather than a shutdown; pending-survives-refresh now fails the retry with a real `IOException`; the request body is asserted; ViewModel tests for the conflict and rejection messages; `MigrationTest` inserts a version 1 row and reads it back at version 2 with `isPending = 0`. What would still be worth adding: the merge branch that keeps a pending row the server no longer returns, and the Start-then-Complete-offline sequence above, which would have caught the gap. |
| Communication | 2 | 4 | `DESIGN.md` now describes the code: the migration as built, the `Mutex` answer corrected with the exact interleaving, and question 3 has the two-line client fix (409 whose body already carries the target status is `Applied`). Round 2 commit message names every change. Still: a "Fix PR score changes." commit six seconds before it with nothing to say, and the PR body is "Fixing enhancement issues". |

## What a 23 to 24 would have added

- Buttons disabled while `isPending`, or the repository refusing a second transition on a pending row, with the test for the offline double tap.
- The two-line 409 idempotency fix you described in `DESIGN.md`, since it costs nothing and you already know it.
- Revert the formatting-only churn in `FakeDispatchBackend` so the diff shows what changed.
- A PR description that says what to look at first.

## Interview notes

Be ready for INTERVIEW.md questions 6, 12 and 14 in particular: what a 500 does to a row under your `else -> Pending`, what the refresh-versus-tap interleaving does to Room now that you have named it, and why a 409 whose body matches your target status is not a conflict. Those three are the ones that separate "handled the codes" from "owns the data model".

---

## Round 1 (commit 93b7ea0): 12 / 25. Below the pass line (20).

The optimistic write, the 200 and 409 data paths, and the retry-then-merge refresh are the right shape and are tested against MockWebServer. What sinks this round is criterion 7 (there is no migration; the app drops the table), criterion 8 (the backend you were told not to touch was changed so the Offline switch no longer goes offline), and a crash path on the one failure the feature exists for. `DESIGN.md` describes a migration that is not in the diff.

| Dimension | Score | Why |
|---|---|---|
| Correctness | 2 | 1 and 3 met. 2 sends the whole DTO, not the contract body. 4: the `IOException` is rethrown into a `viewModelScope.launch` with no handler, so a tap while truly offline crashes the process; it only looks fine on device because the backend was changed to return 404 instead of throwing. 5 and 6: the row is handled, the required message is never shown. 7: `fallbackToDestructiveMigration(dropAllTables = true)` and no `Migration`; every technician loses their cache on upgrade. 8: backend changed, list screen now says "Server returned 404" when offline, a JDK 25 daemon toolchain file was committed. |
| Architecture and boundaries | 3 | `isPending` on the entity is a defensible choice and the retry-first refresh with a merge is the right structure. But the ViewModel sets `isPending` (a data-layer fact) and encodes the transition table, the screen encodes it again, and the domain has nothing. The repository swallows every HTTP code except 409 and 400, so a 404 or 500 leaves a row pending forever with no signal. `catch (e: Exception)` around a suspend call also eats `CancellationException`. |
| Code quality and idiom | 2 | Unused imports (`AutoMigration`, `AutoMigrationSpec`, `SupportSQLiteDatabase`, `android.R`). `ResponseCodes` lives in the API file with a `message` nobody reads. `catch (e: IOException) { throw e }` is a no-op. Fifty lines of reformatting in `SEED` and the encode calls that change nothing. `Color.Red` in two screens. Schema 2 and 3 are byte-identical apart from the version number. |
| Tests | 3 | Six repository tests against MockWebServer: 200, `IOException`, 409, 400, retry-before-GET ordering, pending survives. Deterministic and readable, and the pending-survives test is the one that matters. Not higher because there is no migration test (there is no migration), the ViewModel "optimistic" tests only prove the ViewModel called the fake, and the pending-survives test uses a 500 that the code silently swallows, so it never exercises the `IOException` path the criterion is about. |
| Communication | 2 | `DESIGN.md` is filled in and under a page, but it says "Database migrations use Room AutoMigrations" when the code has none and drops tables, and says a `Mutex` is unnecessary because writes do not overlap when the launch refresh and a tap overlap by design. Q3 is the strongest answer. History: one commit for the whole feature whose message says you did not iterate, then "Updating unit tests". PR body: "Fixing enhancement issues". |

## Criterion by criterion

### 1. Immediate, Room as the only source. Met.

`dao.upsert(pendingEntity)` lands before the network call and the detail screen reads the Flow. Good.

### 2. PATCH with status and version. Partly.

`@Body body: WorkOrderDto` sends `title`, `site`, `priority` and `updatedAt` along with `status` and `version`. The contract body is two fields. The fake server ignores the extras; a real one may reject unknown fields or, worse, apply the stale `updatedAt`. A `StatusPatchDto(status, version)` is three lines.

### 3. 200 replaces the row. Met.

`dao.upsert(response.toEntity().copy(isPending = false))`. Exactly right.

### 4. Offline keeps the change pending; refresh retries first and never discards. Partly, with a crash.

The repository does the right thing with Room and then rethrows `IOException`. `WorkOrderDetailViewModel.updateStatus` launches it with no `try`. An uncaught exception in `viewModelScope` kills the process. You did not see this on device because `FakeDispatchBackend` no longer throws when offline; you changed line 46 to return 404, and 404 is one of the codes the repository's `when` silently ignores. The README said nothing in that file needs to change; the sentence was there because the `IOException` is the whole point of criterion 4. Fix: catch `IOException` in the repository and return a typed outcome (`Applied`, `Pending`, `Conflict(server)`, `Rejected(reason)`) that the ViewModel maps to state; restore the throw in the backend.

The retry-first refresh and the merge are right. One edge: `remainingPending[remoteEntity.id]` only keeps pending rows whose id is in the server list; a pending change on a row the server dropped is discarded. State that as a decision in `DESIGN.md` or keep those rows.

### 5. 409: server wins, message shown. Data yes, message no.

The error body is decoded and upserted with pending cleared. Nothing tells the technician. The detail screen needs a one-shot message, which means the repository has to say "conflict" to someone; a `Boolean` column cannot carry that. This is the same typed-outcome fix as criterion 4.

### 6. 400: roll back, show the server's error. Rollback yes, message no.

`originalEntity` captured before the upsert, restored on 400. Correct. `error` field is never read. If `originalEntity` was itself pending, the rollback clears that earlier pending change; probably acceptable, say so.

### 7. Migration, no destructive fallback. Failed.

`AppContainer` line 43: `.fallbackToDestructiveMigration(dropAllTables = true)`. `DispatchDatabase`: version 3, no `autoMigrations`, no `addMigrations`. The unused `AutoMigration` import says you started one and stopped. What a pass needed:

```kotlin
@Database(entities = [WorkOrderEntity::class], version = 2, exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)])
```

A new `NOT NULL` column with a default is exactly the case `AutoMigration` handles on its own, as long as the entity says `@ColumnInfo(defaultValue = "0") val isPending: Boolean = false`. Or a six-line manual `Migration(1, 2)` with `ALTER TABLE work_orders ADD COLUMN isPending INTEGER NOT NULL DEFAULT 0`. Either way, delete the fallback, go back to version 2, and delete `3.json`.

### 8. Nothing else changes. Not met.

Backend changed (above). The list ViewModel's offline message is now "Server returned 404" instead of "Offline: showing cached work orders". `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon to JDK 25 via foojay download for everyone who opens the project, while the README says JDK 17; delete it. The `.gitignore` rewrite is harmless but unasked for.

## What a pass looked like

- A `StatusPatchDto`, a typed `UpdateResult` sealed class returned by the repository, the ViewModel mapping it to a `message` field in `Loaded`, the screen showing it. About 40 lines.
- Transition table in one place in `domain` (`fun Status.next(): Status?`), used by the ViewModel and the screen.
- `AutoMigration(from = 1, to = 2)` with `defaultValue`, no fallback, `2.json` committed, a `MigrationTest` in `androidTest` that inserts a version 1 row and reads it back at version 2.
- `FakeDispatchBackend` untouched.
- Four or five commits: entity and migration; repository write path and tests; refresh merge and test; ViewModel and screen; design notes.

## Next step

Push fixes to this branch, do not open a new PR. In order: remove the destructive fallback and add the migration (this alone is disqualifying), restore the `IOException` in the backend and catch it in the repository, surface the 409 and 400 messages, delete the daemon toolchain file, correct `DESIGN.md` so it describes the code. Then ask for a re-score. Items 1 and 2 get this to about 18; all five is a pass.
