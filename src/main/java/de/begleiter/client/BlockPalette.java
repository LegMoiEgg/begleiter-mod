package de.begleiter.client;

import de.begleiter.model.CompanionModel;
import de.begleiter.model.Palette;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ordnet Modellfarben echten Blöcken zu. Es werden nur glatte, einfarbige Blöcke benutzt (Beton und
 * Terrakotta), damit die Begleiter nicht wie Wolle oder Muster aussehen.
 */
final class BlockPalette {
    /** Beton in der Reihenfolge der Palette (Weiß ... Schwarz). */
    private static final String[] CONCRETE = {
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    /** Zusätzliche Blöcke mit ungefährer Farbe für die automatische Zuordnung (Haut, Haare, Kleidung). */
    private static final String[] NEAR_IDS = {
        "white_concrete", "orange_concrete", "magenta_concrete", "light_blue_concrete", "yellow_concrete",
        "lime_concrete", "pink_concrete", "gray_concrete", "light_gray_concrete", "cyan_concrete",
        "purple_concrete", "blue_concrete", "brown_concrete", "green_concrete", "red_concrete", "black_concrete",
        "terracotta", "white_terracotta", "orange_terracotta", "magenta_terracotta", "light_blue_terracotta",
        "yellow_terracotta", "lime_terracotta", "pink_terracotta", "gray_terracotta", "light_gray_terracotta",
        "cyan_terracotta", "purple_terracotta", "blue_terracotta", "brown_terracotta", "green_terracotta",
        "red_terracotta", "black_terracotta"
    };
    private static final int[] NEAR_RGB = {
        0xCFD5D6, 0xE06101, 0xA9309F, 0x2389C7, 0xF1AF15,
        0x5EA918, 0xD5658F, 0x36393D, 0x7D7D73, 0x157788,
        0x64209C, 0x2C2E8F, 0x60391E, 0x495B24, 0x8E2121, 0x080A0F,
        0x985F45, 0xD1B1A1, 0xA15325, 0x95586C, 0x716C89,
        0xBA8523, 0x677535, 0xA14E4E, 0x392A23, 0x876B62,
        0x575B5B, 0x764656, 0x4A3B5B, 0x4D3323, 0x4C532A,
        0x8F3D2E, 0x251610
    };

    private BlockPalette() {
    }

    /** Sucht den Block über seine ID, damit Umbenennungen der Java-Konstanten egal sind. */
    private static BlockState lookup(String id) {
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
        return block.defaultBlockState();
    }

    static BlockState block(CompanionModel.Slot slot, int primary, int secondary) {
        return switch (slot) {
            case PRIMARY -> lookup(CONCRETE[Palette.wrap(primary)] + "_concrete");
            case SECONDARY -> lookup(CONCRETE[Palette.wrap(secondary)] + "_concrete");
            case DARK -> lookup("black_concrete");
            case INNER -> lookup("pink_concrete");
            case SKIN, HAIR, SHIRT, PANTS, SHOE -> lookup(nearest(Palette.rgb(slot, primary, secondary)));
        };
    }

    private static String nearest(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        int best = 0;
        long bestDist = Long.MAX_VALUE;
        for (int i = 0; i < NEAR_RGB.length; i++) {
            int c = NEAR_RGB[i];
            long dr = r - ((c >> 16) & 255), dg = g - ((c >> 8) & 255), db = b - (c & 255);
            long dist = dr * dr * 3 + dg * dg * 4 + db * db * 2; // Grün zählt stärker (Wahrnehmung)
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return NEAR_IDS[best];
    }
}
