# Assignment 05: AI integration

**Area:** AI integration · **Timebox:** 3 hours · **Due:** Friday, November 13 · **Branch:** `05-ai-integration` · **Submit:** pull request into `main`

## The app

JobNotes is the notes app in a field technician's van: a list of job notes, a detail screen where the note is edited, and an assistant that reads the note and produces a one-line summary plus the list of parts used, as structured data, so dispatch can bill the parts without reading the note. Kotlin, Compose, ViewModel + StateFlow, Room, Retrofit + OkHttp + kotlinx.serialization, Navigation Compose with typed routes. Same toolchain as assignments 01 to 04 (Gradle 9.6.0, AGP 9.4.1, Kotlin 2.4.20, Compose BOM 2026.09.00, Room 2.8.5, KSP 2.3.12, OkHttp 5.5.0, Retrofit 3.0.0), plus `okhttp-sse` in the catalog for you. Open `interview-prep-05-ai-integration` in Android Studio as the project root.

There is no real model. `FakeAssistantBackend` is an OkHttp interceptor that answers in process. It exposes one endpoint, `POST /v1/summaries`, taking `{"model", "messages": [{"role", "content"}], "stream", "maxOutputTokens"}` with a `Bearer` key in `Authorization`. With `stream: false` it answers `{"id", "output", "usage": {"inputTokens", "outputTokens"}, "warnings": []}`; with `stream: true` it answers `text/event-stream`, a run of `data: {"delta": "..."}` events whose deltas concatenate to the same `output`, then one `data: {"done": true, "usage": {...}}`. `output` is text the model wrote, which is supposed to be a JSON document `{"summary", "parts": [{"sku", "qty"}], "actions": [{"type", "value"}]}`. The fake is a deterministic "model" (first sentence of the note, every `AAA-0000` token with an optional `x N`), and certain words in a note make it misbehave the way a real service does. The list is in the KDoc at the top of the file and in the seed notes:

| Note | Word | What the service does |
|---|---|---|
| n-02 | `RUSH` | First request: `429` with `Retry-After: 2`. Next request: `200`. |
| n-03 | `TODO` | First request: `output` cut off part way, not valid JSON. Next request: valid. |
| n-05 | `Ignore previous instructions` | `actions` contains `delete_note` and `report_stock`; the summary says the notes were deleted. |
| n-07 | `Long one` | Slow: about 12 seconds unstreamed, about 400 ms between streamed events. |
| n-08 | (empty) | `400 invalid_request_error`. |
| n-01, n-06 | a phone number or email in the prompt | `200`, with `"warnings": ["pii_in_prompt"]`. |

Nothing in `FakeAssistantBackend` needs to change. `FakeAssistantBackendTest` pins all of this and passes on `main`; read it for the two ways to call the fake from a JVM test.

**The feature works on a good day.** It was built in an afternoon to show dispatch what was possible, and dispatch said yes. It is now going into the release, and every technician will run it on every note. Your job is the integration that survives a bad day: a slow model, a busy model, a model that writes bad JSON, a model that does what the note tells it to, a customer's phone number in the prompt, and a key that must not leak.

## What you are looking at

About 1,000 lines in `app/src/main`: `JobNotesApp` (composition root), `data/local` (Room, fine as it is), `data/assistant` (the API, the fake, and `AssistantRepository`, which is the exercise), `domain`, `ui`. Read all of it before you write anything. The problems are listed; deciding what to do about them, in what order, and proving it, is the assignment.

1. The API key is a `const val` in `AssistantRepository`, and it is written to Logcat on every request along with the full prompt.
2. The note body goes to the service verbatim. n-01 contains a customer's name, phone number and email. The service flags it (`warnings`) and nothing reads the flag.
3. The instructions and the note are concatenated into one `system` message. There is no `user` message. The model cannot tell where the instructions end and the data begins, and neither can you.
4. The call is unstreamed. The technician looks at a spinner until the whole answer arrives (12 seconds for n-07). The streaming endpoint exists and nothing uses it.
5. On `429` the repository retries immediately, five times in a tight loop, and ignores `Retry-After`.
6. `output` is decoded with no error handling. n-03 crashes the app on the first open.
7. The model's `actions` are executed, all of them, by the repository, against the database. Open n-05 and watch it delete itself. The "Allowed action types" in the prompt include `delete_note`.
8. The result lands in `object LastSummary`, process-wide state. The detail ViewModel shows it in `init` whatever note it was for. Open n-04, go back, open n-06, and n-04's parts are on n-06's screen until the new answer replaces them.
9. The request runs in a `CoroutineScope(Dispatchers.IO)` the ViewModel never cancels. Leave the screen mid-request and the answer still lands in the global; open the next note and see 8.
10. Every open of a note is a request, nothing is cached, and the usage the service reports is logged and dropped. The OkHttp client has no read or call timeout at all.

