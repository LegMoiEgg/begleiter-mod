package de.begleiter.model;

/** Eingaben für die Animation (gleich für Welt und Menü-Vorschau). */
public final class AnimInput {
    /** Laufende Zeit in Sekunden. */
    public float time;
    /** Phase des Laufzyklus in Radiant. */
    public float walkPhase;
    /** 0 = steht, 1 = läuft/rennt. */
    public float walkAmt;
    /** Kopfdrehung zum Blickziel: Yaw positiv = nach links, Pitch positiv = nach unten (Radiant). */
    public float headYaw;
    public float headPitch;
    /** 0..1, Freude (z. B. nach dem Streicheln): Schwanzwedeln. */
    public float happy;
}
