package de.begleiter.model;

/** Pro Gruppe: Rotation (rx, ry, rz in Radiant) und Verschiebung (tx, ty, tz in Modell-Pixeln). */
public final class Pose {
    public final float[][] v = new float[Group.COUNT][6];

    public void clear() {
        for (float[] g : v) {
            java.util.Arrays.fill(g, 0f);
        }
    }

    public void set(Group g, float rx, float ry, float rz, float tx, float ty, float tz) {
        float[] a = v[g.ordinal()];
        a[0] = rx;
        a[1] = ry;
        a[2] = rz;
        a[3] = tx;
        a[4] = ty;
        a[5] = tz;
    }

    public void add(Group g, float rx, float ry, float rz, float tx, float ty, float tz) {
        float[] a = v[g.ordinal()];
        a[0] += rx;
        a[1] += ry;
        a[2] += rz;
        a[3] += tx;
        a[4] += ty;
        a[5] += tz;
    }

    /** Nähert diese Pose um den Faktor f (0..1) an die Zielpose an. */
    public void lerpTo(Pose target, float f) {
        for (int i = 0; i < v.length; i++) {
            for (int k = 0; k < 6; k++) {
                v[i][k] += (target.v[i][k] - v[i][k]) * f;
            }
        }
    }

    public void copyFrom(Pose o) {
        for (int i = 0; i < v.length; i++) {
            System.arraycopy(o.v[i], 0, v[i], 0, 6);
        }
    }
}
