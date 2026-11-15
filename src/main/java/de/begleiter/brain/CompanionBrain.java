package de.begleiter.brain;

import de.begleiter.model.Activity;
import de.begleiter.model.AnimInput;
import de.begleiter.model.AnimalType;
import de.begleiter.model.Animator;
import de.begleiter.model.CompanionModel;
import de.begleiter.model.Models;
import de.begleiter.model.Pose;
import java.util.Map;
import java.util.Random;

/**
 * Verhalten und Bewegung des Begleiters, ohne Minecraft-Klassen. Wird einmal pro Client-Tick (20 Hz)
 * aufgerufen. Er ist rein optisch: es wird nichts an den Server gesendet und nichts in der Welt verändert.
 */
public final class CompanionBrain {
    // --- Zustand, den die Darstellung liest ---
    public double x, y, z;
    /** Blickrichtung des Körpers in Radiant: 0 = +Z, positiv = Richtung +X (links vom Tier). */
    public float facing;
    public Activity activity = Activity.FOLLOW;
    /** Aktuelle (geglättete) Pose. */
    public final Pose pose = new Pose();
    public boolean sitCommand;
    public boolean initialized;
    public float happy;

    // --- Ereignisse für Partikel/Ton, werden vom Manager abgeholt ---
    private boolean teleported;

    // --- interner Zustand ---
    private final Pose target = new Pose();
    private final AnimInput in = new AnimInput();
    private final Random rnd = new Random();

    private double lastPx, lastPz;
    private float lastPYaw, lastPPitch;
    private int stillTicks, afkTicks, followTicks, nextIdleAfter = 40;
    private int activityTicks;
    private int subTimer;
    private int wanderTimer, pauseTimer, stuckTicks;
    private int spinTicks, petCount, sinceLastPet = 1000;
    private double goalX, goalZ;
    private double orbitAngle, orbitRadius = 2.0;
    private int orbitDir = 1;
    private double curSpeed, dirX, dirZ = 1;
    private double vy;
    private boolean onGround = true;
    private float animTime, walkPhase, walkAmt, headYaw, headPitch;
    private boolean hopRequested;

    public void reset() {
        initialized = false;
        activity = Activity.FOLLOW;
        sitCommand = false;
        happy = 0;
        curSpeed = 0;
        vy = 0;
        onGround = true;
        stillTicks = 0;
        afkTicks = 0;
        followTicks = 0;
        spinTicks = 0;
        petCount = 0;
        teleported = false;
        pose.clear();
    }

    /** True genau einmal nach einem Teleport (für Wölkchen-Partikel). */
    public boolean consumeTeleported() {
        boolean t = teleported;
        teleported = false;
        return t;
    }

