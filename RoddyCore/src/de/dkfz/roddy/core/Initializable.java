/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core;

import java.util.LinkedList;
import java.util.List;

/**
 * Base class for objects which should be initialized and destroyed by a project.
 */
public abstract class Initializable {

    private static List<Initializable> initializables = new LinkedList<Initializable>();

    protected Initializable() {
        initializables.add(this);
    }

    public abstract boolean initialize();

    public boolean initialize(boolean aSwitch) { return true; };

    public abstract void destroy();

    public static void destroyAll() {
        for(Initializable i : initializables) {
            i.destroy();
        }
    }
}
