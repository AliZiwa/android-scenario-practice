# Assignment 02: the questions this diff invites

Answer each out loud in under two minutes. These are the follow-ups a Staff interviewer would ask after reading your PR.

1. Walk me through what happens, line by line, when a technician taps Start with the phone in airplane mode on a real network. Where does the `IOException` end up, and what does the user see?
2. You changed `FakeDispatchBackend` so that offline returns 404 instead of throwing. What did that hide, and how did it change the list screen's behaviour?
3. `fallbackToDestructiveMigration(dropAllTables = true)` is in the container. A technician with two hundred cached work orders updates the app on a job site with no signal. What is on their screen after the update, and when does it come back?
4. Adding a `NOT NULL` boolean column: why does `AutoMigration` need `@ColumnInfo(defaultValue = "0")` when the Kotlin property already has `= false`? What does Room actually run?
5. Your database is at version 3 and `2.json` equals `3.json`. What does a device currently at version 2 do on launch under your configuration, and under a correct one?
6. The repository's `catch (HttpException)` handles 409 and 400 and nothing else. A PATCH gets a 500. Describe the row's lifetime from that moment on, and what the technician sees.
7. `catch (e: Exception)` around `updateWorkOrderStatus` inside `refresh()`: what happens if the refresh coroutine is cancelled while that PATCH is in flight? What should the catch have been?
8. The ViewModel sets `isPending = true` and so does the repository. Which one should, and why does it matter which layer owns that fact?
9. On 409 you store the server's row and clear pending. The brief required a message. Where would the "conflict" signal come from, given that your only state is a `Boolean` column? Sketch the return type you would give `updateWorkOrderStatus`.
10. Your `refresh()` merge keeps a pending row only if the server still returns that id. Argue both sides of that choice.
11. The transition table lives in `updateStatus()` and again in the two `enabled =` expressions. Where would you put it so both read from the same place, and what test would you write for it?
12. Launch refresh and a tap on Start can overlap. The refresh reads `getAllPending()` before the tap's upsert and runs `replaceAll` after it. What is in Room when both finish? Your `DESIGN.md` says a `Mutex` is unnecessary; is that still your answer?
13. `@Body body: WorkOrderDto` sends `updatedAt` from the device. On a server that applies every field it receives, what did you just do to the server's timestamp?
14. `DESIGN.md` question 3: the PATCH was applied, the response was lost, the retry gets 409 with a body whose `status` equals what you tried to set. Is that a conflict? What is the two-line client fix that needs nothing from the backend?
15. `gradle-daemon-jvm.properties` with `toolchainVersion=25`: what does that file do to a teammate who clones the repo with JDK 17 installed, and how did it get there?
