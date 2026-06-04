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
        NHitBoxD objToApproach = new NHitBoxD(hb.begin, hb.end, rc, angl);
        Coord2d ul = objToApproach.getCircumscribedUL();
        Coord2d br = objToApproach.getCircumscribedBR();
        begin = new Coord((int) Math.floor((ul.x - MCache.tileqsz.x) / MCache.tilehsz.x),
                (int) Math.floor((ul.y - MCache.tileqsz.y) / MCache.tilehsz.y));
        end = new Coord((int) Math.ceil((br.x + MCache.tileqsz.x) / MCache.tilehsz.x),
                (int) Math.ceil((br.y + MCache.tileqsz.y) / MCache.tilehsz.y));
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
        Coord2d ul = center.sub(MCache.tileqsz);
        Coord2d br = center.add(MCache.tileqsz);
        Coord2d[] cellCorners = new Coord2d[]{
                ul,
                Coord2d.of(br.x, ul.y),
                br,
                Coord2d.of(ul.x, br.y)
        };

        if (isSeparated(Coord2d.of(1, 0), hitBox.c, cellCorners))
            return false;
        if (isSeparated(Coord2d.of(0, 1), hitBox.c, cellCorners))
            return false;

        for (int i = 0; i < 4; i++) {
            Coord2d edge = hitBox.c[(i + 1) % 4].sub(hitBox.c[i]);
            Coord2d axis = Coord2d.of(-edge.y, edge.x);
            if (axis.x == 0 && axis.y == 0)
                continue;
            if (isSeparated(axis, hitBox.c, cellCorners))
                return false;
        }
        return true;
    }

    private static boolean isSeparated(Coord2d axis, Coord2d[] hitBoxCorners, Coord2d[] cellCorners) {
        double hitBoxMin = Double.MAX_VALUE;
        double hitBoxMax = -Double.MAX_VALUE;
        for (Coord2d corner : hitBoxCorners) {
            double projection = corner.dot(axis);
            hitBoxMin = Math.min(hitBoxMin, projection);
            hitBoxMax = Math.max(hitBoxMax, projection);
        }

        double cellMin = Double.MAX_VALUE;
        double cellMax = -Double.MAX_VALUE;
        for (Coord2d corner : cellCorners) {
            double projection = corner.dot(axis);
            cellMin = Math.min(cellMin, projection);
            cellMax = Math.max(cellMax, projection);
        }

        return hitBoxMax < cellMin || cellMax < hitBoxMin;
    }
}
