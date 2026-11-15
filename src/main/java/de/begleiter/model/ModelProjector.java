package de.begleiter.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Zeichnet ein Modell als einfache 3D-Ansicht (orthografisch, Painter-Algorithmus) nur mit
 * gefüllten Rechtecken. So braucht die Menü-Vorschau keine Render-Pipeline von Minecraft.
 */
public final class ModelProjector {
    private ModelProjector() {}

    /** Ziel, auf das gezeichnet wird (im Spiel: GuiGraphicsExtractor#fill). */
    public interface Sink {
        void rect(int x1, int y1, int x2, int y2, int argb);
    }

    private static final class Face {
        final float[] xs = new float[4];
        final float[] ys = new float[4];
        float depth;
        int argb;
    }

    // Eckpunkt-Index = x + 2*y + 4*z (jeweils 0 oder 1)
    private static final int[][] FACE_CORNERS = {
        {1, 3, 7, 5}, // +X
        {0, 4, 6, 2}, // -X
        {2, 6, 7, 3}, // +Y
        {0, 1, 5, 4}, // -Y
        {4, 5, 7, 6}, // +Z
        {0, 2, 3, 1}  // -Z
    };
    private static final float[][] FACE_NORMALS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private static final float LX = 0.35f, LY = 0.8f, LZ = 0.5f;

    /**
     * @param cx,cy     Bildschirmposition des Bodenpunkts unter dem Tier
     * @param pxScale   Bildschirm-Pixel pro Modell-Pixel
     * @param viewYaw   Drehung der Ansicht um die Hochachse (Radiant)
     * @param tilt      Neigung der Kamera nach unten (Radiant)
     */
    public static void draw(Sink sink, CompanionModel model, Pose pose, int primary, int secondary,
                            float cx, float cy, float pxScale, float viewYaw, float tilt) {
        Rigid[] rigids = new Rigid[Group.COUNT];
        model.computeRigids(pose, rigids);

        float[] view = Rigid.mul(Rigid.axis(1, 0, 0, tilt), Rigid.axis(0, 1, 0, viewYaw));
        Rigid viewRot = new Rigid();
        viewRot.qx = view[0];
        viewRot.qy = view[1];
        viewRot.qz = view[2];
        viewRot.qw = view[3];

        drawShadow(sink, cx, cy, pxScale * 7.5f, pxScale * 3.2f);

        List<Face> faces = new ArrayList<>();
        float[] corner = new float[3];
        float[] normal = new float[3];
        float[][] pts = new float[8][3];

        for (CompanionModel.Part part : model.parts) {
            Rigid g = rigids[part.group().ordinal()];
            for (int i = 0; i < 8; i++) {
                corner[0] = part.x() + ((i & 1) != 0 ? part.w() : 0);
                corner[1] = part.y() + ((i & 2) != 0 ? part.h() : 0);
                corner[2] = part.z() + ((i & 4) != 0 ? part.d() : 0);
                g.apply(corner);
                viewRot.rotate(corner);
                pts[i][0] = corner[0];
                pts[i][1] = corner[1];
                pts[i][2] = corner[2];
            }
            int rgb = Palette.rgb(part.slot(), primary, secondary);
            for (int f = 0; f < 6; f++) {
                normal[0] = FACE_NORMALS[f][0];
                normal[1] = FACE_NORMALS[f][1];
                normal[2] = FACE_NORMALS[f][2];
                g.rotate(normal);
                viewRot.rotate(normal);
                if (normal[2] <= 0.02f) {
                    continue; // Rückseite
                }
                float shade = 0.42f + 0.58f * Math.max(0f, normal[0] * LX + normal[1] * LY + normal[2] * LZ);
                Face face = new Face();
                float depth = 0;
                for (int k = 0; k < 4; k++) {
                    float[] pt = pts[FACE_CORNERS[f][k]];
                    face.xs[k] = cx + pt[0] * pxScale;
                    face.ys[k] = cy - pt[1] * pxScale;
                    depth += pt[2];
                }
                face.depth = depth * 0.25f;
                face.argb = shade(rgb, shade);
                faces.add(face);
            }
        }

        faces.sort(Comparator.comparingDouble(f -> f.depth));
        for (Face f : faces) {
            fillQuad(sink, f.xs, f.ys, f.argb);
        }
    }

    private static int shade(int rgb, float s) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * s));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * s));
        int b = Math.min(255, Math.round((rgb & 0xFF) * s));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void drawShadow(Sink sink, float cx, float cy, float rx, float ry) {
        int steps = Math.max(1, Math.round(ry * 2));
        for (int i = -steps; i <= steps; i++) {
            float k = i / (float) steps;
            float half = rx * (float) Math.sqrt(Math.max(0, 1 - k * k));
            int y = Math.round(cy + k * ry);
            sink.rect(Math.round(cx - half), y, Math.round(cx + half), y + 1, 0x44000000);
        }
    }

    /** Füllt ein konvexes Viereck zeilenweise. */
    static void fillQuad(Sink sink, float[] xs, float[] ys, int argb) {
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            minY = Math.min(minY, ys[i]);
            maxY = Math.max(maxY, ys[i]);
        }
        int y0 = (int) Math.floor(minY);
        int y1 = (int) Math.ceil(maxY);
        for (int y = y0; y < y1; y++) {
            float yc = y + 0.5f;
            float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) & 3;
                float ya = ys[i], yb = ys[j];
                if ((yc >= ya && yc < yb) || (yc >= yb && yc < ya)) {
                    float x = xs[i] + (yc - ya) / (yb - ya) * (xs[j] - xs[i]);
                    lo = Math.min(lo, x);
                    hi = Math.max(hi, x);
                }
            }
            if (hi > lo) {
                int xa = Math.round(lo);
                int xb = Math.round(hi);
                if (xb > xa) {
                    sink.rect(xa, y, xb, y + 1, argb);
                }
            }
        }
    }
}
