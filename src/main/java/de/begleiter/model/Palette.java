package de.begleiter.model;

/** Die 16 Fellfarben (entsprechen den Woll-Blöcken im Spiel) und feste Farben für Augen/Nase. */
public final class Palette {
    private Palette() {}

    public static final String[] NAMES = {
        "Weiß", "Orange", "Magenta", "Hellblau", "Gelb", "Hellgrün", "Rosa", "Grau",
        "Hellgrau", "Türkis", "Lila", "Blau", "Braun", "Grün", "Rot", "Schwarz"
    };

    /** Ungefähre Wollfarben für die Vorschau im Menü. */
    public static final int[] RGB = {
        0xE9ECEC, 0xF07613, 0xBD44B3, 0x3AAFD9, 0xF8C627, 0x70B919, 0xED8DAC, 0x3E4447,
        0x8E8E86, 0x158991, 0x792AAC, 0x35399D, 0x724728, 0x546D1B, 0xA12722, 0x141519
    };

    public static final int DARK_RGB = 0x0B0C0E;
    public static final int INNER_RGB = 0xD5658F;

    public static int count() {
        return RGB.length;
    }

    public static int wrap(int index) {
        int n = RGB.length;
        return ((index % n) + n) % n;
    }

    public static int rgb(CompanionModel.Slot slot, int primary, int secondary) {
        int[] skin = HumanSkin.colors();
        return switch (slot) {
            case PRIMARY -> RGB[wrap(primary)];
            case SECONDARY -> RGB[wrap(secondary)];
            case DARK -> DARK_RGB;
            case INNER -> INNER_RGB;
            case SKIN -> skin != null ? skin[HumanSkin.SKIN] : 0xD9A98B;
            case HAIR -> skin != null ? skin[HumanSkin.HAIR] : 0x3B2A1E;
            case HAIR_SIDE -> skin != null ? skin[HumanSkin.HAIR_SIDE] : 0x3B2A1E;
            case SHIRT -> skin != null ? skin[HumanSkin.SHIRT] : RGB[wrap(primary)];
            case PANTS -> skin != null ? skin[HumanSkin.PANTS] : RGB[wrap(secondary)];
            case SHOE -> skin != null ? skin[HumanSkin.SHOE] : 0x3A3A3A;
        };
    }
}
