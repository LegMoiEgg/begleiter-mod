package de.begleiter.model;

/** Was der Begleiter gerade tut. */
public enum Activity {
    /** Läuft um den Spieler herum bzw. folgt ihm. */
    FOLLOW,
    SIT,
    LOOK_AROUND,
    GROOM,
    CHASE_TAIL,
    SLEEP,
    PLAY,
    SNIFF;

    /** Zählt zu den Sitz-Varianten (Körper sitzt am Boden). */
    public boolean isSitting() {
        return this == SIT || this == LOOK_AROUND || this == GROOM;
    }
}
