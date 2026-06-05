# Local PF Six-Unit Cells Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Change local pathfinder graph geometry to use a periodic `0, 3, 5.5, 8, 11` per-tile coordinate mapping and `6x6` player collision cells.

**Architecture:** Centralize non-uniform PF index/world conversion in `nurgling.pf.Utils`, then make rasterization and local PF consumers use those helpers instead of uniform `2.75` math. Keep `NHitBoxD` world-space obstacle geometry unchanged; only local PF candidate cell centers and player-footprint cell size change.

**Tech Stack:** Java, Ant build, simple `main`-based regression tests under `test/nurgling/...`.

---

## File Structure

- Modify `src/nurgling/pf/Utils.java`: own local PF coordinate conversion, cell half-size, and helper methods for non-uniform grid bounds.
- Modify `test/nurgling/pf/CellsArrayRasterizationTest.java`: add RED/GREEN coverage for the new PF mapping, `6x6` cells, and grid-aligned sliver behavior.
- Modify `src/nurgling/pf/CellsArray.java`: compute candidate cell ranges through non-uniform bounds and test `6x6` cells with strict overlap.
- Modify `src/nurgling/pf/NHitBoxD.java`: update PF-grid primitive constructors to use `Utils.CELL_HALFSZ` instead of `MCache.tileqsz`.
- Modify `src/nurgling/pf/NPFMap.java`: replace `Utils.GRID_STEP` sizing and old half-cell terrain sampling with helper-based equivalents.
- Modify `src/nurgling/actions/PathFinder.java`: replace old `MCache.tileqsz` local PF proximity windows with `Utils.CELL_HALFSZ`.
- Check only `src/nurgling/actions/DynamicPf.java` and `src/nurgling/pf/Graph.java`: they already convert through `Utils.pfGridToWorld(...)` and `Utils.toPfGrid(...)`, so they are expected to compile after helper changes without direct edits unless tests reveal a call-site assumption.

---

### Task 1: Add RED Tests For Non-Uniform Local PF Grid

**Files:**
- Modify: `test/nurgling/pf/CellsArrayRasterizationTest.java`

- [ ] **Step 1: Add failing tests for PF axis mapping and cell size**

Edit `test/nurgling/pf/CellsArrayRasterizationTest.java`.

Replace the `main` method with:

```java
public static void main(String[] args) {
    pfCellsUsePeriodicSixUnitPlayerGrid();
    worldCoordinatesSnapToNearestPeriodicPfCell();
    pfCellsUseSixUnitPlayerFootprint();
    cupboardUsesTenByTenHitbox();
    asymmetricHitboxesUseMeasuredLocalOffset();
    herbalistTableUsesMeasuredHitbox();
    includesCellsTouchedOnlyByOverlapAtTheEdge();
    keepsRotatedThinHitboxesRepresented();
}
```

Replace `pfCellsAreCenteredEveryQuarterTile()` with these three methods:

