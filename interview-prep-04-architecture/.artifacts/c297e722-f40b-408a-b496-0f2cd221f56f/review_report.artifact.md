# Code Review Report: PartsBin Architecture (Target: 25/25)

Based on [SCORE.md](file:///Users/aliziwa/Documents/personal/android-scenario-practice/interview-prep-04-architecture/SCORE.md), [ARCHITECTURE.md](file:///Users/aliziwa/Documents/personal/android-scenario-practice/interview-prep-04-architecture/ARCHITECTURE.md), and [INTERVIEW.md](file:///Users/aliziwa/Documents/personal/android-scenario-practice/interview-prep-04-architecture/INTERVIEW.md), the codebase in its current state **cannot score 25/25** (current estimated score sits around **13/25**).

Below is a detailed breakdown of what is failing, missing, or broken across all grading dimensions, along with the exact changes needed to achieve a perfect 25/25 score.

---

## Dimension Breakdown & Scoring Gaps

### 1. Correctness (Current: 3/5)
- **Broken 422 Error Reason:** In `DefaultPartsRepository` (or when handling server responses), parsing 422 error bodies throws a `ServerException` inside a `try` block whose inner/outer catch block intercepts it or falls back to `"Server said 422"`. Consequently, the server's specific reason (`insufficient_stock` or `reason_required`) never reaches the UI.
- **Detail Screen Regression on Error:** When an adjustment fails, `PartDetailViewModel` transitions `uiState` to `PartsDetailState.Error`. This replaces the `Loaded` state entirely, causing the part details, quantity display, amount field, reason field, and restock/use buttons to **vanish** from the screen. The technician is left stranded with only an error message and a broken Dismiss handler (`{ }`).
- **Redundant Refreshes:** The list ViewModel triggers a refresh in `init { load() }` and `LaunchedEffect(Unit) { vm.load() }` in `PartsListScreen`, causing double network calls on first composition and additional refreshes on navigation return.

### 2. Architecture and Boundaries (Current: 3/5)
- **Duplicated Business Logic:** The "low stock" rule (`quantity <= minQuantity`) is duplicated across multiple places (e.g., `Part.kt` and entity/DTO mapping extensions) rather than living solely in a domain model.
- **Misplaced Validation:** Adjustment validation (`require(...)`) lives in the repository/data layer rather than in a pure `domain` function or model.
- **Package and Layer Confusion:** `PartRow` and presentation formatting are mixed up with domain models or placed in ambiguous domain packages, blurring the boundary between pure domain logic and UI row state.

### 3. Code Quality and Idiom (Current: 3/5)
- **`catch (e: Exception)` Anti-Pattern:** Both ViewModels wrap repository calls in broad `catch (e: Exception)` blocks. This swallows coroutine `CancellationException`, which is explicitly flagged across assignments 02, 03, and 04.
- **Inline Fully Qualified Names:** Code contains inline FQNs (e.g., `dev.aliziwa.partsbin.domain.ServerException`, `retrofit2.HttpException`, `java.time.Instant`) instead of proper imports.
- **Unsafe Casts & Composable State Logic:** Using `(uiState as PartsDetailState.Loaded)` unsafe casts instead of exhaustive `when (val s = uiState)` expressions, and invoking `vm.isLowStock()` from UI when `p.lowStock` is already present.
- **Unused/Dead Code & Hygiene:** Unused `PartsListState.Error`, extra dispatchers (`launch(Dispatchers.IO)`) in ViewModels where the repository is already main-safe, and minor diff noise.

### 4. Tests (Current: 3/5)
- **Tolerating Bugs in Tests:** `DefaultPartsRepositoryTest` asserts `message == "insufficient_stock" || message == "Server said 422"`, accepting the 422 parsing bug rather than enforcing the exact contract.
- **Missing Zero-Network Assertions:** Validation tests do not assert `assertEquals(0, mockWebServer.requestCount)`, failing to prove that invalid inputs rejected client-side never touched the network.
- **Weak Exception Assertions:** Network failure tests wrap calls in `try { ... } catch (e: Exception) { assertTrue(e is IOException) }` without failing if no exception is thrown at all.
- **Missing Coverage:** Lack of tests for SKU search case-insensitivity/trimming, date formatting boundaries, and ensuring detail screen error states preserve the loaded part.

### 5. Communication & Documentation (Current: 1/5)
- **ADR Issues:** `ARCHITECTURE.md` carries `Author: AI Assistant` and is dated prior to the assignment's creation date. It refers to `AppContainer` (which was renamed/dissolved), lacks a filled-in "What you did not finish" section, and landed out of order in Git history.
- **PR Body:** Empty PR body, failing to explain how the work was produced or how to review it.
- **Commit History & Brief:** The brief was edited (`Globals` vs `AppContainer`), and commit structures do not match the expected iterative sequence outlined in instructions.

---

## Checklist to Reach 25 / 25

1. **Fix 422 Parsing in Repository:**
   - Parse error body for 422 response codes, extract `.error`, and throw `ServerException(errorReason)` *outside* the try-catch parsing block so it reaches the ViewModel/UI.
2. **Fix Detail Screen State Preservation:**
   - Keep `PartsDetailState.Loaded` on error, storing an optional error message alongside the part row (`Loaded(rowItem, message = ...)`). Dismissing clears the message, keeping the form and part visible.
3. **Consolidate Low Stock Rule:**
   - Place `val lowStock get() = quantity <= minQuantity` exclusively on the domain model (`Part`), and remove duplicate computed fields.
4. **Refactor ViewModel Exception Handling:**
   - Replace broad `catch (e: Exception)` with specific catches (`IllegalArgumentException`, `ServerException`, `IOException`), ensuring `CancellationException` is never swallowed.
5. **Strengthen Unit Tests:**
   - Update `DefaultPartsRepositoryTest` to strictly assert `assertEquals("insufficient_stock", exception?.message)`.
   - Add `assertEquals(0, mockWebServer.requestCount)` to validation tests.
   - Add `fail()` in test catch blocks to prevent false passes when exceptions aren't thrown.
6. **Hand-Write/Clean Up Documentation (`ARCHITECTURE.md`):**
   - Update `ARCHITECTURE.md` with your name, today's date, correct class names, and a completed "What you did not finish" section.
   - Restore `README.md` to its original state.
   - Fill in the PR body detailing what to read first and how the work was produced.
