package de.begleiter.model;

import java.awt.image.BufferedImage;

/**
 * Farben des kleinen Menschen, aus einer Minecraft-Skin-Datei gewonnen. Ein echter Skin lässt sich auf
 * Block-Quader nicht als Textur legen, deshalb wird er auf fünf Hauptfarben (Haut, Haar, Oberteil,
 * Hose, Schuhe) heruntergerechnet.
 */
public final class HumanSkin {
    public static final int SKIN = 0, HAIR = 1, SHIRT = 2, PANTS = 3, SHOE = 4;

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
        int[] out = new int[5];
        out[SKIN] = average(img, 9, 12, 14, 14, 0xD9A98B);   // untere Gesichtshälfte (Nase/Mund-Bereich)
        out[HAIR] = average(img, 8, 0, 15, 7, 0x3B2A1E);     // Kopfoberseite
        out[SHIRT] = average(img, 21, 21, 26, 30, 0x2F9FB0); // Oberkörper vorne
        out[PANTS] = average(img, 4, 22, 7, 28, 0x3A4A9A);   // Bein vorne
        out[SHOE] = average(img, 4, 30, 7, 31, 0x3A3A3A);    // Schuhbereich
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
}
