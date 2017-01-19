/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools;

import de.dkfz.roddy.knowledge.files.FileObject;

/**
 * A file object tuple designed for (generic) job calls with 3 entries
 * tuples are extended by a new class if necessary. This is to reduce code
 * duplicity but also makes the code more complex. Don't know what is better.
 * Maybe a generic Tuple[n] class would be the best way. But then this
 * class would have n accessors.
 */
public class Tuple3<X, Y, Z> extends Tuple2<X,Y> {
    public Z z;

    public Tuple3(X x, Y y, Z z) {
        super(x,y);
        this.z = z;
    }
}
