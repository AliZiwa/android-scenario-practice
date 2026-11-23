# Assignment 05: AI integration

**Area:** AI integration · **Timebox:** 3 hours · **Due:** Friday, November 13 · **Branch:** `05-ai-integration` · **Submit:** pull request into `main`

## Start here

This section is the whole assignment in plain words. The "Full brief" section below it is the detailed reference for when you are working on a specific point.

### What the app is

JobNotes is a small app with a list of job notes and an editor for one note. When you open a note, the app sends the note text to an "AI assistant" and gets back a short summary plus the parts used (part number and quantity), so dispatch can bill the parts without reading the note.

There is no real AI. A fake one lives inside the app (`FakeAssistantBackend`) and pretends to be the service. You do not change the fake.

### How the fake AI misbehaves

The app ships with sample notes named n-01 to n-08. If a note contains a certain word, the fake AI does something bad on purpose. These are your test scenarios, not tasks.

| Note | Word in the note | What the fake AI does |
|---|---|---|
| n-02 | `RUSH` | First request fails with "429 too busy" and a `Retry-After: 2` header. Second request succeeds. |
| n-03 | `TODO` | First answer is cut off in the middle, so it is not valid JSON. Second answer is fine. |
| n-05 | `Ignore previous instructions` | The answer tells the app to delete the note and report stock. |
| n-07 | `Long one` | Very slow: about 12 seconds for a full answer, about 400 ms between streamed pieces. |
| n-08 | (empty note) | Fails with "400 invalid request". |
| n-01, n-06 | a phone number or email in the note | Succeeds, but the answer includes a warning `pii_in_prompt` (personal data was sent). |

### What you have to do

The feature works when the AI behaves. Make it work when the AI misbehaves. These are the fixes, in a sensible order:

1. **The secret key.** It is hard-coded in `AssistantRepository` and printed to Logcat with the full prompt on every request. Move it to `local.properties` (not committed) and read it through `BuildConfig` or an injected `ApiKeyProvider`. Commit `local.properties.example`. Never log the key or the prompt.
2. **Privacy.** Before sending a note, replace any phone number with `[phone]` and any email address with `[email]`.
3. **Message shape.** Send two messages: a `system` message with your instructions and a `user` message with the note. Today both are glued into one `system` message.
4. **Streaming.** Use the streaming endpoint (`stream: true`) so the summary text appears on screen piece by piece instead of a spinner for 12 seconds.
5. **Retry on 429.** Wait the seconds given in `Retry-After` (or 1 s, 2 s, 4 s when the header is missing), show "Assistant busy, retrying", retry at most twice, then show "Assistant unavailable, try again later". Use coroutine `delay`, not `Thread.sleep`.
6. **Bad output.** If the answer is not valid JSON of the expected shape, send one more request with the instruction "Return only the JSON object". If that is also bad, show "Could not read the assistant's answer". The app must never crash on anything the AI returns.
7. **Actions.** The AI returns a list of actions. Only `add_parts` may be executed. Every other type is ignored and counted, and the screen says "Ignored N unsupported actions". The AI must never be able to delete a note or report to dispatch.
8. **No global state.** Delete `object LastSummary`. The answer belongs to the ViewModel of the note that asked for it, so n-04's parts can never appear on n-06's screen.
9. **Cancel on leave.** Leaving the detail screen cancels the request. A cancelled request never shows its answer anywhere.
10. **Timeout, cache, usage.** Five seconds of silence (no bytes) means "Assistant timed out". Cache the summary per note (key: a hash of the note body) so reopening an unchanged note makes no request; saving an edit clears it. Show the total tokens used this session in the list screen's top bar.

One structural rule on top: the `ui` package must reach the assistant only through an interface in `domain`, never through `data.assistant` directly. Redaction (fix 2), output validation (fix 6) and the action allowlist (fix 7) are plain functions in `domain` with their own tests.

### What to deliver