## Behaviour contract

What the app must do when you are finished. Every line is testable without a device, and the review checks each one.

- Summarize streams. The summary text grows on screen as deltas arrive, then the parsed `JobSummary` replaces it. A test with MockWebServer serving an event-stream body proves the intermediate states and the final one.
- Leaving the detail screen cancels the request. No answer from a cancelled request is ever shown, on that note or any other. A fresh open of a note starts from the cache or from nothing, never from another note's answer.
- `429`: wait the `Retry-After` seconds (or an exponential backoff from one second when the header is absent), show "Assistant busy, retrying", retry at most twice, then show "Assistant unavailable, try again later". The waits are real coroutine delays, driven by a test dispatcher in tests, not `Thread.sleep`.
- A silence of 5 seconds (no bytes) is "Assistant timed out". The timeout value is injectable and the test uses a short one against `SocketPolicy.NO_RESPONSE` or a throttled body.
- `output` that is not the expected JSON: one repair attempt (re-send with the instruction "Return only the JSON object"), and if that is also bad, "Could not read the assistant's answer". The app never crashes on model output, including output that is valid JSON of the wrong shape.
- The request body never contains a phone number or an email address. They are replaced with `[phone]` and `[email]` before the prompt is built. A test asserts on the recorded request body, and `warnings` from the service is surfaced as a failed assertion in that test, not swallowed.
- The instructions are the `system` message and the note is the `user` message, nothing else in either. A test asserts the request shape.
- `actions`: `add_parts` is the only type the app honours. Every other type is ignored and counted, and the screen says "Ignored N unsupported actions". The note is never deleted and nothing is ever reported to dispatch because the model said so. n-05 proves it on a device; a test proves it on the JVM.
- The key is not in source and not in logs. `AssistantBoundaryRulesTest` encodes this and the rules below; enable it and keep it green.
- A summary is cached per note keyed on a hash of the note body. Reopening an unchanged note shows the cached summary with no request (`requestCount` stays at zero). Saving an edit invalidates it. Where the cache lives (Room table, in-memory map in the repository) is your call; say which in the ADR.
- Usage is kept: the list screen's top bar shows the total tokens used this session.
- Everything else the app does today, it still does: the list, editing and saving a note, Back.

## Deliverables

