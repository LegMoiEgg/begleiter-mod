package de.begleiter.client.gui;

import de.begleiter.config.BegleiterConfig;
import de.begleiter.model.Activity;
import de.begleiter.model.AnimInput;
import de.begleiter.model.AnimalType;
import de.begleiter.model.Animator;
import de.begleiter.model.ModelProjector;
import de.begleiter.model.Models;
import de.begleiter.model.Palette;
import de.begleiter.model.Pose;
import de.begleiter.model.HumanSkin;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Menü zum Anpassen des Begleiters (Tier, Farben, Größe) mit animierter Vorschau.
 * Erreichbar über die Schaltfläche "Begleiter" im Hauptmenü und im Pausenmenü.
 */
public class CustomizeScreen extends Screen {
    private static final String[] PREVIEW_NAMES = {
        "Laufen", "Sitzen", "Umsehen", "Putzen", "Spielen", "Schnuppern", "Schlafen"
    };
    private static final Activity[] PREVIEW_ACTIVITIES = {
        Activity.FOLLOW, Activity.SIT, Activity.LOOK_AROUND, Activity.GROOM,
        Activity.PLAY, Activity.SNIFF, Activity.SLEEP
    };
    private static final int LABEL_W = 124;
    private static final int ARROW_W = 20;
    private static final int ROW_H = 26;

    private final Screen parent;
    private final BegleiterConfig cfg = BegleiterConfig.get();
    private final Pose pose = new Pose();
    private final Pose target = new Pose();
    private final AnimInput in = new AnimInput();
    private final Random random = new Random();

    private int previewIndex;
    private float time;
    private long lastNanos = System.nanoTime();
    private int colX;
    private int firstRowY;
    private String status = "";

    public CustomizeScreen(Screen parent) {
        super(Component.literal("Begleiter anpassen"));
        this.parent = parent;
    }

    /** Die Schaltfläche, die Haupt- und Pausenmenü bekommen. */
    public static Button menuButton(Screen current, int x, int y, int width) {
        return Button.builder(Component.literal("Begleiter"),
                b -> Minecraft.getInstance().gui.setScreen(new CustomizeScreen(current)))
            .bounds(x, y, width, 20)
            .build();
    }

