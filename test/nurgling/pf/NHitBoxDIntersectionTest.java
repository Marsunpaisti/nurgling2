package nurgling.pf;

import haven.Coord2d;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class NHitBoxDIntersectionTest {
    public static void main(String[] args) {
        rotatedEdgeCrossingCountsAsIntersection();
        borderTouchFollowsIncludeBorderFlag();
        axisAlignedRectangleHelperFollowsBorderFlag();
        axisAlignedFastPathMatchesRectangleHelper();
        transformsAreConstructionOnly();
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

    private static void axisAlignedRectangleHelperFollowsBorderFlag() {
        NHitBoxD box = new NHitBoxD(new Coord2d(0.0, 0.0), new Coord2d(2.0, 2.0));

        if (!box.intersectsAxisAlignedRect(2.0, 0.5, 3.0, 1.5, true)) {
            throw new AssertionError("includeBorder=true must count primitive rectangle border contact");
        }
        if (box.intersectsAxisAlignedRect(2.0, 0.5, 3.0, 1.5, false)) {
            throw new AssertionError("includeBorder=false must reject primitive rectangle border contact");
        }
    }

    private static void axisAlignedFastPathMatchesRectangleHelper() {
        NHitBoxD box = new NHitBoxD(new Coord2d(-1.0, -1.0), new Coord2d(1.0, 1.0));
        NHitBoxD overlappingRect = new NHitBoxD(new Coord2d(0.5, -0.5), new Coord2d(2.0, 0.5));
        NHitBoxD touchingRect = new NHitBoxD(new Coord2d(1.0, -0.5), new Coord2d(2.0, 0.5));

        assertSame(
                box.intersects(overlappingRect, true),
                box.intersectsAxisAlignedRect(0.5, -0.5, 2.0, 0.5, true),
                "axis-aligned overlap must match primitive rectangle helper");
        assertSame(
                box.intersects(touchingRect, true),
                box.intersectsAxisAlignedRect(1.0, -0.5, 2.0, 0.5, true),
                "axis-aligned border-inclusive contact must match primitive rectangle helper");
        assertSame(
                box.intersects(touchingRect, false),
                box.intersectsAxisAlignedRect(1.0, -0.5, 2.0, 0.5, false),
                "axis-aligned border-exclusive contact must match primitive rectangle helper");
    }

    private static void transformsAreConstructionOnly() {
        assertNoPublicMethod("setUnitSquare");
        assertNoPublicMethod("setOrtho");
        assertNoPublicMethod("move_ortho");
        assertNoPublicMethod("move");
    }

    private static void assertSame(boolean expected, boolean actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertNoPublicMethod(String name) {
        for (Method method : NHitBoxD.class.getDeclaredMethods()) {
            if (method.getName().equals(name) && Modifier.isPublic(method.getModifiers())) {
                throw new AssertionError("NHitBoxD." + name + " must not be a public mutator");
            }
        }
    }
}
