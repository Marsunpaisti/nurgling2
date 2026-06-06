package nurgling.navigation;

import haven.Coord2d;
import haven.Pair;
import nurgling.areas.NArea;

public class AreaNavigationHelperTest {
    public static void main(String[] args) {
        localPfAreaTargetsAreInsetFromTileBorders();
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
