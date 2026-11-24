# Assignment 05: what you will learn

**Overall level: Advanced.** This is the level expected of a Senior Android engineer who owns a feature that calls an external AI service in production. A Staff engineer is expected to do this and also write the design note (ADR) in a way the whole team can follow.

Scale used in this program:

| Level | Meaning |
|---|---|
| Beginner | You know the language and the framework. You can follow instructions and make a change in a small codebase. |
| Intermediate | You can build a feature end to end (UI, state, data) and write basic tests for it. Mid-level engineer. |
| Advanced | You can take a feature that works on a good day and make it safe on a bad day: failures, timeouts, cancellation, untrusted input. You test each failure path. Senior engineer. |
| Expert | You design the boundary before the code, name the trade-offs, and your design note would still be right when the libraries change. Staff engineer. |

## What you will learn

Each item says what the skill is, which fix in the README teaches it, and its level.

1. **Keeping secrets out of source code.** How to read an API key from `local.properties` through `BuildConfig` or an injected provider, and why logging a key or a prompt is a security defect. Fix 1. Intermediate.
2. **Redacting personal data before it leaves the device.** Writing a pure function that replaces phone numbers and emails, and proving with a test that the request body never contains them. Fix 2. Intermediate.
3. **Prompt structure.** Why instructions go in a `system` message and user data goes in a `user` message, and what goes wrong when they are mixed (the model cannot tell data from instructions, which is how prompt injection works). Fix 3. Intermediate to Advanced.
4. **Streaming a response with server-sent events (SSE).** Using OkHttp's `okhttp-sse` to receive an answer piece by piece and exposing it to the UI as a Kotlin `Flow`. Fix 4. Advanced.
5. **Retry with backoff.** Reading the `Retry-After` header, waiting with coroutine `delay`, capping the number of retries, and testing the waits with a test dispatcher so the test runs in milliseconds. Fix 5. Advanced.
6. **Treating model output as untrusted input.** Validating the JSON shape with a typed decode, making one repair attempt, and never crashing on anything the model returns. Fix 6. Advanced.
7. **Limiting what the model can make the app do (an action allowlist).** Only `add_parts` is executed. This is the "excessive agency" problem from the OWASP list for LLM applications. Fix 7. Advanced.
8. **Removing global mutable state.** Why a process-wide `object` holding the last result causes one note's answer to appear on another note's screen, and how scoping the result to the ViewModel fixes it. Fix 8. Intermediate to Advanced.
9. **Structured concurrency and cancellation.** Running the request in `viewModelScope` so leaving the screen cancels it, and proving with Turbine that a cancelled request never emits. Fix 9. Advanced.
10. **Timeouts, caching and cost.** A silence timeout that is injectable for tests, a per-note cache keyed on a hash of the note, and keeping token usage visible. Fix 10. Advanced.
11. **Testing at the right boundary.** MockWebServer for the HTTP client (status codes, cut-off bodies, no response), plain unit tests for the pure rules, a fake assistant plus Turbine for the ViewModel. Deliverable 3. Advanced.
12. **Evaluating an AI feature with a golden set.** Running fixed inputs through the real pipeline, reporting a pass rate, and accepting that one case fails because the model is imperfect. Deliverable 4. Advanced to Expert. Most Android engineers have never done this.
13. **Writing an ADR for a trust boundary.** Saying what is trusted, what is not, where each rule lives, what it costs, and what you would log in the field. Deliverable 1. Expert.

## Where this sits in the program

| Assignment | Area | Level |
|---|---|---|
| 01 | Debugging | Intermediate |
| 02 | Enhancement | Intermediate |
| 03 | Code smells | Intermediate to Advanced |
| 04 | Architecture | Advanced |
| 05 | AI integration | Advanced (this one) |
| 06 to 10 | Testing, performance, concurrency, WebSocket, GraphQL | Advanced |
| 11 | Code review | Advanced to Expert |
| 12 | Take-home simulation | Expert |

## How to know you have learned it

You can explain, without notes, why each of the ten fixes was needed and what test would catch its return. That is what `INTERVIEW.md` checks after grading.
