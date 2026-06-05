package nurgling.pf;

import haven.*;
import nurgling.NHitBox;

public class NHitBoxD implements Comparable<NHitBoxD>, java.io.Serializable {
// ul  0
//     _____
//   3|    |1
//    |____|
//       2   br

    // core hitbox data
    public final Coord2d ul, br;
    public final Coord2d rc;
    public final double angle;

    // secondary data
    final double sn, cs;
    public final Coord2d[] n;
    public final double[] d;
    // corners ul=>ur=>br=>bl
    public final Coord2d[] c;
    public final Coord2d[] checkPoints;
    final boolean ortho;
    final boolean primitive;

    public NHitBoxD(Coord rc) {
        this(MCache.tileqsz.sub(MCache.tilehsz), MCache.tileqsz, Utils.pfGridToWorld(rc), 0, true);
    }

    public NHitBoxD(Coord2d ul) {
        this(ul, ul.add(MCache.tilehsz), null, 0, true);
    }

    public NHitBoxD(Coord2d ul, Coord2d br) {
        // TODO empty hitbox center
        // TODO rotten log no hitbox
        this(ul, br, null, 0, false);
    }

    public NHitBoxD(Coord ul, Coord br) {
        this(Utils.pfGridToWorld(ul).sub(MCache.tileqsz), Utils.pfGridToWorld(br).add(MCache.tileqsz));
    }

    public NHitBoxD(Coord2d ul, Coord2d br, Coord2d r) {
        this(ul, br, r, 0, false);
    }

    public NHitBoxD(Gob gob) {
        this(gob.ngob.hitBox, gob.rc, gob.a);
    }

    public NHitBoxD(NHitBox hb, Coord2d r, double angle) {
        this(hb.begin, hb.end, r, angle);
    }

    public NHitBoxD(Coord2d ul, Coord2d br, Coord2d r, double angle) {
        this(ul, br, r, angle, false);
    }

    private NHitBoxD(Coord2d ul, Coord2d br, Coord2d r, double angle, boolean primitive) {
        Coord2d min = Coord2d.of(Math.min(ul.x, br.x), Math.min(ul.y, br.y));
        Coord2d max = Coord2d.of(Math.max(ul.x, br.x), Math.max(ul.y, br.y));
        Coord2d center = (r == null) ? Coord2d.of((min.x + max.x) / 2, (min.y + max.y) / 2) : Coord2d.of(r.x, r.y);

        this.ul = Coord2d.of(min.x - ((r == null) ? center.x : 0), min.y - ((r == null) ? center.y : 0));
        this.br = Coord2d.of(max.x - ((r == null) ? center.x : 0), max.y - ((r == null) ? center.y : 0));
        this.rc = center;
        this.primitive = primitive;

        double kPi = ((2 * angle) / Math.PI);
        boolean cardinal = Math.abs(kPi - Math.rint(kPi)) <= 0.0001;
        if (cardinal) {
            int quarterTurns = (int) Math.round(kPi);
            this.angle = quarterTurns * Math.PI / 2.0;
            this.sn = 0;
            this.cs = 1;
            this.n = new Coord2d[]{Coord2d.of(0, 1), Coord2d.of(-1, 0), Coord2d.of(0, -1), Coord2d.of(1, 0)};
            this.d = new double[]{0, 0, 0, 0};
            this.c = orthoCorners(this.ul, this.br, this.rc, quarterTurns);
            this.ortho = true;
            this.checkPoints = null;
        } else {
            this.angle = angle;
            this.sn = Math.sin(angle);
            this.cs = Math.cos(angle);
            this.n = new Coord2d[]{
                    Coord2d.of(-sn, cs),
                    Coord2d.of(-cs, -sn),
                    Coord2d.of(sn, -cs),
                    Coord2d.of(cs, sn)
            };
            this.c = new Coord2d[]{
                    this.ul.rot(angle).add(this.rc),
                    Coord2d.of(this.br.x, this.ul.y).rot(angle).add(this.rc),
                    this.br.rot(angle).add(this.rc),
                    Coord2d.of(this.ul.x, this.br.y).rot(angle).add(this.rc)
            };
            this.d = new double[4];
            for (int ind = 0; ind < 4; ind++) {
                this.d[ind] = this.n[ind].dot(this.c[ind]);
            }
            this.ortho = false;
            this.checkPoints = computeCheckPoints(primitive, this.ortho, this.c);
        }
    }

