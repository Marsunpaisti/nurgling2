package nurgling.pf;

import haven.Coord2d;

public class NHitBoxDIntersectionTest {
    public static void main(String[] args) {
        rotatedEdgeCrossingCountsAsIntersection();
        borderTouchFollowsIncludeBorderFlag();
    }

    private static void rotatedEdgeCrossingCountsAsIntersection() {
        NHitBoxD first = new NHitBoxD(
                new Coord2d(-18.870772247487736, -0.2877170546160489),
                new Coord2d(18.870772247487736, 0.2877170546160489),
                new Coord2d(18.387289965471666, 12.75118440904825),
                0.6748453130084016);
        NHitBoxD second = new NHitBoxD(
                new Coord2d(-16.775442057285215, -0.6114325829271391),
                new Coord2d(16.775442057285215, 0.6114325829271391),
                new Coord2d(5.622819243768088, 8.659691309252079),
                2.5188752106952843);

        if (!first.intersects(second, true)) {
            throw new AssertionError("Rotated rectangle edge crossing must intersect even when no sampled point is inside");
        }
    }

    private static void borderTouchFollowsIncludeBorderFlag() {
        NHitBoxD left = new NHitBoxD(new Coord2d(-1.0, -1.0), new Coord2d(1.0, 1.0));
        NHitBoxD right = new NHitBoxD(new Coord2d(1.0, -1.0), new Coord2d(3.0, 1.0));

        if (!left.intersects(right, true)) {
            throw new AssertionError("includeBorder=true must count shared borders as intersection");
        }
        if (left.intersects(right, false)) {
            throw new AssertionError("includeBorder=false must not count shared borders as intersection");
        }
    }
}
