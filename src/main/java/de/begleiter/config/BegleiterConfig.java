package de.begleiter.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.begleiter.model.AnimalType;
import de.begleiter.model.HumanSkin;
import de.begleiter.model.Palette;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Gespeicherte Einstellungen (config/begleiter.json). */
public final class BegleiterConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BegleiterConfig instance = new BegleiterConfig();

    public boolean enabled = true;
    public String animal = AnimalType.CAT.name();
    public int primaryColor = AnimalType.CAT.defaultPrimary;
    public int secondaryColor = AnimalType.CAT.defaultSecondary;
    /** Größenfaktor, 1.0 = Standard. */
    public float size = 1.0f;
    /** Pfad zur Skin-Datei des kleinen Menschen (leer = kein Skin). */
    public String skinPath = "";

    /** Wird bei jeder optischen Änderung erhöht, damit die Welt-Darstellung neu aufgebaut wird. */
    public transient int revision;

    public static BegleiterConfig get() {
        return instance;
    }

    public AnimalType animal() {
        return AnimalType.byName(animal, AnimalType.CAT);
    }

    public void setAnimal(AnimalType type) {
        this.animal = type.name();
        touch();
    }

    public void setPrimary(int index) {
        this.primaryColor = Palette.wrap(index);
        touch();
    }

    public void setSecondary(int index) {
        this.secondaryColor = Palette.wrap(index);
        touch();
    }

    public void setSize(float value) {
        this.size = Math.max(0.5f, Math.min(2.0f, Math.round(value * 10f) / 10f));
        touch();
    }

    /** Lädt die Skin-Datei neu und übernimmt ihre Farben (null/leer entfernt den Skin). */
    public void setSkin(String path) {
        this.skinPath = path == null ? "" : path;
        applySkin();
        touch();
    }

    /** Liest {@link #skinPath} und setzt die Farben des Menschen; gibt zurück, ob ein Skin geladen wurde. */
    public boolean applySkin() {
        HumanSkin.set(null);
        if (skinPath == null || skinPath.isBlank()) {
            return false;
        }
        try {
            Path f = Path.of(skinPath);
            if (!Files.isRegularFile(f)) {
                return false;
            }
            int[] colors = HumanSkin.extract(ImageIO.read(f.toFile()));
            HumanSkin.set(colors);
            return colors != null;
        } catch (IOException | RuntimeException e) {
            System.err.println("[Begleiter] Skin konnte nicht gelesen werden: " + e);
            return false;
        }
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
        touch();
    }

    public void touch() {
        revision++;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("begleiter.json");
    }

    public static void load() {
        Path f = file();
        if (Files.isRegularFile(f)) {
            try {
                BegleiterConfig loaded = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), BegleiterConfig.class);
                if (loaded != null) {
                    loaded.primaryColor = Palette.wrap(loaded.primaryColor);
                    loaded.secondaryColor = Palette.wrap(loaded.secondaryColor);
                    loaded.size = Math.max(0.5f, Math.min(2.0f, loaded.size == 0f ? 1.0f : loaded.size));
                    if (loaded.skinPath == null) {
                        loaded.skinPath = "";
                    }
                    instance = loaded;
                }
            } catch (IOException | RuntimeException e) {
                System.err.println("[Begleiter] Konfiguration konnte nicht gelesen werden: " + e);
            }
        } else {
            save();
        }
        instance.applySkin();
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[Begleiter] Konfiguration konnte nicht gespeichert werden: " + e);
        }
    }
}
