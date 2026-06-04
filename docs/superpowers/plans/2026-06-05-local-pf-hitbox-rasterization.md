# Local PF Hitbox Rasterization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make local `PathFinder` mark obstacle cells by true overlap against rotated gob hitboxes instead of rounded extent snapping.

**Architecture:** Keep the change local to `CellsArray`, which is the collision-cell projection used by `NPFMap.addGob()` and `PathFinder.findFreeNearByHB()`. Replace rounded begin/end bounds with floor/ceil bounds and add direct half-cell AABB overlap for orthogonal PF cells against rotated hitbox polygons.

**Tech Stack:** Java 8, Ant build, existing `main()`-style regression tests under `test/`.

---

### Task 0: Build Baseline Classes

**Files:**
- Modify: none
- Test: Ant compile target

- [ ] **Step 1: Compile baseline classes for test classpath**

Run:

```bash
ant hafen-client
```

Expected: `BUILD SUCCESSFUL` and `build/classes` exists for standalone regression-test compilation.

### Task 1: Add Regression Test For Rotated Thin Hitboxes

**Files:**
- Create: `test/nurgling/pf/CellsArrayRasterizationTest.java`
- Modify: none
- Test: `test/nurgling/pf/CellsArrayRasterizationTest.java`

- [ ] **Step 1: Write the failing test**

Create `test/nurgling/pf/CellsArrayRasterizationTest.java`:

```java
package nurgling.pf;

import haven.Coord2d;
import nurgling.NHitBox;

public class CellsArrayRasterizationTest {
    public static void main(String[] args) {
        includesCellsTouchedOnlyByOverlapAtTheEdge();
        keepsRotatedThinHitboxesRepresented();
    }

    private static void includesCellsTouchedOnlyByOverlapAtTheEdge() {
        NHitBox hitBox = new NHitBox(new Coord2d(-2.0, -2.0), new Coord2d(2.0, 2.0), true);
        CellsArray cells = new CellsArray(hitBox, 0, new Coord2d(2.8, 0.0));

        if (!hasBlockedCellAt(cells, 0, 0)) {
            throw new AssertionError("CellsArray must include the half-cell overlapped by an off-grid hitbox edge");
        }
    }

    private static void keepsRotatedThinHitboxesRepresented() {
        NHitBox hitBox = new NHitBox(new Coord2d(-3.465, -5.945), new Coord2d(3.465, 5.945), true);
        CellsArray cells = new CellsArray(hitBox, Math.PI / 4.0, new Coord2d(0.0, 0.0));

        int blocked = countBlocked(cells);
        if (blocked == 0) {
            throw new AssertionError("CellsArray must mark cells for rotated herbalist-table-sized hitboxes");
        }
    }

    private static boolean hasBlockedCellAt(CellsArray cells, int pfX, int pfY) {
        int x = pfX - cells.begin.x;
        int y = pfY - cells.begin.y;
        return x >= 0 && y >= 0 && x < cells.x_len && y < cells.y_len && cells.cells[x][y] != 0;
    }

    private static int countBlocked(CellsArray cells) {
        int blocked = 0;
        for (int x = 0; x < cells.x_len; x++) {
            for (int y = 0; y < cells.y_len; y++) {
                if (cells.cells[x][y] != 0) blocked++;
            }
        }
        return blocked;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
javac -cp "build/classes;etc/json-java.jar" -d build/classes src/nurgling/pf/CellsArray.java && javac -cp "build/classes;etc/json-java.jar" -d build/test-classes test/nurgling/pf/CellsArrayRasterizationTest.java && java -cp "build/classes;build/test-classes;etc/json-java.jar" nurgling.pf.CellsArrayRasterizationTest
```

Expected: FAIL with `CellsArray must include the half-cell overlapped by a rotated thin hitbox edge`.

### Task 2: Replace Rounded Raster Bounds With Floor/Ceil Overlap Bounds

**Files:**
- Modify: `src/nurgling/pf/CellsArray.java`
- Test: `test/nurgling/pf/CellsArrayRasterizationTest.java`

- [ ] **Step 1: Implement precise local PF rasterization**

Replace the `CellsArray(NHitBox hb, double angl, Coord2d rc)` body with logic equivalent to:

```java
NHitBoxD objToApproach = new NHitBoxD(hb.begin, hb.end, rc, angl);
Coord2d ul = objToApproach.getCircumscribedUL();
Coord2d br = objToApproach.getCircumscribedBR();
begin = new Coord((int) Math.floor((ul.x - MCache.tileqsz.x) / MCache.tilehsz.x),
        (int) Math.floor((ul.y - MCache.tileqsz.y) / MCache.tilehsz.y));
end = new Coord((int) Math.ceil((br.x + MCache.tileqsz.x) / MCache.tilehsz.x),
        (int) Math.ceil((br.y + MCache.tileqsz.y) / MCache.tilehsz.y));
x_len = end.x - begin.x + 1;
y_len = end.y - begin.y + 1;
cells = new short[x_len][y_len];
for (int i = 0; i < x_len; i++) {
    for (int j = 0; j < y_len; j++) {
        cells[i][j] = overlapsPfCell(begin.add(i, j), objToApproach) ? (short) 1 : 0;
    }
}
```

Add private helpers in `CellsArray`:

```java
private static boolean overlapsPfCell(Coord cell, NHitBoxD hitBox) {
    Coord2d center = Utils.pfGridToWorld(cell);
    Coord2d cellUl = center.sub(MCache.tileqsz);
    Coord2d cellBr = center.add(MCache.tileqsz);
    return hitBoxIntersectsAxisAlignedCell(hitBox, cellUl, cellBr);
}
```

Use a separating-axis test between the rotated hitbox corners and the axis-aligned PF cell. Include edge contact as overlap because pathfinding collision should be conservative.

- [ ] **Step 2: Run regression test to verify it passes**

Run:

```bash
javac -cp "build/classes;etc/json-java.jar" -d build/test-classes test/nurgling/pf/CellsArrayRasterizationTest.java && java -cp "build/classes;build/test-classes;etc/json-java.jar" nurgling.pf.CellsArrayRasterizationTest
```

Expected: PASS with no output.

### Task 3: Compile The Client

**Files:**
- Modify: none
- Test: Ant compile target

- [ ] **Step 1: Compile with Ant**

Run:

```bash
ant hafen-client
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Commit own changes in isolated worktree**

Run:

```bash
git status --short
git add src/nurgling/pf/CellsArray.java test/nurgling/pf/CellsArrayRasterizationTest.java docs/superpowers/plans/2026-06-05-local-pf-hitbox-rasterization.md
git commit -m "fix: rasterize local pathfinder hitboxes by overlap"
```

Expected: commit created on branch `local-pf-hitbox-rasterization`.
