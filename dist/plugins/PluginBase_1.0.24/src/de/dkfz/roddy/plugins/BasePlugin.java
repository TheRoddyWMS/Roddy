package de.dkfz.roddy.plugins;

/**
 * The base class for all Roddy Plugins.
 */
public abstract class BasePlugin {

    public static final String CURRENT_VERSION_STRING = "1.0.24";
    public static final String CURRENT_VERSION_BUILD_DATE = "Wed Feb 04 10:44:07 CET 2015";

    public abstract String getVersionInfo();
}
