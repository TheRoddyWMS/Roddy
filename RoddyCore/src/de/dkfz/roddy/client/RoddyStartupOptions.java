package de.dkfz.roddy.client;

/**
 * Additional options for the Roddy startup.
 *
 */
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
    autocleanup,
    useconfig(true),
    verbositylevel(true),
    debugOptions(true),
    waitforjobs,
    test,
    useiodir(true),
    disabletrackonlyuserjobs,
    trackonlystartedjobs,
    resubmitjobonerror,
    autosubmit,
    run(true),
    dontrun(true),
    
    useRoddyVersion(true),
    usePluginVersion(true),
    pluginDirectories(true), 
    configurationDirectories(true),
    commandFactoryClass(true),
    executionServiceClass(true), 
    executionServiceAuth(true), 
    executionServiceHost(true), 
    executionServiceUser(true),
    
    detailed;

    public final boolean acceptsParameters;

    RoddyStartupOptions() {
        this(false);
    }

    RoddyStartupOptions(boolean acceptsParameters) {
        this.acceptsParameters = acceptsParameters;
    }
}
