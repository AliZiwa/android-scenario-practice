# Design notes: status changes from the phone

Keep this under a page. Short answers with a reason beat long answers without one.

## 1. Where does the pending change live?

A boolean column `isPending` on the existing `work_orders` Room database table.

- **Why:** Keeps local reads persistent across process deaths, allows Room to be the single source of truth for the UI, and enables instant Flow emissions and querying (`WHERE isPending = 1`). Database migrations use Room AutoMigrations to ensure smooth, non-destructive schema updates without losing cached data.
- **Cost of future edits:** For a simple status flag, a boolean column on the entity is lightweight and zero-overhead. If complex multi-field offline edits (such as technician notes or attachments) arrive next quarter, adding columns per edit type will cause schema bloat. A separate pending mutations queue table would scale better for heterogeneous edits, but `isPending` was chosen here for minimal complexity and seamless UI binding.

## 2. The process dies between the Room write and the PATCH

- **Technician view:** Because `isPending` is persisted in Room, on relaunch the technician immediately sees their work order in "Pending sync" status.
- **Server state:** The server has not received the `PATCH` yet and holds the previous version and status.
- **Sync mechanism:** On launch or manual refresh, `DefaultWorkOrdersRepository.refresh()` runs `dao.getAllPending()`, retrying pending `PATCH` updates first before fetching the server list. If versions match, the server updates to the new status and returns 200 OK, clearing the `isPending` flag in Room.

## 3. The PATCH reached the server but the response never came back

- **On retry:** The server applied the change and bumped `version` to $v+1$, but the client's last known version is still $v$. The retried `PATCH` sends version $v$, triggering a `409 Conflict`. Under the current 409 handling, the server's representation overwrites the client, dropping the client's pending edit.
- **Is the outcome acceptable?:** No, because the technician's edit was valid and processed by the server, but the loss of the network response causes it to appear as a stale edit collision.
- **Changes for client & backend:**
  - **Client:** Incorporate timestamp-based conflict resolution (`updatedAt`) alongside monotonic version numbers when updating work orders.
  - **Backend:** Ask the backend team to accept client timestamps (`updatedAt`) or an idempotency key on `PATCH` requests to recognize retried requests or resolve conflicts based on latest timestamp.

## 4. What you did not build

- **WorkManager:** Omitted because foreground coroutines in the repository handled in-app sync for this scope; I would add WorkManager to guarantee persistent background retries across device reboots and app process kills.
- **Mutex:** Omitted because current repository coroutine execution avoids concurrent writes; I would add a `Mutex` if background sync and UI updates run concurrently on separate threads.
- **Exponential Backoff:** Omitted in favor of retrying pending items on manual refresh or launch; I would add exponential backoff to systematically retry failed network calls for a specified window before giving up without overloading the backend server.

## What is not done

Core features and unit tests are complete. With another hour, I would integrate WorkManager for background retry scheduling and implement timestamp-assisted conflict resolution.
