package de.begleiter.model;

import static de.begleiter.model.Group.BODY;
import static de.begleiter.model.Group.EAR_L;
import static de.begleiter.model.Group.EAR_R;
import static de.begleiter.model.Group.HEAD;
import static de.begleiter.model.Group.LEG_BL;
import static de.begleiter.model.Group.LEG_BR;
import static de.begleiter.model.Group.LEG_FL;
import static de.begleiter.model.Group.LEG_FR;
import static de.begleiter.model.Group.TAIL;

/**
 * Berechnet die Zielpose einer Beschäftigung. Winkel in Radiant, Verschiebungen in Modell-Pixeln.
 *
 * <p>Konventionen (Modell: Blick +Z, oben +Y, links +X): positives rx neigt die Vorderseite nach unten
 * (Kopf nach unten, Bein schwingt nach hinten, Schwanz geht nach oben), positives ry dreht nach links.
 */
public final class Animator {
    private Animator() {}

    public static void compute(Pose p, CompanionModel m, Activity act, AnimInput in) {
        p.clear();
        final float t = in.time;
        final boolean rabbit = m.type == AnimalType.RABBIT;
        final float w = clamp01(in.walkAmt);
        final float s = (float) Math.sin(in.walkPhase);
        final float c = (float) Math.cos(in.walkPhase);

        // Atmen und leichtes Schwanzwedeln in jeder Lage
        p.add(BODY, 0, 0, 0, 0, (float) Math.sin(t * 2.2f) * 0.12f, 0);
        p.add(TAIL, m.tailRaise, (float) Math.sin(t * 1.7f) * 0.22f, 0, 0, 0, 0);

        switch (act) {
            case FOLLOW -> walk(p, rabbit, w, s, c, in.walkPhase);
            case CHASE_TAIL -> {
                walk(p, rabbit, Math.max(w, 0.75f), s, c, in.walkPhase);
                p.add(HEAD, 0, 0.75f, 0, 0, 0, 0);
                p.add(TAIL, 0, -0.8f, 0, 0, 0, 0);
                p.add(BODY, 0, 0, 0.12f, 0, 0, 0);
            }
            case SIT, LOOK_AROUND, GROOM -> {
                sit(p, m, rabbit);
                if (act == Activity.LOOK_AROUND) {
                    p.add(HEAD, (float) Math.sin(t * 0.55f) * 0.12f, (float) Math.sin(t * 0.9f) * 0.9f, 0, 0, 0, 0);
                } else if (act == Activity.GROOM) {
                    float lick = (float) Math.sin(t * 7f);
                    p.add(HEAD, 0.65f + lick * 0.08f, 0.25f, 0, 0, 0, 0);
                    if (rabbit) {
                        p.add(LEG_FL, -1.2f + lick * 0.25f, 0, 0, 0, 0.5f, 0);
                        p.add(LEG_FR, -1.2f - lick * 0.25f, 0, 0, 0, 0.5f, 0);
                    } else {
                        p.add(LEG_FL, -1.5f + lick * 0.25f, 0, 0, 0, 0.8f, 0.6f);
                    }
                }
            }
            case SNIFF -> {
                float nose = (float) Math.sin(t * 9f);
                p.add(HEAD, -0.1f + nose * 0.12f, (float) Math.sin(t * 1.3f) * 0.5f, 0, 0, 0, 0);
                float tw = (float) Math.max(0, Math.sin(t * 6f)) * 0.18f;
                p.add(EAR_L, -tw, 0, 0, 0, 0, 0);
                p.add(EAR_R, -tw, 0, 0, 0, 0, 0);
                if (rabbit) {
                    p.add(BODY, -0.28f, 0, 0, 0, 0.2f, 0);
                    p.add(HEAD, 0.22f, 0, 0, 0, 0, 0);
                    p.add(LEG_BL, 0.25f, 0, 0, 0, 0, 0);
                    p.add(LEG_BR, 0.25f, 0, 0, 0, 0, 0);
                }
            }
            case PLAY -> {
                if (rabbit) {
                    // Freudensprünge: Körper wippt, Ohren fliegen
                    p.add(BODY, (float) Math.sin(t * 9f) * 0.2f, 0, (float) Math.sin(t * 4.5f) * 0.12f, 0, 0, 0);
                    p.add(LEG_FL, -0.6f, 0, 0, 0, 0, 0);
                    p.add(LEG_FR, -0.6f, 0, 0, 0, 0, 0);
                    p.add(LEG_BL, 0.5f, 0, 0, 0, 0, 0);
                    p.add(LEG_BR, 0.5f, 0, 0, 0, 0, 0);
                } else {
                    // Spielaufforderung: Vorderkörper unten, Hinterteil oben, Schwanz wedelt
                    p.add(BODY, 0.30f, 0, 0, 0, -0.5f, 0);
                    p.add(HEAD, -0.38f, 0, 0, 0, 0, 0);
                    p.add(LEG_FL, -0.4f, 0, 0, 0, 0, 0);
                    p.add(LEG_FR, -0.4f, 0, 0, 0, 0, 0);
                    p.add(TAIL, 0.2f, (float) Math.sin(t * 14f) * 0.55f, 0, 0, 0, 0);
                }
            }
            case SLEEP -> sleep(p, m, t);
        }

        if (act != Activity.SLEEP) {
            // Blick zum Spieler
            p.add(HEAD, clamp(in.headPitch, -0.55f, 0.55f), clamp(in.headYaw, -1.0f, 1.0f), 0, 0, 0, 0);

            // Ohrenzucken
            float twL = (float) Math.max(0, Math.sin(t * 1.3f) - 0.9f) * 5f;
            float twR = (float) Math.max(0, Math.sin(t * 1.1f + 2f) - 0.9f) * 5f;
            p.add(EAR_L, 0, 0, twL * 0.5f, 0, 0, 0);
            p.add(EAR_R, 0, 0, -twR * 0.5f, 0, 0, 0);
            if (rabbit) {
                p.add(EAR_L, 0, 0, 0.14f, 0, 0, 0);
                p.add(EAR_R, 0, 0, -0.14f, 0, 0, 0);
            }
        }

        // Freude: wedeln und Ohren nach hinten
        if (in.happy > 0.01f && act != Activity.SLEEP) {
            p.add(TAIL, 0, (float) Math.sin(t * 20f) * 0.7f * in.happy, 0, 0, 0, 0);
            p.add(HEAD, -0.15f * in.happy, 0, 0, 0, 0, 0);
            p.add(EAR_L, -0.2f * in.happy, 0, 0, 0, 0, 0);
            p.add(EAR_R, -0.2f * in.happy, 0, 0, 0, 0, 0);
        }
    }

