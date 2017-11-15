/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client;

/**
 * Created by michael on 31.03.15.
 */
@groovy.transform.CompileStatic
public enum RoddyStartupModeScopes {

    /**
     * Use this scope to leave options for command line processing.
     * They will not be used in the Roddy binary!
     * Instead, they need a script file and are fully handled in the roddy.sh
     * This is useful, if things can be handled in an easier way with scripts,
     * like i.e. file operations
     */
    SCOPE_CLI,

    /**
     * This scope is for startup modes where only a reduced init is necessary.
     */
    SCOPE_REDUCED,

    /**
     * This scope is for modes with a full init. The job manager however will be the direct job manager.
     */
    SCOPE_FULL(true),

    /**
     * This scope starts all modes and also initializes the Job Manager
     */
    SCOPE_FULL_WITHJOBMANAGER(true);

    public final boolean needsFullInit;

    RoddyStartupModeScopes() {
        this(false);
    }

    RoddyStartupModeScopes(boolean fullInit) {
        this.needsFullInit = fullInit;
    }
}
