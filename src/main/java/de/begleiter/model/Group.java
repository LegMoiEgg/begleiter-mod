package de.begleiter.model;

/**
 * Bewegliche Körpergruppen. Die Reihenfolge ist wichtig: Eltern stehen immer vor ihren Kindern.
 * Beine hängen an ROOT (nicht am Körper), damit der Körper sich beim Sitzen neigen kann,
 * ohne dass die Vorderbeine mitkippen.
 */
public enum Group {
    ROOT(-1),
    BODY(0),
    HEAD(1),
    TAIL(1),
    EAR_L(2),
    EAR_R(2),
    LEG_FL(0),
    LEG_FR(0),
    LEG_BL(0),
    LEG_BR(0),
    /** Arme (nur beim Menschen), hängen am Körper. */
    ARM_L(1),
    ARM_R(1);

    /** Ordinal des Elternteils, -1 für ROOT. */
    public final int parentOrdinal;

    Group(int parentOrdinal) {
        this.parentOrdinal = parentOrdinal;
    }

    public static final int COUNT = values().length;
}
