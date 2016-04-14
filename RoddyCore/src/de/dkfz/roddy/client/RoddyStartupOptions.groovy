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
    usefeaturetoggleconfig(true),
    verbositylevel(true),
    debugOptions(true),
    waitforjobs,
    test,
    useiodir(true),
    usemetadatatable(true),
    disabletrackonlyuserjobs,
    trackonlystartedjobs,
    resubmitjobonerror,
    autosubmit,
    run(true),
    dontrun(true),
    cvalues(true),
    enabletoggles(true),
    disabletoggles(true),

    useRoddyVersion(true),
    usePluginVersion(true),
    pluginDirectories(true), 
    configurationDirectories(true),
    commandFactoryClass(true),
    executionServiceClass(true), 
    executionServiceAuth(true), 
    executionServiceHost(true), 
    executionServiceUser(true),
    
    detailed, disallowexit;

    public final boolean acceptsParameters;

    RoddyStartupOptions() {
        this(false);
    }

    RoddyStartupOptions(boolean acceptsParameters) {
        this.acceptsParameters = acceptsParameters;
    }
}
