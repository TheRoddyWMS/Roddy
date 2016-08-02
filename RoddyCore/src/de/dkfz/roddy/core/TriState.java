/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core;

/**
 */
public enum TriState {
    TRUE(true),
    FALSE(false),
    UNKNOWN(false);
    private final boolean defaultsTo;

    private TriState(boolean defaultsTo) {

        this.defaultsTo = defaultsTo;
    }

    public boolean toBoolean() {
        return defaultsTo;
    }
}
