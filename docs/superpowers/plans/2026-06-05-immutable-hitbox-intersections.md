# Immutable Hitbox Intersections Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `NHitBoxD` immutable and add fast exact intersection paths for axis-aligned hitboxes and primitive axis-aligned cell rectangles.

**Architecture:** `NHitBoxD` becomes a computed placed-hitbox value object. Callers keep constructing hitboxes as before, while raster loops avoid temporary cell hitbox objects by using primitive rectangle intersection helpers. `intersects()` uses AABB math when both hitboxes are axis-aligned and SAT otherwise.

**Tech Stack:** Java 8, Ant, plain Java main-method regression tests.

---

### Task 1: Add RED Tests

**Files:**
- Modify: `test/nurgling/pf/NHitBoxDIntersectionTest.java`

- [x] **Step 1: Add tests proving the desired API and immutability contract**

Add tests for `intersectsAxisAlignedRect(...)`, AABB fast-path parity, and construction-only transforms.

- [x] **Step 2: Run tests to verify RED**

Run: `javac -cp "build/classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" -d "build/test-classes" "test/nurgling/pf/NHitBoxDIntersectionTest.java" && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.pf.NHitBoxDIntersectionTest`

Expected: compile failure because `intersectsAxisAlignedRect` does not exist.

### Task 2: Refactor `NHitBoxD` To Construction-Time State

**Files:**
- Modify: `src/nurgling/pf/NHitBoxD.java`

- [x] **Step 1: Replace mutator methods with private construction helpers**

Make transform methods private/static or constructor-only. Keep public constructors compatible with current call sites.

- [x] **Step 2: Add primitive rectangle intersection helper**

Add `public boolean intersectsAxisAlignedRect(double minX, double minY, double maxX, double maxY, boolean includeBorder)`.

- [x] **Step 3: Add AABB fast path**

In `intersects(NHitBoxD other, boolean includeBorder)`, if both hitboxes are axis-aligned, use exact AABB overlap; otherwise use SAT.

- [x] **Step 4: Run tests to verify GREEN**

Run the command from Task 1. Expected: PASS.

### Task 3: Remove Raster Loop Temporary Hitbox Allocation

**Files:**
- Modify: `src/nurgling/pf/CellsArray.java`
- Modify: `src/nurgling/navigation/ChunkNavRecorder.java`

- [x] **Step 1: Update local PF rasterization**

Change `CellsArray.overlapsPfCell(...)` to call `hitBox.intersectsAxisAlignedRect(ul.x, ul.y, br.x, br.y, true)`.

- [x] **Step 2: Update chunk nav rasterization**

Change `ChunkNavRecorder.cellOverlapsHitBox(...)` to compute primitive cell bounds and call `hitBox.intersectsAxisAlignedRect(minX, minY, maxX, maxY, true)`.

- [x] **Step 3: Run raster tests**

Run all three geometry/raster tests. Expected: PASS.

### Task 4: Verify And Commit

**Files:**
- All modified files above.

- [x] **Step 1: Run full verification**

Run: `ant jar && javac -cp "build/classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" -d "build/test-classes" "test/nurgling/pf/CellsArrayRasterizationTest.java" "test/nurgling/pf/NHitBoxDIntersectionTest.java" "test/nurgling/navigation/ChunkNavRecorderRasterizationTest.java" && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.pf.CellsArrayRasterizationTest && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.pf.NHitBoxDIntersectionTest && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.navigation.ChunkNavRecorderRasterizationTest`

Expected: `BUILD SUCCESSFUL`, all tests exit 0.

- [ ] **Step 2: Commit**

Run: `git add src/nurgling/pf/NHitBoxD.java src/nurgling/pf/CellsArray.java src/nurgling/navigation/ChunkNavRecorder.java test/nurgling/pf/NHitBoxDIntersectionTest.java docs/superpowers/plans/2026-06-05-immutable-hitbox-intersections.md && git commit -m "refactor: make hitbox intersections immutable"`

Expected: commit succeeds.
