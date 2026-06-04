package nurgling.pf;

import haven.Coord2d;
import nurgling.NHitBox;

public class CellsArrayRasterizationTest {
    public static void main(String[] args) {
        includesCellsTouchedOnlyByOverlapAtTheEdge();
        keepsRotatedThinHitboxesRepresented();
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
}
