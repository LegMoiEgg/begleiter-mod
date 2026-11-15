package de.begleiter.brain;

/** Bodenabfragen, die das Verhalten braucht (im Spiel von der Client-Welt beantwortet). */
public interface Terrain {
    /** Höhe der Oberkante des tragenden Bodens unterhalb von y + 1.25, oder NaN wenn keiner da ist. */
    double findGround(double x, double y, double z);

    /**
     * Kann das Tier von Höhe curY aus auf (nx, nz) treten? Dazu gehört, dass der ganze Körper
     * (Radius und Höhe aus {@link #setBody}) in keinen Block ragt.
     */
    boolean canStand(double nx, double curY, double nz);

    /** Körpermaße des Tieres in Blöcken für die Kollisionsprüfung. */
    default void setBody(double radius, double height) {}

    /** Oberkante des Wassers an dieser Stelle (nahe Höhe y) oder NaN, wenn dort kein Wasser ist. */
    default double waterSurface(double x, double y, double z) {
        return Double.NaN;
    }

    /** Kann das Tier an dieser Stelle schwimmen (Wasser, Körper stößt nirgends an)? */
    default boolean canSwim(double nx, double curY, double nz) {
        return false;
    }

    /** Ist über (x, y, z) genug Platz für einen Körper mit Radius r und Höhe h? */
    default boolean clearance(double x, double y, double z, double r, double h) {
        return true;
    }
}
