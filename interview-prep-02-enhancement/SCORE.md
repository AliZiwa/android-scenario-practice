# Assignment 02: Enhancement, score

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