    @Override
    protected void init() {
        int total = ARROW_W + 4 + LABEL_W + 4 + ARROW_W;
        colX = this.width / 2 + 16;
        firstRowY = this.height / 2 - 86;
        int y = firstRowY;

        arrows(y, () -> cycleAnimal(-1), () -> cycleAnimal(1));
        y += ROW_H;
        arrows(y, () -> cfg.setPrimary(cfg.primaryColor - 1), () -> cfg.setPrimary(cfg.primaryColor + 1));
        y += ROW_H;
        arrows(y, () -> cfg.setSecondary(cfg.secondaryColor - 1), () -> cfg.setSecondary(cfg.secondaryColor + 1));
        y += ROW_H;
        arrows(y, () -> cfg.setSize(cfg.size - 0.1f), () -> cfg.setSize(cfg.size + 0.1f));
        y += ROW_H;
        arrows(y, () -> previewIndex = Math.floorMod(previewIndex - 1, PREVIEW_NAMES.length),
            () -> previewIndex = Math.floorMod(previewIndex + 1, PREVIEW_NAMES.length));
        y += ROW_H + 6;

        this.addRenderableWidget(Button.builder(toggleLabel(), b -> {
            cfg.setEnabled(!cfg.enabled);
            b.setMessage(toggleLabel());
        }).bounds(colX, y, total, 20).build());
        y += 24;

        int half = (total - 4) / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Zufall"), b -> randomize())
            .bounds(colX, y, half, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> this.onClose())
            .bounds(colX + half + 4, y, half, 20).build());
        y += 24;
        this.addRenderableWidget(Button.builder(Component.literal("Skin laden"), b -> reloadSkin())
            .bounds(colX, y, half, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Skin entfernen"), b -> {
            cfg.setSkin("");
            status = "Skin entfernt";
        }).bounds(colX + half + 4, y, half, 20).build());
    }

    /** Lädt config/begleiter-skin.png (die Datei, die man selbst in den config-Ordner legt). */
    private void reloadSkin() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("begleiter-skin.png");
        if (!Files.isRegularFile(file)) {
            status = "Nicht gefunden: config/begleiter-skin.png";
            return;
        }
        cfg.setSkin(file.toString());
        if (HumanSkin.colors() == null) {
            status = "Kein gültiger Skin (64x64 oder 64x32 PNG nötig)";
            return;
        }
        cfg.setAnimal(AnimalType.HUMAN);
        BegleiterConfig.save();
        status = "Skin geladen";
    }

    /**
     * Skin-Datei ins Fenster ziehen: Sie wird in den Konfigurationsordner kopiert und für den kleinen
     * Menschen benutzt. (Ohne @Override, damit ein anderer Methodenname in einer neuen Version nur
     * das Ziehen abschaltet, aber nichts kaputt macht.)
     */
    public void onFilesDrop(List<Path> paths) {
        for (Path p : paths) {
            if (!p.getFileName().toString().toLowerCase().endsWith(".png")) {
                continue;
            }
            try {
                if (HumanSkin.extract(ImageIO.read(p.toFile())) == null) {
                    status = "Kein gültiger Skin (64x64 oder 64x32 PNG nötig)";
                    return;
                }
                Path target = FabricLoader.getInstance().getConfigDir().resolve("begleiter-skin.png");
                Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                cfg.setSkin(target.toString());
                cfg.setAnimal(AnimalType.HUMAN);
                BegleiterConfig.save();
                status = "Skin geladen: " + p.getFileName();
                return;
            } catch (IOException | RuntimeException e) {
                status = "Skin konnte nicht gelesen werden";
                return;
            }
        }
        status = "Bitte eine PNG-Datei ziehen";
    }

    private void arrows(int y, Runnable minus, Runnable plus) {
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> minus.run())
            .bounds(colX, y, ARROW_W, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> plus.run())
            .bounds(colX + ARROW_W + 4 + LABEL_W + 4, y, ARROW_W, 20).build());
    }

    private Component toggleLabel() {
        return Component.literal(cfg.enabled ? "Begleiter: An" : "Begleiter: Aus");
    }

    private void cycleAnimal(int dir) {
        AnimalType[] all = AnimalType.values();
        int i = Math.floorMod(cfg.animal().ordinal() + dir, all.length);
        cfg.setAnimal(all[i]);
    }

    private void randomize() {
        AnimalType[] all = AnimalType.values();
        cfg.setAnimal(all[random.nextInt(all.length)]);
        int a = random.nextInt(Palette.count());
        int b = random.nextInt(Palette.count());
        if (a == b) {
            b = Palette.wrap(b + 1 + random.nextInt(Palette.count() - 1));
        }
        cfg.setPrimary(a);
        cfg.setSecondary(b);
    }

    @Override
    public void onClose() {
        BegleiterConfig.save();
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Zeit seit dem letzten Bild (unabhängig vom Spiel-Tick, damit die Vorschau flüssig läuft)
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;
        time += dt;

        String title = "Begleiter anpassen";
        graphics.text(this.font, title, (this.width - this.font.width(title)) / 2, 14, 0xFFFFFFFF, true);

        // Beschriftungen zwischen den Pfeil-Schaltflächen
        int labelX = colX + ARROW_W + 4;
        int y = firstRowY;
        label(graphics, labelX, y, "Tier: " + cfg.animal().label, -1);
        y += ROW_H;
        boolean human = cfg.animal() == AnimalType.HUMAN;
        boolean skin = human && HumanSkin.colors() != null;
        label(graphics, labelX, y, (human ? "Oberteil: " : "Fell: ") + Palette.NAMES[Palette.wrap(cfg.primaryColor)], cfg.primaryColor);
        y += ROW_H;
        label(graphics, labelX, y, (human ? "Hose: " : "Akzent: ") + Palette.NAMES[Palette.wrap(cfg.secondaryColor)], cfg.secondaryColor);
        y += ROW_H;
        label(graphics, labelX, y, "Größe: " + Math.round(cfg.size * 100f) + " %", -1);
        y += ROW_H;
        label(graphics, labelX, y, "Pose: " + PREVIEW_NAMES[previewIndex], -1);

        drawPreview(graphics, dt);

        if (human) {
            String line = skin ? "Eigener Skin aktiv (Farben aus der Datei)" : "Skin als config/begleiter-skin.png ablegen, dann \"Skin laden\"";
            graphics.text(this.font, line, 22, this.height - 46, 0xFFFFFF77, false);
        }
        if (!status.isEmpty()) {
            graphics.text(this.font, status, 22, this.height - 34, 0xFF9FE79F, false);
        }

        String hint = "Rechtsklick mit leerer Hand = streicheln | Schleichen + Rechtsklick: Platz, Kopf, Komm";
        graphics.text(this.font, hint, (this.width - this.font.width(hint)) / 2, this.height - 18, 0xFFAAAAAA, false);
    }

    private void label(GuiGraphicsExtractor g, int x, int y, String text, int colorIndex) {
        int tx = x + 4;
        if (colorIndex >= 0) {
            int rgb = 0xFF000000 | Palette.RGB[Palette.wrap(colorIndex)];
            g.fill(x + 3, y + 5, x + 15, y + 17, 0xFF000000);
            g.fill(x + 4, y + 6, x + 14, y + 16, rgb);
            tx = x + 20;
        }
        g.text(this.font, text, tx, y + 6, 0xFFFFFFFF, true);
    }

    private void drawPreview(GuiGraphicsExtractor g, float dt) {
        int panelL = 20;
        int panelR = this.width / 2 + 4;
        int panelT = 30;
        int panelB = this.height - 30;
        g.fill(panelL, panelT, panelR, panelB, 0x66000000);

        Activity act = PREVIEW_ACTIVITIES[previewIndex];
        in.time = time;
        in.walkAmt = act == Activity.FOLLOW ? 1f : 0f;
        in.walkPhase = time * 7f;
        in.headYaw = (float) Math.sin(time * 0.7f) * 0.25f;
        in.headPitch = 0f;
        in.happy = 0f;

        AnimalType type = cfg.animal();
        Animator.compute(target, Models.of(type), act, in);
        pose.lerpTo(target, 1f - (float) Math.exp(-dt * 12f));

        float scale = Math.max(3f, Math.min(9f, (panelB - panelT) / 40f));
        float cx = (panelL + panelR) / 2f;
        float cy = panelT + (panelB - panelT) * 0.72f;
        float viewYaw = 0.65f + 0.5f * (float) Math.sin(time * 0.6f);

        ModelProjector.draw((x1, y1, x2, y2, argb) -> g.fill(x1, y1, x2, y2, argb),
            Models.of(type), pose, cfg.primaryColor, cfg.secondaryColor, cx, cy, scale, viewYaw, 0.3f);
    }
}
