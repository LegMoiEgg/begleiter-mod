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
import de.begleiter.model.AnimalType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
    private CompanionAvatar avatar;
    private float avatarYaw;
    private int avatarTicks;
    private CompanionModel model;
    private Level builtFor;
    private int builtRevision = -1;

    void update(Level level, CompanionBrain brain, BegleiterConfig cfg) {
        if (needsRebuild(level, cfg)) {
            rebuild(level, cfg);
        }
        if (cfg.animal() == AnimalType.HUMAN) {
            updateAvatar(brain);
            return;
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
        if (avatar != null) {
            avatar.discard();
            avatar = null;
        }
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
        boolean human = cfg.animal() == AnimalType.HUMAN;
        if (human) {
            return avatar == null || avatar.isRemoved() || builtFor != level || builtRevision != cfg.revision;
        }
        if (avatar != null) {
            return true;
        }
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
        if (cfg.animal() == AnimalType.HUMAN) {
            avatar = new CompanionAvatar(clientLevel);
            avatar.setCustomSkin(SkinTextures.get(cfg.skinPath));
            avatar.setId(nextId--);
            AttributeInstance scale = avatar.getAttribute(Attributes.SCALE);
            if (scale != null) {
                // Spielerhöhe 1,8 Blöcke auf die Höhe des Modells herunterskalieren
                scale.setBaseValue(model.hitHeight / 16.0 * cfg.size / 1.8);
            }
            avatarYaw = Float.NaN;
            clientLevel.addEntity(avatar);
            builtFor = level;
            builtRevision = cfg.revision;
            return;
        }
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

    /** Überträgt Position, Blickrichtung und Beschäftigung des Verhaltens auf das Spielermodell. */
    private void updateAvatar(CompanionBrain brain) {
        if (avatar == null) {
            return;
        }
        avatar.setPos(brain.x, brain.y, brain.z);

        // Spielerblickrichtung: Yaw 0 = +Z, nach links (+X) = negativer Yaw. Winkel durchgehend halten,
        // damit es keinen Sprung bei +-180 Grad gibt.
        float target = (float) -Math.toDegrees(brain.facing);
        if (Float.isNaN(avatarYaw)) {
            avatarYaw = target;
        } else {
            float diff = ((target - avatarYaw) % 360f + 540f) % 360f - 180f;
            avatarYaw += diff;
        }
        float headYaw = avatarYaw - (float) Math.toDegrees(brain.headYawAngle());
        avatar.setYRot(avatarYaw);
        avatar.setYBodyRot(avatarYaw);
        avatar.setYHeadRot(headYaw);
        avatar.setXRot((float) Math.toDegrees(brain.headPitchAngle()));

        boolean resting = brain.activity.isSitting() || brain.activity == de.begleiter.model.Activity.SLEEP;
        avatar.setShiftKeyDown(resting);

        avatarTicks++;
        boolean cheering = brain.activity == de.begleiter.model.Activity.PLAY || brain.happy > 0.5f;
        if (cheering && avatarTicks % 8 == 0) {
            avatar.swing(InteractionHand.MAIN_HAND);
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityType<Display.BlockDisplay> blockDisplayType() {
        return (EntityType<Display.BlockDisplay>) BuiltInRegistries.ENTITY_TYPE
            .getValue(Identifier.withDefaultNamespace("block_display"));
    }
}
