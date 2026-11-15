package de.begleiter.client;

import de.begleiter.brain.Terrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Beantwortet die Bodenabfragen des Verhaltens mit der Client-Welt. Der Begleiter ist keine echte
 * Entität, deshalb prüfen wir Kollisionsformen selbst. Es gibt nur lesende Zugriffe.
 */
final class LevelTerrain implements Terrain {
    private static final double[][] RING = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {0.7, 0.7}, {0.7, -0.7}, {-0.7, 0.7}, {-0.7, -0.7}
    };

    private Level level;
    private double radius = 0.3;
    private double height = 0.8;

    LevelTerrain use(Level level) {
        this.level = level;
        return this;
    }

    @Override
    public void setBody(double radius, double height) {
        this.radius = Math.max(0.12, radius);
        this.height = Math.max(0.3, height);
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

    /** Stufen bis 1.2 Blöcke, kein tiefer Abgrund (> 3), kein Wasser/Lava, Körper passt hinein. */
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
        if (!level.getFluidState(feet).isEmpty()) {
            return false;
        }
        // Der Boden unter dem Körperumriss darf höher liegen (Stufe), darüber muss Platz sein
        double floor = g;
        for (double[] d : RING) {
            double fg = findGround(nx + d[0] * radius, curY, nz + d[1] * radius);
            if (!Double.isNaN(fg) && fg > floor && fg - g <= 1.2) {
                floor = fg;
            }
        }
        return bodyFree(nx, floor, nz, height);
    }

    @Override
    public double waterSurface(double x, double y, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int top = Mth.floor(y + 0.2);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int by = top; by >= top - 1; by--) {
            pos.set(bx, by, bz);
            if (!level.hasChunkAt(pos)) {
                return Double.NaN;
            }
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                // nach oben bis zur Oberfläche gehen
                int cy = by;
                for (int i = 0; i < 24; i++) {
                    pos.set(bx, cy + 1, bz);
                    if (!level.getFluidState(pos).is(FluidTags.WATER)) {
                        break;
                    }
                    cy++;
                }
                pos.set(bx, cy, bz);
                FluidState fs = level.getFluidState(pos);
                return cy + fs.getHeight(level, pos);
            }
        }
        return Double.NaN;
    }

    @Override
    public boolean canSwim(double nx, double curY, double nz) {
        double ws = waterSurface(nx, curY, nz);
        if (Double.isNaN(ws)) {
            return false;
        }
        double base = ws - 0.35;
        return bodyFree(nx, base, nz, height * 0.7);
    }

    @Override
    public boolean clearance(double x, double y, double z, double r, double h) {
        double oldR = radius;
        radius = r;
        boolean free = bodyFree(x, y, z, h);
        radius = oldR;
        return free;
    }

    /** Prüft Mitte und Umriss auf drei Höhen: kein Punkt des Körpers darf in einer Kollisionsform liegen. */
    private boolean bodyFree(double x, double y, double z, double h) {
        double[] hs = {y + 0.08, y + h * 0.5, y + h - 0.05};
        for (double sy : hs) {
            if (solidAt(x, sy, z)) {
                return false;
            }
            for (double[] d : RING) {
                if (solidAt(x + d[0] * radius, sy, z + d[1] * radius)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean solidAt(double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!level.hasChunkAt(pos)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            return false;
        }
        for (AABB box : shape.toAabbs()) {
            if (box.move(pos).contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }
}
