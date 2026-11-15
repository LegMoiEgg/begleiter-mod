package de.begleiter.model;

import java.util.EnumMap;
import java.util.Map;

/** Verfügbare Tiere. Neue Tiere: hier eintragen und in {@link Models} ein Modell bauen. */
public enum AnimalType {
    CAT("Katze", 1, 0, 0.26f),
    DOG("Hund", 12, 0, 0.30f),
    RABBIT("Hase", 8, 0, 0.22f),
    FOX("Fuchs", 1, 0, 0.30f);

    public final String label;
    public final int defaultPrimary;
    public final int defaultSecondary;
    /** Höchstgeschwindigkeit beim normalen Folgen in Blöcken pro Tick. */
    public final float speed;

    AnimalType(String label, int defaultPrimary, int defaultSecondary, float speed) {
        this.label = label;
        this.defaultPrimary = defaultPrimary;
        this.defaultSecondary = defaultSecondary;
        this.speed = speed;
    }

    /** Gewichte für die Auswahl einer Beschäftigung, wenn der Spieler stillsteht. */
    public Map<Activity, Integer> idleWeights() {
        Map<Activity, Integer> m = new EnumMap<>(Activity.class);
        switch (this) {
            case CAT -> {
                m.put(Activity.SIT, 2);
                m.put(Activity.LOOK_AROUND, 2);
                m.put(Activity.GROOM, 3);
                m.put(Activity.CHASE_TAIL, 1);
                m.put(Activity.PLAY, 1);
                m.put(Activity.SLEEP, 2);
            }
            case DOG -> {
                m.put(Activity.SIT, 3);
                m.put(Activity.LOOK_AROUND, 1);
                m.put(Activity.CHASE_TAIL, 2);
                m.put(Activity.PLAY, 3);
                m.put(Activity.SNIFF, 2);
                m.put(Activity.SLEEP, 1);
            }
            case RABBIT -> {
                m.put(Activity.SIT, 2);
                m.put(Activity.SNIFF, 4);
                m.put(Activity.GROOM, 2);
                m.put(Activity.PLAY, 3);
                m.put(Activity.SLEEP, 1);
            }
            case FOX -> {
                m.put(Activity.SIT, 2);
                m.put(Activity.LOOK_AROUND, 2);
                m.put(Activity.PLAY, 3);
                m.put(Activity.SNIFF, 2);
                m.put(Activity.CHASE_TAIL, 1);
                m.put(Activity.SLEEP, 1);
            }
        }
        return m;
    }

    public static AnimalType byName(String name, AnimalType fallback) {
        if (name != null) {
            for (AnimalType t : values()) {
                if (t.name().equalsIgnoreCase(name)) {
                    return t;
                }
            }
        }
        return fallback;
    }
}
