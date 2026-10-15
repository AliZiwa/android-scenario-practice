# Design notes: status changes from the phone

Keep this under a page. Short answers with a reason beat long answers without one.

## 1. Where does the pending change live?

A nullable column on `work_orders`, a separate table, something else? What does each option cost when a second kind of edit (say, a technician note) arrives next quarter? Why did you pick yours?

## 2. The process dies between the Room write and the PATCH

What does the technician see on relaunch? What does the server believe? What makes the change get sent, and when?

## 3. The PATCH reached the server but the response never came back

The device sees `IOException`; the server has applied the change and bumped `version`. What happens on the retry, and is the outcome acceptable? If not, what would you change in the client, and what would you ask the backend team for?

## 4. What you did not build

Did you use WorkManager, a `Mutex`, a retry queue, exponential backoff? For each one you left out, one sentence on why it was the right call for this scope and what would make you add it.

## What is not done

If you ran out of time, list what is missing and what you would do first with another hour.
