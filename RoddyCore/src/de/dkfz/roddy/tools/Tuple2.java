/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

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