```java
private static void pfCellsUsePeriodicSixUnitPlayerGrid() {
    assertClose(Utils.pfGridToWorld(new Coord(-4, 0)).x, -11.0, "PF grid -4 x");
    assertClose(Utils.pfGridToWorld(new Coord(-3, 0)).x, -8.0, "PF grid -3 x");
    assertClose(Utils.pfGridToWorld(new Coord(-2, 0)).x, -5.5, "PF grid -2 x");
    assertClose(Utils.pfGridToWorld(new Coord(-1, 0)).x, -3.0, "PF grid -1 x");
    assertClose(Utils.pfGridToWorld(new Coord(0, 0)).x, 0.0, "PF grid 0 x");
    assertClose(Utils.pfGridToWorld(new Coord(1, 1)).x, 3.0, "PF grid 1 x");
    assertClose(Utils.pfGridToWorld(new Coord(2, 2)).x, 5.5, "PF grid 2 x");
    assertClose(Utils.pfGridToWorld(new Coord(3, 3)).x, 8.0, "PF grid 3 x");
    assertClose(Utils.pfGridToWorld(new Coord(4, 4)).x, 11.0, "PF grid 4 x");
    assertClose(Utils.pfGridToWorld(new Coord(5, 5)).x, 14.0, "PF grid 5 x");
    assertClose(Utils.pfGridToWorld(new Coord(6, 6)).x, 16.5, "PF grid 6 x");
    assertClose(Utils.pfGridToWorld(new Coord(7, 7)).x, 19.0, "PF grid 7 x");
    assertClose(Utils.pfGridToWorld(new Coord(8, 8)).x, 22.0, "PF grid 8 x");
}

private static void worldCoordinatesSnapToNearestPeriodicPfCell() {
    assertCoord(Utils.toPfGrid(new Coord2d(0.0, 0.0)), 0, 0, "snap exact origin");
    assertCoord(Utils.toPfGrid(new Coord2d(2.9, 3.1)), 1, 1, "snap near offset 3");
    assertCoord(Utils.toPfGrid(new Coord2d(5.6, 5.4)), 2, 2, "snap near tile center");
    assertCoord(Utils.toPfGrid(new Coord2d(8.1, 7.9)), 3, 3, "snap near offset 8");
    assertCoord(Utils.toPfGrid(new Coord2d(10.9, 11.1)), 4, 4, "snap near next tile edge");
    assertCoord(Utils.toPfGrid(new Coord2d(-2.9, -3.1)), -1, -1, "snap negative offset -3");
    assertCoord(Utils.toPfGrid(new Coord2d(-5.4, -5.6)), -2, -2, "snap negative center");
}

private static void pfCellsUseSixUnitPlayerFootprint() {
    assertClose(Utils.CELL_HALFSZ.x, 3.0, "PF cell half width");
    assertClose(Utils.CELL_HALFSZ.y, 3.0, "PF cell half height");
}
```

Add this helper before `assertClose(...)`:

```java
private static void assertCoord(Coord actual, int expectedX, int expectedY, String label) {
    if (actual.x != expectedX || actual.y != expectedY) {
        throw new AssertionError(label + ": expected (" + expectedX + ", " + expectedY + "), got " + actual);
    }
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```bash
javac -cp "build/classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" -d "build/test-classes" "test/nurgling/pf/CellsArrayRasterizationTest.java" && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.pf.CellsArrayRasterizationTest
```

Expected: FAIL because `Utils.pfGridToWorld(new Coord(1, 1)).x` is currently `2.75`, not `3.0`, or because `Utils.CELL_HALFSZ.x` is currently `2.75`, not `3.0`.

- [ ] **Step 3: Commit RED tests**

Run:

```bash
git add "test/nurgling/pf/CellsArrayRasterizationTest.java"
git commit -m "test: cover local pf six unit grid mapping"
```

---

### Task 2: Implement Non-Uniform PF Coordinate Helpers

**Files:**
- Modify: `src/nurgling/pf/Utils.java`
- Test: `test/nurgling/pf/CellsArrayRasterizationTest.java`

- [ ] **Step 1: Replace PF conversion helpers**

Edit `src/nurgling/pf/Utils.java`.

Replace lines 8-19 with:

```java
    public static final Coord2d CELL_HALFSZ = Coord2d.of(3.0, 3.0);
    public static final int PF_STEPS_PER_TILE = 4;

    private static final double[] PF_TILE_OFFSETS = {0.0, 3.0, 5.5, 8.0};
    private static final double MIN_PF_STEP = 2.5;
    private static final double EPS = 0.000001;

    public static Coord toPfGrid(Coord2d coord)
    {
        return new Coord(worldAxisToPf(coord.x), worldAxisToPf(coord.y));
    }

    public static Coord2d pfGridToWorld(Coord coord)
    {
        return Coord2d.of(pfAxisToWorld(coord.x), pfAxisToWorld(coord.y));
    }

    public static double pfAxisToWorld(int index) {
        int tile = Math.floorDiv(index, PF_STEPS_PER_TILE);
        int offset = Math.floorMod(index, PF_STEPS_PER_TILE);
        return tile * MCache.tilesz.x + PF_TILE_OFFSETS[offset];
    }

    public static int worldAxisToPf(double coord) {
        int tile = (int) Math.floor(coord / MCache.tilesz.x);
        int best = tile * PF_STEPS_PER_TILE;
        double bestDist = Double.MAX_VALUE;

        for (int candidateTile = tile - 1; candidateTile <= tile + 1; candidateTile++) {
            for (int offset = 0; offset < PF_STEPS_PER_TILE; offset++) {
                int candidate = candidateTile * PF_STEPS_PER_TILE + offset;
                double dist = Math.abs(pfAxisToWorld(candidate) - coord);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = candidate;
                }
            }
        }

        return best;
    }

    public static int firstPfAxisAtOrAfter(double coord) {
        int index = worldAxisToPf(coord);
        while (pfAxisToWorld(index) < coord - EPS) {
            index++;
        }
        while (pfAxisToWorld(index - 1) >= coord - EPS) {
            index--;
        }
        return index;
    }

    public static int lastPfAxisAtOrBefore(double coord) {
        int index = worldAxisToPf(coord);
        while (pfAxisToWorld(index) > coord + EPS) {
            index--;
        }
        while (pfAxisToWorld(index + 1) <= coord + EPS) {
            index++;
        }
        return index;
    }

    public static int maxPfStepsForWorldDistance(double distance) {
        return (int) Math.ceil(Math.abs(distance) / MIN_PF_STEP);
    }
