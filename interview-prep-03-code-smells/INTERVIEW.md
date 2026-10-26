# Assignment 03: the questions this diff invites

Answer each out loud in under two minutes. These are the follow-ups a Staff interviewer would ask after reading your PR.

1. `Utils.rate(context: Context? = null)` returns 42.0 when no context is passed, and nothing in the app passes one. Walk me through what a technician with `hourly_rate = 55` in SharedPreferences sees in the header and in each row after this change, and how your tests let that through.
2. You said the brief was "no behaviour changes". Define behaviour for this app in one sentence, then tell me whether `submitAllDrafts` going from a sequential loop to `async`/`awaitAll` fits inside it. What changes for a backend that rate-limits per client, and for the order of server ids?
3. `coroutineScope { drafts.map { async { submit(it) } }.awaitAll() }`: `submit` catches every exception internally. If it did not, what would one failing draft do to the others, and what would the count be?
4. You removed `tmp`, bumped Room to version 2 and added `@DeleteColumn`. The committed `1.json` has no `tmp` column and the same `identityHash` as `2.json`. How did that happen, what does it mean for a migration test, and what runs on a device that is actually at version 1?
5. Room's `AutoMigration` for a deleted column rebuilds the table (create `_new_shifts`, copy, drop, rename). What is the user-visible cost of that on a phone with ten thousand rows, and when in the app's lifecycle does it happen?
6. `catch (e: Exception)` in `ShiftRepository.submit` wraps a suspend call. The ViewModel is cleared while a submit is in flight. What does the catch do with the `CancellationException`, and what is returned to a caller that no longer exists?
7. The ViewModel's constructor defaults to `Injector.repository`. Is the service locator gone? What does `viewModel()` in Compose actually call, and why does that compile without a factory?
8. `ShiftRow(shift: ShiftEntity, vm: TimesheetViewModel)`: what does Compose's stability inference make of a `ViewModel` parameter and a `data class` with `var` properties, and what does that do to recomposition of a 200-row list?
9. `submit(shift)` mutates `serverId` and `status` on the entity object that the UI list is currently displaying, then writes it to Room, then the ViewModel re-fetches. Which of those three is the source of truth at each step, and where could a reader observe an intermediate state?
10. `ShiftDao.observeAll()` returns a `Flow` and nothing calls it; the ViewModel re-fetches after every write instead. What would the refactor to a `stateIn` over that flow look like, which of your tests would still pass, and which smell in your catalog does it retire?
11. You labelled `submitAllDrafts` as N+1. Define N+1. Where in this codebase is there an actual N+1, if anywhere?
12. Your catalog says the risk of every fix is "Low" or "Minimal". Pick the one you would least like to defend in a postmortem and tell me what the incident would look like.
13. One commit, 774 files, a 12.9 MB APK. The reviewer has twenty minutes. What do they review, what do they skip, and what did the brief say about history?
14. `assertTrue(state.totalPay > 0.0)` for a ten-hour shift at the default rate. What number should it be, and what class of bug does an inequality assertion never catch?
15. `gradle/gradle-daemon-jvm.properties` with `toolchainVersion=25` appeared in assignment 02 and again here. What creates that file, what does it do on a teammate's machine with JDK 17, and what do you do so it never lands in a commit again?