    public void tick(Terrain terrain, double px, double py, double pz, float pYaw, float pPitch,
                     double pEyeY, AnimalType type) {
        CompanionModel model = Models.of(type);
        if (!initialized) {
            placeNear(terrain, px, py, pz, false);
            initialized = true;
            lastPx = px;
            lastPz = pz;
            lastPYaw = pYaw;
            lastPPitch = pPitch;
            facing = (float) Math.atan2(px - x, pz - z);
        }

        // --- Was macht der Spieler? ---
        double pvx = px - lastPx, pvz = pz - lastPz;
        double pSpeed = Math.sqrt(pvx * pvx + pvz * pvz);
        boolean pTeleported = pSpeed > 8;
        boolean moving = pSpeed > 0.025 && !pTeleported;
        boolean rotated = Math.abs(wrapDeg(pYaw - lastPYaw)) > 0.4f || Math.abs(pPitch - lastPPitch) > 0.4f;
        lastPx = px;
        lastPz = pz;
        lastPYaw = pYaw;
        lastPPitch = pPitch;
        sinceLastPet++;

        double dxp = px - x, dzp = pz - z;
        double distP = Math.sqrt(dxp * dxp + dzp * dzp);

        // Zu weit weg, vom Spieler getrennt oder festgesteckt: neben den Spieler setzen
        if (distP > 16 || Math.abs(py - y) > 12 || pTeleported || (stuckTicks > 100 && distP > 4)) {
            placeNear(terrain, px, py, pz, true);
            stuckTicks = 0;
            if (sitCommand) {
                sitCommand = false;
            }
            setActivity(Activity.FOLLOW, 0);
            dxp = px - x;
            dzp = pz - z;
            distP = Math.sqrt(dxp * dxp + dzp * dzp);
        }

        // Aufstehen, wenn der Spieler sich zu weit entfernt
        if (sitCommand && distP > 10) {
            sitCommand = false;
            setActivity(Activity.FOLLOW, 0);
        }

        updateActivity(moving, rotated, type);

        // --- Wohin? ---
        double maxSpeed = 0;
        hopRequested = false;
        switch (activity) {
            case FOLLOW -> {
                if (moving) {
                    orbitAngle += orbitDir * 0.045;
                    if (rnd.nextInt(400) == 0) {
                        orbitDir = -orbitDir;
                    }
                    orbitRadius = 2.0 + 0.7 * Math.sin(orbitAngle * 0.7);
                    goalX = px + Math.sin(orbitAngle) * orbitRadius + pvx * 7;
                    goalZ = pz + Math.cos(orbitAngle) * orbitRadius + pvz * 7;
                    wanderTimer = 0;
                    pauseTimer = 0;
                } else if (pauseTimer > 0) {
                    pauseTimer--;
                    goalX = x;
                    goalZ = z;
                } else {
                    wanderTimer--;
                    if (wanderTimer <= 0) {
                        double ang = rnd.nextDouble() * Math.PI * 2;
                        double r = 1.3 + rnd.nextDouble() * 1.9;
                        goalX = px + Math.sin(ang) * r;
                        goalZ = pz + Math.cos(ang) * r;
                        wanderTimer = 50 + rnd.nextInt(90);
                    }
                    if (dist2(goalX - x, goalZ - z) < 0.3) {
                        pauseTimer = 20 + rnd.nextInt(40);
                        wanderTimer = 0;
                    }
                }
                maxSpeed = distP > 7 ? type.speed * 1.7 : type.speed;
            }
            case SNIFF -> {
                if (--subTimer <= 0) {
                    double ang = rnd.nextDouble() * Math.PI * 2;
                    goalX = x + Math.sin(ang) * 0.7;
                    goalZ = z + Math.cos(ang) * 0.7;
                    subTimer = 25 + rnd.nextInt(30);
                }
                maxSpeed = 0.05;
            }
            case PLAY -> {
                if (--subTimer <= 0) {
                    double ang = rnd.nextDouble() * Math.PI * 2;
                    double r = 1.2 + rnd.nextDouble() * 1.4;
                    goalX = px + Math.sin(ang) * r;
                    goalZ = pz + Math.cos(ang) * r;
                    subTimer = 18 + rnd.nextInt(16);
                    hopRequested = true;
                }
                maxSpeed = type.speed * 0.8;
            }
            default -> {
                goalX = x;
                goalZ = z;
            }
        }

        moveToward(terrain, goalX, goalZ, maxSpeed);
        if (type == AnimalType.RABBIT && curSpeed > 0.06) {
            hopRequested = true;
        }
        physics(terrain);

        // --- Blickrichtung ---
        float angToPlayer = (float) Math.atan2(px - x, pz - z);
        if (curSpeed > 0.035) {
            facing = approachAngle(facing, (float) Math.atan2(dirX, dirZ), 0.3f);
        } else if (activity == Activity.CHASE_TAIL) {
            facing += 0.32f;
        } else if (spinTicks > 0) {
            facing += 0.28f;
            spinTicks--;
        } else if (activity != Activity.SLEEP) {
            facing = approachAngle(facing, angToPlayer, 0.08f);
        }
        facing = wrapRad(facing);

        // --- Animation ---
        animTime += 0.05f;
        float wantWalk = activity == Activity.CHASE_TAIL ? 0.75f : (float) Math.min(1.0, curSpeed / 0.18);
        walkAmt += (wantWalk - walkAmt) * 0.3f;
        walkPhase += (float) (curSpeed * 3.4) + (activity == Activity.CHASE_TAIL ? 0.45f : 0f);

        float wantYaw = 0, wantPitch = 0;
        if (activity != Activity.SLEEP && activity != Activity.CHASE_TAIL) {
            wantYaw = wrapRad(angToPlayer - facing);
            double flat = Math.max(0.5, Math.sqrt(dist2(px - x, pz - z)));
            wantPitch = (float) Math.atan2((y + 0.45) - pEyeY, flat);
        }
        headYaw += (wantYaw - headYaw) * 0.15f;
        headPitch += (wantPitch - headPitch) * 0.15f;
        happy = Math.max(0f, happy - 0.02f);

        in.time = animTime;
        in.walkPhase = walkPhase;
        in.walkAmt = walkAmt;
        in.headYaw = headYaw;
        in.headPitch = headPitch;
        in.happy = happy;
        Animator.compute(target, model, activity, in);
        pose.lerpTo(target, 0.3f);
    }

