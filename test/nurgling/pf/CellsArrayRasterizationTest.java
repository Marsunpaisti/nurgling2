package nurgling.pf;

import haven.Coord;
import haven.Coord2d;
import nurgling.NHitBox;

public class CellsArrayRasterizationTest {
    public static void main(String[] args) {
        pfCellsAreCenteredEveryQuarterTile();
        cupboardUsesTenByTenHitbox();
        asymmetricHitboxesStillMirrorByDefault();
        herbalistTableUsesMeasuredUnmirroredHitbox();
        includesCellsTouchedOnlyByOverlapAtTheEdge();
        keepsRotatedThinHitboxesRepresented();
    }

    private static void pfCellsAreCenteredEveryQuarterTile() {
        Coord2d firstStep = Utils.pfGridToWorld(new Coord(1, 1));

        assertClose(firstStep.x, 2.75, "PF grid x step");
        assertClose(firstStep.y, 2.75, "PF grid y step");
    }

    private static void cupboardUsesTenByTenHitbox() {
        NHitBox hitBox = NHitBox.findCustom("gfx/terobjs/cupboard");

        assertClose(hitBox.begin.x, -5.0, "cupboard min x");
        assertClose(hitBox.begin.y, -5.0, "cupboard min y");
        assertClose(hitBox.end.x, 5.0, "cupboard max x");
        assertClose(hitBox.end.y, 5.0, "cupboard max y");
    }

    private static void asymmetricHitboxesStillMirrorByDefault() {
        NHitBox hitBox = new NHitBox(new Coord2d(-2.0, -1.0), new Coord2d(4.0, 1.0), true);
        NHitBoxD box = new NHitBoxD(hitBox, new Coord2d(0.0, 0.0), 0);

        assertClose(box.getCircumscribedUL().x, -4.0, "default asymmetric hitbox min x");
        assertClose(box.getCircumscribedBR().x, 2.0, "default asymmetric hitbox max x");
    }

    private static void herbalistTableUsesMeasuredUnmirroredHitbox() {
        NHitBox hitBox = NHitBox.findCustom("gfx/terobjs/htable");
        NHitBoxD box = new NHitBoxD(hitBox, new Coord2d(0.0, 0.0), 0);

        assertClose(box.getCircumscribedUL().x, -2.965, "herbalist table min x");
        assertClose(box.getCircumscribedUL().y, -5.945, "herbalist table min y");
        assertClose(box.getCircumscribedBR().x, 3.965, "herbalist table max x");
        assertClose(box.getCircumscribedBR().y, 5.945, "herbalist table max y");
    }

    private static void includesCellsTouchedOnlyByOverlapAtTheEdge() {
        NHitBox hitBox = new NHitBox(new Coord2d(-1.0, -10.0), new Coord2d(1.0, 10.0), true);
        CellsArray cells = new CellsArray(hitBox, Math.toRadians(11.0), new Coord2d(0.0, 0.0));

        if (!hasBlockedCellAt(cells, 0, -1)) {
            throw new AssertionError("CellsArray must include the half-cell overlapped by a rotated thin hitbox edge");
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
                if (cells.cells[x][y] != 0) {
                    blocked++;
                }
            }
        }
        return blocked;
    }

    private static void assertClose(double actual, double expected, String label) {
        if (Math.abs(actual - expected) > 0.0001) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
