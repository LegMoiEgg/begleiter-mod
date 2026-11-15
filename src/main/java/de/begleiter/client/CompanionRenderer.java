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
    private Display.BlockDisplay seat;
    private float avatarYaw;
    private double prevX = Double.NaN, prevZ;
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
        // Neigung (z. B. auf dem Kopf des Spielers) um die Querachse des Tieres, vor der Blickrichtung
        Rigid tiltRot = new Rigid();
        tiltRot.qx = (float) Math.sin(brain.tilt * 0.5f);
        tiltRot.qw = (float) Math.cos(brain.tilt * 0.5f);
        float[] tiltQ = {tiltRot.qx, tiltRot.qy, tiltRot.qz, tiltRot.qw};

        for (int i = 0; i < displays.size(); i++) {
            CompanionModel.Part part = model.parts.get(i);
            Rigid g = rigids[part.group().ordinal()];

            // Eckpunkt (min) des Quaders durch Gruppen- und Blickrichtungs-Drehung
            float[] corner = {part.x(), part.y(), part.z()};
            g.apply(corner);
            tiltRot.rotate(corner);
            root.rotate(corner);

            float[] q = Rigid.mul(rootQ, Rigid.mul(tiltQ, new float[] {g.qx, g.qy, g.qz, g.qw}));

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
            avatar.stopRiding();
            avatar.discard();
            avatar = null;
        }
        if (seat != null) {
            seat.discard();
            seat = null;
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
            return avatar == null || avatar.isRemoved() || seat == null || seat.isRemoved() || builtFor != level || builtRevision != cfg.revision;
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
            prevX = Double.NaN;
            // Unsichtbarer "Sitz" (Block-Display ohne Block): wer darauf sitzt, bekommt die Sitzhaltung
            seat = new Display.BlockDisplay(blockDisplayType(), clientLevel);
            seat.setId(nextId--);
            clientLevel.addEntity(seat);
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
        if (avatar == null || seat == null) {
            return;
        }

        // Sitzen: Das Modell sitzt auf dem unsichtbaren Sitz, dann nimmt es die Sitzhaltung eines Passagiers
        // ein (Beine nach vorn). Der Sitz liegt auf Bodenhöhe; die Hüfte des Spielermodells liegt dann
        // knapp darüber.
        boolean sitting = brain.activity.isSitting() || brain.activity == de.begleiter.model.Activity.SLEEP;
        // Auf dem Kopf sitzt das Spielermodell etwas höher, damit die Beine nicht im Kopf stecken
        double raise = brain.command == CompanionBrain.Command.HEAD ? 0.10 : 0.0;
        if (sitting) {
            seat.setPos(brain.x, brain.y + raise, brain.z);
            if (!avatar.isPassenger()) {
                avatar.startRiding(seat, true, false);
            }
        } else {
            if (avatar.isPassenger()) {
                avatar.stopRiding();
            }
            avatar.setPos(brain.x, brain.y, brain.z);
        }

        // Laufanimation: Das Modell wird nicht von Minecraft bewegt, also geben wir die Geschwindigkeit vor
        double moved = Double.isNaN(prevX) ? 0 : Math.hypot(brain.x - prevX, brain.z - prevZ);
        prevX = brain.x;
        prevZ = brain.z;
        avatar.walkAnimation.setSpeed(sitting ? 0f : (float) Math.min(moved * 4.0, 1.0));

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
        float pitch = (float) Math.toDegrees(brain.headPitchAngle());
        if (brain.activity == de.begleiter.model.Activity.SLEEP) {
            pitch = 45f; // schlafend: Kopf hängt nach unten
        }
        avatar.setXRot(pitch);
        avatar.setShiftKeyDown(false);
    }

    @SuppressWarnings("unchecked")
    private static EntityType<Display.BlockDisplay> blockDisplayType() {
        return (EntityType<Display.BlockDisplay>) BuiltInRegistries.ENTITY_TYPE
            .getValue(Identifier.withDefaultNamespace("block_display"));
    }
}
