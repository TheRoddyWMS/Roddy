package de.dkfz.roddy.plugins;

/**
 * The base class for all Roddy Plugins.
 */
public abstract class BasePlugin {

    public static final String CURRENT_VERSION_STRING = "1.0.26";
    public static final String CURRENT_VERSION_BUILD_DATE = "Wed Apr 15 15:38:49 CEST 2015";

    public abstract String getVersionInfo();
}
