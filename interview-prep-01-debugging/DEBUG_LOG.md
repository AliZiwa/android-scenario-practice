# Debug log

Fill this in as you go. One section per defect. Keep it short; this is the document a reviewer reads first.

## Defect 1: Crash on Delete

- **Repro:** On Notes Details Screen, attempt to delete the note
- **Root cause:** In the DefaultNotesRepository, whenever delete was called, state changes and notes is called to observeNote with id again. 
- With the !! - its an instant crash on a null pointer since that note doesnt exit
- **Fix:** Remove the !! and use an optional Note?
- **Regression test:** Successfully delete note
- **How I found it:** Android Debugger and Breakpoints.

## Defect 2:
- **Repro:** Rotate phone while app is running multiple times. 
- **Root cause:** MainActivity - retaining a reference to the MainActivity in the SyncManager.Listener method
- **Fix:** Unregister the activity from the SyncManager onDestroy
- **Regression test:** Rotate phone while app is running multiple times. Observe Leaks in LeakCanary Profiler
- **How I found it:** LeakCanary Profiler
- 
## Defect 3:
- **Repro:** Sync Race condition - tap sync twice quickly (or tap it once while the auto-sync on launch is still running). 
- **Root cause:** No conflict resolution or transaction handling on the entity level. 
- **Fix:** Adding conflict resolution and transaction handling. Marked "RemoteId" to be unique on the Notes entity.
- **Regression test:** Sync Race condition - No conflicts. 
- **How I found it:** Code analysis.
## Anything else you noticed

(Things you would flag in code review but did not fix, with a reason.)
