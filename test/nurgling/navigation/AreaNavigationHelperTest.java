package nurgling.navigation;

import haven.Coord2d;
import haven.Area;
import haven.Coord;
import haven.Pair;
import nurgling.areas.NArea;

import java.util.concurrent.atomic.AtomicInteger;

public class AreaNavigationHelperTest {
    public static void main(String[] args) {
        localPfAreaTargetsAreInsetFromTileBorders();
        areaCornerPlanningUsesSingleMultiGoalPlan();
        interruptedAreaCornerPlanningDoesNotStartMultiGoalPlan();
        interruptedAreaCornerPlanningDoesNotStartFallbackPlan();
    }

    private static void localPfAreaTargetsAreInsetFromTileBorders() {
        NArea area = new TestArea(new Pair<>(Coord2d.of(0.0, 0.0), Coord2d.of(11.0, 11.0)));

        Coord2d[] targets = AreaNavigationHelper.getAreaReachTargets(area);

        assertCoord2d(targets[0], 3.0, 3.0, "top-left target");
        assertCoord2d(targets[1], 8.0, 8.0, "bottom-right target");
        assertCoord2d(targets[2], 3.0, 8.0, "bottom-left target");
        assertCoord2d(targets[3], 8.0, 3.0, "top-right target");
    }

    private static void assertCoord2d(Coord2d actual, double expectedX, double expectedY, String label) {
        if (Math.abs(actual.x - expectedX) > 0.0001 || Math.abs(actual.y - expectedY) > 0.0001) {
            throw new AssertionError(label + ": expected (" + expectedX + ", " + expectedY + "), got " + actual);
        }
    }

    private static void areaCornerPlanningUsesSingleMultiGoalPlan() {
        NArea area = new NArea("test");
        area.space = new NArea.Space();
        area.space.space.put(1L, new NArea.VArea(new Area(new Coord(0, 0), new Coord(1, 1))));
        CountingChunkNavManager chunkNav = new CountingChunkNavManager();

        try {
            AreaNavigationHelper.findShortestPathToAreaCorners(area, chunkNav);
        } catch (InterruptedException e) {
            throw new AssertionError("test should not be interrupted", e);
        }

        if (chunkNav.multiGoalPlans.get() != 1) {
            throw new AssertionError("area planning must use exactly one multi-goal plan; calls=" + chunkNav.multiGoalPlans.get());
        }
        if (chunkNav.cornerPlans.get() != 0) {
            throw new AssertionError("area planning must not call per-corner planner; calls=" + chunkNav.cornerPlans.get());
        }
    }

    private static class CountingChunkNavManager extends ChunkNavManager {
        private final AtomicInteger multiGoalPlans = new AtomicInteger(0);
        private final AtomicInteger cornerPlans = new AtomicInteger(0);

        @Override
        public ChunkPath planToAreaTargets(NArea area) {
            multiGoalPlans.incrementAndGet();
            ChunkPath path = new ChunkPath();
            path.totalCost = 7;
            return path;
        }

        @Override
        public ChunkPath planToAreaCorner(NArea area, int cornerIndex) {
            cornerPlans.incrementAndGet();
            return null;
        }
    }

    private static void interruptedAreaCornerPlanningDoesNotStartMultiGoalPlan() {
        NArea area = new NArea("test");
        area.space = new NArea.Space();
        area.space.space.put(1L, new NArea.VArea(new Area(new Coord(0, 0), new Coord(1, 1))));
        CountingChunkNavManager chunkNav = new CountingChunkNavManager();

        Thread.currentThread().interrupt();
        try {
            AreaNavigationHelper.findShortestPathToAreaCorners(area, chunkNav);
            throw new AssertionError("interrupted planning must throw InterruptedException");
        } catch (InterruptedException expected) {
            // Expected.
        } finally {
            Thread.interrupted();
        }

        if (chunkNav.multiGoalPlans.get() != 0) {
            throw new AssertionError("already-interrupted area planning must not start multi-goal plan; calls=" + chunkNav.multiGoalPlans.get());
        }
    }

    private static void interruptedAreaCornerPlanningDoesNotStartFallbackPlan() {
        NArea area = new NArea("test");
        area.space = new NArea.Space();
        area.space.space.put(1L, new NArea.VArea(new Area(new Coord(0, 0), new Coord(1, 1))));
        InterruptingChunkNavManager chunkNav = new InterruptingChunkNavManager();

        try {
            AreaNavigationHelper.findShortestPathToAreaCorners(area, chunkNav);
            throw new AssertionError("interrupted planning must throw InterruptedException");
        } catch (InterruptedException expected) {
            // Expected.
        } finally {
            Thread.interrupted();
        }

        if (chunkNav.fallbackPlans.get() != 0) {
            throw new AssertionError("interrupted area corner planning must not start fallback planToArea; calls=" + chunkNav.fallbackPlans.get());
        }
    }

    private static class InterruptingChunkNavManager extends ChunkNavManager {
        private final AtomicInteger fallbackPlans = new AtomicInteger(0);

        @Override
        public ChunkPath planToAreaTargets(NArea area) {
            Thread.currentThread().interrupt();
            return null;
        }

        @Override
        public ChunkPath planToArea(NArea area) {
            fallbackPlans.incrementAndGet();
            return new ChunkPath();
        }
    }

    private static class TestArea extends NArea {
        private final Pair<Coord2d, Coord2d> rcArea;

        private TestArea(Pair<Coord2d, Coord2d> rcArea) {
            super("test");
            this.rcArea = rcArea;
        }

        @Override
        public Pair<Coord2d, Coord2d> getRCArea() {
            return rcArea;
        }

        @Override
        public boolean isVisible() {
            return true;
        }
    }
}
