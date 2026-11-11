# ADR 002: The assistant boundary in JobNotes

Status: Proposed · Date: · Author:

Keep this to about a page. Write it before the code, and update it when the code teaches you something. Where this document and the code disagree at review time, the document loses. Delete these instruction paragraphs when you fill the sections in.

## Context

What the prototype does with the assistant today and why that cannot ship to every technician. Of the ten problems in the README, say which three matter most and why, in your words.

## Trust

A table. For each of these, trusted or untrusted, and what the app does about it: the note body; the service's `warnings`; the model's `summary`; the model's `parts`; the model's `actions`; the `usage` figures; the API key.

## Decision

### Where the rules live

Name the class or function for each: redaction; prompt construction (which message is system, which is user); output validation (shape, and what counts as bad); the action allowlist; the retry policy; the timeout; the cache key and invalidation.

### What the UI observes

The type the ViewModel collects, how a partial (streamed) answer reaches the screen, how a cancelled request is kept off the screen, and how the second screen learns what the first did.

### Cost

What bounds the number of requests and tokens per note and per session, and where usage is kept.

### Observability

What you would log in the field (and what you would never log), and what the golden set measures. Say what you would do about g-05.

## Alternatives considered

At least three, one or two lines each on why not here. Candidates: JSON schema validation versus a typed decode; an on-device model versus the service; tool calling versus a flat `actions` list; a `WorkManager` job versus a screen-scoped request; caching in Room versus in memory.

## Consequences

What gets easier, what gets harder, and what you would revisit when a real provider replaces the fake. Include at least one cost you accepted.

## What you did not finish

If the timebox ran out, which decisions above are in the code and which are only in this document.
