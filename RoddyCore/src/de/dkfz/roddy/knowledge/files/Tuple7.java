/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

/**
 * A file object tuple designed for (generic) job calls with 7 entries
 */
public class Tuple7<
        X extends FileObject,
        Y extends FileObject,
        Z extends FileObject,
        R extends FileObject,
        Q extends FileObject,
        W extends FileObject,
        S extends FileObject>
        extends Tuple6<X, Y, Z, R, Q, W> {
    private final S value6;

    public Tuple7(X value0, Y value1, Z value2, R value3, Q value4, W value5, S value6) {
        super(value0, value1, value2, value3, value4, value5);
        this.value6 = value6;
    }
}