    /** Streicheln: Freudensprung, Schwanzwedeln; nach mehrmaligem Streicheln eine Drehung. */
    public void pet() {
        happy = 1f;
        if (onGround) {
            vy = 0.24;
            onGround = false;
        }
        petCount = sinceLastPet < 60 ? petCount + 1 : 1;
        sinceLastPet = 0;
        if (petCount >= 3 && activity != Activity.SLEEP) {
            spinTicks = 24;
            petCount = 0;
        }
        if (activity == Activity.SLEEP) {
            setActivity(Activity.FOLLOW, 0);
            afkTicks = 0;
        }
    }

    /** "Platz!" bzw. "Komm!": schaltet zwischen Dauer-Sitzen und freiem Verhalten um. */
    public boolean toggleSit() {
        sitCommand = !sitCommand;
        if (sitCommand) {
            setActivity(Activity.SIT, 160);
        } else {
            setActivity(Activity.FOLLOW, 0);
        }
        return sitCommand;
    }

    // ------------------------------------------------------------------------------------------

    private void updateActivity(boolean moving, boolean rotated, AnimalType type) {
        stillTicks = moving ? 0 : stillTicks + 1;
        afkTicks = (moving || rotated) ? 0 : afkTicks + 1;

        if (sitCommand) {
            if (activity == Activity.SLEEP) {
                if (afkTicks < 20) {
                    setActivity(Activity.SIT, 160);
                }
            } else if (!activity.isSitting()) {
                setActivity(Activity.SIT, 160);
            } else if (--activityTicks <= 0) {
                setActivity(afkTicks > 800 ? Activity.SLEEP : pickSitVariant(), 140 + rnd.nextInt(120));
            }
            return;
        }

        if (activity == Activity.SLEEP) {
            if (afkTicks < 20 || moving) {
                setActivity(Activity.FOLLOW, 0);
                nextIdleAfter = 40;
            }
            return;
        }

        if (moving) {
            if (activity != Activity.FOLLOW) {
                setActivity(Activity.FOLLOW, 0);
            }
            followTicks = 0;
            return;
        }

        if (activity == Activity.FOLLOW) {
            followTicks++;
            if (afkTicks > 800) {
                setActivity(Activity.SLEEP, 0);
            } else if (stillTicks > 50 && followTicks > nextIdleAfter) {
                pickIdle(type);
            }
        } else if (--activityTicks <= 0) {
            setActivity(Activity.FOLLOW, 0);
            nextIdleAfter = 30 + rnd.nextInt(70);
            followTicks = 0;
        }
    }

    private Activity pickSitVariant() {
        Activity[] v = {Activity.SIT, Activity.LOOK_AROUND, Activity.GROOM};
        return v[rnd.nextInt(v.length)];
    }

    private void pickIdle(AnimalType type) {
        Map<Activity, Integer> weights = type.idleWeights();
        int total = 0;
        for (Map.Entry<Activity, Integer> e : weights.entrySet()) {
            if (e.getKey() == Activity.SLEEP && stillTicks < 300) {
                continue;
            }
            total += e.getValue();
        }
        if (total <= 0) {
            return;
        }
        int roll = rnd.nextInt(total);
        Activity chosen = Activity.SIT;
        for (Map.Entry<Activity, Integer> e : weights.entrySet()) {
            if (e.getKey() == Activity.SLEEP && stillTicks < 300) {
                continue;
            }
            roll -= e.getValue();
            if (roll < 0) {
                chosen = e.getKey();
                break;
            }
        }
        int ticks = switch (chosen) {
            case SIT, LOOK_AROUND -> 140 + rnd.nextInt(140);
            case GROOM -> 120 + rnd.nextInt(100);
            case CHASE_TAIL -> 80 + rnd.nextInt(60);
            case PLAY -> 140 + rnd.nextInt(100);
            case SNIFF -> 100 + rnd.nextInt(80);
            case SLEEP -> 400 + rnd.nextInt(400);
            default -> 100;
        };
        setActivity(chosen, ticks);
    }

