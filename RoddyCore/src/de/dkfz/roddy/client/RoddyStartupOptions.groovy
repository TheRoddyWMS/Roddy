/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client;

/**
 * Additional options for the Roddy startup.
 *
 */
@groovy.transform.CompileStatic
public enum RoddyStartupOptions {
    /**
     * An option specifically for listworkflows.
     * Prints a list in this way:
     * ICGCeval@genome
     * ICGCeval@exome
     * ICGCeval.dbg@genome
     * ICGCeval.dbg@exome
     */
    shortlist,
    extendedlist,
    /**
     * Shows entry values and source files for e.g. printruntimeconfig
     */
    showentrysources,
    autocleanup,
    useconfig(true),
    c(true),
    usefeaturetoggleconfig(true),
    verbositylevel(true),
    v(false), // Verbosity of 3
    vv(false), // Verbosity of 5
    debugOptions(true),
    waitforjobs,
    test,
    useiodir(true),
    usemetadatatable(true),
    /**
     * Override project speicifc usedresourcessize
     */
    usedresourcessize(true),
    disabletrackonlyuserjobs,
    trackonlystartedjobs,
    resubmitjobonerror,
    autosubmit,
    run(true),
    dontrun(true),
    cvalues(true),
    enabletoggles(true),
    disabletoggles(true),

    ignorepreparedfilechecks(false),

    useRoddyVersion(true),
    useroddyversion(true),
    rv(true),
    usePluginVersion(true),
    pluginDirectories(true), 
    configurationDirectories(true),
    jobManagerClass(true),
    executionServiceClass(true), 
    executionServiceAuth(true), 
    executionServiceHost(true), 
    executionServiceUser(true),

    userepository(true),

    detailed, disallowexit;

    public final boolean acceptsParameters;

    RoddyStartupOptions() {
        this(false);
    }

    RoddyStartupOptions(boolean acceptsParameters) {
        this.acceptsParameters = acceptsParameters;
    }
}
