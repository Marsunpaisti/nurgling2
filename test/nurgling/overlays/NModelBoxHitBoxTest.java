package nurgling.overlays;

import haven.Coord2d;
import haven.Coord3f;
import nurgling.NHitBox;

public class NModelBoxHitBoxTest {
    public static void main(String[] args) {
        boundingBoxKeepsHitBoxMapSpaceX();
        renderPointFlipsOnlyMapSpaceY();
    }

    private static void boundingBoxKeepsHitBoxMapSpaceX() {
        NHitBox hitBox = new NHitBox(new Coord2d(-2.965, -5.945), new Coord2d(3.965, 5.945), true);
        NModelBox.NBoundingBox boundingBox = NModelBox.NBoundingBox.getBoundingBox(hitBox);
        Coord2d[] vertices = boundingBox.polygons.get(0).vertices;

        assertCoord(vertices[0], -2.965, -5.945, "upper-left vertex");
        assertCoord(vertices[1], 3.965, -5.945, "upper-right vertex");
        assertCoord(vertices[2], 3.965, 5.945, "bottom-right vertex");
        assertCoord(vertices[3], -2.965, 5.945, "bottom-left vertex");
    }

    private static void renderPointFlipsOnlyMapSpaceY() {
        Coord3f rendered = NModelBox.HidePol.renderPoint(Coord2d.of(3.965, 5.945));

        assertClose(rendered.x, 3.965, "render X must preserve hitbox X");
        assertClose(rendered.y, -5.945, "render Y must flip hitbox Y");
        assertClose(rendered.z, 1.0, "render Z lift must stay unchanged");
    }

    private static void assertCoord(Coord2d actual, double expectedX, double expectedY, String label) {
        assertClose(actual.x, expectedX, label + " X");
        assertClose(actual.y, expectedY, label + " Y");
    }

    private static void assertClose(double actual, double expected, String label) {
        if (Math.abs(actual - expected) > 0.000001) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
