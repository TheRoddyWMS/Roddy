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
     * Use this scope to ignore options for command line processing. They will not be used in the Roddy binary!
     * Roddy may be called with the requested mode, but without options.
     *
     * no job manager, no execution service, no filesystem access provider
     * help
     */
    SCOPE_CLI,

    /**
     * This scope is for startup modes where only a reduced init is necessary.
     * direct job manager, no execution service, no filesystem access provider
     */
    SCOPE_REDUCED(true),

    /**
     * This scope is for modes with a full init. The job manager however will be the direct job manager.
     * direct job manager, execution service, filesystem access provider
     */
    SCOPE_FULL(true),

    /**
     * This scope starts all modes and also initializes the Job Manager
     * custom job manager, execution service, filesystem access provider
     */
    SCOPE_FULL_WITHJOBMANAGER(true);

    public final boolean needsJobManager;

    RoddyStartupModeScopes() {
        this(false);
    }

    RoddyStartupModeScopes(boolean fullInit) {
        this.needsJobManager = fullInit;
    }
}
