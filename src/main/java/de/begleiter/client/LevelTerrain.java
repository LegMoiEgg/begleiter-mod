package de.begleiter.client;

import de.begleiter.brain.Terrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Beantwortet die Bodenabfragen des Verhaltens mit der Client-Welt. Der Begleiter ist keine echte
 * Entität, deshalb prüfen wir Kollisionsformen selbst. Es gibt nur lesende Zugriffe.
 */
final class LevelTerrain implements Terrain {
    private Level level;

    LevelTerrain use(Level level) {
        this.level = level;
        return this;
    }

    @Override
    public double findGround(double x, double y, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int startY = Mth.floor(y + 1.25);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int by = startY; by >= startY - 10; by--) {
            pos.set(bx, by, bz);
            if (!level.hasChunkAt(pos)) {
                return Double.NaN;
            }
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return by + shape.max(Direction.Axis.Y);
            }
        }
        return Double.NaN;
    }

    /** Stufen bis 1.2 Blöcke, kein tiefer Abgrund (> 3), kein Wasser/Lava. */
    @Override
    public boolean canStand(double nx, double curY, double nz) {
        double g = findGround(nx, curY, nz);
        if (Double.isNaN(g)) {
            return false;
        }
        if (g - curY > 1.2 || g < curY - 3.0) {
            return false;
        }
        BlockPos feet = BlockPos.containing(nx, g + 0.05, nz);
        return level.getFluidState(feet).isEmpty();
    }
}
