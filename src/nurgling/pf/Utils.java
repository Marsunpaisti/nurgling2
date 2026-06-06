package nurgling.pf;

import haven.*;
import nurgling.NUtils;

public class Utils
{
    public static final Coord2d CELL_HALFSZ = Coord2d.of(3.0, 3.0);
    public static final int PF_STEPS_PER_TILE = 4;

    private static final double[] PF_TILE_OFFSETS = {0.0, 3.0, 5.5, 8.0};
    private static final double MIN_PF_STEP = 2.5;
    static final double EPS = 0.000001;

    public static Coord toPfGrid(Coord2d coord)
    {
        return new Coord(worldAxisToPf(coord.x), worldAxisToPf(coord.y));
    }

    public static Coord2d pfGridToWorld(Coord coord)
    {
        return Coord2d.of(pfAxisToWorld(coord.x), pfAxisToWorld(coord.y));
    }

    public static double pfAxisToWorld(int index)
    {
        int tile = Math.floorDiv(index, PF_STEPS_PER_TILE);
        int offset = Math.floorMod(index, PF_STEPS_PER_TILE);
        return tile * MCache.tilesz.x + PF_TILE_OFFSETS[offset];
    }

    public static int worldAxisToPf(double coord)
    {
        int tile = (int) Math.floor(coord / MCache.tilesz.x);
        int best = tile * PF_STEPS_PER_TILE;
        double bestDist = Double.MAX_VALUE;

        // Check adjacent tiles for coordinates near tile boundaries and negative coordinates.
        for(int candidateTile = tile - 1; candidateTile <= tile + 1; candidateTile++) {
            for(int offset = 0; offset < PF_STEPS_PER_TILE; offset++) {
                int candidate = candidateTile * PF_STEPS_PER_TILE + offset;
                double dist = Math.abs(pfAxisToWorld(candidate) - coord);
                if(dist < bestDist) {
                    bestDist = dist;
                    best = candidate;
                }
            }
        }

        return best;
    }

    public static int firstPfAxisAtOrAfter(double coord)
    {
        int index = worldAxisToPf(coord);
        while(pfAxisToWorld(index) < coord - EPS) {
            index++;
        }
        while(pfAxisToWorld(index - 1) >= coord - EPS) {
            index--;
        }
        return index;
    }

    public static int lastPfAxisAtOrBefore(double coord)
    {
        int index = worldAxisToPf(coord);
        while(pfAxisToWorld(index) > coord + EPS) {
            index--;
        }
        while(pfAxisToWorld(index + 1) <= coord + EPS) {
            index++;
        }
        return index;
    }

    public static int maxPfStepsForWorldDistance(double distance)
    {
        return (int) Math.ceil(Math.abs(distance) / MIN_PF_STEP);
    }

    /**
     * Check if a world coordinate is inside the player's 81-tile visible area.
     * Uses the same calculation as ExploredArea and NMiniMap for consistency.
     * 
     * The visible area is calculated as 9x9 grids (each 100 world units) = 900x900 world units,
     * which equals approximately 81x81 tiles (each tile is 11 world units).
     * 
     * @param coord2d the world coordinate to check
     * @return true if inside visible area, false otherwise
     */
    public static boolean inVisibleArea(Coord2d coord2d) {
        Gob player = NUtils.player();
        if(player == null) {
            return false;
        }
        
        // Use the same calculation as NMiniMap and ExploredArea
        // sgridsz = 100 (world units per grid)
        // visible area = 9 grids = 900 world units in each direction
        Coord2d sgridsz = new Coord2d(100, 100);
        
        // Calculate the upper-left corner of visible area (aligned to grid)
        // player.rc.floor(sgridsz) gives the grid the player is in
        // .sub(4, 4) moves to 4 grids before (so player is roughly centered in 9x9 area)
        // .mul(sgridsz) converts back to world coordinates
        Coord2d ul = player.rc.floor(sgridsz).sub(4, 4).mul(sgridsz);
        
        // Calculate the bottom-right corner
        // 9 grids * 100 units = 900 world units
        Coord2d viewSize = sgridsz.mul(9);
        Coord2d br = ul.add(viewSize);
        
        return coord2d.x >= ul.x && coord2d.x < br.x &&
               coord2d.y >= ul.y && coord2d.y < br.y;
    }
}
