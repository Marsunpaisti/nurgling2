package nurgling.navigation;

import haven.Coord;

import java.util.Arrays;
import java.util.List;

public class UnifiedTilePathfinderMultiGoalTest {
    public static void main(String[] args) {
        choosesNearestTargetFromMultipleGoals();
        singleTargetWrapperMatchesMultiGoalPath();
        ignoresMissingTargetChunksWhenAnotherTargetIsReachable();
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

    private static void ignoresMissingTargetChunksWhenAnotherTargetIsReachable() {
        ChunkNavGraph graph = new ChunkNavGraph();
        graph.addChunk(walkableChunk(1L, new Coord(0, 0)));

        UnifiedTilePathfinder pathfinder = new UnifiedTilePathfinder(graph);
        UnifiedTilePathfinder.UnifiedPath path = pathfinder.findPathToAny(1L, new Coord(0, 0), Arrays.asList(
                new UnifiedTilePathfinder.TileNode(999L, new Coord(1, 1)),
                new UnifiedTilePathfinder.TileNode(1L, new Coord(4, 0))
        ));

        if (path == null || !path.reachable) {
            throw new AssertionError("Reachable target should still be used when another target chunk is missing");
        }
        UnifiedTilePathfinder.TileNode last = path.steps.get(path.steps.size() - 1);
        if (!last.equals(new UnifiedTilePathfinder.TileNode(1L, new Coord(4, 0)))) {
            throw new AssertionError("Expected reachable target (4,0), got " + last.localCoord);
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
