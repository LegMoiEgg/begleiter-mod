package de.begleiter.brain;

/** Bodenabfragen, die das Verhalten braucht (im Spiel von der Client-Welt beantwortet). */
public interface Terrain {
    /** Höhe der Oberkante des tragenden Bodens unterhalb von y + 1.25, oder NaN wenn keiner da ist. */
    double findGround(double x, double y, double z);

    /** Kann das Tier von Höhe curY aus auf (nx, nz) treten? */
    boolean canStand(double nx, double curY, double nz);
}
