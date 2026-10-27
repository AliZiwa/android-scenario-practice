# android-scenario-practice

Interview preparation for Senior and Staff Android roles, run as graded engineering assignments: one every two days, each a standalone Android project in its own folder, submitted as a pull request and reviewed like production code.

## How it works

1. A new folder `interview-prep-<nn>-<area>` lands on `main` with base code, a `README.md` brief, a timebox, and references.
2. Ali works on a branch named `<nn>-<area>` and opens a PR into `main`.
3. The PR is reviewed line by line. `SCORE.md` (rubric, 1 to 5 per dimension, 20 to pass) and `INTERVIEW.md` (the questions a Staff interviewer would ask about that exact code) are pushed to the branch.
4. The PR is merged. The next assignment follows.

## Rules

- No AI-generated or AI-suggested code. Documentation is open.
- Commit as you go; history is reviewed.
- A clean partial beats a rushed complete.

## Rubric

| Dimension | Weight |
|---|---|
| Correctness | 1 to 5 |
| Architecture and boundaries | 1 to 5 |
| Code quality and idiom | 1 to 5 |
| Tests | 1 to 5 |
| Communication (README, ADR, commits) | 1 to 5 |

## Assignments

| # | Area | Folder | Status |
|---|---|---|---|
| 01 | Debugging | [`interview-prep-01-debugging`](interview-prep-01-debugging) | Passed, 21/25 (4 rounds) |
| 02 | Enhancement | [`interview-prep-02-enhancement`](interview-prep-02-enhancement) | Passed, 21/25 (2 rounds) |
| 03 | Code smells | [`interview-prep-03-code-smells`](interview-prep-03-code-smells) | Passed, 20/25 (4 rounds) |
| 04 | Architecture | | |
| 05 | AI integration | | |
| 06 | Testing | | |
| 07 | Performance | | |
| 08 | Concurrency | | |
| 09 | Real-time (WebSocket) | | |
| 10 | GraphQL | | |
| 11 | Code review | | |
| 12 | Take-home simulation | | |

## Toolchain

Gradle 9.6.0 (wrapper), AGP 9.4.1, Kotlin 2.4.20 (built into AGP 9), Compose BOM 2026.09.00, JDK 17. Each assignment folder is its own Gradle project; open the folder, not the repo root, in Android Studio.
