package nurgling.navigation;

import haven.Coord;
import haven.Coord2d;
import nurgling.NHitBox;
import nurgling.pf.NHitBoxD;

public class ChunkNavRecorderRasterizationTest {
    public static void main(String[] args) {
        chunkCellOverlapUsesRotatedHitboxShape();
        chunkCellOverlapDoesNotUseCircumscribedAabbOnly();
        playerPositionUsesChunkNavCellScale();
    }

    private static void chunkCellOverlapUsesRotatedHitboxShape() {
        NHitBox hitBox = new NHitBox(new Coord2d(-1.0, -10.0), new Coord2d(1.0, 10.0), true);
        NHitBoxD rotated = new NHitBoxD(hitBox, new Coord2d(0.0, 0.0), Math.toRadians(11.0));

        if (!ChunkNavRecorder.cellOverlapsHitBox(rotated, 0, -1)) {
            throw new AssertionError("Chunk cell must include real rotated hitbox overlap at the edge");
        }
    }

    private static void chunkCellOverlapDoesNotUseCircumscribedAabbOnly() {
        NHitBox hitBox = new NHitBox(new Coord2d(-1.0, -10.0), new Coord2d(1.0, 10.0), true);
        NHitBoxD rotated = new NHitBoxD(hitBox, new Coord2d(0.0, 0.0), Math.toRadians(45.0));

        if (ChunkNavRecorder.cellOverlapsHitBox(rotated, -2, -2)) {
            throw new AssertionError("Chunk rastering must not mark cells that only overlap the rotated hitbox AABB");
        }
    }

    private static void playerPositionUsesChunkNavCellScale() {
        Coord expected = new Coord(100 * ChunkNavConfig.CELLS_PER_TILE, 0);
        if (!ChunkNavRecorder.worldToChunkCell(Coord2d.of(1100.0, 0.0)).equals(expected)) {
            throw new AssertionError("Chunk-nav player cells must use 2 cells per tile, not local PF's 4-step grid");
        }
    }
}
