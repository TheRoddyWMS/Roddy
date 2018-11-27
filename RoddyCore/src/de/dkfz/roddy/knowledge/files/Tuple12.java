/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

/**
 * A file object tuple designed for (generic) job calls with 2 entries
 */
public class Tuple12<
        A extends FileObject,
        B extends FileObject,
        C extends FileObject,
        D extends FileObject,
        E extends FileObject,
        F extends FileObject,
        G extends FileObject,
        H extends FileObject,
        I extends FileObject,
        J extends FileObject,
        K extends FileObject,
        L extends FileObject
        > extends AbstractFileObjectTuple {

    public final A _a;
    public final B _b;
    public final C _c;
    public final D _d;
    public final E _e;
    public final F _f;
    public final G _g;
    public final H _h;
    public final I _i;
    public final J _j;
    public final K _k;
    public final L _l;

    public Tuple12(A _a, B _b, C _c, D _d, E _e, F _f, G _g, H _h, I _i, J _j, K _k, L _l) {
        super(_a.getExecutionContext());
        this._a = _a;
        this._b = _b;
        this._c = _c;
        this._d = _d;
        this._e = _e;
        this._f = _f;
        this._g = _g;
        this._h = _h;
        this._i = _i;
        this._j = _j;
        this._k = _k;
        this._l = _l;
    }
}

