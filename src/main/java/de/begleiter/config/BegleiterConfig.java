package de.begleiter.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.begleiter.model.AnimalType;
import de.begleiter.model.Palette;
import java.io.IOException;
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
                    instance = loaded;
                }
            } catch (IOException | RuntimeException e) {
                System.err.println("[Begleiter] Konfiguration konnte nicht gelesen werden: " + e);
            }
        } else {
            save();
        }
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[Begleiter] Konfiguration konnte nicht gespeichert werden: " + e);
        }
    }
}
