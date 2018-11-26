/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools

import groovy.transform.CompileStatic

/**
 * Created by heinold on 26.04.17.
 */
@CompileStatic
class CollectionHelperMethods {

    static boolean intersects(Collection a, Collection b) {
        return intersect(a, b).size() > 0
    }

    static Collection intersect(Collection a, Collection b) {
        return a.findAll { _a -> b.contains(_a) }
    }

    static List intersectList(Collection a, Collection b) {
        return a.findAll { _a -> b.contains(_a) } as List
    }
}
