package de.begleiter.model;

/**
 * Starre Transformation (Rotation als Quaternion + Verschiebung), bewusst ohne JOML,
 * damit Modell- und Animationscode ohne Minecraft getestet werden kann.
 */
public final class Rigid {
    public float qx, qy, qz, qw = 1f;
    public float tx, ty, tz;

    public static Rigid identity() {
        return new Rigid();
    }

    /** Quaternion aus Euler-Winkeln, Reihenfolge Y * X * Z (wie bei Minecraft-Entitäten). */
    public static float[] eulerYXZ(float rx, float ry, float rz) {
        float[] qy = axis(0, 1, 0, ry);
        float[] qx = axis(1, 0, 0, rx);
        float[] qz = axis(0, 0, 1, rz);
        return mul(mul(qy, qx), qz);
    }

    public static float[] axis(float ax, float ay, float az, float angle) {
        float s = (float) Math.sin(angle * 0.5f);
        return new float[] {ax * s, ay * s, az * s, (float) Math.cos(angle * 0.5f)};
    }

    /** Hamilton-Produkt a * b, Format {x, y, z, w}. */
    public static float[] mul(float[] a, float[] b) {
        return new float[] {
            a[3] * b[0] + a[0] * b[3] + a[1] * b[2] - a[2] * b[1],
            a[3] * b[1] - a[0] * b[2] + a[1] * b[3] + a[2] * b[0],
            a[3] * b[2] + a[0] * b[1] - a[1] * b[0] + a[2] * b[3],
            a[3] * b[3] - a[0] * b[0] - a[1] * b[1] - a[2] * b[2]
        };
    }

    /** Drehung um einen Pivot plus zusätzliche Verschiebung. */
    public static Rigid aboutPivot(float rx, float ry, float rz,
                                   float px, float py, float pz,
                                   float tx, float ty, float tz) {
        Rigid r = new Rigid();
        float[] q = eulerYXZ(rx, ry, rz);
        r.qx = q[0];
        r.qy = q[1];
        r.qz = q[2];
        r.qw = q[3];
        float[] p = {px, py, pz};
        r.rotate(p);
        r.tx = px - p[0] + tx;
        r.ty = py - p[1] + ty;
        r.tz = pz - p[2] + tz;
        return r;
    }

    /** this ∘ o: erst o, dann this anwenden. */
    public Rigid compose(Rigid o) {
        Rigid r = new Rigid();
        float[] q = mul(new float[] {qx, qy, qz, qw}, new float[] {o.qx, o.qy, o.qz, o.qw});
        r.qx = q[0];
        r.qy = q[1];
        r.qz = q[2];
        r.qw = q[3];
        float[] t = {o.tx, o.ty, o.tz};
        rotate(t);
        r.tx = t[0] + tx;
        r.ty = t[1] + ty;
        r.tz = t[2] + tz;
        return r;
    }

    /** Dreht den Vektor v (in place) mit der Rotation dieser Transformation. */
    public void rotate(float[] v) {
        float cx = qy * v[2] - qz * v[1];
        float cy = qz * v[0] - qx * v[2];
        float cz = qx * v[1] - qy * v[0];
        float tx2 = 2f * cx, ty2 = 2f * cy, tz2 = 2f * cz;
        float rx = qy * tz2 - qz * ty2;
        float ry = qz * tx2 - qx * tz2;
        float rz = qx * ty2 - qy * tx2;
        v[0] += qw * tx2 + rx;
        v[1] += qw * ty2 + ry;
        v[2] += qw * tz2 + rz;
    }

    /** Wendet Rotation und Verschiebung auf den Punkt p an (in place). */
    public void apply(float[] p) {
        rotate(p);
        p[0] += tx;
        p[1] += ty;
        p[2] += tz;
    }
}
