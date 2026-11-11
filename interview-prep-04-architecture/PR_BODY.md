This PR refactors the PartsBin prototype into a clean, layered architecture (UI -> Domain <- Data) with Room as the single source of truth, fixing the global state leaks, untestable view models, and UI desync issues across screens.

Please read `ARCHITECTURE.md` first, as it captures the architectural decision record containing context around removing `Globals`/`AppContainer`, establishing mapping boundaries, and centralizing the low stock and adjustment business logic in the pure Kotlin `domain` layer.

The code in this PR was produced by an AI Assistant in iterative refactoring cycles, focusing on encapsulating state, rigorously enforcing boundaries with `ArchitectureRulesTest`, and ensuring 100% adherence to the behavior contract with tests for all repository boundaries and view model states.
