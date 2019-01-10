/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client

import de.dkfz.roddy.tools.EnumHelper;

/**
 * Additional options for the Roddy startup.
 *
 */
@groovy.transform.CompileStatic
enum RoddyStartupOptions {
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

    forcenativepluginconversion(false),
    forcekeepexecutiondirectory(false),
    ignorecvalueduplicates(false),
    ignoreconfigurationerrors(false),

    /**
     * Override project specific usedresourcessize
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

    disablestrictfilechecks(false),

    @Deprecated
    useRoddyVersion(true),
    useroddyversion(true),
    rv(true),
    usePluginVersion(true),
    pluginDirectories(true),
    configurationDirectories(true),
    additionalImports(true),
    additionalimports(true),

    jobManagerClass(true),
    executionServiceClass(true),
    executionServiceAuth(true),
    executionServiceHost(true),
    executionServiceUser(true),

    // Only for configuration free mode! Tells Roddy which base configuration should be used for the internally created configuration.
    baseconfig(true),

    userepository(true),

    detailed,
    disallowexit;

    public final boolean acceptsParameters;

    RoddyStartupOptions() {
        this(false);
    }

    RoddyStartupOptions(boolean acceptsParameters) {
        this.acceptsParameters = acceptsParameters;
    }

    static Optional<RoddyStartupOptions> fromString(String option) {
        EnumHelper.castFromString(option)
    }
}
