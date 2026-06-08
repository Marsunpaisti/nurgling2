package nurgling.overlays;

import haven.Coord2d;
import haven.Coord3f;
import haven.render.BufPipe;
import haven.render.States;
import nurgling.NHitBox;

import java.awt.Color;

public class NModelBoxHitBoxTest {
    public static void main(String[] args) {
        boundingBoxKeepsHitBoxMapSpaceX();
        renderPointFlipsOnlyMapSpaceY();
        filledMaterialDisablesDepthTest();
        filledLineMaterialDisablesDepthTest();
        filledAlwaysMaterialDisablesDepthTest();
        renderedFillVerticesFaceUpAfterYFlip();
        fillMaterialPreservesBackFaceCulling();
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

    private static void filledAlwaysMaterialDisablesDepthTest() {
        BufPipe pipe = new BufPipe();
        pipe.put(States.depthtest, new States.Depthtest(States.Depthtest.Test.LE));

        NModelBox.HidePol.fillMaterial("FILLED_ALWAYS", false, Color.RED).apply(pipe);

        if (pipe.get(States.depthtest) != null) {
            throw new AssertionError("FILLED_ALWAYS fill material should clear inherited depth test");
        }
    }

    private static void filledMaterialDisablesDepthTest() {
        BufPipe pipe = new BufPipe();
        pipe.put(States.depthtest, new States.Depthtest(States.Depthtest.Test.LE));

        NModelBox.HidePol.fillMaterial("FILLED", false, Color.RED).apply(pipe);

        if (pipe.get(States.depthtest) != null) {
            throw new AssertionError("FILLED fill material should clear inherited depth test");
        }
    }

    private static void filledLineMaterialDisablesDepthTest() {
        BufPipe pipe = new BufPipe();
        pipe.put(States.depthtest, new States.Depthtest(States.Depthtest.Test.LE));

        NModelBox.HidePol.lineMaterial("FILLED", false, Color.YELLOW, 4).apply(pipe);

        if (pipe.get(States.depthtest) != null) {
            throw new AssertionError("FILLED line material should clear inherited depth test");
        }
    }

    private static void renderedFillVerticesFaceUpAfterYFlip() {
        NHitBox hitBox = new NHitBox(new Coord2d(-2.965, -5.945), new Coord2d(3.965, 5.945), true);
        NModelBox.NBoundingBox.Polygon polygon = NModelBox.NBoundingBox.getBoundingBox(hitBox).polygons.get(0);
        Coord3f[] vertices = NModelBox.HidePol.renderVertices(polygon);

        if (signedAreaXY(vertices) <= 0) {
            throw new AssertionError("Rendered fill vertices should be counter-clockwise/top-facing after Y flip");
        }
    }

    private static void fillMaterialPreservesBackFaceCulling() {
        BufPipe pipe = new BufPipe();
        pipe.put(States.facecull, new States.Facecull());

        NModelBox.HidePol.fillMaterial("FILLED", false, Color.RED).apply(pipe);

        if (pipe.get(States.facecull) == null) {
            throw new AssertionError("Fill material should preserve back-face culling; winding should make the quad visible");
        }
    }

    private static double signedAreaXY(Coord3f[] vertices) {
        double area = 0;
        for (int i = 0; i < vertices.length; i++) {
            Coord3f a = vertices[i];
            Coord3f b = vertices[(i + 1) % vertices.length];
            area += (a.x * b.y) - (a.y * b.x);
        }
        return area;
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
