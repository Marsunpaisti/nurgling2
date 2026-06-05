package nurgling.pf;

import haven.*;
import nurgling.*;

public class CellsArray {
    public Coord begin;
    public Coord end;
    public short[][] cells;

    public int x_len;
    public int y_len;

    public CellsArray(Gob gob) {
        this(gob.ngob.hitBox, gob.a, gob.rc);
    }


    public CellsArray(int x_len, int y_len) {
        this.cells = new short[x_len][y_len];
        this.x_len = x_len;
        this.y_len = y_len;
    }

    public CellsArray(NHitBox hb, double angl, Coord2d rc) {
        NHitBoxD objToApproach = new NHitBoxD(hb, rc, angl);
        Coord2d ul = objToApproach.getCircumscribedUL();
        Coord2d br = objToApproach.getCircumscribedBR();
        begin = new Coord((int) Math.floor((ul.x - Utils.CELL_HALFSZ.x) / Utils.GRID_STEP.x),
                (int) Math.floor((ul.y - Utils.CELL_HALFSZ.y) / Utils.GRID_STEP.y));
        end = new Coord((int) Math.ceil((br.x + Utils.CELL_HALFSZ.x) / Utils.GRID_STEP.x),
                (int) Math.ceil((br.y + Utils.CELL_HALFSZ.y) / Utils.GRID_STEP.y));
        x_len = end.x - begin.x + 1;
        y_len = end.y - begin.y + 1;
        cells = new short[x_len][y_len];
        for (int i = 0; i < x_len; i++) {
            for (int j = 0; j < y_len; j++) {
                cells[i][j] = overlapsPfCell(begin.add(i, j), objToApproach) ? (short) 1 : 0;
            }
        }
    }

    private static boolean overlapsPfCell(Coord cell, NHitBoxD hitBox) {
        Coord2d center = Utils.pfGridToWorld(cell);
        Coord2d ul = center.sub(Utils.CELL_HALFSZ);
        Coord2d br = center.add(Utils.CELL_HALFSZ);
        return hitBox.intersectsAxisAlignedRect(ul.x, ul.y, br.x, br.y, true);
    }
}