    private static void walk(Pose p, boolean rabbit, float w, float s, float c, float phase) {
        if (rabbit) {
            // Hoppeln: beide Vorderbeine bzw. beide Hinterbeine gemeinsam
            p.add(LEG_FL, s * 0.8f * w, 0, 0, 0, 0, 0);
            p.add(LEG_FR, s * 0.8f * w, 0, 0, 0, 0, 0);
            p.add(LEG_BL, -s * 1.1f * w, 0, 0, 0, 0, 0);
            p.add(LEG_BR, -s * 1.1f * w, 0, 0, 0, 0, 0);
            p.add(BODY, s * 0.22f * w, 0, 0, 0, 0, 0);
        } else {
            float amp = 0.85f * w;
            p.add(LEG_FL, s * amp, 0, 0, 0, 0, 0);
            p.add(LEG_BR, s * amp, 0, 0, 0, 0, 0);
            p.add(LEG_FR, -s * amp, 0, 0, 0, 0, 0);
            p.add(LEG_BL, -s * amp, 0, 0, 0, 0, 0);
            p.add(BODY, 0, 0, c * 0.05f * w, 0, Math.abs(s) * 0.6f * w, 0);
            p.add(HEAD, (float) Math.sin(phase * 2f) * 0.05f * w, 0, 0, 0, 0, 0);
        }
        p.add(TAIL, 0, (float) Math.sin(phase * 0.5f) * 0.35f * w, 0, 0, 0, 0);
        p.add(EAR_L, -0.4f * w, 0, 0, 0, 0, 0);
        p.add(EAR_R, -0.4f * w, 0, 0, 0, 0, 0);
    }

    private static void sit(Pose p, CompanionModel m, boolean rabbit) {
        // Körper kippt nach hinten (um das Hinterteil), Hinterbeine liegen angewinkelt unter dem Körper
        p.add(BODY, -m.sitPitch, 0, 0, 0, -m.sitDrop, 0);
        p.add(HEAD, m.sitPitch * 0.9f, 0, 0, 0, 0, 0);
        float fold = rabbit ? -1.15f : -1.35f;
        p.add(LEG_BL, fold, 0, 0, 0, -m.sitDrop, 0.5f);
        p.add(LEG_BR, fold, 0, 0, 0, -m.sitDrop, 0.5f);
        p.add(TAIL, -m.tailRaise - 0.1f, 0, 0, 0, -m.sitDrop, 0);
    }

    private static void sleep(Pose p, CompanionModel m, float t) {
        float down = -(m.legH - 1.0f);
        float breathe = (float) Math.sin(t * 1.8f) * 0.18f;
        p.add(BODY, 0, 0, 0, 0, down + breathe, 0);
        p.add(HEAD, 0.75f, 0.55f, 0, 0, 0, 0);
        p.add(LEG_FL, -1.45f, 0, 0, 0, down, 0);
        p.add(LEG_FR, -1.45f, 0, 0, 0, down, 0);
        p.add(LEG_BL, -1.25f, 0, 0, 0, down, 0);
        p.add(LEG_BR, -1.25f, 0, 0, 0, down, 0);
        p.add(TAIL, -m.tailRaise - 0.2f, 1.1f, 0, 0, down, 0);
        p.add(EAR_L, -0.5f, 0, 0, 0, 0, 0);
        p.add(EAR_R, -0.5f, 0, 0, 0, 0, 0);
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