1. **`AI_BOUNDARY.md` first.** A one-page design note (an ADR). The file already has headings with instructions under each; replace the instructions with your answers, then write code.
2. **The fixes** above.
3. **Tests.** For each fix, a test that fails if the fix is removed. `MockWebServer` is a fake HTTP server that runs inside a unit test: you tell it what to answer (a 429, a cut-off body, no response at all) and check what your code does. For the ViewModel, use the provided fake assistant and Turbine.
4. **`GoldenEvalTest`.** Run the five notes in `app/src/test/resources/golden/cases.json` through your real code and check the parts match. Pass if 4 of 5 pass. Case g-05 fails on purpose (the model counts a part that was put back). Do not hack it to pass; explain in the ADR what you would do about it.
5. **Enable `AssistantBoundaryRulesTest`** and keep it green.

### Rules and time

- No AI-written code and no AI-written ADR. Documentation is open. Say in the PR body how you produced the work.
- Commit as you go, one decision per commit.
- Three hours: about 30 minutes reading and writing the ADR, 90 minutes on the fixes, 60 on tests and the golden set.
- If you run out of time, submit what you have and say in the ADR what is missing. A clean partial scores better than a rushed complete. A non-streaming version with every rule tested beats a streaming one with the rules half done.
- Out of scope: no on-device model, no real provider SDK, no `WorkManager`, no Hilt, no new features.

### Submitting

```
git checkout -b 05-ai-integration
# ADR first, then work, commit as you go
git push -u origin 05-ai-integration
```

Open a pull request into `main` titled `05 AI integration: <your one-line summary>`. In the body say what to read first and how the work was produced. I review it line by line and push `SCORE.md` and `INTERVIEW.md` to your branch within 24 hours.

---

## Full brief (reference)

### The app in detail

Kotlin, Compose, ViewModel + StateFlow, Room, Retrofit + OkHttp + kotlinx.serialization, Navigation Compose with typed routes. Same toolchain as assignments 01 to 04 (Gradle 9.6.0, AGP 9.4.1, Kotlin 2.4.20, Compose BOM 2026.09.00, Room 2.8.5, KSP 2.3.12, OkHttp 5.5.0, Retrofit 3.0.0), plus `okhttp-sse` in the catalog for you. Open `interview-prep-05-ai-integration` in Android Studio as the project root.

`FakeAssistantBackend` is an OkHttp interceptor that answers in process. It exposes one endpoint, `POST /v1/summaries`, taking `{"model", "messages": [{"role", "content"}], "stream", "maxOutputTokens"}` with a `Bearer` key in `Authorization`. With `stream: false` it answers `{"id", "output", "usage": {"inputTokens", "outputTokens"}, "warnings": []}`; with `stream: true` it answers `text/event-stream`, a run of `data: {"delta": "..."}` events whose deltas concatenate to the same `output`, then one `data: {"done": true, "usage": {...}}`. `output` is text the model wrote, which is supposed to be a JSON document `{"summary", "parts": [{"sku", "qty"}], "actions": [{"type", "value"}]}`. The fake is a deterministic "model": first sentence of the note as the summary, every `AAA-0000` token with an optional `x N` as a part. The trigger words are listed in the KDoc at the top of the file and in the seed notes. `FakeAssistantBackendTest` pins all of this and passes on `main`; read it for the two ways to call the fake from a JVM test.

About 1,000 lines in `app/src/main`: `JobNotesApp` (composition root), `data/local` (Room, fine as it is), `data/assistant` (the API, the fake, and `AssistantRepository`, which is the exercise), `domain`, `ui`. Read all of it before you write anything.

### Behaviour contract

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

### Deliverables in detail

