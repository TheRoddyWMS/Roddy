package de.dkfz.roddy.tools;

/**
 * A result tuple for two results
 */
public class Tuple2<X, Y> {
    public final X x;
    public final Y y;

    public Tuple2(X x, Y y) {
        this.x = x;
        this.y = y;
    }
}
