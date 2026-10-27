# ADR 001: PartsBin application architecture

Status: Proposed · Date: · Author:

Keep this to about a page. Write it before the code, and update it when the code teaches you something. Where this document and the code disagree at review time, the document loses.

## Context

What is wrong with the prototype, in your words, and which of those problems block the roadmap (sync queue, barcode scanning, second role, tablet layout). Not all ten matter equally; say which three matter most and why.

## Decision

### Layers and the dependency rule

Which packages exist, what each may depend on, and what enforces it. A diagram is welcome:

```
ui  -->  domain  <--  data
         ^
         |
   (nothing below domain knows about Android)
```

### Where logic lives

For each of these, name the class: low stock rule; adjustment validation; DTO to model mapping; entity to model mapping; date formatting; "offline, showing cached" decision.

### Source of truth and sync

What the UI observes, what a refresh does, what an adjustment does to Room before and after the network answers, and what happens on each failure.

### State and events

How UI state is produced (one `StateFlow` per screen? built how?), how one-shot messages reach the screen, and how the detail screen learns about a change made elsewhere.

### Errors

The types that cross the repository boundary, and how each maps to a message.

### Wiring

Where the composition root is, what it owns, how a ViewModel gets its dependencies, how tests get theirs.

### Navigation

How the selected part id reaches the detail screen, and where the Offline switch state lives now that `Globals` is gone.

## Alternatives considered

At least three, with one or two lines each on why not here. Candidates: Hilt; a use-case layer; network as source of truth with Room as cache; a multi-module Gradle split now; Navigation Compose versus the hand-rolled switch.

## Consequences

What gets easier, what gets harder, and what you would want to revisit before the sync queue lands. Include at least one cost you accepted.

## What you did not finish

If the timebox ran out, which decisions above are in the code and which are only in this document.
