/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools;

import groovy.transform.Sortable;

/**
 * A file object tuple designed for (generic) job calls with 3 entries
 * tuples are extended by a new class if necessary. This is to reduce code
 * duplicity but also makes the code more complex. Don't know what is better.
 * Maybe a generic Tuple[n] class would be the best way. But then this
 * class would have n accessors.
 */
public class Tuple5<X, Y, Z, W, Q> {

    public final X x;
    public final Y y;
    public final Z z;
    public final W w;
    public final Q q;

    public Tuple5(X x, Y y, Z z, W w, Q q) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        this.q = q;
    }
}
