package nurgling.pf;

import haven.Coord;
import haven.Coord2d;
import nurgling.NHitBox;

public class CellsArrayRasterizationTest {
    public static void main(String[] args) {
        pfCellsUsePeriodicSixUnitPlayerGrid();
        worldCoordinatesSnapToNearestPeriodicPfCell();
        pfCellsUseSixUnitPlayerFootprint();
        cupboardUsesTenByTenHitbox();
        asymmetricHitboxesUseMeasuredLocalOffset();
        herbalistTableUsesMeasuredHitbox();
        includesCellsTouchedOnlyByOverlapAtTheEdge();
        keepsRotatedThinHitboxesRepresented();
    }

    private static void pfCellsUsePeriodicSixUnitPlayerGrid() {
        assertCoord2d(Utils.pfGridToWorld(new Coord(-4, 0)), -11.0, 0.0, "PF grid -4,0");
        assertCoord2d(Utils.pfGridToWorld(new Coord(-3, 0)), -8.0, 0.0, "PF grid -3,0");
        assertCoord2d(Utils.pfGridToWorld(new Coord(-2, 0)), -5.5, 0.0, "PF grid -2,0");
        assertCoord2d(Utils.pfGridToWorld(new Coord(-1, 0)), -3.0, 0.0, "PF grid -1,0");
        assertCoord2d(Utils.pfGridToWorld(new Coord(0, 0)), 0.0, 0.0, "PF grid 0,0");
        assertCoord2d(Utils.pfGridToWorld(new Coord(1, 1)), 3.0, 3.0, "PF grid 1,1");
        assertCoord2d(Utils.pfGridToWorld(new Coord(2, 2)), 5.5, 5.5, "PF grid 2,2");
        assertCoord2d(Utils.pfGridToWorld(new Coord(3, 3)), 8.0, 8.0, "PF grid 3,3");
        assertCoord2d(Utils.pfGridToWorld(new Coord(4, 4)), 11.0, 11.0, "PF grid 4,4");
        assertCoord2d(Utils.pfGridToWorld(new Coord(5, 5)), 14.0, 14.0, "PF grid 5,5");
        assertCoord2d(Utils.pfGridToWorld(new Coord(6, 6)), 16.5, 16.5, "PF grid 6,6");
        assertCoord2d(Utils.pfGridToWorld(new Coord(7, 7)), 19.0, 19.0, "PF grid 7,7");
        assertCoord2d(Utils.pfGridToWorld(new Coord(8, 8)), 22.0, 22.0, "PF grid 8,8");
    }

    private static void worldCoordinatesSnapToNearestPeriodicPfCell() {
        assertCoord(Utils.toPfGrid(new Coord2d(0.0, 0.0)), 0, 0, "snap exact origin");
        assertCoord(Utils.toPfGrid(new Coord2d(2.9, 3.1)), 1, 1, "snap near offset 3");
        assertCoord(Utils.toPfGrid(new Coord2d(5.6, 5.4)), 2, 2, "snap near tile center");
        assertCoord(Utils.toPfGrid(new Coord2d(8.1, 7.9)), 3, 3, "snap near offset 8");
        assertCoord(Utils.toPfGrid(new Coord2d(10.9, 11.1)), 4, 4, "snap near next tile edge");
        assertCoord(Utils.toPfGrid(new Coord2d(-2.9, -3.1)), -1, -1, "snap negative offset -3");
        assertCoord(Utils.toPfGrid(new Coord2d(-5.4, -5.6)), -2, -2, "snap negative center");
    }

    private static void pfCellsUseSixUnitPlayerFootprint() {
        assertClose(Utils.CELL_HALFSZ.x, 3.0, "PF cell half width");
        assertClose(Utils.CELL_HALFSZ.y, 3.0, "PF cell half height");
    }

    private static void cupboardUsesTenByTenHitbox() {
        NHitBox hitBox = NHitBox.findCustom("gfx/terobjs/cupboard");

        assertClose(hitBox.begin.x, -5.0, "cupboard min x");
        assertClose(hitBox.begin.y, -5.0, "cupboard min y");
        assertClose(hitBox.end.x, 5.0, "cupboard max x");
        assertClose(hitBox.end.y, 5.0, "cupboard max y");
    }

    private static void asymmetricHitboxesUseMeasuredLocalOffset() {
        NHitBox hitBox = new NHitBox(new Coord2d(-2.0, -1.0), new Coord2d(4.0, 1.0), true);
        NHitBoxD box = new NHitBoxD(hitBox, new Coord2d(0.0, 0.0), 0);

        assertClose(box.getCircumscribedUL().x, -2.0, "asymmetric hitbox min x");
        assertClose(box.getCircumscribedBR().x, 4.0, "asymmetric hitbox max x");
    }

    private static void herbalistTableUsesMeasuredHitbox() {
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

    private static void assertCoord(Coord actual, int expectedX, int expectedY, String label) {
        if (actual.x != expectedX || actual.y != expectedY) {
            throw new AssertionError(label + ": expected (" + expectedX + ", " + expectedY + "), got " + actual);
        }
    }

    private static void assertCoord2d(Coord2d actual, double expectedX, double expectedY, String label) {
        assertClose(actual.x, expectedX, label + " x");
        assertClose(actual.y, expectedY, label + " y");
    }

    private static void assertClose(double actual, double expected, String label) {
        if (Math.abs(actual - expected) > 0.0001) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
