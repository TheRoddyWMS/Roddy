/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client


import groovy.transform.CompileStatic

/**
 * Additional options for the Roddy startup.
 *
 */
@CompileStatic
enum RoddyStartupOptions {
    /**
     * An option specifically for listworkflows.
     * Prints a list in this way:
     * ICGCeval@genome
     * ICGCeval@exome
     * ICGCeval.dbg@genome
     * ICGCeval.dbg@exome
     */
    shortList(),
    extendedList(),
    /**
     * Shows entry values and source files for e.g. printruntimeconfig
     */
    showEntrySources(),
    autoCleanup(),
    config(true),
    c(true),
    featureToggleConfig(true),
    verbosityLevel(true),
    v(false), // Verbosity of 3
    vv(false), // Verbosity of 5
    debugOptions(true),
    waitForJobs(),
    test(),
    ioDir(true),
    metadataTable(true),

    forceNativePluginConversion(false),
    forceKeepExecutionDirectory(false),
    ignoreCValueDuplicates(false),
    ignoreConfigurationErrors(false),

    resourcesSize(true),
    disableTrackOnlyUserJobs(),
    trackOnlyStartedJobs(),
    resubmitJobOnError(),
    autoSubmit(),
    run(true),
    dontRun(true),
    cvalues(true),
    enableToggles(true),
    disableToggles(true),

    disableStrictFileChecks(false),

    roddyVersion(true),
    rv(true),
    pluginVersion(true),
    pluginDirectories(true),
    configurationDirectories(true),
    additionalImports(true),

    jobManagerClass(true),
    executionServiceClass(true),
    executionServiceAuth(true),
    executionServiceHost(true),
    executionServiceUser(true),

    // Only for configuration free mode! Tells Roddy which base configuration should be used for the internally created configuration.
    baseConfig(true),

    repository(true),

    detailed(),
    disallowExit()

    ;

    public final boolean acceptsParameters

    RoddyStartupOptions() {
        this(false)
    }

    RoddyStartupOptions(boolean acceptsParameters) {
        this.acceptsParameters = acceptsParameters
    }

    // RoddyStartupOptions are matched by first stripping off any /used?/ prefix, transforming to lower case and
    // matching against the lower case option name.
    static Optional<RoddyStartupOptions> fromString(String option) {
        Map<String, RoddyStartupOptions> options =
                values().collectEntries { [it.name().toLowerCase(), it] }
        return Optional.ofNullable(options.get(option.toLowerCase().replaceFirst(/used?/, ""), null))
    }
}
