# Chunknav Area Multigoal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace four separate chunknav area-corner A* searches with one A* search that accepts multiple candidate targets and stops at the cheapest reached target.

**Architecture:** Keep graph traversal direction forward from the player to targets so portal edge semantics stay unchanged. Add `UnifiedTilePathfinder.findPathToAny(...)` with a min-over-targets heuristic, keep existing `findPath(...)` as a single-target wrapper, and update area planning to build one set of corner approach targets.

**Tech Stack:** Java, existing main-method tests, Ant build.

---

### Task 1: Add Multi-Goal A* To UnifiedTilePathfinder

**Files:**
- Modify: `src/nurgling/navigation/UnifiedTilePathfinder.java`
- Test: `test/nurgling/navigation/UnifiedTilePathfinderMultiGoalTest.java`

- [ ] **Step 1: Write the failing test**

Create `test/nurgling/navigation/UnifiedTilePathfinderMultiGoalTest.java` with a small in-memory `ChunkNavGraph` and one chunk. The test should call `findPathToAny(startChunkId, startLocal, targets)` with two same-chunk targets and assert the reached final step is the nearer target.

```java
package nurgling.navigation;

import haven.Coord;
import java.util.Arrays;
import java.util.List;

public class UnifiedTilePathfinderMultiGoalTest {
    public static void main(String[] args) {
        choosesNearestTargetFromMultipleGoals();
        singleTargetWrapperMatchesMultiGoalPath();
    }

    private static void choosesNearestTargetFromMultipleGoals() {
        ChunkNavGraph graph = new ChunkNavGraph();
        ChunkNavData chunk = walkableChunk(1L, new Coord(0, 0));
        graph.addChunk(chunk);

        UnifiedTilePathfinder pathfinder = new UnifiedTilePathfinder(graph);
        List<UnifiedTilePathfinder.TileNode> targets = Arrays.asList(
                new UnifiedTilePathfinder.TileNode(1L, new Coord(20, 20)),
                new UnifiedTilePathfinder.TileNode(1L, new Coord(3, 0))
        );

        UnifiedTilePathfinder.UnifiedPath path = pathfinder.findPathToAny(1L, new Coord(0, 0), targets);

        if (path == null || !path.reachable) {
            throw new AssertionError("Multi-goal path should be reachable");
        }
        UnifiedTilePathfinder.TileNode last = path.steps.get(path.steps.size() - 1);
        if (!last.equals(new UnifiedTilePathfinder.TileNode(1L, new Coord(3, 0)))) {
            throw new AssertionError("Expected nearest target (3,0), got " + last.localCoord);
        }
    }

    private static void singleTargetWrapperMatchesMultiGoalPath() {
        ChunkNavGraph graph = new ChunkNavGraph();
        graph.addChunk(walkableChunk(1L, new Coord(0, 0)));

        UnifiedTilePathfinder pathfinder = new UnifiedTilePathfinder(graph);
        UnifiedTilePathfinder.UnifiedPath single = pathfinder.findPath(1L, new Coord(0, 0), 1L, new Coord(5, 0));
        UnifiedTilePathfinder.UnifiedPath multi = pathfinder.findPathToAny(1L, new Coord(0, 0),
                Arrays.asList(new UnifiedTilePathfinder.TileNode(1L, new Coord(5, 0))));

        if (single == null || multi == null || single.steps.size() != multi.steps.size()) {
            throw new AssertionError("Single-target wrapper should match one-target multi-goal path");
        }
    }

    private static ChunkNavData walkableChunk(long gridId, Coord gridCoord) {
        ChunkNavData chunk = new ChunkNavData(gridId, gridCoord, new Coord(0, 0));
        for (int x = 0; x < ChunkNavConfig.CELLS_PER_EDGE; x++) {
            for (int y = 0; y < ChunkNavConfig.CELLS_PER_EDGE; y++) {
                chunk.walkability[x][y] = 0;
            }
        }
        return chunk;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `javac -cp "build/classes;etc/json-java.jar" -d build/test-classes test/nurgling/navigation/UnifiedTilePathfinderMultiGoalTest.java`

Expected: compile failure because `findPathToAny(...)` does not exist.

- [ ] **Step 3: Implement minimal multi-goal API**

In `UnifiedTilePathfinder`, change `findPath(...)` to wrap a new method:

```java
public UnifiedPath findPath(long startChunkId, Coord startLocal, long targetChunkId, Coord targetLocal) {
    return findPathToAny(startChunkId, startLocal,
            java.util.Collections.singleton(new TileNode(targetChunkId, targetLocal)));
}
```

Add `findPathToAny(...)` by moving the existing A* body from `findPath(...)` and replacing `targetTile` with a `Set<TileNode> targetTiles`. Stop when `targetTiles.contains(current.tile)`. Compute `h` with:

```java
private double heuristicToAny(TileNode from, Collection<TileNode> targets) {
    double best = Double.MAX_VALUE;
    for (TileNode target : targets) {
        best = Math.min(best, heuristic(from, target));
    }
    return best;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `javac -cp "build/classes;etc/json-java.jar" -d build/classes src/nurgling/navigation/UnifiedTilePathfinder.java && javac -cp "build/classes;etc/json-java.jar" -d build/test-classes test/nurgling/navigation/UnifiedTilePathfinderMultiGoalTest.java && java -cp "build/classes;build/test-classes;etc/json-java.jar" nurgling.navigation.UnifiedTilePathfinderMultiGoalTest`

Expected: no output and exit code 0.

---

### Task 2: Use Multi-Goal A* For Area Corner Planning

**Files:**
- Modify: `src/nurgling/navigation/ChunkNavPlanner.java`
- Modify: `src/nurgling/navigation/AreaNavigationHelper.java`
- Test: `test/nurgling/navigation/AreaNavigationHelperTest.java` or a new focused planner test if helper construction is too coupled.

- [ ] **Step 1: Write the failing test**

Add a regression that verifies the area helper calls one planner method for all corner targets instead of four `planToAreaCorner(...)` calls. If existing test doubles only expose `planToAreaCorner(...)`, add a small test-only subclass or fake `ChunkNavManager` counter for `planToAreaTargets(...)`.

Expected behavior: `findShortestPathToAreaCorners(area, chunkNav)` returns the multi-goal path and does not invoke per-corner planning.

- [ ] **Step 2: Run test to verify it fails**

Run the focused helper test command already used for area helper tests:

`javac -cp "build/classes;etc/json-java.jar" -d build/test-classes test/nurgling/navigation/AreaNavigationHelperTest.java && java -cp "build/classes;build/test-classes;etc/json-java.jar" nurgling.navigation.AreaNavigationHelperTest`

Expected: fail until the multi-goal planner method exists or is used.

- [ ] **Step 3: Add target collection in ChunkNavPlanner**

Add `planToAreaTargets(NArea area)` to `ChunkNavPlanner`. It should:

1. Get `PlayerLocation` once.
2. Iterate `area.space.space` once.
3. For each stored area grid and each `cornerIndex 0..3`, compute the same `cornerLocal`, search direction, and `walkable` target currently used by `planToAreaCorner(...)`.
4. Add each valid target as `new UnifiedTilePathfinder.TileNode(gridId, walkable)`.
5. Call `unifiedPathfinder.findPathToAny(startChunkId, playerLocal, targets)` once.
6. Convert the returned `UnifiedPath` into `ChunkPath` as existing code does.

Keep `planToAreaCorner(...)` as a compatibility wrapper for existing callers during this change.

- [ ] **Step 4: Update AreaNavigationHelper**

Change `findShortestPathToAreaCorners(...)` to call `chunkNav.planToAreaTargets(area)` once. Keep fallback to `chunkNav.planToArea(area)` if multi-goal returns null.

- [ ] **Step 5: Run targeted tests**

Run:

`javac -cp "build/classes;etc/json-java.jar" -d build/test-classes test/nurgling/navigation/UnifiedTilePathfinderMultiGoalTest.java test/nurgling/navigation/AreaNavigationHelperTest.java && java -cp "build/classes;build/test-classes;etc/json-java.jar" nurgling.navigation.UnifiedTilePathfinderMultiGoalTest && java -cp "build/classes;build/test-classes;etc/json-java.jar" nurgling.navigation.AreaNavigationHelperTest`

Expected: no output and exit code 0.

---

### Task 3: Build Verification

**Files:**
- Verify only; no new source edits unless build fails.

- [ ] **Step 1: Run full build**

Run: `ant jar`

Expected: `BUILD SUCCESSFUL`. Existing `LayerUtil` resource decode errors may still print; they are known build noise if Ant exits successfully.

- [ ] **Step 2: Inspect final diff**

Run: `git diff -- src/nurgling/navigation/UnifiedTilePathfinder.java src/nurgling/navigation/ChunkNavPlanner.java src/nurgling/navigation/AreaNavigationHelper.java test/nurgling/navigation/UnifiedTilePathfinderMultiGoalTest.java test/nurgling/navigation/AreaNavigationHelperTest.java`

Expected: only multi-goal area planning changes plus tests.

---

## Self-Review

- Spec coverage: plan covers one A* call for multiple area targets, min-over-target heuristic, and unchanged forward graph semantics.
- Placeholder scan: no TBD/TODO placeholders remain.
- Type consistency: new API is `findPathToAny(startChunkId, startLocal, Collection<TileNode> targets)` and planner API is `planToAreaTargets(NArea area)`.
- Scope: chunknav only; local PF multi-goal is intentionally excluded.
