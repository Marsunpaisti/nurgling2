# Dense Local PF Grid Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Change local pathfinder sampling so `5.5x5.5` PF cells are centered every `2.75` world units, and correct cupboard hitboxes to `10x10`.

**Architecture:** Add explicit PF grid constants in `nurgling.pf.Utils` so world/grid conversion no longer directly uses `MCache.tilehsz`. Update local map sizing, obstacle raster bounds, and terrain corner checks to use the new grid step while keeping cell half-size at `MCache.tileqsz`.

**Tech Stack:** Java 8, Ant build, standalone `main()` regression tests.

---

### Task 1: Add Failing Tests

**Files:**
- Modify: `test/nurgling/pf/CellsArrayRasterizationTest.java`

- [ ] **Step 1: Add tests for cupboard size and PF grid spacing**

Add assertions for `gfx/terobjs/cupboard` bounds `[-5,-5]..[5,5]` and `Utils.pfGridToWorld(new Coord(1, 1)) == (2.75, 2.75)`.

- [ ] **Step 2: Verify red**

Run:

```bash
javac -cp "build/classes;etc/json-java.jar" -d build/test-classes test/nurgling/pf/CellsArrayRasterizationTest.java && java -cp "build/classes;build/test-classes;etc/json-java.jar" nurgling.pf.CellsArrayRasterizationTest
```

Expected: FAIL because current cupboard is `11x11` and PF step is `5.5`.

### Task 2: Implement Dense Grid

**Files:**
- Modify: `src/nurgling/NHitBox.java`
- Modify: `src/nurgling/pf/Utils.java`
- Modify: `src/nurgling/pf/CellsArray.java`
- Modify: `src/nurgling/pf/NPFMap.java`

- [ ] **Step 1: Add local PF constants and conversion helpers**

Use `MCache.tileqsz` as `GRID_STEP` and `MCache.tileqsz` as `CELL_HALFSZ`.

- [ ] **Step 2: Change cupboard custom hitbox**

Set `gfx/terobjs/cupboard` to `new NHitBox(new Coord2d(-5,-5), new Coord2d(5,5), true)`.

- [ ] **Step 3: Update local PF users**

Replace `MCache.tilehsz` in local PF map sizing/raster bounds with `Utils.GRID_STEP`, and keep cell square extents based on `Utils.CELL_HALFSZ`.

- [ ] **Step 4: Verify green**

Run:

```bash
javac -cp "build/classes;etc/json-java.jar" -d build/classes src/nurgling/NHitBox.java src/nurgling/pf/Utils.java src/nurgling/pf/CellsArray.java src/nurgling/pf/NPFMap.java && javac -cp "build/classes;etc/json-java.jar" -d build/test-classes test/nurgling/pf/CellsArrayRasterizationTest.java && java -cp "build/classes;build/test-classes;etc/json-java.jar" nurgling.pf.CellsArrayRasterizationTest
```

Expected: PASS with no output.

### Task 3: Compile And Commit

**Files:**
- Commit all files listed above plus this plan.

- [ ] **Step 1: Compile full client**

Run:

```bash
ant hafen-client
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Commit**

Run:

```bash
git add src/nurgling/NHitBox.java src/nurgling/pf/Utils.java src/nurgling/pf/CellsArray.java src/nurgling/pf/NPFMap.java test/nurgling/pf/CellsArrayRasterizationTest.java docs/superpowers/plans/2026-06-05-dense-local-pf-grid.md
git commit -m "fix: densify local pathfinder grid"
```
