package de.begleiter.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Ein Tiermodell aus Quadern. Koordinaten in Pixeln (16 Pixel = 1 Block), Boden bei y = 0,
 * Blickrichtung +Z, +X ist die linke Seite des Tiers, Mitte bei x = 0.
 */
public final class CompanionModel {

    /** Welche Farbe ein Quader bekommt. */
    public enum Slot { PRIMARY, SECONDARY, DARK, INNER, SKIN, HAIR, SHIRT, PANTS, SHOE }

    public record Part(Group group, Slot slot, float x, float y, float z, float w, float h, float d) {}

    public final AnimalType type;
    public final List<Part> parts;
    private final float[][] pivots;

    /** Größe der Trefferbox für Interaktion (Pixel). */
    public final float hitRadius;
    public final float hitHeight;
    /** Beinlänge in Pixeln (wie tief der Körper beim Schlafen absinkt). */
    public final float legH;
    /** Wie weit der Körper beim Sitzen absinkt (Pixel) und wie stark er sich neigt (Radiant). */
    public final float sitDrop;
    public final float sitPitch;
    /** Grundhaltung des Schwanzes: positiv = nach oben. */
    public final float tailRaise;

    private CompanionModel(Builder b) {
        this.type = b.type;
        this.parts = List.copyOf(b.parts);
        this.pivots = b.pivots;
        this.hitRadius = b.hitRadius;
        this.hitHeight = b.hitHeight;
        this.legH = b.legH;
        this.sitDrop = b.sitDrop;
        this.sitPitch = b.sitPitch;
        this.tailRaise = b.tailRaise;
    }

    public float[] pivot(Group g) {
        return pivots[g.ordinal()];
    }

    /**
     * Berechnet pro Gruppe die Transformation (in Modell-Pixeln, ohne Skalierung und Blickrichtung),
     * indem die Eltern-Ketten aufgelöst werden.
     */
    public void computeRigids(Pose pose, Rigid[] out) {
        for (Group g : Group.values()) {
            int i = g.ordinal();
            float[] p = pose.v[i];
            float[] pv = pivots[i];
            Rigid local = Rigid.aboutPivot(p[0], p[1], p[2], pv[0], pv[1], pv[2], p[3], p[4], p[5]);
            out[i] = g.parentOrdinal < 0 ? local : out[g.parentOrdinal].compose(local);
        }
    }

    public static Builder builder(AnimalType type) {
        return new Builder(type);
    }

    public static final class Builder {
        private final AnimalType type;
        private final List<Part> parts = new ArrayList<>();
        private final float[][] pivots = new float[Group.COUNT][3];
        private float hitRadius = 6, hitHeight = 13, legH = 4, sitDrop = 3, sitPitch = 0.3f, tailRaise = 0.5f;

        private Builder(AnimalType type) {
            this.type = type;
        }

        public Builder pivot(Group g, float x, float y, float z) {
            pivots[g.ordinal()] = new float[] {x, y, z};
            return this;
        }

        /** Quader mit Ecke (x, y, z) und Größe (w, h, d). */
        public Builder box(Group g, Slot s, float x, float y, float z, float w, float h, float d) {
            parts.add(new Part(g, s, x, y, z, w, h, d));
            return this;
        }

        /** Quader auf beiden Körperseiten (spiegelt an x = 0); x ist die Ecke der linken Seite (+X). */
        public Builder mirrored(Group left, Group right, Slot s, float x, float y, float z, float w, float h, float d) {
            box(left, s, x, y, z, w, h, d);
            box(right, s, -x - w, y, z, w, h, d);
            return this;
        }

        public Builder shape(float hitRadius, float hitHeight, float legH,
                             float sitDrop, float sitPitch, float tailRaise) {
            this.hitRadius = hitRadius;
            this.hitHeight = hitHeight;
            this.legH = legH;
            this.sitDrop = sitDrop;
            this.sitPitch = sitPitch;
            this.tailRaise = tailRaise;
            return this;
        }

        public CompanionModel build() {
            return new CompanionModel(this);
        }
    }
}
