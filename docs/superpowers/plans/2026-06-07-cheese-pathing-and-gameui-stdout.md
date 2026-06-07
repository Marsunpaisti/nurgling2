# Cheese Pathing Diagnostics And GameUI Stdout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make cheese rack scan pathing failures explicit and mirror client-generated `GameUI.msg/error` messages to stdout/stderr.

**Architecture:** `ClearRacksAndRecordCapacity` records navigation failures as scan diagnostics instead of collapsing them into `racks=0`. `GameUI` mirrors its local/system messages at the central `msg(String, Color)` and `error(String)` methods without touching `ChatUI` channel messages.

**Tech Stack:** Java 8, Ant build, repository-local Java `main` regression tests.

---

### Task 1: Pathing Diagnostics And Stdout Mirror

**Files:**
- Modify: `src/nurgling/actions/ClearRacksAndRecordCapacity.java`
- Modify: `src/haven/GameUI.java`
- Create: `test/nurgling/actions/CheeseRackNavigationFailureDiagnosticsTest.java`
- Create: `test/haven/GameUIStdoutMirrorTest.java`

- [x] **Step 1: Write failing regression tests**

Create source-level tests requiring `navigationFailed`, `failedAreas`, user-facing path failure logs, and stdout/stderr mirroring prefixes.

- [x] **Step 2: Run tests to verify they fail**

Run: `javac -d build/test-classes test/nurgling/actions/CheeseRackNavigationFailureDiagnosticsTest.java test/haven/GameUIStdoutMirrorTest.java && java -cp build/test-classes nurgling.actions.CheeseRackNavigationFailureDiagnosticsTest && java -cp build/test-classes haven.GameUIStdoutMirrorTest`

Expected: FAIL because the diagnostics and mirroring are not implemented yet.

- [x] **Step 3: Implement minimal changes**

Check the boolean result from `context.goToAreaById(area.id)`. On failure, log and return scan diagnostics with `navigationFailed=1` and `failedAreas=[name#id]`. Add `System.out.println("[GameUI.msg] " + msg)` and `System.err.println("[GameUI.error] " + msg)` in `GameUI`.

- [x] **Step 4: Run tests and build**

Run focused tests and `ant hafen-client`.