```

- [ ] **Step 2: Run tests and verify GREEN for mapping**

Run:

```bash
javac -cp "build/classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" -d "build/test-classes" "test/nurgling/pf/CellsArrayRasterizationTest.java" && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.pf.CellsArrayRasterizationTest
```

Expected: the new mapping assertions pass. Existing rasterization tests may still fail until `CellsArray` no longer divides by removed `GRID_STEP`. If compilation fails because `Utils.GRID_STEP` no longer exists, continue to Task 3 and Task 4 before committing.

- [ ] **Step 3: Commit if compilation and tests pass now**

If the command in Step 2 passes, run:

```bash
git add "src/nurgling/pf/Utils.java"
git commit -m "feat: add non-uniform local pf grid mapping"
```

If Step 2 fails only because production code still references `Utils.GRID_STEP`, do not commit yet; include `Utils.java` in the next task commit.

---

### Task 3: Update CellsArray Rasterization For 6x6 Non-Uniform Cells

**Files:**
- Modify: `test/nurgling/pf/CellsArrayRasterizationTest.java`
- Modify: `src/nurgling/pf/CellsArray.java`
- Carry forward if uncommitted from previous task: `src/nurgling/pf/Utils.java`

- [ ] **Step 1: Add RED test for grid-aligned sliver behavior**

Edit `test/nurgling/pf/CellsArrayRasterizationTest.java`.

Add this call to `main`, after `pfCellsUseSixUnitPlayerFootprint();`:

```java
    sixUnitCellsAvoidNeighborTileSliverBleed();
```

Add this method after `pfCellsUseSixUnitPlayerFootprint()`:

```java
private static void sixUnitCellsAvoidNeighborTileSliverBleed() {
    NHitBox hitBox = new NHitBox(new Coord2d(11.01, -3.0), new Coord2d(17.0, 3.0), true);
    CellsArray cells = new CellsArray(hitBox, 0, Coord2d.of(0.0, 0.0));

    if (hasBlockedCellAt(cells, 3, 0)) {
        throw new AssertionError("PF cell centered at x=8 must not be blocked by a tiny neighboring-tile sliver");
    }
    if (!hasBlockedCellAt(cells, 4, 0)) {
        throw new AssertionError("PF cell centered at x=11 must still detect the neighboring obstacle");
    }
}
```

- [ ] **Step 2: Run test and verify RED**

Run:

```bash
javac -cp "build/classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" -d "build/test-classes" "test/nurgling/pf/CellsArrayRasterizationTest.java" && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.pf.CellsArrayRasterizationTest
```

Expected before implementation: FAIL because candidate range logic still assumes uniform `GRID_STEP`, or because old `2.75/8.25` centers still bleed into the neighboring obstacle.

- [ ] **Step 3: Replace CellsArray candidate range and strict overlap**

Edit `src/nurgling/pf/CellsArray.java`.

Replace lines 29-32 with:

```java
        begin = new Coord(Utils.firstPfAxisAtOrAfter(ul.x - Utils.CELL_HALFSZ.x),
                Utils.firstPfAxisAtOrAfter(ul.y - Utils.CELL_HALFSZ.y));
        end = new Coord(Utils.lastPfAxisAtOrBefore(br.x + Utils.CELL_HALFSZ.x),
                Utils.lastPfAxisAtOrBefore(br.y + Utils.CELL_HALFSZ.y));
