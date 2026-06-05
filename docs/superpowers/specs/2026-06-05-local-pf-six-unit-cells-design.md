# Local PF Six-Unit Cell Design

## Goal

Update local pathfinder graph geometry so each candidate player position is tested with a `6x6` collision square, matching the actual player hitbox, while retaining four graph intervals per Haven tile.

The current local PF grid uses uniform `2.75` world-unit spacing and `5.5x5.5` collision cells. With a real `6x6` player body, simply increasing cell size on the old centers would make the cells centered at tile offsets `2.75` and `8.25` overlap tiny slices of neighboring grid-aligned tiles. That would incorrectly mark otherwise valid positions blocked around common grid-aligned objects.

## Design

Replace the local PF world/index mapping with a periodic non-uniform axis mapping. Each 11-unit Haven tile gets graph coordinates at offsets:

```text
0, 3, 5.5, 8, 11
```

The repeating interval pattern is:

```text
3, 2.5, 2.5, 3
```

Examples for one axis:

```text
index:  ... -4  -3    -2  -1   0  1  2    3  4   5   6     7   8
world:  ... -11 -8  -5.5  -3   0  3  5.5  8  11  14  16.5  19  22
```

Collision cells use `CELL_HALFSZ = Coord2d.of(3, 3)`. This makes each local PF cell represent a `6x6` player-footprint square.

## Components

### `nurgling.pf.Utils`

Add axis helpers that centralize all local PF coordinate conversion:

- `pfAxisToWorld(int index)`: maps an integer PF index to world position using the periodic offsets.
- `worldAxisToPf(double coord)`: snaps a world coordinate to the nearest periodic PF index.
- `firstPfAxisAtOrAfter(double coord)`: returns the lowest PF index whose world coordinate is greater than or equal to `coord`.
- `lastPfAxisAtOrBefore(double coord)`: returns the highest PF index whose world coordinate is less than or equal to `coord`.
- `pfGridToWorld(Coord coord)`: maps both axes through `pfAxisToWorld`.
- `toPfGrid(Coord2d coord)`: maps both axes through `worldAxisToPf`.

The local PF code must stop assuming `world = index * 2.75` and `index = round(world / 2.75)`.

### `nurgling.pf.CellsArray`

Keep exact overlap rasterization through `NHitBoxD.intersectsAxisAlignedRect(...)`, but update candidate range calculation so it handles non-uniform PF coordinates.

Instead of using uniform division by `GRID_STEP`, compute candidate PF index ranges with lower/upper-bound helpers:

```text
first = firstPfAxisAtOrAfter(hitbox min - CELL_HALFSZ)
last  = lastPfAxisAtOrBefore(hitbox max + CELL_HALFSZ)
```

Then test each candidate cell with the existing exact overlap logic.

### Local PF Consumers

Update local PF consumers that use `Utils.GRID_STEP` as a world-distance scale:

- `NPFMap` sizing and boundary calculations.
- Candidate tile lookup helpers around `Utils.pfGridToWorld(...)`.
- `PathFinder` endpoint and target proximity checks that compare PF cells against world coordinates.
- `Graph` path smoothing that converts corners back to PF indices.
- `DynamicPf` path smoothing beam checks.

Keep the graph node indices integer-based. Neighbor expansion can continue to use adjacent integer indices; the resulting world step lengths are now variable but still local and deterministic.

## Data Flow

1. Raw gob hitboxes remain in local map space.
2. `NHitBoxD` places gob hitboxes in world space with gob `rc` and `a`.
3. `CellsArray` computes candidate PF indices using the non-uniform helper functions.
4. Each candidate PF index maps to a world center through `pfGridToWorld(...)`.
5. A `6x6` axis-aligned player cell centered there is tested against the placed hitbox.
6. A blocked PF cell means the real player footprint would overlap the obstacle.

## Non-Goals

- Do not change chunk navigation grid density or chunknav persistent cell size.
- Do not alter `NHitBoxD` rotation, SAT, or AABB intersection semantics.
- Do not add special cases for specific objects; the mapping change is global to local PF.
- Do not model player rotation; local PF player cells remain axis-aligned `6x6` squares.

## Tests

Add or update regression tests for:

- PF axis mapping: indices map to `0, 3, 5.5, 8, 11` within a tile and repeat correctly across negative and positive tiles.
- World-to-PF snapping: representative coordinates snap to the nearest intended offset.
- Cell size: `Utils.CELL_HALFSZ` is `3`, so candidate cells are `6x6`.
- Grid-aligned obstacle behavior: a tile-aligned blocked region does not mark a neighboring-tile PF center blocked only because the old `2.75` or `8.25` center overlaps a tiny sliver.
- Existing hitbox rasterization behavior: htable/cupboard/asymmetric hitbox tests remain correct under the new grid mapping.
- Rotated thin hitboxes remain represented by exact overlap checks.

## Verification

Run after implementation:

```bash
ant jar
javac -cp "build/classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" -d "build/test-classes" "test/nurgling/pf/CellsArrayRasterizationTest.java" "test/nurgling/pf/NHitBoxDIntersectionTest.java" "test/nurgling/navigation/ChunkNavRecorderRasterizationTest.java"
java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.pf.CellsArrayRasterizationTest
java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.pf.NHitBoxDIntersectionTest
java -cp "build/classes;build/test-classes;etc/json-java.jar;etc/postgresql-42.7.5.jar;etc/sqlite-jdbc-3.49.1.0.jar" nurgling.navigation.ChunkNavRecorderRasterizationTest
```

## Risks

The largest risk is hidden uniform-grid assumptions. Any code that derives world distance by multiplying an index delta by `GRID_STEP` must be inspected. Prefer helper-based conversion at the point where coordinates cross from PF index space to world space.

Path optimality may change because neighboring graph steps have variable world lengths. This is acceptable for the first implementation because the local PF already optimizes a discrete path and then smooths it with world-space collision checks.