    public static NHitBoxD shaftBoxObjectFactory(Coord2d begin, Coord2d end, double halfWidth) {
        // TODO refactor
        double halfLength = begin.dist(end) / 2;
        return new NHitBoxD(Coord2d.of(-halfLength, -halfWidth), Coord2d.of(halfLength, halfWidth), begin.add(end).div(2), end.angle(begin));
    }

    private static Coord2d[] orthoCorners(Coord2d ul, Coord2d br, Coord2d rc, int quarterTurns) {
        switch (Math.floorMod(quarterTurns, 4)) {
            case 0:
                return new Coord2d[]{
                        ul.add(rc),
                        Coord2d.of(br.x, ul.y).add(rc),
                        br.add(rc),
                        Coord2d.of(ul.x, br.y).add(rc)
                };
            case 1:
                return new Coord2d[]{
                        Coord2d.of(-br.y, ul.x).add(rc),
                        Coord2d.of(-ul.y, ul.x).add(rc),
                        Coord2d.of(-ul.y, br.x).add(rc),
                        Coord2d.of(-br.y, br.x).add(rc)
                };
            case 2:
                return new Coord2d[]{
                        Coord2d.of(-br.x, -br.y).add(rc),
                        Coord2d.of(-ul.x, -br.y).add(rc),
                        Coord2d.of(-ul.x, -ul.y).add(rc),
                        Coord2d.of(-br.x, -ul.y).add(rc)
                };
            case 3:
                return new Coord2d[]{
                        Coord2d.of(ul.y, -br.x).add(rc),
                        Coord2d.of(br.y, -br.x).add(rc),
                        Coord2d.of(br.y, -ul.x).add(rc),
                        Coord2d.of(ul.y, -ul.x).add(rc)
                };
        }
        throw new IllegalArgumentException("Invalid quarter turn count: " + quarterTurns);
    }