```

Replace line 47 with:

```java
        return hitBox.intersectsAxisAlignedRect(ul.x, ul.y, br.x, br.y, false);
```

Reason: a `6x6` player cell centered at offset `3` or `8` exactly touches tile boundaries. Shared borders must not count as overlap for local PF player-footprint checks, or cells that just fit inside a tile will still be marked blocked by grid-aligned neighbors.

- [ ] **Step 4: Run tests and verify GREEN for CellsArray**

Run:

```bash
javac -cp "build/classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" -d "build/test-classes" "test/nurgling/pf/CellsArrayRasterizationTest.java" && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.pf.CellsArrayRasterizationTest
```

Expected: PASS for `CellsArrayRasterizationTest`.

- [ ] **Step 5: Commit**

Run:

```bash
git add "src/nurgling/pf/Utils.java" "src/nurgling/pf/CellsArray.java" "test/nurgling/pf/CellsArrayRasterizationTest.java"
git commit -m "fix: use six unit local pf cells"
```

---

### Task 4: Remove Remaining Uniform-GRID_STEP Assumptions In Local PF

**Files:**
- Modify: `src/nurgling/pf/NHitBoxD.java`
- Modify: `src/nurgling/pf/NPFMap.java`
- Modify: `src/nurgling/actions/PathFinder.java`

- [ ] **Step 1: Compile to expose remaining `GRID_STEP` usage**

Run:

```bash
ant jar
```

Expected before implementation: FAIL if any code still references `Utils.GRID_STEP`. If it passes, still perform the replacements below because some old `MCache.tileqsz` usages are semantic half-cell assumptions.

- [ ] **Step 2: Update PF primitive hitbox constructors**

Edit `src/nurgling/pf/NHitBoxD.java`.

Replace line 29 with:

```java
        this(Utils.pfGridToWorld(rc).sub(Utils.CELL_HALFSZ), Utils.pfGridToWorld(rc).add(Utils.CELL_HALFSZ), null, 0, true);
```

Replace line 43 with:

```java
        this(Utils.pfGridToWorld(ul).sub(Utils.CELL_HALFSZ), Utils.pfGridToWorld(br).add(Utils.CELL_HALFSZ));
```

- [ ] **Step 3: Update NPFMap sizing and terrain sampling**

Edit `src/nurgling/pf/NPFMap.java`.

Replace line 218 with:

```java
        dsize = Math.max(16, Utils.maxPfStepsForWorldDistance(b.dist(a)) * mul);
```

Replace lines 302-305 with:

```java
                    Coord2d world = Utils.pfGridToWorld(cells[i][j].pos);
                    cand.add(world.add(-Utils.CELL_HALFSZ.x, Utils.CELL_HALFSZ.y).div(MCache.tilesz).floor());
                    cand.add(world.add(Utils.CELL_HALFSZ.x, -Utils.CELL_HALFSZ.y).div(MCache.tilesz).floor());
                    cand.add(world.add(-Utils.CELL_HALFSZ.x, -Utils.CELL_HALFSZ.y).div(MCache.tilesz).floor());
                    cand.add(world.add(Utils.CELL_HALFSZ.x, Utils.CELL_HALFSZ.y).div(MCache.tilesz).floor());