    private void setActivity(Activity next, int ticks) {
        activity = next;
        activityTicks = ticks;
        subTimer = 0;
        if (next == Activity.FOLLOW) {
            wanderTimer = 0;
            pauseTimer = 0;
        }
    }

    private void moveToward(Terrain terrain, double tx, double tz, double maxSpeed) {
        double dx = tx - x, dz = tz - z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double want = (dist < 0.15 || maxSpeed <= 0) ? 0 : Math.min(maxSpeed, dist * 0.2);
        curSpeed += (want - curSpeed) * 0.3;
        if (curSpeed < 0.004 || dist < 1.0e-6) {
            curSpeed = 0;
            return;
        }
        double ux = dx / dist, uz = dz / dist;
        double nx = x + ux * curSpeed, nz = z + uz * curSpeed;
        if (terrain.canStand(nx, y, nz)) {
            x = nx;
            z = nz;
            dirX = ux;
            dirZ = uz;
            stuckTicks = 0;
        } else if (terrain.canStand(nx, y, z)) {
            x = nx;
            dirX = Math.signum(ux);
            dirZ = 0;
        } else if (terrain.canStand(x, y, nz)) {
            z = nz;
            dirX = 0;
            dirZ = Math.signum(uz);
        } else {
            curSpeed = 0;
            stuckTicks++;
            wanderTimer = 0; // neues Ziel suchen
        }
    }

    private void physics(Terrain terrain) {
        double g = terrain.findGround(x, y, z);
        if (Double.isNaN(g)) {
            g = y - 0.3; // kein Boden gefunden: weiter sinken, der Teleport fängt das auf
        }
        if (hopRequested && onGround) {
            vy = 0.2;
            onGround = false;
        }
        if (onGround) {
            double rise = g - y;
            if (rise > 0.001) {
                if (rise <= 0.55) {
                    y = g;
                } else if (rise <= 1.25) {
                    vy = 0.42;
                    onGround = false;
                } else {
                    y = g;
                }
            } else if (rise < -0.05) {
                onGround = false;
                vy = 0;
            } else {
                y = g;
            }
        }
        if (!onGround) {
            vy = (vy - 0.08) * 0.98;
            y += vy;
            if (vy <= 0 && y <= g) {
                y = g;
                vy = 0;
                onGround = true;
            }
        }
    }

    private void placeNear(Terrain terrain, double px, double py, double pz, boolean loud) {
        for (int i = 0; i < 12; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double r = 1.2 + rnd.nextDouble() * 1.3;
            double nx = px + Math.sin(ang) * r, nz = pz + Math.cos(ang) * r;
            if (terrain.canStand(nx, py, nz)) {
                x = nx;
                z = nz;
                y = terrain.findGround(nx, py, nz);
                finishPlacement(loud);
                return;
            }
        }
        x = px;
        y = py;
        z = pz;
        finishPlacement(loud);
    }

    private void finishPlacement(boolean loud) {
        vy = 0;
        onGround = true;
        curSpeed = 0;
        teleported = loud;
        goalX = x;
        goalZ = z;
    }

    private static double dist2(double dx, double dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static float wrapRad(float a) {
        while (a > Math.PI) {
            a -= (float) (Math.PI * 2);
        }
        while (a < -Math.PI) {
            a += (float) (Math.PI * 2);
        }
        return a;
    }

    private static float wrapDeg(float a) {
        while (a > 180f) {
            a -= 360f;
        }
        while (a < -180f) {
            a += 360f;
        }
        return a;
    }

    private static float approachAngle(float cur, float target, float rate) {
        return cur + wrapRad(target - cur) * rate;
    }
}
