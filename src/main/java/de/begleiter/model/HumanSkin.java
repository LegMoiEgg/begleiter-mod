package de.begleiter.model;

import java.awt.image.BufferedImage;

/**
 * Farben des kleinen Menschen, aus einer Minecraft-Skin-Datei gewonnen. Ein echter Skin lässt sich auf
 * Block-Quader nicht als Textur legen, deshalb wird er auf fünf Hauptfarben (Haut, Haar, Oberteil,
 * Hose, Schuhe) heruntergerechnet.
 */
public final class HumanSkin {
    public static final int SKIN = 0, HAIR = 1, SHIRT = 2, PANTS = 3, SHOE = 4, HAIR_SIDE = 5;

    private static volatile int[] colors;

    private HumanSkin() {}

    /** Die aktuellen Farben oder null, wenn kein Skin geladen ist. */
    public static int[] colors() {
        return colors;
    }

    public static void set(int[] value) {
        colors = value;
    }

    /** Liest die Hauptfarben aus einem 64x64- oder 64x32-Skin; null, wenn das Bild nicht passt. */
    public static int[] extract(BufferedImage img) {
        if (img == null || img.getWidth() != 64 || (img.getHeight() != 64 && img.getHeight() != 32)) {
            return null;
        }
        int[] out = new int[6];
        // Median statt Mittelwert: einzelne Ausreißer (Augen, Mund, Strähnen) verfälschen die Farbe nicht
        out[SKIN] = median(img, 9, 12, 14, 15, 0xD9A98B);    // untere Gesichtshälfte (Wangen/Kinn)
        out[HAIR] = median(img, 8, 0, 15, 7, 0x3B2A1E);      // Kopfoberseite
        out[HAIR_SIDE] = median(img, 24, 8, 31, 11, 0x3B2A1E); // Hinterkopf (obere Hälfte), für Seiten und Rücken
        out[SHIRT] = average(img, 21, 21, 26, 30, 0x2F9FB0);  // Oberkörper vorne
        out[PANTS] = average(img, 4, 22, 7, 28, 0x3A4A9A);    // Bein vorne
        out[SHOE] = average(img, 4, 30, 7, 31, 0x3A3A3A);     // Schuhbereich
        return out;
    }

    private static int average(BufferedImage img, int x1, int y1, int x2, int y2, int fallback) {
        long r = 0, g = 0, b = 0, n = 0;
        for (int y = y1; y <= y2 && y < img.getHeight(); y++) {
            for (int x = x1; x <= x2; x++) {
                int argb = img.getRGB(x, y);
                if ((argb >>> 24) < 128) {
                    continue;
                }
                r += (argb >> 16) & 255;
                g += (argb >> 8) & 255;
                b += argb & 255;
                n++;
            }
        }
        if (n == 0) {
            return fallback;
        }
        return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }

    /** Kanalweiser Median der nicht durchsichtigen Pixel. */
    private static int median(BufferedImage img, int x1, int y1, int x2, int y2, int fallback) {
        java.util.List<Integer> rs = new java.util.ArrayList<>();
        java.util.List<Integer> gs = new java.util.ArrayList<>();
        java.util.List<Integer> bs = new java.util.ArrayList<>();
        for (int y = y1; y <= y2 && y < img.getHeight(); y++) {
            for (int x = x1; x <= x2; x++) {
                int argb = img.getRGB(x, y);
                if ((argb >>> 24) < 128) {
                    continue;
                }
                rs.add((argb >> 16) & 255);
                gs.add((argb >> 8) & 255);
                bs.add(argb & 255);
            }
        }
        if (rs.isEmpty()) {
            return fallback;
        }
        java.util.Collections.sort(rs);
        java.util.Collections.sort(gs);
        java.util.Collections.sort(bs);
        int m = rs.size() / 2;
        return rs.get(m) << 16 | gs.get(m) << 8 | bs.get(m);
    }
}