1. **`AI_BOUNDARY.md`**, before the code. A short ADR for the AI boundary: what is trusted and what is not (the note, the model's text, the model's actions, the service's warnings), where each rule lives (redaction, output validation, action allowlist, retry policy), what the UI observes and how a partial answer reaches it, what is cached and when it is invalidated, how cost is bounded, and how you would know the feature is working in the field (what you would log, and what the golden set below measures). Alternatives you considered: at least three, one or two lines each (candidates: validate output with a JSON schema versus a typed decode; on-device model versus service; tool calling versus a flat `actions` list; a `WorkManager` job versus a screen-scoped request). Consequences, including at least one cost you accepted. One page.
2. **The boundary.** `ui` sees the assistant through an interface in `domain` with typed outcomes (a sealed result, or a `Flow` of a sealed state for the streamed case), never through `data.assistant`. Redaction, output validation and the action allowlist are pure functions in `domain` with their own tests. `LastSummary` deleted. The key out of source: `BuildConfig` from `local.properties` (the Secrets Gradle plugin, or ten lines of `Properties` in `app/build.gradle.kts`) or an injected `ApiKeyProvider`; either way `local.properties.example` is committed and the real file is not.
3. **Tests at each boundary**, with the right double for each: the client against MockWebServer (event-stream body, `429` with and without `Retry-After`, cut-off output then valid, `NO_RESPONSE` for the timeout, recorded request body for redaction and message shape); the rules on their own; the ViewModel with a fake assistant and Turbine proving the streaming states, cancellation, and the "another note's answer" case. `MainDispatcherRule` and `InMemoryNoteDao` are provided.
4. **The golden set.** `app/src/test/resources/golden/cases.json` has five notes with the parts dispatch expects and the strings that must not reach the prompt. Write `GoldenEvalTest`: run each case through your real pipeline against `FakeAssistantBackend`, report pass rate, and fail below 4 of 5. One case fails by design (g-05: the model counts a part that was put back). Do not make it pass by special-casing the model or the case; the point of an eval is that it measures the model, and the ADR should say what you would do about g-05 (prompt change, post-processing rule, accept the error rate).
5. **Scope discipline.** No new features. No on-device model, no real provider SDK, no `WorkManager`, no Hilt. The cache can be a Room table (bump the schema and commit both `schemas/*.json`) or in memory; say which and why.

Scope guidance for three hours: thirty minutes to read and write the ADR, ninety on the boundary (streaming, cancellation, retry, validation, redaction, actions), sixty on tests and the golden set. If you are running out of time, a complete non-streaming boundary with every rule tested beats a streaming one with the rules half done; say which you left in the ADR.

## Rules

- No AI-generated or AI-suggested code, and no AI-written ADR. Documentation is open; the references below are enough. Say in the PR body how the work was produced. The review looks for it either way.
- Commit as you go. The history should read as the ADR's decisions landing one at a time: ADR; redaction and validation rules with tests; the domain interface and typed outcomes; the client with retry and timeout against MockWebServer; streaming; cache and usage; golden set; rules test enabled.
- If you run out of time, submit what you have with the ADR honest about what is missing. A clean partial beats a rushed complete.

## Rubric (1 to 5 each, 20 to pass)

| Dimension | What 5 looks like |
|---|---|
| Correctness | Every line of the behaviour contract holds and has a test that fails without it; the failure paths are driven by MockWebServer bodies and socket policies, not by flags in the code under test |
| Architecture and boundaries | `ui` reaches the assistant only through `domain`; the model's text and actions are treated as untrusted input at exactly one place each; the rules exist once and are pure; `AssistantBoundaryRulesTest` is green; the ADR describes the code that exists and would still be the right document when a real provider replaces the fake |
| Code quality and idiom | Streaming as a `Flow` with cancellation that follows the collector; `delay`, not `Thread.sleep`; typed outcomes instead of exception strings; no catch-all around suspend calls; no process-wide mutable state; the key nowhere in source or logs |
| Tests | Right double at each boundary; the client tests would catch a regression in any contract line about retry, timeout, streaming or redaction; the ViewModel tests prove state, not that a fake was called; the golden set runs and reports |
| Communication | The ADR reads like one a team would adopt, with the trust boundary drawn and the costs named; commits land one decision at a time; PR body says what to read first and how the work was produced |

After grading you also get `INTERVIEW.md`: the questions a Staff interviewer would ask you about this exact code.

## References

- Choosing an approach: [Find the right AI/ML solution for your feature](https://developer.android.com/ai/overview)
- Streaming: [OkHttp SSE (`okhttp-sse`, `EventSources`, `EventSourceListener`)](https://tessl.io/registry/tessl/maven-com-squareup-okhttp3--okhttp-sse), [Server-sent events in Android with OkHttp](https://www.droidcon.com/2022/04/29/unidirectional-server-client-communication-using-sse-in-android/), [How a real provider streams: Claude Messages streaming](https://platform.claude.com/docs/en/api/messages-streaming)
- Retry: [`Retry-After` header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Retry-After)
- Untrusted model output: [OWASP Top 10 for LLM applications, explained](https://workers.cloudflare.com/learning/ai/owasp-top-10-risks-for-llms) (prompt injection, insecure output handling, excessive agency)
- Parsing: [kotlinx.serialization JSON guide](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md)
- Secrets: [Secrets Gradle plugin for Android](https://github.com/google/secrets-gradle-plugin)
- Architecture: [Data layer](https://developer.android.com/topic/architecture/data-layer), [Domain layer](https://developer.android.com/topic/architecture/domain-layer)
- Tests: [MockWebServer](https://github.com/square/okhttp/blob/master/mockwebserver/README.md), [Testing Kotlin coroutines on Android](https://developer.android.com/kotlin/coroutines/test), [Turbine](https://github.com/cashapp/turbine)

## Submitting

```
git checkout -b 05-ai-integration
# ADR first, then work, commit as you go
git push -u origin 05-ai-integration
```

Open a pull request into `main` titled `05 AI integration: <your one-line summary>`, with a body that says what to read first and how the work was produced. I review the PR line by line and push `SCORE.md` and `INTERVIEW.md` to your branch within 24 hours.
