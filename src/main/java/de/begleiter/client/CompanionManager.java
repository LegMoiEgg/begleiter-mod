package de.begleiter.client;

import de.begleiter.brain.CompanionBrain;
import de.begleiter.config.BegleiterConfig;
import de.begleiter.model.AnimalType;
import java.util.Optional;
import java.util.Random;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Verbindet Verhalten ({@link CompanionBrain}) und Darstellung ({@link CompanionRenderer}) mit dem
 * Client-Tick und kümmert sich um die Interaktion (Rechtsklick mit leerer Hand).
 */
public final class CompanionManager {
    private static final CompanionBrain BRAIN = new CompanionBrain();
    private static final CompanionRenderer RENDERER = new CompanionRenderer();
    private static final LevelTerrain TERRAIN = new LevelTerrain();
    private static final Random RANDOM = new Random();
    private static Level lastLevel;
    private static boolean wasUse;
    private static long lastInteract = -100;

    private CompanionManager() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(CompanionManager::tick);
        UseItemCallback.EVENT.register((player, level, hand) -> onUse(player, level, hand));
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> onUse(player, level, hand));
    }

    private static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        BegleiterConfig cfg = BegleiterConfig.get();

        if (level == null || player == null || !cfg.enabled) {
            RENDERER.discard();
            BRAIN.reset();
            lastLevel = null;
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        if (level != lastLevel) {
            RENDERER.discard();
            BRAIN.reset();
            lastLevel = level;
        }

        BRAIN.scale = cfg.size;
        BRAIN.setPlayerState(player.onGround(), player.isInWater(), player.getBoundingBox().maxY);
        BRAIN.tick(TERRAIN.use(level), player.getX(), player.getY(), player.getZ(),
            player.getYRot(), player.getXRot(), player.getEyeY(), cfg.animal());
        RENDERER.update(level, BRAIN, cfg);

        // Ein Rechtsklick mit leerer Hand in die Luft löst in Minecraft kein Ereignis aus. Deshalb wird die
        // Taste zusätzlich selbst abgefragt (die Ereignisse oben schlucken den Klick, wenn sie greifen).
        boolean use = mc.options.keyUse.isDown();
        if (use && !wasUse) {
            interact(player, level);
        }
        wasUse = use;

        if (BRAIN.consumeTeleported()) {
            for (int i = 0; i < 6; i++) {
                level.addParticle(ParticleTypes.POOF, BRAIN.x + (RANDOM.nextDouble() - 0.5) * 0.4,
                    BRAIN.y + 0.2 + RANDOM.nextDouble() * 0.3, BRAIN.z + (RANDOM.nextDouble() - 0.5) * 0.4,
                    0.0, 0.02, 0.0);
            }
        }
    }

    /**
     * Rechtsklick mit leerer Haupthand auf den Begleiter: streicheln. Mit Schleichen: "Platz!" / "Komm!".
     * Wirkt nur, wenn der Begleiter angeschaut wird und kein Block näher ist, damit nichts anderes blockiert wird.
     */
    private static InteractionResult onUse(Player player, Level level, InteractionHand hand) {
        if (!level.isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        return interact(player, level) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    /** Gemeinsame Logik für Klick-Ereignis und Tastenabfrage; gibt zurück, ob der Klick benutzt wurde. */
    private static boolean interact(Player player, Level level) {
        BegleiterConfig cfg = BegleiterConfig.get();
        if (!cfg.enabled || !BRAIN.initialized || !player.getMainHandItem().isEmpty()) {
            return false;
        }
        long now = level.getGameTime();
        if (now - lastInteract < 4) {
            return false;
        }
        // Der Begleiter sitzt auf dem Kopf: Schleichen + Rechtsklick (leere Hand) holt ihn herunter
        if (BRAIN.command == CompanionBrain.Command.HEAD) {
            if (player.isShiftKeyDown()) {
                lastInteract = now;
                BRAIN.releaseHead();
                playSound(level, cfg.animal(), 1.2f);
                return true;
            }
            return false;
        }
        if (!isLookingAtCompanion(player, cfg)) {
            return false;
        }
        lastInteract = now;

        AnimalType type = cfg.animal();
        double top = BRAIN.y + RENDERER.hitHeight(cfg);
        if (player.isShiftKeyDown()) {
            CompanionBrain.Command cmd = BRAIN.cycleCommand();
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, BRAIN.x + (RANDOM.nextDouble() - 0.5) * 0.5,
                    top - 0.1 + RANDOM.nextDouble() * 0.2, BRAIN.z + (RANDOM.nextDouble() - 0.5) * 0.5,
                    0.0, 0.0, 0.0);
            }
            if (cmd != CompanionBrain.Command.SIT) {
                playSound(level, type, 1.2f);
            }
        } else {
            BRAIN.pet();
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.HEART, BRAIN.x + (RANDOM.nextDouble() - 0.5) * 0.5,
                    top + RANDOM.nextDouble() * 0.2, BRAIN.z + (RANDOM.nextDouble() - 0.5) * 0.5,
                    0.0, 0.05, 0.0);
            }
            playSound(level, type, 1.0f);
        }
        return true;
    }

    private static boolean isLookingAtCompanion(Player player, BegleiterConfig cfg) {
        double r = RENDERER.hitRadius(cfg);
        double h = RENDERER.hitHeight(cfg);
        AABB box = new AABB(BRAIN.x - r, BRAIN.y, BRAIN.z - r, BRAIN.x + r, BRAIN.y + h, BRAIN.z + r);

        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(4.5));
        Optional<Vec3> hit = box.clip(eye, end);
        if (hit.isEmpty()) {
            return false;
        }

        // Ein Block, der näher ist als der Begleiter, hat Vorrang.
        HitResult target = Minecraft.getInstance().hitResult;
        if (target != null && target.getType() == HitResult.Type.BLOCK) {
            return eye.distanceToSqr(hit.get()) < eye.distanceToSqr(target.getLocation());
        }
        return true;
    }

    /** Geräusche aus dem Spiel, über die Sound-ID gesucht (fehlt eine ID, wird ein Ersatz benutzt). */
    private static void playSound(Level level, AnimalType type, float pitch) {
        String id = switch (type) {
            case CAT -> RANDOM.nextInt(3) == 0 ? "entity.cat.purreow" : "entity.cat.purr";
            case DOG -> "entity.wolf.pant";
            case RABBIT -> "entity.rabbit.jump";
            case FOX -> "entity.fox.ambient";
            case HUMAN -> "entity.villager.yes";
        };
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.withDefaultNamespace(id));
        if (sound == null) {
            sound = SoundEvents.FOX_AMBIENT;
        }
        level.playLocalSound(BRAIN.x, BRAIN.y + 0.3, BRAIN.z, sound, SoundSource.NEUTRAL,
            0.6f, pitch * (0.9f + RANDOM.nextFloat() * 0.2f), false);
    }
}
