package de.begleiter.client;

import de.begleiter.model.CompanionModel;
import de.begleiter.model.Palette;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Ordnet Modellfarben echten Blöcken zu (Wolle für Farben, Beton für Details). */
final class BlockPalette {
    private static final String[] COLORS = {
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
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
            case PRIMARY -> lookup(COLORS[Palette.wrap(primary)] + "_wool");
            case SECONDARY -> lookup(COLORS[Palette.wrap(secondary)] + "_wool");
            case DARK -> lookup("black_concrete");
            case INNER -> lookup("pink_concrete");
        };
    }
}
