# Cheese Mixed-Stage Start Work Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let cheese orders create newly added curd trays even when later stages of the same order are still active.

**Architecture:** Keep the existing rack/buffer movement pass for non-start stages. Change only the curd creation pass so it selects a pending `start` step directly instead of the first pending step in the order status list.

**Tech Stack:** Java 8, Ant build, repository-local Java `main` regression tests.

---

### Task 1: Start-Step Selection

**Files:**
- Modify: `src/nurgling/actions/ProcessCheeseOrderInBatches.java`
- Create: `test/nurgling/actions/ProcessCheeseOrderInBatchesStartStepTest.java`

- [x] **Step 1: Write the failing regression test**

Create a source-level regression test that requires `ProcessCheeseOrderInBatches` to select `start` work directly and avoid calling the old first-pending-step helper.

- [x] **Step 2: Run test to verify it fails**

Run: `javac -d build/test-classes test/nurgling/actions/ProcessCheeseOrderInBatchesStartStepTest.java && java -cp build/test-classes nurgling.actions.ProcessCheeseOrderInBatchesStartStepTest`

Expected: FAIL because `getStartStep` does not exist yet.

- [x] **Step 3: Write minimal implementation**

Replace `getCurrentStep(order)` with `getStartStep(order)`, and make `getStartStep` return only steps where `place` is `start` and `left > 0`.

- [x] **Step 4: Run test to verify it passes**

Run: `javac -d build/test-classes test/nurgling/actions/ProcessCheeseOrderInBatchesStartStepTest.java && java -cp build/test-classes nurgling.actions.ProcessCheeseOrderInBatchesStartStepTest`

Expected: PASS.

- [x] **Step 5: Build project**

Run: `ant hafen-client`

Expected: build succeeds with no compile errors from changed cheese files.