```

- [ ] **Step 4: Update PathFinder local half-cell checks**

Edit `src/nurgling/actions/PathFinder.java`.

Replace lines 283-284 with:

```java
                            if (coord2d.x + Utils.CELL_HALFSZ.x > tcoord.x && coord2d.x - Utils.CELL_HALFSZ.x < tcoord.x ||
                                    coord2d.y + Utils.CELL_HALFSZ.y > tcoord.y && coord2d.y - Utils.CELL_HALFSZ.y < tcoord.y)
```

- [ ] **Step 5: Search for stale assumptions**

Run:

```bash
git grep -n "GRID_STEP\|MCache\.tileqsz" -- src/nurgling/pf src/nurgling/actions/PathFinder.java src/nurgling/actions/DynamicPf.java
```

Expected: no `GRID_STEP` matches and no `MCache.tileqsz` matches in local PF cell-sizing code.

If `MCache.tileqsz` remains in `NHitBoxD`, `CellsArray`, `NPFMap`, or `PathFinder` for local PF cell sizing, replace it with `Utils.CELL_HALFSZ` or helper-based conversion before continuing.

- [ ] **Step 6: Build and run focused PF tests**

Run:

```bash
ant jar && javac -cp "build/classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" -d "build/test-classes" "test/nurgling/pf/CellsArrayRasterizationTest.java" "test/nurgling/pf/NHitBoxDIntersectionTest.java" && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.pf.CellsArrayRasterizationTest && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.pf.NHitBoxDIntersectionTest
```

Expected: `BUILD SUCCESSFUL`; both Java test processes exit with code `0`. Existing resource decode noise from `LayerUtil` is acceptable if Ant exits successfully.

- [ ] **Step 7: Commit**

Run:

```bash
git add "src/nurgling/pf/NHitBoxD.java" "src/nurgling/pf/NPFMap.java" "src/nurgling/actions/PathFinder.java"
git commit -m "fix: remove local pf uniform grid assumptions"
```

---

### Task 5: Full Regression Verification

**Files:**
- Test only; no source edits expected.

- [ ] **Step 1: Run full target verification**

Run:

```bash
ant jar && javac -cp "build/classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" -d "build/test-classes" "test/nurgling/overlays/NModelBoxHitBoxTest.java" "test/nurgling/pf/CellsArrayRasterizationTest.java" "test/nurgling/pf/NHitBoxDIntersectionTest.java" "test/nurgling/navigation/ChunkNavRecorderRasterizationTest.java" && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.overlays.NModelBoxHitBoxTest && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.pf.CellsArrayRasterizationTest && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.pf.NHitBoxDIntersectionTest && java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.navigation.ChunkNavRecorderRasterizationTest
```

Expected: `BUILD SUCCESSFUL`; all Java test processes exit with code `0`.

- [ ] **Step 2: Inspect final diff and status**

Run:

```bash
git status --short
git log --oneline -5
```

Expected: only intended source/test changes are uncommitted if Task 5 discovers small fixes. If there are no uncommitted changes, status is clean.

- [ ] **Step 3: Commit any verification-only fixes**

If Task 5 required changes, run:

```bash
git add "src/nurgling/pf/Utils.java" "src/nurgling/pf/CellsArray.java" "src/nurgling/pf/NHitBoxD.java" "src/nurgling/pf/NPFMap.java" "src/nurgling/actions/PathFinder.java" "test/nurgling/pf/CellsArrayRasterizationTest.java"
git commit -m "fix: stabilize six unit local pf cells"
```

If Task 5 required no changes, do not create an empty commit.

---

## Final Handoff

After all tasks pass, report:

- Branch name and latest commit hash.
- Full verification command used and whether it exited successfully.
- Whether `git status --short` is clean.
- Whether `LayerUtil` printed known resource decode noise during `ant jar`.

Do not merge to `master` until the user asks or the execution workflow explicitly reaches the merge step.
