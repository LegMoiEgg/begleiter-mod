package de.begleiter.client;

import com.mojang.math.Transformation;
import de.begleiter.brain.CompanionBrain;
import de.begleiter.config.BegleiterConfig;
import de.begleiter.mixin.BlockDisplayAccessor;
import de.begleiter.mixin.DisplayAccessor;
import de.begleiter.model.CompanionModel;
import de.begleiter.model.Group;
import de.begleiter.model.Models;
import de.begleiter.model.Rigid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Stellt den Begleiter mit Block-Display-Entities dar, die nur im Client existieren (nichts wird an den
 * Server gesendet). Jeder Quader des Modells ist ein Block-Display, dessen Transformation jeden Tick
 * aus der aktuellen Pose berechnet wird. So übernimmt die normale Entity-Darstellung von Minecraft
 * Beleuchtung, Transparenz und Sichtbarkeit.
 */
final class CompanionRenderer {
    /** Negative IDs, damit sie nie mit IDs echter Server-Entitäten kollidieren. */
    private static int nextId = -2_000_000;

    private final List<Display.BlockDisplay> displays = new ArrayList<>();
    private final Rigid[] rigids = new Rigid[Group.COUNT];
    private CompanionModel model;
    private Level builtFor;
    private int builtRevision = -1;

    void update(Level level, CompanionBrain brain, BegleiterConfig cfg) {
        if (needsRebuild(level, cfg)) {
            rebuild(level, cfg);
        }
        if (model == null || displays.isEmpty()) {
            return;
        }

        model.computeRigids(brain.pose, rigids);
        float k = cfg.size / 16f;

        Rigid root = new Rigid();
        root.qy = (float) Math.sin(brain.facing * 0.5f);
        root.qw = (float) Math.cos(brain.facing * 0.5f);
        float[] rootQ = {root.qx, root.qy, root.qz, root.qw};

        for (int i = 0; i < displays.size(); i++) {
            CompanionModel.Part part = model.parts.get(i);
            Rigid g = rigids[part.group().ordinal()];

            // Eckpunkt (min) des Quaders durch Gruppen- und Blickrichtungs-Drehung
            float[] corner = {part.x(), part.y(), part.z()};
            g.apply(corner);
            root.rotate(corner);

            float[] q = Rigid.mul(rootQ, new float[] {g.qx, g.qy, g.qz, g.qw});

            Transformation transformation = new Transformation(
                new Vector3f(corner[0] * k, corner[1] * k, corner[2] * k),
                new Quaternionf(q[0], q[1], q[2], q[3]),
                new Vector3f(part.w() * k, part.h() * k, part.d() * k),
                new Quaternionf());

            Display.BlockDisplay display = displays.get(i);
            ((DisplayAccessor) (Object) display).begleiter$setTransformation(transformation);
            display.setPos(brain.x, brain.y, brain.z);
        }
    }

    /** Entfernt alle Entities (z. B. beim Verlassen der Welt oder wenn der Begleiter ausgeschaltet wird). */
    void discard() {
        for (Display.BlockDisplay d : displays) {
            d.discard();
        }
        displays.clear();
        builtFor = null;
        model = null;
    }

    /** Aktuelle Trefferbox-Größen in Blöcken, für die Interaktion. */
    double hitRadius(BegleiterConfig cfg) {
        return Models.of(cfg.animal()).hitRadius / 16.0 * cfg.size * 1.15;
    }

    double hitHeight(BegleiterConfig cfg) {
        return Models.of(cfg.animal()).hitHeight / 16.0 * cfg.size * 1.15;
    }

    private boolean needsRebuild(Level level, BegleiterConfig cfg) {
        if (displays.isEmpty() || builtFor != level || builtRevision != cfg.revision) {
            return true;
        }
        for (Display.BlockDisplay d : displays) {
            if (d.isRemoved()) {
                return true;
            }
        }
        return false;
    }

    private void rebuild(Level level, BegleiterConfig cfg) {
        discard();
        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }
        model = Models.of(cfg.animal());
        for (CompanionModel.Part part : model.parts) {
            Display.BlockDisplay display = new Display.BlockDisplay(blockDisplayType(), clientLevel);
            ((BlockDisplayAccessor) (Object) display).begleiter$setBlockState(
                BlockPalette.block(part.slot(), cfg.primaryColor, cfg.secondaryColor));
            display.setId(nextId--);
            clientLevel.addEntity(display);
            displays.add(display);
        }
        builtFor = level;
        builtRevision = cfg.revision;
    }

    @SuppressWarnings("unchecked")
    private static EntityType<Display.BlockDisplay> blockDisplayType() {
        return (EntityType<Display.BlockDisplay>) BuiltInRegistries.ENTITY_TYPE
            .getValue(Identifier.withDefaultNamespace("block_display"));
    }
}