    private static Coord2d[] computeCheckPoints(boolean primitive, boolean ortho, Coord2d[] c) {
        if (primitive || ortho) return null;

        double xRange = c[0].dist(c[1]);
        double yRange = c[0].dist(c[3]);
        int xCnt = (int) Math.floor(xRange / MCache.tilehsz.x);
        int yCnt = (int) Math.floor(yRange / MCache.tilehsz.x);
        if ((xCnt + yCnt) == 0) return new Coord2d[0];

        Coord2d[] checkPoints = new Coord2d[2 * (xCnt + yCnt)];
        for (int i = 0; i < xCnt; i++) {
            checkPoints[i] = c[0].mul((double) (i + 1) / (xCnt + 1)).add(c[1].mul((double) (xCnt - i) / (xCnt + 1)));
            checkPoints[i + xCnt] = c[3].mul((double) (i + 1) / (xCnt + 1)).add(c[2].mul((double) (xCnt - i) / (xCnt + 1)));
        }
        for (int i = 0; i < yCnt; i++) {
            checkPoints[2 * xCnt + i] = c[0].mul((double) (i + 1) / (xCnt + 1)).add(c[3].mul((double) (xCnt - i) / (xCnt + 1)));
            checkPoints[2 * xCnt + i + yCnt] = c[1].mul((double) (i + 1) / (xCnt + 1)).add(c[2].mul((double) (xCnt - i) / (xCnt + 1)));
        }
        return checkPoints;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NHitBoxD)) {
            return (false);
        }
        NHitBoxD a = (NHitBoxD) o;
        return (a.ul.equals(ul) && a.br.equals(br) && a.rc.equals(rc) && (a.angle == this.angle));
    }

    public int hashCode() {
        int X = ul.hashCode() / 2;
        int Y = br.hashCode() / 2;
        return X + Y;
    }

    public int compareTo(NHitBoxD c) {
        return br.equals(c.br) ? ul.compareTo(c.ul) : br.compareTo(c.br);
    }

    public void reCalc_dv() {

    }

    public Coord2d projectCenter(Coord2d direction) {
        // TODO refactor
        double tau = Float.MAX_VALUE;
        for (int k = 0; k < 4; k++) {
            if (Math.abs(direction.dot(this.n[k])) > 0.0001) {
                double tauTemp = Math.min(tau, (d[k] - rc.dot(n[k])) / direction.dot(n[k]));
                if (tauTemp > 0) tau = tauTemp;
            }
        }
        return rc.add(direction.mul(tau));
    }

    public Coord2d getCircumscribedUL() {
        double mx = Double.MAX_VALUE;
        double my = Double.MAX_VALUE;
        for (int ind = 0; ind < 4; ind++) {
            if (c[ind].x < mx)
                mx = c[ind].x;
            if (c[ind].y < my)
                my = c[ind].y;
        }
        return new Coord2d(mx, my);
    }

    public Coord2d getCircumscribedBR() {
        double mx = -Double.MAX_VALUE;
        double my = -Double.MAX_VALUE;
        for (int ind = 0; ind < 4; ind++) {
            if (c[ind].x > mx)
                mx = c[ind].x;
            if (c[ind].y > my)
                my = c[ind].y;
        }
        return new Coord2d(mx, my);
    }

    public double diag() {
        return br.dist(ul);
    }

    public Coord2d sz() {
        return br.sub(ul);
    }

    public boolean containsSemiOpen(Coord2d c) {
        if (!ortho) {
            return (n[0].dot(c) >= d[0]) && (n[1].dot(c) > d[1]) && (n[2].dot(c) > d[2]) && (n[3].dot(c) >= d[3]);
        } else {
            return ((c.x >= this.c[0].x) && (c.y >= this.c[0].y) && (c.x < this.c[2].x) && (c.y < this.c[2].y));
        }
    }

    public boolean contains(Coord2d c, boolean includeBorder) {
        if (!ortho) {
            if (includeBorder)
                return (n[0].dot(c) >= d[0]) && (n[1].dot(c) >= d[1]) && (n[2].dot(c) >= d[2]) && (n[3].dot(c) >= d[3]);
            else
                return (n[0].dot(c) > d[0]) && (n[1].dot(c) > d[1]) && (n[2].dot(c) > d[2]) && (n[3].dot(c) > d[3]);
        } else {
            if (includeBorder)
                return ((c.x >= this.c[0].x) && (c.y >= this.c[0].y) && (c.x <= this.c[2].x) && (c.y <= this.c[2].y));
            else
                return ((c.x > this.c[0].x) && (c.y > this.c[0].y) && (c.x < this.c[2].x) && (c.y < this.c[2].y));
        }
    }

    public boolean intersects(NHitBoxD other, boolean includeBorder) {
        if (ortho && other.ortho) {
            return overlapsAxisAligned(c[0].x, c[0].y, c[2].x, c[2].y, other.c[0].x, other.c[0].y, other.c[2].x, other.c[2].y, includeBorder);
        }

        for (int i = 0; i < 4; i++) {
            double axisX = -(c[(i + 1) % 4].y - c[i].y);
            double axisY = c[(i + 1) % 4].x - c[i].x;
            if (isSeparated(axisX, axisY, c, other.c, includeBorder))
                return false;
        }

        for (int i = 0; i < 4; i++) {
            double axisX = -(other.c[(i + 1) % 4].y - other.c[i].y);
            double axisY = other.c[(i + 1) % 4].x - other.c[i].x;
            if (isSeparated(axisX, axisY, c, other.c, includeBorder))
                return false;
        }

        return true;
    }

    public boolean intersectsAxisAlignedRect(double minX, double minY, double maxX, double maxY, boolean includeBorder) {
        double rectMinX = Math.min(minX, maxX);
        double rectMinY = Math.min(minY, maxY);
        double rectMaxX = Math.max(minX, maxX);
        double rectMaxY = Math.max(minY, maxY);

        if (ortho) {
            return overlapsAxisAligned(c[0].x, c[0].y, c[2].x, c[2].y, rectMinX, rectMinY, rectMaxX, rectMaxY, includeBorder);
        }

        for (int i = 0; i < 4; i++) {
            double axisX = -(c[(i + 1) % 4].y - c[i].y);
            double axisY = c[(i + 1) % 4].x - c[i].x;
            if (isSeparated(axisX, axisY, c, rectMinX, rectMinY, rectMaxX, rectMaxY, includeBorder))
                return false;
        }

        if (isRangeSeparated(minCornerX(c), maxCornerX(c), rectMinX, rectMaxX, includeBorder))
            return false;
        return !isRangeSeparated(minCornerY(c), maxCornerY(c), rectMinY, rectMaxY, includeBorder);
    }

    private static boolean overlapsAxisAligned(double firstMinX, double firstMinY, double firstMaxX, double firstMaxY,
                                              double secondMinX, double secondMinY, double secondMaxX, double secondMaxY,
                                              boolean includeBorder) {
        double aMinX = Math.min(firstMinX, firstMaxX);
        double aMinY = Math.min(firstMinY, firstMaxY);
        double aMaxX = Math.max(firstMinX, firstMaxX);
        double aMaxY = Math.max(firstMinY, firstMaxY);
        double bMinX = Math.min(secondMinX, secondMaxX);
        double bMinY = Math.min(secondMinY, secondMaxY);
        double bMaxX = Math.max(secondMinX, secondMaxX);
        double bMaxY = Math.max(secondMinY, secondMaxY);

        return !isRangeSeparated(aMinX, aMaxX, bMinX, bMaxX, includeBorder) &&
                !isRangeSeparated(aMinY, aMaxY, bMinY, bMaxY, includeBorder);
    }

    private static boolean isSeparated(double axisX, double axisY, Coord2d[] first, Coord2d[] second, boolean includeBorder) {
        if (axisX == 0 && axisY == 0)
            return false;

        double firstMin = Double.MAX_VALUE;
        double firstMax = -Double.MAX_VALUE;
        for (Coord2d corner : first) {
            double projection = corner.x * axisX + corner.y * axisY;
            firstMin = Math.min(firstMin, projection);
            firstMax = Math.max(firstMax, projection);
        }

        double secondMin = Double.MAX_VALUE;
        double secondMax = -Double.MAX_VALUE;
        for (Coord2d corner : second) {
            double projection = corner.x * axisX + corner.y * axisY;
            secondMin = Math.min(secondMin, projection);
            secondMax = Math.max(secondMax, projection);
        }

        return isRangeSeparated(firstMin, firstMax, secondMin, secondMax, includeBorder);
    }

    private static boolean isSeparated(double axisX, double axisY, Coord2d[] first,
                                       double rectMinX, double rectMinY, double rectMaxX, double rectMaxY,
                                       boolean includeBorder) {
        if (axisX == 0 && axisY == 0)
            return false;

        double firstMin = Double.MAX_VALUE;
        double firstMax = -Double.MAX_VALUE;
        for (Coord2d corner : first) {
            double projection = corner.x * axisX + corner.y * axisY;
            firstMin = Math.min(firstMin, projection);
            firstMax = Math.max(firstMax, projection);
        }

        double secondA = rectMinX * axisX + rectMinY * axisY;
        double secondB = rectMaxX * axisX + rectMinY * axisY;
        double secondC = rectMaxX * axisX + rectMaxY * axisY;
        double secondD = rectMinX * axisX + rectMaxY * axisY;
        double secondMin = Math.min(Math.min(secondA, secondB), Math.min(secondC, secondD));
        double secondMax = Math.max(Math.max(secondA, secondB), Math.max(secondC, secondD));

        return isRangeSeparated(firstMin, firstMax, secondMin, secondMax, includeBorder);
    }

    private static boolean isRangeSeparated(double firstMin, double firstMax, double secondMin, double secondMax, boolean includeBorder) {
        if (includeBorder)
            return firstMax < secondMin || secondMax < firstMin;
        return firstMax <= secondMin || secondMax <= firstMin;
    }

    private static double minCornerX(Coord2d[] corners) {
        double min = Double.MAX_VALUE;
        for (Coord2d corner : corners)
            min = Math.min(min, corner.x);
        return min;
    }

    private static double maxCornerX(Coord2d[] corners) {
        double max = -Double.MAX_VALUE;
        for (Coord2d corner : corners)
            max = Math.max(max, corner.x);
        return max;
    }

    private static double minCornerY(Coord2d[] corners) {
        double min = Double.MAX_VALUE;
        for (Coord2d corner : corners)
            min = Math.min(min, corner.y);
        return min;
    }

    private static double maxCornerY(Coord2d[] corners) {
        double max = -Double.MAX_VALUE;
        for (Coord2d corner : corners)
            max = Math.max(max, corner.y);
        return max;
    }
}
