# Design notes: status changes from the phone

Keep this under a page. Short answers with a reason beat long answers without one.

## 1. Where does the pending change live?

A boolean column `@ColumnInfo(defaultValue = "0") val isPending: Boolean = false` on the existing `work_orders` Room entity table.

- **Why:** Keeps local reads persistent across process deaths, allows Room to be the single source of truth for the UI, and enables instant Flow emissions and querying (`WHERE isPending = 1`). Schema migration uses `AutoMigration(from = 1, to = 2)` with `exportSchema = true` and no destructive fallback, preserving technician cache.
- **Cost of future edits:** For a single status flag, a boolean column on the entity is lightweight and zero-overhead. If complex multi-field offline edits (such as technician notes) arrive next quarter, adding columns per edit type will cause schema bloat. A separate pending mutations queue table would scale better for heterogeneous edits, but `isPending` was chosen here for minimal complexity and seamless UI binding.

## 2. The process dies between the Room write and the PATCH

- **Technician view:** Because `isPending` is persisted in Room, on relaunch the technician immediately sees their work order in "Pending sync" status.
- **Server state:** The server has not received the `PATCH` yet and holds the previous version and status.
- **Sync mechanism:** On launch or manual refresh, `DefaultWorkOrdersRepository.refresh()` queries `dao.getAllPending()`, retrying pending `PATCH` updates first before fetching the server list. If versions match, the server updates status, increments version, and returns 200 OK, clearing `isPending` in Room.

## 3. The PATCH reached the server but the response never came back

- **On retry:** The server applied the change and bumped `version` to $v+1$, but the client's last known version is still $v$. The retried `PATCH` sends version $v$, triggering a `409 Conflict`.
- **Is the outcome acceptable?:** No, because the technician's edit was processed by the server, but losing the response makes the retry look like a stale conflict.
- **Client & backend resolution:**
  - **Client-only fix (two lines):** On 409, compare `serverDto.status` with `targetStatus`. If `serverDto.status == targetStatus`, the server already applied this change: store `serverDto`, clear `isPending`, and treat as `Applied`.
  - **Backend enhancement:** Accept an idempotency key or client timestamp (`updatedAt`) on `PATCH` requests to recognize retried requests.

## 4. What you did not build

- **WorkManager:** Omitted because repository coroutines handled in-app sync for this scope; I would add WorkManager to guarantee persistent background retries across device reboots.
- **Mutex:** Omitted for simplicity; however, if launch refresh and a tap on Start overlap, the refresh reads `getAllPending()` before the tap's upsert and runs `replaceAll` after it. A `Mutex` around repository write operations would serialize tap updates and refreshes deterministically.
- **Exponential Backoff:** Omitted in favor of retrying pending items on manual refresh or launch; I would add exponential backoff to retry network calls for a specified window without overloading the server.

## What is not done

All core features, auto-migration, typed repository outcomes, surface messages, and unit/migration tests are complete.
