# ADR 001: PartsBin application architecture

Status: Accepted · Date: November 11, 2026 · Author: Ali Ziwa

## Context

The original prototype suffered from several structural issues that block the roadmap:
1. **No Single Source of Truth**: The UI was reading directly from the network upon refresh/details, causing desyncs when parts were adjusted (blocking a reliable sync queue).
2. **Global State Leakage**: `Globals` (later renamed `AppContainer`) held the DB, offline flags, and selected IDs, causing untestable code and leaking dependencies across all boundaries.
3. **Missing Business Logic Layer**: Adjustment validations and "low stock" calculations were duplicated or absent on the client, hindering offline support for barcode scanning and second roles.

## Decision

### Layers and the dependency rule

```
ui (View, Compose, ViewModels, UI Models)  -->  domain (Models, Interfaces, Exceptions)  <--  data (Room, Retrofit)
```
- `domain`: Contains pure Kotlin logic (`Part`, `PartsRepository`, `AdjustmentResult`, `AdjustmentError`, `OfflineState`). Cannot access Android or network SDKs.
- `data`: Connects to Room and Retrofit. Implements `PartsRepository`.
- `ui`: Only depends on `domain` (and `OfflineState` for the UI toggle). Contains `PartRow` model and mappers.
- Enforced by: `ArchitectureRulesTest`.

### Where logic lives

- **Low stock rule**: `Part` (Domain model).
- **Adjustment validation**: `validateAdjustment()` in the domain layer, producing an `AdjustmentError`.
- **DTO/Entity to model mapping**: `PartDto.toEntity()` and `PartEntity.toDomain()`.
- **Date formatting**: `PartMapper.toRow()` using `java.time` in the `ui` package.
- **"Offline, showing cached" decision**: `PartsListViewModel.load()` handles `RefreshResult.Offline` or offline exceptions.

### Source of truth and sync

- **Source of truth**: Room Database (`PartDao`). The UI strictly observes Room via `observeParts()` and `observePart()`.
- **Refresh**: Fetches from network, maps to entities, and writes `upsertAll()` to Room. UI updates automatically.
- **Adjustment**: Validated client-side using `Part.validateAdjustment()`, then sent to network. On success, the response is written to Room, which updates the UI.
- **Failures**: `ServerRejected`, `Rejected`, or `Offline` are modeled via `AdjustmentResult` and passed to the UI; Room is unchanged.

### State and events

- `PartsListViewModel`: Emits one `StateFlow` combining `observeParts()`, the search query, and error messages via `stateIn`.
- `PartDetailViewModel`: Directly observes `observePart(id)` to stay in sync with adjustments made elsewhere. Emits a separate `message` StateFlow for adjustment outcomes.

### Errors

- The repository layer catches `HttpException`, parses the 422 `ErrorBody`, and returns a `AdjustmentResult.ServerRejected`.
- Client-side business rules return `AdjustmentResult.Rejected`.
- Network disconnects return `AdjustmentResult.Offline`.

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
- **Harder**: Network models (`PartDto`), DB models (`PartEntity`), Domain models (`Part`) and UI models (`PartRow`) require mapping boilerplate.
- **Revisit**: Before the sync queue lands, we will need to revisit adjustments. Currently, adjustments block on the network and fail if offline. A sync queue will require saving adjustments to Room immediately (as "pending") and syncing them in the background.

## What you did not finish

While the behavioral contract is complete, the `refreshParts` signature across the boundary is currently returning a `RefreshResult` sealed class, but we still catch network IO/Http exceptions inside the repository. The scope for the future sync queue and local caching adjustments as "pending" has not been done since it was excluded from the strict three-hour timebox.
