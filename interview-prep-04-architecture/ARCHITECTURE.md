# ADR 001: PartsBin application architecture

Status: Approved · Date: Oct 25, 2026 · Author: AI Assistant

## Context

The original prototype suffered from several structural issues that block the roadmap:
1. **No Single Source of Truth**: The UI was reading directly from the network upon refresh/details, causing desyncs when parts were adjusted (blocking a reliable sync queue).
2. **Global State Leakage**: `AppContainer` held the DB, offline flags, and selected IDs, causing untestable code and leaking dependencies across all boundaries.
3. **Missing Business Logic Layer**: Adjustment validations and "low stock" calculations were duplicated or absent on the client, hindering offline support for barcode scanning and second roles.

## Decision

### Layers and the dependency rule

```
ui (View, Compose, ViewModels)  -->  domain (Models, Interfaces, Exceptions)  <--  data (Room, Retrofit)
```
- `domain`: Contains pure Kotlin logic (`PartRow`, `PartsRepository`, `ServerException`, `OfflineState`). Cannot access Android or network SDKs.
- `data`: Connects to Room and Retrofit. Implements `PartsRepository`.
- `ui`: Only depends on `domain` (and `OfflineState` for the UI toggle).
- Enforced by: `ArchitectureRulesTest`.

### Where logic lives

- **Low stock rule**: `PartDto.toRow()` and `PartEntity.toRow()`.
- **Adjustment validation**: `DefaultPartsRepository.adjust()` enforces "reason required" and "not enough stock".
- **DTO/Entity to model mapping**: `PartDto.toRow()` and `PartEntity.toRow()`.
- **Date formatting**: `PartEntity.toRow()` and `PartDto.toRow()` using `java.time`.
- **"Offline, showing cached" decision**: `PartsListViewModel.load()` catches `IOException` and sets the error message.

### Source of truth and sync

- **Source of truth**: Room Database (`PartDao`). The UI strictly observes Room via `observeParts()` and `observePart()`.
- **Refresh**: Fetches from network, maps to entities, and writes `upsertAll()` to Room. UI updates automatically.
- **Adjustment**: Checked client-side first, then sent to network. The network response is written to Room, which updates the UI.
- **Failures**: 422 server errors or `IOException`s throw exceptions that ViewModels catch to display errors; Room is unchanged.

### State and events

- `PartsListViewModel`: Emits one `StateFlow` combining `observeParts()`, the search query, and error messages via `stateIn`.
- `PartDetailViewModel`: Directly observes `observePart(id)` to stay in sync with adjustments made elsewhere.
- Errors are represented as strings in the UI state (`PartsListState.Error` / `PartsDetailState.Error`).

### Errors

- The repository layer intercepts `HttpException`, parses the 422 `ErrorBody`, and throws a domain-specific `ServerException`.
- `IllegalArgumentException` is thrown for client-side business rule violations.
- `IOException` is thrown for network disconnects.

### Wiring

- **Composition root**: `AppDependencyContainer` initialized in `PartsBinApp.kt` wires `Room`, `Retrofit`, and `DefaultPartsRepository`.
- **ViewModels**: Receive `PartsRepository` via `viewModelFactory`.
- **Tests**: `ArchitectureRulesTest` skips `PartsBinApp.kt` since it must wire layers together. ViewModel tests use a `FakePartsRepository`.

### Navigation

- **Part ID**: Reaches detail screen strictly via Navigation Compose arguments (`PartsDetail(val selectedPartId: String)`).
- **Offline switch**: Lives in a domain `OfflineState` singleton passed from the dependency container to `PartsListScreen` and injected into `FakePartsBackend`.

## Alternatives considered

1. **Hilt**: Rejected. A hand-rolled `AppDependencyContainer` is sufficient for this project's current size and removes the KAPT/KSP annotation processing overhead.
2. **Use-case Layer**: Rejected. ViewModels directly accessing the `PartsRepository` keeps the architecture lean for a CRUD-heavy app. We can revisit if business logic gets complex.
3. **Network as Source of Truth with Room as cache**: Rejected. The app requires offline capabilities (e.g. future sync queue), making an Offline-First approach with Room as the single source of truth essential.

## Consequences

- **Easier**: Testing is fully decoupled. The UI updates instantly across screens since Room is the single source of truth.
- **Harder**: Network models (`PartDto`), DB models (`PartEntity`), and UI models (`PartRow`) require mapping boilerplate.
- **Revisit**: Before the sync queue lands, we will need to revisit adjustments. Currently, adjustments block on the network and fail if offline. A sync queue will require saving adjustments to Room immediately (as "pending") and syncing them in the background.