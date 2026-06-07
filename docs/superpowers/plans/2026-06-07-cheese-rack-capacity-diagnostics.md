# Cheese Rack Capacity Diagnostics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Explain why cheese curd creation reports no rack space by logging the rack capacity scan details and including them in the no-space message.

**Architecture:** Keep capacity calculation in `ClearRacksAndRecordCapacity`, but capture a per-place diagnostic string while scanning. Pass those diagnostics through `CheeseProductionBot` into `ProcessCheeseOrderInBatches`, where no-space messages include both the current capacity and scan details.

**Tech Stack:** Java 8, Ant build, repository-local Java `main` regression tests.

---

### Task 1: Capacity Diagnostic Logging

**Files:**
- Modify: `src/nurgling/actions/ClearRacksAndRecordCapacity.java`
- Modify: `src/nurgling/actions/ProcessCheeseOrderInBatches.java`
- Modify: `src/nurgling/actions/bots/CheeseProductionBot.java`
- Create: `test/nurgling/actions/CheeseRackCapacityDiagnosticsTest.java`

- [x] **Step 1: Write failing regression test**

Create `CheeseRackCapacityDiagnosticsTest` to require capacity diagnostics to be recorded, passed through, and included in no-space messages.

- [x] **Step 2: Run test to verify it fails**

Run: `javac -d build/test-classes test/nurgling/actions/CheeseRackCapacityDiagnosticsTest.java && java -cp build/test-classes nurgling.actions.CheeseRackCapacityDiagnosticsTest`

Expected: FAIL because the diagnostic methods and message details do not exist yet.

- [x] **Step 3: Implement minimal diagnostics**

Add per-place summary fields: `areas`, `racks`, `empty`, `partial`, `full`, `recordedCapacity`, `movedToArea`, and `availableNow`.

- [x] **Step 4: Run test and build**

Run the regression test and `ant hafen-client`.