1. **`AI_BOUNDARY.md`**, before the code. What is trusted and what is not (the note, the model's text, the model's actions, the service's warnings), where each rule lives (redaction, output validation, action allowlist, retry policy), what the UI observes and how a partial answer reaches it, what is cached and when it is invalidated, how cost is bounded, and how you would know the feature is working in the field (what you would log, and what the golden set measures). At least three alternatives considered, one or two lines each (candidates: JSON schema validation versus a typed decode; on-device model versus service; tool calling versus a flat `actions` list; a `WorkManager` job versus a screen-scoped request). Consequences, including at least one cost you accepted. One page.
2. **The boundary.** `ui` sees the assistant through an interface in `domain` with typed outcomes (a sealed result, or a `Flow` of a sealed state for the streamed case), never through `data.assistant`. Redaction, output validation and the action allowlist are pure functions in `domain` with their own tests. `LastSummary` deleted. The key out of source: `BuildConfig` from `local.properties` (the Secrets Gradle plugin, or ten lines of `Properties` in `app/build.gradle.kts`) or an injected `ApiKeyProvider`; either way `local.properties.example` is committed and the real file is not.
3. **Tests at each boundary**, with the right double for each: the client against MockWebServer (event-stream body, `429` with and without `Retry-After`, cut-off output then valid, `NO_RESPONSE` for the timeout, recorded request body for redaction and message shape); the rules on their own; the ViewModel with a fake assistant and Turbine proving the streaming states, cancellation, and the "another note's answer" case. `MainDispatcherRule` and `InMemoryNoteDao` are provided.
4. **The golden set.** `app/src/test/resources/golden/cases.json` has five notes with the parts dispatch expects and the strings that must not reach the prompt. `GoldenEvalTest` runs each case through your real pipeline against `FakeAssistantBackend`, reports the pass rate, and fails below 4 of 5. The point of an eval is that it measures the model, so g-05 stays failing and the ADR says what you would do about it (prompt change, post-processing rule, accept the error rate).
5. **Scope discipline.** No new features. The cache can be a Room table (bump the schema and commit both `schemas/*.json`) or in memory; say which and why.

Suggested commit order: ADR; redaction and validation rules with tests; the domain interface and typed outcomes; the client with retry and timeout against MockWebServer; streaming; cache and usage; golden set; rules test enabled.

### Rubric (1 to 5 each, 20 to pass)

| Dimension | What 5 looks like |
|---|---|
| Correctness | Every line of the behaviour contract holds and has a test that fails without it; the failure paths are driven by MockWebServer bodies and socket policies, not by flags in the code under test |
| Architecture and boundaries | `ui` reaches the assistant only through `domain`; the model's text and actions are treated as untrusted input at exactly one place each; the rules exist once and are pure; `AssistantBoundaryRulesTest` is green; the ADR describes the code that exists and would still be the right document when a real provider replaces the fake |
| Code quality and idiom | Streaming as a `Flow` with cancellation that follows the collector; `delay`, not `Thread.sleep`; typed outcomes instead of exception strings; no catch-all around suspend calls; no process-wide mutable state; the key nowhere in source or logs |
| Tests | Right double at each boundary; the client tests would catch a regression in any contract line about retry, timeout, streaming or redaction; the ViewModel tests prove state, not that a fake was called; the golden set runs and reports |
| Communication | The ADR reads like one a team would adopt, with the trust boundary drawn and the costs named; commits land one decision at a time; PR body says what to read first and how the work was produced |

After grading you also get `INTERVIEW.md`: the questions a Staff interviewer would ask you about this exact code.

### References

- Choosing an approach: [Find the right AI/ML solution for your feature](https://developer.android.com/ai/overview)
- Streaming: [OkHttp SSE (`okhttp-sse`, `EventSources`, `EventSourceListener`)](https://tessl.io/registry/tessl/maven-com-squareup-okhttp3--okhttp-sse), [Server-sent events in Android with OkHttp](https://www.droidcon.com/2022/04/29/unidirectional-server-client-communication-using-sse-in-android/), [How a real provider streams: Claude Messages streaming](https://platform.claude.com/docs/en/api/messages-streaming)
- Retry: [`Retry-After` header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Retry-After)
- Untrusted model output: [OWASP Top 10 for LLM applications, explained](https://workers.cloudflare.com/learning/ai/owasp-top-10-risks-for-llms) (prompt injection, insecure output handling, excessive agency)
- Parsing: [kotlinx.serialization JSON guide](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md)
- Secrets: [Secrets Gradle plugin for Android](https://github.com/google/secrets-gradle-plugin)
- Architecture: [Data layer](https://developer.android.com/topic/architecture/data-layer), [Domain layer](https://developer.android.com/topic/architecture/domain-layer)
- Tests: [MockWebServer](https://github.com/square/okhttp/blob/master/mockwebserver/README.md), [Testing Kotlin coroutines on Android](https://developer.android.com/kotlin/coroutines/test), [Turbine](https://github.com/cashapp/turbine)
