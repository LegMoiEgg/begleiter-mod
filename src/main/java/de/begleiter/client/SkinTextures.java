package de.begleiter.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/** Lädt eine Skin-PNG-Datei als echte Textur, die das Spielermodell benutzen kann. */
final class SkinTextures {
    private static int counter;
    private static Identifier current;
    private static String loadedKey = "";
    private static PlayerSkin loadedSkin;

    private SkinTextures() {
    }

    /** Gibt die Skin für diese Datei zurück (zwischengespeichert) oder null, wenn sie fehlt oder ungültig ist. */
    static PlayerSkin get(String path) {
        if (path == null || path.isBlank()) {
            release();
            return null;
        }
        Path file = Path.of(path);
        if (!Files.isRegularFile(file)) {
            release();
            return null;
        }
        String key;
        try {
            key = path + "@" + Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return loadedSkin;
        }
        if (key.equals(loadedKey)) {
            return loadedSkin;
        }
        release();
        loadedKey = key;
        try {
            BufferedImage skin = toModernLayout(ImageIO.read(file.toFile()));
            if (skin == null) {
                return null;
            }
            NativeImage image = new NativeImage(64, 64, true);
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    image.setPixel(x, y, skin.getRGB(x, y));
                }
            }
            Identifier texture = Identifier.fromNamespaceAndPath("begleiter", "textures/skin/s" + (counter++) + ".png");
            Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(() -> "begleiter skin", image));
            current = texture;
            ClientAsset.ResourceTexture body = new ClientAsset.ResourceTexture(texture, texture);
            loadedSkin = new PlayerSkin(body, null, null, PlayerModelType.WIDE, false);
            return loadedSkin;
        } catch (IOException | RuntimeException e) {
            System.err.println("[Begleiter] Skin-Textur konnte nicht erstellt werden: " + e);
            loadedSkin = null;
            return null;
        }
    }

    private static void release() {
        if (current != null) {
            Minecraft.getInstance().getTextureManager().release(current);
            current = null;
        }
        loadedKey = "";
        loadedSkin = null;
    }

    /** Macht aus einem 64x32-Skin (alt) einen 64x64-Skin; 64x64 bleibt unverändert. */
    private static BufferedImage toModernLayout(BufferedImage src) {
        if (src == null || src.getWidth() != 64 || (src.getHeight() != 64 && src.getHeight() != 32)) {
            return null;
        }
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < 64; x++) {
                out.setRGB(x, y, src.getRGB(x, y));
            }
        }
        if (src.getHeight() == 32) {
            // Alte Skins haben nur ein Bein/einen Arm: spiegeln für die zweite Seite
            int[][] areas = {
                {4, 16, 4, 4, 20, 48}, {8, 16, 4, 4, 24, 48}, {0, 20, 4, 12, 24, 52}, {4, 20, 4, 12, 20, 52},
                {8, 20, 4, 12, 28, 52}, {12, 20, 4, 12, 32, 52}, {44, 16, 4, 4, 36, 48}, {48, 16, 4, 4, 40, 48},
                {40, 20, 4, 12, 40, 52}, {44, 20, 4, 12, 36, 52}, {48, 20, 4, 12, 44, 52}, {52, 20, 4, 12, 48, 52}
            };
            for (int[] a : areas) {
                for (int dy = 0; dy < a[3]; dy++) {
                    for (int dx = 0; dx < a[2]; dx++) {
                        out.setRGB(a[4] + (a[2] - 1 - dx), a[5] + dy, src.getRGB(a[0] + dx, a[1] + dy));
                    }
                }
            }
        }
        return out;
    }
}
